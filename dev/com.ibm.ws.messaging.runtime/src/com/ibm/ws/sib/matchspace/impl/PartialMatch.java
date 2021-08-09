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

// Import required classes.
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.SearchResults;

/** A Matcher-like data structure that is used within a StringMatcher for a single
 * identifier.  Each PartialMatch captures a portion of a String LIKE pattern.  The
 * interface differs from ContentMatcher in detail but a PartialMatch tree can intervene
 * between a StringMatcher and its ContentMatcher children.
 */
class PartialMatch
{

  // Standard trace boilerplate
  private static final Class cclass = PartialMatch.class;
  private static Trace tc = TraceUtils.getTrace(PartialMatch.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  /** The PartialMatch objects that are peers within the tree form a chain ordered by
   * increasing key length.  The first peer always has zero chars and is called the anchor.
   */
  PartialMatch next;
  
  /** The owning StringMatcher */
  StringMatcher owner;
  
  /** Each PartialMatch covers a sequence of characters that must be present in the String,
   * called a "key" here. 
   */
  char[] key;
  
  /** The children for which this PartialMatch is followed by a "final" matchMany (either
   * the end of the prefix when there is no suffix or the end of the suffix). We retain this
   * Matcher to provide an optimised "common case" codepath.
   */
  ContentMatcher singleMatchManyChild;
  
  /** A list of the children for which this PartialMatch is followed by more than one matchMany
   * wildcard. We need a map, because we may have expressions like A//C//E and A//Z//E each of which
   * has a common prefix and suffix but a different set of midClauses.
   */
  protected List matchManyChildren;
  
  /** The children for which this PartialMatch is followed by a suffix evaluation */
  PartialMatch suffix;

  /** The children for which this PartialMatch is followed by a matchOne.
   */
  PartialMatch matchOneChild;
  
  /** The children for which this PartialMatch is the final match and exhausts all the
   * characters in the string.
   */
  ContentMatcher exactChild;
  
  /**
   * This class wraps a pattern and content matcher pair.
   *
   */
  private class MatchManyWrapper
  {
    // The pattern associated with the child matcher
    PatternWrapper pattern;
    // The child matcher
    ContentMatcher matcher;

    
    /**
     * Construct a new MatchManyWrapper
     * 
     * @param pattern
     * @param matcher
     */
    public MatchManyWrapper(PatternWrapper pattern, ContentMatcher matcher) 
    {
      this.pattern = pattern;
      this.matcher = matcher;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) 
    {
      if (o instanceof MatchManyWrapper) 
      {
        MatchManyWrapper other = (MatchManyWrapper) o;
        
        if (pattern.equals(other.pattern))
          return true;
        else
          return false;
      }
      else
        return false;
    }    
  }
  
  /** Construct the first PartialMatch in a chain (the anchor, which always has zero 
   * characters) */
  PartialMatch(StringMatcher owner)
  {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "PartialMatch", "owner: " + owner);
    key = new char[0];
    this.owner = owner;
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "PartialMatch");
  }

  /** Overrideable wrapper for the constructor */
  PartialMatch newPartialMatch() {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "newPartialMatch");
    PartialMatch ans = new PartialMatch(owner);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "newPartialMatch", ans);
    return ans;
  }

  // Internal Constructor (for everything but the anchor)
  PartialMatch(char[] key, PartialMatch next, StringMatcher owner)
  {

    if (tc.isEntryEnabled())
      tc.entry(cclass, "PartialMatch", "key: " + new String(key) + " next: " + next +
        " owner: " + owner);

    this.key = key;
    this.next = next;
    this.owner = owner;

    if (tc.isEntryEnabled())
      tc.exit(cclass, "PartialMatch", this);
  }

  /** An overrideable wrapper for the internal constructor */
  PartialMatch newPartialMatch(char[] key, PartialMatch pm) {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "newPartialMatch", "key: "+new String(key)+", pm:"+pm);
    PartialMatch ans = new PartialMatch(key, pm, owner);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "newPartialMatch", ans);
    return ans;
  }

  // Find or create the appropriate entry for a particular key.  We assume this method is 
  // called on the zero keylen entry (first in the chain).
  PartialMatch findOrCreate(char[] key)
  {

    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "findOrCreate", "key: " + new String(key));

    for (PartialMatch pm = this;; pm = pm.next)
    {
      if (Arrays.equals(key, pm.key))
      {
        if (tc.isEntryEnabled())
          tc.exit(this,cclass, "findOrCreate", "pm: " + pm);
        return pm;
      }
      if (pm.next == null || pm.next.key.length > key.length)
      {
        // We didn't get an exact key match, so we need to make a new PartialMatch
        // structure
        pm.next = newPartialMatch(key, pm.next);

        if (tc.isEntryEnabled())
          tc.exit(this,cclass, "findOrCreate", "pm.next: " + pm.next);

        return pm.next;
      }
    }
  }
  
  // Remove empty entries from a chain (except for the anchor, which is never removed)
  void cleanChain()
  {

    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "cleanChain");

    for (PartialMatch p = this; p.next != null; )
      if (p.next.isEmpty())
        p.next = p.next.next;
      else
        p = p.next;

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "cleanChain");
  }

  // Tests for emptiness.  The anchor (keylen == 0) is never considered empty
  boolean isEmpty()
  {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "isEmpty");

    boolean manyChildResult = singleMatchManyChild == null && 
                                (matchManyChildren == null || matchManyChildren.isEmpty());
    boolean result = manyChildResult && matchOneChild == null && 
      exactChild == null && suffix == null;

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "isEmpty", "result: " + new Boolean(result));

    return result;
  }

  // Tests for emptiness of the entire chain (called on the anchor)
  boolean isEmptyChain()
  {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "isEmptyChain");

    boolean result = next == null && isEmpty();

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "isEmptyChain", "result: " + new Boolean(result));
    return result;
  }

  /** Put a new Pattern or part thereof into this PartialMatch or its down-chain peers
   *    
   * @param pattern the pattern being considered, wrapped in a PatternWrapper
   * @param selector the Conjunction from the original ContentMatcher.put
   * @param object the MatchTarget from the original ContentMatcher.put
   * @param subExpr the InternTable from the original ContentMatcher.put
   */
  void put(PatternWrapper pattern, Conjunction selector, MatchTarget object,
      InternTable subExpr) throws MatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "put", new Object[]{pattern,selector,object,subExpr});
    switch (pattern.getState()) {
      case PatternWrapper.FINAL_EXACT:
        exactChild = owner.nextMatcher(selector, exactChild);
        exactChild.put(selector, object, subExpr);
        break;
      case PatternWrapper.FINAL_MANY:
        if(hasMidClauses(pattern))
        {
          // We have multiple multi-level wildcarding and need to work with the
          // matchManyChildren list
          
          ContentMatcher next = null;
          MatchManyWrapper wrapper= findMatchManyWrapper(pattern);
          if(wrapper != null)
            next = wrapper.matcher;
          ContentMatcher newNext = owner.nextMatcher(selector, next);
          if (newNext != next)
          {
            if (matchManyChildren == null)
              matchManyChildren = new ArrayList(3); // 3 is somewhat arbitrary
            MatchManyWrapper matchManyElement = new MatchManyWrapper(pattern, newNext);
            matchManyChildren.add(matchManyElement);
          }
          newNext.put(selector, object, subExpr);          
        }
        else
        {
          // Simpler case where there is a single multi-level wildcard
          singleMatchManyChild = owner.nextMatcher(selector, singleMatchManyChild);
          singleMatchManyChild.put(selector, object, subExpr);
        }
        break;
      case PatternWrapper.PREFIX_CHARS:
      case PatternWrapper.SUFFIX_CHARS:
        PartialMatch pm = findOrCreate(pattern.getChars());
        pm.put(pattern, selector, object, subExpr);
        break;
      case PatternWrapper.SKIP_ONE_PREFIX:
      case PatternWrapper.SKIP_ONE_SUFFIX:
        if (matchOneChild == null)
          matchOneChild = newPartialMatch();
        pattern.advance();
        matchOneChild.put(pattern, selector, object, subExpr);
        break;
      case PatternWrapper.SWITCH_TO_SUFFIX:
        if (suffix == null)
          suffix = newPartialMatch();
        pattern.advance();
        suffix.put(pattern, selector, object, subExpr);
        break;
    }
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "put");
  }
  
  /** Remove a Pattern or part thereof from this PartialMatch or its down-chain peers
   * @param pattern the Pattern being considered, wrapped in a PatternWrapper
   * @param selector the Conjunction from the original ContentMatcher.remove
   * @param object the MatchTarget from the original ContentMatcher.remove
   * @param subExpr the InternTable from the original ContentMatcher.remove
   */
  void remove(PatternWrapper pattern, Conjunction selector, MatchTarget object,
      InternTable subExpr, OrdinalPosition parentId) throws MatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "remove", new Object[]{pattern,selector,object,subExpr});
    switch (pattern.getState()) {
      case PatternWrapper.FINAL_EXACT:
        exactChild = exactChild.remove(selector, object, subExpr, parentId);
        break;
      case PatternWrapper.FINAL_MANY:
        if(hasMidClauses(pattern))
        {
          // We have multiple multi-level wildcarding and need to work with the
          // matchManyChildren list
          MatchManyWrapper wrapper = findMatchManyWrapper(pattern);
          // if we couldn't find the wrapper, throw an exception 
          if (wrapper == null)
            throw new MatchingException();
          ContentMatcher next = wrapper.matcher;
          ContentMatcher newNext =
            (ContentMatcher) next.remove(selector, object, subExpr, parentId);
          if (newNext == null)
            matchManyChildren.remove(wrapper);
          else if (newNext != next)
          {
            MatchManyWrapper newWrapper = new MatchManyWrapper(pattern, newNext);
            matchManyChildren.add(newWrapper);
          }
        }
        else
        {
          // Simpler case where there is a single multi-level wildcard
          singleMatchManyChild = 
            singleMatchManyChild.remove(selector, object, subExpr, parentId);
        }        
        
        break;
      case PatternWrapper.PREFIX_CHARS:
      case PatternWrapper.SUFFIX_CHARS:
        char[] chars = pattern.getChars();
        for (PartialMatch pm = this ;; pm = pm.next) {
          if (pm == null)
            throw new MatchingException();
          if (Arrays.equals(chars, pm.key)) {
            pm.remove(pattern, selector, object, subExpr, parentId);
            break;
          }
        }
        break;
      case PatternWrapper.SKIP_ONE_PREFIX:
      case PatternWrapper.SKIP_ONE_SUFFIX:
        pattern.advance();
        matchOneChild.remove(pattern, selector, object, subExpr, parentId);
        if (matchOneChild.isEmptyChain())
          matchOneChild = null;
        break;
      case PatternWrapper.SWITCH_TO_SUFFIX:
        pattern.advance();
        suffix.remove(pattern, selector, object, subExpr, parentId);
        if (suffix.isEmptyChain())
          suffix = null;
        break;
    }
    if (key.length == 0)
      cleanChain();
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "remove");
  }

  /** Perform get operation on this PartialMatch and its descendents
   * @param chars the array of characters containing the value of the Identifier
   * @param start the start character within chars
   * @param length the number of characters to consider within chars
   * @param invert perform suffix matching (examine the end of the range first)
   * @param msg the MatchSpaceKey to be passed to ContentMatchers lower in the tree
   * @param cache the EvalCache to be passed to ContentMatchers lower in the tree
   * @param result the SearchResults to accumulate any results
   */
  void get(char[] chars, int start, int length, boolean invert, MatchSpaceKey msg,
      EvalCache cache, Object contextValue, SearchResults result) 
      throws MatchingException, BadMessageFormatMatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "get", new Object[]{chars,new Integer(start),new Integer(length),
        new Boolean(invert),msg,cache,result});
   examineMatches:
    for (PartialMatch pm = this; pm != null && length >= pm.key.length; pm = pm.next) {
      int origin = invert ? (start + length - pm.key.length) : start;
      for (int i = 0; i < pm.key.length; i++)
        if (pm.key[i] != chars[origin + i])
          continue examineMatches;
      // A match on pm; either it exhausts chars (exact) or only part thereof
      if (length == pm.key.length) {
        // An exact match fires both exactChild and matchManyChild
        if (pm.exactChild != null)
          pm.exactChild.get(null, msg, cache, contextValue, result);
        // Process just the default MatchManyChild
        if (pm.singleMatchManyChild != null)
          pm.singleMatchManyChild.get(null, msg, cache, contextValue, result);        
      }
      else
        if (!invert)
          pm.doPartialGet(chars, start + pm.key.length, length-pm.key.length, false, 
            msg, cache, contextValue, result);
        else
          pm.doPartialGet(chars, start, length-pm.key.length, true, msg, cache, contextValue, result);
    }
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "get");
  }

  /** Take action when a portion of a String matches this PartialMatch but is not
   *   exhausted by it
   * @param chars the characters containing the value
   * @param start the start of the range of characters remaining after the partial match
   * @param length the number of characters remaining after the partial match (> 0)
   * @param invert perform suffix matching (examine the end of the range first)
   * @param msg MatchSpaceKey to be passed to lower ContentMatchers
   * @param cache EvalCache to be passed to lower ContentMatchers
   * @param result SearchResults to be passed to lower ContentMatchers
   */
  void doPartialGet(char[] chars, int start, int length, boolean invert, MatchSpaceKey msg,
      EvalCache cache, Object contextValue, SearchResults result)
      throws MatchingException, BadMessageFormatMatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "doPartialGet", new Object[] {chars,new Integer(start),
        new Integer(length),new Boolean(invert),msg,cache,result});
    if (matchOneChild != null)
      if (!invert)
        matchOneChild.get(chars, start+1, length-1, false, msg, cache, contextValue, result);
      else
        matchOneChild.get(chars, start, length-1, true, msg, cache, contextValue, result);
    if (suffix != null)
      suffix.get(chars, start, length, true, msg, cache, contextValue, result);
    getFromManyChildMatchers(chars, start, length, msg, cache, contextValue, result);    
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "doPartialGet");
  }
  
  public boolean hasMidClauses(PatternWrapper pattern)
  {
    return pattern.hasMidClauses(); 
  }

  /**
   * In the case where subscriptions have been made with multiple multi-level
   * wildcards, this method drives MatchSpace get() processing against the 
   * corresponding Matchers.
   *  
   * @param chars
   * @param start
   * @param length
   * @param msg
   * @param cache
   * @param contextValue
   * @param result
   * @throws MatchingException
   * @throws BadMessageFormatMatchingException
   */
  public void getFromManyChildMatchers(char[] chars, int start, int length, MatchSpaceKey msg,
      EvalCache cache, Object contextValue, SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException 
  {
    if (singleMatchManyChild != null)
      singleMatchManyChild.get(null, msg, cache, contextValue, result);
    
    if (matchManyChildren != null)
    {
      // We have some subscriptions with multiple multi-level wildcards
      // We accept that these simply will be a little slow to process.
      // Iterate over the matchManyChildren
      Iterator iter = matchManyChildren.iterator();

      while(iter.hasNext())
      {
        MatchManyWrapper nextElement = (MatchManyWrapper) iter.next();
        PatternWrapper nextPattern = nextElement.pattern;
        if(nextPattern.matchMidClauses(chars, start, length))
        {
          ContentMatcher next = nextElement.matcher;
          if (next != null)
          {
            next.get(null, msg, cache, contextValue, result);
          }
        }
      } // eof while more children in the keyset
    }
  }
  
  /**
   * Locates a wrapped pattern+matcher pair where we've been give a pattern.
   * 
   * @param pattern
   * @return
   */
  private MatchManyWrapper findMatchManyWrapper(PatternWrapper pattern)
  {
    MatchManyWrapper wrapper = null;
    if (matchManyChildren != null)
    {
      // Iterate over the matchManyChildren
      Iterator iter = matchManyChildren.iterator();

      while(iter.hasNext())
      {
        MatchManyWrapper nextElement = (MatchManyWrapper) iter.next();
        PatternWrapper nextPattern = nextElement.pattern;
        if(nextPattern.equals(pattern))
        {
          // We're done, we've found the matching pattern
          wrapper = nextElement;
          break;
        }
      } // eof while more children in the list
    }
    return wrapper;
  }  
}
