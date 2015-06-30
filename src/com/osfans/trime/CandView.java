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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;

/**
 * View to show candidate words.
 */
public class CandView extends View {

  /**
   * Listens to candidate-view actions.
   */
  public static interface CandViewListener {
    void onPickCandidate(int index);
  }

  public static final int MAX_CANDIDATE_COUNT = 20;
  private static final int CANDIDATE_TOUCH_OFFSET = -12;

  private CandViewListener listener;
  private int highlightIndex;
  private Rime mRime;

  private Drawable candidateHighlight;
  private Drawable candidateSeparator;
  private Paint paint, paintpy;

  private Rect candidateRect[] = new Rect[MAX_CANDIDATE_COUNT];
  private final String candFontSizeKey = "pref_cand_font_size";
  private final SharedPreferences preferences;

  public CandView(Context context, AttributeSet attrs) {
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
    mRime = Rime.getRime();
  }
  
  public void setCandViewListener(CandViewListener listener) {
    this.listener = listener;
  }

  /**
   * Highlight the first candidate as the default candidate.
   */
  public void update() {
    removeHighlight();
    updateCandidateWidth();
    if (mRime.getCandNum() > 0) {
      highlightIndex = mRime.getCandHighlightIndex();
      invalidate();
    }    
  }

  /**
   * Picks the highlighted candidate.
   *
   * @return {@code false} if no candidate is highlighted and picked.
   */
  public boolean pickHighlighted(int index) {
    if ((highlightIndex != -1) && (listener != null)) {
      listener.onPickCandidate(index == -1 ? highlightIndex : index);
      return true;
    }
    return false;
  }

  private boolean updateHighlight(int x, int y) {
    int index = getCandidateIndex(x, y);
    if (index != -1) {
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
    float size = getCandFontSize();
    if (size != paint.getTextSize()) {
      paint.setTextSize(size);
    }

    final int count = mRime.getCandNum();
    if (count <= 0) return;

    // Draw the separator at the left edge of the first candidate. 
    candidateSeparator.setBounds(
      candidateRect[0].left,
      candidateRect[0].top,
      candidateRect[0].left + candidateSeparator.getIntrinsicWidth(),
      candidateRect[0].bottom);
    candidateSeparator.draw(canvas);

    final int y =
        (int) (((getHeight() + paintpy.getTextSize() - paint.getTextSize()) / 2) - paint.ascent());
    int x = 0;
    int j = 0;
    
    for (int i = 0; i < count; i++) {
      // Calculate a position where the text could be centered in the rectangle.
      String candidate = getCandidate(i);
      x = (int) ((candidateRect[j].left + candidateRect[j].right - paint.measureText(candidate)) / 2);
      canvas.drawText(candidate, x, y, paint);
      String comment = mRime.getComment(i);
      if (comment != null) {
        float x2 = (int) ((candidateRect[j].left + candidateRect[j].right - paintpy.measureText(comment)) / 2);
        canvas.drawText(comment, x2, - paintpy.ascent(), paintpy);
      }
      // Draw the separator at the right edge of each candidate.
      candidateSeparator.setBounds(
        candidateRect[j].right - candidateSeparator.getIntrinsicWidth(),
        candidateRect[j].top,
        candidateRect[j].right,
        candidateRect[j].bottom);
      candidateSeparator.draw(canvas);
      j++;
    }
    if (!mRime.isFirst()) {
      String candidate = getCandidate(-4);
      x = (int) ((candidateRect[j].left + candidateRect[j].right - paint.measureText(candidate)) / 2);
      canvas.drawText(candidate, x, y, paint);
      candidateSeparator.setBounds(
        candidateRect[j].right - candidateSeparator.getIntrinsicWidth(),
        candidateRect[j].top,
        candidateRect[j].right,
        candidateRect[j].bottom);
      candidateSeparator.draw(canvas);
      j++;
    }
    if (!mRime.isLast()) {
      String candidate = getCandidate(-5);
      x = (int) ((candidateRect[j].left + candidateRect[j].right - paint.measureText(candidate)) / 2);
      canvas.drawText(candidate, x, y, paint);
      candidateSeparator.setBounds(
        candidateRect[j].right - candidateSeparator.getIntrinsicWidth(),
        candidateRect[j].top,
        candidateRect[j].right,
        candidateRect[j].bottom);
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
    int i = 0;
    int x = 0;
    candidateRect[i++] = new Rect(0, top, x += getCandidateWidth(0) + 1, bottom);
    int count = mRime.getCandNum();
    for (int j = 1; j < count; j++) candidateRect[i++] = new Rect(x, top, x += getCandidateWidth(j), bottom);
    if (!mRime.isFirst()) candidateRect[i++] = new Rect(x, top, x += getCandidateWidth(-4), bottom);
    if (!mRime.isLast()) candidateRect[i++] = new Rect(x, top, x += getCandidateWidth(-5), bottom);
    LayoutParams params = getLayoutParams();
    params.width = x;
    setLayoutParams(params);
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

    int j = 0;
    int n = mRime.getCandNum();
    for (int i = 0; i < n; i++) {
      // Enlarge the rectangle to be more responsive to user clicks.
      r.set(candidateRect[j++]);
      r.inset(0, CANDIDATE_TOUCH_OFFSET);
      if (r.contains(x, y)) {
        // Returns -1 if there is no candidate in the hitting rectangle.
        return (i < n) ? i : -1;
      }
    }

    if (!mRime.isFirst()) { //Page Up
      r.set(candidateRect[j++]);
      r.inset(0, CANDIDATE_TOUCH_OFFSET);
      if (r.contains(x, y)) {
        return -4;
      }
    }

    if (!mRime.isLast()) { //Page Down
      r.set(candidateRect[j++]);
      r.inset(0, CANDIDATE_TOUCH_OFFSET);
      if (r.contains(x, y)) {
        return -5;
      }
    }

    return -1;
  }

  public float getCandFontSize() {
    int size = Integer.parseInt(preferences.getString(candFontSizeKey, "20"));
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, Resources.getSystem().getDisplayMetrics());
  }

  private String getCandidate(int i) {
    String s = null;
    if (i >= 0) s = mRime.getCandidate(i);
    else if (i == -4) s = "◀ ";
    else if (i == -5) s = " ▶";
    return s;
  }

  private int getCandidateWidth(int i) {
    String s = getCandidate(i);
    float n = (s == null ? 0 : s.codePointCount(0, s.length())) + 0.5f;
    float x0 = n * getCandFontSize();
    if (i >= 0) {
      String comment = mRime.getComment(i);
      if (comment != null) {
        float x2 = paintpy.measureText(comment);
        if (x2 > x0) x0 = x2;
      }
    }
    return (int)x0;
  }
}
