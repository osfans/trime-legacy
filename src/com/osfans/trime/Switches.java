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

public class Switches {
  private List<Map<String,Object>> mSwitches;
  private Map<String, Boolean> mStatus;

  public Switches(Object o) {
    mSwitches = (List<Map<String,Object>>) o;
    initStatus();
  }

  private void initStatus() {
    mStatus = new HashMap<String,Boolean>();
    for (Map<String, Object> m: mSwitches) {
      mStatus.put((String)m.get("name"), false);
    }
  }

  public boolean getStatus(String k) {
    return mStatus.containsKey(k) ? mStatus.get(k) : false;
  }

  public boolean setStatus(String k, boolean v) {
    mStatus.put(k, v);
    return v;
  }

  public MatrixCursor query() {
    MatrixCursor menuCursor;
    if (mSwitches == null) return null;
    menuCursor = new MatrixCursor(new String[] {"hz", "switch"});
    for (Map<String, Object> m: mSwitches) {
      String k = (String)m.get("name");
      boolean v = mStatus.containsKey(k) ? mStatus.get(k) : false;
      menuCursor.addRow(new Object[] {((List<String>)(m.get("states"))).get(v ? 1 : 0), k});
    }
    menuCursor.moveToFirst();
    return menuCursor;
  }
}
