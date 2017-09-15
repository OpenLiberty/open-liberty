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

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.ws.sib.transactions.TransactionCommon;

/**
 * @author cwilkin
 */

public interface ExceptionDestinationHandler
{
  /**
   * This method contains the routine used to handle an undeliverable message. The
   * method examines the attributes of a message to determine what to do with it.
   * 
   * It is possible for a message to be discarded, blocked, or sent to an exception
   * destination. The following return types give an indication of what should be
   * done with the message after the call.
   * 
   * OK
   * The message was delivered to the exception destination. It can now be 
   * dereferenced.
   * 
   * DISCARD
   * The message does not need to be delivered to an exception destination and is
   * no longer needed. The message can be dereferenced.
   * 
   * BLOCK
   * The message could not be delivered to the exception destination at this time
   * due to a transient problem. Retry the operation at a later time.
   * 
   * ERROR
   * An error occurred during delivery to the exception destination. The message
   * was not delivered. An FFDC will have been generated.
   * 
   * @param msg - The undeliverable message
   * @param alternateUser - name of user against which access checks should be made
   * @param tran - The transaction that the message will be delivered under
   * @param exceptionReason - The reason why the message could not be delivered
   * @param exceptionStrings - A list of inserts to place into an error message
   * @return A code indicating what we did with the message
   */ 
  public UndeliverableReturnCode handleUndeliverableMessage(
        SIBusMessage msg,
        String alternateUser,
        TransactionCommon tran,
        int exceptionReason,
        String [] exceptionStrings );        

  /**
   * Sets the destination that could not be delivered to.
   * 
   * @param destinationName - The destination to set
   */
  
  public void setDestination(SIDestinationAddress destAddress) throws SIException;
}
