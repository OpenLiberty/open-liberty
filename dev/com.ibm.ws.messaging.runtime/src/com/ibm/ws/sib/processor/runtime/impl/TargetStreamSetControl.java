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

import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.gd.TargetStream;
import com.ibm.ws.sib.processor.gd.TargetStreamManager;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.runtime.DeliveryStreamType;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamReceiverControllable;
import com.ibm.ws.sib.processor.runtime.SIMPInboundReceiverControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A Controllable for a set of target streama
 */
public class TargetStreamSetControl
  extends AbstractControlAdapter
    implements SIMPInboundReceiverControllable
{
  protected TargetStreamManager tsm;
  private StreamSet streamSet;
  private SIBUuid12 streamID;
  private HealthStateTree _healthState;

  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  private static final TraceComponent tc =
    SibTr.register(
      TargetStreamSetControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  private class TargetStreamControllableIterator extends BasicSIMPIterator
  {    
   
    TargetStreamManager tsm = null;
    
    /**
     * 
     * @param _targetStreamSet An iterator for the target streams contained
     * in a single stream set
     */
    public TargetStreamControllableIterator(Iterator targetStreamSet, TargetStreamManager tsm)
    {
      super(targetStreamSet);
      this.tsm = tsm;
    }
    
    public Object next()
    {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "next");
      
      TargetStream targetStream = (TargetStream)super.next();
      TargetStreamControl targetStreamControl = 
        targetStream.getControlAdapter();
      targetStreamControl.setTSM(tsm);
        
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "next", targetStreamControl);      
      return targetStreamControl;
    }
    

  }
  
  /**
   * Warning - setTargetStreamManager must be called with the owning
   * TargetStreamManager before this control adapter can be used
   * 
   * @param streamSet
   */
  public TargetStreamSetControl(StreamSet streamSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "TargetStreamSetControl", new Object[] { streamSet });

    this.streamSet = streamSet;
    this.streamID = streamSet.getStreamID();
    this._healthState = new HealthStateTree();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "TargetStreamSetControl", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPPtoPInboundReceiverControllable#requestFlushAtSource(byte)
   */
  public void requestFlushAtSource(boolean indoubtDiscard) throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "requestFlushAtSource", new Boolean(indoubtDiscard));

    assertValidControllable();

    SIBUuid8 source = streamSet.getRemoteMEUuid();
    SIBUuid12 destID = streamSet.getDestUuid();
    SIBUuid8 busID = streamSet.getBusUuid();
    try
    {
      //TODO pass through the indoubtDiscard flag      
      tsm.requestFlushAtSource(source, destID, busID, streamID, indoubtDiscard);
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.TargetStreamSetControl.requestFlushAtSource",
        "1:152:1.16",
        this);
      
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"TargetStreamSetControl.requestFlushAtSource",
                "1:160:1.16",
                          e,
                          streamID},
            null), e);

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "requestFlushAtSource", finalE);
      throw finalE;             
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "requestFlushAtSource");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetReceiverControllable#getLastTickReceived()
   */
  public long getLastTickReceived() throws SIMPControllableNotFoundException, SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLastTickReceived");

    assertValidControllable();

    long tick = 0;
    Iterator itr = null;
    try
    {
      itr = streamSet.iterator();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.TargetStreamSetControl.getLastTickReceived",
          "1:194:1.16",
          this);
      
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"TargetStreamSetControl.getLastTickReceived",
                  "1:202:1.16",
                            e},
              null), e);
            
        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getLastTickReceived", finalE);
        throw finalE;
    }
    while(itr.hasNext())
    {
      TargetStream stream = (TargetStream) itr.next();
      long newTick = stream.getLastKnownTick();
      tick = (newTick > tick) ? newTick : tick;
    }   

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLastTickReceived", new Long(tick));
      
    return tick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetReceiverControllable#forceFlushAtTarget()
   * 
   *Function Name:forceFlushAtTarget
   *
   *Parameters:  
   *
   *Description: If the sourceME has been deleted there is no opportunity to complete the flush  
   *             or fill any gaps. This method will discard the target stream without completing 
   *             and ignoring any gaps. If the source still exists no messages will be lost because
   *             they can be retransmitted by the source. However there is a risk that messages 
   *             will be duplicated because an ack generated by the target may not have been received
   *             by the source, causing the source to send the same message again to a new instance 
   *             of the same stream which the target recreates. 
   *             On completion no stream state exists, just as if flush had completed.
   *
   */
  public void forceFlushAtTarget() throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "forceFlushAtTarget");
    
    assertValidControllable();
    
    try
    {
      tsm.forceFlush(streamID);
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.TargetStreamSetControl.forceFlushAtTarget",
        "1:256:1.16",
        this);
      
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"TargetStreamSetControl.forceFlushAtTarget",
                "1:264:1.16",
                          e,
                          streamID},
            null), e);

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "forceFlushAtTarget", finalE);
      throw finalE;             
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "forceFlushAtTarget");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetControllable#getType()
   */
  public DeliveryStreamType getType()
  {
    return null;
    //should probably get rid of this method because we don't actually know what type
    //of target stream these are!!
    //return DeliveryStreamType.UNICAST_TARGET;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getId");
    String returnString = streamID.toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getId", returnString);
    return returnString;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#assertValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(streamSet == null || tsm == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {"TargetStreamSetControl.assertValidControllable",
                "1:325:1.16",
                          streamID},
            null));

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "assertValidControllable", finalE);
      throw finalE;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable", tsm);
    
    streamSet = null;
    tsm = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable"); 
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }

  public void setTargetStreamManager(TargetStreamManager tsm)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setTargetStreamManager", tsm);
    //yuk - nasty hack! Would rather have set this in the constructor
    this.tsm = tsm;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setTargetStreamManager");    
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetReceiverControllable#getNumberOfMessagesReceived
   */
  public long getNumberOfMessagesReceived()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfMessagesReceived");
    Iterator streams = null;
    try
    {
      streams = streamSet.iterator();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.TargetStreamSetControl.getNumberOfMessagesReceived",
          "1:388:1.16",
          this);
      
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"TargetStreamSetControl.getNumberOfMessagesReceived",
                  "1:396:1.16",
                            e},
              null), e);
            
        SibTr.exception(tc, finalE);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getNumberOfMessagesReceived", finalE);
//        throw finalE;
    }
    long numMsgsReceived = 0;
    while(streams.hasNext())
    {
      TargetStream streamControl = (TargetStream)streams.next();
      numMsgsReceived += streamControl.getNumberOfMessagesReceived();  
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfMessagesReceived", new Long(numMsgsReceived));
    return numMsgsReceived;
  }
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetReceiverControllable#getDepth
   */
  public long getDepth()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDepth");
    Iterator streams = null;
    try
    {
      streams = streamSet.iterator();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.TargetStreamSetControl.getDepth",
          "1:432:1.16",
          this);
      
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"TargetStreamSetControl.getDepth",
                  "1:440:1.16",
                            e},
              null), e);
            
        SibTr.exception(tc, finalE);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getDepth", finalE);
//        throw finalE;
    }
    long depth = 0;
    while(streams.hasNext())
    {
      TargetStream streamControl = (TargetStream)streams.next();
      depth += streamControl.getAllMessagesOnStream().size();  
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDepth", new Long(depth));
    return depth;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetReceiverControllable#getStreams
   */
  public SIMPIterator getStreams()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreams");
    Iterator it = null;
    try
    {
      it = streamSet.iterator();
    }
    catch (SIResourceException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.TargetStreamSetControl.getStreams",
          "1:476:1.16",
          this);
      
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {"TargetStreamSetControl.getStreams",
                  "1:484:1.16",
                            e},
              null), e);
            
        SibTr.exception(tc, finalE);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getStreams", finalE);
//        throw finalE;
    }
    TargetStreamControllableIterator targetStreamIterator = 
      new TargetStreamControllableIterator(it, tsm);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreams", targetStreamIterator);      
    return targetStreamIterator;      
  }
  
  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteEngineUuid");
    String uuid = streamSet.getRemoteMEUuid().toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteEngineUuid", uuid);
    return uuid;
  }

  public HealthStateListener getHealthState() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getHealthState");
    
    // Iterate over the streams - get the worst health state
    Iterator it = getStreams();
    while (it.hasNext())
    {
      SIMPDeliveryStreamReceiverControllable control = 
            (SIMPDeliveryStreamReceiverControllable) it.next();
        ((HealthStateTree)_healthState).addHealthStateNode(control.getHealthState());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getHealthState", _healthState);
    return (HealthStateListener)_healthState;
  } 
}
