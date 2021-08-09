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

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.AIStream;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteConsumerReceiverControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a remote queue to perform dynamic
 * control operations.
 */
public class RemoteQueuePoint
  extends RemoteMediationPoint
  implements SIMPRemoteQueuePointControllable, XmitPoint
{
	private SIBUuid8 remoteME;
	private DestinationHandler destinationHandler;
	
  private static TraceComponent tc =
    SibTr.register(
      RemoteQueuePoint.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  public RemoteQueuePoint(SIBUuid8 remoteME,
                          DestinationHandler destinationHandler,
                          MessageProcessor messageProcessor)
  {
    super(remoteME, destinationHandler, messageProcessor, ControllableType.REMOTE_QUEUE_POINT);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "RemoteQueuePoint",
        new Object[] { remoteME, destinationHandler, messageProcessor });

    //setType(ControllableType.QUEUE_POINT);

		this.remoteME = remoteME;
		this.destinationHandler = destinationHandler;
		
    /**
     * The id for a RQP need to identify where it is targetted. It therefore include the remoteME
     * plus the lookup key for the consumers stream. 
     */
    
    id = destinationHandler.getUuid().toString()+
         RuntimeControlConstants.REMOTE_QUEUE_ID_INSERT+
         remoteME.toString();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "RemoteQueuePoint", this);     
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable#getRemoteConsumerReceiver()
   */
  public SIMPRemoteConsumerReceiverControllable getRemoteConsumerReceiver()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteConsumerReceiver");
      
    // SIB0113a
    // This method gets the non gathering RemoteConsumerReceiver - in other words the
    // controllable for the remote consumerdispatcher with a null gatheringUuid
    
    SIMPRemoteConsumerReceiverControllable remoteConsumerReceiverControl = null;
    // createAIH is false as if there isn't one already created then there is little benefit to 
    // creating a new one.
    RemoteConsumerDispatcher rcd = 
      destinationHandler.getRemoteConsumerDispatcher(remoteME, null, false);
    if(rcd!=null)
    {
      AnycastInputHandler aih = rcd.getAnycastInputHandler();
      if(aih!=null)
      {
        AIStream aiStream = aih.getAIStream();
        if(aiStream!=null)
        {
          remoteConsumerReceiverControl = 
            (SIMPRemoteConsumerReceiverControllable) aiStream.getControlAdapter();
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteConsumerReceiver", remoteConsumerReceiverControl);
    return remoteConsumerReceiverControl;
  }

  public SIMPIterator getRemoteConsumerReceiverIterator() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteConsumerReceiverIterator");
      
    // SIB0113a
    // This method gets a list of all remote consumer receivers. There is a RCR
    // for every gathering stream (each stream has a different set of scopedMEs)
    
    Iterator<AnycastInputControl> aihIterator = destinationHandler.getAIControlAdapterIterator();
    ArrayList<ControlAdapter> rcrList = new ArrayList<ControlAdapter>();
    
    while(aihIterator.hasNext())
    {
      AIStream stream = aihIterator.next().getStream();
      if (stream!=null && stream.getAnycastInputHandler().getLocalisationUuid().equals(remoteME))
        rcrList.add(stream.getControlAdapter());
    }
    
    SIMPIterator rcrIterator = 
      new BasicSIMPIterator(rcrList.iterator());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteConsumerReceiverIterator", rcrIterator);
    return rcrIterator;
  }

  public long getNumberOfCompletedRequests() 
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfCompletedRequests");

    Iterator<AnycastInputControl> aihIterator = destinationHandler.getAIControlAdapterIterator();
    long requests = 0;
    
    while(aihIterator.hasNext())
      requests+= aihIterator.next().getNumberOfCompletedRequests();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfCompletedRequests", Long.valueOf(requests));
    return requests;
    
  }
  

}


