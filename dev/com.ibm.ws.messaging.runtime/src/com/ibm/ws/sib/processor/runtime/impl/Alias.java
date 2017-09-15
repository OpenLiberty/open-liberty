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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.impl.AliasDestinationHandler;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.indexes.DestinationIndex;
import com.ibm.ws.sib.processor.runtime.SIMPAliasControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable;
import com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a Alias to perform dynamic
 * control operations.
 * 
 * The operations in this interface are specific to a alias.
 */
public final class Alias extends AbstractControlAdapter implements SIMPAliasControllable
{
  
  private MessageProcessorControl mpControl;
  private DestinationIndex index;
  private DestinationManager destinationManager;
  private AliasDestinationHandler aliasDest;
  
  private static TraceComponent tc =
    SibTr.register(
      LocalQueuePoint.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
 
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  
  
  public Alias(MessageProcessor messageProcessor,
               AliasDestinationHandler destination)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "Alias",
        new Object[] { messageProcessor, destination });
        
    mpControl = (MessageProcessorControl) messageProcessor.getControlAdapter();
    destinationManager = messageProcessor.getDestinationManager();
    index = destinationManager.getDestinationIndex();
    aliasDest = destination;  
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "Alias", this);   
  }
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getTargetMessageHandler()
   */
  public SIMPMessageHandlerControllable getTargetMessageHandler()
  {
    return (SIMPMessageHandlerControllable)aliasDest.getResolvedDestinationHandler().getControlAdapter();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getBrowserIterator()
   */
  public SIMPIterator getBrowserIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getConsumerIterator()
   */
  public SIMPIterator getConsumerIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getProducerIterator()
   */
  public SIMPIterator getProducerIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getBus()
   */
  public String getBus()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getBus");
    
    String bus = aliasDest.getBus();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getBus", bus);
    
    return bus;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getDefaultPriority()
   */
  public int getDefaultPriority()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDefaultPriority");
    
    int priority = aliasDest.getDefaultPriority();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDefaultPriority", new Integer(priority));
    
    return priority;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getMaxReliability()
   */
  public Reliability getMaxReliability()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getMaxReliability");
    
    Reliability maxRel = aliasDest.getMaxReliability();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getMaxReliability", maxRel);
    
    return maxRel;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#getDefaultReliability()
   */
  public Reliability getDefaultReliability()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDefaultReliability");
    
    Reliability rel = aliasDest.getDefaultReliability();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDefaultReliability", rel);
    
    return rel;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#isReceiveAllowed()
   */
  public boolean isReceiveAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isReceiveAllowed");
    
    boolean isReceiveAllowed = aliasDest.isReceiveAllowed();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isReceiveAllowed", new Boolean(isReceiveAllowed));
    
    return isReceiveAllowed;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isSendAllowed");
    
    boolean isSendAllowed = aliasDest.isSendAllowed();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isSendAllowed", new Boolean(isSendAllowed));
    
    return isSendAllowed;
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
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isLocal");
    
    boolean isLocal = false;
    if (aliasDest.getResolvedDestinationHandler().getLocalLocalizationPoint() != null)
      isLocal = true;
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isLocal", new Boolean(isLocal));
    
    return isLocal;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isAlias()
   */
  public boolean isAlias()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "isAlias");
      SibTr.exit(tc, "isAlias", Boolean.TRUE);
    }    
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isForeign()
   */
  public boolean isForeign()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isForeign");
    
    boolean isForeign = aliasDest.isForeign();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isForeign", new Boolean(isForeign));
    
    return isForeign;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getDescription()
   */
  public String getDescription()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDescription");
      
    String desc = aliasDest.getDescription();
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDescription", desc);
      
    return desc;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getDestinationContext()
   */
  public Map getDestinationContext()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDestinationContext");
      
    Map context = aliasDest.getDefinition().getDestinationContext();
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDestinationContext", context);
      
    return context;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(aliasDest == null || !aliasDest.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"Alias.assertValidControllable",
                          "1:317:1.20",
                          aliasDest},
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
    aliasDest = null;
    index = null;
    mpControl = null;
    
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

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isSystem()
   */
  public boolean isSystem()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isSystem");
    
    boolean isSystem = aliasDest.isSystem();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isSystem", new Boolean(isSystem));
    
    return isSystem;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isTemporary()
   */
  public boolean isTemporary()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isTemporary");
    
    boolean isTemporary = aliasDest.isTemporary();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isTemporary", new Boolean(isTemporary));
    
    return isTemporary;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getState()
   */
  public String getState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getState");
    String state = 
      index.getState(aliasDest).toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getState");
    return state;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getName");
    
    String name = aliasDest.getName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getName", name);
    
    return name;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getUUID()
   */
  public SIBUuid12 getUUID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getUUID");
    
    SIBUuid12 uuid = aliasDest.getUuid();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getUUID", uuid);
    
    return uuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getId");    
    String id = getUUID().toString();    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getId", id);   
    return id;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPAliasControllable#isProducerQOSOverrideEnabled()
   */
  public boolean isProducerQOSOverrideEnabled()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isProducerQOSOverrideEnabled");    
    boolean allowed = aliasDest.isOverrideOfQOSByProducerAllowed();    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "isProducerQOSOverrideEnabled", new Boolean(allowed));   
    return allowed;
  }

}
