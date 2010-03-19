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

/**
 * Extends WordDictionary to provide zhuyin word-suggestions.
 */
public class ZhuyinDictionary extends WordDictionary {

  private static final int APPROX_DICTIONARY_SIZE = 65536;
  private static final int TONES_COUNT = ZhuyinTable.getTonesCount();

  public ZhuyinDictionary(Context context) {
    super(context, R.raw.dict_zhuyin, APPROX_DICTIONARY_SIZE);
  }

  @Override
  public String getWords(CharSequence input) {
    // Look up the syllables index; return empty string for invalid syllables.
    String[] pair = ZhuyinTable.stripTones(input.toString());
    int syllablesIndex = (pair != null) ?
        ZhuyinTable.getSyllablesIndex(pair[0]) : -1;
    if (syllablesIndex < 0) {
      return "";
    }

    // [22-initials * 39-finals] syllables array; each syllables entry points to
    // a char[] containing words for that syllables.
    char[][] dictionary = dictionary();
    char[] data = (dictionary != null) ? dictionary[syllablesIndex] : null;
    if (data == null) {
      return "";
    }

    // Counts of words for each tone are stored in the array beginning.
    int tone = ZhuyinTable.getTones(pair[1].charAt(0));
    int length = (int) data[tone];
    if (length == 0) {
      return "";
    }

    int start = TONES_COUNT;
    for (int i = 0;  i < tone; i++) {
      start += (int) data[i];
    }

    return String.copyValueOf(data, start, length);
  }
}
