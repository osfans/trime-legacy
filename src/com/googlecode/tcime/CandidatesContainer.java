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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * Contains all candidates in pages where users could move forward (next page)
 * or move backward (previous) page to select one of these candidates. 
 */
public class CandidatesContainer extends LinearLayout {

  private static final int ARROW_ALPHA_ENABLED = 0xff;
  private static final int ARROW_ALPHA_DISABLED = 0x40;

  private CandidateView candidateView;
  private ImageButton leftArrow;
  private ImageButton rightArrow;
  private String words;
  private boolean highlightDefault;
  private int currentPage;
  private int pageCount;

  public CandidatesContainer(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    candidateView = (CandidateView) findViewById(R.id.candidate_view);

    leftArrow = (ImageButton) findViewById(R.id.arrow_left);
    leftArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showPage(currentPage - 1);
      }
    });

    rightArrow = (ImageButton) findViewById(R.id.arrow_right);
    rightArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showPage(currentPage + 1);
      }
    });
  }

  public void setCandidateViewListener(
      CandidateView.CandidateViewListener listener) {
    candidateView.setCandidateViewListener(listener);
  }
  
  public void setCandidates(String words, boolean highlightDefault) {
    // All the words will be split into pages and shown in the candidate-view.
    this.words = words;
    this.highlightDefault = highlightDefault;
    pageCount = getPageCount();
    showPage(0);
  }

  public boolean pickHighlighted() {
    return candidateView.pickHighlighted();
  }

  private void showPage(int page) {
    if (isPageEmpty(page)) {
      candidateView.setCandidates("");
      enableArrow(leftArrow, false);
      enableArrow(rightArrow, false);
    } else {
      final int start = page * CandidateView.MAX_CANDIDATE_COUNT;
      final int end = start + Math.min(
          words.length() - start, CandidateView.MAX_CANDIDATE_COUNT);

      candidateView.setCandidates(words.substring(start, end));
      if (highlightDefault) {
        candidateView.highlightDefault();
      }
      enableArrow(leftArrow, (page > 0) ? true : false);
      enableArrow(rightArrow, (page < pageCount - 1) ? true : false);
    }
    currentPage = page;
  }

  /**
   * Checks if it's an empty page holding no candidates.
   */
  private boolean isPageEmpty(int page) {
    if (page < 0 || page >= pageCount) {
      return true;
    }

    // There are candidates in this page. 
    return false;
  }

  private int getPageCount() {
    return (int) Math.ceil(
        (double) words.length() / CandidateView.MAX_CANDIDATE_COUNT);
  }

  private void enableArrow(ImageButton arrow, boolean enabled) {
    arrow.setEnabled(enabled);
    arrow.setAlpha(enabled ? ARROW_ALPHA_ENABLED : ARROW_ALPHA_DISABLED);
  }

}
