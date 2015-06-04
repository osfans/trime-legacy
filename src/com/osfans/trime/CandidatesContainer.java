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
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.util.Log;

/**
 * Contains all candidates in pages where users could move forward (next page)
 * or move backward (previous) page to select one of these candidates. 
 */
public class CandidatesContainer extends LinearLayout {

  private static final int ARROW_ALPHA_ENABLED = 0xff;
  private static final int ARROW_ALPHA_DISABLED = 0x40;

  private CandidateView candidateView;
  private Button leftArrow;
  private Button rightArrow;
  private Rime mRime;

  public CandidatesContainer(Context context, AttributeSet attrs) {
    super(context, attrs);
    mRime = Rime.getRime();
  }
  
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    candidateView = (CandidateView) findViewById(R.id.candidate_view);

    leftArrow = (Button) findViewById(R.id.arrow_left);
    leftArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        movePage(Keyboard.XK_Page_Up);
      }
    });

    rightArrow = (Button) findViewById(R.id.arrow_right);
    rightArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        movePage(Keyboard.XK_Page_Down);
      }
    });
  }

  public void setCandidateViewListener(
      CandidateView.CandidateViewListener listener) {
    candidateView.setCandidateViewListener(listener);
  }
  
  public void setCandidates(boolean highlightDefault) {
    // All the words will be split into pages and shown in the candidate-view.
    candidateView.highlightDefault(highlightDefault);
    movePage(0);
  }

  public boolean pickHighlighted(int index) {
    return candidateView.pickHighlighted(index);
  }

  private void movePage(int direction) {
    if (direction != 0 ) {
      mRime.onKey(direction);
      candidateView.highlightDefault(true);
    }
    enableArrow(leftArrow, !mRime.isFirst());
    enableArrow(rightArrow, !mRime.isLast());
  }

  private void enableArrow(Button arrow, boolean enabled) {
    arrow.setEnabled(enabled);
    //arrow.setAlpha(enabled ? ARROW_ALPHA_ENABLED : ARROW_ALPHA_DISABLED);
  }
}
