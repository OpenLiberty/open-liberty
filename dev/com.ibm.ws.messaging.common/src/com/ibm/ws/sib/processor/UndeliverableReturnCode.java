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

package com.ibm.ws.sib.processor;

/**
 * The class is a TypeDef for all the return codes of the 
 * ExceptionDestination.handleUndeliverableMessage() method. The behaviour
 * of this method will vary and we need to give the calling code an indication of
 * what happened to the message.
*/
public final class UndeliverableReturnCode
{
  /**
   * The message was not required to be sent to the exception destination and 
   * therefore needs to be discarded
   */
  public final static UndeliverableReturnCode DISCARD =
    new UndeliverableReturnCode("DISCARD");

  /**
   * The message should or could not be delivered to the exception destination 
   * at this point in time and the message should be left where it is until a 
   * later time
   */
  public final static UndeliverableReturnCode BLOCK =
    new UndeliverableReturnCode("BLOCK");

  /**
   * The message was successfully sent to the exception destination
   */
  public final static UndeliverableReturnCode OK =
    new UndeliverableReturnCode("OK");
    
  /**
   * An error occurred sending the message to the exception destination.
   * The message was not delivered.
   */
  public final static UndeliverableReturnCode ERROR =
    new UndeliverableReturnCode("ERROR");

  /**
   Returns a string representing the <code>UndeliverableReturnCode</code> value 
   
   @return a string representing the <code>UndeliverableReturnCode</code> value
  */
  public String toString()
  {
    return name;
  }

  /**
   * A string representing the return code
  */
  private final String name;

  /**
   * Creates a new return code value with the name given.
  */
  private UndeliverableReturnCode(String name)
  {
    this.name = name;
  }
}
