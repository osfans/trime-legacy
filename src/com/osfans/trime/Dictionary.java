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
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.MergeCursor;

import java.util.regex.*;
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
  private final SharedPreferences mPref;
  private Switches mSwitches;

  private Map<String,Object> mSchema, mDefaultSchema;

  private Object keyboard;
  private String table, schema_name;
  private String delimiter, alphabet, initials;

  private String[][] preeditRule, commentRule;
  private String half;
  private int max_code_length_;
  private boolean auto_select_;
  private Pattern auto_select_pattern_;
  private boolean has_phrase_gap, has_prism;
  private String segment_sql, px_sql;
  private String hz_sql, py_sql;
  private String opencc_sql = "select t from opencc where opencc match ?";
  private String schema_sql = "select * from schema";
  private String association_sql = "select distinct substr(hz,%d) from `%s` where hz match '^%s*' and length(hz) > %d limit 100";
  private Pattern rule_sep = Pattern.compile("\\W");
  private String reverse_dictionary, reverse_prefix, reverse_tips, reverse_sql;
  private String[][] reverse_preedit_format, reverse_comment_format;
  private Pattern reverse_pattern;
  private boolean is_reverse;
  private String py_pattern = "([^ANDOR \t]+)";

  protected Dictionary(Context context) {
    mPref = PreferenceManager.getDefaultSharedPreferences(context);
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

  public boolean isAutoSelect(CharSequence s) {
    if (max_code_length_ > 0 && max_code_length_ == s.length()) return true; //最大碼長
    return auto_select_ && (auto_select_pattern_ != null) && auto_select_pattern_.matcher(s).matches(); //正則上屏
  }

  private boolean isSyllable(String s) {
    s = h2f(s.trim());
    if (hasDelimiter()) s = s.replace(getDelimiter(), "*" + getDelimiter());
    Cursor cursor = query(segment_sql, new String[]{s + "*"});
    if (cursor == null) return false;
    cursor.close();
    return true;
  }

  public String segment(String r, CharSequence text) {
    if (isSyllable(r + text)) return r + text;
    if (hasDelimiter() && isSyllable(r + getDelimiter() + text)) return r + getDelimiter() + text;
    return null;
  }

  private String translate(String s, String[][] rules) {
    if (rules == null) return s;
    for (String[] rule:  rules) {
      //Log.e("kyle", "apply rule "+rule[0]+":"+rule[1]+"->"+rule[2]);
      if (rule[0].contentEquals("xlit")) {
        String[] rulea = rule[1].split("");
        String[] ruleb = rule[2].split("");
        int n = rulea.length;
        if (n == ruleb.length) {
          for (int i = 0; i < n; i++) if (rulea[i].length() > 0) s = s.replace(rulea[i], ruleb[i]);
        }
      } else if(rule[0].contentEquals("xform"))  s = s.replaceAll(rule[1],rule[2]);
    }
    return s;
  }

  private String[][] getRule(String k1, String k2) {
    List<String> rule = (List<String>)getValue(k1, k2);
    if (rule != null && rule.size() > 0) {
      int n = rule.size();
      String[][] rules = new String[n][4];
      for(int i = 0; i < n; i++) {
        String s = rule.get(i);
        Matcher m = rule_sep.matcher(s);
        if (m.find()) {
          rules[i] = s.split(m.group(), 4);
          //Log.e("kyle", "get rule="+rules[i][0]+",1="+rules[i][1]+"->"+rule[2]);
        }
      }
      return rules;
    }
    return null;
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

  private boolean GetBool(String k1, String k2) {
    Object v = getValue(k1, k2);
    if (v == null) return false;
    return (Boolean) v;
  }

  private int GetInt(String k1, String k2) {
    Object v = getValue(k1, k2);
    if (v == null) return 0;
    return (Integer) v;
  }

  private String GetString(String k1, String k2) {
    Object v = getValue(k1, k2);
    if (v == null) return null;
    return (String) v;
  }

  private Pattern GetPattern(String k1, String k2) {
    Object v = getValue(k1, k2);
    if (v == null) return null;
    return Pattern.compile((String)v);
  }

  private void initHalf() {
    String a = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    StringBuilder s = new StringBuilder();
    for (char i: alphabet.toCharArray()) {
      if (a.indexOf(i) >=0) s.append(i);
    }
    half = s.toString();
  }

  private String h2f(String s) {
    if (half.isEmpty()) return s;
    for (int i: half.toCharArray()) {
      s = s.replace((char)i, (char)(i - 0x20 + 0xff00));
    }
    return s;
  }

  private String f2h(String s) {
    if (half.isEmpty()) return s;
    for (int i: half.toCharArray()) {
      s = s.replace((char)(i - 0x20 + 0xff00), (char)i);
    }
    return s;
  }

  private void initSchema() {
    int id = getSchemaId();
    String sql = "SELECT * FROM schema WHERE _id = %d OR _id = 0 ORDER BY _id DESC";
    Cursor cursor = query(String.format(sql, id), null);
    if (cursor == null) return;
    mSchema = (Map<String,Object>)new Yaml().load(cursor.getString(cursor.getColumnIndex("full")));
    cursor.close();

    schema_name = GetString("schema", "schema_id");
    delimiter = GetString("speller", "delimiter");
    alphabet = GetString("speller", "alphabet");
    initials = GetString("speller", "initials");
    max_code_length_ = GetInt("speller", "max_code_length");
    auto_select_ = GetBool("speller", "auto_select");
    auto_select_pattern_ = GetPattern("speller", "auto_select_pattern");
    has_prism = (getValue("speller", "algebra") != null);

    preeditRule = getRule("translator", "preedit_format");
    commentRule = getRule("translator", "comment_format");

    table = GetString("translator", "dictionary");
    sql = "SELECT phrase_gap FROM dictionary WHERE name = ?";
    cursor = query(sql, new String[]{table});
    if (cursor != null) {
      has_phrase_gap = (cursor.getInt(0) == 1);
      cursor.close();
    }

    if (has_prism) {
      px_sql = String.format("select py from `%s.prism` where px match ?", schema_name);
      segment_sql= String.format("select _id from schema where px match ? and schema_id = '%s'", schema_name);
    } else {
      segment_sql = String.format("select pya from `%s` where pya match ? limit 1", table);
    }

    hz_sql = "select %s from `" + table + "` where %s %s limit 100";
    py_sql = String.format("select trim(pya || ' ' || pyb || ' ' || pyc || ' ' || pyz) as py from `%s` where hz match ? limit 100", table);

    keyboard = getValue("trime", "keyboard");
    initHalf();

    reverse_dictionary = GetString("reverse_lookup", "dictionary");
    if (reverse_dictionary != null) {
      reverse_prefix = GetString("reverse_lookup", "prefix");
      reverse_tips = GetString("reverse_lookup", "tips");
      reverse_preedit_format = getRule("reverse_lookup", "preedit_format");
      reverse_comment_format = getRule("reverse_lookup", "comment_format");
      reverse_pattern = Pattern.compile(((Map<String,String>)getValue("recognizer", "patterns")).get("reverse_lookup"));
      reverse_sql = String.format("select `%s`.hz, `%s`.pya as py from `%s`, `%s` where `%s`.pya match ? and `%s`.pyb = '' and `%s` match 'hz:' || `%s`.hz limit 100", table, table, table, reverse_dictionary,  reverse_dictionary, reverse_dictionary, table, reverse_dictionary);
    }
    mSwitches = new Switches(getValue("switches"));
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
    return query(schema_sql, null);
  }

  public String preedit(String s, Cursor cursor) {
    if (isEmbedFirst() && cursor != null) {
      s = cursor.getString(0);
    } else {
      s = f2h(s.trim());
      if (isReverse(s) && reverse_preedit_format != null) {
        s = s.substring(reverse_prefix.length());
        s = reverse_tips + translate(s, reverse_preedit_format);
      } else {
        s = translate(s, preeditRule);
        if (hasDelimiter()) s = s.replace(getDelimiter(), "'");
      }
    }
    return s;
  }

  public String comment(String s) {
    s = f2h(s.trim());
    return translate(s, is_reverse ? reverse_comment_format : commentRule);
  }

  public String[] getComment(CharSequence code) {
    Cursor cursor = query(py_sql, new String[]{code.toString()});
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

  private String queryPrism(String s) {
    Cursor cursor = query(px_sql, new String[]{s});
    if (cursor != null) {
      s = cursor.getString(0).replace(" ", " OR ");
      cursor.close();
    }
    return s;
  }

  private String getWhere(String s, boolean fullPyOn) {
    s = h2f(s.trim());
    boolean is_phrase = hasDelimiter() && s.contains(getDelimiter()) && !isSingle();
    if (is_phrase) {
      StringBuilder sb = new StringBuilder();
      if (has_prism) {
        for (String i: s.split(getDelimiter())) {
          sb.append(queryPrism(i) + getDelimiter());
        }
        s = sb.toString().trim();
      }

      String[] sl = s.split(getDelimiter(), 4);
      sb.setLength(0);
      sb.append(String.format("`%s` match '", table));
      sb.append(" " + sl[0].replaceAll(py_pattern, "pya:$1"));
      sb.append(" " + sl[1].replaceAll(py_pattern, "pyb:$1"));
      if (sl.length == 2) sb.append("' AND pyc == ''");
      if (sl.length >= 3) sb.append(" " + sl[2].replaceAll(py_pattern, "pyc:$1"));
      if (sl.length == 3) sb.append("' AND pyz == ''");
      if (sl.length == 4) sb.append(" " + sl[3].replaceAll(py_pattern, "pyz:$1") + "'");
      s = sb.toString();
    } else {
      if (has_prism) s = queryPrism(s);
      if (!fullPyOn) s = s.replaceAll(py_pattern, "$1* -$1");
      s = String.format("pya match '%s' AND pyb == ''", s);
    }
    return s;
  }

  /**
   * Returns a string containing words as suggestions for the specified input.
   * 
   * @param input should not be null.
   * @return a concatenated string of characters, or an empty string if there
   *     is no word for that input.
   */
  public Cursor queryWord(CharSequence code) {
    String s = code.toString();
    if (isReverse(s)) return queryReverse(s);
    is_reverse = false;

    String sql = String.format(hz_sql, getQueryCol(), getWhere(s, true), getSingle());
    Cursor cursor = query(sql, null);
    if (isFullPy()) return cursor;
    if (cursor != null && cursor.getCount() == 100) return cursor;
    if (hasDelimiter() && s.contains(getDelimiter()) && !isSingle()) return cursor;

    Cursor cursor1; //模糊搜索
    sql = String.format(hz_sql, getQueryCol(), getWhere(s, false), getSingle());
    cursor1 = query(sql, null);
    if (cursor1 == null) return cursor;
    if (cursor == null) return cursor1;
    return new MergeCursor(new Cursor[]{cursor, cursor1});
  }

  public Cursor getAssociation(CharSequence code) {
    if (!isAssociation()) return null;
    String s = code.toString();
    int len = s.length();
    return query(String.format(association_sql, len + 1, table, s, len), null);
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

  private String mm(String text, String rule) {
    if (rule.isEmpty()) return text;

    Cursor cursor;
    int l = text.length();
    int start = 0, end = l;
    StringBuilder t = new StringBuilder();
    String s;

    while (start < end) {
      s = text.substring(start, end);
      cursor =  query(opencc_sql, new String[]{String.format("s:%s r:%s", s, rule)});
      if (cursor == null) {
        if (start + 1 == end) {
          t.append(s);
          start++;
          end = l;
        } else end--;
      } else {
        t.append(cursor.getString(0).split(" ")[0]);
        cursor.close();
        start = end;
        end = l;
      }
    }
    return t.toString();
  }

  public String openCC(String text) {
    String st = getOpenCC();
    for(String r: st.split(",")) text = mm(text, r);
    return text;
  }

  public boolean isCommitPy() {
    return mPref.getBoolean("pref_commit_py", false);
  }

  public String getOpenCC() {
    return mPref.getString("pref_opencc", "");
  }

  private boolean isFullPy() {
    return mPref.getBoolean("pref_full_py", false);
  }

  public boolean hasDelimiter() {
    return has_phrase_gap && (delimiter != null) && delimiter.length() > 0;
  }

  public boolean isDelimiter(CharSequence s) {
    return hasDelimiter() && s.length() > 0 && s.charAt(0) != ' ' && delimiter.contains(s);
  }

  public String getDelimiter() {
    return hasDelimiter() ? "\t" : "";
  }

  public boolean isKeyboardPreview() {
    return mPref.getBoolean("pref_keyboard_preview", true);
  }

  private boolean isSingle() {
    return mPref.getBoolean("pref_single", false);
  }

  private String getSingle() {
    return isSingle() ? " and length(hz) == 1" : "";
  }

  private boolean isAssociation() {
    return mPref.getBoolean("pref_association", false);
  }

  private String getQueryCol() {
    return mPref.getBoolean("pref_py_prompt", false) ? "hz, trim(pya || ' ' || pyb || ' ' || pyc || ' ' || pyz) as py" : "hz";
  }

  private boolean isEmbedFirst() {
    return mPref.getBoolean("pref_embed_first", false);
  }

  public int getSchemaId() {
    return mPref.getInt("_id", 0);
  }

  public boolean setSchemaId(int id) {
    SharedPreferences.Editor edit = mPref.edit();
    edit.putInt("_id", id);
    boolean ret = edit.commit();
    if (ret) initSchema();
    return ret;
  }

  public int getCandTextSize() {
    return Integer.parseInt(mPref.getString("pref_cand_font_size", "22"));
  }

  public int getKeyTextSize() {
    return Integer.parseInt(mPref.getString("pref_key_font_size", "22"));
  }

  public boolean isReverse(String s) {
    return reverse_dictionary != null && reverse_pattern != null && reverse_pattern.matcher(s).matches();
  }

  private Cursor queryReverse(String s) {
    if (!isReverse(s)) return null;
    is_reverse = true;
    s = s.substring(reverse_prefix.length());
    Cursor cursor = query(reverse_sql, new String[]{s});
    if (cursor != null && cursor.getCount() == 100) return cursor;
    Cursor cursor1 = query(reverse_sql, new String[]{s + "* -" + s});
    if (cursor1 == null) return cursor;
    if (cursor == null) return cursor1;
    return new MergeCursor(new Cursor[]{cursor, cursor1});
  }

  public boolean toggleStatus(String k) {
    boolean v = !mSwitches.getStatus(k);
    return mSwitches.setStatus(k, v);
  }

  public boolean getAsciiMode() {
    return mSwitches.getStatus("ascii_mode");
  }

  public Cursor queryStatus() {
    return mSwitches.queryStatus();
  }
}
