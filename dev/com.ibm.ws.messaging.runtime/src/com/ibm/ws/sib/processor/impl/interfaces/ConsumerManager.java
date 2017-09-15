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

import java.util.List;

import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.BrowserSessionImpl;
import com.ibm.ws.sib.processor.impl.JSConsumerSet;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.OrderingContextImpl;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

public interface ConsumerManager extends Browsable
{
  /**
   * Attach a new ConsumerPoint to this ConsumerManager.
   * 
   * @param consumerPoint The consumer point being attached
   * @param selector  The Filter that the consumer has specified
   * @param discriminator  The discriminator that the consumer has specified
   * @param connectionUuid  The connections UUID
   * @param readAhead  If the consumer can read ahead
   * @param forwardScanning  If the consumer is forward scanning
   * @param consumerSet  If XD classification is enabled, this specifies the ConsumerSet that
   * this consumer belongs to.
   * @return The ConsumerKey object which was created for this consumer point.
   * being deleted
   * @throws SISessionDroppedException 
   */
  public ConsumerKey attachConsumerPoint(
      ConsumerPoint consumerPoint,
      SelectionCriteria criteria,
      SIBUuid12 connectionUuid,
      boolean readAhead,
      boolean forwardScanning,
      JSConsumerSet consumerSet)
    throws SINotPossibleInCurrentConfigurationException, SIDestinationLockedException, SISelectorSyntaxException, SIDiscriminatorSyntaxException, SIResourceException, SISessionDroppedException;
  
  /**
   * Detach a consumer point from this Consumer Manager.
   * 
   * @param consumerKey The ConsumerKey object of the consumer point
   * being detached
   */
  public void detachConsumerPoint(ConsumerKey consumerKey)
    throws SIResourceException, SINotPossibleInCurrentConfigurationException;
    
  
  /**
   * Attach a BrowserSession to this ConsumerManager
   * @param browserSession  The browser session to attach
   */
  public void attachBrowser(BrowserSessionImpl browserSession)
    throws SINotPossibleInCurrentConfigurationException, SIResourceException;
    
  /**
   * Detach a BrowserSession from this ConsumerManager
   * @param browserSession  The browser session to detach
   */
  public void detachBrowser(BrowserSessionImpl browserSession);    
    
  /**
   * Get the number of consumers on this ConsumerDispatcher
   * <p>
   * Feature 166832.23
   * 
   * @return number of consumers.
   */
  public int getConsumerCount();
  
  /**
   * Used by the unit tests to return the list of consumer points
   * This list is cloned to stop illegal access to the ConsumerPoints
   * controlled by this ConsumerDispatcher
   * @return
   */
  public List getConsumerPoints();
  
  /**
   * 
   */
  public void setReadyForUse();

  /**
   * @return
   */
  public boolean isLocked();

  /**
   * @return
   */
  public BaseDestinationHandler getDestination();

  /**
   * @return
   */
  public MessageProcessor getMessageProcessor();

  /**
   * @param consumerKey
   * @param orderingGroup
   * @return
   * @throws SIResourceException
   * @throws SISessionDroppedException 
   */
  public ConsumerKeyGroup joinKeyGroup(ConsumerKey consumerKey, OrderingContextImpl orderingGroup) throws SIResourceException, SISessionDroppedException;

  public boolean isNewTransactionAllowed(TransactionCommon transaction);
}
