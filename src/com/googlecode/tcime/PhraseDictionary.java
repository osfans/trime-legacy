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
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Reads a phrase dictionary and provides following-word suggestions as a list
 * of characters for the given character.
 */
public class PhraseDictionary {

  private static final int APPROX_DICTIONARY_SIZE = 131072;

  private final CountDownLatch loading = new CountDownLatch(1);
  private final DictionaryLoader loader;

  public PhraseDictionary(Context context) {
    loader = new DictionaryLoader(
        context.getResources().openRawResource(R.raw.dict_phrases),
        APPROX_DICTIONARY_SIZE, loading);
    new Thread(loader).start();
  }

  /**
   * Returns a string containing the following-word suggestions of phrases for
   * the given word.
   * 
   * @param c the current word to look for its following words of phrases.
   * @return a concatenated string of characters, or an empty string if there
   *     is no following-word suggestions for that word.
   */
  public String getFollowingWords(char c) {
    try {
      loading.await();
    } catch (InterruptedException e) {
      Log.e("PhraseDictionary", "Loading is interrupted: ", e);
    }

    // Phrases are stored in an array consisting of three character arrays. 
    // char[0][] contains a char[] of words to look for phrases.
    // char[2][] contains a char[] of following words for char[0][].
    // char[1][] contains offsets of char[0][] words to map its following words. 
    // For example, there are 5 phrases: Aa, Aa', Bb, Bb', Cc.
    // char[0][] { A, B, C }
    // char[1][] { 0, 2, 4 }
    // char[2][] { a, a', b, b', c}
    char[][] dictionary = loader.result();
    if (dictionary == null || dictionary.length != 3) {
      return "";
    }

    int index = Arrays.binarySearch(dictionary[0], c);
    if (index >= 0) {
      int offset = dictionary[1][index];
      int count = (index < dictionary[1].length - 1) ?
          (dictionary[1][index + 1] - offset) : (dictionary[2].length - offset);
      return String.valueOf(dictionary[2], offset, count);
    }
    return "";
  }
}
