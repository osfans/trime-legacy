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

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.text.InputType;

/**
 * Switches between four input modes: two symbol modes (number-symbol and
 * shift-symbol) and two letter modes (Chinese and English), by three toggling
 * keys: mode-change-letter, mode-change, and shift keys.
 * 
 * <pre>
 * State transition (the initial state is always 'English'):
 *   English
 *     MODE_CHANGE_LETTER -> Chinese
 *     MODE_CHANGE -> NumberSymbol
 *     SHIFT -> English (no-op)
 *   Chinese
 *     MODE_CHANGE_LETTER -> English
 *     MODE_CHANGE -> NumberSymbol
 *     SHIFT (n/a)
 *   NumberSymbol
 *     MODE_CHANGE_LETTER (n/a) 
 *     MODE_CHANGE -> English or Chinese
 *     SHIFT -> ShiftSymbol
 *   ShiftSymbol
 *     MODE_CHANGE_LETTER (n/a)
 *     MODE_CHANGE -> English or Chinese
 *     SHIFT -> NumberSymbol
 * </pre>
 *     
 */
public class KeyboardSwitch {

  private final Context context;
  private final int chineseKeyboardId;
  private SoftKeyboard numberSymbolKeyboard;
  private SoftKeyboard shiftSymbolKeyboard;
  private SoftKeyboard englishKeyboard;
  private SoftKeyboard chineseKeyboard;
  private SoftKeyboard currentKeyboard;

  private boolean wasEnglishToSymbol;
  private int currentDisplayWidth;

  public KeyboardSwitch(Context context, int chineseKeyboardId) {
    this.context = context;
    this.chineseKeyboardId = chineseKeyboardId;
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
    englishKeyboard = new SoftKeyboard(context, R.xml.qwerty);
    chineseKeyboard = new SoftKeyboard(context, chineseKeyboardId);
    numberSymbolKeyboard = new SoftKeyboard(context, R.xml.symbols);
    shiftSymbolKeyboard = new SoftKeyboard(context, R.xml.symbols_shift);

    if (currentKeyboard == null) {
      // Select English keyboard at the first time the input method is launched.
      toEnglish();
    } else {
      // Preserve the selected keyboard and its shift-status.
      boolean isShifted = currentKeyboard.isShifted();
      if (currentKeyboard.isEnglish()) {
        toEnglish();
      } else if (currentKeyboard.isChinese()) {
        toChinese();
      } else if (currentKeyboard.isNumberSymbol()) {
        toNumberSymbol();
      } else if (currentKeyboard.isShiftSymbol()) {
        toShiftSymbol();
      } else {
        throw new IllegalStateException("The keyboard-mode is invalid.");
      }
      // Restore shift-status.
      currentKeyboard.setShifted(isShifted);
    }
  }

  public Keyboard getCurrentKeyboard() {
    return currentKeyboard;
  }

  /**
   * Switches to the appropriate keyboard based on the type of text being
   * edited, for example, the symbol keyboard for numbers.
   * 
   * @param inputType one of the {@code InputType.TYPE_CLASS_*} values listed in
   *     {@link android.text.InputType}.
   */
  public void onStartInput(int inputType) {
    switch (inputType & InputType.TYPE_MASK_CLASS) {
      case InputType.TYPE_CLASS_NUMBER:
      case InputType.TYPE_CLASS_DATETIME:
      case InputType.TYPE_CLASS_PHONE:
        // Numbers, dates, and phones default to the symbol keyboard, with
        // no extra features.
        toNumberSymbol();
        break;

      case InputType.TYPE_CLASS_TEXT:
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if ((variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            || (variation == InputType.TYPE_TEXT_VARIATION_URI)
            || (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD)
            || (variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
          toEnglish();
        } else {
          // Switch to non-symbol keyboard, either Chinese or English keyboard,
          // for other general text editing.
          toNonSymbols();
        }
        break;

      default:
        // Switch to non-symbol keyboard, either Chinese or English keyboard,
        // for all other input types.
        toNonSymbols();
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
        if (currentKeyboard.isEnglish()) {
          toChinese();
        } else {
          toEnglish();
        }
        return true;

      case Keyboard.KEYCODE_MODE_CHANGE:
        if (currentKeyboard.isSymbols()) {
          toNonSymbols();
        } else {
          toNumberSymbol();
        }
        return true;
        
      case Keyboard.KEYCODE_SHIFT:
        if (currentKeyboard.isNumberSymbol()) {
          toShiftSymbol();
          return true;
        } else if (currentKeyboard.isShiftSymbol()) {
          toNumberSymbol();
          return true;
        }
    }

    // Return false if the key isn't consumed to switch a keyboard.
    return false;
  }

  /**
   * Switches to the number-symbol keyboard and remembers if it was English.
   */
  private void toNumberSymbol() {
    if (!currentKeyboard.isSymbols()) {
      // Remember the current non-symbol keyboard to switch back from symbols.
      wasEnglishToSymbol = currentKeyboard.isEnglish();
    }

    currentKeyboard = numberSymbolKeyboard;
  }
  
  private void toShiftSymbol() {
    currentKeyboard = shiftSymbolKeyboard;
  }

  private void toEnglish() {
    currentKeyboard = englishKeyboard;
  }

  private void toChinese() {
    currentKeyboard = chineseKeyboard;
  }

  /**
   * Switches from symbol (number-symbol or shift-symbol) keyboard,
   * back to the non-symbol (English or Chinese) keyboard.
   */
  private void toNonSymbols() {
    if (currentKeyboard.isSymbols()) {
      if (wasEnglishToSymbol) {
        toEnglish();
      } else {
        toChinese();
      }
    }
  }
}
