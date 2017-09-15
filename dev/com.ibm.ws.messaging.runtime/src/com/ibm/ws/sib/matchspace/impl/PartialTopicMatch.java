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

import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.MatchSpace;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.SearchResults;

/** Override PartialMatch to tailor behavior for topic strings  
 */
class PartialTopicMatch extends PartialMatch {

  // Standard trace boilerplate
  private static final Class cclass = PartialTopicMatch.class;
  private static Trace tc = TraceUtils.getTrace(PartialTopicMatch.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  /** Constructor */
  PartialTopicMatch(StringMatcher owner) {
    super(owner);
    if (tc.isEntryEnabled())
      tc.entry(cclass, "PartialTopicMatch", "owner: " + owner);
    if (tc.isEntryEnabled())
      tc.exit(cclass, "PartialTopicMatch", this);
  }
  
  // Implement newPartialMatch
  PartialMatch newPartialMatch() {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "newPartialMatch");
    PartialMatch ans = new PartialTopicMatch(owner);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "newPartialMatch", ans);
    return ans;
  }

  // Internal constructor
  private PartialTopicMatch(char[] key, PartialMatch next, StringMatcher owner) {
    super(key, next, owner);
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "PartialTopicMatch", "key: "+new String(key)+", next: "+next+
        ", owner: "+owner);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "PartialTopicMatch");
  }

  // Implement newPartialMatch for internal construction
  PartialMatch newPartialMatch(char[] key, PartialMatch pm) {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "newPartialMatch", "key: "+new String(key)+", pm: "+pm);
    PartialMatch ans = new PartialTopicMatch(key, pm, owner);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "newPartialMatch", ans);
    return ans;
  }

  // Implement doPartialGet
  void doPartialGet(
    char[] chars,
    int start,
    int length,
    boolean invert,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException
   {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "doPartialGet", new Object[]{chars,new Integer(start),
        new Integer(length),new Boolean(invert),msg,cache,result});
    if (matchOneChild != null)
      if (!invert) {
        int skip = skipForward(chars, start, length);
        if (skip > 0)
          matchOneChild.get(chars, start+skip, length-skip, false, msg, cache, contextValue, result);
      }
      else {
        int skip = skipBackward(chars, start, length);
        if (skip > 0)
          matchOneChild.get(chars, start, length-skip, true, msg, cache, contextValue, result);
      }
    // for suffix and matchManyChild, unless we are at the start or end of the chars
    // the next character must be the separator.
    if (invert && start + length < chars.length && 
        chars[start + length - 1] != MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
      ;  // do not process
    else if (!invert && start > 0 && chars[start] != MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
      ; // do not process
    else {
      // We meet the conditions for suffix and matchManyChild
      if (suffix != null)
        suffix.get(chars, start, length, true, msg, cache, contextValue, result);
      
      // Handle the get from the manyChildMatchers
      getFromManyChildMatchers(chars, start, length, msg, cache, contextValue, result);
    }
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "doPartialGet");
  }

  // Skip forward to next separator
  private int skipForward(char[] chars, int start, int length) {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "skipForward", new Object[]{chars,new Integer(start),
        new Integer(length)});
    int ans = length;
    for (int i = 0; i < length; i++)
      if (chars[start+i] == MatchSpace.SUBTOPIC_SEPARATOR_CHAR) {
        ans = i;
        break;
      }
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "skipForward", new Integer(ans));
    return ans;
  }

  // Skip backward to next separator   */
  private int skipBackward(char[] chars, int start, int length) {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "skipBackward", new Object[]{chars,new Integer(start),
        new Integer(length)});
    int ans = length;
    for (int i = 0; i < length; i++)
      if (chars[start+length-i-1] == MatchSpace.SUBTOPIC_SEPARATOR_CHAR) {
        ans = i;
        break;
      }
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "skipBackward", new Integer(ans));
    return ans;
  }
  
  // Override get to rule out NONWILD_MARKER
  void get(
    char[] chars,
    int start,
    int length,
    boolean invert,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    SearchResults result)
      throws MatchingException, BadMessageFormatMatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "get", new Object[]{chars,new Integer(start),new Integer(length),
        new Boolean(invert),msg,cache,result});
    if (start == chars.length || chars[start] != MatchSpace.NONWILD_MARKER)
      super.get(chars, start, length, invert, msg, cache, contextValue, result);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "get");
  }
}
