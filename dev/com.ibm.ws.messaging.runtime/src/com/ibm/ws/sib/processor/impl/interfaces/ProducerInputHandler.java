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
package com.ibm.ws.sib.processor.impl.interfaces;

// Import required classes.
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.processor.impl.interfaces.MessageProducer;
import com.ibm.ws.sib.processor.exceptions.SIMPResourceException;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.gd.TargetStreamManager;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author tevans
 */
/**
 * An interface class for the different types of inpt class.
 */

public interface ProducerInputHandler extends InputHandler
{ 
  /**
   * @param producerSession  The producer session to attach
   * @throws SIMPResourceException 
   */
  public void attachProducer(MessageProducer producerSession)
    throws SINotPossibleInCurrentConfigurationException, SIMPResourceException;
 
  /**
    * @param impl
    */
  public void detachProducer(MessageProducer producerSession);

  /**
   * Restores the GD target streams
   * 
   */
  public void reconstituteTargetStreams(StreamSet streamSet)
  throws SIResourceException;
          
  /**
   * Method closeProducersDestinationDeleted.
   * <p>Close and detach all producer sessions.</p>
   */
  public void closeProducersDestinationDeleted();
  
  public TargetStreamManager getTargetStreamManager();
  
  public int getProducerCount();
  
  /**
   * @return false if there are any target streams to this
   * localization that still have state
   */
  public boolean getInboundStreamsEmpty();
  
  /**
   * Detaches all ProducerSessions attached to this InputHandler.
   * Attaches each to the new handler.
   * Updates each session with this new InputHandler.
   * 
   * This is called during dynamic config.
   * It is possible that a non-mediated PM destination
   * has a PEV mediation added i.e. pre-mediatied on MQ.
   * Under such circumstances the open producer sessions have
   * to detach and reattach to the new InputHandler.
   * 
   * @param newHandler the handler that the ProducerSessions should attach to
   * @param isPreMediated if the new InputHandler is for the pre-mediated side 
   * of the destination or not.
   */
  public void detachAllProducersForNewInputHandler(ProducerInputHandler newHandler)throws SIException;

  /**
   * Called by applications when sending messages
   *  
   * @param msg Message to be sent
   * @param transaction Transaction to use
   * @param inAddress Address of the ProducerSession
   * @param sender Handle to the ProducerSession
   * @param msgFRP Indicates if the message set an FRP
   * @throws SIConnectionLostException
   * @throws SIRollbackException
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIIncorrectCallException
   * @throws SIResourceException
   */
  public void handleProducerMessage(MessageItem msg,
                                    TransactionCommon transaction,
                                    JsDestinationAddress inAddress,
                                    MessageProducer sender,
                                    boolean msgFRP)
    throws SIConnectionLostException, 
           SIRollbackException, 
           SINotPossibleInCurrentConfigurationException, 
           SIIncorrectCallException, 
           SIResourceException;
}
