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

import java.util.Date;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.RangeList;
import com.ibm.ws.sib.processor.gd.SourceStream;
import com.ibm.ws.sib.processor.gd.Stream;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageControllable;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * ControlAdapter for a message being transmitted to a remote messaging engine
 */
public class TransmitMessage extends AbstractControlAdapter implements SIMPTransmitMessageControllable
{
  private Stream _stream;
  private long _tick;
  private long _previousTick;
  private long _messageID;
  private DownstreamControl _downControl;
  private SIMPMessage _msg; // Only set if this represents an uncommitted msg
  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  private static final TraceComponent tc =
    SibTr.register(
      TransmitMessage.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  public TransmitMessage(long msgStoreid, Stream stream, DownstreamControl downControl)
    throws SIResourceException
  {
    this._messageID = msgStoreid;
    this._downControl = downControl;
    SIMPMessage msg = getSIMPMessage();
    this._tick = msg.getMessage().getGuaranteedValueValueTick();
    this._stream = stream;
    this._previousTick = msg.getMessage().getGuaranteedValueStartTick()-1;
  }

  public TransmitMessage(SIMPMessage msg, Stream stream)    
  {
    this._msg = msg;
    this._tick = msg.getMessage().getGuaranteedValueValueTick();
    this._stream = stream;
    this._previousTick = msg.getMessage().getGuaranteedValueStartTick()-1;
  }

  private TickRange getTickRange()
  {
    return _stream.getTickRange(_tick);
  }
 
  public SIMPMessage getSIMPMessage() 
   throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSIMPMessage");

    SIMPMessage message = _msg;
    if (message==null) // If committed msg, look it up
      message = _downControl.getValueMessage(_messageID);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSIMPMessage", message);
    return message;
  }

  public JsMessage getJsMessage() 
    throws SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getJsMessage");

    assertValidControllable();

    JsMessage jsMessage = null;
    try
    {
      jsMessage = getSIMPMessage().getMessage();
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed  
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getJsMessage", e);
      throw new SIMPRuntimeOperationFailedException(e);
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getJsMessage", jsMessage);
    return jsMessage;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getState()
   */
  public String getState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState");

    //The only valid states in the SourceStream are Uncommitted,
    //Value and Completed so all tick ranges will be in one of
    //these states
    TickRange range = getTickRange();
    
    long firstSendPending = RangeList.INFINITY;
    if(_stream instanceof SourceStream)
      firstSendPending = ((SourceStream)_stream).getFirstMsgOutsideWindow();
  
    //we're actually only interested in Uncommitted and Value
    //Uncommitted maps to COMMITTING
    //Value before the in-doubt cursor are PENDING_ACK
    //Value after the in-doubt cursor are PENDING_SEND
    State state = State.COMPLETE;
    if(range.type == TickRange.Uncommitted)
      state = State.COMMITTING;
    else if(range.type == TickRange.Value && _tick < firstSendPending)
      state = State.PENDING_ACKNOWLEDGEMENT;
    else if(range.type == TickRange.Value && _tick >= firstSendPending)
      state = State.PENDING_SEND;
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", state);

    return state.toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getStartTick()
   */
  public long getStartTick()
  {
    return _tick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getEndTick()
   */
  public long getEndTick()
  {
    return _tick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#removeMessage(boolean)
   */
  public void moveMessage(boolean discard) throws SIMPControllableNotFoundException, 
                                                    SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "moveMessage", Boolean.valueOf(discard));
      
    assertValidControllable();

    // remove the message from the MessageStore
    QueuedMessage queuedMessage = null;
    try
    {
      queuedMessage = (QueuedMessage)getSIMPMessage().getControlAdapter();
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed  
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "moveMessage", e);
      throw new SIMPRuntimeOperationFailedException(e);
    } 
    
    // A null message implies it's already gone from the MsgStore
    if(queuedMessage != null)
    {
      queuedMessage.moveMessage(discard);
    
      // Remove message from the source stream
      try
      {
        _stream.writeSilenceForced(_tick);
      }
      catch (SIException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.TransmitMessage.moveMessage",
          "1:245:1.42",
          this);
    
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0003",
              new Object[] {"TransmitMessage.moveMessage",
                  "1:253:1.42",
                            e,
                            new Long(_tick)},
              null), e);

        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "moveMessage", finalE);
        throw finalE;             
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "moveMessage");
   
  }  
  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getSequenceID
   */
  public long getSequenceID()
  {
    return _tick;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getProducedTime
   */  
  public Date getProducedTime()
  {
    Date producedTime = null;
    try
    {
      producedTime =  new Date(getSIMPMessage().getMessage().getTimestamp().longValue());
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed  
      SibTr.exception(tc, e);
    } 
     
    return producedTime;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getPreviousSequenceId
   */ 
  public long getPreviousSequenceId()
  {
    return _previousTick; 
  }

  /*
   * Allow the stream to be accessed by others
   */
  public Stream getStream()
  {
    return _stream;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getId");
    String id = ""+_messageID;
      SibTr.exit(tc, "getId", id);
    return id;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return null;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    TickRange range = getTickRange();
    if(range.type != TickRange.Uncommitted && range.type != TickRange.Value)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"TransmitMessage.assertValidControllable",
                "1:346:1.42",
                          new Long(_tick)},
            null));
  
      SibTr.exception(tc, finalE);
      throw finalE;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    _tick = -1;
    _stream = null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getTransactionId()
   */
  public String getTransactionId()
  {
    String tranID = null;
    try
    {
      PersistentTranId pTranID = getSIMPMessage().getTransactionId();
      
      if (pTranID != null)
      {
        tranID = pTranID.toTMString(); 
      }
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed  
      SibTr.exception(tc, e);
    }      

    return tranID;
  }
  
  public void putMessageToExceptionDestination()
  {
    
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getMEArrivalTimestamp()
   */
  public long getMEArrivalTimestamp() throws SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMEArrivalTimestamp");

    long timestamp =  getJsMessage().getCurrentMEArrivalTimestamp();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMEArrivalTimestamp", new Long(timestamp));
    return timestamp;
  }
  
  /**
   * Class to represent a transmit message identifier. Needs 3 parts. Reliability
   * and Priority to locate the stream within the streamset. Tick to identify
   * the message on the stream.
   *
   */
  
  class TransmitID {
    
    Reliability reliability;
    int priority;
    long tick;
    
    TransmitID(String id)
    {
      String[] tokens = id.split(":");
      
      this.reliability = Reliability.getReliabilityByName(tokens[0]);
      this.priority = Integer.parseInt(tokens[1]);
      this.tick = Long.parseLong(tokens[2]);      
    }
    
    /**
     * @return
     */
    public int getPriority()
    {
      return priority;
    }

    /**
     * @return
     */
    public Reliability getReliability()
    {
      return reliability;
    }

    /**
     * @return
     */
    public long getTickValue()
    {
      return tick;
    }

  }
}
