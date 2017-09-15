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

package com.ibm.ws.sib.mfp.util;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * A HashedArray is like an array and like a hash table.  It has only get and set
 * operations and an integral index (like an array).  It occupies storage proportional to
 * its non-null contents rather than its capacity (like a hash table).  The key is a
 * unsigned long (all 64 bits can be used).  The values must implement
 * HashedArray.Element, so there is no extra object allocated to hold key-value pairs.
 *
 * <p>The class is designed for applications where a value with each index is stored at
 * most once.  If multiple values with the same index are stored, only the <em>first</em>
 * can be retrieved, but later values occupy storage anyway, so multiple stores should be
 * avoided.
 */

public class HashedArray {
  private static TraceComponent tc = SibTr.register(HashedArray.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private Element[][] buckets;
  private int[] counts;
  private int initBucketSize;
  private int totalSize;

  /**
   * Constructor
   *
   * @param nbuckets the number of buckets to allocate.  Different values give different
   * performance
   *
   * @param initBucketSize the initial size at which to make a new bucket.  Buckets double
   * in size once they grow beyond their initial size.  Different values give different
   * performance.
   */
  public HashedArray(int nbuckets, int initBucketSize) {
    init(nbuckets, initBucketSize);
  }

  /**
   * Perform the real work of the constructor(s)
   */
  private final void init(int nbuckets, int initBucketSize) {
    buckets = new Element[nbuckets][];
    counts = new int[nbuckets];
    this.initBucketSize = initBucketSize;
  }

  /**
   * Get an element from the array
   */
  synchronized public Element get(long index) {
    int bind = ((int)index & Integer.MAX_VALUE) % buckets.length;
    Element[] bucket = buckets[bind];
    if (bucket == null)
      return null;
    for (int i = 0; i < counts[bind]; i++)
      if (bucket[i].getIndex() == index)
        return bucket[i];
    return null;
  }

  /**
   * Set an element into the array
   */
  synchronized public void set(Element value) {
    int bind = ((int)value.getIndex() & Integer.MAX_VALUE) % buckets.length;
    Element[] bucket = buckets[bind];
    int count = counts[bind];
    if (bucket == null)
      buckets[bind] = bucket = new Element[initBucketSize];
    else if (bucket.length == count) {
      bucket = new Element[count * 2];
      System.arraycopy(buckets[bind], 0, bucket, 0, count);
      buckets[bind] = bucket;
    }
    bucket[count] = value;
    counts[bind] = count + 1;
    totalSize += 1;
  }

  /**
   * Return the number of elements in this HashedArray
   * There is no point synchronizing this method explicitly, as if used in isolation
   * it probably wouldn't matter how accurate the point-in-time size was. However, if
   * it is used to create the array to pass into toArray(), the caller MUST synchronize
   * round the two calls to ensure the array passed in is of the correct size.
   */
  public int size() {
    return totalSize;
  }

  /**
   * Return the contents of the HashedArray as an array
   */
  synchronized public Object[] toArray(Object[] values) {
    int count = 0;
    for (int bind = 0; bind < buckets.length; bind++) {
      if (counts[bind] > 0)
         System.arraycopy(buckets[bind], 0, values, count, counts[bind]);
      count += counts[bind];
    }
    return values;
  }

  /**
   * For testing purposes (only) we can remove items from the HashedArray.  This is not
   * very efficient, but is only used for testcases so probably doesn't matter too much.
   * This method has been made synchronized for thread-safety, just in case anyone ever uses it
   */
  synchronized public Element remove(long index) {
    Element oldValue = get(index);
    if (oldValue  != null) {
      int bind = ((int)index & Integer.MAX_VALUE) % buckets.length;
      Element[] bucket = buckets[bind];
      int count = counts[bind];
      int i = 0;
      while (bucket[i].getIndex() != index)
        i++;
      System.arraycopy(bucket, i+1, bucket, i, count-(i+1));
      counts[bind] = count - 1;
      totalSize -= 1;
    }
    return oldValue;
  }

  /**
   * Interface that must be implemented in order to be stored in a HashedArray
   */
  public static interface Element {
    long getIndex();
  }
}
