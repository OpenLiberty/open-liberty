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
package com.ibm.ws.sib.matchspace.selector.impl;

public class OrdinalPosition 
{

  /** Specifies level at which this ordinal position applies, always 0 for JMS identifiers */
  public int majorPosition = 0;
  
  /** Specifies level at which this ordinal position applies within a level or
   * step as defined by the majorPosition */
  private int minorPosition = 0;

  /** Create a new SimpleMatcher for a given Identifier */

  public OrdinalPosition(int major, int minor)
  {
    majorPosition = major;
    minorPosition = minor;
  }

  public int compareTo(Object o)
  {
    int ret = 0;
    // Class cast exc
    OrdinalPosition other = (OrdinalPosition)o;
    
    if(other.majorPosition == majorPosition)
    {
      ret = minorPosition - other.minorPosition;
    }
    else
      ret = majorPosition - other.majorPosition;
     
    return ret;
  }  

  public boolean equals(Object o)
  {
    if (o instanceof OrdinalPosition)
    {
      if(((OrdinalPosition)o).majorPosition == majorPosition)
      {
        // Majors are equal, compare minors
        if(((OrdinalPosition)o).minorPosition == minorPosition)
          return true; // minors are equal
        else
          return false; // minors are unequal
      }
      else
        return false; // majors are unequal 
    }
    else
      return false; // different types of object
  }    
  
  public String toString()
  {
    return "Major: " + majorPosition + " minor: " + minorPosition;
  }  
}
