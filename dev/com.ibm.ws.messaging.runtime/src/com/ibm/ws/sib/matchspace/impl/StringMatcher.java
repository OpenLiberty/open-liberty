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
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.selector.impl.LikeOperatorImpl;
import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.selector.impl.Pattern;

public class StringMatcher extends EqualityMatcher
{

  // Standard trace boilerplate
  private static final Class cclass = StringMatcher.class;
  private static Trace tc = TraceUtils.getTrace(StringMatcher.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  // The subtree of PartialMatches under this StringMatcher
  PartialMatch subTree;

  /** Constructor */

  public StringMatcher(Identifier id)
  {
    super(id);
    if (tc.isEntryEnabled())
      tc.entry(cclass, "StringMatcher", "id: " + id);
    if (id.getType() == Selector.TOPIC)
      subTree = new PartialTopicMatch(this);
    else
      subTree = new PartialMatch(this);
    if (tc.isEntryEnabled())
      tc.exit(cclass, "StringMatcher", this);
  }

  //------------------------------------------------------------------------------
  // Method: StringMatcher.handlePut
  //------------------------------------------------------------------------------

  void handlePut(
    SimpleTest test,
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr)
    throws MatchingException
  {
    if (tc.isEntryEnabled())
      tc.entry(
        this,
        cclass,
        "handlePut",
        new Object[] { test, selector, object, subExpr });
    Object value = test.getValue();
    if (value != null)
      handleEqualityPut(value, selector, object, subExpr);
    else {
      Pattern pattern = ((LikeOperatorImpl) test.getTests()[0]).getInternalPattern();
      subTree.put(new PatternWrapper(pattern), selector, object, subExpr);
    }

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "handlePut");
  }

  //------------------------------------------------------------------------------
  // Method: StringMatcher.handleGet
  //------------------------------------------------------------------------------

  void handleGet(
    Object value,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException
  {
    if (tc.isEntryEnabled())
      tc.entry(
        this,
        cclass,
        "handleGet",
        "value: "
          + value
          + "msg: "
          + msg
          + ", context: "
          + contextValue
          + ", result: "
          + result);

    if (!(value instanceof String))
      return;
    char[] chars = ((String) value).toCharArray();
    subTree.get(chars, 0, chars.length, false, msg, cache, contextValue, result);
    if (haveEqualityMatches())
      handleEqualityGet(value, msg, cache, contextValue, result);
    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "handleGet");
  }

  //------------------------------------------------------------------------------
  // Method: StringMatcher.handleRemove
  //------------------------------------------------------------------------------

  void handleRemove(
    SimpleTest test,
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr,
    OrdinalPosition parentId)
    throws MatchingException
  {
    if (tc.isEntryEnabled())
      tc.entry(
        this,
        cclass,
        "handleRemove",
        "test: "
          + test
          + "selector: "
          + selector
          + ", object: "
          + object);

    Object value = test.getValue();
    if (value != null)
      handleEqualityRemove(value, selector, object, subExpr, parentId);
    else {
      Pattern pattern = ((LikeOperatorImpl) test.getTests()[0]).getInternalPattern();
      subTree.remove(new PatternWrapper(pattern), selector, object, subExpr, ordinalPosition);
    }

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "handleRemove");
  }

  //------------------------------------------------------------------------------
  // Method: BooleanMatcher.isEmpty
  //------------------------------------------------------------------------------

  boolean isEmpty()
  {
    return super.isEmpty() && subTree.isEmptyChain();
  }
} // StringMatcher
