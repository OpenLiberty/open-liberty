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
package com.ibm.ws.sib.processor.runtime;

/**
 * A type-safe enumeration for the indoubt action for force flushing.
 *
 */
public class IndoubtAction
{
  /**
   * The INDOUBT_DELETE causes indoubt messages to be discarded, 
   *  risking their loss.
   */
  public final static IndoubtAction INDOUBT_DELETE =
    new IndoubtAction("Indoubt_Delete", 0);

  /**
   * The INDOUBT_LEAVE means no action is taken for indoubt messages, 
   *  so that they might be resent and duplicated.
   */
  public final static IndoubtAction INDOUBT_LEAVE =
    new IndoubtAction("Indoubt_Leave", 1);

  /**
   * INDOUBT_EXCEPTION causes indoubt messages to be sent to the exception destination, 
   * this gives the possibility that the messages are duplicated.
   */
  public final static IndoubtAction INDOUBT_EXCEPTION =
    new IndoubtAction("Indoubt_Exception", 2);

  /**
   * INDOUBT_REALLOCATE causes the messages to be reallocated, 
   * possbly to the exception destination, and possibly duplicated.
   */
  public final static IndoubtAction INDOUBT_REALLOCATE =
    new IndoubtAction("Indoubt_Reallocate", 3);

  
  /**
   Returns a string representing the IndoubtAction value 
  
   @return a string representing the IndoubtAction value
  */
  public final String toString()
  {
    return name;
  }

  /**
   * Returns an integer value representing this IndoubtAction
   * 
   * @return an integer value representing this IndoubtAction
   */
  public final int toInt()
  {
    return value;
  }

  /**
   * Get the IndoubtAction represented by the given integer value;
   * 
   * @param value the integer representation of the required IndoubtAction
   * @return the IndoubtAction represented by the given integer value
   */
  public final static IndoubtAction getIndoubtActionType(int value)
  {
    return set[value];
  }

  private final String name;
  private final int value;
  private final static IndoubtAction[] set =
  {
    INDOUBT_DELETE,
    INDOUBT_LEAVE,
    INDOUBT_EXCEPTION,
    INDOUBT_REALLOCATE
  };

  private IndoubtAction(String name, int value)
  {
    this.name = name;
    this.value = value;
  }
}
