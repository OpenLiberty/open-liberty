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

package com.ibm.ws.sib.processor.gd;

import com.ibm.ws.sib.processor.utils.BlockVector;

/**
 * This class implements a 'complete' range-list. Each range is of the form [l, h] where
 * l and h are long stamps and l <= h.
 * The list [l0, h0], [l1, h1] ,..., [lk, hk] is such that
 * hi + 1 = l(i+1) and l0 = 0 and hk = INFINITY, hence it is 'complete'.
 *
 * The list supports a cursor, which can be efficiently set to any stamp j.
 * The cursor at j actually refers to a range-object [l, h] such that j \in [l, h].
 * The getPrev() and getNext() methods are used for navigating the list using
 * the cursor.
 * The replace() and get() methods do not use the cursor value, but may reset it
 * to an arbitrary point.
 *
 * The list can be updated by replacing part of it by the list
 *  [a0, b0], [a1, b1], ..., [am, bm]
 * where bi + 1 = a(i+1). However, a0, bm need not be 0 and INFINITY respectively.
 * If there is a range [li, hi] that partly overlaps with [a0, b0], and li < a0,
 * then the [li, hi] object's range is trimmed to [li, a0-1].
 * If there is a range [lj, hj] that partly overlaps with [am, bm] and hj > bm, then
 * the [lj, hj] object's range is trimmed to [bm+1, hj].
 * If the same object [lj, hj] is such that lj < a0 and hj > bm, then this object
 * is 'cloned' and the ranges trimmed as described earlier.
 * The previous discussion implies that the range of a range-object is mutable.
 *
 * Concurrency: The data-structure is unsynchronized, since we expect synchronization
 * will be more effective at a higher-level. However, the implementation is required
 * to allow concurrent invocation of the get() method.
 *
 * NOTE:
 * Cloning (of RangeObjects) is not performed unless absolutely necessary (when the object is split)
 * Also, a user/client of the RangeList is allowed to mutate the object 'in-place' as long as the
 * actual range is not changed.
 *
 *
 */

public interface RangeList
{

  public static final long INFINITY = Long.MAX_VALUE;

  /**
   * @param stamp The stamp to set the cursor to.
   */
  public void setCursor(long stamp);

  /** should only be used if no writes happen in between */
  public Object getMark(Object mark);
  public void setCursor(Object mark);

  /**
   * (slight misnomer)
   * Returns the range-object at the cursor position and steps the cursor to the next
   * object if there is a next, otherwise the cursor stays at the same place
   */
  public RangeObject getNext();

  /**
   * (slight misnomer)
   * Returns the range-object at the cursor position and steps the cursor to the prev
   * object if there is a prev, otherwise the cursor stays at the same place
   */
  public RangeObject getPrev();

  /**
   * Returns the range-object at the cursor position
   */
  public RangeObject getCurr();

  /**
   * Gets all the range-objects in this list which overlap with the provided
   * range. May reset the cursor for optimizing future method invocations.
   * @param readList The list of range-objects is appended to readList in
   *                 ascending order.
   */
  public void get(RangeObject r, BlockVector readList);
  public void get(long startstamp, long endstamp, BlockVector readList);

  /**
   * @param writeList contains an 'incomplete' list of range-objects to replace the
   *         information currently at these ticks. RangeObjects in writeList are
   *        added onto the list while some are removed from the list.
   */
  public void replace(BlockVector writeList);

  /**
   * w.startstamp = 0.
   * provided as don't have to search for position to replace
   */
  public void replacePrefix(RangeObject w);

  /** New mutation method added for durable message streams */
  /**
   *  splits the range object [a, b] at the current cursor position, that contains stamp,
   *  into two objects, [a, stamp-1], [stamp, b]. The cursor is made to point to [stamp, b].
   * If a==stamp, nothing is done.
   * @param stamp > 0
   * 
   * @exception GDException  Thrown when there is an error with the message.
   */
  public void splitStart(long stamp);

  /**
   *  splits the range object [a, b] at the current cursor position, that contains stamp,
   *  into two objects, [a, stamp], [stamp+1, b]. The cursor is made to point to [a, stamp]
   * If stamp==b, nothing is done
   * @param stamp >= 0
   * 
   * @exception GDException  Thrown when there is an error with the message.
   */
  public void splitEnd(long stamp);

}
