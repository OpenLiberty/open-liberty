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

package com.ibm.ws.sib.exitpoint.systemcontext;

import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * <p>This interface is used by the ContextInserter to do the real work. This
 *   ensures that the ContextInserter is small and delegates to the implementation.
 * </p>
 * 
 * <p>SIB build component: sib.exitpoint.systemcontext</p>
 * 
 * @author nottinga 
 * @version 1.12
 * @since 1.0
 */
public interface ContextInserterWorker
{
  /* ------------------------------------------------------------------------ */
  /* insertRequestContext method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * <p>If this method returns true then the context has been inserted into the 
   *   supplied message, if it returns false then no context is in the message 
   *   and it should not be sent. If the supplied SIBusMessage is not a 
   *   supported implementation to have context inserted then an 
   *   IllegalArgumentException will be thrown.
   * </p>
   * 
   * <p>
   * 
   * @param message the message context should be added to
   * @return true if the message has been populated, false otherwise
   * @throws IllegalArgumentException if the SIBusMessage is not a supported implementation
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public boolean insertRequestContext(SIBusMessage message) 
      throws IllegalArgumentException, UnsatisfiedLinkError;
  
  /* ------------------------------------------------------------------------ */
  /* insertResponseContext method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * <p>If this method returns true then the context has been inserted into the 
   *   supplied message, if it returns false then no context is in the message 
   *   and it should not be sent. If the supplied SIBusMessage is not a 
   *   supported implementation to have context inserted then an 
   *   IllegalArgumentException will be thrown.
   * </p>
   * 
   * <p>
   * 
   * @param message the message context should be added to
   * @return true if the message has been populated, false otherwise
   * @throws IllegalArgumentException if the SIBusMessage is not a supported implementation
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public boolean insertResponseContext(SIBusMessage message) 
      throws IllegalArgumentException, UnsatisfiedLinkError;
  
  /* ------------------------------------------------------------------------ */
  /* requestFailed method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * <p>If after calling insertContext and sending the request message a 
   *   response is not received the requestFailed method should be called. 
   *   This gives the system context providers an opportunity to react to the 
   *   failure.
   * </p> 
   * 
   * @throws UnsatisfiedLinkError if the underlying list of handlers cannot be located
   */
  public void requestFailed() throws UnsatisfiedLinkError;
  /* -------------------------------------------------------------------------*/
  /* requestSucceeded method
  /* -------------------------------------------------------------------------*/
  /**
   * <p>When the request has succeeded then the system context
   *   provider may need to receive a notification. As an example the
   *   transaction system context provider suspends a transaction when a request
   *   is made and needs to resume it when the request has completed.
   * </p>
   *
   */
  public void requestSucceeded();
  /* -------------------------------------------------------------------------*/
  /* requestSucceeded method
  /* -------------------------------------------------------------------------*/
  /**
   * <p>When the request has succeeded then the system context
   *   provider may need to receive a notification. As an example the
   *   transaction system context provider suspends a transaction when a request
   *   is made and needs to resume it when the request has completed.
   * </p>
   * 
   * @param message The response message.
   */
  public void requestSucceeded(SIBusMessage message);
}
