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
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable;
import com.ibm.ws.sib.processor.runtime.SIMPPtoPOutboundTransmitControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a remote queue to perform dynamic
 * control operations.
 */
public class RemoteMediationPoint
    extends AbstractRegisteredControlAdapter
    implements  XmitPoint
{
  private XmitPointControl xmitPointControl;
  private SIBUuid8 remoteME;
  private SIMPMessageHandlerControllable messageHandlerControllable;
  private String messageHandlerName;
  protected String id;

  private static TraceComponent tc =
    SibTr.register(
      RemoteMediationPoint.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  public RemoteMediationPoint(SIBUuid8 remoteME,
                            DestinationHandler destinationHandler,
                            MessageProcessor messageProcessor,
                            ControllableType controllableType)
  {
    super(messageProcessor, controllableType);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "RemoteMediationPoint",
        new Object[] { remoteME, destinationHandler, messageProcessor, controllableType });

    messageHandlerControllable =
      (SIMPMessageHandlerControllable)destinationHandler.getControlAdapter();
    messageHandlerName = messageHandlerControllable.getName();
  
    this.remoteME = remoteME;
    id = destinationHandler.getUuid().toString()+
         RuntimeControlConstants.REMOTE_MEDIATION_ID_INSERT+
         remoteME.toString();
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "RemoteMediationPoint", this);     
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

    SIMPPtoPOutboundTransmitControllable control = null;
    if(xmitPointControl != null)
    {
      control = xmitPointControl.getPtoPOutboundTransmit();
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPtoPOutboundTransmit", control);

    return control;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
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

    if(xmitPointControl != null)
    {
      xmitPointControl.assertValidControllable();
    }
//      if(itemStream == null || !itemStream.isInStore())
//      {
//        SIMPControllableNotFoundException e = new SIMPControllableNotFoundException("Controllable no longer exists");
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//          SibTr.exception(tc, e);
//        throw e; 
//      }
  
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

    dereferenceXmitQueuePointControl();
    super.dereferenceControllable();
  
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
    
    if( isRegistered() || getMessageHandler().isTemporary()) 
    {
      // We're a temporary queue or Registered already. Don't register a 2nd time.
    }
    else
    {
      super.registerControlAdapterAsMBean();
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit( tc, "registerControlAdapterAsMBean" ); 
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
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#getUUID()
   */
  public SIBUuid12 getUUID()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#isReceiveAllowed()
   */
  public boolean isReceiveAllowed()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#setReceiveAllowed(boolean)
   */
  public void setReceiveAllowed(boolean arg)
  {
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
  
    long destHighMsgs = 0;
    if(xmitPointControl != null)
    {
      destHighMsgs = xmitPointControl.getDestinationHighMsgs();
    } 
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDestinationHighMsgs", new Long(destHighMsgs));
  
    return destHighMsgs ;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.XmitQueuePoint#setXmitQueuePointControl(com.ibm.ws.sib.processor.runtime.XmitQueuePointControl)
   */
  public void setXmitQueuePointControl(XmitPointControl xmitPointControl)
  {
    this.xmitPointControl = xmitPointControl;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.XmitQueuePoint#dereferenceXmitQueuePointControl()
   */
  public void dereferenceXmitQueuePointControl()
  {
    xmitPointControl = null;
  }
  

}
