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
 * This is the comparator that is used when we want to find the first
 * record in the index whose value is greater than or equal to the
 * supplied key.  An equal compare is turned into a "less than".
 *
 * @author Stewart L. Palmer
 */

class SearchComparatorGE extends SearchComparator
{

  SearchComparatorGE(
    java.util.Comparator   comparator)
  { super(comparator); }

  public int compare(
    Object     o1,
    Object     o2)
  {
    int result = super.internalCompare(o1, o2);
    if (result == 0)
      result = -9;
    return result;
  }

  public int type()
  { return SearchComparator.GE; }

}
