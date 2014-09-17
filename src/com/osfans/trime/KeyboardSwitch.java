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
import android.text.InputType;

/**
 * Switches between two letter modes (Chinese and English), by two toggling
 * keys: mode-change-letter, mode-change-layout.
 * 
 * <pre>
 * State transition (the initial state is always 'English'):
 *   English
 *     MODE_CHANGE_LETTER -> Chinese
 *     SHIFT -> English (no-op)
 *   Chinese
 *     MODE_CHANGE_LETTER -> English
 *     SHIFT (n/a)
 * </pre>
 *     
 */
public class KeyboardSwitch {

  private final Context context;
  private int englishKeyboardId = 0;
  private final int[] englishKeyboardIds = {R.xml.en_qwerty, R.xml.en_dvorak};
  private int chineseKeyboardId;
  private int chineseKeyboardCount;

  private SoftKeyboard[] englishKeyboards;
  private SoftKeyboard[] chineseKeyboards;
  private SoftKeyboard currentKeyboard;
  private String chineses;

  private int currentDisplayWidth;

  public KeyboardSwitch(Context context) {
    this.context = context;
  }

  public void reset(){
    chineseKeyboardId = 0;
    toEnglish(false);
  }

  public void initializeKeyboard(String s) {
    chineses = s;
    englishKeyboards = new SoftKeyboard[englishKeyboardIds.length];
    for (int i = 0; i < englishKeyboardIds.length; i++ ) englishKeyboards[i] = new SoftKeyboard(context, englishKeyboardIds[i]);

    String[] keys = s.split("\n");
    chineseKeyboardCount = keys.length;
    chineseKeyboards = new SoftKeyboard[chineseKeyboardCount];
    for (int i = 0; i < chineseKeyboardCount; i++ ) {
        String[]labels = keys[i].split("\\|");
        int xmlLayoutResId;
        switch (labels.length) {
            case 27:
            case 28:
            case 29:
                xmlLayoutResId = R.xml.key27;
                break;
            case 30:
            case 31:
            case 32:
                xmlLayoutResId = R.xml.key30;
                break;
            case 40:
            case 41:
            case 42:
                xmlLayoutResId = R.xml.key40;
                break;
            case 50:
            case 51:
            case 52:
                xmlLayoutResId = R.xml.key50;
                break;
            default:
                xmlLayoutResId = R.xml.key37;
                break;
        }
        chineseKeyboards[i] = new SoftKeyboard(context, xmlLayoutResId, labels);
    }
    reset();
  }

  /**
   * Recreates the keyboards if the display-width has been changed.
   * 
   * @param displayWidth the display-width for keyboards.
   */
  public void initializeKeyboard(int displayWidth) {
    if ((currentKeyboard != null) && (displayWidth == currentDisplayWidth)) {
      return;
    }

    currentDisplayWidth = displayWidth;
    initializeKeyboard(chineses);
  }

  public Keyboard getCurrentKeyboard() {
    return currentKeyboard;
  }

  public boolean isEnglish() {
    return currentKeyboard != null && currentKeyboard.isEnglish();
  }

  /**
   * Switches to the appropriate keyboard based on the type of text being
   * edited, for example, the symbol keyboard for numbers.
   * 
   * @param inputType one of the {@code InputType.TYPE_CLASS_*} values listed in
   *     {@link android.text.InputType}.
   */
  public void onStartInput(int inputType) {
    if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if ((variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            || (variation == InputType.TYPE_TEXT_VARIATION_URI)
            || (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD)
            || (variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) || (currentKeyboard.isEnglish())) {
          toEnglish(true);
          currentKeyboard.setShifted(currentKeyboard.isShifted());
        } else toEnglish(false);
     }
  }

  /**
   * Consumes the pressed key-code and switch keyboard if applicable.
   * 
   * @return {@code true} if the keyboard is switched; otherwise {@code false}.
   */
  public boolean onKey(int keyCode) {
    switch (keyCode) {
      case SoftKeyboard.KEYCODE_MODE_CHANGE_LETTER:
        toEnglish(!isEnglish());
        return true;

      case SoftKeyboard.KEYCODE_MODE_CHANGE:
        if (currentKeyboard.isEnglish()) {
           englishKeyboardId++;
           if (englishKeyboardId >= englishKeyboardIds.length) englishKeyboardId = 0;
           toEnglish(true);
        } else {
           chineseKeyboardId++;
           if (chineseKeyboardId >= chineseKeyboardCount) chineseKeyboardId = 0;
           toEnglish(false);
        }
        return true;
    }

    // Return false if the key isn't consumed to switch a keyboard.
    return false;
  }

  private void toEnglish(boolean isEnglish) {
    currentKeyboard = isEnglish ? englishKeyboards[englishKeyboardId] : chineseKeyboards[chineseKeyboardId];
  }
}
