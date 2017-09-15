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
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.PubSubInputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.filters.SourceStreamSetFilter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/**
 * @author tevans
 * 
 * A ProtocolItemStream is used by the DestinationHandler for storing 
 * target stream protocol state for guaranteed delivery.
 */
public class SourceProtocolItemStream extends ProtocolItemStream
{

  /**
   * Trace.
   */
  private static final TraceComponent tc =
    SibTr.register(
      SourceProtocolItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
 
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public SourceProtocolItemStream()
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
  public SourceProtocolItemStream(
    BaseDestinationHandler destinationHandler,
    Transaction transaction)
    throws OutOfCacheSpace,
           MessageStoreException
  {
    super(destinationHandler, transaction);
  }
      
  /**
   * Rebuild Guaranteed Delivery Target Streams 
   * 
   * Feature 171905.99
   * 
   * 
   * @throws MessageStoreException
   */   
  public void reconstituteSourceStreams(DestinationHandler dest, PtoPOutputHandler outputHandler, int startMode) 
  
  throws MessageStoreException, SIDiscriminatorSyntaxException, SIErrorException, SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteSourceStreams", new Integer(startMode));

    
    /*
     * Iterate through all contained Protocol Items, looking for StreamSet
     * associated with source. Note that outputHandler passed in is null if this 
     * is Pubsub. 
     */ 
     
    SourceStreamSetFilter filter = null;
    if( dest.isPubSub())
    { 
      filter = new SourceStreamSetFilter(null);
    }
    else
    {
      filter = new SourceStreamSetFilter(outputHandler.getTargetMEUuid());
    }
    
    NonLockingCursor cursor = null;
    
    try
    {
      cursor = newNonLockingItemCursor(filter);    
           
	    AbstractItem item = null;
	    
	    //should only be one match!!
	    while (null != (item = cursor.next()))
	    {
	      StreamSet streamSet = (StreamSet)item;
          streamSet.initializeNonPersistent(dest.getTxManager());
	      
	      // Don't do flush if we are asked to start in recovery mode 
	      if(   ((startMode & JsConstants.ME_START_FLUSH ) == JsConstants.ME_START_FLUSH )
	         && ((startMode & JsConstants.ME_START_RECOVERY ) == 0 ) )
	      {
	        // Log message to console saying we have started flush for this steamSet
	        // parmeters are:- ME name, streamSet Uuid, destinationName
	        SibTr.info(
	          tc,
	          "FLUSH_INITIATED_MESSAGE_CWSIP0781",
	          new Object[] { dest.getMessageProcessor().getMessagingEngineName(),
	                        streamSet.getStreamID(),
	                        dest.getName() });
	      }
	      try
	      {
	        if( dest.isPubSub())
	        {
	          PubSubInputHandler inputHandler = (PubSubInputHandler)dest.getInputHandler();
	          inputHandler.reconstitutePubSubSourceStreams(streamSet, startMode);
	        }
	        else
	        {        
	          outputHandler.reconstitutePtoPSourceStreams(streamSet, startMode);
	        }
	      }
	      catch (SIResourceException e)
	      {
	        // FFDC
	        FFDCFilter.processException(
	          e,
	          "com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream.reconstituteSourceStreams",
	          "1:179:1.29",
	          this);
	        
	        // Don't do flush if we are asked to start in recovery mode 
	        if(   ((startMode & JsConstants.ME_START_FLUSH ) == JsConstants.ME_START_FLUSH )
	           && ((startMode & JsConstants.ME_START_RECOVERY ) == 0 ) )
	        {
	          // Log message to console saying flush for this steamSet has failed
	          // parmeters are:- ME name, streamSet Uuid, destinationName, exception.
	          SibTr.error(
	            tc,
	            "FLUSH_FAILED_MESSAGE_CWSIP0782",
	            new Object[] {dest.getMessageProcessor().getMessagingEngineName(),
	                          streamSet.getStreamID(),
	                          dest.getName(),
	                          e });
	        }
	        
	        // Rethrow exception after logging error
	        if (tc.isEntryEnabled()) SibTr.exit(tc, "reconstituteSourceStreams");
	        throw(e);
	      
	      }
	    }
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }
    
    if (tc.isEntryEnabled())  
      SibTr.exit(tc, "reconstituteSourceStreams");      
  }  
}
