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
package com.ibm.ws.sib.processor.impl.store.itemstreams;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.PubSubInputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author caseyj
 * 
 * A ProxyReferenceStream is used in guaranteed delivery.
 */
public final class ProxyReferenceStream 
  extends MessageReferenceStream
{
  /**
   * Trace.
   */
  private static TraceComponent tc =
    SibTr.register(
      ProxyReferenceStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
   

  private PubSubInputHandler pubSubInputHandler;
  private PubSubMessageItemStream parentItemStream;
   
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public ProxyReferenceStream()
  {
    super();

    // This space intentionally blank
  }

  /**
   * Cold start constructor.  Adds itself to the parent ItemStream given.
   * 
   * @param parentItemStream  The stream to add ourselves to after 
   *                           construction.
   * @param transaction       The transaction to use for the add.  Cannot be
   *                           null.
   * 
   * @throws MessageStoreException if there was an error adding the stream.
   */
  public ProxyReferenceStream(
    PubSubMessageItemStream parentItemStream,
    Transaction transaction)
    throws OutOfCacheSpace, MessageStoreException
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ProxyReferenceStream", 
        new Object[] { parentItemStream, transaction });

    this.parentItemStream = parentItemStream;
    // The storage strategy for a reference stream can be no better than the
    // parent stream storage strategy.  The parent message stream storage
    // strategy would usually be the most persistent, but if is slaved to a 
    // temporary destination then it will have a lesser reliability.
    setStorageStrategy(parentItemStream.getStorageStrategy());

    parentItemStream.addReferenceStream(this, transaction);     
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ProxyReferenceStream", this);
  }
  
  /**
   * Method setPubSubInputHandler.
   * @param pubSubInputHandler
   */
  public void setPubSubInputHandler(PubSubInputHandler pubSubInputHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setPubSubInputHandler", pubSubInputHandler);
    this.pubSubInputHandler = pubSubInputHandler;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setPubSubInputHandler");
  }
  
  /**
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.MessageReferenceStream#registerListeners(SIMPMessage)
   */
  public void registerListeners(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerListeners", msg);
      
    pubSubInputHandler.registerForEvents(msg);
    // We want to know about post commits so we can turn Uncommitted
    // into Value.  We want to know about post rollbacks so we can
    // decrement the use count.  
    msg.registerMessageEventListener(MessageEvents.POST_COMMIT_ADD, pubSubInputHandler);
    msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, pubSubInputHandler);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerListeners");
  }

  @Override
  public void eventPostCommitAdd(Transaction transaction)  throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostCommitAdd", transaction);
    super.eventPostCommitAdd(transaction);
    parentItemStream.incrementReferenceStreamCount();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostCommitAdd");
  }
}
