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

package com.osfans.trime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

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

    Preference importdb = findPreference(getString(R.string.prefs_importdb));
    importdb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
        importDatabase();
        return true;
      }
    });

    Preference exportdb = findPreference(getString(R.string.prefs_exportdb));
    exportdb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
        exportDatabase();
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

  private void importDatabase() {
    boolean ret = false;
    try {
        File dbFile = new File("/sdcard", "trime.db");
        if (dbFile.exists()) {
            AssetDatabaseOpenHelper.importDatabase(new FileInputStream(dbFile));
            ret = true;
        }
    } catch (IOException e) {
        throw new RuntimeException("Error creating source database", e);
    }
    Toast.makeText(getApplicationContext(), ret ? R.string.importdb_success : R.string.importdb_failure, Toast.LENGTH_SHORT).show();
  }

  private void exportDatabase() {
    boolean ret = false;
    try {
        File dbFile = new File("/sdcard", "trime.db");
        AssetDatabaseOpenHelper.exportDatabase(new FileOutputStream(dbFile));
        ret = true;
    } catch (IOException e) {
        throw new RuntimeException("Error creating source database", e);
    }
    Toast.makeText(getApplicationContext(), ret ? R.string.exportdb_success : R.string.exportdb_failure, Toast.LENGTH_SHORT).show();
  }
}
