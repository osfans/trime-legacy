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
import android.database.Cursor;

import java.util.ArrayList;

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
  private Cursor cursor;
  private boolean highlightDefault;
  private int currentWordCount;
  private int currentWordSkip;
  private Dictionary dialectDictionary;

  public CandidatesContainer(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    candidateView = (CandidateView) findViewById(R.id.candidate_view);

    leftArrow = (Button) findViewById(R.id.arrow_left);
    leftArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        movePage(-1);
      }
    });

    rightArrow = (Button) findViewById(R.id.arrow_right);
    rightArrow.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        movePage(1);
      }
    });
  }

  public void setCandidateViewListener(
      CandidateView.CandidateViewListener listener) {
    candidateView.setCandidateViewListener(listener);
  }
  
  public void setCandidates(Cursor words, boolean highlightDefault, Dictionary dialectDictionary) {
    // All the words will be split into pages and shown in the candidate-view.
    if (cursor != null) cursor.close();
    cursor = words;
    this.highlightDefault = highlightDefault;
    currentWordCount = 0;
    currentWordSkip = 0;
    movePage(0);
    this.dialectDictionary = dialectDictionary;
  }

  public boolean pickHighlighted(int index) {
    if (index >= currentWordCount) return false;
    return candidateView.pickHighlighted(index);
  }
 
	private boolean isFirst() {
		return (cursor != null) && cursor.isFirst();
	}

	private boolean isLast() {
		return (cursor != null) && (cursor.getPosition() + currentWordSkip + currentWordCount >= cursor.getCount());
	}

  private void movePage(int direction) {
    if (cursor == null || cursor.getCount() == 0) {
      candidateView.setCandidates(null);
      enableArrow(leftArrow, false);
      enableArrow(rightArrow, false);
    } else {
      candidateView.setCandidates(getCandidates(direction));
      if (highlightDefault) candidateView.highlightDefault();
      enableArrow(leftArrow, !isFirst());
      enableArrow(rightArrow, !isLast());
    }
  }

  private String[] getCandidates(int direction) {
    if ((direction > 0 && isLast()) || (direction < 0 && isFirst()) ) {
            currentWordCount = 0;
            currentWordSkip = 0;
            return null;
    }
    
    int p = 0;
    if (direction > 0 && currentWordCount + currentWordSkip> 0)  {
        cursor.move(currentWordCount + currentWordSkip);
        p = cursor.getPosition();
    } else if (direction < 0) cursor.move(-1);
    
    float n = 0;
    ArrayList<String> candidates = new ArrayList<String>();
    int max_len = candidateView.getCandMaxLen();
    int max_num = candidateView.getCandNum();
    currentWordSkip = 0;
    do {
        String word = cursor.getString(0);
        String py = cursor.getColumnCount() > 1 ? dialectDictionary.comment(cursor.getString(1)) : "";
        String s = String.format("%s\t%s", word, py);
        if (candidates.contains(s)) { //單屏去重
             currentWordSkip++;
             continue;
        }
        n += candidateView.len(word);
        if (n > max_len && candidates.size() > 0) {
            if(direction < 0) cursor.move(1);
            break;
        }
        if(direction < 0) candidates.add(0, s);
        else candidates.add(s);
        if (n >= max_len || candidates.size() >= max_num) break;
    } while (cursor.move(direction>=0?1:-1));
    if(direction >=0) cursor.moveToPosition(p);
    else if(direction<0 && cursor.isBeforeFirst()) cursor.moveToFirst();
    currentWordCount = candidates.size(); 
    String[] ret = new String[currentWordCount];
    candidates.toArray(ret);
    return ret;
  }

  private void enableArrow(Button arrow, boolean enabled) {
    arrow.setEnabled(enabled);
    //arrow.setAlpha(enabled ? ARROW_ALPHA_ENABLED : ARROW_ALPHA_DISABLED);
  }

}
