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

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

/**
 * Extends WordDictionary to provide cangjie word-suggestions.
 */
public class CangjieDictionary extends WordDictionary {

  private static final int APPROX_DICTIONARY_SIZE = 65536;
  private boolean simplified;
  private Collator collator = Collator.getInstance(Locale.TRADITIONAL_CHINESE);

  public CangjieDictionary(Context context) {
    super(context, R.raw.dict_cangjie, APPROX_DICTIONARY_SIZE);
  }

  public void setSimplified(boolean simplified) {
    this.simplified = simplified;
  }

  @Override
  public String getWords(CharSequence input) {
    // Look up the index in the dictionary for the specified input.
    int primaryIndex = CangjieTable.getPrimaryIndex(input);
    if (primaryIndex < 0) {
      return "";
    }

    // [25 * 26] char[] array; each primary entry points to a char[]
    // containing words with the same primary index; then words can be looked up
    // by their secondary index stored at the beginning of each char[].
    // For example, the first primary entry is for '日' code and looks like:
    // char[0][]: { 0, 0, '日', '曰' }
    char[][] dictionary = dictionary();
    char[] data = (dictionary != null) ? dictionary[primaryIndex] : null;
    if (data == null) {
      return "";
    }

    if (simplified) {
      // Sort words of this primary index for simplified-cangjie.
      return sortWords(data);
    }

    int secondaryIndex = CangjieTable.getSecondaryIndex(input);
    if (secondaryIndex < 0) {
      return "";
    }
    // Find words match this secondary index for cangjie.
    return searchWords(secondaryIndex, data);
  }

  private String sortWords(char[] data) {
    int length = data.length / 2;
    String[] keys = new String[length];
    for (int i = 0; i < length; i++) {
      keys[i] = String.valueOf(data[length + i]);
    }
    Arrays.sort(keys, collator);
    char[] sorted = new char[length];
    for (int i = 0; i < length; i++) {
      sorted[i] = keys[i].charAt(0);
    }
    return String.valueOf(sorted);
  }

  private String searchWords(int secondaryIndex, char[] data) {
    int length = data.length / 2;
    int i = binarySearch(data, 0, length, (char) secondaryIndex);
    if (i < 0) {
      return "";
    }
    // There may be more than one words with the same index; look up words with
    // the same secondary index.
    int start = i;
    while (start > 0) {
      if (data[start - 1] != (char) secondaryIndex) {
        break;
      }
      start--;
    }
    int end = i + 1;
    while (end < length) {
      if (data[end] != (char) secondaryIndex) {
        break;
      }
      end++;
    }
    return String.valueOf(data, start + length, end - start);
  }

  /**
   * Binary-searches a range of the specified array for the specified value.
   * @param fromIndex: index of the first element (inclusive) to be searched.
   * @param toIndex: index of the last element (exclusive) to be searched.
   * @return -1 if the value isn't found. 
   * TODO: Remove this once Arrays binarySearch supports search within a range.
   */
  private static int binarySearch(
      char[] array, int fromIndex, int toIndex, char value) {
    int low = fromIndex, mid = -1, high = toIndex - 1;
    while (low <= high) {
      mid = (low + high) >> 1;
      if (value > array[mid]) {
        low = mid + 1;
      } else if (value == array[mid]) {
        return mid;
      } else {
        high = mid - 1;
      }
    }
    return -1;
  }
}
