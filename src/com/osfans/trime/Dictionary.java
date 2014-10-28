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

import android.content.Context;
//import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.regex.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.io.IOException;

import org.yaml.snakeyaml.Yaml;

/**
 * Reads a word-dictionary and provides word-suggestions as a list of characters
 * for the specified input.
 */
public class Dictionary {

  private SQLiteDatabase mDatabase;
  private DictionaryHelper mHelper;
  private final SharedPreferences preferences;

  private Map<String,Object> mSchema, mDefaultSchema;
  private Object keyboard;
  private String table;
  private String delimiter, alphabet, initials;

  private Pattern syllableP, autoSelectSyllableP;
  private String[][] preeditRule, spellRule, lookupRule, commentRule, fuzzyRule;
  private String[]  namedFuzzyRules;
  private boolean[] fuzzyRulesPref;

  protected Dictionary(Context context) {
    preferences = PreferenceManager.getDefaultSharedPreferences(context);
    initDefaultSchema(context);
    mHelper = new DictionaryHelper(context);
  }

  public void init(Context context) {
    mDatabase = mHelper.getReadableDatabase();
    initSchema();
  }

  public DictionaryHelper getHelper() {
    return mHelper;
  }

  private void initDefaultSchema(Context context) {
    try {
      mDefaultSchema = (Map<String,Object>)new Yaml().load(context.getAssets().open("default.yaml"));
    } catch (IOException e) {
      throw new RuntimeException("Error load default.yaml", e);
    }
  }

  public boolean isAlphabet(CharSequence cs, boolean hasComposingText) {
    if (!hasComposingText && initials != null && cs.length() == 1 && !initials.contains(cs)) return false;
    String[] ss = cs.toString().split("");
    for(String s: ss) if(!alphabet.contains(s)) return false;
    return true;
  }

  private boolean isSyllable(String s) {
    if (syllableP == null) return true;
    if (!hasDelimiter()) return syllableP.matcher(s).matches();
    String[] ss = s.split(getDelimiter());
    for (String i: ss) if(!syllableP.matcher(i).matches()) return false;
    return true;
  }

  public boolean isAutoSelect(CharSequence s) {
    return (autoSelectSyllableP != null) && autoSelectSyllableP.matcher(s).matches();
  }

  public String correctSpell(String r, CharSequence text) {
    String s = translate(r + text, spellRule);
    if (isSyllable(s)) return s;
    if (hasDelimiter()) {
      s = translate(r + getDelimiter() + text, spellRule);
      if (isSyllable(s)) return s;
    }
    return null;
  }

  private String fuzzyText(String s) {
    if (fuzzyRule == null) return s;
    int n = fuzzyRule.length;
    if (n == 0) return s;
    StringBuilder r = new StringBuilder(s);
    ArrayList<Integer> b = new ArrayList<Integer>();
    ArrayList<Integer> bn = new ArrayList<Integer>();
    ArrayList<String> fuzzyList =  new ArrayList<String>();
    if (fuzzyRulesPref != null) {
      for (int i = 0; i < fuzzyRulesPref.length; i++) {
        if (fuzzyRulesPref[i]) fuzzyList.add(namedFuzzyRules[i]);
      }
    }
    for (int j=0; j<n; j++){
      String[] rule = fuzzyRule[j];
      if (rule[0].length() == 0 || fuzzyList.contains(rule[0])) {
        Matcher m = Pattern.compile(rule[1]).matcher(s);
        while(m.find()) {
          b.add(j);
          bn.add(m.start());
        }
      }
    }
    int cnt = b.size();
    if (cnt == 0) return s;
    String p = s;
    for (int i = 1;  i < (1 << cnt); i++) {
      p = s;
      for (int j = 0; j < cnt; j++) {
        int bj = b.get(j);
        int bnj = bn.get(j);
        String[] rule = fuzzyRule[bj];
        if ((i & (1 << j)) != 0) {
          StringBuffer sb = new StringBuffer(p.length());
          Matcher m = Pattern.compile(rule[1]).matcher(p);
          if (m.find(bnj)) m.appendReplacement(sb, rule[2]);
          m.appendTail(sb);
          m.reset();
          p = sb.toString();
        }
      }
      r.append(" OR ");
      r.append(p);
    }
    return r.toString();
  }

  private String translate(String s, String[][] rules) {
    if (rules == null) return s;
    for (String[] rule:  rules) {
      if (rule[0].contentEquals("xlit")) {
        String[] rulea = rule[1].split(rule[1].contains("|") ? "\\|" : "");
        String[] ruleb = rule[2].split(rule[2].contains("|") ? "\\|" : "");
        int n = rulea.length;
        if (n == ruleb.length) {
          for (int i = 0; i < n; i++) if (rulea[i].length() > 0) s = s.replace(rulea[i], ruleb[i]);
        }
      } else s = s.replaceAll(rule[1],rule[2]);
    }
    return s;
  }

  private String[][] getRule(String k1, String k2) {
    List<String> rule = (List<String>)getValue(k1, k2);
    if (rule!=null && rule.size() > 0) {
      int n = rule.size();
      String[][] rules = new String[n][4];
      for(int i = 0; i < n; i++) {
        String s = rule.get(i);
        rules[i] = s.split(s.contains(" ") ? " " : "(?<!\\\\)/", 4);
        for(int j = 0; j < rules[i].length; j++)
          rules[i][j] = rules[i][j].replace("\\/","/");
      }
      return rules;
    }
    return null;
  }

  public void setFuzzyRule( int which, boolean isChecked) {
    fuzzyRulesPref[which] = isChecked;
    StringBuilder s = new StringBuilder();
    for(boolean b: fuzzyRulesPref) s.append( b ? "1" : "0");

    SharedPreferences.Editor edit = preferences.edit();
    edit.putString(String.format("fuzzy%d", getSchemaId()), s.toString());
    edit.commit();
  }

  private void initNamedFuzzyRule() {
    ArrayList<String> fuzzyList = new ArrayList<String>();
    if (fuzzyRule != null) {
      for(String[] i: fuzzyRule) {
        if(i[0].length() > 0) {
          if (!fuzzyList.contains(i[0])) fuzzyList.add(i[0]);
        }
      }
    }
    if (fuzzyList.size()>0) {
      fuzzyRulesPref =  new boolean[fuzzyList.size()];
      namedFuzzyRules = new String[fuzzyList.size()];
      fuzzyList.toArray(namedFuzzyRules);
      String s = preferences.getString(String.format("fuzzy%d", getSchemaId()), "");
      if (s.length() > 0) {
        for(int i = 0; i < s.length(); i++) fuzzyRulesPref[i] = (s.charAt(i) == '1');
      }
    } else {
      namedFuzzyRules = null;
      fuzzyRulesPref = null;
    }
  }

  public String[] getNamedFuzzyRules() {
    return namedFuzzyRules;
  }

  public boolean[] getFuzzyRulesPref() {
    return fuzzyRulesPref;
  }

  private Object getValue(String k1) {
    if (mSchema.containsKey(k1)) return mSchema.get(k1);
    if (mDefaultSchema.containsKey(k1)) return mDefaultSchema.get(k1);
    return null;
  }

  private Object getValue(String k1, String k2) {
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

  private Object getDefaultValue(String k1, String k2, Object o) {
    Object ret = getValue(k1, k2);
    return (ret != null) ? ret : o;
  }

  private void initSchema() {
    int id = getSchemaId();
    Cursor cursor = query(String.format("select * from schema where _id = %d", id), null);
    if (cursor == null) return;
    mSchema = (Map<String,Object>)new Yaml().load(cursor.getString(cursor.getColumnIndex("full")));
    cursor.close();

    delimiter = (String)getValue("speller", "delimiter");
    alphabet = (String)getValue("speller", "alphabet");
    initials = (String)getValue("speller", "initials");
    preeditRule = getRule("translator", "preedit_format");
    commentRule = getRule("translator", "comment_format");
    table = (String)getValue("translator", "dictionary");

    String a = (String)getValue("trime", "syllable");
    syllableP = (a!=null) ? Pattern.compile(a) : null;
    a = (String) getValue("trime", "auto_select_syllable");
    autoSelectSyllableP = (a!=null) ? Pattern.compile(a) : null;
    spellRule = getRule("trime", "spell");
    lookupRule = getRule("trime", "lookup");
    fuzzyRule = getRule("trime", "fuzzy");
    keyboard = (Object)getValue("trime", "keyboard");
    initNamedFuzzyRule();
  }

  public Object getKeyboards() {
    return keyboard;
  }

  public String getSchemaTitle() {
    StringBuilder sb = new StringBuilder();
    for(String i: new String[]{"name", "version"}) {
        sb.append(getDefaultValue("schema", i, "") + " ");
    }
    return sb.toString();
  }

  public String[] getSchemaInfo() {
    StringBuilder sb = new StringBuilder();
    for(String i: new String[]{"author", "description"}) {
      sb.append(getDefaultValue("schema", i, "") + "\n");
    }
    return sb.toString().replace("\n\n", "\n").split("\n");
  }

  public Cursor getSchemas() {
    return query("select * from schema", null);
  }

  public String preedit(String s) {
    return translate(s, preeditRule);
  }

  public String comment(String s) {
    return translate(s, commentRule);
  }

  public String[] getComment(CharSequence code) {
    String sql = String.format("select py from %s where hz match ?", table);
    Cursor cursor = query(sql, new String[]{code.toString()});
    if (cursor == null) return null;
    int n = cursor.getCount();
    int i = 0;
    String[] s = new String[n];
    do {
        s[i++] = comment(cursor.getString(0));
    } while (cursor.moveToNext());
    cursor.close();
    return s;
  }

  /**
   * Returns a string containing words as suggestions for the specified input.
   * 
   * @param input should not be null.
   * @return a concatenated string of characters, or an empty string if there
   *     is no word for that input.
   */
  public Cursor getWord(CharSequence code) {
    String s = code.toString();
    s = translate(s, lookupRule);
    if (fuzzyRule != null) s = fuzzyText(s);

    boolean fullPyOn = isFullPy() && s.length() < 3;
    if (hasDelimiter() && s.contains(getDelimiter())) return getPhrase(s.replace(getDelimiter(), "'"));

    Cursor cursor = null;
    String sql;
    //Log.e("kyle", "word start");
    sql = String.format("select %s from %s where py match ? and not glob('* *', py) %s", getQueryCol(), table, getSingle());
    cursor = query(sql, new String[]{s});
    if (cursor == null && !fullPyOn) {
      s = s.replace(" OR", "* OR") + "*";
      cursor = query(sql + " limit 100", new String[]{s});
    }
    //Log.e("kyle", "word end");
    return cursor;
  }

  private Cursor getPhrase(CharSequence code) {
    boolean fullPyOn = isFullPy() && code.length() < 6;
    String sql = String.format("select %s from %s where py match ? limit 100", getQueryCol(), table);
    String s = String.format("\"^%s\"",code.toString().replace(" OR ", "\" OR \"^").replace("'", " "));
    //Log.e("kyle", "phrase start");
    Cursor cursor = query(sql, new String[]{s});
    if (cursor != null || fullPyOn) return cursor;
    s = String.format("\"^%s*\"",code.toString().replace(" OR ", "*\" OR \"^").replace("'", " "));
    cursor = query(sql, new String[]{s});
    if (cursor != null) return cursor;
    s = String.format("\"^%s*\"",code.toString().replace(" OR ", "*\" OR \"^").replace("'", "* "));
    cursor = query(sql, new String[]{s});
    //Log.e("kyle", "phrase end");
    return cursor;
  }

  public Cursor getAssociation(CharSequence code) {
    if (!isAssociation()) return null;
    String s = code.toString();
    int len = s.length();
    String sqlFormat = "select distinct substr(hz,%d) from %s where hz match '^%s*' and length(hz) > %d limit 100";
    return query(String.format(sqlFormat, len + 1, table, s, len), null);
  }
  
  /**
   * Performs a database query.
   * @param selection The selection clause
   * @param selectionArgs Selection arguments for "?" components in the selection
   * @param columns The columns to return
   * @return A Cursor over all rows matching the query
   */
  private Cursor query(String sql, String[] selectionArgs) {
    /* The SQLiteBuilder provides a map for all possible columns requested to
     * actual columns in the database, creating a simple column alias mechanism
     * by which the ContentProvider does not need to know the real column names
     */
    Cursor cursor = mDatabase.rawQuery(sql, selectionArgs);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
  }

  public String toSC(String text) {
    if (!isSC()) return text;
    Cursor cursor =  query("select s from opencc where t match ?", new String[]{text});
    if (cursor != null) {
      String s = cursor.getString(0).split(" ")[0];
      cursor.close();
      return s;
    }
    StringBuilder s = new StringBuilder();
    for (String i: text.split("\\B")) {
      cursor = query("select s from opencc where t match ?", new String[]{i});
      if (cursor == null) s.append(i);
      else {
        s.append(cursor.getString(0).split(" ")[0]);
        cursor.close();
      }
    }
    return s.toString();
  }

  public boolean isCommitPy() {
    return preferences.getBoolean("pref_commit_py", false);
  }

  public boolean isSC() {
    return preferences.getBoolean("pref_sc", false);
  }

  private boolean isFullPy() {
    return preferences.getBoolean("pref_full_py", false);
  }

  public boolean hasDelimiter() {
      return (delimiter != null) && delimiter.length() > 0;
  }

  public boolean isDelimiter(CharSequence s) {
      return hasDelimiter() && s.length() > 0 && s.charAt(0) != ' ' && delimiter.contains(s);
  }

  public String getDelimiter() {
    return hasDelimiter() ? delimiter.substring(0, 1) : "";
  }

  public boolean isKeyboardPreview() {
      return preferences.getBoolean("pref_keyboard_preview", true);
  }

  private String getSingle() {
    return preferences.getBoolean("pref_single", false) ? " and length(hz) == 1" : "";
  }

  private boolean isAssociation() {
    return preferences.getBoolean("pref_association", false);
  }

  private String getQueryCol() {
    return preferences.getBoolean("pref_py_prompt", false) ? "hz,py" : "hz";
  }

  public boolean isInitChinese() {
    return preferences.getBoolean("pref_init_chinese", false);
  }

  public int getSchemaId() {
    return preferences.getInt("_id", 0);
  }

  public boolean setSchemaId(int id) {
    SharedPreferences.Editor edit = preferences.edit();
    edit.putInt("_id", id);
    boolean ret = edit.commit();
    if (ret) initSchema();
    return ret;
  }

  public int getCandTextSize() {
    return Integer.parseInt(preferences.getString("pref_cand_font_size", "22"));
  }

  public int getKeyTextSize() {
    return Integer.parseInt(preferences.getString("pref_key_font_size", "22"));
  }
}
