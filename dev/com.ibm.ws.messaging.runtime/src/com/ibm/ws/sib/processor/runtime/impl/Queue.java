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
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueueControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPXmitPoint;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a queue to perform dynamic
 * control operations.
 */
public class Queue extends MediatedMessageHandlerControl implements SIMPQueueControllable
{
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      Queue.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  public Queue(MessageProcessor messageProcessor,
               BaseDestinationHandler destination)
  {
    super(messageProcessor,destination);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "Queue", new Object[]{messageProcessor, destination});
      SibTr.exit(tc, "Queue", this);
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getLocalQueuePointControl()
   */
  public SIMPLocalQueuePointControllable getLocalQueuePointControl()
    throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLocalQueuePointControl");
      
    assertMessageHandlerNotCorrupt();
    
    SIMPLocalQueuePointControllable control = null;
    LocalizationPoint is = baseDest.getLocalLocalizationPoint();
    if(is != null)
    {
      control = (SIMPLocalQueuePointControllable) is.getControlAdapter();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLocalQueuePointControl", control);
    return control;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getRemoteQueuePointIterator()
   */
  public SIMPIterator getRemoteQueuePointIterator() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteQueuePointIterator");
      
    assertMessageHandlerNotCorrupt();
    
    Index index = baseDest.getRemoteQueuePoints();
    SIMPIterator itr = index.iterator();
    SIMPIterator returnItr = new BasicSIMPIterator(itr);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteQueuePointIterator", returnItr);
    return returnItr;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getRemoteQueuePointControlByID(java.lang.String)
   */
  public SIMPRemoteQueuePointControllable getRemoteQueuePointControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteQueuePointControlByID", id);
      
    assertMessageHandlerNotCorrupt();
    SIMPIterator remoteQueuePoints = getRemoteQueuePointIterator();
    SIMPRemoteQueuePointControllable rqp = null;
    while(remoteQueuePoints.hasNext())
    {
      SIMPXmitPoint nextRqp = (SIMPXmitPoint)remoteQueuePoints.next();
       String rqpID = nextRqp.getId();
       if(id.equals(rqpID))
       {
         //we have found it
         rqp = (SIMPRemoteQueuePointControllable) nextRqp;
         break; 
       }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteQueuePointControlByID", rqp);
    return rqp;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueueControllable#getRemoteQueuePointControlByMEUuid(java.lang.String)
   */
  public SIMPRemoteQueuePointControllable getRemoteQueuePointControlByMEUuid(String id) throws SIMPInvalidRuntimeIDException, SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "getRemoteQueuePointControlByMEUuid",
        new Object[] { id });
    
    assertMessageHandlerNotCorrupt();
    
//  ids are assumed to be SIBUuid8 format
    SIBUuid8 uuid = null;
    try
    {
      uuid = new SIBUuid8(id);
    }
    catch(NumberFormatException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.Queue.getRemoteQueuePointControlByMEUuid",
        "1:172:1.30",
        this);
    
      SIMPInvalidRuntimeIDException finalE =
        new SIMPInvalidRuntimeIDException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"Queue.getRemoteQueuePointControlByMEUuid",
                "1:180:1.30",
                          e,
                          id},
            null), e);

      SibTr.exception(tc, finalE);
      SibTr.error(tc,"INTERNAL_MESSAGING_ERROR_CWSIP0003",
      new Object[] {"Queue.getRemoteQueuePointControlByMEUuid",
          "1:188:1.30",
                    e,
                    id}); 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRemoteQueuePointControlByMEUuid", finalE);
      throw finalE;            
    }
    
    // TODO : shouldnt provide null here... should provide the consumerSetId
    SIMPRemoteQueuePointControllable remote = baseDest.getRemoteQueuePointControl(uuid, false);
    if(remote == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"Queue.getRemoteQueuePointControlByMEUuid",
                          "1:204:1.30",
                          id},
            null));     

      SibTr.exception(tc, finalE);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
      new Object[] {"Queue.getRemoteQueuePointControlByMEUuid",
                    "1:211:1.30",
                    id});
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRemoteQueuePointControlByMEUuid", finalE);
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteQueuePointControlByMEUuid", remote);
    
    return remote;
  }
  
}
