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

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import android.database.MatrixCursor;
import android.util.Log;

public class Punct {
  private Map<String, Object> mPunct;
  private Map<String, Integer> mPair;

  public Punct(Object o) {
    mPunct = (Map<String,Object>) o;
    mPair = new HashMap<String, Integer>();
  }

  static public CharSequence transform(CharSequence text, boolean up, boolean full) {
    if (text.length() == 0 || !(up || full)) return text;
    StringBuilder sb = new StringBuilder();
    for (int i: text.toString().toCharArray()){
      if (up) i = Character.toUpperCase(i); //大寫
      if (full && i >= 0x20 && i <= 0x7E) i = i - 0x20 + 0xff00; //全角
      sb.append((char)i);
    }
    return sb.toString();
  }

  public MatrixCursor query(CharSequence s, boolean full) {
    MatrixCursor cursor = new MatrixCursor(new String[] {"hz"});
    String k = full ? "full_shape" : "half_shape";
    if (mPunct.containsKey(k)) {
      Map<String,Object> m = (Map<String,Object>)(mPunct.get(k));
      if (m.containsKey(s)) { 
        Object o = m.get(s);
        if (o instanceof String) cursor.addRow(new Object[] {o});
        else if (o instanceof List) {
          for(String i: (List<String>) o) cursor.addRow(new Object[] {i});
        } else if (o instanceof Map) {
          Map<String, Object> mo = (Map<String, Object>) o;
          if (mo.containsKey("commit")) {
            cursor.addRow(new Object[] {(String)mo.get("commit")});
              Log.e("kyle", "k="+k+",commit="+mo.get("commit"));
          } else if (mo.containsKey("pair")) {
            List<String> ls = (List<String>)mo.get("pair");
            int index = 0;
            if (mPair.containsKey(s)) index = 1 - mPair.get(s);
            cursor.addRow(new Object[] {ls.get(index)});
            mPair.put(s.toString(), index);
            Log.e("kyle", "i="+index+",ls="+ls.get(index));
          }
        }
        cursor.moveToFirst();
        return cursor;
      }
    }
    return null;
  }
}
