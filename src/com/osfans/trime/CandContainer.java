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
public class CandContainer extends LinearLayout {

  private static final int ARROW_ALPHA_ENABLED = 0xff;
  private static final int ARROW_ALPHA_DISABLED = 0x40;

  private CandView candidateView;
  private Button leftArrow;
  private Button rightArrow;
  private Rime mRime;

  public CandContainer(Context context, AttributeSet attrs) {
    super(context, attrs);
    mRime = Rime.getRime();
  }
  
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    candidateView = (CandView) findViewById(R.id.candidate_view);

    leftArrow = (Button) findViewById(R.id.arrow_left);
    leftArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        updatePage(Keyboard.XK_Page_Up);
      }
    });

    rightArrow = (Button) findViewById(R.id.arrow_right);
    rightArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        updatePage(Keyboard.XK_Page_Down);
      }
    });
  }

  public void setCandViewListener(
      CandView.CandViewListener listener) {
    candidateView.setCandViewListener(listener);
  }

  public void updatePage(int keyCode) {
    if (keyCode != 0 ) {
      Trime.getService().onKey(keyCode, null);
    }
    candidateView.update();
    enableArrow(leftArrow, !mRime.isFirst());
    enableArrow(rightArrow, !mRime.isLast());
  }

  private void enableArrow(Button arrow, boolean enabled) {
    arrow.setEnabled(enabled);
    arrow.setAlpha(enabled ? ARROW_ALPHA_ENABLED : ARROW_ALPHA_DISABLED);
  }
}
