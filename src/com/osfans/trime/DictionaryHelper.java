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

import java.io.*;
import java.util.Map;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.text.format.Time;
import android.util.Log;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import org.yaml.snakeyaml.Yaml;

public class DictionaryHelper extends SQLiteOpenHelper {

  private final Context mContext;
  private SQLiteDatabase mDatabase;
  private static final String DB_NAME = "trime.db";
  private static final File sd = new File("/sdcard");
  private static final File dbFile = new File("/data/data/com.osfans.trime/databases/", DB_NAME);
  private static final int DB_VER = 2;
  private static final int BLK_SIZE = 1024;
  private static final String fs = "...";
  private static final String comment = "#";
  private static final String newline = "\n";

  NotificationManager mNotifyManager;
  NotificationCompat.Builder mBuilder;
  int notify_id = 1;

  DictionaryHelper(Context context) {
    super(context, DB_NAME, null, DB_VER);
    mContext = context;
    initProgress();
    loadAssetsDB();
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // mDatabase = db;
  }

  private void initProgress() {
    mNotifyManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
    mBuilder = new NotificationCompat.Builder(mContext);
    mBuilder.setSmallIcon(R.drawable.smallicon);
  }

  private void loadAssetsDB() {
    if (dbFile.exists()) return;
    try {
      dbFile.getParentFile().mkdir();
      mBuilder.setContentTitle(mContext.getString(R.string.initdb_message));
      copyDatabase(mContext.getAssets().open(DB_NAME), null);
    } catch (IOException e) {
      throw new RuntimeException("Error creating source database", e);
    }
  }

  static String[] getImportNames() {
    FilenameFilter ff = new FilenameFilter(){
      public boolean accept(File dir, String fn){
        return fn.endsWith(".db") || fn.endsWith(".schema.yaml") || fn.endsWith(".dict.yaml");
      }
    };
    return sd.list(ff);
  }

  static String getExportName() {
    Time t = new Time();
    t.setToNow();
    return String.format("trime_%s.db", t.format2445());
  }

  private boolean copyDatabase(InputStream is, String s) {
    boolean success = false;
    mNotifyManager.notify(notify_id, mBuilder.build());
    try {
      if (is == null ) is = new FileInputStream(dbFile);

      OutputStream os = null;
      if (s == null) {
        close();
        os = new FileOutputStream(dbFile);
      } else {
        os = new FileOutputStream(new File(sd, s));
      }

      int max = is.available();
      int count = 0;
      int progress = 0;
      byte[] buffer = new byte[BLK_SIZE];
      while (is.read(buffer) > 0) {
        os.write(buffer);
        count++;
        if ((count % 100) == 0) {
          progress = count * BLK_SIZE;
          mBuilder.setProgress(max, progress, false)
            .setContentText(String.format("%d / 100", progress * 100 / max));
          mNotifyManager.notify(notify_id, mBuilder.build());
        }
      }
      os.flush();
      os.close();
      is.close();
      success = true;
    } catch (Exception e) {
      throw new RuntimeException("Error copy database", e);
    } finally {
      mNotifyManager.cancel(notify_id);
    }
    return success;
  }

  private boolean importSchema(InputStream is) {
    boolean success = false;
    SQLiteDatabase db =  getWritableDatabase();
    db.beginTransaction();
    try {
      Yaml yaml = new Yaml();
      Map<String,Object> y = (Map<String,Object>)(yaml.load(is));
      Map<String,Object> m = (Map<String,Object>)y.get("schema");
      String schema_id = (String)m.get("schema_id");
      String name = (String)m.get("name");
      String full = yaml.dump(y);
      is.close();

      ContentValues initialValues = new ContentValues();
      initialValues.put("schema_id", schema_id);
      initialValues.put("name", name);
      initialValues.put("full", full);

      long r = db.update("schema", initialValues, "schema_id = ?", new String[] {schema_id});
      if (r == 0) r = db.insert("schema", null, initialValues);
      db.setTransactionSuccessful();
      success = true;
    } catch (Exception e) {
      throw new RuntimeException("Error import schema", e);
    } finally {
      db.endTransaction();
    }
    return success;
  }

  private boolean importDict(InputStream is) {
    boolean success = false;
    SQLiteDatabase db =  getWritableDatabase();
    db.beginTransaction();
    try {
      String line;
      StringBuilder content = new StringBuilder();
      InputStreamReader ir = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(ir);
      while ((line = br.readLine()) != null && !line.contentEquals(fs)) {
        content.append(line);
        content.append(newline);
      }

      Yaml yaml = new Yaml();
      Map<String,Object> y = (Map<String,Object>)(yaml.load(content.toString()));
      String table = (String)y.get("name");

      db.execSQL("DROP TABLE IF EXISTS " + table);
      db.execSQL(String.format("CREATE VIRTUAL TABLE %s USING fts3(hz, py)", table));

      ContentValues initialValues = new ContentValues(2);
      int max = is.available();
      int progress = 0;
      int count = 0;
      while ((line = br.readLine()) != null) {
        if (line.startsWith(comment)) continue;
        String[] s = line.split("\t");
        if (s.length < 2) continue;
        initialValues.put("hz", s[0]);
        initialValues.put("py", s[1]);
        db.insert(table, null, initialValues);
        initialValues.clear();
        count++;
        if ((count % 1000) == 0) {
          progress = max - is.available();
          mBuilder.setProgress(max, progress, false)
            .setContentText(String.format("%d / 100", progress * 100 / max));
          mNotifyManager.notify(notify_id, mBuilder.build());
        }
      }
      is.close();
      db.setTransactionSuccessful();
      success = true;
    } catch (Exception e) {
      throw new RuntimeException("Error import dict", e);
    } finally {
      db.endTransaction();
      mNotifyManager.cancel(notify_id);
    }
    return success;
  }

  private void toast(String t, String s) {
    mBuilder.setContentTitle(t)
            .setContentText(s)
            .setProgress(0, 0, false);
    mNotifyManager.notify(notify_id, mBuilder.build());
  }

  public void importDatabase(final String s) {
    try {
      final InputStream is = new FileInputStream(new File(sd, s));
      mBuilder.setContentTitle(String.format(mContext.getString(R.string.importdb_message), s));
      if (s.endsWith(".db") || s.endsWith(".schema.yaml")) {
        boolean success = s.endsWith(".db") ? copyDatabase(is, null) : importSchema(is);
        if (success) toast(mContext.getString(R.string.importdb_success), s);
        else toast(mContext.getString(R.string.importdb_failure), s);
      } else if (s.endsWith(".dict.yaml")) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            boolean success = importDict(is);
            if (success) toast(mContext.getString(R.string.importdb_success), s);
            else toast(mContext.getString(R.string.importdb_failure), s);
          }
        }
      ).start();
    }
    } catch (Exception e) {
      throw new RuntimeException("Error import Database", e);
    }
  }

  public void exportDatabase(final String s) {
    mBuilder.setContentTitle(mContext.getString(R.string.exportdb_message));
    boolean success = copyDatabase(null, s);
    if (success) toast(mContext.getString(R.string.exportdb_success), s);
    else toast(mContext.getString(R.string.exportdb_failure), null);
  }
}

