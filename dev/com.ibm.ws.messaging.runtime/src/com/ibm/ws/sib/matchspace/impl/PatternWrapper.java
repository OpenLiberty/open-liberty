/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.matchspace.impl;

import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.selector.impl.Pattern;

/**  A wrapper for a Pattern that reports its state and provides pieces on demand.
 */
class PatternWrapper {

  // Standard trace boilerplate
  private static final Class cclass = PatternWrapper.class;
  private static Trace tc = TraceUtils.getTrace(PatternWrapper.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  /** The state of the Pattern in which its next item is some prefix characters */
  static final int PREFIX_CHARS = 0;
  
  /** The state of the Pattern in which its next item is some suffix characters */
  static final int SUFFIX_CHARS = 1;
  
  /** The state of the Pattern in which we are either at the end of the prefix or there
   * is no prefix and there is a suffix to switch to
   */ 
  static final int SWITCH_TO_SUFFIX = 2;

  /** The state of the Pattern in which we have exhausted it and there is no matchMany
   * left to process (so the String must be completely consumed).
   */
  static final int FINAL_EXACT = 3;

  /** The state of the Pattern in which we have exhausted it but there is a matchMany
   * left to process (so the String need not be completely consumed).
   */
  static final int FINAL_MANY = 4;

  /** The state of the Pattern in which the next item is a matchOne within the prefix */
  static final int SKIP_ONE_PREFIX = 5;
  
  /** The state of the Pattern in which the next item is a matchOne within the suffix */
  static final int SKIP_ONE_SUFFIX = 6;

  // The Pattern being wrapped
  private Pattern pattern;
  
  // Indicator that we are finished with the prefix
  boolean prefixDone;
  
  // Which part of the prefix or suffix is next to process
  int next;

  /** Wrap a new Pattern */
  PatternWrapper(Pattern pattern) {
    if (tc.isEntryEnabled())
      tc.entry(cclass, "PatternWrapper", pattern);
    this.pattern = pattern;
    if (tc.isEntryEnabled())
      tc.exit(cclass, "PatternWrapper");
  }
  
  /** Get the state of this Pattern (which item is next to process */
  int getState() {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "getState");
    int state;
    if (!prefixDone) {
      // Somewhere in the prefix or at beginning and there is no prefix
      Pattern.Clause prefix = pattern.getPrefix();
      if (prefix == null || next >= prefix.items.length) {
        // There is no prefix or we're at the end of it
        Pattern.Clause suffix = pattern.getSuffix();
        // Answer depends on the suffix
        state = suffix == null ? FINAL_MANY : suffix == prefix ? FINAL_EXACT
          : SWITCH_TO_SUFFIX;
      }
      else
        // We're somewhere in the body of the prefix
        state = prefix.items[next] == Pattern.matchOne ? SKIP_ONE_PREFIX : PREFIX_CHARS;
    }
    else {
      // We're in the suffix
      Pattern.Clause suffix = pattern.getSuffix();
      state = (next < 0) ? FINAL_MANY
        : suffix.items[next] == Pattern.matchOne ? SKIP_ONE_SUFFIX : SUFFIX_CHARS;  
    }
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "getState", new Integer(state));
    return state;
  }
  
  /** Advance the state of this pattern */
  void advance() {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "advance");
    if (!prefixDone) {
      Pattern.Clause prefix = pattern.getPrefix();
      if (prefix == null || next >= prefix.items.length) {
        Pattern.Clause suffix = pattern.getSuffix();
        if (suffix != null && suffix != prefix) {
          // SWITCH_TO_SUFFIX
          prefixDone = true;
          next = suffix.items.length-1;
        }
        // else in one of the FINAL_ states, so do nothing
      }
      else
        // somewhere in the body of the prefix, so advance by incrementing
        next++;
    }
    else
      // somewhere in the suffix, so advance by decrementing
      next--;
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "advance");
  }
  
  /** Get the characters and advance (assumes the pattern is in one of the CHARS states */
  char[] getChars() {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "getChars");
    char[] ans;
    if (prefixDone)
      ans = (char[]) pattern.getSuffix().items[next--];
    else
      ans = (char[]) pattern.getPrefix().items[next++];
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "getChars", new String(ans));
    return ans;
  }
  
  /**
   * Does the underlying pattern have mid clauses or not. If it does then
   * that means that the pattern has multiple multi-level wildcards.
   * 
   * @return
   */
  public boolean hasMidClauses()
  {
    return pattern.hasMidClauses(); 
  }

  /**
   * Test whether the underlying pattern's mid clauses match a set of characters.
   * 
   * @param chars
   * @param start
   * @param length
   * @return
   */
  public boolean matchMidClauses(char[] chars, int start, int length) 
  {
    return pattern.matchMiddle(chars, start, start+length);

  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) 
  {
    if (o instanceof PatternWrapper) 
    {
      PatternWrapper other = (PatternWrapper) o;
      
      if (pattern.equals(other.pattern))
        return true;
      else
        return false;

    }
    else
      return false;
  }
  
}
