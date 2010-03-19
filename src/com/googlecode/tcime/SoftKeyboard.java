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
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;

/**
 * A soft keyboard definition.
 */
public class SoftKeyboard extends Keyboard {

  public static final int KEYCODE_MODE_CHANGE_LETTER = -200;
  public static final int KEYCODE_OPTIONS = -100;
  private static final int KEYCODE_ENTER = 10;
  private static final String ESCAPE_LABEL = "Esc";
  
  private final int id;
  private Key symbolKey;
  private Key enterKey;
  private Drawable enterIcon;
  private Drawable enterPreviewIcon;
  private int enterKeyIndex;
  private boolean escaped;
  

  public SoftKeyboard(Context context, int xmlLayoutResId) {
    super(context, xmlLayoutResId);
    id = xmlLayoutResId;
  }

  public boolean isEnglish() {
    return id == R.xml.qwerty;
  }

  public boolean isZhuyin() {
    return id == R.xml.zhuyin;
  }

  public boolean isCangjie() {
    return id == R.xml.cangjie;
  }

  public boolean isChinese() {
    return isZhuyin() || isCangjie();
  }

  public boolean isNumberSymbol() {
    return id == R.xml.symbols;
  }
  
  public boolean isShiftSymbol() {
    return id == R.xml.symbols_shift;
  }

  /**
   * Returns {@code true} if the current keyboard is the symbol (number-symbol
   * or shift-symbol) keyboard; otherwise returns {@code false}.
   */
  public boolean isSymbols() {
    return isNumberSymbol() || isShiftSymbol();
  }

  /**
   * Updates the on/off status of sticky keys (symbol-key and shift-key).
   */
  public void updateStickyKeys() {
    if (isSymbols()) {
      // Updates the shift-key status for symbol keyboards: shifted-off for 
      // number-symbol keyboard and shifted-on for shift-symbol keyboard.
      setShifted(isShiftSymbol());
    }

    if (symbolKey != null) {
      symbolKey.on = isSymbols();
    }
  }

  public int getEscapeKeyIndex() {
    // Escape-key is the enter-key set 'Esc'.
    return enterKeyIndex;
  }

  public boolean hasEscape() {
    return escaped;
  }

  /**
   * Sets enter-key as the escape-key.
   *
   * @return {@code true} if the key is changed.
   */
  public boolean setEscape(boolean escapeState) {
    if ((escaped != escapeState) && (enterKey != null)) {
      if (SoftKeyboardView.canRedrawKey()) {
        if (escapeState) {
          enterKey.icon = null;
          enterKey.iconPreview = null;
          enterKey.label = ESCAPE_LABEL;
        } else {
          enterKey.icon = enterIcon;
          enterKey.iconPreview = enterPreviewIcon;
          enterKey.label = null;
        }
      }
      escaped = escapeState;
      return true;
    }
    return false;
  }

  @Override
  protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
      XmlResourceParser parser) {
    Key key = new SoftKey(res, parent, x, y, parser);
    if (key.codes[0] == KEYCODE_MODE_CHANGE) {
      symbolKey = key;
    } else if (key.codes[0] == KEYCODE_ENTER) {
      enterKey = key;
      enterIcon = key.icon;
      enterPreviewIcon = key.iconPreview;
      enterKeyIndex = getKeys().size();
      escaped = false;
    }
    return key;
  }

  /**
   * A soft key definition.
   */
  static class SoftKey extends Keyboard.Key {

    public SoftKey(Resources res, Keyboard.Row parent, int x, int y,
        XmlResourceParser parser) {
      super(res, parent, x, y, parser);
    }

    @Override
    public void onReleased(boolean inside) {
      // Override the default implementation to make the sticky status unchanged
      // since it has been handled by SoftKeyboard and InputView.
      pressed = !pressed;
    }
  }
}
