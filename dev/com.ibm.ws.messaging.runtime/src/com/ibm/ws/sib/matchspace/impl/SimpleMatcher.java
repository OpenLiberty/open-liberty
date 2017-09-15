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

import com.ibm.ws.sib.matchspace.MatchingException;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;

abstract class SimpleMatcher extends ContentMatcher
{

  /** Identifier tested by this node */

  Identifier id;

  /** Arc to children representing subscriptions requiring this attribute to be NULL */

  protected ContentMatcher nullChild;

  /** Arc to children representing subscriptions requiring this attribute to be non-NULL */

  protected ContentMatcher notNullChild;

  /** Create a new SimpleMatcher for a given Identifier */

  SimpleMatcher(Identifier id)
  {
    super((OrdinalPosition)id.getOrdinalPosition());
    this.id = id;
  }

  /** Implementation of put method (handles nullChild and notNullChild; parent handles
   * vacantChild; handlePut method in subclasses handles everything else).
   **/

  void put(
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr)
    throws MatchingException
  {
    SimpleTest test = Factory.findTest(ordinalPosition, selector);
    if (test == null)
      super.put(selector, object, subExpr);
    else
    {
      ContentMatcher next;
      if (test.getKind() == SimpleTest.NULL)
        next = nullChild = nextMatcher(selector, nullChild);
      else
        if (test.getKind() == SimpleTest.NOTNULL)
          next = notNullChild = nextMatcher(selector, notNullChild);
        else
        {
          handlePut(test, selector, object, subExpr);
          return;
        }
      next.put(selector, object, subExpr);
    }
  }

  /** Implementation of get method (handles nullChild and notNullChild; parent handles
   * vacantChild; handleGet method in subclasses handles everything else).
   **/

  void get(
    Object value,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException
  {
    if (value == null && msg != null)
      value = getValue(msg, contextValue);
    if (value == null)
    {
      if (nullChild != null)
        nullChild.get(null, msg, cache, contextValue, result);
    }
    else
    {
      handleGet(value, msg, cache, contextValue, result);
      if (notNullChild != null)
        notNullChild.get(null, msg, cache, contextValue, result);
    }
    super.get(null, msg, cache, contextValue, result);
  }

  /** Default implementation of getValue(), SetVal subclasses will
   * override 
   * 
   * @param msg
   * @param contextValue
   * @throws MatchingException
   * @throws BadMessageFormatMatchingException
   */
  Object getValue(
      MatchSpaceKey msg,
      Object contextValue)
      throws MatchingException, BadMessageFormatMatchingException
    {
      return msg.getIdentifierValue(id, false, contextValue, false);
    }  
  
  /** Implementation of remove method (handles nullChild and notNullChild; parent handles
   * vacantChild; handleRemove method in subclasses handles everything else).
   **/

  ContentMatcher remove(
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr,
    OrdinalPosition parentId)
    throws MatchingException
  {
    SimpleTest test = Factory.findTest(ordinalPosition, selector);
    if (test == null)
      super.remove(selector, object, subExpr, ordinalPosition);
    else
      if (test.getKind() == SimpleTest.NULL)
        nullChild =
          (ContentMatcher) nullChild.remove(selector, object, subExpr, ordinalPosition);
      else
        if (test.getKind() == SimpleTest.NOTNULL)
          notNullChild =
            (ContentMatcher) notNullChild.remove(
              selector,
              object,
              subExpr,
              ordinalPosition);
        else
          handleRemove(test, selector, object, subExpr, ordinalPosition);
    if (isEmpty())
    {
      if (vacantChild instanceof DifficultMatcher) 
      {
        // reset the vacant child's ordinal position to our parent's, as this SimpleMatcher is
        // to be deleted
        vacantChild.ordinalPosition = parentId;
      }
      return vacantChild;
    }
    else
      return this;
  }

  /** Test whether this Matcher is empty of children other than the vacantChild, and
   * therefore can be deleted. The vacantChild is handled differently because of the
   * "dont-care-chaining" feature: we will delete this Matcher even if it has a
   * vacantChild, and we will simply let this Matcher's parent point directly at the
   * vacantChild.
   **/

  boolean isEmpty()
  {
    return nullChild == null && notNullChild == null;
  }

  // Abstract methods: to be implemented by each subclass
  abstract void handlePut(
    SimpleTest test,
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr)
    throws MatchingException;
  abstract void handleGet(
    Object value,
    MatchSpaceKey msg,
    EvalCache cache,
    Object contextValue,
    SearchResults result)
    throws MatchingException, BadMessageFormatMatchingException;
  abstract void handleRemove(
    SimpleTest test,
    Conjunction selector,
    MatchTarget object,
    InternTable subExpr,
    OrdinalPosition parentId)
    throws MatchingException;
}
