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

import com.ibm.ws.sib.matchspace.Selector;

import java.util.HashMap;
import java.util.Iterator;

/** This class serves as the "intern table" for all Selector subexpressions within
 * MatchSpace.  It implements the Selector.InternTable interface required by the
 * Selector.intern and unintern methods.
 **/

public final class InternTable extends HashMap implements Selector.InternTable
{

  // Constant governing how high the unique id counter is allowed to grow before
  // attempting compression of unique ids.

  private static final int COUNTER_LIMIT = 10000;

  // Constant governing how much the unique id counter must be reduced by in order to make
  // compression of unique ids worthwhile

  private static final int MIN_REDUCE = 2000;

  // Counter holding the next uniqueId to be assigned

  private int counter = 1;

  private static final long serialVersionUID = 5260158026529697853L;
  /** Implement the getNextUniqueId function */

  public int getNextUniqueId()
  {
    if (counter > COUNTER_LIMIT && counter - size() >= MIN_REDUCE)
      compress();
    return counter++;
  }

  /** Return the required evaluation cache size for an indexed evaluation cache (as
   * implemented by the EvalCache abstract class).
   **/

  public int evalCacheSize()
  {
    return counter;
  }

  // Compress the uniqueId assignments for Selectors currently in the intern table

  private void compress()
  {
    counter = 1;
    for (Iterator e = values().iterator(); e.hasNext(); ) 
    {
      Selector s = (Selector) e.next();
      s.setUniqueId(counter++);
    }
  }

  // Specialize clear() method to reset counter.
  public void clear()
  {
    super.clear();
    counter = 1;
  }

}
