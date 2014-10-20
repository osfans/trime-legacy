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
import android.text.format.Time;
//import android.util.Log;

import org.yaml.snakeyaml.Yaml;

public class DatabaseHelper {

    private static final String DB_NAME = "trime.db";
    private static final File sd = new File("/sdcard");
    private static final File dbFile = new File("/data/data/com.osfans.trime/databases/", DB_NAME);

    static String[] getFiles() {
        FilenameFilter ff = new FilenameFilter(){
            public boolean accept(File dir, String fn){
                return fn.endsWith(".db") || fn.endsWith(".yaml");
            }
        };
        return sd.list(ff);
    }

    static String getExportName() {
      Time t = new Time();
      t.setToNow();
      return String.format("trime_%s.db", t.format2445());
    }

    static SQLiteDatabase openDatabase(Context context) {
        if (!dbFile.exists()) {
            try {
                dbFile.getParentFile().mkdir();
                importDatabase(context.getAssets().open(DB_NAME));
            } catch (IOException e) {
                throw new RuntimeException("Error creating source database", e);
            }
        }
        return SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }

    static void initDictionary() {
        TRIME ime = TRIME.getService();
        if (ime != null) ime.initDictionary();
    }

    static boolean copyDatabase(Object ois, Object oos) {
        boolean success = false;
        try {
            InputStream is = null;
            OutputStream os = null;
            if (ois == null ) is = new FileInputStream(dbFile);
            else if (ois instanceof String) is = new FileInputStream(new File(sd, (String)ois));
            else is = (InputStream) ois;
            if (oos == null ) os = new FileOutputStream(dbFile);
            else if (oos instanceof String) os = new FileOutputStream(new File(sd, (String)oos));
            else oos = (OutputStream) oos;

            byte[] buffer = new byte[1024];
            while (is.read(buffer) > 0) {
                os.write(buffer);
            }
            os.flush();
            os.close();
            is.close();
            success = true;
        } catch (Exception e) {
            throw new RuntimeException("Error copy database", e);
        }
        return success;
    }

    static boolean updateSchema(String fn) {
        boolean success = false;
        SQLiteDatabase db =  SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        db.beginTransaction();
        try {
            InputStream is = new FileInputStream(new File(sd, fn));
            Yaml yaml = new Yaml();
            Map<String,Object> y = (Map<String,Object>)(yaml.load(is));
            Map<String,Object> m = (Map<String,Object>)y.get("schema");
            String schema_id = (String)m.get("schema_id");
            String name = (String)m.get("name");
            String full = yaml.dump(y);

            ContentValues initialValues = new ContentValues();
            initialValues.put("schema_id", schema_id);
            initialValues.put("name", name);
            initialValues.put("full", full);
            long r = db.update("schema", initialValues, "schema_id = ?", new String[] {schema_id});
            if (r == 0){
                r = db.insert("schema", null, initialValues);
            }
            db.setTransactionSuccessful();
            success = true;
        } catch (Exception e) {
            throw new RuntimeException("Error update schema", e);
        } finally {
            db.endTransaction();
        }
        return success;
    }

    static boolean importDatabase(Object ois) {
        if (ois instanceof String && ((String)ois).endsWith(".schema.yaml")) {
            return updateSchema((String)ois);
        }
        return copyDatabase(ois, null);
    }

    static boolean exportDatabase(Object oos) {
        return copyDatabase(null, oos);
    }
}

