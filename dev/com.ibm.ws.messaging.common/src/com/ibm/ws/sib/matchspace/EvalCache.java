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

/** This interface supports concurrent MatchSpace search processing. It is used in order to
 * cache evaluation information for content filters. A cache is required for each message
 * that is being processed by search and holds identifier values and evaluated subtrees for
 * that message thus avoiding excessive message parsing.  
 * 
 **/

//---------------------------------------------------------------------------

public interface EvalCache
{
  //------------------------------------------------------------------------------
  // Method: EvalCache.prepareCache
  //------------------------------------------------------------------------------
  /**  Ensure the cache is big enough and able to handle another message match.
   * Also, increase the "generation" counter to invalidate the cache.
   *
   * Created: 99-01-27
   */
  //---------------------------------------------------------------------------
  public void prepareCache(int size);

  //------------------------------------------------------------------------------
  // Method: EvalCache.getExprValue
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-27
   */
  //---------------------------------------------------------------------------
  public Object getExprValue(int id);

  //------------------------------------------------------------------------------
  // Method: EvalCache.saveExprValue
  //------------------------------------------------------------------------------
  /**
   *
   * Created: 99-01-27
   */
  //---------------------------------------------------------------------------
  public void saveExprValue(int id, Object value);

  /** A vacuous EvalCache that can be used to evaluate identifier-less subtrees for
   * optimization purposes.
   **/

  public static EvalCache DUMMY = new EvalCache()
  {
    public void prepareCache(int size){};
    public Object getExprValue(int id){ return null;};
    public void saveExprValue(int id, Object value){};  	
  };

} // EvalCache
