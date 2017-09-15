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

package com.ibm.ws.sib.processor.utils;

import java.lang.reflect.Array;

/**
 * A utility class which encapsulates an array of Objects and produces
 * string output when requested.  This is useful when passing in arrays
 * to the various trace functions.
 */
public class Enumarray {

  // The array we'll be displaying
  private Object toDisplay;

  /**
   * Construct a new array displayer.
   *
   * @param array The array to be displayed.  "null" is allowable.
   */
  public Enumarray(Object array)
  {
    toDisplay = array;
  }

  /**
   * Stringify the array we've been configured with.
   *
   * @return A stringified version of the internal array.
   */
  public String toString()
  {
    if (toDisplay == null)
    {
      return "<null>";
    } 
    
    // First make sure this is really an array.
    // Should probably thrown an exception int the <ctor> but we'll excuse
    // this minor lapse.
    if (!toDisplay.getClass().isArray())
    {
      return toDisplay.toString();
    } 
      
    int l = Array.getLength(toDisplay);
    StringBuffer out = new StringBuffer();
    out.append("[");
    for(int i=0; i<l; i++, out.append(","))
    out.append(Array.get(toDisplay, i).toString());
    out.append("]");
    return out.toString();          
  }
}
