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

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup;
import com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.transactions.TransactionCommon;

public interface JSConsumerManager extends ConsumerManager {

  public void checkInitialIndoubts(DispatchableConsumerPoint point) throws SIResourceException;

  public void setCurrentTransaction(TransactionCommon transaction, JSLockedMessageEnumeration lme);

  public ConsumerDispatcherState getConsumerDispatcherState();

  public long newReadyConsumer(JSConsumerKey key, boolean specific);

  public void removeReadyConsumer(JSConsumerKey key, boolean specific);

  public void removeKeyGroup(LocalQPConsumerKeyGroup group);

  public void setCurrentTransaction(SIMPMessage msg, boolean isInDoubtOnRemoteConsumer);

  public boolean isPubSub();
  
  /** Methods for accessing msgs from AOStreams **/
  
  /**
   * On Restart of a DME/IME, the messages referenced by an AOValue are retrieved via the 
   * RemoteConsumerDispatcher using the getMessageByValue method.
   */
  public SIMPMessage getMessageByValue(AOValue value)
    throws SIResourceException;

}
