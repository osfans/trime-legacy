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

import java.util.regex.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Reads a word-dictionary and provides word-suggestions as a list of characters
 * for the specified input.
 */
public class Dictionary {

  private final SQLiteDatabase mDatabase;
  private final SharedPreferences preferences;
  private final String scKey;
  private final String fullPyKey;
  private final String commitPyKey;
  private final String idKey = "_id";

  private final String defaultAlphabet = "[a-z0-9]+";
  private final String defaultSyllable = "[a-z0-9]+";
  public Pattern alphabetP, syllableP, autoSelectSyllableP;
  public String[][] pyspellRule, py2ipaRule, ipa2pyRule, ipafuzzyRule;
  public String[]  namedFuzzyRules = null;
  public boolean[] fuzzyRulesPref = null;
  public String table, phraseTable, keyboard;

  protected Dictionary(
      Context context) {
    preferences = PreferenceManager.getDefaultSharedPreferences(context);
    scKey = context.getString(R.string.prefs_sc_key);
    fullPyKey = context.getString(R.string.prefs_full_py_key);
    commitPyKey = context.getString(R.string.prefs_commit_py_key);
    mDatabase = AssetDatabaseOpenHelper.openDatabase(context);
    initSchema();
  }

    public boolean isAlphabet(CharSequence s) {
        return alphabetP.matcher(s).matches();
    }

    public boolean isSyllable(String s) {
        for(String i: s.split("'")) if(!syllableP.matcher(i).matches()) return false;
        return true;
    }

    public boolean isAutoSelect(CharSequence s) {
        return (autoSelectSyllableP != null) && autoSelectSyllableP.matcher(s).matches();
    }

    public String correctSpell(String s) {
        s = translate(s, pyspellRule);
        return isSyllable(s) ? s : "";
    }

    public String ipa2py(String s) {
        return translate(s, ipa2pyRule);
    }

    private String fuzzyText(String s) {
        if (ipafuzzyRule == null) return s;
        int n = ipafuzzyRule.length;
        if (n == 0) return s;
        StringBuilder r = new StringBuilder(s);
        ArrayList<Integer> b = new ArrayList<Integer>();
        ArrayList<String> fuzzyList =  new ArrayList<String>();
        for(int i = 0; i < fuzzyRulesPref.length; i++)  {
            if(fuzzyRulesPref[i])fuzzyList.add(namedFuzzyRules[i]);
        }
        String p = s;
        for (int j=0; j<n; j++){
            String[] rule = ipafuzzyRule[j];
            if (rule[0].length() == 0 || fuzzyList.contains(rule[0])) {
                String temp = p;
                p = p.replaceAll(rule[1],rule[2]);
                if (!p.contentEquals(temp)) b.add(j);
            }
        }
        int cnt = b.size();
        if (cnt > 0) {
            r.append(" OR ");
            r.append(p);
        }
        for (int i = 1;  i < ((1 << cnt) - 1); i++) {
            p = s;
            for (int j = 0; j < cnt; j++) {
                String[] rule = ipafuzzyRule[b.get(j)];
                if ((i & (1 << j)) != 0) p = p.replaceAll(rule[1],rule[2]);
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
                String[] rulea = rule[1].split(rule[1].contains("|") ? "\\|" : "\\B");
                String[] ruleb = rule[2].split(rule[2].contains("|") ? "\\|" : "\\B");
                int n = rulea.length;
                if (n == ruleb.length) {
                    for (int i = 0; i < n; i++) s = s.replace(rulea[i], ruleb[i]);
                }
            } else s = s.replaceAll(rule[1],rule[2]);
        }
        return s;
    }

    private String[][] getRule(Cursor cursor, String column) {
        String s = cursor.getString(cursor.getColumnIndex(column)).trim();
        String[] rule = s.split("\n");
        if (s.length() > 0 && rule.length > 0) {
            String[][] rules = new String[rule.length][4];
            for(int i = 0; i < rule.length; i++) {
                rules[i] = rule[i].split("(?<!\\\\)/", 4);
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
        for(boolean b: fuzzyRulesPref) s.append(b?"1":"0");
          SharedPreferences.Editor edit = preferences.edit();
          edit.putString(String.format("fuzzy%d", getSchemaId()), s.toString());
          edit.commit();
    }

    private void initNamedFuzzyRule() {
        ArrayList<String> fuzzyList = new ArrayList<String>();
        if (ipafuzzyRule != null) {
            for(String[] i: ipafuzzyRule) {
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

    private void initSchema() {
        int id = getSchemaId();
        Cursor cursor = query(String.format("select * from schema where _id = %d", id), null);
        if (cursor == null) return;
        table = cursor.getString(cursor.getColumnIndex("dictionary"));
        phraseTable = cursor.getString(cursor.getColumnIndex("phrase"));
        if (phraseTable.length() == 0) phraseTable = table;
        keyboard = cursor.getString(cursor.getColumnIndex("keyboard"));

        String a = cursor.getString(cursor.getColumnIndex("alphabet"));
        alphabetP = Pattern.compile((a!=null && a.length() > 0) ? a : defaultAlphabet);
        a = cursor.getString(cursor.getColumnIndex("syllable"));
        syllableP = Pattern.compile((a!=null && a.length() > 0) ? a : defaultSyllable);
        a = cursor.getString(cursor.getColumnIndex("auto_select_syllable"));
        autoSelectSyllableP = (a!=null && a.length() > 0) ? Pattern.compile(a) : null;
        pyspellRule = getRule(cursor, "pyspell");
        py2ipaRule = getRule(cursor, "py2ipa");
        ipa2pyRule = getRule(cursor, "ipa2py");
        ipafuzzyRule = getRule(cursor, "ipafuzzy");
        initNamedFuzzyRule();
        cursor.close();
    }

    public String getKeyboard() {
        return keyboard;
    }

    private String querySchema(String s) {
        int id = getSchemaId();
        Cursor cursor = query(String.format("select * from schema where _id = %d", id), null);
        if (cursor == null) return "";
        String ret = cursor.getString(cursor.getColumnIndex(s));
        cursor.close();
        return ret;
    }

    public String getSchemaInfo() {
        StringBuilder sb = new StringBuilder();
        for(String i: new String[]{"name", "version", "author"}) {
            sb.append(querySchema(i) + " ");
        }
        return sb.toString();
    }

    public Cursor getSchemas() {
        return query("select * from schema", null);
    }

  public String[] getPy(CharSequence code) {
      String sql = String.format("select py from %s where hz match ?", table);
      Cursor cursor = query(sql, new String[]{code.toString()});
      if (cursor == null) return null;
      int n = cursor.getCount();
      int i = 0;
      String[] s = new String[n];
      do {
          s[i++] = ipa2py(cursor.getString(0));
      }while(cursor.moveToNext());
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
    s = translate(s, py2ipaRule);
    String t = s;
    if (ipafuzzyRule != null) s = fuzzyText(s);

    boolean fullPyOn = isFullPy() && s.length() < 3;
    if (s.contains("'")) return getPhrase(s);

    String sql = String.format("select hz from %s where py match ? and not glob('* *', py)", table);
    if (!s.contentEquals(t) || s.contains("*")) sql = sql.replace("select hz ", "select hz,py ");
    Cursor cursor = query(sql, new String[]{s});
    if (cursor != null || fullPyOn) return cursor;

    s = s.replace(" OR", "* OR") + "*";
    sql = sql.replace("select hz ", "select hz,py ");
    return query(sql + " limit 100", new String[]{s});
  }

  private Cursor getPhrase(CharSequence code) {
    boolean fullPyOn = isFullPy() && code.length() < 6;
    if (phraseTable.contentEquals("phrase")) return null;
    String sql = String.format("select hz from %s where py match ? limit 100", phraseTable);
    String s = String.format("\"^%s\"",code.toString().replace(" OR ", "\" OR \"^").replace("'", " "));
    //Log.e("kyle", "phrase start");
    Cursor cursor = query(sql, new String[]{s});
    if (cursor != null || fullPyOn) return cursor;
    sql = sql.replace("select hz", "select hz,py");
    s = String.format("\"^%s*\"",code.toString().replace(" OR ", "*\" OR \"^").replace("'", " "));
    cursor = query(sql, new String[]{s});
    if (cursor != null) return cursor;
    s = String.format("\"^%s*\"",code.toString().replace(" OR ", "*\" OR \"^").replace("'", "* "));
    cursor = query(sql, new String[]{s});
    //Log.e("kyle", "phrase end");
    return cursor;
  }

  public Cursor getAssociation(CharSequence code) {
      String s = code.toString();
      int len = s.length();
      String sqlFormat = "select distinct substr(hz,%d) from %s where hz match '^%s*' and length(hz) > %d order by docid limit 100";
      return query(String.format(sqlFormat, len + 1, phraseTable, s, len), null);
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
      return preferences.getBoolean(commitPyKey, false);
  }

  public boolean isSC() {
      return preferences.getBoolean(scKey, false);
  }

  private boolean isFullPy() {
      return preferences.getBoolean(fullPyKey, false);
  }

  public int getSchemaId() {
      return preferences.getInt(idKey, 0);
  }

  public boolean isShapeCode() {
      return preferences.getBoolean("prefs_shape_code", false);
  }

  public boolean isKeyboardPreview() {
      return preferences.getBoolean("prefs_keyboard_preview", false);
  }

  public boolean setSchemaId(int id) {
      SharedPreferences.Editor edit = preferences.edit();
      edit.putInt(idKey, id);
      boolean ret = edit.commit();
      if (ret) initSchema();
      return ret;
  }
}
