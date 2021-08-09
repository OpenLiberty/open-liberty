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
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.AIStream;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.DeliveryStreamType;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.IndoubtAction;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable;
import com.ibm.ws.sib.processor.runtime.anycast.AIStreamIterator;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This object represents the message requests (and the received responses)
 * that we have submitted to a remote ME
 * There is just one stream for all remoteGET message flows between 2 MEs
 * Hence the concepts of reliability and priority have no meaning here 
 */
public class RemoteConsumerReceiver extends AbstractControlAdapter implements SIMPRemoteConsumerReceiverControllable
{
  // The AIStream for which this object represents
  private AIStream aiStream;
  // The destination that this remote get is trying to retrieve from
  private DestinationHandler destinationHandler;
  // The uuid of the destination ME
  private SIBUuid8 dme;
  private SIBUuid12 gatheringTargetDestUuid;
  // The AnycastInputHandler
  private AnycastInputHandler anycastIH;  
    
  //NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    
  private static TraceComponent tc =
    SibTr.register(
  RemoteConsumerReceiver.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * This object is created with the aiStream so that is can retreive the ticks from
   * it. It is created for each remote get that is executed against an ME
   * 
   * @param aiStream
   * @param msgStore
   * @param destinationHandler
   * @param anycastIH
   * @param dme
   */
  public RemoteConsumerReceiver(AIStream aiStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "RemoteConsumerReceiver", new Object[]{aiStream});
      
    this.aiStream = aiStream;
    this.anycastIH = aiStream.getAnycastInputHandler();
    this.destinationHandler = anycastIH.getBaseDestinationHandler();
    this.dme = anycastIH.getLocalisationUuid();
    this.gatheringTargetDestUuid = anycastIH.getGatheringTargetDestUuid();
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "RemoteConsumerReceiver");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable#getRemoteMessageRequestIterator()
   */
  public SIMPIterator getRemoteMessageRequestIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteMessageRequestIterator");
      
    // Create the iterator over the AIStream
    AIStreamIterator aiStreamIterator = new AIStreamIterator(aiStream);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "getRemoteMessageRequestIterator", aiStreamIterator);
              
    return aiStreamIterator;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable#requestFlushAtSource(byte)
   */
  public void requestFlushAtSource(IndoubtAction indoubtAction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "requestFlushAtSource", indoubtAction);
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "requestFlushAtSource"); 
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetReceiverControllable#getLastTickReceived()
   */
  public long getLastTickReceived() throws SIMPRuntimeOperationFailedException
  {
    //Returns the tick of the last message received and acknowleged
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLastTickReceived");

    try
    {
      assertValidControllable();
    }
    catch (SIMPControllableNotFoundException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.RemoteConsumerReceiver.getLastTickReceived",
        "1:161:1.47",
        this);
      
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"RemoteConsumerReceiver.getLastTickReceived",
                "1:169:1.47",
                          e,
                          aiStream.getStreamId()},
            null), e);
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getLastTickReceived", finalE);
      throw finalE;
    }
    //the last tick received and acknowledged is the compelted prefix
    long tick = aiStream.getStateStream().getCompletedPrefix();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLastTickReceived", Long.valueOf(tick));
      
    return tick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetReceiverControllable#forceFlushAtTarget()
   */
  public void forceFlushAtTarget() throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "forceFlushAtTarget");
    
    try
    {
      assertValidControllable();
    }
    catch (SIMPControllableNotFoundException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.RemoteConsumerReceiver.forceFlushAtTarget",
        "1:204:1.47",
        this);
      
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"RemoteConsumerReceiver.forceFlushAtTarget",
                "1:212:1.47",
                          e,
                          aiStream.getStreamId()},
            null), e);
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "forceFlushAtTarget", finalE);
      throw finalE;
    }
    
    AnycastInputHandler aih = destinationHandler.getAnycastInputHandler(dme, gatheringTargetDestUuid, true);
    
    if (aih != null)
    {
      aih.forceFlushAtTarget();
      try
      {
        String key = 
          SIMPUtils.getRemoteGetKey(dme, gatheringTargetDestUuid);
        ((BaseDestinationHandler)destinationHandler).removeAnycastInputHandlerAndRCD(key);
      }
      catch(SIResourceException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.RemoteConsumerReceiver.forceFlushAtTarget",
          "1:238:1.47",
          this);
      
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0003",
              new Object[] {"RemoteConsumerReceiver.forceFlushAtTarget",
                  "1:246:1.47",
                            e,
                            aiStream.getStreamId()},
              null), e);
            
        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "forceFlushAtTarget", finalE);
        throw finalE;
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "forceFlushAtTarget");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryStreamSetControllable#getType()
   */
  public DeliveryStreamType getType()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getType");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getType", DeliveryStreamType.ANYCAST_TARGET);
    return DeliveryStreamType.ANYCAST_TARGET;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getId");
      
    String id = aiStream.getStreamId().toString();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getId");
    return id;
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
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable#cancelAllRequests()
   */  
  public void cancelAllRequests()
  {
    try
    {
      this.forceFlushAtTarget();
    }
    catch(SIMPRuntimeOperationFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.RemoteConsumerReceiver.cancelAllRequests",
        "1:316:1.47",
        this); 
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.RemoteConsumerReceiver.cancelAllRequests", 
                       "1:321:1.47", 
                       SIMPUtils.getStackTrace(e) });
      SibTr.exception(tc, e);
    }    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(aiStream == null || aiStream.getAIProtocolItemStream() == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"RemoteConsumerReceiver.assertValidControllable",
                "1:342:1.47",
                          aiStream},
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
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable#getNumberOfActiveMessages()
   */
  public int getNumberOfActiveRequests()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getNumberOfActiveRequests");
    
    //This will return every request in a state other than COMPLETE
    List all = aiStream.getTicksOnStream();
    Iterator it = all.iterator();
    
    // Filter out rejected ticks
    int activeCount = 0;
    while (it.hasNext())
    {
      TickRange tr = aiStream.getTickRange(((Long)it.next()).longValue());
      if( !(tr.type == TickRange.Rejected) )
        activeCount++;
    }
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getNumberOfActiveRequests", Integer.valueOf(activeCount));
    return activeCount;  
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable#getNumberOfCompletedRequests()
   */
  public long getNumberOfCompletedRequests()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getNumberOfCompletedRequests" );

    long returnValue = anycastIH.getCompletedRequestCount();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getNumberOfCompletedRequests", Long.valueOf(returnValue));
    return returnValue;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable#getNumberOfRequestsIssuecd()
   */
  public long getNumberOfRequestsIssued()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getNumberOfRequestsIssued");
    long returnValue = anycastIH.getTotalSentRequests();   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getNumberOfRequestsIssued", Long.valueOf(returnValue));     
    return returnValue;
  }
  
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryReceiverControllable#getStreamID()
   */
  public SIBUuid12 getStreamID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getStreamID");  
    SIBUuid12 returnValue = aiStream.getStreamId();   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getStreamID", returnValue);
    return returnValue;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPDeliveryReceiverControllable#getStreamState()
   */
  public StreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getStreamState");  
    StreamState returnValue = aiStream.getStreamState();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getStreamState", returnValue);
    return returnValue;  
  }
  
  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getRemoteEngineUuid");     
    String returnString = this.dme.toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRemoteEngineUuid", returnString);
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
    {
      SibTr.entry(tc, "getDestinationUuid"); 
      SibTr.exit(tc, "getDestinationUuid", gatheringTargetDestUuid);
    }
    
    return String.valueOf(gatheringTargetDestUuid);  
  }

  public boolean isGathering() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isGathering");    
    
    boolean isGathering = gatheringTargetDestUuid!=null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isGathering", Boolean.valueOf(isGathering));
    
    return isGathering;  
  }
  
}
