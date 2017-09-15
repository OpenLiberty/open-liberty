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

import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.indexes.DestinationIndex;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * The adapter presented by a queue to perform dynamic
 * control operations.
 */
public abstract class MediatedMessageHandlerControl extends AbstractControlAdapter
{
  protected MessageProcessorControl mpControl;
  protected DestinationIndex index;
  protected DestinationManager destinationManager;
  protected BaseDestinationHandler baseDest;
  protected String name;
  protected SIBUuid12 uuid;
  protected MessageProcessor messageProcessor;
  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static TraceComponent tc =
    SibTr.register(
      MediatedMessageHandlerControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  public MediatedMessageHandlerControl(MessageProcessor messageProcessor,
               BaseDestinationHandler destination)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "MediatedMessageHandlerControl",
        new Object[] { messageProcessor, destination });

    mpControl = (MessageProcessorControl)messageProcessor.getControlAdapter();
    destinationManager = messageProcessor.getDestinationManager();
    this.messageProcessor = messageProcessor;
    index = destinationManager.getDestinationIndex();
    baseDest = destination;
    name = baseDest.getName();
    uuid = baseDest.getUuid();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MediatedMessageHandlerControl", this);        
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#isSystem()
   */
  public boolean isSystem()
  {
    return baseDest.isSystem();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return name;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#isTemporary()
   */
  public boolean isTemporary()
  {
    return baseDest.isTemporary();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getState()
   */
  public String getState()
  {
    return index.getState(baseDest).toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getBrowserIterator()
   */
  public SIMPIterator getBrowserIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getConsumerIterator()
   */
  public SIMPIterator getConsumerIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getProducerIterator()
   */
  public SIMPIterator getProducerIterator()
  {
    return null;
  }
 
 

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getAlterationTime()
   */
  public long getAlterationTime()
  {
    return baseDest.getAlterationTime();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getDestinationType()
   */
  public DestinationType getDestinationType()
  {
    return baseDest.getDestinationType();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getDestinationContext()
   */
  public Map getDestinationContext()
  {
    return baseDest.getDefinition().getDestinationContext();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    return baseDest.isSendAllowed();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#isReceiveAllowed()
   */
  public boolean isReceiveAllowed()
  {
    return baseDest.isReceiveAllowed();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#isReceiveExclusive()
   */
  public boolean isReceiveExclusive()
  {
    return baseDest.isReceiveExclusive();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getDefaultPriority()
   */
  public int getDefaultPriority()
  {
    return baseDest.getDefaultPriority();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getExceptionDestination()
   */
  public String getExceptionDestination()
  {
    return baseDest.getExceptionDestination();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getMaxFailedDeliveries()
   */
  public int getMaxFailedDeliveries()
  {
    return baseDest.getMaxFailedDeliveries();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#isOverrideOfQOSByProducerAllowed()
   */
  public boolean isOverrideOfQOSByProducerAllowed()
  {
    return baseDest.isOverrideOfQOSByProducerAllowed();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getDefaultReliability()
   */
  public Reliability getDefaultReliability()
  {
    return baseDest.getDefaultReliability();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getMaxReliability()
   */
  public Reliability getMaxReliability()
  {
    return baseDest.getMaxReliability();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getMessageProcessor()
   */
  public SIMPMessageProcessorControllable getMessageProcessor()
  {
    return mpControl;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isLocal()
   */
  public boolean isLocal()
  {
    return baseDest.hasLocal();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isAlias()
   */
  public boolean isAlias()
  {
    return baseDest.isAlias();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isForeign()
   */
  public boolean isForeign()
  {
    return baseDest.isForeign();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getUUID()
   */
  public SIBUuid12 getUUID()
  {
    return uuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getDescription()
   */
  public String getDescription()
  {
    return baseDest.getDescription();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    return uuid.toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(baseDest == null || !baseDest.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"MediateMessageHandlerControl.assertValidControllable",
                "1:372:1.15",
                          uuid},
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

    destinationManager = null;
    baseDest = null;
    index = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
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
  
  
  /**
   * A utility method for methods which can only execute if the underlying
   * message handler is not corrupt or waiting to be reset on restart.
   * 
   * @throws SIMPException
   */  
  protected void assertMessageHandlerNotCorrupt() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertMessageHandlerNotCorrupt");
      
    if (baseDest.isCorruptOrIndoubt())
    {
      String nlsMsg = nls.getFormattedMessage(
        "MESSAGE_HANDLER_CORRUPT_ERROR_CWSIP0201", null, null);
        
      SIMPException e = new SIMPException(nlsMsg);
                 
      SibTr.exception(tc, e);
      
      throw e;      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertMessageHandlerNotCorrupt");
  }
}
