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

/**
 * Cangjie input method.
 */
public class CangjieIME extends AbstractIME {
  private CangjieEditor cangjieEditor;
  private CangjieDictionary cangjieDictionary;

  @Override
  protected KeyboardSwitch createKeyboardSwitch(Context context) {
    return new KeyboardSwitch(context, R.xml.cangjie);
  }

  @Override
  protected Editor createEditor() {
    cangjieEditor =  new CangjieEditor();
    return cangjieEditor;
  }

  @Override
  protected WordDictionary createWordDictionary(Context context) {
    cangjieDictionary = new CangjieDictionary(context);
    return cangjieDictionary;
  }

  @Override
  public void onKey(int primaryCode, int[] keyCodes) {
    if (handleCangjieSimplified(primaryCode)) {
      return;
    }
    super.onKey(primaryCode, keyCodes);
  }

  private boolean handleCangjieSimplified(int keyCode) {
    if (keyCode == Keyboard.KEYCODE_SHIFT) {
      if ((inputView != null) && inputView.toggleCangjieSimplified()) {
        boolean simplified = inputView.isCangjieSimplified();
        cangjieEditor.setSimplified(simplified);
        cangjieDictionary.setSimplified(simplified);
        escape();
        return true;
      }
    }
    return false;
  }
}
