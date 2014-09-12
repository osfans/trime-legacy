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
import android.inputmethodservice.Keyboard;
import android.util.Log;
import java.util.List;

/**
 * A soft keyboard definition.
 */
public class SoftKeyboard extends Keyboard {

  public static final int KEYCODE_MODE_CHANGE_LETTER = -200;
  public static final int KEYCODE_OPTIONS = -100;
  public static final int KEYCODE_SCHEMA_OPTIONS = -99;
  public static final int KEYCODE_REVERSE = 96;

  private final int id;
  
  public boolean isEnglish() {
    return id == R.xml.en_qwerty || id == R.xml.en_dvorak ;
  }

  public SoftKeyboard(Context context, int xmlLayoutResId) {
    super(context, xmlLayoutResId);
    id = xmlLayoutResId;
  }

  public SoftKeyboard(Context context, int xmlLayoutResId, String[] labels) {
    this(context, xmlLayoutResId);

    List<Keyboard.Key> keys = getKeys();
    for(int i = 0; i < labels.length; i++) {
        Keyboard.Key key = keys.get(i);
        String label = labels[i];
        if (label.length() > 0) {
            if (label.startsWith("[")) {
                String s = label.substring(1, label.length()-1);
                key.label = s.substring(0,2);
                key.codes = new int[s.length()];
                for(int j = 0; j < s.length(); j++) key.codes[j] = s.codePointAt(j);
            } else {
                String[] text = label.split(":");
                if (text[0].length() > 0) {
                    text[0] = text[0].contentEquals("\\\\") ? "\\":text[0].replace("\\","");
                    key.label = text[0];
                    if (key.codes[0] > 0) key.text = text[text.length - 1];
                }
            }
        }
    }
  }
}
