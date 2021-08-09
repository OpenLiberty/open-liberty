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
/** The Factory class contains subroutines used by ContentMatcher put methods.  These
 * either create ContentMatchers or find SimpleTests within Conjunctions.
 **/

package com.ibm.ws.sib.matchspace.impl;

// Import required classes.
import com.ibm.ws.sib.matchspace.selector.impl.ExtendedSimpleTestImpl;
import com.ibm.ws.sib.matchspace.selector.impl.OrdinalPosition;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.Conjunction;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.SimpleTest;

final class Factory
{
  private Factory()
  {
  }

  // Standard trace boilerplate
  private static final Class cclass = Factory.class;
  private static Trace tc = TraceUtils.getTrace(Factory.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  /** "Create" a new ContentMatcher given the ordinal position of the parent and the child
   * Matcher that currently occupies the place in which the new ContentMatcher will be
   * placed.  Sometimes no new ContentMatcher is created because the old child Matcher is
   * either usable as is or needs to have the new ContentMatcher created below it.  In
   * that case, the old Matcher is returned.
   *
   * @param lastOrdinalPosition the ordinal position of the parent of the new Matcher.
   *
   * @param selector the Conjunction representing the subscription that is the occasion of
   * creating this new Matcher.
   *
   * @param oldMatcher the current child whose place may be usurped by the new Matcher, or
   * null if no current child exists.  The oldMatcher will become the vacantChild if a new
   * Matcher is created.
   *
   * @return the new occupant of the child position occupied by oldMatcher; this may still
   * be oldMatcher if no new Matcher was created or else a new Matcher, with oldMatcher as
   * its vacantChild.
   **/

  static ContentMatcher createMatcher(
    OrdinalPosition lastOrdinalPosition,
    Conjunction selector,
    ContentMatcher oldMatcher)
  {
    if (tc.isEntryEnabled())
      tc.entry(
        cclass,
        "createMatcher",
        "lastOrdinalPosition: "
          + lastOrdinalPosition
          + ",selector: "
          + selector
          + ", oldmatcher: "
          + oldMatcher);

    // If oldMatcher is not null and is a DifficultMatcher, the new Matcher must be
    // created under it, so we just return it.

    if (oldMatcher instanceof DifficultMatcher)
    {
      if (tc.isDebugEnabled())
        tc.debug(
          cclass,
          "createMatcher",
          "Reusing old DifficultMatcher with position: "
            + oldMatcher.ordinalPosition
            + " for position "
            + lastOrdinalPosition);
      if (tc.isEntryEnabled())
        tc.exit(cclass, "createMatcher");
      return oldMatcher;
    }

    // If any SimpleTests are supplied, we find the SimpleTest (if any) whose
    // ordinalPosition is the least one greater than lastOrdinalPosition.  This gives us
    // the Identifier that is the next to be considered.  Note that oldMatcher's
    // ordinalPosition cannot be less than lastOrdinalPosition.

    if (oldMatcher != null && oldMatcher.ordinalPosition.compareTo(lastOrdinalPosition) < 0)
      throw new IllegalStateException();
    if (selector != null)
      for (int i = 0; i < selector.getSimpleTests().length; i++)
      {
        OrdinalPosition newPos = (OrdinalPosition) selector.getSimpleTests()[i].getIdentifier().getOrdinalPosition();

        if (oldMatcher != null && newPos.compareTo(oldMatcher.ordinalPosition) >= 0)
        {
          // (hence newPos > lastOrdinalPosition as well).  No new Matcher should be
          // created now because oldMatcher is either the Matcher we want or the new one
          // should be created somewhere under it.

          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "Reusing "
                + oldMatcher.getClass().getName()
                + " for position: "
                + lastOrdinalPosition
                + "; next test is at: "
                + newPos);
          if (tc.isEntryEnabled())
            tc.exit(cclass, "createMatcher");
          return oldMatcher;
        }

        if (newPos.compareTo(lastOrdinalPosition) > 0)
        {
          // (but newPos < oldMatcher.ordinalPosition if oldMatcher exists at all).  A new
          // Matcher must be created and oldMatcher becomes its vacantChild.

          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "Creating new matcher at position " + newPos);
          if (tc.isEntryEnabled())
            tc.exit(cclass, "createMatcher");
          return createMatcher(selector.getSimpleTests()[i], oldMatcher);
        }
      }

    // If no SimpleTests were supplied or there are none whose ordinalPosition is beyond
    // lastOrdinalPosition, we must return a DifficultMatcher.  We already know that
    // oldMatcher is not a DifficultMatcher (or we wouldn't have gotten this far), so the
    // new one must be interposed ahead of oldMatcher.

    ContentMatcher ans = new DifficultMatcher(lastOrdinalPosition);

    if (tc.isDebugEnabled())
    {
      if (oldMatcher != null)
        tc.debug(
          cclass,
          "createMatcher",
          "New DifficultMatcher at position "
            + lastOrdinalPosition
            + " with successor "
            + oldMatcher.getClass().getName()
            + " at position "
            + oldMatcher.ordinalPosition);
      else
        tc.debug(
          cclass,
          "createMatcher",
          "New DifficultMatcher at position "
            + lastOrdinalPosition
            + " with null successor.");
    }

    ans.vacantChild = oldMatcher;

    if (tc.isEntryEnabled())
      tc.exit(cclass, "createMatcher");
    return ans;
  }

  // Subroutine to determine the kind of Matcher to create and create it.

  private static ContentMatcher createMatcher(
    SimpleTest test,
    ContentMatcher oldMatcher)
  {
    if (tc.isEntryEnabled())
      tc.entry(
        cclass,
        "createMatcher",
        "test: " + test + ", oldmatcher: " + oldMatcher);
    Identifier id = test.getIdentifier();
    boolean isExtended = (test instanceof ExtendedSimpleTestImpl);
    ContentMatcher ans;
    switch (id.getType())
    {
      case Selector.BOOLEAN :
        if(!isExtended)
        {
          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "New BooleanMatcher for id " + id.getName());
          ans = new BooleanMatcher(id);
        }
        else
        {
          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "New SetValBooleanMatcher for id " + id.getName());
          ans = new SetValBooleanMatcher(id);          
        }
        break;
      case Selector.UNKNOWN :
      case Selector.OBJECT :
        if (tc.isDebugEnabled())
          tc.debug(
            cclass,
            "createMatcher",
            "New EqualityMatcher for id " + id.getName());
        ans = new EqualityMatcher(id);
        break;
      case Selector.STRING :
      case Selector.TOPIC :
        if(!isExtended)
        {        
          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "New StringMatcher for id " + id.getName());
          ans = new StringMatcher(id);
        }
        else
        {
          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "New SetValStringMatcher for id " + id.getName());
          ans = new SetValStringMatcher(id);          
        }          
        break;
      case Selector.CHILD :
        if (tc.isDebugEnabled())
          tc.debug(
            cclass,
            "createMatcher",
            "New ExtensionMatcher for id " + id.getName());
        ans = new SetValChildAccessMatcher(id);
        break;        
      default :
        if(!isExtended)
        {             
          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "New NumericMatcher for id " + id.getName());
          ans = new NumericMatcher(id);
        }
        else
        {
          if (tc.isDebugEnabled())
            tc.debug(
              cclass,
              "createMatcher",
              "New SetValNumericMatcher for id " + id.getName());
          ans = new SetValNumericMatcher(id);          
        }
    }
    
    ans.vacantChild = oldMatcher;

    if (tc.isEntryEnabled())
      tc.exit(cclass, "createMatcher");
    return ans;
  }

  /** Find the SimpleTest for a given ordinalPosition if present in a Conjunction
   *
   * @param ordinalPosition the position to search for
   *
   * @param selector the Conjunction in which to search
   *
   * @return the requisite SimpleTest if found, null otherwise
   **/

  static SimpleTest findTest(OrdinalPosition ordinalPosition, Conjunction selector)
  {
    if (selector == null)
      return null;
    for (int i = 0; i < selector.getSimpleTests().length; i++)
    {
      SimpleTest cand = selector.getSimpleTests()[i];
      OrdinalPosition candOrdinalPosition = (OrdinalPosition) cand.getIdentifier().getOrdinalPosition();
      if (candOrdinalPosition.equals(ordinalPosition))
        return cand;
    }
    return null;
  }
}
