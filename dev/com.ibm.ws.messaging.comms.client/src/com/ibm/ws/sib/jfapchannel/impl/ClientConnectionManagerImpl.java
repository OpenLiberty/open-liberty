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
package com.ibm.ws.sib.jfapchannel.impl;

import java.net.InetSocketAddress;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implementation of the client connection manager. Provides useful
 * client functions - like being able to establish conversations with
 * other MEs
 * 
 * @author prestona
 */
public class ClientConnectionManagerImpl extends ClientConnectionManager
{
    /** Trace */
    private static final TraceComponent tc = SibTr.register(ClientConnectionManagerImpl.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    /** NLS */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE); // F178022

    /** Log class info on load */
    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/ClientConnectionManagerImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.47");
    }

    // Reference to helper object which keeps track of in use outbound connections.
    private static OutboundConnectionTracker tracker = null;

    // Set to true if we try to initalise but are broken.
    private static boolean initialisationFailed = false;

    public ClientConnectionManagerImpl()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Implementation of the connect method provided by our abstract parent. Attempts to establish a
     * conversation to the specified remote host using the appropriate chain. This may involve
     * creating a new connection or reusing an existing one. The harder part is doing this in such a
     * way as to avoid blocking all calls while processing a single new outbound connection attempt.
     * 
     * @param remoteHost
     * @param arl
     * @return Connection
     */
    @Override
    public Conversation connect(InetSocketAddress remoteHost,
                                ConversationReceiveListener arl,
                                String chainName)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { remoteHost, arl, chainName });

        if (initialisationFailed)
        {
            String nlsMsg = nls.getFormattedMessage("EXCP_CONN_FAIL_NO_CF_SICJ0007", null, null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "connection failed because comms failed to initialise");
            throw new SIResourceException(nlsMsg);
        }

        Conversation conversation = tracker.connect(remoteHost, arl, chainName, Conversation.CLIENT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", conversation);
        return conversation;
    }

    /**
     * Starts a Conversation with the host using the information specified in the endpoint.
     * 
     * @param endpoint The endpoint to connect to.
     * @param conversationReceiveListener The receive listener to use.
     * 
     * @return Returns a Conversation to the host.
     */
    @Override
    public Conversation connect(Object endpoint,
                                ConversationReceiveListener conversationReceiveListener)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { endpoint, conversationReceiveListener });

        if (initialisationFailed)
        {
            String nlsMsg = nls.getFormattedMessage("EXCP_CONN_FAIL_NO_CF_SICJ0007", null, null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "connection failed because comms failed to initialise");
            throw new SIResourceException(nlsMsg);
        }

        Conversation conversation = tracker.connect(endpoint, conversationReceiveListener, Conversation.CLIENT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", conversation);
        return conversation;
    }

    /**
     * Starts a Conversation on z/OS across the Cross-Memory channel.
     * 
     * @param receiveListener
     * @param type the way the conversation will be used.
     * 
     * @return Returns a Conversation.
     */
    @Override
    public Conversation connect(final ConversationReceiveListener receiveListener, final ConversationUsageType type) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { receiveListener, type });

        if (initialisationFailed)
        {
            String nlsMsg = nls.getFormattedMessage("EXCP_CONN_FAIL_NO_CF_SICJ0007", null, null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "connection failed because comms failed to initialise");
            throw new SIResourceException(nlsMsg);
        }

        Conversation conversation = tracker.connect(receiveListener, Conversation.CLIENT, type);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", conversation);
        return conversation;
    }

    /**
     * Initialises the Client Connection Manager.
     * 
     * @see com.ibm.ws.sib.jfapchannel.ClientConnectionManager#initialise()
     */
    public static void initialise() throws SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initialise");

        initialisationFailed = true;

        Framework framework = Framework.getInstance();

        if (framework != null)
        {
            tracker = new OutboundConnectionTracker(framework);
            initialisationFailed = false;
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "initialisation failed");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialise");
    }

    /**
     * @return Returns a List of the active outbound conversations.
     */
    @Override
    public List getActiveOutboundConversations()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getActiveOutboundConversations");

        List convs = null;
        if (tracker != null)
        {
            convs = tracker.getAllOutboundConversations();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getActiveOutboundConversations", convs);
        return convs;
    }

    /**
     * @return Returns a dirty List of the active outbound conversations without obtaining locks, or null if we weren't able to
     *         generate the list
     */
    @Override
    public List getActiveOutboundConversationsForFfdc()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getActiveOutboundConversationsForFfdc");

        List convs = null;
        if (tracker != null)
        {
            convs = tracker.getAllOutboundConversationsForFfdc();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getActiveOutboundConversationsForFfdc", convs);
        return convs;
    }

    /**
     * For unit test only!
     * 
     * @param tracker
     */
    protected static void setOutboundConnectionTracker(OutboundConnectionTracker tracker)
    {
        ClientConnectionManagerImpl.tracker = tracker;
    }

    @Override
    public OutboundConnectionTracker getOutboundConnectionTracker() {
        return tracker;
    }
}
