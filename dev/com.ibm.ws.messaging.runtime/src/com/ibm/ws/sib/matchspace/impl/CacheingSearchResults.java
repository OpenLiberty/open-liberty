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
import java.util.List;

import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

// SearchResults specialization for doing wildcard lookups and cacheing the result.

class CacheingSearchResults implements SearchResults
{
  private static final Class cclass = CacheingSearchResults.class;
  private static Trace tc = TraceUtils.getTrace(CacheingSearchResults.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  
  private SearchResults delegate;
  private List wildMatchers;
  boolean hasContent;

  /** Constructor */
  CacheingSearchResults(SearchResults delegate)
  {
    if (tc.isEntryEnabled())
      tc.entry(cclass, "CacheingMatcher", "delegate: " + delegate);

    this.delegate = delegate;
    wildMatchers = new ArrayList();

    if (tc.isEntryEnabled())
      tc.exit(cclass, "CacheingSearchResults", this);
  }

  /*
   * Adds a reference to an object list to the result set's list
   * of object lists.
   *
   * @param objects an array of FastVectors of objects, indexed by type
   */
  public void addObjects(List[] objects)
  {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "addObjects", "objects: " + objects);

    if (delegate != null)
      delegate.addObjects(objects);

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "addObjects");
  }

  // Cache methods do nothing for this type
  public Object provideCacheable(Object key)
  {
    if (tc.isDebugEnabled())
      tc.debug(this,cclass, "provideCacheable", key);
    return null;
  }
  public boolean acceptCacheable(Object cached)
  {
    if (tc.isDebugEnabled())
      tc.debug(this,cclass, "acceptCacheable", cached);
    return false;
  }

  /** Caches a matcher */

  void setMatcher(ContentMatcher m)
  {

    if (tc.isEntryEnabled())
      tc.entry(
        this,cclass,
        "setMatcher",
        "matcher: " + m + ", hasContent: " + new Boolean(hasContent));

    wildMatchers.add(m);
    this.hasContent |= m.hasTests();

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "setMatcher");
  }

  /** Reset method (allowed to be a no-op since this is a special purpose object not used
   * for any 'real' MatchSpace searches) */

  public void reset()
  {
  }

  /** Returns the Matcher cache as an array */

  ContentMatcher[] getMatchers()
  {
    if (tc.isEntryEnabled())
      tc.entry(this,cclass, "getMatchers");

    //  ans removed as it is never used !
    //    Matcher[] ans = new Matcher[wildMatchers.size()];

    if (tc.isEntryEnabled())
      tc.exit(this,cclass, "getMatchers");
    return (ContentMatcher[]) wildMatchers.toArray(new ContentMatcher[0]);

  }
}
