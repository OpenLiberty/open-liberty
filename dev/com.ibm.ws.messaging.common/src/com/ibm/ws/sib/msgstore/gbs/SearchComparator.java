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
package com.ibm.ws.sib.msgstore.gbs;

/**
 * A comparator for use by index search.
 *
 * <p>Depending on the the result wanted a comparator may be called on
 * to return a modified version of the truth with respect to any
 * compare.  There is one instance of this base class per index.  It
 * holds the comparator that is used for index searches.  This one
 * instance is a factory for one instance each of the subclasses which
 * are.
 *
 * <dl>
 * <dt>SearchComparatorEQ</dt>
 * <dd>Comparator used for searches which require an exact match for the
 * given search key.</dd>
 * <dt>SearchComparatorGT</dt>
 * <dd>Comparator used for searches which are to find an index entry
 * that is strictly greater than the given search key.</dd>
 * <dt>SearchComparatorGE</dt>
 * <dd>Comparator used for searches which are to find an index entry
 * that is greater than or equal to the given search key.</dd>
 * </dl>
 *
 * @author Stewart L. Palmer
 */

public class SearchComparator
{

  public static final int EQ = 1;
  public static final int GT = 2;
  public static final int GE = 3;

  private SearchComparator()
  {}

  public SearchComparator(
    java.util.Comparator   comparator)
  {
    if (comparator == null)
      throw new IllegalArgumentException("comparator is null");
    _comparator = comparator;
  }

  public int compare(
    Object     o1,
    Object     o2)
  {
    String x = "SearchComparator.compare() was called.";
    throw new IllegalArgumentException(x);
  }

  public int type()
  {
    String x = "SearchComparator.type() was called.";
    throw new IllegalArgumentException(x);
  }

  protected int internalCompare(
    Object     o1,
    Object     o2)
  {
    if (o1 == null)
      throw new NullPointerException("o1 is null");
    if (o2 == null)
      throw new NullPointerException("o2 is null");
    if (_comparator == null)
      throw new NullPointerException("_comparator is null");
    return _comparator.compare(o1, o2);
  }

  public final SearchComparator getSingleton(
    int                   type)
  {
    SearchComparator   x = null;
    switch(type)
    {
      case SearchComparator.EQ:
        if (_eqcomp == null)
          _eqcomp = new SearchComparatorEQ(_comparator);
        x = _eqcomp;
        break;
      case SearchComparator.GE:
        if (_gecomp == null)
          _gecomp = new SearchComparatorGE(_comparator);
        x = _gecomp;
        break;
      case SearchComparator.GT:
        if (_gtcomp == null)
          _gtcomp = new SearchComparatorGT(_comparator);
        x =  _gtcomp;
        break;
      default:
        throw new IllegalArgumentException("type = " + type);
    }

    return x;
  }

  java.util.Comparator   _comparator;

  private SearchComparator  _eqcomp;
  private SearchComparator  _gecomp;
  private SearchComparator  _gtcomp;

}
