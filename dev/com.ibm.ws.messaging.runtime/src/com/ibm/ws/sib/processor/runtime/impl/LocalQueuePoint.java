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
package com.ibm.ws.sib.processor.runtime.impl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.TargetStreamManager;
import com.ibm.ws.sib.processor.impl.AOBrowserSession;
import com.ibm.ws.sib.processor.impl.AOBrowserSessionKey;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable;
import com.ibm.ws.sib.processor.runtime.anycast.RemoteBrowserIterator;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a queueing point to perform dynamic
 * control operations.
 * <p>
 * The operations in this interface are specific to a queueing point.
 */
public class LocalQueuePoint extends AbstractRegisteredControlAdapter implements SIMPLocalQueuePointControllable
{
  private BaseDestinationHandler destinationHandler;

  private ProducerInputHandler inputHandler;
  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  private static TraceComponent tc =
    SibTr.register(
      LocalQueuePoint.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  /**
   * A reference back to the item stream for which this is an adapter.
   */
  private PtoPLocalMsgsItemStream itemStream;
  private SIMPMessageHandlerControllable handlerControl;
  private String messageHandlerName;
  private String id;

  public LocalQueuePoint(MessageProcessor messageProcessor,
                         PtoPLocalMsgsItemStream itemStream)
  {
    super(messageProcessor, ControllableType.QUEUE_POINT );
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "LocalQueuePoint",
        new Object[] { messageProcessor, itemStream });

    this.itemStream = itemStream;
    
    destinationHandler = itemStream.getDestinationHandler();
    handlerControl = (SIMPMessageHandlerControllable) destinationHandler.getControlAdapter();
    inputHandler = (ProducerInputHandler) destinationHandler.getInputHandler();
    messageHandlerName = handlerControl.getName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "LocalQueuePoint", this);
  }

  /**
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalizationControllable#setDestinationHighMsgs(long)
   */
  public void setDestinationHighMsgs(long newDestHighMsgs)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setDestinationHighMsgs", new Long(newDestHighMsgs));
    
    itemStream.setDestHighMsgs(newDestHighMsgs);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "setDestinationHighMsgs");
  }

  /**
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalizationControllable#setDestinationLowMsgs(long)
   */
  public void setDestinationLowMsgs(long newDestLowMsgs)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setDestinationLowMsgs", new Long(newDestLowMsgs));
    
    itemStream.setDestHighMsgs(newDestLowMsgs);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "setDestinationLowMsgs");
  }

  /**
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalizationControllable#getDestinationHighMsgs()
   *
   */
  public long getDestinationHighMsgs()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDestinationHighMsgs");
    
    long destHighMsgs = itemStream.getDestHighMsgs();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDestinationHighMsgs", new Long(destHighMsgs));
    
    return destHighMsgs ;
  }

  /**
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalizationControllable#getDestinationLowMsgs()
   *
   */
  public long getDestinationLowMsgs()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDestinationLowMsgs");
    
    long destLowMsgs = itemStream.getDestLowMsgs();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDestinationLowMsgs", new Long(destLowMsgs));
    
    return destLowMsgs ;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalizationControllable#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isSendAllowed");
    
    boolean isSendAllowed = itemStream.isSendAllowed();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isSendAllowed", new Boolean(isSendAllowed));
    
    return isSendAllowed ;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalizationControllable#setSendAllowed(boolean)
   */
  public void setSendAllowed(boolean newSendAllowedValue )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setSendAllowed", new Boolean(newSendAllowedValue));
    
    itemStream.setSendAllowed( newSendAllowedValue );
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setSendAllowed", new Boolean(newSendAllowedValue));
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalQueueControllable#getMessageHandler()
   */
  public SIMPMessageHandlerControllable getMessageHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getMessageHandler");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getMessageHandler", handlerControl);

    return handlerControl ;      
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalQueueControllable#getQueuedMessageIterator()
   */
  public SIMPIterator getQueuedMessageIterator() 
  throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessageIterator");

    List<ControlAdapter> messages = new ArrayList<ControlAdapter>();
    
    NonLockingCursor cursor = null;
    try
    {
      cursor = itemStream.newNonLockingItemCursor(null);
      cursor.allowUnavailableItems();
      MessageItem item = (MessageItem)cursor.next();
      while(item != null)
      {
        // force the arrival time to be written to the jsMessage  
        item.forceCurrentMEArrivalTimeToJsMessage();
        
        // It's possible to get a null adapter back 
        ControlAdapter cAdapter = item.getControlAdapter();
        if(cAdapter != null)
          messages.add(cAdapter);
        
        item = (MessageItem)cursor.next();      
      }      
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.getQueuedMessageIterator",
          "1:264:1.58", 
          this);
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getQueuedMessageIterator", e);
      throw new SIMPRuntimeOperationFailedException(e);
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuedMessageIterator");
      
    return new BasicSIMPIterator(messages.iterator());    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable#deleteAllQueuedMessages(boolean)
   */
  public void moveMessages(boolean discard) throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteAllQueuedMessages", new Boolean(discard));

    assertValidControllable();
    
    SIMPRuntimeOperationFailedException runtimeException = null;
    
    SIMPIterator msgItr = getQueuedMessageIterator();
    while(msgItr.hasNext())
    {
      QueuedMessage msg = (QueuedMessage) msgItr.next();
      try
      {
        msg.moveMessage(discard);
      }
      catch (SIMPControllableNotFoundException e)
      {
        //this probably means the message was already deleted while we were
        //busy ... we'll log the exception and move on
        
        FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.deleteAllQueuedMessages",
            "1:311:1.58",
            this);
      
        SibTr.exception(tc, e);
      }
      catch (SIMPRuntimeOperationFailedException e)
      {
        //this could happen for a number of reasons. We will remember the first
        //one of these and carry on trying to delete the rest of the messages.
        
        FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.deleteAllQueuedMessages",
            "1:324:1.58",
            this);
      
        SibTr.exception(tc, e);
        
        if(runtimeException == null) runtimeException = e;
      }
    }
    
    if(runtimeException != null)
    {
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0003",
              new Object[] {"QueuedMessage.removeMessage",
                            "1:340:1.58",
                            runtimeException,
                            id},
              null), runtimeException);
    
        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeMessage", finalE);
        throw finalE;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteAllQueuedMessages");         
  }
  
  public SIMPQueuedMessageControllable getQueuedMessageByID(String ID)
      throws SIMPInvalidRuntimeIDException,
             SIMPControllableNotFoundException,
             SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessageByID", new Object[] { ID });

    long messageID = AbstractItem.NO_ID;
    try
    {
      messageID = Long.parseLong(ID);
    }
    catch(NumberFormatException e)
    {
//    FFDC
      FFDCFilter.processException(
         e,
         "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.getQueuedMessageByID",
         "1:373:1.58",
         this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getQueuedMessageByID", "SIMPInvalidRuntimeIDException");
      throw new SIMPInvalidRuntimeIDException(e);
    }
    
    // PK17340 begin.
    Object messageObject = null;
    try
    {
      messageObject = itemStream.findById(messageID);
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.getQueuedMessageByID",
          "1:391:1.58", 
          this);
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getQueuedMessageByID", e);
      throw new SIMPRuntimeOperationFailedException(e);
    }
    
    SIMPMessage message = null; 
    if (messageObject instanceof SIMPMessage)
    {
    	// messageObject may not be a SIMPMessage if the id provided is for an item 
    	// in the message store that is not a message.
    	// ItemStream#findById returns null if the object is not found, so keeping
    	// message null is appropriate if the messageObject is not a SIMPMessage.
      message = (SIMPMessage) messageObject;
    }
    // PK17340 end

    if(message == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getQueuedMessageByID", "SIMPControllableNotFoundException");
      throw new SIMPControllableNotFoundException(
        nls.getFormattedMessage(
          "MESSAGE_EXISTS_ERROR_CWSIP0572",
            new Object[] {
              ID,
              destinationHandler.getName() },
              null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuedMessageByID");
      
    return (SIMPQueuedMessageControllable) message.getControlAdapter();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalQueueControllable#getP2PInboundReceiverIterator()
   */
  public SIMPIterator getPtoPInboundReceiverIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPtoPInboundReceiverIterator");

    TargetStreamManager tsm = inputHandler.getTargetStreamManager();
    SIMPIterator itr = tsm.getTargetStreamSetControlIterator();
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPtoPInboundReceiverIterator", itr);

    return itr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getName()
   */
  public String getName()
  {
    return messageHandlerName;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getId()
   */
  public String getId()
  {
    try
    {
      if(id == null) 
        id = destinationHandler.getUuid()+
             RuntimeControlConstants.QUEUE_ID_INSERT+
             itemStream.getID();
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.getId",
          "1:471:1.58", 
          this);
      SibTr.exception(tc, e);
    }
    
    return id;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable#getNumberOfQueuedMessages()
   */
  public long getNumberOfQueuedMessages()
  {
    long count = 0;
    try
    {
      count = itemStream.getStatistics().getTotalItemCount();
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.getNumberOfQueuedMessages",
          "1:494:1.58", 
          this);
      SibTr.exception(tc, e);
    }    
    return count;
  }

  /**
   * Registers this control adapter with the mbean interface.
   * <p>
   * Will not re-register if already registered.
   */
  public synchronized void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry( tc, "registerControlAdapterAsMBean" ); 
      
    if( isRegistered() || getMessageHandler().isTemporary()) 
    {
      // We're a temporary queue or Registered already. Don't register a 2nd time.
    }
    else
    {
      super.registerControlAdapterAsMBean();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit( tc, "registerControlAdapterAsMBean" ); 
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkValidControllable");

    if(itemStream == null || !itemStream.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"LocalQueuePoint.assertValidControllable",
                          "1:539:1.58",
                          id},
            null));
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, finalE);
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");
    
    super.dereferenceControllable();
    itemStream = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getUuid()
   */
  public String getUuid()
  {
    LocalizationDefinition def = itemStream.getLocalizationDefinition();
    String uuid = null;
    if(def != null)
    {
      uuid = def.getUuid();
    }
    return uuid;
  }

 

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getRemoteEngineUuid()
   */
  public String getRemoteEngineUuid()
  {
    return itemStream.getLocalizingMEUuid().toString();
  }
  
  /**
   * toString representation of LocalQueuePoint
   */
  public String toString()
  {
    if (itemStream != null)
      return ((SIMPMessageHandlerControllable)itemStream.getDestinationHandler().getControlAdapter()).getName();
    
    return "LocalQueuePoint under construction.....";
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable#getRemoteBrowserReceiver()
   */
  public SIMPIterator getRemoteBrowserReceiverIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getRemoteBrowserReceiverIterator");
    
    AnycastOutputHandler aoh = destinationHandler.getAnycastOutputHandler();
    
    // Get the hashtable of all the browserSessions that are currently active
    Hashtable tableOfBrowserSessions = aoh.getBrowserSessions();
    
    // Get all the keys (AOBrowserSessionKey) that are contained in the table
    Set keySet = tableOfBrowserSessions.keySet();
    
    // Create our new list which we will passed to the RemoteBrowserIterator 
    List<AOBrowserSession> listOfBrowserSessions = new ArrayList<AOBrowserSession>();
    
    // For all the keys in the table
    for (Iterator iter = keySet.iterator(); iter.hasNext();)
    {
      // Get the first key
      AOBrowserSessionKey browserSessionKey = (AOBrowserSessionKey) iter.next();
      // Get the AOBrowserSession for this key
      AOBrowserSession aoBrowserSession = (AOBrowserSession) tableOfBrowserSessions.get(browserSessionKey);
      // Add the session to our new list
      listOfBrowserSessions.add(aoBrowserSession);
    }
    
    RemoteBrowserIterator remoteBrowserIterator = new RemoteBrowserIterator(listOfBrowserSessions.iterator());
          
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRemoteBrowserReceiverIterator", remoteBrowserIterator);
    return remoteBrowserIterator;
  } 
  
  public SIMPIterator getAllRemoteConsumerTransmitIterator() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAllRemoteConsumerTransmitIterator");
    
    SIMPIterator itr = 
      new BasicSIMPIterator(destinationHandler.getAOControlAdapterIterator());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAllRemoteConsumerTransmitIterator", itr);

    return itr;
  }

  public SIMPIterator getNonGatheringRemoteConsumerTransmitIterator() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNonGatheringRemoteConsumerTransmitIterator");

    Iterator<ControlAdapter> itr = destinationHandler.getAOControlAdapterIterator();
    
    // Extract the non-gathering streams
    ArrayList<SIMPRemoteConsumerTransmitControllable> nonGathAOstreams = 
      new ArrayList<SIMPRemoteConsumerTransmitControllable>();
    while(itr.hasNext())
    {
      SIMPRemoteConsumerTransmitControllable adapter = 
        (SIMPRemoteConsumerTransmitControllable)itr.next();
      if (!adapter.isGathering())
        nonGathAOstreams.add(adapter);      
    }

    SIMPIterator nonGathItr = 
      new BasicSIMPIterator(nonGathAOstreams.iterator());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNonGatheringRemoteConsumerTransmitIterator", nonGathItr);

    return nonGathItr;
  } 
  // 673411 start
/* The method returns the iterator containing the items stored on the queue point in messagestore. It retrieves 
 * the messages starting from position at fromIndex in the subcursor holding the linkedlist of all items, and 
 * ending at toIndex in the subcursor. The total retrieved items are equal to or less than totalNumberOfMsgsPerPage 
 * on console. 
 * The method iteratively calls the cursor.next(fromIndex) method and breaks when either counter equals toIndex 
 * or the totalQueuedMessages on the queuepoint is reached. Each call to cursor.next(fromIndex) returns a single 
 * item from messagestore which is positioned equal to fromIndex or greater than fromIndex in the linkedlist of items.
 * 
 */
public SIMPIterator getQueuedMessageIterator(int fromIndex, int toIndex, int totalNumberOfMsgsPerPage)
throws SIMPRuntimeOperationFailedException
{
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	SibTr.entry(tc, "getQueuedMessageIterator(int fromIndex= "+fromIndex+", int toIndex= "+toIndex+", int totalNumberOfMsgsPerPage= "+totalNumberOfMsgsPerPage);
	List<ControlAdapter> messages = new ArrayList<ControlAdapter>();
	NonLockingCursor cursor = null;
	int iCount = fromIndex-1;
	try
	{
	cursor = itemStream.newNonLockingItemCursor(null);
	cursor.allowUnavailableItems();
	long totalQueuedMessages=getNumberOfQueuedMessages();
	MessageItem item=(MessageItem)cursor.next(fromIndex-1);// 673411
	
	while(item != null)
	{			
			if(iCount >=toIndex || (iCount > totalQueuedMessages)){
			break;
			}
		
		// force the arrival time to be written to the jsMessage
		item.forceCurrentMEArrivalTimeToJsMessage();
		// It's possible to get a null adapter back
		ControlAdapter cAdapter = item.getControlAdapter();
		if(cAdapter != null)
		{
			if(((iCount >= (fromIndex-1))&& iCount <= toIndex) && messages.size() <= totalNumberOfMsgsPerPage)
			{
				messages.add(cAdapter);
				
			}
		}
		AbstractItem abItem=cursor.next(fromIndex-1);// 673411
			if(abItem!=null){
			item = (MessageItem)abItem;
			}else
			{
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      			SibTr.debug(tc," Item retrieved from messagestore is null at index"+iCount);
			break;
			}
		iCount++;
	}

	
	}
	catch (MessageStoreException e)
	{
	FFDCFilter.processException(e,"com.ibm.ws.sib.processor.runtime.LocalQueuePoint.getQueuedMessageIterator",
	"1:263:1.57",	this);
	SibTr.exception(tc, e);
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	SibTr.exit(tc, "getQueuedMessageIterator", e);
	throw new SIMPRuntimeOperationFailedException(e);
	}
	finally
	{
	if (cursor != null)
	cursor.finished();
	}

	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	SibTr.exit(tc, "getQueuedMessageIterator fromIndex="+fromIndex+" toIndex= "+toIndex+" totalMessagesPerpage= "+totalNumberOfMsgsPerPage);

	return new BasicSIMPIterator(messages.iterator());

} //673411-ends


}
