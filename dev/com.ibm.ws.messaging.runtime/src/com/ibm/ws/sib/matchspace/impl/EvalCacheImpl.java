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
import com.ibm.ws.sib.matchspace.EvalCache;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

//------------------------------------------------------------------------------
// Class EvalCache
//------------------------------------------------------------------------------
/**
 * EvalCache
 *
 *
 */
//---------------------------------------------------------------------------

public class EvalCacheImpl implements EvalCache
{

  // Standard trace boilerplate
  
  private static final Class cclass = EvalCacheImpl.class;
  private static Trace tc = TraceUtils.getTrace(EvalCacheImpl.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  int generation = 1;
  int[] cacheTag;
  Object[] cacheValue;

  //------------------------------------------------------------------------------
  // Method: EvalCache.prepareCache
  //------------------------------------------------------------------------------
  /**  Ensure the cache is big enough and able to handle another message match.
   * Also, increase the "generation" counter to invalidate the cache.
   *
   * Created: 99-01-27
   */
  //---------------------------------------------------------------------------
  public
  void prepareCache(int size){
   if (tc.isEntryEnabled())
      tc.entry(this,cclass, "prepareCache", "size: " + size);

    int oldSize = (cacheTag == null) ? 0 : cacheTag.length;

    if (size <= oldSize) {
      // If the cache is big enough, make sure we are not out of counters
      if (generation == Integer.MAX_VALUE) {
        generation = 1;
        for (int i = 0; i < size; i++)
          cacheTag[i] = 0;
      } else generation++;

    if (tc.isEntryEnabled())
	  tc.exit(this,cclass, "prepareCache");

      return;
    }

    // Allocate fresh

    cacheTag = new int[2*size];
    cacheValue = new Object[2*size];

    // Reset generation since all cache fields are null.
    generation = 1;

    if (tc.isEntryEnabled())
	  tc.exit(this,cclass, "prepareCache");
  } //sizeCache


  //------------------------------------------------------------------------------
  // Method: EvalCache.getExprValue
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-27
   */
  //---------------------------------------------------------------------------
  public
  Object getExprValue(int id){

   if (tc.isEntryEnabled())
      tc.entry(this,cclass, "getExprValue", "id: " + new Integer(id));
    Object result = null;
    if (cacheTag[id] == generation)
      result = cacheValue[id];

    if (tc.isEntryEnabled())
	  tc.exit(this,cclass, "getExprValue","result: " + result);
    return result;
  } //getExprValue


  //------------------------------------------------------------------------------
  // Method: EvalCache.saveExprValue
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-27
   */
  //---------------------------------------------------------------------------
  public
  void saveExprValue(int id, Object value){

   if (tc.isEntryEnabled())
      tc.entry(this,cclass, "saveExprValue", "id: " + new Integer(id) +",value: "+value);
    cacheTag[id] = generation;
    cacheValue[id] = value;
   if (tc.isEntryEnabled())
	 tc.exit(this,cclass, "saveExprValue");
  } //saveExprValue

} // EvalCacheImpl
