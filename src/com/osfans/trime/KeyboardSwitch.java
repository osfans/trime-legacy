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

import java.util.List;

import android.content.Context;
import android.text.InputType;

public class KeyboardSwitch {

  private final Context context;

  private Keyboard[] keyboards;
  private Keyboard currentKeyboard;
  private int currentId, lastId;
  private List<Object> keys;

  private int currentDisplayWidth;

  public KeyboardSwitch(Context context) {
    this.context = context;
    currentId = 0;
    lastId = 0;
  }

  public void reset(){
    currentId = 0;
    currentKeyboard = keyboards[currentId];
  }

  public void init(Object o) {
   
    keys = (List<Object>)o;    
    int n = keys.size();
    keyboards = new Keyboard[n];
    for (int i = 0; i < n; i++ ) {
        keyboards[i] = new Keyboard(context, keys.get(i));
    }

    reset();
  }

  /**
   * Recreates the keyboards if the display-width has been changed.
   * 
   * @param displayWidth the display-width for keyboards.
   */
  public void init(int displayWidth) {
    if ((currentKeyboard != null) && (displayWidth == currentDisplayWidth)) {
      return;
    }

    currentDisplayWidth = displayWidth;
    init(keys);
  }

  public Keyboard getCurrentKeyboard() {
    return currentKeyboard;
  }

  public boolean isChinese() {
    return currentKeyboard != null && currentKeyboard.isChinese();
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
            || (variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) || !isChinese()) {
          currentId = 1;
          lastId = 1;
          currentKeyboard = keyboards[currentId];
          currentKeyboard.setShifted(currentKeyboard.isShifted());
        } else reset();
     }
  }

  /**
   * Consumes the pressed key-code and switch keyboard if applicable.
   * 
   * @return {@code true} if the keyboard is switched; otherwise {@code false}.
   */
  public boolean onKey(int keyCode) {
    if (keyCode <= Keyboard.KEYCODE_MODE_SWITCH) {
      lastId = currentId;
      currentId = Keyboard.KEYCODE_MODE_SWITCH - keyCode;
      currentKeyboard = keyboards[currentId];
      return true;
    } else if (keyCode == Keyboard.KEYCODE_MODE_NEXT) {
      lastId = currentId;
      currentId++;
      if(currentId>=keyboards.length) currentId = 0;
      currentKeyboard = keyboards[currentId];
      return true;
    } else if (keyCode == Keyboard.KEYCODE_MODE_PREV) {
      lastId = currentId;
      currentId--;
      if(currentId<0) currentId = keyboards.length - 1;
      currentKeyboard = keyboards[currentId];
      return true;
    } else if (keyCode == Keyboard.KEYCODE_MODE_LAST) {
      int tmp = currentId;
      currentId = lastId;
      lastId = tmp;
      currentKeyboard = keyboards[currentId];
      return true; 
    }
    // Return false if the key isn't consumed to switch a keyboard.
    return false;
  }
}
