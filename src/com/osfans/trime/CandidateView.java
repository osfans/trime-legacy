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
import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;

/**
 * View to show candidate words.
 */
public class CandidateView extends View {

  /**
   * Listens to candidate-view actions.
   */
  public static interface CandidateViewListener {
    void onPickCandidate(ContentValues candidate);
  }

  public static final int MAX_CANDIDATE_COUNT = 20;
  private static final int CANDIDATE_TOUCH_OFFSET = -12;

  private CandidateViewListener listener;
  private ContentValues[] candidates = null;
  private int highlightIndex;

  private Drawable candidateHighlight;
  private Drawable candidateSeparator;
  private Paint paint, paintpy;

  private Rect candidateRect[] = new Rect[MAX_CANDIDATE_COUNT];
  private final String candNumKey = "pref_cand_num";
  private final String candFontSizeKey = "pref_cand_font_size";
  private final String candMaxPhraseKey = "pref_cand_max_phrase";
  private final SharedPreferences preferences;

  public CandidateView(Context context, AttributeSet attrs) {
    super(context, attrs);

    preferences = PreferenceManager.getDefaultSharedPreferences(context);

    Resources r = context.getResources();
    candidateHighlight = r.getDrawable(R.drawable.candidate_highlight);
    candidateSeparator = r.getDrawable(R.drawable.candidate_separator);

    paint = new Paint();
    paint.setColor(r.getColor(android.R.color.black));
    paint.setAntiAlias(true);
    paint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_text_size));
    paint.setStrokeWidth(0);

    paintpy = new Paint();
    paintpy.setColor(r.getColor(R.color.pinyin_normal));
    paintpy.setAntiAlias(true);
    paintpy.setTextSize(r.getDimensionPixelSize(R.dimen.pinyin_text_size));
    paintpy.setStrokeWidth(0);

    setWillNotDraw(false);
  }
  
  public void setCandidateViewListener(CandidateViewListener listener) {
    this.listener = listener;
  }

  /**
   * Sets the candidates being shown, and the view will be updated accordingly.
   * 
   * @param candidates a string contains 0 to MAX_CANDIDATE_COUNT candidates.
   */
  public void setCandidates(ContentValues[] candidates) {
    this.candidates = candidates;
    updateCandidateWidth();
    removeHighlight();
  }

  /**
   * Highlight the first candidate as the default candidate.
   */
  public void highlightDefault() {
    if (candidates.length > 0) {
      highlightIndex = 0;
      invalidate();
    }    
  }

  /**
   * Picks the highlighted candidate.
   *
   * @return {@code false} if no candidate is highlighted and picked.
   */
  public boolean pickHighlighted(int index) {
    if ((highlightIndex >= 0) && (listener != null)) {
      listener.onPickCandidate(getCandidate(index < 0 ? highlightIndex : index));
      return true;
    }
    return false;
  }

  private boolean updateHighlight(int x, int y) {
    int index = getCandidateIndex(x, y);
    if (index >= 0) {
      highlightIndex = index;
      invalidate();
      return true;
    }
    return false;
  }

  private void removeHighlight() {
    highlightIndex = -1;
    invalidate();
    requestLayout();
  }

  private void drawHighlight(Canvas canvas) {
    if (highlightIndex >= 0) {
      candidateHighlight.setBounds(candidateRect[highlightIndex]);
      candidateHighlight.draw(canvas);
    }
  }

  private void drawCandidates(Canvas canvas) {
    final int count = candidates!=null ? candidates.length : 0;

    float size = getCandFontSize();
    if (size != paint.getTextSize()) {
      paint.setTextSize(size);
    }

    if (count <= 0) return;

    // Draw the separator at the left edge of the first candidate. 
    candidateSeparator.setBounds(
      candidateRect[0].left,
      candidateRect[0].top,
      candidateRect[0].left + candidateSeparator.getIntrinsicWidth(),
      candidateRect[0].bottom);
    candidateSeparator.draw(canvas);

    boolean hasPy = getCandidatePy(0) != null;
    final int y =
        (int) (((getHeight() + (hasPy ? paintpy.getTextSize() : 0) - paint.getTextSize()) / 2) - paint.ascent());
    for (int i = 0; i < count; i++) {
      // Calculate a position where the text could be centered in the rectangle.
      String candidateHz = getCandDisplay(getCandidateHz(i));
      float x = (int) ((candidateRect[i].left + candidateRect[i].right - paint.measureText(candidateHz)) / 2);
      canvas.drawText(candidateHz, x, y, paint);
      if (hasPy) {
        String candidatePy = getCandidatePy(i);
        float x2 = (int) ((candidateRect[i].left + candidateRect[i].right - paintpy.measureText(candidatePy)) / 2);
        canvas.drawText(candidatePy, x2, - paintpy.ascent(), paintpy);
      }
      // Draw the separator at the right edge of each candidate.
      candidateSeparator.setBounds(
        candidateRect[i].right - candidateSeparator.getIntrinsicWidth(),
        candidateRect[i].top,
        candidateRect[i].right,
        candidateRect[i].bottom);
      candidateSeparator.draw(canvas);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (canvas == null) {
      return;
    }
    super.onDraw(canvas);

    drawHighlight(canvas);
    drawCandidates(canvas);
  }

  private void updateCandidateWidth() {
    final int top = 0;
    final int bottom = getHeight();

    // Set the first candidate 1-pixel wider since it'd accommodate two
    // candidate-separators.  
    candidateRect[0] = new Rect(0, top, getCandidateWidth(0) + 1, bottom);
    for (int i = 1, x = candidateRect[0].right; i < getCandNum(); i++) {
      candidateRect[i] = new Rect(x, top, x += getCandidateWidth(i), bottom);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    updateCandidateWidth();
  }

  @Override
  public boolean onTouchEvent(MotionEvent me) {
    int action = me.getAction();
    int x = (int) me.getX();
    int y = (int) me.getY();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_MOVE:
        updateHighlight(x, y);
        break;
      case MotionEvent.ACTION_UP:
        if (updateHighlight(x, y)) {
          pickHighlighted(-1);
        }
        break;
    }
    return true;
  }

  /**
   * Returns the index of the candidate which the given coordinate points to.
   * 
   * @return -1 if no candidate is mapped to the given (x, y) coordinate.
   */
  private int getCandidateIndex(int x, int y) {
    Rect r = new Rect();
    for (int i = 0; i < MAX_CANDIDATE_COUNT; i++) {
      if (candidates!= null && candidateRect[i] != null) {
        // Enlarge the rectangle to be more responsive to user clicks.
        r.set(candidateRect[i]);
        r.inset(0, CANDIDATE_TOUCH_OFFSET);
        if (r.contains(x, y)) {
          // Returns -1 if there is no candidate in the hitting rectangle.
          return (i < candidates.length) ? i : -1;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the candidate by the given candidate index.
   * 
   * @param index should be >= 0 and < candidates.length().
   */
  private ContentValues getCandidate(int index) {
    return (candidates!=null && candidates.length > index) ? candidates[index] : null;
  }

  private String getCandidateHz(int index) {
    return getCandidateKey(index, "hz");
  }

  private String getCandidatePy(int index) {
    return getCandidateKey(index, "py");
  }

  private String getCandidateKey(int index, String k) {
    ContentValues r = getCandidate(index);
    if (r != null && r.containsKey(k)) return r.getAsString(k);
    return null;
  }

  public float len(String s) {
    int n = s == null ? 0 : s.codePointCount(0, s.length());
    int m = getCandMaxPhrase();
    if ( n > m ) n = m;
    return n + 0.5f;
  }

  public float getCandFontSize() {
      int size = Integer.parseInt(preferences.getString(candFontSizeKey, "20"));
      return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, Resources.getSystem().getDisplayMetrics());
  }

  public int getCandNum() {
    return Integer.parseInt(preferences.getString(candNumKey, "5"));
  }

  public int getCandMaxPhrase() {
    return Integer.parseInt(preferences.getString(candMaxPhraseKey, "8"));
  }

  public String getCandDisplay(String s) {
    int n = s == null ? 0 : s.codePointCount(0, s.length());
    int m = getCandMaxPhrase();
    if ( n > m) return s.substring(0, m-1) + "…";
    return s;
  }

  private int getCandidateWidth(int index) {
    return (int)(len(getCandidateHz(index)) * getCandFontSize());
  }

  public int getCandMaxLen() {
    int w = getWidth(); //可能爲0
    return (int)(w/getCandFontSize());
  }
}
