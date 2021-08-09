/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.impl.octracker.ConnectionData;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An object which represents the client side of a connection (socket)
 */
public class OutboundConnection extends Connection
{
    private static final TraceComponent tc = SibTr.register(OutboundConnection.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

    //@start_class_string_prolog@
    public static final String $sccsid = "@(#) 1.44 SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/OutboundConnection.java, SIB.comms, WASX.SIB, uu1215.01 11/09/22 03:41:21 [4/12/12 22:14:14]";
    //@end_class_string_prolog@

    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Source Info: " + $sccsid);
    }

    // Keeps track of outbound connections in use.
    private OutboundConnectionTracker tracker = null; // F171173

    // Has handshakeComplete been invoked on one of the conversations that are
    // multiplexed over this connection.
    private boolean handshakeComplete = false; // D181493

    // Monitor used to wait on and provide exclusion for handshake operations.
    // This monitor should be owned before examining the isFirst and handshakeComplete
    // attributes of this object.  Anyone waiting on this monitor will be woken exactly
    // once on the first call of handshakeComplete.
    private final Object handshakeMonitor = new Object(); // D181493

    // Information about this connection.  For use by the outboud conneciton tracker.
    private ConnectionData connectionData; // F191566

    // Is this connection currently in the process of being purged?
    private boolean beingPurged = false; // F191566

    private int handshakersWaiting = 0; // D223637

    /** Eye catcher for use in debugSummaryMessage. */
    private final String eyeCatcher;

    /**
     * Creates a new client connection
     * 
     * @throws FrameworkException if anything goes wrong while creating a new instace of OutboundConnection.
     */
    public OutboundConnection(NetworkConnectionContext connLink, // F177053
                              NetworkConnection vc,
                              OutboundConnectionTracker connTracker, // F171173
                              int heartbeatInterval, // F175658
                              int heartbeatTimeout, // F175658
                              ConnectionData connectionData) throws FrameworkException
    {
        super(connLink, vc, heartbeatInterval, heartbeatTimeout); // F174772, F175658

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>",
                        new Object[] { connLink, vc, connTracker, "" + heartbeatInterval, "" + heartbeatTimeout });
        this.connectionData = connectionData; // F191566
        tracker = connTracker;

        // begin D224570
        // Make description "OUT localhost:port->remotehost:port"
        // using dotted decimal host addresses.
        description = "OUT " + tcpCtx.getLocalAddress().getHostAddress() + ":"
                      + tcpCtx.getLocalPort() + "->" + tcpCtx.getRemoteAddress().getHostAddress() + ":"
                      + tcpCtx.getRemotePort();

        //Generate eye catcher for use with debugSummaryMessage.
        //This is always of the format: client ip address:client port:server ip address:server port
        eyeCatcher = tcpCtx.getLocalAddress().getHostAddress() + ":" + tcpCtx.getLocalPort() + ":" + tcpCtx.getRemoteAddress().getHostAddress() + ":" + tcpCtx.getRemotePort();

        remoteHostAddress = tcpCtx.getRemoteAddress().getHostAddress(); // D226223
        chainName = getMetaData().getChainName(); // D226223

        //@stoptracescan@
        if (TraceComponent.isAnyTracingEnabled())
            JFapUtils.debugSummaryMessage(tc, this, null, "New outbound connection established");
        //@starttracescan@
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Does the work required to start up a new conversation on this connection.
     * 
     * @return Conversation The conversation started.
     */
    // begin 181493
    public ConversationImpl startNewConversation(ConversationReceiveListener defaultReceiveListener, boolean handshakeRequired)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "startNewClientConversation", new Object[] { defaultReceiveListener, Boolean.valueOf(handshakeRequired) });

        if (handshakeRequired)
        {
            // begin D181493
            // Obtain ownership of the handshakeMonitor to synchronize access to the isFirst
            // and handshakeComplete attributes.  We may also need to wait on this.
            synchronized (handshakeMonitor)
            {
                if (!handshakeComplete)
                {
                    if (handshakersWaiting == 0)
                    {
                        // If this is the first conversation - then simply drop through without
                        // worrying about handshakes.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "first conversation on connection");
                        ++handshakersWaiting;
                    }
                    else
                    {
                        // We entered here because we are not the first conversation using this connection
                        // yet no-one has called handshake complete on the first conversation.  We must
                        // wait until handshaking is complete before we can proceed.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "waiting for handshaking to be complete");
                        boolean interrupted = false;
                        do
                        {
                            // Wait on the handshake monitor ignoring interruptions.
                            ++handshakersWaiting;
                            interrupted = false;
                            try
                            {
                                handshakeMonitor.wait();
                            } catch (InterruptedException e)
                            {
                                // No FFDC code needed
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(this, tc, "interrupted whilst waiting for handshaking");
                                interrupted = true;
                            }
                        } while (interrupted);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "finsihed waiting for handshaking");
                    }
                }
            }
        }

        // Obtain a conversation ID to use.  We do this by reserving a conversation ID in the
        // ID table - then creating a conversation with the reserved ID.  The call to
        // startNewConversationGeneric then adds the conversation into the table in the slot
        // belonging to the reserved ID.
        int conversationId = 0;
        try
        {
            conversationId = conversationTable.reserveId();
        } catch (IdTableFullException e)
        {
            // No FFDC code needed
            throw new SIResourceException(TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "OUTBOUNDCONN_IDTABLEFULL_SICJ0055", null,
                                                                       "OUTBOUNDCONN_IDTABLEFULL_SICJ0055"), e); // D226223
        }
        // end D181493

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "connection table allocated conversation id: " + conversationId);
        ConversationImpl c =
                        new ConversationImpl((short) conversationId, !handshakeComplete, this, defaultReceiveListener); // D221433
        ConversationImpl retValue = startNewConversationGeneric(c, true, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "startNewClientConversation", retValue);
        return retValue;
    }

    // end 181493

    /**
     * Notification that we receive when one of our conversations closes.
     * 
     * @see com.ibm.ws.sib.jfapchannel.impl.Connection#closeNotification(com.ibm.ws.sib.jfapchannel.Conversation)
     */
    @Override
    public void closeNotification(Conversation c)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "closeNotification", c);

        // Pass this to the connection tracker as this keeps a reference count of the number
        // of open conversations and may wish to close the underlying socket.
        tracker.closeConversation(this); // F171173

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "closeNotification");
    }

    /** @see com.ibm.ws.sib.jfapchannel.impl.Connection#invalidateImpl(boolean, Throwable) */
    // begin F176003, D179183
    @Override
    public void invalidateImpl(boolean notifyPeer, Throwable throwable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "invalidateImpl", new Object[] { "" + notifyPeer, throwable });

        if ((throwable != null) && TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            SibTr.exception(tc, throwable);

        JFapConnectionBrokenException exception = new JFapConnectionBrokenException(
                        TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE,
                                                     "OUTBOUNDCONV_INVALIDATE_SICJ0056",
                                                     new Object[] { remoteHostAddress, "" + getMetaData().getRemotePort(), chainName },
                                                     null)
                        );

        if (throwable != null)
            exception.initCause(throwable);
        wakeupAllConversationsWithException(exception, true);
        tracker.purgeFromInvalidateImpl(this, notifyPeer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "invalidateImpl");
    }

    // end F176003, D179183

    /**
     * When an outbound connection is closed by the remote host, we need to purge the
     * connection from the outbound connection tracker.
     */
    @Override
    protected void connectionClosedByPeer()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connectionClosedByPeer");
        tracker.purgeClosedConnection(this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connectionClosedByPeer");
    }

    // begin D181493
    /**
     * Invoked when a conversation associated with this connection invokes it's handshake
     * complete method. This code wakes up anyone waiting on the handshake monitor. This
     * releases any threads blocked inside the startNewConversation method which are
     * waiting for the first conversation to complete its handshaking.
     */
    @Override
    protected void handshakeComplete()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "handshakeComplete");
        synchronized (handshakeMonitor)
        {
            if (!handshakeComplete)
            {
                handshakeComplete = true;
                handshakersWaiting = 0; // D223637
                handshakeMonitor.notifyAll();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "handshakeComplete");
    }

    // end D181493

    // begin D221433
    /** @see com.ibm.ws.sib.jfapchannel.impl.Connection#handshakeFailed() */
    @Override
    protected void handshakeFailed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "handshakeFailed");

        // invalidate now to avoid a deadlock when waiters deadlock us when
        // we try to do logical close
        invalidate(true, null, null);
        //remove the connection data since the handshake has failed
        tracker.removeConnectionDataFromGroup(this);
        synchronized (handshakeMonitor)
        {
            if (!handshakeComplete)
            {
                handshakersWaiting = 0; // D223637
                handshakeMonitor.notifyAll();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "handshakeFailed");
    }

    // end D22143

    // Start D212672
    /**
     * This method is used to set the connection data being used for this outbound connection.
     * The connection data can change when a socket is pooled and then retrieved and re-used.
     * 
     * @param connectionData
     */
    public void setConnectionData(ConnectionData connectionData)
    {
        this.connectionData = connectionData;
    }

    // End D212672

    // begin F191566
    /**
     * Returns the connection data for this connection. This is used by the outbound
     * connection tracking code to manage the connection.
     */
    public ConnectionData getConnectionData()
    {
        return connectionData;
    }

    // end F191566

    // begin F191566
    /**
     * Returns true if the connection is currently being purged from the connection
     * tracker.
     */
    public boolean isBeingPurged()
    {
        return beingPurged;
    }

    // end F191566

    // begin F191566
    /**
     * Used to mark the connection as being purged from the connection tracker.
     */
    public void beingPurged()
    {
        beingPurged = true;
    }

    // end F191566

    // begin F191566
    /**
     * Clones a conversation on this connection.
     * 
     * @param receiveListener The receive listener to use for the cloned connection.
     * @return Conversation the cloned conversation.
     * @throws SICommsException Thrown if the clone operation fails.
     */
    @Override
    protected Conversation cloneConversation(ConversationReceiveListener receiveListener)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cloneConnection", receiveListener);
        Conversation returnConversation;
        returnConversation = tracker.cloneConversation(this, receiveListener);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cloneConnection", returnConversation);
        return returnConversation;
    }

    // end F191566

    // begin D196678.10.1
    /** @see com.ibm.ws.sib.jfapchannel.impl.Connection#getMetaData() */
    @Override
    protected ConversationMetaData getMetaData()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMetaData");
        ConversationMetaData retValue = connChannel.getMetaData();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMetaData", retValue);
        return retValue;
    }

    // end D196678.10.1

    /**
     * @return Returns false for this type of connection.
     */
    @Override
    protected boolean isInbound()
    {
        return false;
    }

    /**
     * This method is not applicable for outbound conversations and should not be called.
     * 
     * @see com.ibm.ws.sib.jfapchannel.impl.Connection#startNewConversation(com.ibm.ws.sib.jfapchannel.impl.ConversationImpl)
     */
    @Override
    protected ConversationImpl startNewConversation(ConversationImpl conv) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "startNewConversation", conv);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "startNewConversation");
        throw new SIErrorException();
    }

    /**
     * @return Returns useful information about this connection.
     */
    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("OutboundConnection@").append(Integer.toHexString(System.identityHashCode(this)));
        buf.append(": {");
        buf.append("Remote Address: ");
        buf.append(remoteHostAddress);
        buf.append(", Chain: ");
        buf.append(chainName);
        buf.append(", Heartbeat Timeout: ");
        buf.append(getHeartbeatTimeoutForToString());
        buf.append(", Heartbeat Interval: ");
        buf.append(getHeartbeatIntervalForToString());
        buf.append(", Handshake Complete: ");
        buf.append(handshakeComplete);
        buf.append(", Handshakers Waiting: ");
        buf.append(handshakersWaiting);
        buf.append("}\nEvents follow:\n");
        buf.append(getDiagnostics(false));

        return buf.toString();
    }

    /**
     * @see Connection#getEyeCatcher()
     */
    @Override
    public String getEyeCatcher()
    {
        return eyeCatcher;
    }
}
