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
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author tevans
 * 
 * A ProtocolItemStream is used by the DestinationHandler for storing 
 * target stream protocol state for guaranteed delivery.
 */
public class TargetProtocolItemStream extends ProtocolItemStream
{

  /**
   * Trace.
   */
  private static final TraceComponent tc =
    SibTr.register(
      TargetProtocolItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
    
   
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public TargetProtocolItemStream()
  {
    super();
    // This space intentionally blank
  }

  /**
   * Cold start constructor.  The constructed object will be added to the 
   * DestinationHandler ItemStream.
   * 
   * @param destinationHandler  The DestinationHandler this ProtocolItemStream
   *                             stores messages for.
   * @param transaction         Transaction to use to add this object to the
   *                             Message Store.  Cannot be null.
   * 
   * @throws MessageStoreException
   */
  public TargetProtocolItemStream(
    BaseDestinationHandler destinationHandler,
    Transaction transaction)
    throws OutOfCacheSpace, MessageStoreException
  {
    super(destinationHandler, transaction);
  }
  
  /**
   * Complete recovery of a ProtocolItemStream retrieved from the MessageStore.
   * 
   * @param destinationHandler to use in reconstitution.
   * @param txManager
   * @param inputHandler
   */    
  public void reconstitute(BaseDestinationHandler destinationHandler, 
                           SIMPTransactionManager txManager, 
                           ProducerInputHandler inputHandler)
    throws MessageStoreException, SIException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitute", new Object[] {destinationHandler, txManager, inputHandler}); 

    super.reconstitute(destinationHandler, txManager);
    
    reconstituteTargetStreams(inputHandler);
          
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "reconstitute");           
  }     
    
  /**
   * Rebuild Guaranteed Delivery Target Streams 
   * 
   * Feature 171905.99
   * 
   * 
   * @throws MessageStoreException
   * @throws SIResourceException
   */   
  private void reconstituteTargetStreams(ProducerInputHandler inputHandler)
    throws MessageStoreException,
    SIException,
    SIResourceException   
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteTargetStreams");

    /*
     * Iterate through all contained Protocol Items, rebuilding
     * associated target streams for each.
     */ 
    NonLockingCursor cursor = null;
    
    try
    {
      cursor = newNonLockingItemCursor(new ClassEqualsFilter(StreamSet.class));
           
	    AbstractItem item = null;
	    while (null != (item = cursor.next()))
	    {
	      StreamSet streamSet = (StreamSet)item;
	      inputHandler.reconstituteTargetStreams(streamSet);
	    }
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }

    if (tc.isEntryEnabled())  
      SibTr.exit(tc, "reconstituteTargetStreams");      
  }  
}
