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
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

/**
 * Shows a soft keyboard, rendering keys and detecting key presses.
 */
public class SoftKeyboardView extends KeyboardView {
  
  private static final int UPPER_CASE_OFFSET = -32;

  public SoftKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SoftKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected boolean onLongPress(Key key) {
    // 0xFF01~0xFF5E map to the full-width forms of the characters from
    // 0x21~0x7E. Make the long press as producing corresponding full-width
    // forms for these characters by adding the offset (0xff01 - 0x21).

    // English lower-case to upper-case
    if(((SoftKeyboard)getKeyboard()).isEnglish() &&
            key.popupResId == 0 && key.codes[0] >= 97 && key.codes[0] <= 124){
        getOnKeyboardActionListener().onKey(key.codes[0] + UPPER_CASE_OFFSET, null);
        return true;
    }

    if (key.codes[0] == SoftKeyboard.KEYCODE_MODE_CHANGE_LETTER) {
      getOnKeyboardActionListener().onKey(SoftKeyboard.KEYCODE_OPTIONS, null);
      return true;
    } else if (key.codes[0] == SoftKeyboard.KEYCODE_MODE_CHANGE) {
      getOnKeyboardActionListener().onKey(SoftKeyboard.KEYCODE_SCHEMA_OPTIONS, null);
      return true;
    } else if (key.codes[0] == "，".charAt(0)) {
      getOnKeyboardActionListener().onKey(SoftKeyboard.KEYCODE_REVERSE, null);
      return true;
    } else {
      return super.onLongPress(key);
    }
  }

}
