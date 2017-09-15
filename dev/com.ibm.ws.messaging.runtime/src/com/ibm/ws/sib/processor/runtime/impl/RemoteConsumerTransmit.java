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
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.AOStream;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.runtime.DeliveryStreamType;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.IndoubtAction;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable;
import com.ibm.ws.sib.processor.runtime.anycast.AOStreamIterator;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This object represents the requests that have come from a remote ME for a message
 * on this destination ME.
 */
public class RemoteConsumerTransmit extends AbstractControlAdapter implements SIMPRemoteConsumerTransmitControllable
{
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
	private static final TraceComponent tc =
		SibTr.register(
	RemoteConsumerTransmit.class,
			SIMPConstants.MP_TRACE_GROUP,
			SIMPConstants.RESOURCE_BUNDLE);
	
  // The AOStream that contains the ticks for this RME to DME remote get
	private AOStream _aoStream;
  // the messageProcessor
  private MessageProcessor _messageProcessor;
  // The destination that is been got from
  private AnycastOutputHandler _aoh;
	
  /**
   * Constructor for the RemoteConsumerTransmit. This is passed the AOStream for which
   * this object will give information about. This object is created for each AOStream that
   * is on the DME i.e. for each remote get request made on the localizaing destinations ME.
   * 
   * @param aoStream
   * @param messageProcessor
   * @param destination
   */
	public RemoteConsumerTransmit(AOStream aoStream, 
                                AnycastOutputHandler aoh)
	{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "RemoteConsumerTransmit", new Object[]{aoStream, aoh});

		_aoStream = aoStream;
    _messageProcessor = aoh.getMessageProcessor();
    _aoh = aoh;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "RemoteConsumerTransmit");
	}

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable#getTransmitMessageRequestIterator()
   */
  public SIMPIterator getTransmitMessageRequestIterator()
  {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getTransmitMessageRequestIterator");
		
  	AOStreamIterator aoStreamIterator = new AOStreamIterator(_aoStream, _messageProcessor, _aoh);
    
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getTransmitMessageRequestIterator", aoStreamIterator);
					
		return aoStreamIterator;
  }

  /**
   * The anycast protocol contains no guesses.
   * 
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#containsGuesses()
   */
  public boolean containsGuesses()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "containsGuesses");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "containsGuesses", Boolean.FALSE);
    return false;
  }

  /**
   * The anycast protocol contains no indoubt messages 
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#getCurrentMaxIndoubtMessages(int, int)
   */
  public int getCurrentMaxIndoubtMessages(int priority, int COS)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getCurrentMaxIndoubtMessages", new Object[]{new Integer(priority), new Integer(COS)});
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getCurrentMaxIndoubtMessages");
    return 0;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#forceFlushAtSource()
   */
  public void forceFlushAtSource()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "forceFlushAtSource");
    
    _aoh.forceFlushAtSource(_aoStream.getRemoteMEUuid(), _aoStream.getGatheringTargetDestUuid());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "forceFlushAtSource");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetTransmitControllable#clearMessagesAtSource(byte)
   */
  public void clearMessagesAtSource(IndoubtAction indoubtAction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "clearMessagesAtSource", indoubtAction);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "clearMessagesAtSource");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetControllable#getType()
   */
  public DeliveryStreamType getType()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getType");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getType", DeliveryStreamType.ANYCAST_SOURCE);
      
    return DeliveryStreamType.ANYCAST_SOURCE;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getId");
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getId", _aoStream.getID());
    return _aoStream.getID();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getName");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getName", null);
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(_aoStream == null || _aoStream.itemStream == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"RemoteConsumerTransmit.assertValidControllable",
                          "1:252:1.44", 
                          _aoStream},
            null));
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, finalE);
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "dereferenceControllable");
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "dereferenceControllable");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "registerControlAdapterAsMBean");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "registerControlAdapterAsMBean");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "deregisterControlAdapterMBean");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "deregisterControlAdapterMBean");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "runtimeEventOccurred", event);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "runtimeEventOccurred");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable#getNumberOfQueuedMessages()
   */
  public int getNumberOfQueuedMessages()
  {
    //this value is the number of requests in the AOValue state
    int count = 0;
    
    NonLockingCursor cursor = null;
    
    try
    {
      cursor = _aoStream.itemStream.newNonLockingItemCursor(null);
      cursor.allowUnavailableItems();
      AOValue item = (AOValue)cursor.next();
      while(item != null)
      {
        count ++;
        item = (AOValue)cursor.next();
      }      
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.RemoteConsumerTransmit.getNumberOfQueuedMessages",
        "1:340:1.44", 
        this);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.RemoteConsumerTransmit.getNumberOfQueuedMessages", 
                       "1:344:1.44", 
                       SIMPUtils.getStackTrace(e) });
      count = -1;
        
    }
    finally
    {
      if (cursor != null)
        cursor.finished();     
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfQueuedMessages", new Integer(count));
    return count;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable#getQueuedMessageByID(java.lang.String)
   */
  public SIMPQueuedMessageControllable getQueuedMessageByID(String ID)
    throws SIMPInvalidRuntimeIDException, SIMPControllableNotFoundException, SIMPException
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
      FFDCFilter.processException(
         e,
         "com.ibm.ws.sib.processor.runtime.RemoteConsumerTransmit.getQueuedMessageByID",
         "1:379:1.44", 
         this);
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getQueuedMessageByID", e);
      throw new SIMPInvalidRuntimeIDException(e);
    }
    
    SIMPMessage message = null;

    try
    {
      message = (SIMPMessage)_aoStream.itemStream.findById(messageID);
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.RemoteConsumerTransmit.getQueuedMessageByID",
          "1:398:1.44", 
          this);
      SibTr.exception(tc, e);
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
         SibTr.exit(tc, "getQueuedMessageByID", e);
       throw new SIMPRuntimeOperationFailedException(e);
    }

    if(message == null)
    {
      SIMPControllableNotFoundException e =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"RemoteConsumerTransmit.assertValidControllable",
                          "1:413:1.44", 
                          _aoStream},
            null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getQueuedMessageByID");
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuedMessageByID");
      
    return (SIMPQueuedMessageControllable) message.getControlAdapter();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable#getQueuedMessageIterator()
   */
  public SIMPIterator getQueuedMessageIterator() throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessageIterator");

    List<ControlAdapter> messages = new ArrayList<ControlAdapter>();
    NonLockingCursor cursor = null;
    try
    {
      cursor = _aoStream.itemStream.newNonLockingItemCursor(null);
      cursor.allowUnavailableItems();
      AOValue item = (AOValue)cursor.next();
      while(item != null)
      {
        try
        {
          // It's possible to get a null adapter back 
          ControlAdapter cAdapter = item.getControlAdapter();
          if(cAdapter != null)
            messages.add(cAdapter);
          
          item = (AOValue)cursor.next();
        }
        catch (NotInMessageStore ef)
        {
          // No FFDC code needed
          // The message has already been consumed. Trace the exception but allow processing to continue.
          SibTr.exception(tc, ef);
        }
      }      
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.RemoteConsumerTransmit.getQueuedMessageIterator",
        "1:467:1.44", 
        this);
        
      SIMPRuntimeOperationFailedException e1 =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"RemoteConsumerTransmit.getQueuedMessageIterator",
                          "1:475:1.44", 
                          e,
                          _aoStream.streamId},
            null), e);
            
      SibTr.exception(tc, e1);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getQueuedMessageIterator", e1);
      throw e1;
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
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable#getNumberOfCompletedRequests()
   */
  public long getNumberOfCompletedRequests()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfCompletedRequests"); 
    long completedRequests = 
      getNumberOfRequestsReceived() - getNumberOfCurrentRequests();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfCompletedRequests", new Long(completedRequests));
    return completedRequests; 
  }
  
	/* (non-Javadoc)
	 * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable#getNumberOfRequestsReceived()
	 */
	public long getNumberOfRequestsReceived()
	{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfRequestsReceived");
      
		long returnValue = _aoStream.getTotalRequestsReceived();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfRequestsReceived", new Long(returnValue));
    return returnValue; 
	}
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerTransmitControllable#getNumberOfRequestsReceived()
   */
  public long getNumberOfCurrentRequests()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfCurrentRequests");
    //request, value, rejected and accepted messages are all counted
    long returnValue = _aoStream.getNumberOfRequestsInState(TickRange.Requested);
    returnValue+=_aoStream.getNumberOfRequestsInState(TickRange.Value);
    returnValue+=_aoStream.getNumberOfRequestsInState(TickRange.Rejected);
    returnValue+=_aoStream.getNumberOfRequestsInState(TickRange.Accepted);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfCurrentRequests", new Long(returnValue));
    return returnValue;     
  }
  
  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable#getStreamID()
   */
  public SIBUuid12 getStreamID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamID");
    SIBUuid12 id = _aoStream.streamId;  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamID", id); 
    return id;           
  }
  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable#getStreamState()
   */
  public StreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamState");
    StreamState returnValue = _aoStream.getStreamState();      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamState", returnValue);
    return returnValue;
  }
  
  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid");    
    String returnString = 
      this._aoStream.getRemoteMEUuid().toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid", returnString);
    return returnString;            
  }

  public HealthState getHealthState() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getHealthState");
      SibTr.exit(tc, "getHealthState", HealthState.GREEN);
    }  
    return new HealthStateTree();
  }

  public String getDestinationUuid() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestinationUuid");    
    
    SIBUuid12 destUuid = _aoStream.getGatheringTargetDestUuid();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestinationUuid", destUuid);
    
    return String.valueOf(destUuid);  
  }

  public boolean isGathering() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isGathering");    
    
    boolean isGathering =
      _aoStream.getGatheringTargetDestUuid()!=null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isGathering", Boolean.valueOf(isGathering));
    
    return isGathering;  
  }  
}
