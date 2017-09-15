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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.SourceStream;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamTransmitControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A controllable view of an individual source stream
 * @author tpm100
 * 
 */
public class SourceStreamControl  extends AbstractControlAdapter 
                    implements SIMPDeliveryStreamTransmitControllable
{
  private static final TraceNLS nls = 
  TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  private static final TraceComponent tc =
  SibTr.register(
    SourceStreamControl.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);
   
  private SourceStream _sourceStream;
  private StreamSet _sourceStreamSet;
  private HealthState _healthState;
  private DownstreamControl _downControl;
  
  
  public SourceStreamControl(SourceStream sourceStream, StreamSet sourceStreamSet, DownstreamControl downControl)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "SourceStreamControl", 
      new Object[]{sourceStream, sourceStreamSet, downControl});
    _sourceStream = sourceStream;
    _sourceStreamSet = sourceStreamSet; 
    _downControl = downControl;
    _healthState = new HealthStateTree();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "SourceStreamControl", this);    
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamTransmitControllable#getReliability
   */
  public Reliability getReliability()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getReliability");
    Reliability rel =  _sourceStream.getReliability();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getReliability", rel);    
    return rel;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamTransmitControllable#getPriority
   */
  public int getPriority()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getPriority");
    int priority = _sourceStream.getPriority();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getPriority", new Integer(priority));
    return priority;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamTransmitControllable#getNumberOfActiveMessages
   */
  public int getNumberOfActiveMessages()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getNumberOfActiveMessages");
    int activeMessages = 
      (int)_sourceStream.countAllMessagesOnStream();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getNumberOfActiveMessages", new Integer(activeMessages));      
    return activeMessages;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamTransmitControllable#getTransmitMessageByID
   */
  public SIMPTransmitMessageControllable getTransmitMessageByID(String id)
  throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTransmitMessageByID", id);
      
    SIMPTransmitMessageControllable transmitMessage = null;
    try
    {
      //we should only get a transmit message if it is still
      //on the stream 
      Iterator<TickRange> messagesOnStream = 
        _sourceStream.getAllMessageItemsOnStream(true).iterator();
      while(messagesOnStream.hasNext())
      {
        long msgStoreId = -1;
        TickRange tr = messagesOnStream.next();
        if(tr.value == null)
        {
          // Only committed msgs have valid ids
          msgStoreId = tr.itemStreamIndex;
        
          if(Long.parseLong(id) == msgStoreId)
            transmitMessage=new LinkTransmitMessageControl(msgStoreId, _sourceStream, _downControl);
        }
      }
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed  
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getTransmitMessageByID", e);
      throw new SIMPRuntimeOperationFailedException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTransmitMessageByID", transmitMessage);
    return transmitMessage;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamTransmitControllable#getTransmitMessagesIterator
   */
  public SIMPIterator getTransmitMessagesIterator(int maxMsgs)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTransmitMessagesIterator");
    
    // TODO : Method needs to throw SIMPRuntimInvalid to admin
    
    SIMPIterator returnIterator = null;
 
    Iterator<TickRange> messagesOnStream =
      _sourceStream.getAllMessageItemsOnStream(true).iterator();
    
     //now build a collection of TransmitMessageControllable
    Collection<SIMPTransmitMessageControllable> transmitMessages = 
      new LinkedList<SIMPTransmitMessageControllable>();
    
    boolean allMsgs = 
      (maxMsgs == SIMPConstants.SIMPCONTROL_RETURN_ALL_MESSAGES); 
    
    int index = 0;
    while((allMsgs || (index < maxMsgs)) && messagesOnStream.hasNext())
    {
      try
      {
        TickRange tr = messagesOnStream.next();
        if(tr.value == null)
          transmitMessages.add(new LinkTransmitMessageControl(tr.itemStreamIndex, _sourceStream, _downControl));  
        else
          transmitMessages.add(new LinkTransmitMessageControl(((SIMPMessage)tr.value), _sourceStream));  
         
        index++;
      }
      catch (SIResourceException e)
      {
        // No FFDC code needed  
        SibTr.exception(tc, e);      
      }
    }
    returnIterator = 
      new TransmitMessageControllableIterator(transmitMessages.iterator());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTransmitMessagesIterator", returnIterator);      
    return returnIterator;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable#getStreamState
   */
  public StreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getStreamState");
    StreamState returnState = _sourceStream.getStreamState();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getStreamState", returnState);
    return returnState;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable#getStreamID
   */
  public SIBUuid12 getStreamID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getStreamID");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getStreamID", _sourceStreamSet.getStreamID());      
    return  _sourceStreamSet.getStreamID(); 
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName
   */
  public String getName()
  {
    return null;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getId");
    String id = _sourceStream.getID();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getId", id);
    return id; 
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#assertValidControllable
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "assertValidControllable");
      if(_sourceStream == null || _sourceStreamSet == null )
      {
        SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {"SourceStreamControl.assertValidControllable",
              "1:299:1.48",
                  _sourceStreamSet},
                  null));
    
        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "assertValidControllable", finalE);
        throw finalE;
      }
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#dereferenceControllable
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "dereferenceControllable");
    _sourceStream = null;
    _sourceStreamSet = null;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred
   */
  public void runtimeEventOccurred( RuntimeEvent event )
  {
  }
  
  public long getNumberOfMessagesSent()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getNumberOfMessagesSent");
    
    long returnValue =  _sourceStream.getTotalMessagesSent();     
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getNumberOfMessagesSent", new Long(returnValue));      
    return returnValue;
  }
  
  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid");
    String uuid = _sourceStreamSet.getRemoteMEUuid().toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteEngineUuid", uuid);
    
    return uuid;
  }  
  
  public void moveMessage(String msgId, boolean discard) 
  throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException
{
  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    SibTr.entry(tc, "moveMessage", new Object[]{msgId, new Boolean(discard)});
  
    SIMPTransmitMessageControllable xmitMsg = 
          this.getTransmitMessageByID(msgId);
    if(xmitMsg!=null)
    {
      xmitMsg.moveMessage(discard);
    } 

  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    SibTr.exit(tc, "moveMessage");
}  
  public long getLastMsgSentTime()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLastMsgSentTime");
    
    long time = _sourceStream.getLastMsgSentTime();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLastMsgSentTime", time);
    
    return time; 
  }
  
  public HealthStateListener getHealthState() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getHealthState");
      SibTr.exit(tc, "getHealthState", _healthState);
    }  
    return (HealthStateListener)_healthState;
  }
  
}
