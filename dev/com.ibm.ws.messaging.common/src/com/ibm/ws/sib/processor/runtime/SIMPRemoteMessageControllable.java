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

import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;


/**
 * An interface for a message participating in a flow between 
 * messaging engines
 */
public interface SIMPRemoteMessageControllable extends SIMPControllable
{
  /**
   * @throws SIMPException
   * @return string  The state of the tick, as defined above.
   *                !! Should we return a simpler version opf the state eg, Commiting, Pending Send, and 
   *                !! Pending Acknowlegement. 
   *                !! If so then we dont need getCurrentMaxIndoubtMessages(int priority,int COS)
   */
  String getState() throws SIMPException;  
  /**
   * @return long  The earliest tick covered by this DeliveryStreamTickRange.
   */
  long getStartTick();
  
  /**
   * 
   * @return long  The latest tick covered by this DeliveryStreamTickRange.
   */
  long getEndTick();  
  
  /**
   * @throws SIMPControllableNotFoundException
   * @throws SIMPException
   * @return JsMessage  The JsMessage associated with this message if there is one.
   * Otherwise it returns null 
   */
  JsMessage getJsMessage() throws SIMPControllableNotFoundException, SIMPException;  
  
  
  /**
   * If the stream contains a poison message that the target cannot process, for example
   *  because the message is too big for the target to handle the administrator may 
   *  instruct the stream to discard the message and treat it as lost. Optionally 
   *  the message is transferred to the exception destination at the source. 
   *  This request is atomic and the message will have been moved by the time this 
   *  method returns.
   *     
   * @param discard true of the message is to be discarded. 
   *                false if the message is to be sent to the exception destination.
   */
  void moveMessage(boolean discard) throws SIMPControllableNotFoundException,
    SIMPRuntimeOperationFailedException;   

  /**
   * Get the transaction identifier XID of the transaction commiting the message or null 
   * if the message is already in commited state.
   *
   * @return a Transaction ID
   */  
  String getTransactionId() throws SIMPException;
  
  /**
   * Get the timestamp (ms) at which this message arrived on this messaging
   * engine
   * @return timestamp (ms)
   */
  long getMEArrivalTimestamp() throws SIMPControllableNotFoundException, SIMPException;  
}
