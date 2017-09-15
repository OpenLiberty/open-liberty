/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Properties;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.websphere.sib.management.SibNotificationConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jfap.inbound.channel.CommsServerServiceFacade;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsEngineComponentWithEventListener;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.comms.ClientComponentHandshake;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.CompHandshake;
import com.ibm.ws.sib.comms.ConnectionMetaData;
import com.ibm.ws.sib.comms.ConnectionProperties;
import com.ibm.ws.sib.comms.client.ConnectionMetaDataImpl;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.server.IdToSICoreConnectionTable;
import com.ibm.ws.sib.comms.server.ServerJFapCommunicator;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.ConnectionClosedListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.NoCapacityException;
import com.ibm.ws.sib.mfp.ConnectionSchemaSet;
import com.ibm.ws.sib.mfp.impl.CompHandshakeFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * An implementation of the ClientConnection to be used by TRM on the server.
 */
public class ServerSideConnection extends ServerJFapCommunicator implements ClientConnection,
                ConnectionClosedListener
{
    private static String CLASS_NAME = ServerSideConnection.class.getName();
    private static final TraceComponent tc = SibTr.register(ServerSideConnection.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

    //@start_class_string_prolog@
    public static final String $sccsid = "@(#) 1.63 SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/ServerSideConnection.java, SIB.comms, WASX.SIB, aa1225.01 10/03/25 10:06:19 [7/2/12 05:59:53]";
    //@end_class_string_prolog@

    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Source Info: " + $sccsid);
    }

    /** The SICoreConnection */
    private SICoreConnection conn;

    private boolean conversationClosed = false;

    private static final int NOTIFICATION_TYPE_CLIENT_CONNECT = 1;
    private static final int NOTIFICATION_TYPE_CLIENT_DISCONNECT = 2;
    private static final int NOTIFICATION_TYPE_CLIENT_FAILURE = 3;

    private String connectionUserId = null;

    // Note whether this is a cloned connection or not - to save on an exchange (line turn around) during SEG_CREATE_CLONED_CONNECTION we
    // can reset an existing cloned connection each time one is closed at SEG_CLOSE_CONNECTION - we only do this reset for cloned connections
    // not for directly created connections hence the need to know whether a connection was cloned or not. For cloned connections this is set
    // to the parent core connection otherwise it is set null.
    private final SICoreConnection parentConnection;

    /**
     * Constructors
     */
    public ServerSideConnection(final Conversation conversation) {
        this(conversation, null);
    }

    public ServerSideConnection(final Conversation conversation, final SICoreConnection parent) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", "conversation=" + conversation + ", parent=" + parent);

        setConversation(conversation);
        conversation.addConnectionClosedListener(this, ConversationUsageType.JFAP);
        parentConnection = parent;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Return the parent core connection
     */
    public SICoreConnection getParentConnection() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getParentConnection");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getParentConnection", "rc=" + parentConnection);
        return parentConnection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.comms.ClientConnection#connect(com.ibm.ws.sib.comms.ConnectionProperties, com.ibm.ws.sib.comms.ClientComponentHandshake)
     */
    @Override
    public void connect(ConnectionProperties cp, ClientComponentHandshake cch)
                    throws SIResourceException, SIAuthenticationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "connect");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "connect");

        // This method call is only valid on the client version of this class
        SIErrorException e = new SIErrorException(
                        nls.getFormattedMessage("INVALID_METHOD_ON_SERVER_SICO2050", null, null)
                        );

        FFDCFilter.processException(e, CLASS_NAME + ".connect",
                                    CommsConstants.SERVERSIDECONNECTION_CONNECT_01, this);
        throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.comms.ClientConnection#setSIMPConnection(com.ibm.ws.sib.processor.SIMPConnection)
     */
    @Override
    public void setSICoreConnection(SICoreConnection conn)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setSICoreConnection");

        // Get a little paranoid that this isn't been set twice or anything...
        if ((this.conn != null) && (this.conn != conn))
        {
            SIErrorException e = new SIErrorException(
                            nls.getFormattedMessage("SICONN_ALREADY_SET_SICO2051", null, null)
                            );

            FFDCFilter.processException(e, CLASS_NAME + ".setSICoreConnection",
                                        CommsConstants.SERVERSIDECONNECTION_SETSICORECONN_01, this);
            throw e;
        }

        // Associate this conversation's ID with the SICoreConnection assigned in a
        // per physical connection table.
        IdToSICoreConnectionTable idToConnTable =
                        ((ServerLinkLevelState) getConversation().getLinkLevelAttachment()).getSICoreConnectionTable();
        idToConnTable.add(getConversation().getId(), conn);

        this.conn = conn;

        emitNotification(NOTIFICATION_TYPE_CLIENT_CONNECT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setSICoreConnection");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.comms.ClientConnection#getSIMPConnection()
     */
    @Override
    public SICoreConnection getSICoreConnection() throws SIConnectionLostException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSICoreConnection");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSICoreConnection");
        return this.conn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.comms.CommsConnection#trmHandshakeExchange(byte[])
     */
    @Override
    public byte[] trmHandshakeExchange(byte[] data)
                    throws
                    SIConnectionLostException,
                    SIConnectionDroppedException,
                    SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "trmHandshakeExchange");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "trmHandshakeExchange");

        // This method call is only valid on the client version of this class
        SIErrorException e = new SIErrorException(
                        nls.getFormattedMessage("INVALID_METHOD_ON_SERVER_SICO2052", null, null)
                        );

        FFDCFilter.processException(e, CLASS_NAME + ".trmHandshakeExchange",
                                    CommsConstants.SERVERSIDECONNECTION_TRMEXCHANGE_01, this);
        throw e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.comms.CommsConnection#mfpHandshakeExchange(byte[])
     */
    @Override
    public byte[] mfpHandshakeExchange(byte[] data)
                    throws
                    SIConnectionLostException,
                    SIConnectionDroppedException,
                    SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "mfpHandshakeExchange");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "mfpHandshakeExchange");

        // This method call is only valid on the client version of this class
        SIErrorException e = new SIErrorException(
                        nls.getFormattedMessage("INVALID_METHOD_ON_SERVER_SICO2053", null, null)
                        );

        FFDCFilter.processException(e, CLASS_NAME + ".mfpHandshakeExchange",
                                    CommsConstants.SERVERSIDECONNECTION_MFPEXCHANGE_01, this);
        throw e;
    }

    /**
     * Attempts to send an MFP message schema to the peer at the highest possible
     * priority. At Message encode time MFP can discover that the destination entity
     * cannot decode the message about to be sent. This method is then called to
     * send a top priority transmission containing the message schema ahead of the
     * message relying on it to ensure it can be decoded correctly.
     * 
     * @param schemaData The data to send to the MFP component on this Connection's peer.
     * 
     * @throws SIConnectionLostException Thrown if a communications failure occurres.
     * @throws SIConnectionDroppedException Thrown if the underlying connection is closed
     * @throws NoCapacityException Thrown if there is currently no capacity to send
     *             the data.
     * @throws SIConnectionUnavailableException Thrown if it is invalid to invoke
     *             this method at this point in time. For example if the Connection has been
     *             closed.
     */
    @Override
    public void sendMFPSchema(byte[] schemaData)
                    throws
                    SIConnectionLostException,
                    SIConnectionDroppedException,
                    SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendMFPSchema");

        CommsByteBuffer data = getCommsByteBuffer();
        data.wrap(schemaData);

        //Pass on schema data to server
        jfapSend(data,
                 JFapChannelConstants.SEG_SEND_SCHEMA_NOREPLY,
                 JFapChannelConstants.PRIORITY_HIGHEST,
                 true,
                 ThrottlingPolicy.BLOCK_THREAD);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendMFPSchema");
    }

    /**
     * Closes the conversation.
     * 
     * @oparam reset - true means the connection is being reset (rather than closed)
     * 
     * @see com.ibm.ws.sib.comms.ClientConnection#close()
     */
    @Override
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");
        close(false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    public void close(final boolean reset)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "close", "reset=" + reset);

        synchronized (this)
        {
            if (!conversationClosed)
            {
                emitNotification(NOTIFICATION_TYPE_CLIENT_DISCONNECT);
            }
            conversationClosed = true;
        }

        if (!reset) {
            ServerTransportAcceptListener.getInstance().removeConversation(getConversation());

            try
            {
                // Now close the conversation - if we are connected to a FAP 9 or above client then we can close
                // the conversation quickly and avoid further additional costly exchanges since we know that the
                // other end will do the same.
                if (getConversation().getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_9) {
                    getConversation().fastClose();
                } else {
                    getConversation().close();
                }
            } catch (SIConnectionLostException e)
            {
                FFDCFilter.processException(e, CLASS_NAME + ".close",
                                            CommsConstants.SERVERSIDECONNECTION_CLOSE_01, this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Unable to close conversation", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "close");
    }

    /** @see com.ibm.ws.sib.comms.CommsConnection#getConnectionInfo() */
    @Override
    public String getConnectionInfo()
    {
        return null;
    }

    /** @see com.ibm.ws.sib.comms.CommsConnection#getMetaData() */
    @Override
    public ConnectionMetaData getMetaData()
    {
        return new ConnectionMetaDataImpl(getConversation().getMetaData(), getConversation().getHandshakeProperties());
    }

    /**
     * Called when the JFap channel has closed the underlying connection. At this point we can
     * inform MFP so that they can delete the schema information that refered to this connection.
     * 
     * @param connectionReference
     */
    @Override
    public void connectionClosed(Object connectionReference)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "connectionClosed", connectionReference);

        // Take this oppurtunity to clean up any resources thatare left hanging around
        ServerTransportAcceptListener.getInstance().removeAllConversations(connectionReference);

        try
        {
            // Get hold of MFP and inform it of connection closure
            CompHandshake ch = (CompHandshake) CompHandshakeFactory.getInstance();
            ch.compClose(this);
        } catch (Exception e1)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "MFP unable to create CompHandshake Singleton", e1);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "connectionClosed");
    }

    /**
     * Called when the underlying network connection fails. This is used to emit a failure
     * event notification.
     */
    public void failed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "failed");

        synchronized (this)
        {
            if (!conversationClosed)
            {
                emitNotification(NOTIFICATION_TYPE_CLIENT_FAILURE);
            }
            conversationClosed = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "failed");
    }

    /**
     * Used to synchronously request an MFP schema from an attached client.
     * <p>
     * The method of servers requesting schemas from clients is not supported.
     */
    @Override
    public byte[] requestMFPSchemata(byte[] schemaData)
                    throws SIConnectionLostException,
                    SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "requestMFPSchemata", schemaData);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "requestMFPSchemata", null);

        // This method call is only valid on the client version of this class
        SIErrorException e = new SIErrorException(
                        nls.getFormattedMessage("INVALID_METHOD_ON_SERVER_SICO2053", null, null)
                        );

        FFDCFilter.processException(e, CLASS_NAME + ".mfpHandshakeExchange",
                                    CommsConstants.SERVERSIDECONNECTION_REQUESTMFPSCHEMATA_01, this);
        throw e;
    }

    /**
     * Locates a JS messaging engine instance using the ME name supplied as an argument.
     * 
     * @param meName The name of the ME to attempt to retrieve a JSMessagingEngine class for.
     * @return JsMessagingEngine The JsMessagingEngine class (if successful) or null (if unsuccessful).
     */
    private JsMessagingEngine findJsMessagingEngineByName(String meName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findJsMessagingEngineByName", meName);

        JsMessagingEngine result = null;
        JsAdminService adminService = null;
        Enumeration vEnum = null;

        adminService = CommsServerServiceFacade.getJsAdminService();
        if (adminService != null)
            vEnum = adminService.listMessagingEngines();

        while (vEnum != null && vEnum.hasMoreElements() && result == null)
        {
            final JsMessagingEngine me = (JsMessagingEngine) vEnum.nextElement();

            if (me.getName().equals(meName))
                result = me;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findJsMessagingEngineByName", result);
        return result;
    }

    /**
     * Emits a runtime event notification, depending on the notification type supplied as an argument.
     */
    private void emitNotification(int notificationType)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "emitNotification", "" + notificationType);

        if (conn != null)
        {
            String meName = conn.getMeName();
            JsMessagingEngine engine = findJsMessagingEngineByName(meName);
            if (engine != null && engine.isEventNotificationEnabled())
            {
                try
                {
                    Properties properties = new Properties();
                    String notification = null;
                    String nlsText = null;
                    if (connectionUserId == null)
                        connectionUserId = conn.getResolvedUserid();
                    properties.put(SibNotificationConstants.KEY_CLIENT_USERID, connectionUserId);

                    switch (notificationType)
                    {
                        case (NOTIFICATION_TYPE_CLIENT_CONNECT):
                            notification = SibNotificationConstants.TYPE_SIB_CLIENT_CONNECTION_START;
                            properties.put(SibNotificationConstants.KEY_FAP_TYPE, SibNotificationConstants.FAP_TYPE_JFAP);
                            InetAddress inetAddress = getConversation().getMetaData().getRemoteAddress();
                            String addressStr;
                            if (inetAddress == null)
                                addressStr = "0.0.0.0";
                            else
                                addressStr = inetAddress.getHostAddress();
                            nlsText = nls.getFormattedMessage("CLIENT_CONNECTED_SICO8015", new Object[] { connectionUserId, addressStr }, null);
                            properties.put(SibNotificationConstants.KEY_COMMUNICATIONS_ADDRESS, addressStr);
                            break;
                        case (NOTIFICATION_TYPE_CLIENT_DISCONNECT):
                            notification = SibNotificationConstants.TYPE_SIB_CLIENT_CONNECTION_STOP;
                            nlsText = nls.getFormattedMessage("CLIENT_DISCONNECTED_SICO8016", new Object[] { connectionUserId }, null);
                            properties.put(SibNotificationConstants.KEY_STOP_REASON, SibNotificationConstants.STOP_REASON_CLIENT_SHUTDOWN); //D261781
                            break;
                        case (NOTIFICATION_TYPE_CLIENT_FAILURE):
                            notification = SibNotificationConstants.TYPE_SIB_CLIENT_CONNECTION_STOP;
                            nlsText = nls.getFormattedMessage("CLIENT_DISCONNECTED_SICO8016", new Object[] { connectionUserId }, null);
                            properties.put(SibNotificationConstants.KEY_STOP_REASON, SibNotificationConstants.STOP_REASON_COMMUNICATIONS_FAILURE);
                            break;
                        default:
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "unknown notification type");
                            throw new SIErrorException("Unknown notification type");
                    }

                    RuntimeEventListener eventListener =
                                    ((JsEngineComponentWithEventListener) engine.getMessageProcessor()).getRuntimeEventListener();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Issuing event notification on engine: " + engine + "\n" +
                                              "notification: " + notification + "\nproperties: " + properties);

                    eventListener.runtimeEventOccurred(engine,
                                                       notification,
                                                       nlsText,
                                                       properties);
                } catch (Exception e)
                {
                    FFDCFilter.processException(e, CLASS_NAME + ".emitNotification",
                                                CommsConstants.SERVERSIDECONNECTION_EMITNOTIFICATION_01, this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exception(this, tc, e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "emitNotification");
    }

    /**
     * setSchemaSet
     * Sets the schemaSet in the underlying Connection.
     * 
     * @param schemaSet The SchemaSet which pertains to the Connection.
     */
    @Override
    public void setSchemaSet(ConnectionSchemaSet schemaSet)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSchemaSet", schemaSet);
        getConversation().setSchemaSet(schemaSet);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSchemaSet");
    }

    /**
     * getSchemaSet
     * Returns the MFP SchemaSet which pertains to the underlying Connection.
     * 
     * @throws SIConnectionDroppedException Thrown if the underlying connection is closed.
     * 
     * @return ConnectionSchemaSet The SchemaSet belonging to the underlying Connection.
     */
    @Override
    public ConnectionSchemaSet getSchemaSet() throws SIConnectionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getSchemaSet");
        ConnectionSchemaSet result = getConversation().getSchemaSet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getSchemaSet", result);
        return result;
    }
}
