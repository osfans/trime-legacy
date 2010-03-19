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

/**
 * Extends Editor to compose by zhuyin rules. 
 */
public class ZhuyinEditor extends Editor {

  /**
   * Decomposes the composing-text into four parts: initials, yi-wu-yu-finals,
   * other-finals, and tones, for example, { 'ㄅ', 'ㄨ', 'ㄚ' , 'ˋ' }
   * 
   * @return an array of four characters as
   *     { initials, yi-wu-yu-finals, other-finals, tones }; any element could
   *     be null character if the corresponding part is absent in the input. 
   */
  private char[] decompose() {
    char[] results = new char[] { '\0', '\0', '\0', '\0' };

    String[] pair = ZhuyinTable.stripTones(composingText.toString());
    if (pair != null) {
      // Decompose tones.
      char tone = pair[1].charAt(0);
      if (tone != ZhuyinTable.DEFAULT_TONE) {
        results[3] = tone;
      }

      // Decompose initials.
      String syllables = pair[0];
      if (ZhuyinTable.getInitials(syllables.charAt(0)) > 0) {
        results [0] = syllables.charAt(0);
        syllables = syllables.substring(1);
      }

      // Decompose finals.
      if (syllables.length() > 0) {
        if (ZhuyinTable.isYiWuYuFinals(syllables.charAt(0))) {
          results[1] = syllables.charAt(0);
          if (syllables.length() > 1) {
            results[2] = syllables.charAt(1);
          }
        } else {
          results[2] = syllables.charAt(0);
        }
      }
    }
    return results;
  }

  /**
   * Composes the key-code into the composing-text by zhuyin composing rules.
   */
  @Override
  public boolean doCompose(int keyCode) {
    char c = (char) keyCode;
    if (ZhuyinTable.isTone(c)) {
      if (!hasComposingText()) {
        // Tones are accepted only when there's text in composing.
        return false;
      }
      String[] pair = ZhuyinTable.stripTones(composingText.toString());
      if (pair == null) {
        // Tones cannot be composed if there's no syllables.
        return false;
      }

      // Replace the original tone with the new tone, but the default tone
      // character should not be composed into the composing text.
      char tone = pair[1].charAt(0);
      if (c == ZhuyinTable.DEFAULT_TONE) {
        if (tone != ZhuyinTable.DEFAULT_TONE) {
          composingText.deleteCharAt(composingText.length() - 1);
        }
      } else {
        if (tone == ZhuyinTable.DEFAULT_TONE) {
          composingText.append(c);
        } else {
          composingText.setCharAt(composingText.length() - 1, c);
        }
      }
    } else if (ZhuyinTable.getInitials(c) > 0) {
      // Insert the initial or replace the original initial.
      if ((composingText.length() <= 0)
          || (ZhuyinTable.getInitials(composingText.charAt(0)) == 0)) {
        composingText.insert(0, c);
      } else {
        composingText.setCharAt(0, c);
      }
    } else if (ZhuyinTable.getFinals(String.valueOf(c)) > 0) {
      // Replace the finals in the decomposed of syllables and tones.
      char[] decomposed = decompose();
      if (ZhuyinTable.isYiWuYuFinals(c)) {
        decomposed[1] = c;
      } else {
        decomposed[2] = c;
      }

      // Compose back the text after the finals replacement.
      composingText.setLength(0);
      for (int i = 0; i < decomposed.length; i++) {
        if (decomposed[i] != '\0') {
          composingText.append(decomposed[i]);
        }
      }
    } else {
      return false;
    }
    return true;
  }
}
