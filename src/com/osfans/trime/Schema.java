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
import java.util.List;
import java.util.regex.*;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

public class Schema {
  private Map<String,Object> mSchema, mDefaultSchema;
  private Pattern rule_sep = Pattern.compile("\\W");

  public Schema(InputStream o) {
    mDefaultSchema = (Map<String,Object>)new Yaml().load(o);
  }

  public void load(String s) {
    mSchema = (Map<String,Object>)new Yaml().load(s);
  }

  private Object _getValue(String k1) {
    if (mSchema.containsKey(k1)) return mSchema.get(k1);
    if (mDefaultSchema.containsKey(k1)) return mDefaultSchema.get(k1);
    return null;
  }

  private Object _getValue(String k1, String k2) {
    Map<String, Object> m;
    if (mSchema.containsKey(k1)) {
      m = (Map<String, Object>)mSchema.get(k1);
      if (m != null && m.containsKey(k2)) return m.get(k2);
    }
    if (mDefaultSchema.containsKey(k1)) {
      m = (Map<String, Object>)mDefaultSchema.get(k1);
      if (m != null && m.containsKey(k2)) return m.get(k2);
    }
    return null;
  }

  public Object getValue(String s) {
    String[] ss = s.split("/");
    if (ss.length == 1) return _getValue(ss[0]);
    else if(ss.length == 2) return _getValue(ss[0], ss[1]);
    return null;
  }

  private Object getDefaultValue(String s, Object o) {
    Object ret = getValue(s);
    return (ret != null) ? ret : o;
  }

  public boolean getBool(String s) {
    Object v = getValue(s);
    if (v == null) return false;
    return (Boolean) v;
  }

  public int getInt(String s) {
    Object v = getValue(s);
    if (v == null) return 0;
    return (Integer) v;
  }

  public String getString(String s) {
    Object v = getValue(s);
    if (v == null) return null;
    return (String) v;
  }

  public Pattern getPattern(String s) {
    Object v = getValue(s);
    if (v == null) return null;
    return Pattern.compile((String)v);
  }

  public String[][] getRule(String s) {
    List<String> rule = (List<String>)getValue(s);
    if (rule != null && rule.size() > 0) {
      int n = rule.size();
      String[][] rules = new String[n][4];
      for(int i = 0; i < n; i++) {
        String r = rule.get(i);
        Matcher m = rule_sep.matcher(r);
        if (m.find()) {
          rules[i] = r.split(m.group(), 4);
        }
      }
      return rules;
    }
    return null;
  }

  public String getTitle() {
    StringBuilder sb = new StringBuilder();
    for(String i: new String[]{"schema/name", "schema/version"}) {
      sb.append(getDefaultValue(i, "") + " ");
    }
    return sb.toString();
  }

  public String[] getInfo() {
    StringBuilder sb = new StringBuilder();
    for(String i: new String[]{"schema/author", "schema/description"}) {
      sb.append(getDefaultValue(i, "") + "\n");
    }
    return sb.toString().replace("\n\n", "\n").split("\n");
  }

  public Object getKeyboards() {
    return getValue("trime/keyboard");
  }

  public boolean hasPrism() {
    return getValue("speller/algebra") != null;
  }

  public Pattern getReversePattern() {
    Object o = getValue("recognizer/patterns");
    if (o == null) return null;
    return Pattern.compile(((Map<String,String>)o).get("reverse_lookup"));
  }
}
