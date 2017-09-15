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

package com.ibm.ws.sib.processor.impl;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.SIMPLinkReceiverControllable;
import com.ibm.ws.sib.processor.runtime.SIMPLinkRemoteMessagePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPLinkTransmitterControllable;
import com.ibm.ws.sib.processor.runtime.SIMPVirtualLinkControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author millwood
 * <p>The LinkChangeListener is registered with TRM and is called back
 * whenever a change occurs to a link that is advertised in WLM.  This change could be
 * the advertisement or unadvertisement of the link.  On a link localising ME,
 * the notification is also when TRM connects to the messaging engine in the
 * foreign bus that localises the other end of the link.</p>
 * 
 * Note:  TRM refers to inter-bus links as bridges
 */
public final class LinkChangeListener implements com.ibm.ws.sib.trm.links.LinkChangeListener
{
  private static final TraceComponent tc =
    SibTr.register(
      LinkChangeListener.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  private MessageProcessor _messageProcessor;
  private DestinationManager _destinationManager;
  
  private HashMap<SIBUuid12, Boolean> linkStarted = new HashMap();

  /**
   * Constructs a new DestinationChangeListener
   *
   * @param messageProcessor The MP main object
   */
  public LinkChangeListener(
    MessageProcessor messageProcessor
  )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "LinkChangeListener",
        messageProcessor);

    _messageProcessor = messageProcessor;
    _destinationManager = messageProcessor.getDestinationManager();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkChangeListener", this);
  }

  public void linkChange (SIBUuid12 linkUuid, SIBUuid8 outboundMEUuid, SIBUuid8 inboundMEUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "linkChange", new Object[] {linkUuid, 
                                                    outboundMEUuid, 
                                                    inboundMEUuid});
   
    // Check if the link is cached in DestinationManager on this ME
    DestinationHandler dh = _destinationManager.getDestinationInternal(linkUuid, false);

    if ((dh != null) && (dh instanceof LinkHandler))
    {
      LinkHandler linkHandler = (LinkHandler) dh;
      boolean connectionDown = false;
      
      // Only need to take action if a link has become available.    
      if (outboundMEUuid != null)
      { 
        String linkType = linkHandler.getType(); 
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Changing a link of type: " + linkType);          
        try
        {
          if(linkType.equals("SIBVirtualMQLink")) // An MQLink          
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Drive updateLocalisationSet for an MQLinkHandler");                
            // Defect 242676: For MQLinks the inboundMEUuid will be null. We still
            // want to drive the updateLocalisationSet() call for MQLinks so that 
            // any pending messages (when the link was unavailable) can be forwarded 
            // to the link localising ME. But we need to drive it with a null 
            // RoutingMEUuid and non-null localising MEUuid.
            linkHandler.updateLocalisationSet(outboundMEUuid, // the localising ME uuid 
                                              null); // the routing ME uuid  
            
            // Keep track of whether the link has been started
            if (linkStarted.get(linkUuid) == null || !linkStarted.get(linkUuid).booleanValue())
            {
              // Call reallocation to remove any guesses in the stream
              linkHandler.requestReallocation();
            
              // SIB0115 - If the MQLink is not localised on this ME then we can go ahead and
              // update the health state of the LinkTransmitter controllables
              if(!linkHandler.isMQLink())
              {
                Iterator it = 
                  ((SIMPVirtualLinkControllable)linkHandler.getControlAdapter())
                    .getLinkRemoteQueuePointControllableIterator();
                while (it.hasNext())
                  ((HealthStateListener)((SIMPLinkTransmitterControllable)((SIMPLinkRemoteMessagePointControllable)it.next()).getOutboundTransmit())
                      .getHealthState()).updateHealth(HealthStateListener.CONNECTION_UNAVAILABLE_STATE,
                                                      HealthState.GREEN);
              }
              
              // Flag that the link is started
              linkStarted.put(linkUuid,Boolean.TRUE);              
            }
          }
          else // A SIB link
          {
            if(inboundMEUuid != null)
            {
              SIBUuid8 existingInboundUuid = 
                linkHandler.updateLocalisationSet(inboundMEUuid, // the localising ME uuid 
                                                outboundMEUuid); // the routing ME uuid
                                                
              if (linkStarted.get(linkUuid) == null || !linkStarted.get(linkUuid).booleanValue() || !existingInboundUuid.equals(inboundMEUuid))                
              {
                // Initiate pubsub across the link
                if (outboundMEUuid.equals(_messageProcessor.getMessagingEngineUuid()))
                  _messageProcessor.getProxyHandler().linkStarted(linkHandler.getBusName(), inboundMEUuid);
                  
                // Call reallocation to remove any guesses in the stream
                linkHandler.requestReallocation(); 
                
                // SIB0115
                // Update the health state of the LinkTransmitter controllables
                Iterator it = 
                  ((SIMPVirtualLinkControllable)linkHandler.getControlAdapter())
                    .getLinkRemoteQueuePointControllableIterator();
                while (it.hasNext())
                  ((HealthStateListener)((SIMPLinkTransmitterControllable)((SIMPLinkRemoteMessagePointControllable)it.next()).getOutboundTransmit())
                      .getHealthState()).updateHealth(HealthStateListener.CONNECTION_UNAVAILABLE_STATE,
                                                      HealthState.GREEN);
                
                // Update Link Receiver controllables
                it = 
                  ((SIMPVirtualLinkControllable)linkHandler.getControlAdapter())
                    .getLinkReceiverControllableIterator();
                while (it.hasNext())
                  ((HealthStateListener)((SIMPLinkReceiverControllable)it.next())
                      .getHealthState()).updateHealth(HealthStateListener.CONNECTION_UNAVAILABLE_STATE,
                                                      HealthState.GREEN);
                                
                linkStarted.put(linkUuid,Boolean.TRUE);
              }            
            } 
            else
              connectionDown = true;
          }
        }
        catch (SIException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.LinkChangeListener.linkChange",
            "1:219:1.28",
            this);
        }   
      }
      else
        connectionDown = true;
      
      // Update the health state and linkStarted map where the Link has gone down
      if (connectionDown) 
      {
        // Link is unavailable
        linkStarted.put(linkUuid,Boolean.FALSE); 
        
        // SIB0115
        // Update the health state of the Link Transmitter controllables if the link does not refer to an
        // MQLink homed on this ME.
        if(!linkHandler.isMQLink())
        {
          Iterator it = 
            ((SIMPVirtualLinkControllable)linkHandler.getControlAdapter())
              .getLinkRemoteQueuePointControllableIterator();
          while (it.hasNext())
            ((HealthStateListener)((SIMPLinkTransmitterControllable)((SIMPLinkRemoteMessagePointControllable)it.next()).getOutboundTransmit())
                .getHealthState()).updateHealth(HealthStateListener.CONNECTION_UNAVAILABLE_STATE,
                                              HealthState.RED);
        }
      
        // Update Link Receiver controllables where the vld does not reference an MQLink
        if(!linkHandler.getType().equals("SIBVirtualMQLink")) 
        {
          Iterator it = 
            ((SIMPVirtualLinkControllable)linkHandler.getControlAdapter())
              .getLinkReceiverControllableIterator();
          while (it.hasNext())
            ((HealthStateListener)((SIMPLinkReceiverControllable)it.next())
                .getHealthState()).updateHealth(HealthStateListener.CONNECTION_UNAVAILABLE_STATE,
                                                HealthState.RED);
        }
      }
    } 

      

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "linkChange");

    return;
  }
}
