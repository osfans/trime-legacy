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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;

/**
 * Plays sound and motion effect.
 */
public class SoundMotionEffect {
  private static final int VIBRATE_DURATION = 30;
  private static final float FX_VOLUME = -1.0f;

  private final Context context;
  private final SharedPreferences preferences;
  private final String vibrateKey;
  private final String soundKey;

  private boolean vibrateOn;
  private Vibrator vibrator;
  private boolean soundOn;
  private AudioManager audioManager;

  public SoundMotionEffect(Context context) {
    this.context = context;
    preferences = PreferenceManager.getDefaultSharedPreferences(context);
    vibrateKey = context.getString(R.string.prefs_vibrate_key);
    soundKey = context.getString(R.string.prefs_sound_key);
  }

  public void reset() {
    vibrateOn = preferences.getBoolean(vibrateKey, false);
    if (vibrateOn && (vibrator == null)) {
      vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    soundOn = preferences.getBoolean(soundKey, false);
    if (soundOn && (audioManager == null)) {
      audioManager = 
        (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
  }

  public void vibrate() {
    if (vibrateOn && (vibrator != null)) {
      vibrator.vibrate(VIBRATE_DURATION);
    }
  }

  public void playSound() {
    if (soundOn && (audioManager != null)) {
      audioManager.playSoundEffect(
          AudioManager.FX_KEYPRESS_STANDARD, FX_VOLUME);
    }
  }
}
