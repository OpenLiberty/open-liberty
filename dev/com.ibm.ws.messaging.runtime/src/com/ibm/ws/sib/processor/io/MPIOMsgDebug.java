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
package com.ibm.ws.sib.processor.io;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

public final class MPIOMsgDebug
{
  public final static String toString(final ControlMessage controlMsg, final int priority)
  {
    return debug(null,controlMsg,priority);
  }
  
  public final static String debug(final TraceComponent tc, final ControlMessage controlMsg, final int priority)
  {
    final ProtocolType protocolType = controlMsg.getGuaranteedProtocolType();
    final ControlMessageType controlType = controlMsg.getControlMessageType();
    final int hashCode = controlMsg.hashCode();
    final int msgPriority = controlMsg.getPriority() == null ? 0 : controlMsg.getPriority().intValue();
    final int sendPriority = priority;
    final long startTick;
    final long endTick;
    final SIBUuid12 streamId = controlMsg.getGuaranteedStreamUUID();
    
    if(controlType == ControlMessageType.ACK)
    {
      startTick = ((ControlAck)controlMsg).getAckPrefix();
      endTick = -1;
    }
    else if(controlType == ControlMessageType.ACKEXPECTED)
    {
      startTick = ((ControlAckExpected)controlMsg).getTick();
      endTick = -1;
    }
    else if(controlType == ControlMessageType.NACK)
    {
      startTick = ((ControlNack)controlMsg).getStartTick();
      endTick = ((ControlNack)controlMsg).getEndTick();
    }
    else if(controlType == ControlMessageType.SILENCE)
    {
      startTick = ((ControlSilence)controlMsg).getStartTick();
      endTick = ((ControlSilence)controlMsg).getEndTick();
    }     
    else // We can't be bothered to interogate all types of control messages
    {
      startTick = -1;
      endTick = -1;
    }
    
    final SIBUuid8 targetMessaginEngineUUID = controlMsg.getGuaranteedTargetMessagingEngineUUID();
    final SIBUuid8 sourceMessaginEngineUUID = controlMsg.getGuaranteedSourceMessagingEngineUUID();
    final SIBUuid12 targetDestDefUUID = controlMsg.getGuaranteedTargetDestinationDefinitionUUID();
    final SIBUuid8 sourceBusUUID = controlMsg.getGuaranteedCrossBusSourceBusUUID();
    final String linkName = controlMsg.getGuaranteedCrossBusLinkName();
      
    final JsDestinationAddress routingDestination = controlMsg.getRoutingDestination();
        
    if(tc != null)
    {
      debug(tc,
            protocolType,
            controlType,
            hashCode,
            msgPriority,
            sendPriority,
            startTick,
            endTick,
            streamId,
            targetMessaginEngineUUID,
            sourceMessaginEngineUUID,
            targetDestDefUUID,
            sourceBusUUID,
            linkName,
            routingDestination);
      
      return null;
    }
    else
    {
      return toString(protocolType,
                      controlType,
                      hashCode,
                      msgPriority,
                      sendPriority,
                      startTick,
                      endTick,
                      streamId,
                      targetMessaginEngineUUID,
                      sourceMessaginEngineUUID,
                      targetDestDefUUID,
                      sourceBusUUID,
                      linkName,
                      routingDestination);
    }
  }
  
  public final static String toString(final JsMessage jsMsg, final int priority)
  {
    return debug(null,jsMsg,priority);
  }
  
  public final static String debug(final TraceComponent tc, final JsMessage jsMsg, final int priority)
  {
    final ProtocolType protocolType = jsMsg.getGuaranteedProtocolType();
    final ControlMessageType controlType = null;
    final int hashCode = jsMsg.hashCode();
    final int msgPriority = jsMsg.getPriority() == null ? 0 : jsMsg.getPriority().intValue();
    final int sendPriority = priority;
    final long startTick = jsMsg.getGuaranteedValueValueTick();
    final long endTick = -1;
    final SIBUuid12 streamId = jsMsg.getGuaranteedStreamUUID();
    
    final SIBUuid8 targetMessaginEngineUUID = jsMsg.getGuaranteedTargetMessagingEngineUUID();
    final SIBUuid8 sourceMessaginEngineUUID = jsMsg.getGuaranteedSourceMessagingEngineUUID();
    final SIBUuid12 targetDestDefUUID = jsMsg.getGuaranteedTargetDestinationDefinitionUUID();
    final SIBUuid8 sourceBusUUID = jsMsg.getGuaranteedCrossBusSourceBusUUID();
    final String linkName = jsMsg.getGuaranteedCrossBusLinkName();
      
    final JsDestinationAddress routingDestination = jsMsg.getRoutingDestination();
        
    if(tc != null)
    {
      debug(tc,
            protocolType,
            controlType,
            hashCode,
            msgPriority,
            sendPriority,
            startTick,
            endTick,
            streamId,
            targetMessaginEngineUUID,
            sourceMessaginEngineUUID,
            targetDestDefUUID,
            sourceBusUUID,
            linkName,
            routingDestination);
      
      return null;
    }
    else
    {
      return toString(protocolType,
                      controlType,
                      hashCode,
                      msgPriority,
                      sendPriority,
                      startTick,
                      endTick,
                      streamId,
                      targetMessaginEngineUUID,
                      sourceMessaginEngineUUID,
                      targetDestDefUUID,
                      sourceBusUUID,
                      linkName,
                      routingDestination);
    }
  }
  
  private final static String toString( final ProtocolType protocolType,
                                        final ControlMessageType controlType,
                                        final int hashCode,
                                        final int msgPriority,
                                        final int sendPriority,
                                        final long startTick,
                                        final long endTick,
                                        final SIBUuid12 streamId,
                                        final SIBUuid8 targetMessaginEngineUUID,
                                        final SIBUuid8 sourceMessaginEngineUUID,
                                        final SIBUuid12 targetDestDefUUID,
                                        final SIBUuid8 sourceBusUUID,
                                        final String linkName,
                                        final JsDestinationAddress routingDestination)  
  {
    StringBuffer buffer = new StringBuffer(protocolType.toString());    
    if(controlType == null)
      buffer.append(" data message");
    else
    {
      buffer.append(" ");
      buffer.append(controlType);
      buffer.append(" message\n");
    }
    
    buffer.append("HashCode           : ");
    buffer.append(Integer.toHexString(hashCode));
    buffer.append("\n");
    
    if(sendPriority != -1)
    {
      buffer.append("Send Priority      : ");
      buffer.append(sendPriority);
      buffer.append("(msg)\n");
    }
    
    buffer.append("Msg Priority       : ");
    buffer.append(msgPriority);
    buffer.append("(msg)\n");
    
    buffer.append("Tick               : ");
    buffer.append(startTick);
    if(endTick != -1)
    {
      buffer.append("/");
      buffer.append(endTick);
    }
    buffer.append("\n");
    
    buffer.append("StreamId           : ");
    buffer.append(streamId);
    buffer.append("\n");
    
    buffer.append("Source ME          : ");
    buffer.append(sourceMessaginEngineUUID);
    buffer.append("\n");
    
    buffer.append("Target ME          : ");
    buffer.append(targetMessaginEngineUUID);
    buffer.append("\n");
    
    buffer.append("Target Dest        : ");
    buffer.append(targetDestDefUUID);
    buffer.append("\n");
    
    buffer.append("Source Bus         : ");
    buffer.append(sourceBusUUID);
    buffer.append("\n");
    
    buffer.append("Link Name          : ");
    buffer.append(linkName);
    buffer.append("\n");
    
    if(routingDestination != null)
    {
      buffer.append("Routing Dest Name: ");
      buffer.append(routingDestination.getDestinationName());
      buffer.append("\n");
      
      buffer.append("Routing Dest Bus : ");
      buffer.append(routingDestination.getBusName());      
    }
    buffer.append("\n");
    
    return buffer.toString();
  }

  private final static void debug(final TraceComponent tc,
                                  final ProtocolType protocolType,
                                  final ControlMessageType controlType,
                                  final int hashCode,
                                  final int msgPriority,
                                  final int sendPriority,
                                  final long startTick,
                                  final long endTick,
                                  final SIBUuid12 streamId,
                                  final SIBUuid8 targetMessaginEngineUUID,
                                  final SIBUuid8 sourceMessaginEngineUUID,
                                  final SIBUuid12 targetDestDefUUID,
                                  final SIBUuid8 sourceBusUUID,
                                  final String linkName,
                                  final JsDestinationAddress routingDestination)  
  {
    if(controlType == null)
    {
      SibTr.debug(tc,protocolType+" data message");
    }
    else
    {
      SibTr.debug(tc,protocolType+" "+controlType+" message");
    }
    
    SibTr.debug(tc,"HashCode           : "+Integer.toHexString(hashCode));
    if(sendPriority != -1)
    {
      SibTr.debug(tc,"Send Priority      : "+sendPriority);
    }
    SibTr.debug(tc,"Msg Priority       : "+msgPriority);
    SibTr.debug(tc,"Tick               : "+startTick);
    if(endTick != -1)
    {
      SibTr.debug(tc,"Tick               : "+startTick+"/"+endTick);
    }
    else
    {
      SibTr.debug(tc,"Tick               : "+startTick);
    }
    SibTr.debug(tc,"StreamId           : "+streamId);
    SibTr.debug(tc,"Source ME          : "+sourceMessaginEngineUUID);
    SibTr.debug(tc,"Target ME          : "+targetMessaginEngineUUID);
    SibTr.debug(tc,"Target Dest        : "+targetDestDefUUID);
    SibTr.debug(tc,"Source Bus         : "+sourceBusUUID);
    SibTr.debug(tc,"Link Name          : "+linkName);
    
    if(routingDestination != null)
    {
      SibTr.debug(tc,"Routing Dest Name: "+routingDestination.getDestinationName());
      SibTr.debug(tc,"Routing Dest Bus : "+routingDestination.getBusName());      
    }
  }

  public static void debug(TraceComponent tc, AbstractMessage aMessage, int priority)
  {
    if(aMessage.isControlMessage())
    {
      debug(tc,(ControlMessage)aMessage,priority);
    }
    else
    {
      debug(tc,(JsMessage)aMessage,priority);
    }
  }
}
