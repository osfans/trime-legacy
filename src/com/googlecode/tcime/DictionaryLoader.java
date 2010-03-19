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

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Loads a dictionary in a different thread.
 */
public class DictionaryLoader implements Runnable {

  private final InputStream ins;
  private final int size;
  private final CountDownLatch doneSignal;
  private char[][] result;

  /**
   * Loads dictionary data from a given input stream and use a given latch to
   * signal when the loading is complete. Note that the inputstream will be
   * closed after the loading.
   * @param ins the dictionary input stream.
   * @param size the approximate dictionary file size for buffer allocation.
   * @param doneSignal the signal to notify the loading completion.
   */
  public DictionaryLoader(
      InputStream ins, int size, CountDownLatch doneSignal) {
    this.ins = ins;
    this.size = size;
    this.doneSignal = doneSignal;
  }

  public void run() {
    ObjectInputStream oin = null;
    try {
      // The size is set approximate to the file size for better performance.
      BufferedInputStream bis = new BufferedInputStream(ins, size);
      oin = new ObjectInputStream(bis);
      result = (char[][]) oin.readObject();
    } catch (ClassNotFoundException ioe) {
      Log.e("Dictionary", "Couldn't read the dictionary file: ", ioe);
    } catch (IOException ioe) {
      Log.e("Dictionary", "Couldn't read the dictionary file: ", ioe);
    } finally {
      if (oin != null) {
        try {
          oin.close();
        } catch (IOException ioe) {
          Log.e("Dictionary", "Error occurs while closing the reader" + ioe);
        }
      }
    }
    doneSignal.countDown();
  }
  
  /**
   * Returns the dictionary character array loaded by this loader.
   * @return the result; otherwise, null.
   */
  public char[][] result() {
    return result;
  }
}

