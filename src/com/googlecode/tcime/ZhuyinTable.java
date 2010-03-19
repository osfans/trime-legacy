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

class ZhuyinTable {

  // All Chinese characters are mapped into a zhuyin table as described in
  // http://en.wikipedia.org/wiki/Zhuyin_table.
  static final int INITIALS_SIZE = 22;
  static final char DEFAULT_TONE = ' ';

  // Finals that can be appended after 'ㄧ' (yi), 'ㄨ' (wu), or 'ㄩ' (yu).
  private static final char[] yiEndingFinals = new char[]
      { '\u311a', '\u311b', '\u311d', '\u311e', '\u3120', '\u3121', '\u3122',
        '\u3123', '\u3124', '\u3125' };
  private static final char[] wuEndingFinals = new char[]
      { '\u311a', '\u311b', '\u311e', '\u311f', '\u3122', '\u3123', '\u3124',
        '\u3125' };
  private static final char[] yuEndingFinals = new char[]
      { '\u311d', '\u3122', '\u3123', '\u3125' };

  // 'ㄧ' (yi) finals start from position 14 and are followed by 'ㄨ' (wu)
  // finals, and 'ㄩ' (yu) finals follow after 'ㄨ' (wu) finals.
  private static final int YI_FINALS_INDEX = 14;
  private static final int WU_FINALS_INDEX = 25;
  private static final int YU_FINALS_INDEX = 34;

  // 'ㄧ' (yi), 'ㄨ' (wu) , and 'ㄩ' (yu) finals.
  private static final char YI_FINALS = '\u3127';
  private static final char WU_FINALS = '\u3128';
  private static final char YU_FINALS = '\u3129';

  // Default tone and four tone symbols: '˙', 'ˊ', 'ˇ', and 'ˋ'.
  private static final char[] tones = new char[]
      { DEFAULT_TONE, '\u02d9', '\u02ca', '\u02c7', '\u02cb' };

  private ZhuyinTable() {
  }

  /**
   * Returns the row-index in the zhuyin table for the given initials.
   * 
   * @return [0, INITIALS_SIZE - 1] for valid initials index; otherwise -1.
   */
  static int getInitials(char initials) {
    // Calculate the index by its distance to the first initials 'ㄅ' (b).
    int index = initials - '\u3105' + 1;
    if (index >= INITIALS_SIZE) {
      // Syllables starting with finals can still be valid.
      return 0;
    }
    return (index >= 0) ? index : -1;
  }

  /**
   * Returns the column-index in the zhuyin table for the given finals.
   * 
   * @return a negative value for invalid finals.
   */
  static int getFinals(String finals) {
    if (finals.length() == 0) {
     // Syllables ending with no finals can still be valid.
      return 0;
    }
    if (finals.length() > 2) {
      return -1;
    }
  
    // Compute the index instead of direct lookup the whole array to save
    // traversing time. First calculate the distance to the first finals
    // 'ㄚ' (a).
    int index = finals.charAt(0) - '\u311a' + 1;
    if (index < YI_FINALS_INDEX) {
      return index;
    }
  
    // Check 'ㄧ' (yi), 'ㄨ' (wu) , and 'ㄩ' (yu) group finals.
    char[] endingFinals;
    switch (finals.charAt(0)) {
      case YI_FINALS:
        index = YI_FINALS_INDEX;
        endingFinals = yiEndingFinals;
        break;
      case WU_FINALS:
        index = WU_FINALS_INDEX;
        endingFinals = wuEndingFinals;
        break;
      case YU_FINALS:
        index = YU_FINALS_INDEX;
        endingFinals = yuEndingFinals;
        break;
      default:
        return -1;
    }
  
    if (finals.length() == 1) {
      return index;
    }
    for (int i = 0; i < endingFinals.length; i++) {
      if (finals.charAt(1) == endingFinals[i]) {
        return index + i + 1;
      }
    }
    return -1;
  }

  /**
   * Returns the index in the dictionary for the given syllables.
   * 
   * @param syllables should not be null or an empty string.
   * @return a negative value for invalid syllables.
   */
  static int getSyllablesIndex(String syllables) {
    int initials = getInitials(syllables.charAt(0));
    if (initials < 0) {
      return -1;
    }

    // Strip out initials before getting finals column-index.
    int finals = getFinals(
        (initials != 0) ? syllables.substring(1) : syllables);
    if (finals < 0) {
      return -1;
    }

    return (finals * INITIALS_SIZE + initials);
  }

  /**
   * Returns the tone index for the given character.
   */
  static int getTones(char c) {
    for (int i = 0; i < tones.length; i++) {
      if (tones[i] == c) {
        return i;
      }
    }
    // Treat all other characters as the default tone with the index 0.
    return 0;
  }

  /**
   * Returns the count of available tones.
   */
  static int getTonesCount() {
    return tones.length;
  }

  /**
   * Checks if the character is one of the four tone marks.
   */
  static boolean isTone(char c) {
    for (int i = 0; i < tones.length; i++) {
      if (tones[i] == c) {
        return true;
      }
    }
    return false;
  }

  static boolean isYiWuYuFinals(char c) {
    switch (c) {
      case YI_FINALS:
      case WU_FINALS:
      case YU_FINALS:
        return true;
    }
    return false;
  }

  /**
   * Strips the input into two parts: syllables and its tone.
   * 
   * @return the first element as the syllables and second element as the tone;
   *     null if the input couldn't be stripped into two parts.
   */
  static String[] stripTones(String input) {
    final int last = input.length() - 1;
    if (last < 0) {
      return null;
    }

    char tone = input.charAt(last);
    if (isTone(tone)) {
      String syllables = input.substring(0, last);
      if (syllables.length() <= 0) {
        return null;
      }
      return new String[] { syllables, String.valueOf(tone) };
    }
    // Treat the tone-less input as the default tone (tone-0).
    return new String[] { input, String.valueOf(DEFAULT_TONE) };
  }
}
