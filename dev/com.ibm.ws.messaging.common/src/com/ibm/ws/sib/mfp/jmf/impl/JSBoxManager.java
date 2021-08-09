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

package com.ibm.ws.sib.mfp.jmf.impl;

import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;

/** This class manages all creation of JSBoxedListImpl objects
 * by JSMessageImpl and JSCompatibleMessageImpl and also maintains
 * a cache of such objects. 
 */
class JSBoxManager {
  // Associated part
  private JMFNativePart part;
  
  // Local value of schema.getBoxed() for fast access.
  private int[][] boxed;

  // The secondary cache for JSBoxedListImpl objects used to deliver boxed values.  Will
  // be the same length as the 'boxed' array.
  private JSBoxedListImpl[] boxedCache;

  /** Make a new JSBoxManager
   * 
   * @param part the JMFNativePart with which this box manager is associated
   * @param boxed the result of calling getBoxed() on the schema associated with
   *    part.
   */
  JSBoxManager(JMFNativePart part, int[][] boxed) {
    this.part = part;
    this.boxed = boxed;
    reset();
  }

  /** Reset the boxedCache to the empty state
   */
  void reset() {
    boxedCache = new JSBoxedListImpl[boxed.length];    
  }

  /** Remove entries from the boxedCache after removing the entry from the main cache 
   * that they depend upon.
   * @param boxedAccessor the accessor of the entry that was removed from the main cache
   */
  void cleanupBoxedCache(int boxedAccessor) {
    for (int i = 0; i < boxedCache.length; i++)
      if (boxed[i][0] == boxedAccessor)
        boxedCache[i] = null;
  }
  
  /** Return the box accessor for a given entry in the boxed array */
  int getBoxAccessor(int boxedIndex) {
    return boxed[boxedIndex][0];
  }
  
  /** Delegatee of getValue that handles the case where the value is boxed.  The
   * accessor has been reduced to be a valid index into the boxed and boxedCache arrays.
   */
  Object getBoxedlValue(int accessor)
      throws JMFSchemaViolationException, 
      JMFModelNotImplementedException, 
      JMFMessageCorruptionException, JMFUninitializedAccessException {
    JSBoxedListImpl ans = boxedCache[accessor];
    if (ans != null)
      return ans;
    JSVaryingList subAns = (JSVaryingList)part.getValue(boxed[accessor][0]);
    boxedCache[accessor] =
      ans = JSBoxedListImpl.create(subAns, boxed[accessor][1]);
    return ans;
  }
  
  /** Delegatee of setValue used when the value is boxed.  The accessor is reduced so as
   * to index the boxes array and is valid for that purpose.
   */
  void setBoxedValue(int accessor, Object val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
    if (val == boxedCache[accessor])
      // Resetting the same value is a no-op, even if it's been mutated
      return;
    int boxAccessor = boxed[accessor][0];
    JSVaryingList subAns;
    if (part.isPresent(boxAccessor))
      subAns = (JSVaryingList) part.getValue(boxAccessor);
    else
      subAns = (JSVaryingList) part.createBoxList(boxAccessor, val);
    boxedCache[accessor] =
      JSBoxedListImpl.create(subAns, boxed[accessor][1], val);
  }
}
