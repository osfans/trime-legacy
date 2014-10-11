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

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.database.Cursor;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Window;
import android.view.WindowManager;
import android.content.Intent;

/**
 * Abstract class extended by all Dialect IME.
 */
public class TRIME extends InputMethodService implements 
    KeyboardView.OnKeyboardActionListener, CandidateView.CandidateViewListener {

  protected KeyboardView inputView;
  private CandidatesContainer candidatesContainer;
  private KeyboardSwitch keyboardSwitch;
  private Dictionary dialectDictionary;
  private SoundMotionEffect effect;
  private int orientation;

  protected StringBuilder composingText = new StringBuilder();
  private boolean canCompose;
  private boolean enterAsLineBreak;
  private boolean isLeftApo = true;
  private boolean isLeftQuote = true;

  protected int[] keyboardIds;
  protected int dictionaryId;
  private AlertDialog mOptionsDialog;

  private void initKeyboard() {
    keyboardSwitch = new KeyboardSwitch(this);
    keyboardSwitch.initializeKeyboard(dialectDictionary.getKeyboards());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    dialectDictionary = new Dictionary(this);
    effect = new SoundMotionEffect(this);
    initKeyboard();

    orientation = getResources().getConfiguration().orientation;
    // Use the following line to debug IME service.
    //android.os.Debug.waitForDebugger();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    if (orientation != newConfig.orientation) {
      // Clear composing text and candidates for orientation change.
      escape();
      orientation = newConfig.orientation;
    }
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart,
      int newSelEnd, int candidatesStart, int candidatesEnd) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, 
        candidatesStart, candidatesEnd);
    if ((candidatesEnd != -1) &&
        ((newSelStart != candidatesEnd) || (newSelEnd != candidatesEnd))) {
      // Clear composing text and its candidates for cursor movement.
      escape();
    }
    // Update the caps-lock status for the current cursor position.
    //updateCursorCapsToInputView();
  }

  @Override
  public void onComputeInsets(InputMethodService.Insets outInsets) {
    super.onComputeInsets(outInsets);
    outInsets.contentTopInsets = outInsets.visibleTopInsets;
  }

  @Override
  public View onCreateInputView() {
    inputView = (KeyboardView) getLayoutInflater().inflate(
        R.layout.input, null);
    inputView.setOnKeyboardActionListener(this);
    return inputView;
  }

  @Override
  public View onCreateCandidatesView() {
    candidatesContainer = (CandidatesContainer) getLayoutInflater().inflate(
        R.layout.candidates, null);
    candidatesContainer.setCandidateViewListener(this);
    return candidatesContainer;
  }
    
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        editorstart(attribute.inputType);
        effect.reset();
    }

  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    super.onStartInputView(attribute, restarting);
    bindKeyboardToInputView();
  }

  @Override
  public void onFinishInput() {
    // Clear composing as any active composing text will be finished, same as in
    // onFinishInputView, onFinishCandidatesView, and onUnbindInput.
    clearComposingText();
    super.onFinishInput();
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    if (hasComposingText()) commitText(composingText); //退出時上屏
    super.onFinishInputView(finishingInput);
    // Dismiss any pop-ups when the input-view is being finished and hidden.
    inputView.closing();
  }

  @Override
  public void onFinishCandidatesView(boolean finishingInput) {
    clearComposingText();
    super.onFinishCandidatesView(finishingInput);
  }

  @Override
  public void onUnbindInput() {
    clearComposingText();
    if (dialectDictionary != null && dialectDictionary.isInitChinese()) keyboardSwitch.reset(); //初始中文
    super.onUnbindInput();
  }

  private void bindKeyboardToInputView() {
    if (inputView != null) {
      // Bind the selected keyboard to the input view.
      Keyboard sk = (Keyboard)keyboardSwitch.getCurrentKeyboard();
      int i = dialectDictionary.getKeyTextSize();
      inputView.setTextSize(i);
      inputView.setKeyboard(sk);
      inputView.setPreviewEnabled(dialectDictionary.isKeyboardPreview());
      //updateCursorCapsToInputView();
    }
  }

  /**
   * Resets the internal state of this editor, typically called when a new input
   * session commences.
   */
  private void editorstart(int inputType) {
    canCompose = false;
    enterAsLineBreak = false;

    switch (inputType & InputType.TYPE_MASK_CLASS) {
      case InputType.TYPE_CLASS_TEXT:
        canCompose = true;
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
          // Make enter-key as line-breaks for messaging.
          enterAsLineBreak = true;
        }
        break;
    }
    // Select a keyboard based on the input type of the editing field.
    keyboardSwitch.initializeKeyboard(getMaxWidth());
    keyboardSwitch.onStartInput(inputType);
    setCandidatesViewShown(true);
    escape();
    setCandidatesViewShown(false);
  }
  /**
   * Commits the given text to the editing field.
   */
  private void commitText(CharSequence text) {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
      if (text.length() > 1) {
        // Batch edit a sequence of characters.
        ic.beginBatchEdit();
        ic.commitText(text, 1);
        ic.endBatchEdit();
      } else {
        ic.commitText(text, 1);
      }
      escape();
    }
  }

  private CharSequence getLastText() {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
      return ic.getTextBeforeCursor(1,0);
    }
    return "";
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
        clearComposingText(); //返回鍵清屏
        if ((inputView != null) && inputView.handleBack()) { //按返回鍵關閉輸入窗
            return true;
        }
    }

    if (processKey(event)) return true;
    return super.onKeyDown(keyCode, event);
  }

    private boolean processKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int keyChar = 0;
        CharSequence keyText = "";
        if (KeyEvent.KEYCODE_SPACE == keyCode && event.isShiftPressed()) {
            keyChar = Keyboard.KEYCODE_MODE_NEXT;
            onKey(keyChar, null);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DEL && !hasComposingText()) {
            return false;
        }
        if (!isChinese()) return false;
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            keyChar = keyCode - KeyEvent.KEYCODE_A + 'a';
        } else if (!event.isShiftPressed() && keyCode >= KeyEvent.KEYCODE_0
                && keyCode <= KeyEvent.KEYCODE_9) {
            keyChar = keyCode - KeyEvent.KEYCODE_0 + '0';
        } else if (keyCode == KeyEvent.KEYCODE_COMMA) {
            keyText = event.isShiftPressed() ? "《" : "，";
        } else if (keyCode == KeyEvent.KEYCODE_PERIOD) {
            keyText = event.isShiftPressed() ? "》" : "。";
        } else if (keyCode == KeyEvent.KEYCODE_SEMICOLON) {
            keyText = event.isShiftPressed() ? "：" : "；";
        } else if (keyCode == KeyEvent.KEYCODE_SLASH) {
            keyText = event.isShiftPressed() ? "？" : "、";
        } else if (keyCode == KeyEvent.KEYCODE_BACKSLASH) {
            keyText = "、";
        } else if (keyCode == KeyEvent.KEYCODE_MINUS && event.isShiftPressed()) {
            keyText = "——";
        } else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE && !event.isShiftPressed()) {
            keyText = isLeftApo ? "‘" : "’";
            isLeftApo = !isLeftApo;
        } else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE && event.isShiftPressed()) {
            keyText = isLeftQuote ? "“" : "”";
            isLeftQuote = !isLeftQuote;
        } else if (keyCode == KeyEvent.KEYCODE_1 && event.isShiftPressed()) {
            keyText = "！";
        } else if (keyCode == KeyEvent.KEYCODE_4 && event.isShiftPressed()) {
            keyText =  "￥";
        } else if (keyCode == KeyEvent.KEYCODE_6 && event.isShiftPressed()) {
            keyText = "……";
        } else if (keyCode == KeyEvent.KEYCODE_9 && event.isShiftPressed()) {
            keyText = "（";
        } else if (keyCode == KeyEvent.KEYCODE_0 && event.isShiftPressed()) {
            keyText = "）";
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            keyChar = ' ';
        } else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) {
            keyChar = '\'';
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            keyChar = Keyboard.KEYCODE_DELETE;
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            keyChar = '\n';
        } else if (keyCode == KeyEvent.KEYCODE_GRAVE) {
            keyChar = '`';
        }
        if (keyText.length() > 0) {
            if (candidatesContainer != null) candidatesContainer.pickHighlighted(-1);
            commitText(keyText);
            return true;
        } else if (0 != keyChar) {
            onKey(keyChar, null);
            return true;
        }
        return false;
    }

  public void onKey(int primaryCode, int[] keyCodes) {
    if (keyboardSwitch.onKey(primaryCode)) {
      escape();
      bindKeyboardToInputView();
      return;
    }
    if (handleOption(primaryCode) || handleCapsLock(primaryCode)
        || handleEnter(primaryCode) || handleSpace(primaryCode) || handleSelect(primaryCode)
        || handleDelete(primaryCode) || handleReverse(primaryCode) || handleComposing(primaryCode)) {
      return;
    }
    handleKey(primaryCode);
  }

  public void onText(CharSequence text) {
    if(inputView.isShifted() && text.length() > 0) { //換檔輸出大寫字母
        for (char c: text.toString().toCharArray()){
            c = Character.toUpperCase(c);
            commitText(String.valueOf(c));
        }
        return;
    }
    if (isChinese() && hasComposingText() && text.length() > 0 && dialectDictionary.isDelimiter(text)) {
        if (!composingText.toString().endsWith(dialectDictionary.getDelimiter())) {
            composingText.append(dialectDictionary.getDelimiter());  //手動切分音节
            updateComposingText();
        }
    } else if (isChinese() && ((hasComposingText() && text.length() == 0) || dialectDictionary.isAlphabet(text))) {
        String r = composingText.toString();
        String s = dialectDictionary.correctSpell(r, text);
        if (s == null && !dialectDictionary.hasDelimiter()) {
            if (candidatesContainer != null) candidatesContainer.pickHighlighted(-1); //自動上屏
            s = dialectDictionary.correctSpell("", text);
        }
        if (s != null) {
            composingText.delete(0, composingText.length());
            composingText.append(s);
            Cursor cursor = dialectDictionary.getWord(composingText);
            setCandidates(cursor, true);
            if (dialectDictionary.isAutoSelect(composingText) && cursor != null && cursor.getCount() == 1) {
                if (candidatesContainer != null) candidatesContainer.pickHighlighted(0);
            } else {
                updateComposingText();
            }
        }
    } else {
        if (candidatesContainer != null) candidatesContainer.pickHighlighted(-1);
        commitText(text);
    }
  }

  public void onPress(int primaryCode) {
    effect.vibrate();
    effect.playSound(primaryCode);
  }

  public void onRelease(int primaryCode) {
    // no-op
  }

  public void swipeLeft() {
    // no-op
  }

  public void swipeRight() {
    // no-op
  }

  public void swipeUp() {
    // no-op
  }

  public void swipeDown() {
    requestHideSelf(0);
  }

  public void onPickCandidate(String candidate) {
    // Commit the picked candidate and suggest its following words.
    String[] s = candidate.split("\t", 2);
    String sc = dialectDictionary.toSC(s[0]);
    String py = s[1].length() > 0 ? s[1] : composingText.toString();
    commitText(composingText.length() > 0 && dialectDictionary.isCommitPy() ? String.format("%s(%s)", sc, py) : sc);
    setCandidates(dialectDictionary.getAssociation(s[0]), false);
  }

  public boolean hasComposingText() {
    return composingText.length() > 0;
  }

  private void updateComposingText() {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
      // Set cursor position 1 to advance the cursor to the text end.
      ic.setComposingText(composingText, 1);
    }
  }

  public void clearComposingText() {
    if (hasComposingText()) {
      // Clear composing only when there's composing-text to avoid the selected
      // text being cleared unexpectedly.
      composingText.setLength(0);
      updateComposingText();
    }
  }
    private void setSchema() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this)
            .setTitle(dialectDictionary.getSchemaTitle())
            .setMultiChoiceItems(dialectDictionary.namedFuzzyRules, dialectDictionary.fuzzyRulesPref,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface di, int which, boolean isChecked) {
                        //di.dismiss();
                        dialectDictionary.setFuzzyRule(which, isChecked);
                    }
                });
        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (candidatesContainer != null) lp.token = candidatesContainer.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
    }

  private void setCandidates(Cursor cursor, boolean highlightDefault) {
    if (candidatesContainer != null) {
      candidatesContainer.setCandidates(cursor, highlightDefault, dialectDictionary);
      setCandidatesViewShown(canCompose && isChinese());
    }
  }

  private boolean handleOption(int keyCode) {
    if (keyCode == Keyboard.KEYCODE_OPTIONS || keyCode == Keyboard.KEYCODE_SCHEMA_OPTIONS) {
        // Create a Dialog menu
        AlertDialog.Builder builder;
        if (keyCode == Keyboard.KEYCODE_OPTIONS) {
            builder = new AlertDialog.Builder(this)
            .setTitle(R.string.ime_name)
            //.setIcon(android.R.drawable.ic_menu_preferences)
            .setCancelable(true)
            .setNegativeButton(R.string.other_ime, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int id) {
                    di.dismiss();
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
                }
            })
            .setPositiveButton(R.string.set_ime, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int id) {
                    di.dismiss();
                    Intent iSetting = new Intent();
                    iSetting.setClass(TRIME.this, ImePreferenceActivity.class);
                    iSetting.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(iSetting);
                    escape(); //全局設置時清屏
                }
            })
            .setSingleChoiceItems(dialectDictionary.getSchemas(), dialectDictionary.getSchemaId(), "name",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int id) {
                    di.dismiss();
                    if (dialectDictionary.setSchemaId(id)) {
                        initKeyboard();
                        bindKeyboardToInputView();
                    }
                }
            });
            if (dialectDictionary.namedFuzzyRules != null) builder.setNeutralButton(R.string.set_schema, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int id) {
                    di.dismiss();
                    setSchema();
                }
            });
        } else {
            builder = new AlertDialog.Builder(this)
            .setTitle(dialectDictionary.getSchemaTitle())
            .setPositiveButton(R.string.close, null)
            .setItems(dialectDictionary.getSchemaInfo(), null);
        }
        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (candidatesContainer != null) lp.token = candidatesContainer.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
        return true;
    }
    return false;
  }

  private boolean handleCapsLock(int keyCode) {
    return (keyCode == Keyboard.KEYCODE_SHIFT) && inputView.setShifted(!inputView.isShifted());
  }

  private boolean handleEnter(int keyCode) {
    if (keyCode == '\n') {
      if (hasComposingText()) {
        commitText(composingText);
       } else if (enterAsLineBreak) {
        commitText("\n");
      } else {
        sendKeyChar('\n');
      }
      return true;
    }
    return false;
  }

  private boolean handleSpace(int keyCode) {
    if (candidatesContainer != null && keyCode == ' ' && !dialectDictionary.isDelimiter(" ")) {
      if (!candidatesContainer.pickHighlighted(-1)) {
        if (hasComposingText()) clearComposingText();
        else commitText(" ");
      }
      return true;
    }
    return false;
  }

  private boolean handleSelect(int keyCode) {
    if (candidatesContainer != null && keyCode >= '1' && keyCode <= '9' && !dialectDictionary.isAlphabet(String.valueOf((char)keyCode))) {
      return candidatesContainer.pickHighlighted(keyCode - '1');
    }
    return false;
  }

  private boolean handleDelete(int keyCode) {
    // Handle delete-key only when no composing text. 
    if ((keyCode == Keyboard.KEYCODE_DELETE)) {
        if (composingText.length() == 1) {
            escape();
        } else if (hasComposingText()) {
            composingText.deleteCharAt(composingText.length() - 1);
            onText("");
        } else {
            escape();
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        }
      return true;
    }
    return false;
  }

  private boolean handleReverse(int keyCode) {
    if (keyCode == Keyboard.KEYCODE_REVERSE && !hasComposingText()) {
      CharSequence s = getLastText();
      if (s.length() == 0) return true;
      //setCandidates(dialectDictionary.getPy(s), false);
      String[] pys = dialectDictionary.getPy(s);
      if (pys != null) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this)
            .setTitle(String.format(getString(R.string.pronunciation), s))
            .setItems(pys, 
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int id) {
                    String py = (String)((AlertDialog)di).getListView().getAdapter().getItem(id);
                    commitText(String.format("(%s)",py));
                    di.dismiss();
                }
            });
        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (candidatesContainer != null) lp.token = candidatesContainer.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
      }
      return true;
    }
    return false;
  }

    private boolean isChinese() {
        return keyboardSwitch.isChinese();
    }

  private boolean handleComposing(int keyCode) {
    if(canCompose && isChinese()) {
        String s = String.valueOf((char) keyCode);
        if (dialectDictionary.isAlphabet(s) || dialectDictionary.isDelimiter(s)) {
            onText(s);
            return true;
        } else {
            if (candidatesContainer != null) candidatesContainer.pickHighlighted(-1);
        }
    }
    return false;
  }

  /**
   * Handles input of SoftKeybaord key code that has not been consumed by
   * other handling-methods.
   */
  private void handleKey(int keyCode) {
    if (isInputViewShown() && inputView.isShifted()) {
      keyCode = Character.toUpperCase(keyCode);
    }
    commitText(String.valueOf((char) keyCode));
  }

  /**
   * Simulates PC Esc-key function by clearing all composing-text or candidates.
   */
  private void escape() {
    clearComposingText();
    setCandidates(null, false);
  }
}
