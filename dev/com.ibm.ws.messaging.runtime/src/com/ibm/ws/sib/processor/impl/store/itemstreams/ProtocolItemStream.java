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
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author prmf
 * 
 * A ProtocolItemStream is used by the DestinationHandler for storing protocol
 * state for guaranteed delivery.
 */
public class ProtocolItemStream extends SIMPItemStream
{
  /**
   * Trace.
   */
  private static TraceComponent tc =
    SibTr.register(
      ProtocolItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
   
  
  //Maintain a reference to the destinationHandler
  private BaseDestinationHandler destinationHandler = null;

  /**
   * Warm start constructor invoked by the Message Store.
   */
  public ProtocolItemStream()
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
   * @throws OutOfCacheSpace  For message store resource problems.
   * @throws MessageStoreException
   */
  public ProtocolItemStream(
    BaseDestinationHandler destinationHandler,
    Transaction transaction)
    throws OutOfCacheSpace,
           MessageStoreException
  {
    super();

    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "ProtocolItemStream",  
        new Object[]{destinationHandler, transaction});
        
    // The ProtocolItemStream has to have the same storage strategy as its
    // parent destination.  Two reasons:
    //
    // 1.  The message store will not allow the ItemStream to 
    // be stored if it has a more permanent storage strategy.
    // 2.  If the DestinationHandler is not persistently stored (e.g. if it is
    // a temporary destination) then this stream should also not be persistent.
    setStorageStrategy(destinationHandler.getStorageStrategy());
    
    destinationHandler.addItemStream(this, transaction);

    initializeNonPersistent(destinationHandler,
                            destinationHandler.getTxManager());

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "ProtocolItemStream", this);
  }
  
  /**
   * Complete recovery of a ProtocolItemStream retrieved from the MessageStore.
   * 
   * @param destinationHandler to use in reconstitution.
   * @param txManager
   */    
  public void reconstitute(BaseDestinationHandler destinationHandler,
                           SIMPTransactionManager txManager)
    throws MessageStoreException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitute", new Object[] {destinationHandler, txManager}); 
      
    initializeNonPersistent(destinationHandler, txManager);
        
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "reconstitute");           
  }     
  
  /**
   * Initialize non-persistent fields.  These fields are common to both MS
   * reconstitution of DestinationHandlers and initial creation.
   * <p>
   * In the warm start case, by the time this method is called, we know we
   * have all the persistent information available.
   * 
   * @param destinationHandler to reference
   */  
  void initializeNonPersistent(BaseDestinationHandler destinationHandler,
                               SIMPTransactionManager txManager) throws MessageStoreException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "initializeNonPersistent", new Object[] {destinationHandler, txManager});     
    
    this.destinationHandler = destinationHandler;
    
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
	      streamSet.initializeNonPersistent(txManager);
	    }
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "initializeNonPersistent");     
  }
      
  /**
   * Return the count of ProtocolItems (for diagnostic use)
   * 
   * @return DestinationHandler
   */
  public int countProtocolItems()
    throws MessageStoreException
  {
    if (tc.isEntryEnabled())  
      SibTr.entry(tc, "countProtocolItems");      

    int count = 0;
    
    NonLockingCursor cursor 
      = newNonLockingItemCursor(new ClassEqualsFilter(StreamSet.class));
           
    AbstractItem item = cursor.next();
    while (null != item)
    {
      //ProtocolItem protocolItem = (ProtocolItem)item;
      count++;
      item = cursor.next();
    }
 
    if (tc.isEntryEnabled())  
      SibTr.exit(tc, "countProtocolItems", new Integer(count));
            
    return count;
  }
  
  /**
   * Return the destinationHandler that the protocol itemstream is associated 
   * with 
   */
  public BaseDestinationHandler getDestinationHandler()
  {
    if (tc.isEntryEnabled())  
    {
      SibTr.entry(tc, "getDestinationHandler");
      SibTr.exit(tc, "getDestinationHandler", destinationHandler);
    }      
    return destinationHandler;
  }
}
