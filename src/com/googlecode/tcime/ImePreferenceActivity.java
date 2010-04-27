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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Manages IME preferences. 
 */
public class ImePreferenceActivity extends PreferenceActivity {

  private final String licenseUrl = "file:///android_asset/licensing.html";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.prefs);

    Preference license = findPreference(getString(R.string.prefs_licensing));
    license.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
        showLicenseDialog();
        return true;
      }
    });
  }

  private void showLicenseDialog() {
    View licenseView = View.inflate(this, R.layout.licensing, null);
    WebView webView = (WebView) licenseView.findViewById(R.id.license_view);
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Disable all links open from this web view.
        return true;
      }
    });
    webView.loadUrl(licenseUrl);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.ime_name);
    builder.setView(licenseView);
    builder.show();
  }
}
