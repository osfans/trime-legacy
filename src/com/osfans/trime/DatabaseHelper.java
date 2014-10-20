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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;

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
        } finally {
            return success;
        }
    }

    static boolean importDatabase(Object ois) {
        return copyDatabase(ois, null);
    }

    static boolean exportDatabase(Object oos) {
        return copyDatabase(null, oos);
    }
}

