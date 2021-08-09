/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jfap.inbound.channel.CommsServerServiceFacade;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationSession;

/**
 * This class has some static helper methods for the server code.
 * 
 * @author Gareth Matthews
 */
public class StaticCATHelper {
    /** Class name for FFDC's */
    private static String CLASS_NAME = StaticCATHelper.class.getName();

    /** Our buffer pool manager */
    private static CommsByteBufferPool poolManager = CommsByteBufferPool.getInstance();

    /** Registers our trace component */
    private static final TraceComponent tc = SibTr.register(StaticCATHelper.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Log class info on static load */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/StaticCATHelper.java, SIB.comms, WASX.SIB, aa1225.01 1.62");
    }

    // *******************************************************************************************
    // *                         Exeption Handling Methods                                       *
    // *******************************************************************************************

    // Exception handling is done by using 1 public methods and 3 private helper methods and works
    // as follows:
    //
    // 1 - Comms code will call sendExceptionToClient()
    // 2 - This code create the buffer and send the data. The buffer is created by the method
    //     createExceptionBuffer().
    // 3 - The exception Id is determined by the getExceptionId() method and then the addException()
    //     will add the parts of the exception (the message etc) to the buffer. The
    //     createExceptionBuffer() method will also traverse down all the linked exceptions.

    /**
     * Sends an exception response back to the client.
     * 
     * @param throwable The exception to send back
     * @param probeId The probe ID of any corresponding FFDC record.
     * @param conversation The conversaton to use.
     * @param requestNumber The request number to reply with.
     */
    public static void sendExceptionToClient(Throwable throwable, String probeId,
                                             Conversation conversation, int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendExceptionToClient",
                        new Object[]
                        {
                         throwable,
                         probeId,
                         conversation,
                         requestNumber
                        });

        CommsByteBuffer buffer = poolManager.allocate();
        buffer.putException(throwable, probeId, conversation);
        // defect 99984 checking whether jmsServer feature is intact or its removed 
        if (CommsServerServiceFacade.getJsAdminService() != null)
        {
            try {
                conversation.send(buffer,
                                  JFapChannelConstants.SEG_EXCEPTION,
                                  requestNumber,
                                  JFapChannelConstants.PRIORITY_MEDIUM,
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
            } catch (SIException c) {
                FFDCFilter.processException(c, CLASS_NAME + ".sendExceptionToClient",
                                            CommsConstants.STATICCATHELPER_SEND_EXCEP_01);

                SibTr.error(tc, "COMMUNICATION_ERROR_SICO2023", c);
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "conversation send is not being called as jmsadminService is null");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendExceptionToClient");
    }

    /**
     * This method is used to flow a message down to the client that will get picked up
     * and delivered to the asynchronousException method of any listeners that the client
     * has registered.
     * 
     * @param throwable
     * @param probeId
     * @param clientSessionId
     * @param conversation
     * @param requestNumber
     */
    public static void sendAsyncExceptionToClient(Throwable throwable,
                                                  String probeId, short clientSessionId,
                                                  Conversation conversation, int requestNumber) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendAsyncExceptionToClient",
                        new Object[]
                        {
                         throwable,
                         probeId,
                         "" + clientSessionId,
                         conversation,
                         "" + requestNumber
                        });

        // BIT16 ConnectionObjectId
        // BIT16 Event Id
        // BIT16 ConsumerSessionId
        // Exception...
        CommsByteBuffer buffer = poolManager.allocate();
        buffer.putShort(0); // We do not need the connection object ID on the client
        buffer.putShort(CommsConstants.EVENTID_ASYNC_EXCEPTION); // Async exception
        buffer.putShort(clientSessionId);
        buffer.putException(throwable, probeId, conversation);

        try {
            conversation.send(buffer,
                              JFapChannelConstants.SEG_EVENT_OCCURRED,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
        } catch (SIException c) {
            FFDCFilter.processException(c, CLASS_NAME + ".sendAsyncExceptionToClient",
                                        CommsConstants.STATICCATHELPER_SEND_ASEXCEP_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2023", c);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendAsyncExceptionToClient");
    }

    // *******************************************************************************************
    // *                         Session Response Methods                                        *
    // *******************************************************************************************

    /**
     * Because of the larger amount of data needed to be sent back on the response to a session
     * create, I have split this into a seperate method so that we are not repeating code
     * all over the place.
     * 
     * @param segmentType The segment type to send the response with.
     * @param requestNumber The request number we are replying to.
     * @param conversation The conversation to send the reply on.
     * @param sessionId The session id of the session we just created.
     * @param session The session.
     * @param originalDestinationAddr The original address that was passed in. We will only send back
     *            a destination address if the actual one is different.
     */
    public static void sendSessionCreateResponse(int segmentType, int requestNumber,
                                                 Conversation conversation, short sessionId,
                                                 DestinationSession session,
                                                 SIDestinationAddress originalDestinationAddr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendSessionCreateResponse");

        CommsByteBuffer buffer = poolManager.allocate();

        // Add the Message processor session id if we are sending back a consumer response
        if (segmentType == JFapChannelConstants.SEG_CREATE_CONS_FOR_DURABLE_SUB_R ||
            segmentType == JFapChannelConstants.SEG_CREATE_CONSUMER_SESS_R) {
            long id = 0;
            try {
                id = ((ConsumerSession) session).getId();
            } catch (SIException e) {
                //No FFDC code needed
                //Only FFDC if we haven't received a meTerminated event.
                if (!((ConversationState) conversation.getAttachment()).hasMETerminated()) {
                    FFDCFilter.processException(e, CLASS_NAME + ".sendSessionCreateResponse",
                                                CommsConstants.STATICCATHELPER_SENDSESSRESPONSE_02);
                }

                // Not a lot we can do here - so just debug the error
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Unable to get session id", e);
            }
            buffer.putLong(id);
        }

        if (segmentType == JFapChannelConstants.SEG_CREATE_CONSUMER_SESS_R) {
            buffer.putShort(CommsConstants.CF_UNICAST);
        }

        buffer.putShort(sessionId);

        // Now get the destination address from the session so we can get sizes
        JsDestinationAddress destAddress = (JsDestinationAddress) session.getDestinationAddress();

        // We should only send back the destination address if it is different from the original.
        // To do this, we can do a simple compare on their toString() methods.
        if (originalDestinationAddr == null ||
            (!originalDestinationAddr.toString().equals(destAddress.toString()))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Destination address is different: Orig, New",
                            new Object[]
                            {
                             originalDestinationAddr,
                             destAddress
                            });

            buffer.putSIDestinationAddress(destAddress, conversation.getHandshakeProperties().getFapLevel());
        }

        try {
            // Send the response to the client.
            conversation.send(buffer,
                              segmentType,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
        } catch (SIException e) {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".sendSessionCreateResponse",
                                        CommsConstants.STATICCATHELPER_SENDSESSRESPONSE_01);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2023", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendSessionCreateResponse");
    }

}
