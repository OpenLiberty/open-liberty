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

import com.ibm.ws.sib.matchspace.utils.FFDC;

/**
 * This class must be extended by subclasses whose instances are to be associated
 * with filters in the matching space.
 */
public abstract class MatchTarget implements Cloneable
{
  private static final Class cclass = MatchTarget.class;
  // The type is set by the constructor.  The index property is
  // set by MatchSpace.  The type(), setIndex(), and getIndex() methods
  // are not overrideable.  A typical MatchTarget specialization implements equals(),
  // hashCode() and whatever other behavior it likes.
  private int type;
  private int index;

  // Constructor (the only one) requires a type

  protected MatchTarget(int type)
  {
    this.type = type;
  }

  //------------------------------------------------------------------------------
  // Method: MatchTarget.type
  //------------------------------------------------------------------------------
  /** Returns an integer describing the type of this MatchTarget.<p>
   *
   * This type will be passed to implementations of SearchResults when
   * a group of MatchTargets are added at match time.<p>
   *
   * The type code returned should be defined as a constant in subclasses.
   *
   * Created: 98-10-09
   */
  //---------------------------------------------------------------------------
  public final int type()
  {
    return type;
  }

  //------------------------------------------------------------------------------
  // Method: MatchTarget.setIndex
  //------------------------------------------------------------------------------
  /** Records an integer that is used inside the matcher to manage the target efficiently.
   * This method need not be implemented by subclasses and shouldn't be overridden. */
  public final void setIndex(int index)
  {
    this.index = index;
  }

  //------------------------------------------------------------------------------
  // Method: MatchTarget.getIndex
  //------------------------------------------------------------------------------
  /** Returns the integer recorded by setIndex. */
  public final int getIndex()
  {
    return index;
  }

  //------------------------------------------------------------------------------
  // Method: MatchTarget.duplicate
  //------------------------------------------------------------------------------
  /** Creates a clone of this MatchTarget.  Override only if the system clone support does
   * not produce a correct result.
   **/
  public MatchTarget duplicate()
  {
    try
    {
      return (MatchTarget) clone();
    }
    catch (CloneNotSupportedException e)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      FFDC.processException(cclass,
          "com.ibm.ws.sib.matchspace.MatchTarget.duplicate",
          e,
          "1:112:1.15");        
      // should not happen
      throw new IllegalStateException();
    }
  }
}
