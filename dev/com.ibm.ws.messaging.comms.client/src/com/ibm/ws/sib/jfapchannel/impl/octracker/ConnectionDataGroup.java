/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl.octracker;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.JFapConnectFailedException;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory;
import com.ibm.ws.sib.jfapchannel.impl.JFapAddress;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.utils.Semaphore;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Groups together connection data objects by remote host. Groups are used by the
 * outbound connection tracker to multiplex conversations over several connections
 * to the same target "remote" process.
 * 
 * @see com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker
 */
public class ConnectionDataGroup
{
    private static final TraceComponent tc = SibTr.register(ConnectionDataGroup.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/octracker/ConnectionDataGroup.java, SIB.comms, WASX.SIB, uu1215.01 1.55");
    }

    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE); // D226223

    // Reference to the outbound connection tracker to which this group, ultimately, belongs
    private final OutboundConnectionTracker tracker;

    /**
     * Connection data objects which are part of this group.
     * Users of this object must always have its lock prior to using it.
     * 
     * Note that this object also guards the useCount field of any ConnectionData
     * held within the map (i.e. you must hold this object as lock before reading
     * or updating the use count of any ConnectionData object held in this list)
     */
    private final LinkedList<ConnectionData> connectionData = new LinkedList<ConnectionData>();

    // The endpoint descriptor relating to the process to which all connections
    // in this group are connected.
    private final EndPointDescriptor groupEndpointDescriptor;

    // A counter which is incremented every time we select this group as being a
    // group we want to establish a connection for, and decremented every time
    // a connection is established.  It is used to close a timing window where
    // the group might be selected to make a connection and subsequently removed
    // for being empty prior to the connection been attempted. You must hold the
    // object lock for this before referencing this field
    private int connectAttemptsPending = 0;

    // Number of conversations that we will support multiplexed over the same
    // connection.
    private final int conversationsPerConnection;

    // Reference to the virtual connection factory factory use when creating
    // non-CFEndPoint based connections.  Will not be used if this connection
    // groups endpoint descriptor is CFEndPoint based.
    private final ExistingNetworkConnectionFactoryHolder currentConnectionFactoryFactory;

    // A reference to the channel framework
    private final Framework framework;

    // begin F196678.10
    private final int heartbeatInterval;
    private final int heartbeatTimeout;
    // end F196678.10

    /**
     * Lock to ensure we handle only one connect call at a time
     */
    private final Object connectionMonitor = new Object() {};

    /* ************************************************************************ */
    /* WARNING: NOTE the lock hierarchy for the locks in this class */
    /*                                                                         */
    /* 1. connectionMonitor */
    /* 2. this */
    /* 3. connectionData */
    /*                                                                         */
    /* Once you hold a lock at a given level you MUST NOT attempt to acquire */
    /* a lock from a higher level (e.g. code like */
    /*                                                                         */
    /* synchronized(connectionData) */
    /* { */
    /* synchronized(this) */
    /* { */
    /*                                                                         */
    /* is forbidden (code it the other way around if needed)) */
    /* ************************************************************************ */

    /**
     * A NetworkConnectionFactoryHolder is a wrapper for a network connection factory
     * and allows the doConnect code not to care WHERE the network connection factory
     * actually comes from.
     */
    private static interface NetworkConnectionFactoryHolder
    {
        /**
         * @return NetworkConnectionFactory the NetworkConnectionFactory held by this holder
         * @throws FrameworkException is thrown if the NetworkConnectionFactory needed to obtained from a framework and the framework failed
         * @throws JFapConnectFailedException if no NetworkConnectionFactory could be obtained.
         */
        NetworkConnectionFactory getFactory() throws FrameworkException, JFapConnectFailedException;
    }

    /**
     * An ExistingNetworkConnectionFactoryHolder simply holds a network connection
     * factory that already exists, or if the wrapped network connection factory is
     * null, used when the ConnectionDataGroup must be provided with the details
     * of the network connection factory to be used on the connect call
     */
    private static class ExistingNetworkConnectionFactoryHolder implements NetworkConnectionFactoryHolder
    {
        private final NetworkConnectionFactory outboundVirtualConnectionFactory;

        /**
         * Construct a new ExistingNetworkConnectionFactoryHolder.
         * 
         * @param vcf The existing factory to be wrapped (null if no factory is to
         *            be held by this holder)
         */
        public ExistingNetworkConnectionFactoryHolder(NetworkConnectionFactory vcf)
        {
            outboundVirtualConnectionFactory = vcf;
        }

        /**
         * @see com.ibm.ws.sib.jfapchannel.impl.octracker.ConnectionDataGroup.NetworkConnectionFactoryHolder#getFactory()
         * @return NetworkConnectionFactory held by this holder
         */
        @Override
        public NetworkConnectionFactory getFactory()
        {
            return outboundVirtualConnectionFactory;
        }

        /**
         * @return true if this holder is holding a network connection factory
         */
        public boolean isNonNull()
        {
            return (outboundVirtualConnectionFactory != null);
        }
    }

    /**
     * A CreateNewVirtualConnectionFactory is a NetworkConnectionFactoryHolder that
     * will obtain a new network connection factory when getFactory is called, based on
     * the parameters passed on construction
     */
    private class CreateNewVirtualConnectionFactory implements NetworkConnectionFactoryHolder
    {
        /** The endPoint object to which the network connection factory will connect connections */
        private Object endPoint;
        /** The end point to which the network connection factory will connect connections */
        private InetAddress epAddress;
        /** The port to which the network connection factory will connect connections */
        private int epPort;

        /**
         * Construct a new CreateNewVirtualConnectionFactory.
         * 
         * @param endPoint The end point Object to which the network connection factory will connect connections
         */
        public CreateNewVirtualConnectionFactory(Object endPoint)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "CreateNewVirtualConnectionFactory.<init>", endPoint);
            this.endPoint = endPoint;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "CreateNewVirtualConnectionFactory.<init>");
        }

        /**
         * Note this method should only be invoked once per CreateNewVirtualConnectionFactory. This is not policed.
         * 
         * @see com.ibm.ws.sib.jfapchannel.impl.octracker.ConnectionDataGroup.NetworkConnectionFactoryHolder#getFactory()
         * @return NetworkConnectionFactory the network connection factory
         * @throws FrameworkException is thrown if the framework encounters a problem creating the network connection factory
         * @throws JFapConnectFailedException is thrown if the framework fails to create a network connection factory
         */
        @Override
        public NetworkConnectionFactory getFactory() throws FrameworkException, JFapConnectFailedException
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "CreateNewVirtualConnectionFactory.getFactory");

            // First prepare the endpoint and save the address
            this.endPoint = framework.prepareOutboundConnection(endPoint);
            epAddress = framework.getHostAddress(endPoint);
            epPort = framework.getHostPort(endPoint);

            NetworkTransportFactory transportFactory = framework.getNetworkTransportFactory();
            NetworkConnectionFactory vcFactory = transportFactory.getOutboundNetworkConnectionFactoryFromEndPoint(endPoint);

            if (vcFactory == null)
            {
                JFapConnectFailedException e = new JFapConnectFailedException(
                                nls.getFormattedMessage("CONNDATAGROUP_CONNFAILED_SICJ0063",
                                                        new Object[]
                                                        {
                                                         epAddress.getHostAddress(),
                                                         "" + epPort
                                                        },
                                                        "CONNDATAGROUP_CONNFAILED_SICJ0063")
                                );
                FFDCFilter.processException(e,
                                            "com.ibm.ws.sib.jfapchannel.impl.octracker.ConnectionDataGroup",
                                            JFapChannelConstants.CONNDATAGROUP_CONNECT_07,
                                            endPoint);
                throw e;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "CreateNewVirtualConnectionFactory.getFactory", vcFactory);
            return vcFactory;
        }

        /**
         * @return InetAddress the endpoint address used when the factory was obtained. Undefined if
         *         getFactory has not yet been called
         */
        public InetAddress getEndPointAddress()
        {
            return epAddress;
        }

        /**
         * @return int the endpoint port used when the factory was obtained. Undefined if
         *         getFactory has not yet been called
         */
        public int getEndPointPort()
        {
            return epPort;
        }
    }

    /**
     * A JFapAddressHolder is used to hold the jfap address to be used for a conversation. The address
     * can either be passed in directly, or be obtained from a CreateNewVirtualConnectionFactory (in which
     * case the getAddress method MUST be called after the getFactory method on the CreateNewVirtualConnectionFactory
     * is called)
     */
    private static class JFapAddressHolder
    {
        private CreateNewVirtualConnectionFactory factory;
        private Conversation.ConversationType conversationType;
        private InetSocketAddress inetAddress;
        private JFapAddress jFapAddress;
        private String hostAddress;

        /**
         * Flag indicating if this JFapAddressHolder is empty or not.
         */
        private final boolean empty;

        /**
         * Construct an empty JFapAddressHolder.
         * Calling getAddress() or getErrorInserts() will return null.
         */
        public JFapAddressHolder()
        {
            empty = true;
        }

        /**
         * @param factory The CreateNewVirtualConnectionFactory from which to obtain the details
         *            of the endpoint to be used
         * @param conversationType The type of conversation to be performed over this JFapAddress
         */
        public JFapAddressHolder(CreateNewVirtualConnectionFactory factory, Conversation.ConversationType conversationType)
        {
            this.factory = factory;
            this.conversationType = conversationType;
            this.inetAddress = null;
            this.jFapAddress = null;
            this.hostAddress = null;
            empty = false;
        }

        /**
         * @param inetAddress The InetSocketAddress over we which to get a JFapAddress
         * @param conversationType The type of conversation to be performed over this JFapAddress
         */
        public JFapAddressHolder(InetSocketAddress inetAddress, Conversation.ConversationType conversationType)
        {
            this.factory = null;
            this.conversationType = conversationType;
            this.inetAddress = inetAddress;
            this.jFapAddress = null;
            this.hostAddress = null; // Force lazy computation if getErrorInserts is called
            empty = false;
        }

        /**
         * @return JFapAddress The JFapAddress being held by this holder (obtained, if necessary) from the
         *         CreateNewVirtualConnectionFactory or from the InetSocketAddress. If empty this will
         *         be null.
         */
        public JFapAddress getAddress()
        {
            //Return null if empty.
            if (empty)
            {
                return null;
            }

            // Lazily get the actual jFapAddress 
            if (jFapAddress == null)
            {
                // If we're using a factory, ask that for the endpoint address and port and construct the InetSocketAddress from that
                if (factory != null)
                {
                    InetAddress epAddress = factory.getEndPointAddress();
                    int epPort = factory.getEndPointPort();
                    inetAddress = new InetSocketAddress(epAddress, epPort);
                }

                jFapAddress = new JFapAddress(inetAddress, conversationType);
            }

            return jFapAddress;
        }

        /**
         * @return Object[] An array of inserts for a CONNDATAGROUP_CONNFAILED_SICJ0063 that gives
         *         the details about the JFapAddress being held by this holder. If empty this will
         *         be null.
         */
        public Object[] getErrorInserts()
        {
            //Return null if empty.
            if (empty)
            {
                return null;
            }

            if (hostAddress == null)
            {
                // Derive the host address from the inetAddress
                if (inetAddress.getAddress() != null)
                    hostAddress = inetAddress.getAddress().toString();
                else
                    hostAddress = inetAddress.toString();
            }

            return new Object[] { hostAddress, inetAddress.getPort() };
        }
    }

    /**
     * Creates a new connection group for a non-CFEndPoint based endpoint descriptor.
     * 
     * @param tracker The tracker to which this group belongs.
     * @param virtualConnectionFactory The connection factory to use when establishing
     *            new connections
     * @param descriptor The endpoint descriptor that represents the remote host all
     *            connections in this group are connected to.
     * @param conversationsPerConnection Maximum number of conversations we are
     *            prepared to multiplex over a single connection.
     * @param heartbeatInterval The interval between heartbeats (in second)
     * @param heartbeatTimeout How long before a heartbeat is declared missed (in seconds)
     */
    protected ConnectionDataGroup(OutboundConnectionTracker tracker,
                                  NetworkConnectionFactory virtualConnectionFactory,
                                  EndPointDescriptor descriptor,
                                  int conversationsPerConnection,
                                  int heartbeatInterval,
                                  int heartbeatTimeout) // F196678.10
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { tracker, virtualConnectionFactory, descriptor, "" + conversationsPerConnection });
        this.groupEndpointDescriptor = descriptor;
        this.tracker = tracker;
        this.currentConnectionFactoryFactory = new ExistingNetworkConnectionFactoryHolder(virtualConnectionFactory);
        this.conversationsPerConnection = conversationsPerConnection;
        this.framework = null; // F193388
        this.heartbeatInterval = heartbeatInterval; // F196678.10
        this.heartbeatTimeout = heartbeatTimeout; // F196678.10
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Creates a new connection group for a CFEndPoint based endpoint descriptor.
     * 
     * @param tracker The tracker to which this group belongs.
     * @param descriptor The endpoint descriptor that represents the remote host all
     *            connections in this group are connected to.
     * @param conversationsPerConnection Maximum number of conversations we are
     *            prepared to multiplex over a single connection.
     * @param framework The framework over which to connect
     * @param heartbeatInterval The interval between heartbeats (in second)
     * @param heartbeatTimeout How long before a heartbeat is declared missed (in seconds)
     */
    protected ConnectionDataGroup(OutboundConnectionTracker tracker,
                                  EndPointDescriptor descriptor,
                                  int conversationsPerConnection,
                                  Framework framework,
                                  int heartbeatInterval,
                                  int heartbeatTimeout) // F193388, F196678.10
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { tracker, descriptor, "" + conversationsPerConnection, framework, "" + heartbeatInterval, "" + heartbeatTimeout }); // F193388, F1966781
        this.groupEndpointDescriptor = descriptor;
        this.tracker = tracker;
        this.currentConnectionFactoryFactory = new ExistingNetworkConnectionFactoryHolder(null);
        this.conversationsPerConnection = conversationsPerConnection;
        this.framework = framework; // F193388
        this.heartbeatInterval = heartbeatInterval; // F196678.10
        this.heartbeatTimeout = heartbeatTimeout; // F196678.10
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    // begin 244595
    /**
     * Creates a new connection group for a CFEndPoint based endpoint descriptor.
     * 
     * @param tracker The tracker to which this group belongs.
     * @param virtualConnectionFactory The connection factory to use when establishing
     *            new connections
     * @param descriptor The endpoint descriptor that represents the remote host all
     *            connections in this group are connected to.
     * @param framework The framework over which to connect
     */
    protected ConnectionDataGroup(OutboundConnectionTracker tracker,
                                  NetworkConnectionFactory virtualConnectionFactory,
                                  EndPointDescriptor descriptor,
                                  Framework framework)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { tracker, virtualConnectionFactory, descriptor, framework });
        this.groupEndpointDescriptor = descriptor;
        this.tracker = tracker;
        this.currentConnectionFactoryFactory = new ExistingNetworkConnectionFactoryHolder(virtualConnectionFactory);
        this.conversationsPerConnection = Integer.MAX_VALUE;
        this.framework = framework;
        this.heartbeatInterval = Integer.MAX_VALUE;
        this.heartbeatTimeout = Integer.MAX_VALUE;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    // end 244595

    /**
     * @return Returns a clone of the connection data list for this group.
     */
    protected List getConnections()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnections");

        final List rc;

        synchronized (connectionData)
        {
            rc = (List) connectionData.clone();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConnections", rc);
        return rc;
    }

    /**
     * Helper method, chooses the connection to use for a new conversation.
     * 
     * @return The connection data object relating to a connection to use for multiplexing
     *         a new conversation with.
     */
    private ConnectionData findConnectionDataToUse()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findConnectionDataToUse");

        // Look through the the connection data in this group to try and find
        // a connection with the lowest use count.
        ConnectionData connectionDataToUse = null;

        synchronized (connectionData)
        {
            int lowestUseCount = conversationsPerConnection;
            for (int i = 0; i < connectionData.size(); ++i)
            {
                final ConnectionData cd = connectionData.get(i);

                if (cd.getUseCount() < lowestUseCount)
                {
                    lowestUseCount = cd.getUseCount();
                    connectionDataToUse = cd;
                }
            }

            // If we cannot find a suitable connection data object, see if the
            // idle connections pool has a connection we could use to create one.
            if (connectionDataToUse == null)
            {
                final IdleConnectionPool p = IdleConnectionPool.getInstance();
                final OutboundConnection oc = p.remove(groupEndpointDescriptor);
                if (oc != null)
                {
                    connectionDataToUse = new ConnectionData(this, groupEndpointDescriptor);
                    oc.setConnectionData(connectionDataToUse);
                    connectionDataToUse.setConnection(oc);
                    connectionData.add(connectionDataToUse);

                }
            }

            // If we now have a connectionDataToUse, increment its use count NOW
            // so that it won't get accidentally added to the idle pool
            if (connectionDataToUse != null)
            {
                connectionDataToUse.incrementUseCount();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findConnectionDataToUse", connectionDataToUse);
        return connectionDataToUse;
    }

    /**
     * Creates and returns a new conversation. The conversation must be connected to
     * the same remote process as the conversation group is associated with. The conversation
     * will be created over a connection within this connection group.
     * 
     * @param endPoint The endpoint that the conversation will be attached to. This must be
     *            the same as the endpoint associated with this group or an exception will be thrown.
     * @param conversationReceiveListener The conversation receive listener to use for the
     *            new conversation.
     * @param conversationType The type of conversation being established
     * @return The conversation created.
     * @throws SIResourceException Thrown if the connect attempt fails.
     */
    protected Conversation connect(Object endPoint,
                                   ConversationReceiveListener conversationReceiveListener,
                                   Conversation.ConversationType conversationType)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { endPoint, conversationReceiveListener, conversationType });

        // Paranoia: check that this group was intended for endpoint style of operation (i.e., a NetworkConnectionFactory WAS NOT provided on construction)
        if (currentConnectionFactoryFactory.isNonNull())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "type of endpoint does not match group");
            throw new SIErrorException(nls.getFormattedMessage("CONNDATAGROUP_INTERNAL_SICJ0062", null, "CONNDATAGROUP_INTERNAL_SICJ0062")); // D226223
        }

        CreateNewVirtualConnectionFactory factory = new CreateNewVirtualConnectionFactory(endPoint);
        JFapAddressHolder jfapAddressHolder = new JFapAddressHolder(factory, conversationType);
        Conversation retConversation = doConnect(conversationReceiveListener, ConversationUsageType.JFAP, jfapAddressHolder, factory);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retConversation);
        return retConversation;
    }

    /**
     * Creates and returns a new conversation. The conversation must be connected to
     * the same remote process as the conversation group is associated with. The conversation
     * will be created over a connection within this connection group.
     * 
     * @param remoteAddress The remote address to connect this new conversation to.
     * @param chainName The chain name to use when establishing the new conversation.
     * @param conversationReceiveListener The conversation receive listener to use for the
     *            new conversation.
     * @param conversationType The type of conversation being established
     * @return The conversation created.
     * @throws SIResourceException thrown if the connect attempt fails.
     */
    protected Conversation connect(InetSocketAddress remoteAddress,
                                   String chainName,
                                   ConversationReceiveListener conversationReceiveListener,
                                   Conversation.ConversationType conversationType)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { remoteAddress, chainName, conversationReceiveListener, conversationType });

        // Paranoia: check that this group was intended for chainname style of operation (i.e., a NetworkConnectionFactory WAS provided on construction)
        if (!!!currentConnectionFactoryFactory.isNonNull())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "type of endpoint does not match group");
            throw new SIErrorException(nls.getFormattedMessage("CONNDATAGROUP_INTERNAL_SICJ0062", null, "CONNDATAGROUP_INTERNAL_SICJ0062")); // D226223
        }

        JFapAddressHolder jfapAddressHolder = new JFapAddressHolder(remoteAddress, conversationType);
        Conversation retConversation = doConnect(conversationReceiveListener, ConversationUsageType.JFAP, jfapAddressHolder, currentConnectionFactoryFactory);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retConversation);
        return retConversation;
    }

    // begin F244696
    /**
     * Creates and returns a new conversation. The conversation must be connected to
     * the same remote process as the conversation group is associated with. The conversation
     * will be created over a connection within this connection group.
     * 
     * @param conversationReceiveListener The conversation receive listener to use for the
     *            new conversation.
     * @param usageType Usage type for the conversation.
     * @return The conversation created.
     * @throws SIResourceException thrown if the connect attempt fails.
     */
    protected Conversation connect(final ConversationReceiveListener conversationReceiveListener, final ConversationUsageType usageType)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { conversationReceiveListener, usageType });

        //Pass in an 'empty' JFapAddressHolder.
        Conversation retConversation = doConnect(conversationReceiveListener, usageType, new JFapAddressHolder(), currentConnectionFactoryFactory);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retConversation);
        return retConversation;
    }

    /**
     * Actually do the connecting, creating a new ConnectionData if required
     * 
     * @param conversationReceiveListener The receive listener for the conversation
     * @param usageType Usage type for the conversation.
     * @param jfapAddressHolder The holder for jfapAddress over which to connect (if any)
     * @param ncfHolder The holder from which to get the a network connection factory (from which the virtual connection can be obtained)
     * @return Conversation The resulting conversation if we are connected
     * @throws JFapConnectFailedException is thrown if the connection cannot be made
     * @throws SIResourceException is thrown if the new conversation cannot be created
     */
    private Conversation doConnect(ConversationReceiveListener conversationReceiveListener, ConversationUsageType usageType, JFapAddressHolder jfapAddressHolder,
                                   final NetworkConnectionFactoryHolder ncfHolder) throws JFapConnectFailedException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "doConnect", new Object[] { conversationReceiveListener, usageType, jfapAddressHolder, ncfHolder });

        Conversation retConversation;
        ConnectionData connectionDataToUse;

        synchronized (connectionMonitor)
        {
            try
            {
                connectionDataToUse = findConnectionDataToUse();

                boolean isNewConnectionData = false;

                if (connectionDataToUse == null)
                {
                    // We were either unable to find a connection at all, or we could not
                    // find a connection with a use count lower than the maximum number of
                    // conversations we want to share a connection.  Either way, create a
                    // new connection and add it to the group.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "connection data does not already exist");
                    try
                    {

                        NetworkConnection vc = connectOverNetwork(jfapAddressHolder, ncfHolder);
                        connectionDataToUse = createnewConnectionData(vc);
                        isNewConnectionData = true;
                    } catch (FrameworkException frameworkException)
                    {
                        // Something went wrong attempting to establish the outbound connection.
                        // Link the exception that was thrown to a new SICommsException and throw
                        // it back to the caller.
                        FFDCFilter.processException(frameworkException,
                                                    "com.ibm.ws.sib.jfapchannel.impl.octracker.ConnectionDataGroup.doConnect",
                                                    JFapChannelConstants.CONNDATAGROUP_CONNECT_04,
                                                    this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.exception(this, tc, frameworkException);
                        throw new SIErrorException(nls.getFormattedMessage("CONNDATAGROUP_INTERNAL_SICJ0062", null, "CONNDATAGROUP_INTERNAL_SICJ0062"), frameworkException);
                    }
                }

                retConversation = startNewConversation(connectionDataToUse, conversationReceiveListener, isNewConnectionData, usageType.requiresNormalHandshakeProcessing());
            } finally
            {
                // Whatever happened, this connection attempt is no longer pending.
                //
                //We have to hold the connectionMonitor lock prior to obtaining the lock on 'this' as there is the potential for deadlock in the following
                //scenario.
                // 
                //Thread 1 starts the first conversation over the connection and has to perform handshaking. 
                //Thread 2 starts another conversation and has to wait for thread 1 to perform the handshake. This means that thread 2 has acquired the lock on 'this'.
                //Thread 1 tries to acquire the lock on 'this' below.
                //Deadlock...
                synchronized (this)
                {
                    --connectAttemptsPending;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, connectAttemptsPending + " connection attempts still pending");
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "doConnect", retConversation);
        return retConversation;
    }

    /**
     * Create a new connection over the network
     * 
     * @param addressHolder The holder from which to obtain the jfap address (if any) over which to connect
     * @param factoryHolder The holder from which to get the a network connection factory (from which the virtual connection can be obtained)
     * @return NetworkConnection the network connection that was created
     * @throws FrameworkException if no network connection can be created
     * @throws JFapConnectFailedException if the connection fail
     */
    private NetworkConnection connectOverNetwork(JFapAddressHolder addressHolder, NetworkConnectionFactoryHolder factoryHolder) throws JFapConnectFailedException, FrameworkException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connectOverNetwork", new Object[] { addressHolder, factoryHolder });

        NetworkConnectionFactory vcf = factoryHolder.getFactory();
        NetworkConnection vc = vcf.createConnection();
        Semaphore sem = new Semaphore();
        ClientConnectionReadyCallback callback = new ClientConnectionReadyCallback(sem);

        vc.connectAsynch(addressHolder.getAddress(), callback);
        sem.waitOnIgnoringInterruptions();

        if (!callback.connectionSucceeded())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Connect has failed due to ", callback.getException());

            String failureKey;
            Object[] failureInserts;
            if (addressHolder.getAddress() != null)
            {
                failureKey = "CONNDATAGROUP_CONNFAILED_SICJ0063";
                failureInserts = addressHolder.getErrorInserts();
            }
            else
            {
                failureKey = "CONNDATAGROUP_CONNFAILED_SICJ0080";
                failureInserts = new Object[] {};
            }

            String message = nls.getFormattedMessage(failureKey, failureInserts, failureKey);

            throw new JFapConnectFailedException(message, callback.getException());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connectOverNetwork", vc);
        return vc;
    }

    /**
     * Create a new Connection data object
     * 
     * @param vc The network connection over which to create a connection data
     * @return ConnectionData A new connection data object
     * @throws FrameworkException is thrown if the new connection data cannot be created
     */
    private ConnectionData createnewConnectionData(NetworkConnection vc) throws FrameworkException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createnewConnectionData", vc);

        ConnectionData connectionDataToUse;
        NetworkConnectionContext connLink = vc.getNetworkConnectionContext();

        connectionDataToUse = new ConnectionData(this, groupEndpointDescriptor);
        connectionDataToUse.incrementUseCount(); // New connections start with a use count of zero and it is now in use
                                                 // No need to lock as it's not in the connectionData list yet

        OutboundConnection oc = new OutboundConnection(connLink,
                        vc,
                        tracker,
                        heartbeatInterval,
                        heartbeatTimeout,
                        connectionDataToUse);

        connectionDataToUse.setConnection(oc);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createnewConnectionData", connectionDataToUse);
        return connectionDataToUse;
    }

    /**
     * start a new conversation using the specified ConnectionData object
     * 
     * @param connectionDataToUse The connection data on which to start a new conversation
     * @param conversationReceiveListener The conversation receive listener for the new conversation
     * @param isNewConnectionData Does the connection data still need to be added to the group?
     * @param handshake Can the conversation use normal handshaking?
     * @return Conversation The new conversation
     * @throws SIResourceException is thrown if the new conversation cannot be started
     */
    private Conversation startNewConversation(ConnectionData connectionDataToUse, ConversationReceiveListener conversationReceiveListener, boolean isNewConnectionData,
                                              boolean handshake) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "startNewConversation",
                        new Object[] { connectionDataToUse, conversationReceiveListener, Boolean.valueOf(isNewConnectionData), Boolean.valueOf(handshake) });

        Conversation retConversation;
        synchronized (this)
        {
            if (isNewConnectionData)
            {
                synchronized (connectionData)
                {
                    connectionData.add(connectionDataToUse);
                }
            }

            // Start a new conversation over the designated connection.
            // If this is a connection on behalf of the WMQRA then we need to disable the normal handshaking behaviour
            // in order to allow other non WMQRA conversations to handshake as normal.
            retConversation = connectionDataToUse.getConnection().startNewConversation(conversationReceiveListener, handshake);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "startNewConversation", retConversation);
        return retConversation;
    }

    /**
     * Marks the group as having been selected to establish a new conversation. This is
     * used to close a window where the group can be selected, but then get closed before
     * the connect call is made.
     */
    protected synchronized void connectionPending()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connectionPending");
        ++connectAttemptsPending;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, connectAttemptsPending + " connection attempts now pending");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connectionPending");
    }

    /**
     * Close a conversation on the specified connection. The connection must be part of this
     * group. If the connection has no more conversations left using it, then it is added
     * to the idle pool.
     * 
     * @param connection The connection to close a conversation on.
     */
    protected synchronized void close(OutboundConnection connection)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close", connection);

        // Paranoia: Check that this connection believes that it belongs in this group.
        if (connection.getConnectionData().getConnectionDataGroup() != this)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "connection does not belong to this group", connection.getConnectionData().getConnectionDataGroup());
            throw new SIErrorException(nls.getFormattedMessage("CONNDATAGROUP_INTERNAL_SICJ0062", null, "CONNDATAGROUP_INTERNAL_SICJ0062")); // D226223
        }

        ConnectionData data;
        boolean isNowIdle = false;

        synchronized (connectionData)
        {
            data = connection.getConnectionData();
            data.decrementUseCount();
            if (data.getUseCount() == 0)
            {
                // If no one is using this connection, then remove it from our group and
                // add it to the idle pool.
                connectionData.remove(data);
                isNowIdle = true;
            }
        }

        if (isNowIdle)
        {
            IdleConnectionPool.getInstance().add(data.getConnection(), groupEndpointDescriptor);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /**
     * Return the endpoint descriptor associated with this group.
     * 
     * @return Return the endpoint descriptor associated with this group.
     */
    protected EndPointDescriptor getEndPointDescriptor()
    {
        return groupEndpointDescriptor;
    }

    /**
     * Determines if this group is empty.
     * For the group to be empty, it must contain no connection
     * data objects, and have no connection attempts pending. This second criteria stops
     * the group from being discarded between selection for establishing a new conversation
     * and actually establishing the conversation.
     * 
     * @return True iff the group is "empty".
     */
    protected boolean isEmpty()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isEmpty");
        boolean result;

        synchronized (this)
        {
            synchronized (connectionData)
            {
                result = connectionData.isEmpty();
            }

            result = result && (connectAttemptsPending == 0);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isEmpty", "" + result);
        return result;
    }

    /**
     * Clone a conversation on a connection in this group.
     * 
     * @param connection The connection on which we want to clone a conversation.
     * @param conversationReceiveListener The receive listener to associated with the new
     *            conversation.
     * @return Conversation A cloned conversation.
     * @throws SIResourceException if the conversation does not belong to this ConnectionDataGroup
     */
    public Conversation clone(OutboundConnection connection,
                              ConversationReceiveListener conversationReceiveListener)
                    throws com.ibm.websphere.sib.exception.SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clone", new Object[] { connection, conversationReceiveListener });

        // Paranoia: Check that this connection believes that it belongs in this group.
        if (connection.getConnectionData().getConnectionDataGroup() != this)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "connection not part of this data group", connection.getConnectionData().getConnectionDataGroup());
            throw new SIErrorException(nls.getFormattedMessage("CONNDATAGROUP_INTERNAL_SICJ0062", null, "CONNDATAGROUP_INTERNAL_SICJ0062")); // D226223
        }

        Conversation retConversation;
        synchronized (connectionMonitor)
        {
            synchronized (this)
            {
                ConnectionData connectionDataToUse = connection.getConnectionData();
                retConversation =
                                connectionDataToUse.getConnection().startNewConversation(conversationReceiveListener, true);
                synchronized (connectionData)
                {
                    connectionDataToUse.incrementUseCount();
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clone", retConversation);
        return retConversation;
    }

    /**
     * Purge a connection from this group from within invalidate processing.
     * Purging a connection removes it from the group even if the connection
     * still has conversations associated with it.
     * The purged connection is closed and not added to the idle pool.
     * 
     * @param connection The connection to purge
     * @param notifyPeer Should we notify our peer that the connection is being closed?
     */
    protected void purgeFromInvalidateImpl(OutboundConnection connection, boolean notifyPeer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "purgeFromInvalidateImpl", new Object[] { connection, Boolean.valueOf(notifyPeer) });
        purge(connection, true, notifyPeer);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "purgeFromInvalidateImpl");
    }

    /**
     * Purge a connection from this group, because it has already been closed.
     * The purged connection is not added to the idle pool.
     * 
     * @param connection The connection to purge
     */
    protected void purgeClosedConnection(OutboundConnection connection)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "purgeClosedConnection", connection);
        purge(connection, false, false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "purgeClosedConnection");
    }

    private void purge(OutboundConnection connection, boolean calledFromInvalidateImpl, boolean notifyPeer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "purge", new Object[] { connection, Boolean.valueOf(calledFromInvalidateImpl), Boolean.valueOf(notifyPeer) });

        boolean connectionBeingPurged = false;
        synchronized (connection)
        {
            connectionBeingPurged = connection.isBeingPurged();
            if (!connectionBeingPurged)
                connection.beingPurged();
        }

        if (!connectionBeingPurged)
        {
            boolean doPhysicalClose;
            synchronized (connectionData)
            {
                doPhysicalClose =
                                connectionData.remove(connection.getConnectionData());
            }

            if (doPhysicalClose && calledFromInvalidateImpl)
            {
                // This method is called from within invalidateImpl, so
                // call the appropriate physical close method.
                connection.physicalCloseFromInvalidateImpl(notifyPeer);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "purge");
    }

    /**
     * @return the NetworkConnectionFactory
     */
    public NetworkConnectionFactory getNetworkConnectionFactory() {
        return currentConnectionFactoryFactory.getFactory();
    }

    /**
     * Remove the connection from the group
     * 
     * @param cd
     */
    public void removeConnectionDataFromGroup(ConnectionData cd) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeConnectionDataFromGroup", new Object[] { cd });
        boolean removed = false;
        synchronized (connectionData)
        {
            removed = connectionData.remove(cd);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeConnectionDataFromGroup", new Object[] { removed });

    }

}
