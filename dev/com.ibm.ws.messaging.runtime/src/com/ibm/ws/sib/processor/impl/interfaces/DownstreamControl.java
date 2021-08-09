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

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * An interface class for handling downstream messages
 */
public interface DownstreamControl
{

  public void sendAckExpectedMessage(
    long ackExpStamp,
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
    throws SIResourceException;

   public void sendSilenceMessage(
    long startStamp,
    long endStamp,
    long completedPrefix,
    boolean requestedOnly,
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
    throws SIResourceException;

  public List sendValueMessages(
    List msgList,
    long completedPrefix,
    boolean requestedOnly,
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
    throws SIResourceException;
  
  /**
   * Retreive the message item from the store if it exists.
   * If there is an error retreiving the item or if the item has expired
   * then null is returned.
   */  
  public MessageItem getValueMessage(long msgStoreID)
    throws SIResourceException;
  
  public void sendFlushedMessage(SIBUuid8 target, SIBUuid12 streamID)
    throws SIResourceException;
    
  public void sendNotFlushedMessage(SIBUuid8 target, SIBUuid12 streamID, long requestID) throws SIResourceException;
}
