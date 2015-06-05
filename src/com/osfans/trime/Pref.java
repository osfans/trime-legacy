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

import android.content.Context;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Pref {
  private final SharedPreferences mPref;

  protected Pref(Context context) {
    mPref = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public boolean isCommitPy() {
    return mPref.getBoolean("pref_commit_py", false);
  }

  public String getOpenCC() {
    return mPref.getString("pref_opencc", "");
  }

  private boolean isFullPy() {
    return mPref.getBoolean("pref_full_py", false);
  }

  public boolean isKeyboardPreview() {
    return mPref.getBoolean("pref_keyboard_preview", true);
  }

  private boolean isSingle() {
    return mPref.getBoolean("pref_single", false);
  }

  private boolean isAssociation() {
    return mPref.getBoolean("pref_association", false);
  }

  private String getQueryCol() {
    return mPref.getBoolean("pref_py_prompt", false) ? "hz, trim(pya || ' ' || pyb || ' ' || pyc || ' ' || pyz) as py" : "hz";
  }

  private boolean isEmbedFirst() {
    return mPref.getBoolean("pref_embed_first", false);
  }

  public boolean isInvalidCommit() {
    return mPref.getString("pref_invalid_action", "0").contentEquals("1");
  }

  public boolean isInvalidClear() {
    return mPref.getString("pref_invalid_action", "0").contentEquals("2");
  }

  public boolean isInvalidAction() {
    return !mPref.getString("pref_invalid_action", "0").contentEquals("0");
  }

  public int getCandTextSize() {
    return Integer.parseInt(mPref.getString("pref_cand_font_size", "22"));
  }

  public int getKeyTextSize() {
    return Integer.parseInt(mPref.getString("pref_key_font_size", "22"));
  }
}
