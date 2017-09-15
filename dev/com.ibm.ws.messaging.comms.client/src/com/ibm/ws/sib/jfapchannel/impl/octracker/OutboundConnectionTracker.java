/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl.octracker;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Keeps track of established outbound connections. The tracker class exists
 * to facilitate managing multiplexing multiple conversations over a smaller
 * set of connections.
 * <p>
 * The tracker is backed by the IdleConnectionPool class which acts as a repository
 * for connections not currently in use. When a new conversation is requested, we may
 * query this pool to see if it already contains a suitable connection. When we finish
 * using a connection it may be returned to the idle pool.
 * <p>
 * Despite the name, the connection tracker actually tracks groups of connection, rather
 * than dealing in individual connections. This is because, depending on the number of
 * conversations we want to multiplex over a connection, we may potentially need several
 * connections to the same remote host.
 * 
 * @see com.ibm.ws.sib.jfapchannel.impl.octracker.ConnectionDataGroup
 * @see com.ibm.ws.sib.jfapchannel.impl.octracker.IdleConnectionPool
 */
public class OutboundConnectionTracker
{
    private static final TraceComponent tc = SibTr.register(OutboundConnectionTracker.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/octracker/OutboundConnectionTracker.java, SIB.comms, WASX.SIB, uu1215.01 1.39");
    }

    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE); // D226223

    // Number of (non-cloned) conversations that can share the same
    // the same underlying connection.
    private int conversationsPerConnection;

    // Maps end point descriptors to connection groups.  This is the main data-structure
    // backing the connection tracker.  It allows us to get a connection group from a
    // description of the machine we want to connect to.
    private final HashMap<EndPointDescriptor, ConnectionDataGroup> endPointToGroupMap =
                    new HashMap<EndPointDescriptor, ConnectionDataGroup>();

    // Reference to the JFap transport framework - used when establishing connections using the
    // overloaded form of "connect" which accepts a host, port and chain name.
    private Framework framework;

    private final BridgeServiceEndPointDescriptor bridgeServiceDescriptor = new BridgeServiceEndPointDescriptor(); // D259087

    private final LinkedList<OutboundConnection> closeList = new LinkedList<OutboundConnection>();

    /**
     * End point descriptor for CFEndPoint style of endpoints. This is
     * a representation of something we want to connect to.
     */
    private class CFEndPointDescriptor extends EndPointDescriptor
    {
        private final Object endPoint;
        private final Conversation.ConversationType convType;

        protected CFEndPointDescriptor(Object cfEndPoint, Conversation.ConversationType convType)
        {
            this.endPoint = cfEndPoint;
            this.convType = convType;
        }

        @Override
        public boolean equals(Object o)
        {
            // CFW CFEndPoints do not implement equals() * sigh *. As such we have to do it ourselves
            // but this cannot be done here as we are in framework agnostic land. We must therefore
            // delegate to the current framework which knows about the specific EP types and can
            // work out whether the EP's are equal
            boolean isEqual = false;
            if (o instanceof CFEndPointDescriptor)
            {
                CFEndPointDescriptor cfDescriptor = (CFEndPointDescriptor) o;
                isEqual = framework.areEndPointsEqual(cfDescriptor.endPoint, this.endPoint) &&
                          cfDescriptor.convType == this.convType;

            }

            return isEqual;
        }

        // Start D240362
        @Override
        public int hashCode()
        {
            return framework.getEndPointHashCode(this.endPoint) ^
                   this.convType.hashCode();
        }

        @Override
        public String toString()
        {
            return "CFEndPointDescriptor@" + Integer.toHexString(System.identityHashCode(this)) +
                   ": " + endPoint + " -> " + convType;
        }
        // End D240362
    }

    /**
     * End point descriptor for host/port style of endpoints. This
     * is a representation of something we want to connect to.
     */
    private static class HostPortEndPointDescriptor extends EndPointDescriptor
    {
        private final InetSocketAddress address;
        private final String chainName;
        private final Conversation.ConversationType convType;

        protected HostPortEndPointDescriptor(InetSocketAddress address,
                                             String chainName,
                                             Conversation.ConversationType convType)
        {
            this.address = address;
            this.chainName = chainName;
            this.convType = convType;
        }

        @Override
        public boolean equals(Object o)
        {
            boolean isEqual = false;
            if (o instanceof HostPortEndPointDescriptor)
            {
                HostPortEndPointDescriptor hped = (HostPortEndPointDescriptor) o;
                isEqual = (hped.address.equals(address)) &&
                          (hped.chainName.equals(chainName)) &&
                          (hped.convType == convType);
            }
            return isEqual;
        }

        @Override
        public int hashCode()
        {
            return address.hashCode() ^ chainName.hashCode() ^ convType.hashCode();
        }

        @Override
        public String toString()
        {
            return "HostPortEndPointDescriptor@" + Integer.toHexString(System.identityHashCode(this)) +
                   ": " + address + ":" + chainName + " -> " + convType;
        }
    }

    // begin F244595
    private static class BridgeServiceEndPointDescriptor extends EndPointDescriptor
    {}

    // end F244595

    /**
     * Creates a new instance of the outbound connection tracker.
     * 
     * @param channelFramework A reference to the channel framework.
     */
    public OutboundConnectionTracker(Framework channelFramework) // F196678.10
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", channelFramework);

        this.framework = channelFramework;

        // Determine how many conversations we should multiplex over the same
        // connection before starting a new connection.
        conversationsPerConnection = JFapChannelConstants.DEFAULT_CONVERSATIONS_PER_CONN; // D258248
        try
        {
            conversationsPerConnection =
                            Integer.parseInt(RuntimeInfo.getProperty("com.ibm.ws.sib.jfapchannel.CONVERSATIONS_PER_CONNECTION"));
        } catch (NumberFormatException nfe)
        {
            // No FFDC code needed
        }
        if (conversationsPerConnection < 1)
            conversationsPerConnection = 1;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "conversationsPerConnection=" + conversationsPerConnection);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Gets a list of all the active conversations known about by the outbound connection tracker.
     * 
     * @return Returns a list
     */
    @SuppressWarnings("unchecked")
    public List getAllOutboundConversations()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAllOutboundConversations");

        final List<Conversation> result = new ArrayList<Conversation>();
        final HashMap<EndPointDescriptor, ConnectionDataGroup> endPointToGroupMapClone;

        //Clone endPointToGroupMap to minimize amount of time we hold its monitor.
        synchronized (endPointToGroupMap)
        {
            endPointToGroupMapClone = (HashMap<EndPointDescriptor, ConnectionDataGroup>) endPointToGroupMap.clone();
        }

        for (final Iterator groupIterator = endPointToGroupMapClone.values().iterator(); groupIterator.hasNext();)
        {
            final ConnectionDataGroup group = (ConnectionDataGroup) groupIterator.next();

            //Don't need to get the monitor of ConnectionDataGroup in order to perform this action.
            final List connectionData = group.getConnections();

            for (final Iterator connectionDataIterator = connectionData.iterator(); connectionDataIterator.hasNext();)
            {
                final ConnectionData thisConnectionData = (ConnectionData) connectionDataIterator.next();
                final Connection connection = thisConnectionData.getConnection();

                // Now get all the conversations on the link
                final Conversation[] conversationArray = connection.getConversations();
                for (Conversation conv : conversationArray)
                {
                    if (conv != null)
                        result.add(conv);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getAllOutboundConversations", result);
        return result;
    }

    /**
     * Gets a dirty list of all the active conversations known about by the outbound connection tracker without obtaining any locks.
     * 
     * @return Returns a list, or null if we weren't able to create the list
     */
    @SuppressWarnings("unchecked")
    public List getAllOutboundConversationsForFfdc()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAllOutboundConversationsForFfdc");

        final List<Conversation> result = new ArrayList<Conversation>();
        HashMap<EndPointDescriptor, ConnectionDataGroup> endPointToGroupMapClone = null;

        // Clone endPointToGroupMap. This might fail with an exception (such as ConcurrentModificationException) so we'll
        // try a few times before we give up. This is really just our best effort to get a dirty read.
        int cloneRetryCount = 0;
        while ((endPointToGroupMapClone == null) && (cloneRetryCount < 3)) {
            try {
                endPointToGroupMapClone = (HashMap<EndPointDescriptor, ConnectionDataGroup>) endPointToGroupMap.clone();
            } catch (Exception e) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.exception(this, tc, e);
            }
            cloneRetryCount++;
        }
        if (endPointToGroupMapClone == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getAllOutboundConversationsForFfdc", null);
            return null;
        }

        for (final Iterator groupIterator = endPointToGroupMapClone.values().iterator(); groupIterator.hasNext();)
        {
            final ConnectionDataGroup group = (ConnectionDataGroup) groupIterator.next();

            //Don't need to get the monitor of ConnectionDataGroup in order to perform this action.
            final List connectionData = group.getConnections();

            for (final Iterator connectionDataIterator = connectionData.iterator(); connectionDataIterator.hasNext();)
            {
                final ConnectionData thisConnectionData = (ConnectionData) connectionDataIterator.next();
                final Connection connection = thisConnectionData.getConnection();

                // Now get all the conversations on the link
                final Conversation[] conversationArray = connection.getConversations();
                for (Conversation conv : conversationArray)
                {
                    if (conv != null)
                        result.add(conv);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getAllOutboundConversationsForFfdc", result);
        return result;
    }

    /**
     * Closes a conversation for the specified connection. It is unnecessary to
     * require a reference to the conversation being closed as by the time this method
     * is invoked, the conversation has been marked "closed" and all that remains is the
     * book keeping. This method notifies the appropriate connection group that a
     * conversation has closed, and if necessary, removes the group from the map of those
     * tracked.
     * 
     * @param connectionHostingConversation The connection which is hosting the conversation
     *            we are closing.
     */
    public void closeConversation(OutboundConnection connectionHostingConversation)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "closeConversation", connectionHostingConversation);

        // To avoid deadlock - keep a list of pending closes and only allow one thread
        // to process this list.
        boolean closeOnThisThread = false;
        synchronized (closeList)
        {
            closeOnThisThread = closeList.isEmpty();
            closeList.addLast(connectionHostingConversation);
            if (closeOnThisThread)
                connectionHostingConversation = closeList.getFirst();
        }

        while (closeOnThisThread)
        {
            ConnectionData connectionData = connectionHostingConversation.getConnectionData();
            if (connectionData != null)
            {
                EndPointDescriptor endPointDescriptior = connectionData.getEndPointDescriptor();
                ConnectionDataGroup group;

                // Take the endpoint to group map's monitor to prevent anyone else from getting
                // ahold of and using this group whilst we perform an update.
                synchronized (endPointToGroupMap)
                {
                    // Find the group.
                    group = endPointToGroupMap.get(endPointDescriptior);
                    if ((group == null) || (group != connectionData.getConnectionDataGroup()))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            if (group == null)
                                SibTr.debug(this, tc, "group == nul");
                            else
                                SibTr.debug(this, tc, "group != connectionData.getConnectionDataGroup()");
                        }

                        // Record an FFDC, but then ignore this close request (and continue with the next one)
                        // NOTE: If we don't continue NO close will ever be processed
                        Exception e = new SIErrorException(nls.getFormattedMessage("OUTCONNTRACKER_INTERNAL_SICJ0064", null, "OUTCONNTRACKER_INTERNAL_SICJ0064")); // D226223
                        FFDCFilter.processException(e, "com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker",
                                                    JFapChannelConstants.OUTBOUNDCONNTRACKER_CLOSECONV_01, connectionData);
                    }
                    else
                    {
                        // Notify the group that a conversation using the specified connection
                        // has closed.
                        group.close(connectionHostingConversation);

                        // If the connection group has become empty, remove it from out map.
                        if (group.isEmpty())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "group: " + group + " has become empty");
                            endPointToGroupMap.remove(group.getEndPointDescriptor());
                        }
                    }
                }
            }

            synchronized (closeList)
            {
                closeList.removeFirst();
                closeOnThisThread = !closeList.isEmpty();
                if (closeOnThisThread)
                {
                    connectionHostingConversation = closeList.getFirst();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "close list has an entry: " + connectionHostingConversation);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "closeConversation");
    }

    public void removeConnectionDataFromGroup(OutboundConnection oc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeConnectionDataFromGroup", new Object[] { oc });
        ConnectionData connectionData = oc.getConnectionData();

        if (connectionData != null)
        {
            ConnectionDataGroup cdGroup = connectionData.getConnectionDataGroup();
            cdGroup.removeConnectionDataFromGroup(connectionData);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeConnectionDataFromGroup");
    }

    /**
     * Attempts to establish a new conversation with the specified remote machine.
     * This method attempts to find or create a suitable connection group for
     * establishing the conversation. Once it has a suitable group, it will
     * attempt to create a new conversation using that group.
     * 
     * @param endPoint The channel framework endpoint for the remote host we will attempt
     *            to establish a new conversation with.
     * @param receiveListener The receive listener to use for the new conversation.
     * @param conversationType the type of the conversation being established.
     * @return Conversation The new conversation created.
     * @throws SICommsException Thrown if something goes wrong whilst attempting to establish
     *             the new conversation.
     */
    public Conversation connect(Object endPoint,
                                ConversationReceiveListener receiveListener,
                                Conversation.ConversationType conversationType)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { endPoint, receiveListener, conversationType });
        CFEndPointDescriptor testEndPoint = new CFEndPointDescriptor(endPoint, conversationType);
        ConnectionDataGroup connGroup;

        // Take the monitor of the end point to group map.  This allows us to select or
        // create a new group, safe in the knowledge that no-one else can add a duplicate
        // or remove the group we are interested in.
        synchronized (endPointToGroupMap)
        {
            // See if there is already a connection group for the end point we are interested
            // in, if not create one and add it to the map.
            connGroup = endPointToGroupMap.get(testEndPoint);

            // Contemplate issuing a warning if the endpoint is SSL enabled, we are running in
            // the client container, and the SSL properties file is not available.
            framework.warnIfSSLAndPropertiesFileMissing(endPoint);

            // Determine the heartbeat interval and timeout from our configuration.
            Map properties = framework.getOutboundConnectionProperties(endPoint);
            int heartbeatInterval = determineHeartbeatInterval(properties);
            int heartbeatTimeout = determineHeartbeatTimeout(properties);

            if (connGroup == null)
            {
                connGroup = new ConnectionDataGroup(this,
                                testEndPoint,
                                conversationsPerConnection,
                                framework,
                                heartbeatInterval,
                                heartbeatTimeout);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "created new connection data group: " + connGroup);
                endPointToGroupMap.put(testEndPoint, connGroup);
            }

            // Notify the connection group of our intention to make a connection at some point in
            // the future.  This mechanism is used to prevent an "empty" group from being
            // closed in the interval between us releasing the lock on the endpoint->group map
            // and actually making the connect attempt.  The motivation behind this is to avoid
            // holding the endpoint->group map monitor for the (potentially very long) duration
            // of an connect attempt.
            connGroup.connectionPending();
        }

        // Attempt to establish a new conversation.
        Conversation retConversation = connGroup.connect(endPoint,
                                                         receiveListener,
                                                         conversationType);

        if (retConversation != null)
            retConversation.setConversationType(conversationType);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retConversation);
        return retConversation;
    }

    /**
     * Attempts to establish a new conversation with the specified remote machine.
     * This method attempts to find or create a suitable connection group for
     * establishing the conversation. Once it has a suitable group, it will
     * attempt to create a new conversation using that group.
     * 
     * @param remoteHost The address of the remote host to attempt to establish a new
     *            conversation with
     * @param receiveListener The receive listener to use for the new conversation.
     * @param chainName The name of the chain to use when establishing the remote connection.
     * @param conversationType the type of the conversation being established.
     * @return Conversation The new conversation created.
     * @throws SICommsException Thrown if something goes wrong whilst attempting to establish
     *             the new conversation.
     */
    public Conversation connect(InetSocketAddress remoteHost,
                                ConversationReceiveListener receiveListener,
                                String chainName,
                                Conversation.ConversationType conversationType)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { remoteHost, receiveListener, chainName, conversationType });

        EndPointDescriptor testEndPoint = new HostPortEndPointDescriptor(remoteHost, chainName, conversationType);
        ConnectionDataGroup connGroup;

        // Take the monitor of the end point to group map.  This allows us to select or
        // create a new group, safe in the knowledge that no-one else can add a duplicate
        // or remove the group we are interested in.
        synchronized (endPointToGroupMap)
        {
            // See if there is already a connection group for the end point we are interested
            // in, if not create one and add it to the map.
            connGroup = endPointToGroupMap.get(testEndPoint);

            // Contemplate issuing a warning if the chain is SSL enabled, we are running in
            // the client container, and the SSL properties file is not available.
            framework.warnIfSSLAndPropertiesFileMissing(chainName);

            if (connGroup == null)
            {
                NetworkConnectionFactory virtualConnectionFactory;
                try
                {
                    NetworkTransportFactory transportFactory = framework.getNetworkTransportFactory();
                    virtualConnectionFactory = transportFactory.getOutboundNetworkConnectionFactoryByName(chainName);

                    if (virtualConnectionFactory == null)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unknown chain name: " + chainName);
                        throw new SIErrorException(nls.getFormattedMessage("OUTCONNTRACKER_INTERNAL_SICJ0064", null, "OUTCONNTRACKER_INTERNAL_SICJ0064")); // D226223
                    }

                    // How often should we heartbeat?
                    // Currently in liberty we dont have an option to configure heartbeat interval and timout 
                    // Hence hard coding to default
                    int heartbeatInterval = JFapChannelConstants.DEFAULT_HEARTBEAT_INTERVAL;
                    int heartbeatTimeout = JFapChannelConstants.DEFAULT_HEARTBEAT_TIMEOUT;

                    connGroup = new ConnectionDataGroup(this,
                                    virtualConnectionFactory,
                                    testEndPoint,
                                    conversationsPerConnection,
                                    heartbeatInterval,
                                    heartbeatTimeout);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "created new connection data group: " + connGroup);
                    endPointToGroupMap.put(testEndPoint, connGroup);
                } catch (FrameworkException frameworkException)
                {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.exception(this, tc, frameworkException);
                    throw new SIErrorException(nls.getFormattedMessage("OUTCONNTRACKER_INTERNAL_SICJ0064", null, "OUTCONNTRACKER_INTERNAL_SICJ0064"), frameworkException); // D226223
                }
            }

            // Notify the connection group of our intention to make a connection at some point in
            // the future.  This mechanism is used to prevent an "empty" group from being
            // closed in the interval between us releasing the lock on the endpoint->group map
            // and actually making the connect attempt.  The motivation behind this is to avoid
            // holding the endpoint->group map monitor for the (potentially very long) duration
            // of an connect attempt.
            connGroup.connectionPending();
        }

        // Attempt to establish a new conversation.
        Conversation retConversation = connGroup.connect(remoteHost,
                                                         chainName,
                                                         receiveListener,
                                                         conversationType);

        if (retConversation != null)
            retConversation.setConversationType(conversationType);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retConversation);
        return retConversation;
    }

    // begin F244595, D259087
    public Conversation connect(final ConversationReceiveListener receiveListener, final Conversation.ConversationType conversationType, final ConversationUsageType usageType) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { receiveListener, conversationType, usageType });

        final EndPointDescriptor testEndPoint = bridgeServiceDescriptor;

        ConnectionDataGroup connGroup;
        String chainName = JFapChannelConstants.CHAIN_NAME_TCPPROXYBRIDGESERVICE_OUTBOUND;

        // Take the monitor of the end point to group map.  This allows us to select or
        // create a new group, safe in the knowledge that no-one else can add a duplicate
        // or remove the group we are interested in.
        synchronized (endPointToGroupMap)
        {
            // See if there is already a connection group for the end point we are interested
            // in, if not create one and add it to the map.
            connGroup = endPointToGroupMap.get(testEndPoint);
            if (connGroup == null)
            {
                NetworkConnectionFactory virtualConnectionFactory;
                try
                {
                    NetworkTransportFactory transportFactory = framework.getNetworkTransportFactory();
                    virtualConnectionFactory = transportFactory.getOutboundNetworkConnectionFactoryByName(chainName);

                    if (virtualConnectionFactory == null)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Unknown chain name: " + chainName);
                        throw new SIErrorException(nls.getFormattedMessage("OUTCONNTRACKER_INTERNAL_SICJ0064", null, "OUTCONNTRACKER_INTERNAL_SICJ0064")); // D226223
                    }

                    connGroup = new ConnectionDataGroup(this,
                                    virtualConnectionFactory,
                                    testEndPoint,
                                    framework);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "created new connection data group: " + connGroup);
                    endPointToGroupMap.put(testEndPoint, connGroup);
                } catch (FrameworkException frameworkException)
                {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.exception(this, tc, frameworkException);
                    throw new SIErrorException(nls.getFormattedMessage("OUTCONNTRACKER_INTERNAL_SICJ0064", null, "OUTCONNTRACKER_INTERNAL_SICJ0064"), frameworkException); // D226223
                }
            }

            // Notify the connection group of our intention to make a connection at some point in
            // the future.  This mechanism is used to prevent an "empty" group from being
            // closed in the interval between us releasing the lock on the endpoint->group map
            // and actually making the connect attempt.  The motivation behind this is to avoid
            // holding the endpoint->group map monitor for the (potentially very long) duration
            // of an connect attempt.
            connGroup.connectionPending();
        }

        // Attempt to establish a new conversation.
        Conversation retConversation = connGroup.connect(receiveListener, usageType);

        if (retConversation != null)
            retConversation.setConversationType(conversationType);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retConversation);
        return retConversation;
    }

    // end F244595, D259087

    // begin F196678.10
    private int determineHeartbeatInterval(Map properties)
    {
        // How often should we heartbeat?
        int heartbeatInterval = JFapChannelConstants.DEFAULT_HEARTBEAT_INTERVAL;
        try
        {
            heartbeatInterval = Integer.parseInt(RuntimeInfo.getProperty(JFapChannelConstants.RUNTIMEINFO_KEY_HEARTBEAT_INTERVAL, "" + heartbeatInterval));
        } catch (NumberFormatException nfe)
        {
            // No FFDC code needed
        }

        if (properties != null)
        {
            String intervalStr = (String) properties.get(JFapChannelConstants.CHANNEL_CONFIG_HEARTBEAT_INTERVAL_PROPERTY);
            if (intervalStr != null)
            {
                try
                {
                    heartbeatInterval = Integer.parseInt(intervalStr);
                } catch (NumberFormatException nfe)
                {
                    // No FFDC code needed
                }
            }
        }
        return heartbeatInterval;
    }

    // end F196678.10

    // begin F196678.10
    private int determineHeartbeatTimeout(Map properties)
    {
        // How often should we heartbeat?
        int heartbeatTimeout = JFapChannelConstants.DEFAULT_HEARTBEAT_TIMEOUT;
        try
        {
            heartbeatTimeout = Integer.parseInt(RuntimeInfo.getProperty(JFapChannelConstants.RUNTIMEINFO_KEY_HEARTBEAT_TIMEOUT, "" + heartbeatTimeout));
        } catch (NumberFormatException nfe)
        {
            // No FFDC code needed
        }

        if (properties != null)
        {
            String timeoutStr = (String) properties.get(JFapChannelConstants.CHANNEL_CONFIG_HEARTBEAT_TIMEOUT_PROPERTY);
            if (timeoutStr != null)
            {
                try
                {
                    heartbeatTimeout = Integer.parseInt(timeoutStr);
                } catch (NumberFormatException nfe)
                {
                    // No FFDC code needed
                }
            }
        }
        return heartbeatTimeout;
    }

    // end F196678.10

    /**
     * Clones a conversation. The method locates the appropriate connection group and
     * invokes it's clone method.
     * 
     * @param connection The connection to clone a conversation for.
     * @param conversationReceiveListener The receive listener to use for the cloned connection
     * @return Conversation a new cloned conversation
     * @throws SICommsException Thrown if the clone operation fails.
     */
    public Conversation cloneConversation(OutboundConnection connection,
                                          ConversationReceiveListener conversationReceiveListener)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cloneConversation", new Object[] { connection, conversationReceiveListener });
        Conversation clonedConversation =
                        connection.getConnectionData().getConnectionDataGroup().clone(connection, conversationReceiveListener);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cloneConversation", clonedConversation);
        return clonedConversation;
    }

    /**
     * Purges a conneciton from the tracker. This is invoked when an error is detected on
     * a connection and we do not want any further conversations to attempt to use it.
     * 
     * @param connection The connection to purge
     * @param notifyPeer Should we send notification to the connections peer that the purge
     *            is taking place?
     */
    public void purgeFromInvalidateImpl(OutboundConnection connection, boolean notifyPeer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "purgeFromInvalidateImpl", new Object[] { connection, "" + notifyPeer });
        connection.getConnectionData().getConnectionDataGroup().purgeFromInvalidateImpl(connection, notifyPeer);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "purgeFromInvalidateImpl");
    }

    /**
     * Purges a connection from the tracker, that has been closed by the remote peer, so
     * can no longer be used.
     * 
     * @param connection The connection to purge
     */
    public void purgeClosedConnection(OutboundConnection connection)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "purgeClosedConnection", connection);
        connection.getConnectionData().getConnectionDataGroup().purgeClosedConnection(connection);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "purgeClosedConnection");
    }

    /**
     * Terminate all the connections associated with the chain
     * 
     * @param chainName
     * @throws Exception
     */
    public void terminateConnectionsAssociatedWithChain(String chainName) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "terminateConnectionsAssociatedWithChain", chainName);

        synchronized (endPointToGroupMap) {

            try {
                final HashMap<EndPointDescriptor, ConnectionDataGroup> endPointToGroupMapClone = (HashMap<EndPointDescriptor, ConnectionDataGroup>) endPointToGroupMap.clone();
                Iterator<EndPointDescriptor> it = endPointToGroupMapClone.keySet().iterator();
                while (it.hasNext()) {
                    EndPointDescriptor ed = it.next();

                    if ((((HostPortEndPointDescriptor) ed).chainName).equals(chainName)) {

                        ConnectionDataGroup cdGroup = endPointToGroupMapClone.get(ed);

                        final List connectionData = cdGroup.getConnections();

                        for (final Iterator connectionDataIterator = connectionData.iterator(); connectionDataIterator.hasNext();)
                        {
                            final ConnectionData thisConnectionData = (ConnectionData) connectionDataIterator.next();
                            final OutboundConnection oc = thisConnectionData.getConnection();
                            //close all the conservations associated with the OC
                            Conversation[] conv = oc.getConversations();
                            for (Conversation c : conv) {
                                try {
                                    c.fastClose();
                                } catch (Exception e) {//Don't let the exception mess up closing other remaining conservations
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(tc, "Error while fast closing the conversation", e);
                                }

                            }
                            try {
                                // terminate the physical connection
                                purgeFromInvalidateImpl(oc, false);
                            } catch (Exception e) {//Don't let the exception mess up closing other remaining connection
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "Error while purging the physical connection", e);
                            }
                        }

                        try {
                            // destroy the OutboundVirtualConnection associated with the chainName
                            cdGroup.getNetworkConnectionFactory().getOutboundVirtualConFactory().destroy();
                        } catch (Exception e) {//Don't let the exception mess up closing other remaining connection
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "Error while destroying the outbound virtual connection", e);
                        }
                        // in fastClose() the entry is removed but still we do a remove 
                        endPointToGroupMap.remove(ed);

                    }
                }

            } catch (Exception e) {
                throw e;
            } finally {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(this, tc, "terminateConnectionsAssociatedWithChain");
            }

        }
    }

    /**
     * @param framework the framework to set
     */
    public void setChanelFramework(Framework framework) {
        this.framework = framework;
    }
}
