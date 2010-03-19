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

import java.util.HashMap;

/**
 * Defines cangjie letters and calculates the index of the given cangjie code.
 */
class CangjieTable {

  // Cangjie 25 letters with number-index starting from 1:
  // 日月金木水火土竹戈十大中一弓人心手口尸廿山女田難卜
  private static final HashMap<Character, Integer> letters =
      new HashMap<Character, Integer>();
  static {
    int i = 1;
    letters.put('\u65e5', i++);
    letters.put('\u6708', i++);
    letters.put('\u91d1', i++);
    letters.put('\u6728', i++);
    letters.put('\u6c34', i++);
    letters.put('\u706b', i++);
    letters.put('\u571f', i++);
    letters.put('\u7af9', i++);
    letters.put('\u6208', i++);
    letters.put('\u5341', i++);
    letters.put('\u5927', i++);
    letters.put('\u4e2d', i++);
    letters.put('\u4e00', i++);
    letters.put('\u5f13', i++);
    letters.put('\u4eba', i++);
    letters.put('\u5fc3', i++);
    letters.put('\u624b', i++);
    letters.put('\u53e3', i++);
    letters.put('\u5c38', i++);
    letters.put('\u5eff', i++);
    letters.put('\u5c71', i++);
    letters.put('\u5973', i++);
    letters.put('\u7530', i++);
    letters.put('\u96e3', i++);
    letters.put('\u535c', i++);
  }

  // Cangjie codes contain at most five letters. A cangjie code can be
  // converted to a numerical code by the number-index of each letter.
  // The absent letter will be indexed as 0 if the cangjie code contains less
  // than five-letters.
  static final int MAX_CODE_LENGTH = 5;
  static final int MAX_SIMPLIFIED_CODE_LENGTH = 2;
  private static final int BASE_NUMBER = letters.size() + 1;

  private CangjieTable() {
  }

  /**
   * Returns {@code true} only if the given character is a valid cangjie letter.
   */
  static boolean isLetter(char c) {
    return letters.containsKey(c);
  }

  /**
   * Returns the primary index calculated by the first and last letter of
   * the given cangjie code.
   *
   * @param code should not be null.
   * @return -1 for invalid code.
   */
  static int getPrimaryIndex(CharSequence code) {
    int length = code.length();
    if ((length < 1) || (length > MAX_CODE_LENGTH)) {
      return -1;
    }
    char c = code.charAt(0);
    if (!isLetter(c)) {
      return -1;
    }
    // The first letter cannot be absent in the code; therefore, the numerical
    // index of the first letter starts from 0 instead.
    int index = (letters.get(c) - 1) * BASE_NUMBER;
    if (length < 2) {
      return index;
    }

    c = code.charAt(length - 1);
    if (!isLetter(c)) {
      return -1;
    }
    return index + letters.get(c);
  }

  /**
   * Returns the secondary index calculated by letters between the first and
   * last letter of the given cangjie code.
   *
   * @param code should not be null.
   * @return -1 for invalid code.
   */
  static int getSecondaryIndex(CharSequence code) {
    int index = 0;
    int last = code.length() - 1;
    for (int i = 1; i < last; i++) {
      char c = code.charAt(i);
      if (!isLetter(c)) {
        return -1;
      }
      index = index * BASE_NUMBER + letters.get(c);
    }
    int maxEnd = MAX_CODE_LENGTH - 1;
    for (int i = last; i < maxEnd; i++) {
      index = index * BASE_NUMBER;
    }
    return index;
  }
}
