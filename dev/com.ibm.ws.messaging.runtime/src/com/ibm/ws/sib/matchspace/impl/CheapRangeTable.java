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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.matchspace.SimpleTest;
import com.ibm.ws.sib.matchspace.selector.impl.EvaluatorImpl;
/**
 * This class maintains a table of ranged values with associated targets for those
 * values.  When given a value to search, it returns all targets that meet the range.
 *
 * @author Daniel Sturman
 */
public class CheapRangeTable {

  public static int numTables = 0;
  public static int numEntries = 0;

  int size = 0;

  RangeEntry[] ranges;

  /**
   * Create a new table.
   */
  public CheapRangeTable() {
    ranges = new RangeEntry[3];
  }


  /**
   * Insert a range and an associated target into the table.
   *
   * @param test a SimpleTest of kind==NUMERIC containing the range
   * @param target The target associated with the range.
   */
  public void insert(SimpleTest test, Object target) {
    if (size == ranges.length) {
      RangeEntry[] tmp = new RangeEntry[2*size];
      System.arraycopy(ranges,0,tmp,0,size);
      ranges = tmp;
    }

    ranges[size] = new RangeEntry(test, target);
    size++;
  }

  /** Retrieve the Object associated with an exactly defined range */

  public Object getExact(SimpleTest test) {
    for (int i = 0; i < size; i++) {
      if (ranges[i].correspondsTo(test))
        return ranges[i].target;
    }
    return null;
  }

  /** Replace the Object in a range that is known to exist */

  public void replace(SimpleTest test, Object target) {
    for (int i = 0; i < size; i++)
      if (ranges[i].correspondsTo(test)) {
        ranges[i].target = target;
        return;
      }
    throw new IllegalStateException();
  }

  /**
   * Find targets associated with all ranges including this value.
   *
   * @param value Value to search for.
   * @return List of all targets found.
   */
  public List find(Number value) { // was NumericValue
    List targets = new ArrayList(1);

    for (int i = 0; i < size; i++) {
      if (ranges[i].contains(value))
        targets.add(ranges[i].target);
    }

    return targets;
  }


  public boolean isEmpty() {
    return size == 0;
  }

  public void remove(SimpleTest test) {
    if (size == 0)
      throw new IllegalStateException();

    int toGo = -1;
    for (int i = 0; toGo < 0 && i < size; i++) {
      if (ranges[i].correspondsTo(test))
        toGo = i;
    }

    if (toGo < 0)
      throw new IllegalStateException();

    System.arraycopy(ranges,toGo+1,ranges,toGo,size-toGo-1);
    size--;
  }

  class RangeEntry {
    Number lower; // was NumericValue
    boolean lowIncl;
    Number upper; // was NumericValue
    boolean upIncl;
    Object target;
    RangeEntry(SimpleTest test, Object t) {
      lower = test.getLower();
      lowIncl = test.isLowIncl();
      upper = test.getUpper();
      upIncl = test.isUpIncl();
      target = t;
    }
    boolean correspondsTo(SimpleTest t) {
      if (lowIncl != t.isLowIncl() || upIncl != t.isUpIncl())
        return false;
      if (lower == null)
        if (t.getLower() != null)
          return false;
        else;
      else if (t.getLower() == null)
        return false;
      else if (!EvaluatorImpl.equals(lower, t.getLower()))
        return false;
      if (upper == null)
        if (t.getUpper() != null)
          return false;
        else;
      else if (t.getUpper() == null)
        return false;
      else if (!EvaluatorImpl.equals(upper, t.getUpper()))
        return false;
      return true;
    }
    boolean contains(Number v) {
      if (lower != null) {
        int comp = EvaluatorImpl.compare(lower,v);
        if (comp > 0 || !lowIncl && comp == 0)
          return false;
      }
      if (upper != null) {
        int comp = EvaluatorImpl.compare(upper, v);
        if (comp < 0 || !upIncl && comp == 0)
          return false;
      }
      return true;
    }
  }
}
