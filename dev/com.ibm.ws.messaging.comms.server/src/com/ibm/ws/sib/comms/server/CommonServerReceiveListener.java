/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CATHandshakeProperties;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.server.clientsupport.StaticCATHelper;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * This abstract class is extended by all server side receive listeners. It provides the ability
 * to receive a JFap handshake and interpret the data. All results of the handshake are stored in
 * the link level state.
 * 
 * @author Gareth Matthews
 */
public abstract class CommonServerReceiveListener {
    /** The trace component */
    private static final TraceComponent tc = SibTr.register(CommonServerReceiveListener.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Class name for FFDC's */
    private static String CLASS_NAME = CommonServerReceiveListener.class.getName();

    /** Our NLS reference object */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

    /**
     * A flag to indicate whether this receive listener is performing client -> ME connection
     * handling (or false to indicate ME-ME).
     */
    private boolean clientConnection = true;

    /** The pool manager needed by all receive listeners */
    protected CommsServerByteBufferPool poolManager = CommsServerByteBufferPool.getInstance();

    /**
     * Constructor
     * 
     * @param clientConnection
     */
    public CommonServerReceiveListener(boolean clientConnection) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", "" + clientConnection);
        this.clientConnection = clientConnection;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Exchange operating parameters with the remote machine. The format of the data is essentially
     * a list of variable length fields. This allows for new fields to be added at any time without
     * requiring that either end of the channel alter their behaviour. If a user of the protocol
     * receives data containing an item that is not understood then it will be ignored. However, care
     * should be taken in sending back data to the peer as the initiator of the handshake will not
     * tollerate data that it does not understand (the kind of 'you know what version I am and so
     * should know I won't understand that' idea).
     * <p>
     * This method is used to receive handshakes from both ME's and remote clients. As such, it is
     * important to understand this implication when making changes to this method. If specific
     * code paths need to be followed, use of the <code>clientConnection</code> instance variable
     * should be checked as this is set to true for Client connections and false for ME-ME
     * connections.
     * <p>
     * The data format is as follows:
     * <ul>
     * <li>BIT16 FieldId
     * <li>BIT16 FieldLength
     * <li>BYTE[] Field
     * </ul>
     * <p>
     * The fields, their Ids and their lengths currently defined are as follows:-
     * <table>
     * <tr>
     * <td> ConnectionType </td><td> (ID 0x0000, Length 2) </td>
     * </tr><tr>
     * <td colspan="2">
     * This field determines the type of connection this handshake refers to. The two valid
     * values are CommsConstants.HANDSHAKE_CLIENT and CommsConstants.HANDSHAKE_ME.
     * It should be noted that we should never receive a HANDSHAKE_ME - as ME connections
     * should be filtered to a different receive listener.
     * </td>
     * </tr><tr>
     * <td> ProductVersion </td><td> (ID 0x0001, Length 2) </td>
     * </tr><tr>
     * <td colspan="2">
     * High order nibble gives the major product version. Lower nibble gives the minor version.
     * eg. 0x0301 refers to product version 3.1.
     * </td>
     * </tr><tr>
     * <td> FAPLevel </td><td> (ID 0x0002, Length 2) </td>
     * </tr><tr>
     * <td colspan="2">
     * Current supported level of the FAP.
     * This document describes version 1
     * </td>
     * </tr><tr>
     * <td> MaximumMessageSize </td><td> (ID 0x0003, Length 8) </td>
     * </tr><tr>
     * <td colspan="2">
     * The maximum size of messages supported by the Message Engine.
     * <strong>Negotiated downwards</strong>
     * </td>
     * </tr><tr>
     * <td> MaximumTransmissionSize </td><td> (ID 0x0004, Length 4) </td>
     * </tr><tr>
     * <td colspan="2">
     * The maximum size of an individual segment flow.
     * <strong>Negotiated downwards</strong>
     * </td>
     * </tr><tr>
     * <td> HeartbeatInterval </td><td> (ID 0x0005, Length 2) </td>
     * </tr><tr>
     * <td colspan="2">
     * The minimum time interval, in seconds, that the other machine should send Heartbeat
     * flows. A value of zero indicates that no Heartbeats should ever be sent.
     * <strong>Negotiated upwards</strong>
     * </td>
     * </tr><tr>
     * <td> HeartbeatTimeout </td><td> (ID 0x000D, Length 2) </td>
     * </tr><tr>
     * <td colspan="2">
     * The minimum amount of time to wait for a reply from a heartbeat request before assuming the
     * connection is dead.
     * <strong>Negotiated upwards</strong>
     * </td>
     * </tr><tr>
     * <td> Capabilities </td><td> (ID 0x0007, Length 2) </td>
     * </tr><tr>
     * <td colspan="2">
     * Bit flags indicating the capabilities of the communicating program:
     * <ul>
     * <li>0x0001 Transactions are supported
     * <li>0x0002 Reliable messages supported
     * <li>0x0004 Assured messages supported
     * <li>0xFFF8 Reserved
     * </ul>
     * </td>
     * </tr><tr>
     * <td> Product Id </td><td> (ID 0x000B, Length 2) </td>
     * </tr><tr>
     * <td colspan="2">
     * Identifies the product attempting to establish a connection. Known identifiers
     * are:
     * <ul>
     * <li> 0x0001 Jetstream </li>
     * <li> 0x0002 WebSphere MQ </li>
     * <li> 0x0002 .NET </li>
     * </ul>
     * It is not clear how widely used this field is.
     * </td>
     * </tr><tr>
     * <td> Supported FAP Versions </td><td> (UD 0x000C, Length 32) </td>
     * </tr><tr>
     * <td colspan="2">
     * A bitmap which specifies which FAP versions the party attempting to establish the
     * connection supports. The bitmap is 32 bytes long (as to include 256 FAP values) and
     * in big-endian (Java) format. The LSB corresponds to FAP version 1.
     * <p>
     * <strong>This was introduced in FAP version 3</strong><br>
     * If this field is not present then the highest FAP level that may be returned from
     * negotiation is FAP level 2.
     * </td>
     * </tr><tr>
     * <td> WMQRA Connect</td><td> (UD 0x000E, Length 0) </td>
     * </tr><tr>
     * <td colspan="2">
     * If this field is flowed in a handshake then it indicates to the WMQ RA support code running in the z/OS CRA
     * that it needs to perform special processing to allow support for the split process WMQ RA architecture.
     * <p>
     * <strong>NB</strong>: This field is only valid on handshakes originating from the z/OS SR to a z/OS CRA via the TCP Proxy Bridge chain.
     * </tr><tr>
     * <td> Cell name</td><td> (UD 0x000F, Length 4 + string length) </td>
     * </tr><tr>
     * <td colspan="2">
     * If this field is flowed it provides the name of the cell in which the calling code is running.
     * </tr><tr>
     * <td> Node name</td><td> (UD 0x0010, Length 4 + string length) </td>
     * </tr><tr>
     * <td colspan="2">
     * If this field is flowed it provides the name of the node in which the calling code is running.
     * </tr><tr>
     * <td> Server name</td><td> (UD 0x0011, Length 4 + string length) </td>
     * </tr><tr>
     * <td colspan="2">
     * If this field is flowed it provides the name of the server in which the calling code is running.
     * </tr><tr>
     * <td> Cluster name</td><td> (UD 0x0012, Length 4 + string length) </td>
     * </tr><tr>
     * <td colspan="2">
     * If this field is flowed it provides the name of the cluster in which the calling code is running.
     * </td></tr></table>
     * 
     * @param request
     * @param conversation
     * @param requestNumber
     * @param allocatedFromBufferPool
     */
    protected void rcvHandshake(CommsByteBuffer request, Conversation conversation, int requestNumber,
                                boolean allocatedFromBufferPool) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rcvHandshake",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              "" + requestNumber,
                                              "" + allocatedFromBufferPool,
                                           });

        //How the conversation which performed this handshake is to be used. By default this will be JFAP unless something
        //else is specified as part of the handshake.
        ConversationUsageType usageType = ConversationUsageType.JFAP;

        //Create handshake properties here but only set them later if this isn't a WMQRA handshake.
        CATHandshakeProperties catHandshakeProps = new CATHandshakeProperties();

        // Ensure we send back a connection type to be consistent with the doc.
        // Just send back what we received...
        CommsByteBuffer reply = poolManager.allocate();
        reply.put(request.get());

        // Get the current FAP level, overriding from SIB properties if required
        int currentFapLevel = CommsUtils.getRuntimeIntProperty(CommsConstants.SERVER_FAP_LEVEL_KEY,
                                                               "" + CommsConstants.CURRENT_FAP_VERSION);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Server is FAP Level: " + currentFapLevel);

        boolean noErrorOccurred = true;
        // These are the mandatory fields we need to check. If any of these are missing on any
        // initial handshake flow, then this is an error.
        boolean rcvFapLevel = false;
        boolean rcvProdVersion = false;

        // The Client -> ME initial handshake should always carry the Product Id (not needed for
        // ME-> ME).
        boolean rcvProdId = false;

        // Ensure we do checking about the correct FAP level to select.
        boolean rcvSupportedFaps = false;
        boolean[] supportedFaps = null;
        int peerFapLevel = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "----- Received the following handshake data -----");

        while (request.hasRemaining() && noErrorOccurred) {
            short fieldId = request.getShort();

            switch (fieldId) {

                case CommsConstants.FIELDID_PRODUCT_VERSION:

                    short productVersionFieldLength = request.getShort();

                    // Ensure the length is 2
                    if (productVersionFieldLength != 2) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Product Version field length", productVersionFieldLength);
                        rejectHandshake(conversation, requestNumber, "ProductVersion length");
                        noErrorOccurred = false;
                    } else {
                        byte upperProductVersion = request.get();
                        byte lowerProductVersion = request.get();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Product Version   : " + upperProductVersion + "." + lowerProductVersion);

                        rcvProdVersion = true;

                        catHandshakeProps.setMajorVersion(upperProductVersion);
                        catHandshakeProps.setMinorVersion(lowerProductVersion);

                        // Always inform the client what version we are
                        reply.putShort(CommsConstants.FIELDID_PRODUCT_VERSION);
                        reply.putShort(2);
                        reply.put((byte) SIMPConstants.API_MAJOR_VERSION);
                        reply.put((byte) SIMPConstants.API_MINOR_VERSION);
                    }

                    break;

                case CommsConstants.FIELDID_FAP_LEVEL:

                    short fapLevelFieldLength = request.getShort();

                    // Ensure the length is 2
                    if (fapLevelFieldLength != 2) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected FAP Level field length", fapLevelFieldLength);
                        rejectHandshake(conversation, requestNumber, "FAPLevel length");
                        noErrorOccurred = false;
                    } else {
                        peerFapLevel = request.getShort();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " FAP Version       : " + peerFapLevel);
                        rcvFapLevel = true;
                    }

                    break;

                case CommsConstants.FIELDID_MAX_MESSAGE_SIZE:

                    short maxMessageFieldLength = request.getShort();

                    // Ensure the length is 8
                    if (maxMessageFieldLength != 8) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Max Message Size field length", maxMessageFieldLength);
                        rejectHandshake(conversation, requestNumber, "MaxMessageSize length");
                        noErrorOccurred = false;
                    } else {
                        long maxMessageSize = request.getLong();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Max Msg Size      : " + maxMessageSize);
                        catHandshakeProps.setMaxMessageSize(maxMessageSize);
                    }

                    break;

                case CommsConstants.FIELDID_MAX_TRANSMISSION_SIZE:

                    short maxTransmissionFieldLength = request.getShort();

                    // Ensure the length is 4
                    if (maxTransmissionFieldLength != 4) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Max Transmission Size field length", maxTransmissionFieldLength);
                        rejectHandshake(conversation, requestNumber, "MaxTransmissionSize length");
                        noErrorOccurred = false;
                    } else {
                        int maxTransmissionSize = request.getInt();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Max Tx Size       : " + maxTransmissionSize);
                        catHandshakeProps.setMaxTransmissionSize(maxTransmissionSize);
                    }

                    break;

                case CommsConstants.FIELDID_HEARTBEAT_INTERVAL:

                    short heartbeatFieldLength = request.getShort();

                    // Ensure the length is 2
                    if (heartbeatFieldLength != 2) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Heartbeat Interval field length", heartbeatFieldLength);
                        rejectHandshake(conversation, requestNumber, "HeartbeatInterval length");
                        noErrorOccurred = false;
                    } else {
                        short heartbeatInterval = request.getShort();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Heartbeat Interval: " + heartbeatInterval);

                        // Heartbeats are negotiated upwards - so take the maximum of the two values
                        short maxInterval = (short) Math.max(conversation.getHeartbeatInterval(), heartbeatInterval);

                        // If we are set to be higher than the client, then we need to reply
                        // and tell the client
                        if (maxInterval > heartbeatInterval) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "> Negotiating Heartbeat Interval to: " + maxInterval);

                            reply.putShort(CommsConstants.FIELDID_HEARTBEAT_INTERVAL);
                            reply.putShort(2);
                            reply.putShort(maxInterval);
                        }
                        // Otherwise accept the client values
                        else {
                            conversation.setHeartbeatInterval(maxInterval);
                        }

                        catHandshakeProps.setHeartbeatInterval(maxInterval);
                    }

                    break;

                case CommsConstants.FIELDID_HEARTBEAT_TIMEOUT:

                    short heartbeatTimeoutFieldLength = request.getShort();

                    // Ensure the length is 2
                    if (heartbeatTimeoutFieldLength != 2) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Heartbeat Timeout field length", heartbeatTimeoutFieldLength);
                        rejectHandshake(conversation, requestNumber, "HeartbeatTimeout length");
                        noErrorOccurred = false;
                    } else {
                        short heartbeatTimeout = request.getShort();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Heartbeat Timeout : " + heartbeatTimeout);

                        // Heartbeats are negotiated upwards - so take the maximum of the two values
                        short maxTimeout = (short) Math.max(conversation.getHeartbeatTimeout(), heartbeatTimeout);

                        // If we are set to be higher than the client, then we need to reply
                        // and tell the client
                        if (maxTimeout > heartbeatTimeout) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "> Negotiating Heartbeat Timeout to: " + maxTimeout);

                            reply.putShort(CommsConstants.FIELDID_HEARTBEAT_TIMEOUT);
                            reply.putShort(2);
                            reply.putShort(maxTimeout);
                        }
                        // Otherwise accept the client values
                        else {
                            conversation.setHeartbeatTimeout(maxTimeout);
                        }

                        catHandshakeProps.setHeartbeatTimeout(maxTimeout);
                    }

                    break;

                case CommsConstants.FIELDID_CAPABILITIES:

                    short capabilityLength = request.getShort();

                    if (capabilityLength != 2) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Capability field length", capabilityLength);
                        rejectHandshake(conversation, requestNumber, "Capability length");
                        noErrorOccurred = false;
                    } else {
                        short capability = request.getShort();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Capabilities      : 0x" + Integer.toHexString(capability));

                        if ((capability & CommsConstants.CAPABILITIY_RESERVED_MASK) != 0) {
                            // If we have been presented with capabilities we are unfamiliar with, mask
                            // off the unfamiliar bits and reply stating that we support the capabilities
                            // we are aware of.
                            capability &= ~CommsConstants.CAPABILITIY_RESERVED_MASK;

                            reply.putShort(CommsConstants.FIELDID_CAPABILITIES);
                            reply.putShort(2);
                            reply.putShort(capability);
                        }

                        catHandshakeProps.setCapabilites(capability);
                    }

                    break;

                case CommsConstants.FIELDID_PRODUCT_ID:

                    short productIdLength = request.getShort();

                    if (productIdLength != 2) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Product Id length", productIdLength);
                        rejectHandshake(conversation, requestNumber, "Product Id length");
                        noErrorOccurred = false;
                    } else {
                        short productId = request.getShort();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Peer Product Id   : 0x" + Integer.toHexString(productId));

                        rcvProdId = true;

                        catHandshakeProps.setPeerProductId(productId);

                        // Reply to the client letting them know who we are
                        reply.putShort(CommsConstants.FIELDID_PRODUCT_ID);
                        reply.putShort(2);
                        reply.putShort(CommsConstants.PRODUCT_ID_JETSTREAM);
                    }

                    break;

                case CommsConstants.FIELDID_SUPORTED_FAPS:

                    short supportedFapsLength = request.getShort();
                    if (supportedFapsLength != 32) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected Supported FAPs length: " + supportedFapsLength);
                        rejectHandshake(conversation, requestNumber, "Supported FAPs length");
                        noErrorOccurred = false;
                    } else {
                        // If trace is switched on - create a StringBuffer to build up a description
                        // of the supported FAP levels.
                        StringBuffer traceData = null;

                        // Read the supported FAP bitmap as an array of 32 bytes.
                        byte[] fapsBitmap = request.get(32);

                        // Convert the bitmap into an array of boolean values where
                        // index 0 corresponds to FAP level 1.
                        supportedFaps = new boolean[256];
                        for (int i = 0; i < supportedFaps.length; ++i) {
                            int byteValue = fapsBitmap[(255 - i) / 8];
                            boolean bitOn = ((byteValue >> (i % 8)) & 0x01) == 0x01;
                            supportedFaps[i] = bitOn;

                            if (bitOn) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    if (traceData == null)
                                        traceData = new StringBuffer().append(i + 1);
                                    else
                                        traceData.append(", ").append(i + 1);
                                }
                            }
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, " Supported FAP's   : " + traceData.toString());

                        // Note that we found a supported FAPs field.
                        rcvSupportedFaps = true;
                    }
                    break;

                case CommsConstants.FIELDID_CONVERSATION_USAGE_TYPE:

                    final short length = request.getShort();

                    if (length != 4) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unexpected FIELDID_CONVERSATION_USAGE_TYPE length: " + length);
                        rejectHandshake(conversation, requestNumber, "FIELDID_CONVERSATION_USAGE_TYPE length");
                        noErrorOccurred = false;
                    } else {
                        //Generate ConversationUsageType from buffer.
                        usageType = ConversationUsageType.deserialize(request);

                        //We only allow non-JFAP usage types on z/OS in the CRA so check this is the case.
                        if (usageType != ConversationUsageType.JFAP) {
                            //...Romil Liberty change
                            //final PlatformHelper helper = PlatformHelperFactory.getPlatformHelper();
                            //if(!(helper.isZOS() && helper.isCRAJvm()))
                            //{
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Not running on z/OS CRA rejecting handshake");
                            rejectHandshake(conversation, requestNumber, "FIELDID_CONVERSATION_USAGE_TYPE platform");
                            noErrorOccurred = false;

                            //Venu Liberty COMMS
                            // z/OS specific code has been removed
                        }
                    }
                    break;

                case CommsConstants.FIELDID_CELL_NAME:
                    //Ignore field length.
                    request.getShort();
                    final String remoteCellName = request.getString();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Remote cell name: " + remoteCellName);

                    catHandshakeProps.setRemoteCellName(remoteCellName);

                    break;

                case CommsConstants.FIELDID_NODE_NAME:
                    //Ignore field length.
                    request.getShort();
                    final String remoteNodeName = request.getString();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Remote node name: " + remoteNodeName);

                    catHandshakeProps.setRemoteNodeName(remoteNodeName);

                    break;

                case CommsConstants.FIELDID_SERVER_NAME:
                    //Ignore field length.
                    request.getShort();
                    final String remoteServerName = request.getString();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Remote server name: " + remoteServerName);

                    catHandshakeProps.setRemoteServerName(remoteServerName);

                    break;

                case CommsConstants.FIELDID_CLUSTER_NAME:
                    //Ignore field length.
                    request.getShort();
                    final String remoteClusterName = request.getString();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Remote cluster name: " + remoteClusterName);

                    catHandshakeProps.setRemoteClusterName(remoteClusterName);

                    break;

                default:

                    // We must ensure that we skip past the data so that we safely ignore the unknown
                    // field. The format will always be the same, i.e. BIT16 length, followed by the data.
                    short fieldLength = request.getShort();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "* Unknown Field    : Id: " + fieldId + ", Length: " + fieldLength);

                    // Now skip the data
                    request.skip(fieldLength);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "----- End of handshake data ---------------------");

        if (noErrorOccurred) {
            // Now check we received all the mandatory fields
            if (!rcvFapLevel) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "No FAP level present in handshake");
                rejectHandshake(conversation, requestNumber, "FAPLevel");
            } else if (!rcvProdVersion) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "No Product Version present in handshake");
                rejectHandshake(conversation, requestNumber, "ProductVersion");
            } else if (!rcvProdId && clientConnection) // This is only bad if this is a client - ME conn
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "No Product Id present in handshake");
                rejectHandshake(conversation, requestNumber, "ProductId");
            }
            // Otherwise, we can accept the connection
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Handshake contained enough information to continue");
                boolean foundSuitableFapLevel = true;

                // Start out by considering using our peer's FAP.
                int linkFapLevel = peerFapLevel;

                // If the peers FAP level is greater than the level we support
                // downgrade the support to our level.
                if (linkFapLevel > currentFapLevel) {
                    linkFapLevel = currentFapLevel;
                }

                // Did the FAP flow contain a supported FAP versions field? Note we only look at these
                // fields if we ourselves are at FAP level 3 or above.
                if (rcvSupportedFaps && currentFapLevel >= JFapChannelConstants.FAP_VERSION_3) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Examining the peers supported FAP levels");

                    // It may be the case that an IFIX has resulted in the client
                    // not understanding the level of FAP that we wish to use.
                    // Consult the set of supported FAP levels that the client
                    // sent to determine a common, supported, FAP level.
                    // Note: that the table of suitable FAP levels uses index zero
                    //       for FAP level 1 - hence the +1 and -1 code.
                    int i = linkFapLevel - 1;
                    boolean suitableLevelFound = false;
                    while ((i >= 0) && !suitableLevelFound) {
                        suitableLevelFound =
                                        supportedFaps[i] &&
                                                        (supportedFaps[i] == CommsConstants.isFapLevelSupported(i + 1));

                        if (!suitableLevelFound)
                            i -= 1;
                    }

                    if (suitableLevelFound)
                        linkFapLevel = i + 1;
                    else {
                        // There are no mutually agreeable FAP levels.
                        // Fail the handshake.
                        foundSuitableFapLevel = false;
                        rejectHandshake(conversation, requestNumber, "No mutually agreeable FAP level");
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Peer did not supply supported FAP levels " +
                                                              "or SupportedFAPVersions field is not applicable");

                    // If the peer did not supply any supported FAP versions then we can only talk
                    // version 1 or 2.
                    if (peerFapLevel == JFapChannelConstants.FAP_VERSION_1) {
                        linkFapLevel = JFapChannelConstants.FAP_VERSION_1;
                    } else {
                        linkFapLevel = JFapChannelConstants.FAP_VERSION_2;
                    }
                }

                // If we have found a suitable FAP level - complete the handshake.
                if (foundSuitableFapLevel) {
                    // Add FAP level to reply.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Informing peer of negotiatied value: " + linkFapLevel);

                    reply.putShort(CommsConstants.FIELDID_FAP_LEVEL);
                    reply.putShort(2);
                    reply.putShort(linkFapLevel);

                    // Set the FAP level in the handshake properties - so that we can
                    // refer to it later.
                    catHandshakeProps.setFapLevel((short) linkFapLevel);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Handshake Properties: ", catHandshakeProps);

                    //Only set handshake properties on Conversation if usageType requires it.
                    if (usageType.requiresNormalHandshakeProcessing())
                        conversation.setHandshakeProperties(catHandshakeProps);

                    // Send the handshake reply to our peer.
                    try {
                        conversation.send(reply,
                                          JFapChannelConstants.SEG_HANDSHAKE,
                                          requestNumber,
                                          JFapChannelConstants.PRIORITY_HANDSHAKE,
                                          true,
                                          ThrottlingPolicy.BLOCK_THREAD,
                                          null);
                    } catch (SIException e) {
                        FFDCFilter.processException(e,
                                                    CLASS_NAME + ".rcvHandshake",
                                                    CommsConstants.COMMONSERVERRECEIVELISTENER_RHSHAKE_01,
                                                    this);

                        SibTr.error(tc, "COMMUNICATION_ERROR_SICO2019", e);
                    }
                }
            }
        }

        request.release(allocatedFromBufferPool);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rcvHandshake");
    }

    /**
     * This method is used to inform the client that we are rejecting their handshake. Typically
     * this will never happen unless a third party client is written, or an internal error occurs.
     * However, we should check for an inproperly formatted handshake and inform the client if
     * such an error occurs.
     * 
     * @param conversation
     * @param requestNumber
     * @param rejectedField A String that indicates the field that was rejected.
     */
    private void rejectHandshake(Conversation conversation, int requestNumber, String rejectedField) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rejectHandshake",
                                           new Object[] { conversation, requestNumber, rejectedField });

        SIConnectionLostException exception = new SIConnectionLostException(
                        nls.getFormattedMessage("INVALID_PROP_SICO8012", null, null)
                        );

        FFDCFilter.processException(exception,
                                    CLASS_NAME + ".rejectHandshake",
                                    CommsConstants.COMMONSERVERRECEIVELISTENER_HSREJCT_01,
                                    this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Invalid handshake type received - rejecting field:",
                                           rejectedField);

        StaticCATHelper.sendExceptionToClient(exception,
                                              CommsConstants.COMMONSERVERRECEIVELISTENER_HSREJCT_01,
                                              conversation,
                                              requestNumber);

        // At this point we really don't want anything more to do with this client - so close him
        closeConnection(conversation);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rejectHandshake");
    }

    /**
     * This method should be implemented by subclasses such that when it is called the connection
     * is immediately closed. No suprises there then.
     * <p>
     * This method is provided to give any specific receiver listeners the oppurtunity to perform
     * any required clean up before actually closing the converstation. Implementors at the very
     * least should call close() on the Conversation.
     * 
     * @param conversation The conversation to be closed.
     */
    protected abstract void closeConnection(Conversation conversation);
}
