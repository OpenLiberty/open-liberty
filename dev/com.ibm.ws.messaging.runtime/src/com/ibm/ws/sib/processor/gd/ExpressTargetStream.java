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

package com.ibm.ws.sib.processor.gd;

import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MessageDeliverer;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.impl.TargetStreamControl;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

public class ExpressTargetStream extends ControllableStream implements TargetStream
{
  private MessageDeliverer deliverer;

  private static TraceComponent tc =
    SibTr.register(
      ExpressTargetStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  private long lastTick;
  private long messagesReceived;
  private long timeLastMsgReceived;
  
  private SIBUuid12 streamSetUUID;
  SIBUuid8 remoteEngineUUID;  
  
  // The current state of this stream.
  private ExpressTargetStreamState streamState = ExpressTargetStreamState.ACTIVE;

  
  
  /**
   * An enumeration for the possible states for this type of target stream 
   * @author tpm100
   */
  public static class ExpressTargetStreamState extends TargetStream.TargetStreamState
  {
    //TODO In future releases we might expose new stream states
    public static final ExpressTargetStreamState ACTIVE = 
      new ExpressTargetStreamState("Active", 1);

    private ExpressTargetStreamState(String _name, int _id)
    {
      super(_name, _id);
    }   
  }

  public ExpressTargetStream(MessageDeliverer deliverer, SIBUuid8 remoteMEUuid, SIBUuid12 _streamSetUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "ExpressTargetStream", new Object[]{deliverer, remoteMEUuid, _streamSetUuid});

    this.remoteEngineUUID = remoteMEUuid;
    lastTick = -1L;
    this.deliverer = deliverer;
    streamSetUUID = _streamSetUuid;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ExpressTargetStream", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.OutputStream#getLDprefix()
   */
  public long getCompletedPrefix()
  {
    return lastTick;
  }
  
  
  private boolean isStreamFull(JsMessage msg) throws SIException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isStreamFull", msg);
    
    //We are only interested if the message will overflow the 
    //destination.
    boolean isStreamFull = false;
    int blockingReason = deliverer.checkAbleToAcceptMessage(msg.getRoutingDestination());
    if(blockingReason != DestinationHandler.OUTPUT_HANDLER_FOUND)
      isStreamFull = true;   
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isStreamFull", new Boolean(isStreamFull));
    return isStreamFull;      
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.OutputStream#writeValue(com.ibm.ws.sib.mfp.JsMessage)
   */
  public synchronized void writeValue(MessageItem msgItem)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "writeValue", msgItem);
    
    timeLastMsgReceived = System.currentTimeMillis();
      
    if(msgItem.getMessage().getGuaranteedValueValueTick() > lastTick)
    {  
      try
      {
        //Check if the stream is full.
        //For express target streams, we simply look at whether the
        //number of messages on the destination is higher than the destination
        //high limit. 
        //See defect 24425
        if(isStreamFull(msgItem.getMessage()))
        {
          //there is no room for this message so we discard it
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          {
            SibTr.debug(tc, "discarding message as the stream is full.");
          }
        }
        else
        {
          deliverer.deliverExpressMessage(msgItem,this);
          messagesReceived++;  
        }
      }
      catch (SIException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.ExpressTargetStream.writeValue",
          "1:181:1.54",
          this);

        SibTr.exception(tc, e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "writeValue", e);

        throw new SIErrorException(e);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "writeValue");
  }

  public long reconstituteCompletedPrefix( long prefix ) 
  {
     return lastTick = prefix;
  }

  public void setCompletedPrefix( long prefix )
  {
     // update the completedPrefix 
     lastTick = prefix;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#writeSilence(com.ibm.ws.sib.mfp.control.ControlSilence)
   */
  public void writeSilence(ControlSilence m)
  {
    // should never get called
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#writeSilence(com.ibm.ws.sib.processor.impl.store.items.MessageItem)
   */
  public void writeSilence(MessageItem m)
  {
    // should never get called   
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#processAckExpected(long)
   */
  public void processAckExpected(long stamp)
  {
    //should never get called
  }

  /**
   * Flush this stream by discarding any nacks we may be waiting on
   * (all such ticks automatically become finality).  When this
   * process is complete, any persistent state for the stream may be
   * discarded.
   *
   * @throws GDException if an error occurs in writeRange.
   */
  public void flush()
  {
    // no op for express streams
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchPrecommit(com.ibm.ws.sib.msgstore.transactions.Transaction)
   */
  public void batchPrecommit(TransactionCommon currentTran)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchCommitted()
   */
  public void batchCommitted()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchRolledBack()
   */
  public void batchRolledBack()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#getLastKnownTick()
   */
  public long getLastKnownTick()
  {
    return lastTick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getStateStream()
   */
  public StateStream getStateStream()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#writeSilenceForced(long)
   */
  public void writeSilenceForced(long tick)
  {
  }
 
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.ControllableStream#getPriority()
   */
  protected int getPriority()
  {
    return 0;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.ControllableStream#getReliability()
   */
  protected Reliability getReliability()
  {
    return Reliability.EXPRESS_NONPERSISTENT;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#getControlAdapter
   */
  public TargetStreamControl getControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getControlAdapter");

    TargetStreamControl targetStreamControl = 
        new TargetStreamControl(remoteEngineUUID, this, streamSetUUID, 
        getReliability(), getPriority());
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getControlAdapter", targetStreamControl);
    return targetStreamControl;    
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#getAllMessagesOnStream
   */
  public List<MessageItem> getAllMessagesOnStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getAllMessagesOnStream");
    
    List<MessageItem> returnValue = 
      new LinkedList<MessageItem>();    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getAllMessagesOnStream", returnValue);      
    //there are no active messages on an express stream so we
    //return an empty array
    return returnValue;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#getNumberOfMessagesReceived
   */
   public long getNumberOfMessagesReceived()
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.entry(tc, "getNumberOfMessagesReceived");
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.exit(tc, "getNumberOfMessagesReceived", new Long(messagesReceived));
     
     return messagesReceived;
   }
   
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.TargetStream#getStreamState
   */
  public TargetStreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getStreamState");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getStreamState", streamState);
        
    return streamState;
  }
  
  public long countAllMessagesOnStream()
  {
    return 0;
  }

  public long getLastMsgReceivedTimestamp() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLastMsgReceivedTimestamp");    
   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    SibTr.exit(tc, "getLastMsgReceivedTimestamp", new Long(timeLastMsgReceived));
    
   return timeLastMsgReceived;  
  } 
   


  

}
