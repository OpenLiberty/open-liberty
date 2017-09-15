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
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.DestinationForeignDefinition;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.impl.BusHandler;
import com.ibm.ws.sib.processor.impl.ForeignDestinationHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.runtime.SIMPForeignBusControllable;
import com.ibm.ws.sib.processor.runtime.SIMPForeignDestinationControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * 
 */
public class ForeignDestination extends AbstractControlAdapter implements SIMPForeignDestinationControllable
{
  private static TraceComponent tc =
    SibTr.register(
      ForeignDestination.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  private ForeignDestinationHandler foreignDest;
  private MessageProcessor messageProcessor;

  public ForeignDestination(MessageProcessor messageProcessor, 
                             ForeignDestinationHandler foreignDest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "ForeignDestination", new Object[] { messageProcessor, foreignDest});
            
    this.foreignDest = foreignDest;
    this.messageProcessor = messageProcessor; 
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ForeignDestination", this); 
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPForeignDestinationControllable#getForeignDestinationDefinition()
   */
  public DestinationForeignDefinition getForeignDestinationDefinition()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getForeignDestinationDefinition");
    DestinationForeignDefinition definition = 
      (DestinationForeignDefinition) foreignDest.getDefinition();   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getForeignDestinationDefinition");
    return definition;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPForeignDestinationControllable#getTargetForeignBus()
   */
  public SIMPForeignBusControllable getTargetForeignBus()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetForeignBus");
    
    //Lookup foreign bus
    BusHandler foreignBus = null;
    try
    {
      foreignBus =
        messageProcessor.getDestinationManager().findBus(foreignDest.getBus());
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.getTargetForeignBus",
        "1:114:1.19",
        this);
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.getTargetForeignBus", 
                       "1:119:1.19", 
                       SIMPUtils.getStackTrace(e) }); 
      SibTr.exception(tc, e);      
    }   
      
    SIMPForeignBusControllable busControllable = null;
    
    if (foreignBus != null)
      busControllable = (SIMPForeignBusControllable) foreignBus.getControlAdapter();
           
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetForeignBus");
      
    return busControllable;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPForeignDestinationControllable#getProducerIterator()
   */
  public SIMPIterator getProducerIterator()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getMessageProcessor()
   */
  public SIMPMessageProcessorControllable getMessageProcessor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMessageProcessor");
      SibTr.exit(tc, "getMessageProcessor"); 
    }
    return (SIMPMessageProcessorControllable)messageProcessor.getControlAdapter();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#isLocal()
   */
  public boolean isLocal()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "isLocal");
    
    boolean isLocal = false;
    if (foreignDest.getResolvedDestinationHandler().getLocalLocalizationPoint() != null)
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
    {
      SibTr.entry(tc, "isForeign");
      SibTr.exit(tc, "isForeign", Boolean.TRUE);
    }    
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable#getUUID()
   */
  public SIBUuid12 getUUID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getUUID");
    
    SIBUuid12 uuid = foreignDest.getUuid();
    
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
      
    String desc = foreignDest.getDescription();
      
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
      
    Map context = foreignDest.getDefinition().getDestinationContext();
      
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

    if(foreignDest == null || !foreignDest.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"ForeignDest.assertValidControllable",
                          "1:263:1.19",
                          foreignDest},
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

    foreignDest = null;
    messageProcessor = null;
    
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
    
    boolean isSystem = foreignDest.isSystem();
    
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
    
    boolean isTemporary = foreignDest.isTemporary();
    
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
      messageProcessor.getDestinationManager().getDestinationIndex().getState(foreignDest).toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getState");
    return state;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getName");
    
    String name = foreignDest.getName();
    
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
      SibTr.entry(tc, "getId");    
    String id = getUUID().toString();    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getId", id);   
    return id;
  }
}
