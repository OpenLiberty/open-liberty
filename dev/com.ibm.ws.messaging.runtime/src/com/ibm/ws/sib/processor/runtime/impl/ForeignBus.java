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
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.impl.BusHandler;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.runtime.SIMPForeignBusControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable;
import com.ibm.ws.sib.processor.runtime.SIMPVirtualLinkControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * 
 */
public class ForeignBus extends AbstractControlAdapter implements SIMPForeignBusControllable
{
  private MessageProcessorControl _mpControl;
  private MessageProcessor _messageProcessor;
  private DestinationManager _destinationManager;
  private BusHandler _foreignBus;
  
  private static final TraceComponent tc =
  SibTr.register(
    LocalQueuePoint.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);
  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  
  
  public ForeignBus(MessageProcessor messageProcessor,
               BusHandler foreignBus)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "ForeignBus",
        new Object[] { messageProcessor, foreignBus });
          
    _mpControl = (MessageProcessorControl) messageProcessor.getControlAdapter();
    _destinationManager = messageProcessor.getDestinationManager();
    _messageProcessor = messageProcessor;
    _foreignBus = foreignBus;  
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ForeignBus", this);   
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPForeignBusControllable#getProducerIterator()
   */
  public SIMPIterator getProducerIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPForeignBusControllable#getInterBusLinkIterator()
   */
  public SIMPVirtualLinkControllable getVirtualLinkControlAdapter()
  {
    return (SIMPVirtualLinkControllable)_foreignBus.getResolvedDestinationHandler().getControlAdapter();

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getMessageProcessor()
   */
  public SIMPMessageProcessorControllable getMessageProcessor()
  {
    return _mpControl;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isLocal()
   */
  public boolean isLocal()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isLocal");
    
    boolean isLocal = 
      (_foreignBus.getResolvedDestinationHandler().getLocalLocalizationPoint() != null);
    
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
    {
      SibTr.entry(tc, "isForeign");
      SibTr.exit(tc, "isForeign", Boolean.FALSE);
    }      
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getUUID()
   */
  public SIBUuid12 getUUID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getUUID");
    SIBUuid12 uuid = _foreignBus.getUuid();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getUUID", uuid);      
    return uuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getDescription()
   */
  public String getDescription()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDescription");
    String desc = _foreignBus.getDescription();
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
    Map context = _foreignBus.getDefinition().getDestinationContext();
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

    if(_foreignBus == null || !_foreignBus.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"ForeignBus.assertValidControllable",
                          "1:205:1.22",
                          _foreignBus},
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

    _destinationManager = null;
    _foreignBus = null;
    _mpControl = null;
    
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
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getBrowserIterator()
   */
  public SIMPIterator getBrowserIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getConsumerIterator()
   */
  public SIMPIterator getConsumerIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isSystem()
   */
  public boolean isSystem()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isSystem");    
    boolean isSystem = _foreignBus.isSystem();
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
    boolean isTemporary = _foreignBus.isTemporary();
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
      _destinationManager.getForeignBusIndex().getState(_foreignBus).toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getState", state);
    return state;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getName");    
    String name = _foreignBus.getName();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getName", name);      
    return name;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getName");    
    String id = getUUID().toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getName", id);      
    return id;
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPForeignBusControllable#getForeignBusDefinition()
   */
  public ForeignBusDefinition getForeignBusDefinition()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getForeignBusDefinition");    
    ForeignBusDefinition busDef = _messageProcessor.getForeignBus(getName());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getForeignBusDefinition", busDef);      
    return busDef;
  }
  
  /**
   * Get the default priority
   */
  public int getDefaultPriority()
  {
    return _foreignBus.getDefaultPriority();
  }
  
  /**
   * Get the default reliability
   */
  
  public Reliability getDefaultReliability()
  {
    return _foreignBus.getDefaultReliability();
  }
  
  /**
   * Determines if messages can be sent to the foreign bus
   * @return
   */
  public boolean isSendAllowed()
  {
    return _foreignBus.isSendAllowed();
  }

}
