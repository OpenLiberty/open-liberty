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

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.ws.sib.matchspace.SimpleTest;

/** An extension to SimpleMatcher for all Identifier types that support equality tests
 * (this means all types except BOOLEAN).   This implementation serves as the concrete
 * class for Identifiers of type UNKNOWN and OBJECT.  Subclasses handle types STRING,
 * TOPIC, BOOLEAN, and NUMERIC.
 */
class EqualityMatcher extends SimpleMatcher {

  // Standard trace boilerplate
  private static final Class cclass = EqualityMatcher.class;
  private static Trace tc = TraceUtils.getTrace(EqualityMatcher.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  // The initial size to make the 'children' Map
  private static final int INITIAL_CHILDREN_CAPACITY = 8;
  
  // The Map from values to exact-match children.  This is not instantiated until first
  // needed, because, if this Matcher is the root and cacheing is active, this Matcher
  // will never see equality matches at all.
  private Map children;
  
  // An indicator that cacheing is active
  private boolean cacheing;

  /** Make a new EqualityMatcher
   * @param id the identifier to use
   */
  EqualityMatcher(Identifier id) {
    super(id);
  }

  // Implement handlePut.
  void handlePut(
    SimpleTest test,
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr)
    throws MatchingException {
      if (tc.isEntryEnabled())
        tc.entry(this,cclass, "handlePut",
          new Object[] {test,selector,object,subExpr} );
      Object value = test.getValue();
      if (value == null)
        throw new IllegalStateException();
      handleEqualityPut(value, selector, object, subExpr);
      if (tc.isEntryEnabled())
        tc.exit(this,cclass, "handlePut");        
  }

  /** Perform the handlePut function when the test is an equality test
   * @param value the value of the SimpleTest's comparand, perhaps wrapped
   *    (e.g. by NumericMatcher)
   * @param selector the Conjunction representing all or part of a selector
   * @param object the MatchTarget to install
   * @param subExpr the InternTable used to assign unique ids to subexpressions
   */
  void handleEqualityPut(Object value,  Conjunction selector,
      MatchTarget object, InternTable subExpr) throws MatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "handleEqualityPut",
        new Object[] {value,selector,object,subExpr} );
    ContentMatcher next = 
      (ContentMatcher) ((children == null) ? null : children.get(value));
    ContentMatcher newNext = nextMatcher(selector, next);
    if (newNext != next)
    {
      if (children == null)
        children = new HashMap(INITIAL_CHILDREN_CAPACITY);
      children.put(value, newNext);
    }
    newNext.put(selector, object, subExpr);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "handleEqualityPut");        
  }

  // Implement handleGet
  void handleGet(
    Object value,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException {
      if (tc.isEntryEnabled())
        tc.entry(this,cclass, "handleGet",
          new Object[] {value,msg,cache,contextValue,result} );
      handleEqualityGet(value, msg, cache, contextValue, result);
      if (tc.isEntryEnabled())
        tc.exit(this,cclass, "handleGet");        
  }

  /** Perform the handleGet function for all equality tests in this matcher (may
   *   need to be combined with other logic to cover other kinds of tests)
   * @param value the value of the Identifier, perhaps wrapped (e.g., by NumericMatcher)
   * @param msg the MatchSpaceKey to pass on to other matchers
   * @param cache the EvalCache used to avoid redundent evaluation
   * @param result the SearchResult object to receive the results
   */
  void handleEqualityGet(Object value, MatchSpaceKey msg,
      EvalCache cache, Object contextValue, SearchResults result) throws MatchingException,
        BadMessageFormatMatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "handleEqualityGet",
        new Object[] {value,msg,cache,contextValue,result} );
    if (children == null)
      return;
    ContentMatcher next = (ContentMatcher) children.get(value);
    if (next != null)
    {
      next.get(null, msg, cache, contextValue, result);
    }
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "handleEqualityGet");        
  }

  // Implement handleRemove
  void handleRemove(
    SimpleTest test,
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr,
    OrdinalPosition parentId)
    throws MatchingException {
      if (tc.isEntryEnabled())
        tc.entry(this,cclass, "handleRemove",
          new Object[] {test,selector,object,subExpr} );
      Object value = test.getValue();
      if (value == null)
        throw new IllegalStateException();
      handleEqualityRemove(value, selector, object, subExpr, parentId);
      if (tc.isEntryEnabled())
        tc.exit(this,cclass, "handleRemove");        
  }

  /** Perform the handleRemove function when the test is an equality test
   * @param value the value of the SimpleTest's comparand, perhaps wrapped
   *    (e.g. by NumericMatcher)
   * @param selector the Conjunction representing all or part of a selector
   * @param object the MatchTarget to remove
   * @param subExpr the InternTable used to assign unique ids to subexpressions
   */
  void handleEqualityRemove(Object value, Conjunction selector,
      MatchTarget object, InternTable subExpr, OrdinalPosition parentId) throws MatchingException {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "handleEqualityRemove",
        new Object[] {value,selector,object,subExpr} );
    ContentMatcher next = (ContentMatcher) children.get(value);
    ContentMatcher newNext =
      (ContentMatcher) next.remove(selector, object, subExpr, ordinalPosition);
    if (newNext == null)
      children.remove(value);
    else if (newNext != next)
      children.put(value, newNext);
      if (tc.isEntryEnabled())
        tc.exit(this,cclass, "handleEqualityRemove");        
  }
  
  // Override isEmpty to check whether children is empty
  boolean isEmpty() {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "isEmpty");
    boolean ans = super.isEmpty() && !haveEqualityMatches();
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "isEmpty", new Boolean(ans));
    return ans;        
  }

  /** Indicates whether this Matcher has any equality matches
   * @return true if there are any
   */
  boolean haveEqualityMatches() {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "haveEqualityMatches");
    boolean ans = children != null && !children.isEmpty();
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "haveEqualityMatches", new Boolean(ans));
    return ans;
  }
  
  // Override nextMatcher to handle possibility of cacheing
  ContentMatcher nextMatcher(Conjunction selector, ContentMatcher oldMatcher) {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "nextMatcher", "selector: "+selector+", oldMatcher: "+oldMatcher);
    ContentMatcher ans;
    if (!cacheing)
      ans = super.nextMatcher(selector, oldMatcher);
    else if (oldMatcher == null)
      ans = new CacheingMatcher(ordinalPosition, super.nextMatcher(selector, null));   
    else 
      ans = oldMatcher;
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "nextMatcher", ans);
    return ans;
  }
  
  /** Set the cacheing mode of this EqualityMatcher
   * @param b true if cacheing is on for this EqualityMatcher.
   */
  void setCacheing(boolean b) {
    cacheing = b;
  }
}
