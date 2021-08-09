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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.gd.SourceStreamManager;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable;
import com.ibm.ws.sib.processor.runtime.SIMPPtoPOutboundTransmitControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueueControllable;
import com.ibm.ws.sib.processor.runtime.SIMPXmitPoint;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a remote queue to perform dynamic
 * control operations.
 */
public class XmitPointControl
  extends AbstractControlAdapter
  implements SIMPXmitPoint
{
  private XmitPoint xmitQueue;
  private SIBUuid8 remoteME;
  private PtoPXmitMsgsItemStream itemStream;
  private SIMPMessageHandlerControllable messageHandlerControllable;
  private String messageHandlerName;
  private String id;

  private static final TraceComponent tc =
    SibTr.register(
      XmitPointControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  
  
  public XmitPointControl(XmitPoint xmitQueue,
                          PtoPXmitMsgsItemStream itemStream,
                          DestinationHandler destinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "XmitPointControl",
        new Object[] { xmitQueue, itemStream, destinationHandler });

    this.itemStream = itemStream;
    messageHandlerControllable =
      (SIMPMessageHandlerControllable)destinationHandler.getControlAdapter();
    messageHandlerName = messageHandlerControllable.getName();
    
    remoteME = itemStream.getLocalizingMEUuid();
    this.xmitQueue = xmitQueue;
    xmitQueue.setXmitQueuePointControl(this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "XmitPointControl", this);     
  }

  public SIMPMessageHandlerControllable getMessageHandler()
  {
    return messageHandlerControllable;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#getPtoPOutboundTransmit(java.lang.String, java.lang.String)
   */
  public SIMPPtoPOutboundTransmitControllable getPtoPOutboundTransmit()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPtoPOutboundTransmit");

    PtoPOutputHandler outputHandler = (PtoPOutputHandler) itemStream.getOutputHandler();
    SourceStreamManager sourceStreamManager = outputHandler.getSourceStreamManager();
    SIMPPtoPOutboundTransmitControllable control =
      (SIMPPtoPOutboundTransmitControllable)
        sourceStreamManager.getStreamSetRuntimeControl();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPtoPOutboundTransmit");

    return control;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    try
    {
      if(id == null) 
        id = ""+itemStream.getID();
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.XmitPointControl.getId",
          "1:136:1.17", 
          this);
      
      SibTr.exception(tc, e);
    }
    return id;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return messageHandlerName;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(itemStream == null || !itemStream.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"XmitPointControl.assertValidControllable",
                          "1:167:1.17",
                          id},
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

    itemStream = null;
    if(xmitQueue != null)
    {
      xmitQueue.dereferenceXmitQueuePointControl();
      xmitQueue = null;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

    /**
   * Registers this control adapter with the mbean interface.
   * <p>
   * Will not re-register if already registered.
   */
  public synchronized void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry( tc, "registerControlAdapterAsMBean" ); 
      
    if( !messageHandlerControllable.isTemporary()) 
    {
      xmitQueue.registerControlAdapterAsMBean();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit( tc, "registerControlAdapterAsMBean" ); 
  }

  public void deregisterControlAdapterMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deregisterControlAdapterMBean");

    dereferenceControllable();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterControlAdapterMBean");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getUuid()
   */
  public String getUuid()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getConfigId()
   */
  public String getConfigId()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getRemoteEngineUuid()
   */
  public String getRemoteEngineUuid()
  {
    return remoteME.toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#getQueue()
   */
  public SIMPQueueControllable getQueue()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#getUUID()
   */
  public SIBUuid12 getUUID()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#setSendAllowed(boolean)
   */
  public void setSendAllowed(boolean arg)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#getDestinationHighMsgs()
   */
  public long getDestinationHighMsgs()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDestinationHighMsgs");
    
    long destHighMsgs = itemStream.getDestHighMsgs();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDestinationHighMsgs", new Long(destHighMsgs));
    
    return destHighMsgs ;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
    if( !messageHandlerControllable.isTemporary()) 
    {
      xmitQueue.runtimeEventOccurred(event);
    }    
  }

}
