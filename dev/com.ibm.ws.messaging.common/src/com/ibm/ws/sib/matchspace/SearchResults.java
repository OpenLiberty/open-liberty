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
package com.ibm.ws.sib.matchspace;

import java.util.List;

/**
 * This class provides a trivial abstract base class for building a
 * traversal's result set.
 *
 */
public interface SearchResults
{
  /*
   * Adds a reference to an object list to the result set's list
   * of object lists.
   *
   * @param objects an array FastVectors of objects, indexed by type
   */
  public abstract void addObjects(List[] objects);

  /*
   * Provides something that can potentially short-circuit the entire search.
   *
   * @param rootIdVal the value of the root Identifier for which the result set is built
   */
  public abstract Object provideCacheable(Object rootIdVal)
    throws MatchingException;

  /* Accepts something that was previously cached.  Only called when the match
   * circumstances are identical (the same topic being matched with no intervening changes
   * in subscriptions).
   *
   * @param cached the cached Object to be reused
   *
   * @return true if the cached object is acceptable.  We assume the primary reason for
   * returning false would be that the cached object is not of the expected type, which
   * can happen if the same MatchSpace is searched by two different kinds of SearchResults
   * object, both of which are trying to do caching.
   * */
  public abstract boolean acceptCacheable(Object cached);

  /** Discard contents preparatory to starting over again */

  public abstract void reset();
}
