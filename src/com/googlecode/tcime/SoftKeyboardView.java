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
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Shows a soft keyboard, rendering keys and detecting key presses.
 */
public class SoftKeyboardView extends KeyboardView {
  
  private static final int FULL_WIDTH_OFFSET = 0xFEE0;

  private SoftKeyboard currentKeyboard;
  private boolean capsLock;
  private boolean cangjieSimplified;

  private static Method invalidateKeyMethod;
  static {
    try {
      invalidateKeyMethod = KeyboardView.class.getMethod(
              "invalidateKey", new Class[] { int.class } );
    } catch (NoSuchMethodException nsme) {
    }
  }

  public static boolean canRedrawKey() {
    return invalidateKeyMethod != null;
  }

  public SoftKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SoftKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  private boolean canCapsLock() {
    // Caps-lock can only be toggled on English keyboard.
    return (currentKeyboard != null) && currentKeyboard.isEnglish();
  }

  public boolean toggleCapsLock() {
    if (canCapsLock()) {
      capsLock = !isShifted();
      setShifted(capsLock);
      return true;
    }
    return false;
  }

  public void updateCursorCaps(int caps) {
    if (canCapsLock()) {
      setShifted(capsLock || (caps != 0));
    }
  }

  private boolean canCangjieSimplified() {
    // Simplified-cangjie can only be toggled on Cangjie keyboard.
    return (currentKeyboard != null) && currentKeyboard.isCangjie();
  }

  public boolean toggleCangjieSimplified() {
    if (canCangjieSimplified()) {
      cangjieSimplified = !isShifted();
      setShifted(cangjieSimplified);
      return true;
    }
    return false;
  }

  public boolean isCangjieSimplified() {
    return cangjieSimplified;
  }

  public boolean hasEscape() {
    return (currentKeyboard != null) && currentKeyboard.hasEscape();
  }

  public void setEscape(boolean escape) {
    if ((currentKeyboard != null) && currentKeyboard.setEscape(escape)) {
      invalidateEscapeKey();
    }
  }

  private void invalidateEscapeKey() {
    // invalidateKey method is only supported since 1.6.
    if (invalidateKeyMethod != null) {
      try {
        invalidateKeyMethod.invoke(this, currentKeyboard.getEscapeKeyIndex());
      } catch (IllegalArgumentException e) {
        Log.e("SoftKeyboardView", "exception: ", e);
      } catch (IllegalAccessException e) {
        Log.e("SoftKeyboardView", "exception: ", e);
      } catch (InvocationTargetException e) {
        Log.e("SoftKeyboardView", "exception: ", e);
      }
    }
  }

  @Override
  public void setKeyboard(Keyboard keyboard) {
    if (keyboard instanceof SoftKeyboard) {
      boolean escape = hasEscape();
      currentKeyboard = (SoftKeyboard) keyboard;
      currentKeyboard.updateStickyKeys();
      currentKeyboard.setEscape(escape);
    }
    super.setKeyboard(keyboard);
  }

  @Override
  protected boolean onLongPress(Key key) {
    // 0xFF01~0xFF5E map to the full-width forms of the characters from
    // 0x21~0x7E. Make the long press as producing corresponding full-width
    // forms for these characters by adding the offset (0xff01 - 0x21).
    if (currentKeyboard != null && currentKeyboard.isSymbols() &&
        key.popupResId == 0 && key.codes[0] >= 0x21 && key.codes[0] <= 0x7E) {
      getOnKeyboardActionListener().onKey(
          key.codes[0] + FULL_WIDTH_OFFSET, null);
      return true;
    } else if (key.codes[0] == SoftKeyboard.KEYCODE_MODE_CHANGE_LETTER) {
      getOnKeyboardActionListener().onKey(SoftKeyboard.KEYCODE_OPTIONS, null);
      return true;
    } else {
      return super.onLongPress(key);
    }
  }

}
