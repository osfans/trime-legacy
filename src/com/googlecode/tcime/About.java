/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.tcime;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Shows IME about-information.
 */
public class About implements DialogInterface.OnDismissListener {

  private final Context context;
  private final String licenseUrl = "file:///android_asset/licensing.html";
  private final String aboutTitle;
  private Dialog aboutDialog;
  private Dialog licenseDialog;

  public About(Context context) {
    aboutTitle = context.getString(R.string.ime_name);
    this.context = context;
  }

  /**
   * Displays an about modal dialog over the given view.
   */
  public void show(final View view) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(aboutTitle);
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.setItems(
        new CharSequence[] { context.getString(R.string.about_licensing) },
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            if (which == 0) {
              showLicenseDialog(view);
            }
          }
        });
    aboutDialog = builder.create();
    attachDialogToView(aboutDialog, view);
    aboutDialog.setOnDismissListener(this);
    aboutDialog.show();
  }

  private void showLicenseDialog(View view) {
    View licenseView = View.inflate(context, R.layout.licensing, null);
    WebView webView = (WebView) licenseView.findViewById(R.id.license_view);
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Disable all links open from this web view.
        return true;
      }
    });
    webView.loadUrl(licenseUrl);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(aboutTitle);
    builder.setView(licenseView);
    licenseDialog = builder.create();;
    attachDialogToView(licenseDialog, view);
    licenseDialog.setOnDismissListener(this);
    licenseDialog.show();
  }
  
  private void attachDialogToView(Dialog dialog, View view) {
    Window window = dialog.getWindow();
    WindowManager.LayoutParams params = window.getAttributes();
    params.token = view.getWindowToken();
    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    window.setAttributes(params);
    window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
  }

  /**
   * Dismisses any dialogs in display. 
   */
  public void dismiss() {
    if (aboutDialog != null) {
      aboutDialog.dismiss();
    }
    if (licenseDialog != null) {
      licenseDialog.dismiss();
    }
  }

  public void onDismiss(DialogInterface dialog) {
    if (dialog == aboutDialog) {
      aboutDialog = null;
    } else if (dialog == licenseDialog) {
      licenseDialog = null;
    }
  }
}
