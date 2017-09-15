/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.server.impl;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.List;

import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.jfapchannel.server.AcceptListenerFactory;
import com.ibm.ws.sib.jfapchannel.server.ServerConnectionManager;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implementation of the ServerConnectionManager.
 * 
 * @author prestona
 */
public class ServerConnectionManagerImpl extends ServerConnectionManager {
    private static final com.ibm.websphere.ras.TraceComponent tc = SibTr.register(ServerConnectionManagerImpl.class,
                                                                                  JFapChannelConstants.MSG_GROUP,
                                                                                  JFapChannelConstants.MSG_BUNDLE);

    /* ************************************************************************** */
    /**
     * A ServerConfigChangeListener is registered with admin so that we can give
     * the channel framework a prod to reconcile inbound chains if a bus is added
     * or deleted
     */
    /* ************************************************************************** */
    private static final class ServerConfigChangeListener {
        /* -------------------------------------------------------------------------- */
        /*
         * configChanged method
         * /* --------------------------------------------------------------------------
         */
        /**
         * Notified that the config has changed. Since we carefully register for just
         * those changes we care about, we can assume that if we are told about the
         * change we should reconcile the inbound chains
         * 
         * @see com.ibm.ws.management.service.ConfigChangeListener#configChanged(com.ibm.websphere.management.repository.ConfigRepositoryEvent)
         * @param event
         */
        public void configChanged() {
        //Venu Liberty COMMS TODO
        }
    }

    /** NLS */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);

    /** Log class info on load */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.server.impl/src/com/ibm/ws/sib/jfapchannel/impl/ServerConnectionManagerImpl.java, SIB.comms, WASX.SIB, aa1225.01 1.53");
    }

    /**
     * Register with admin as a regular expression listener for dynamic config updates
     * - when the update arrives we reconcile the inbound chains
     */
    private static final String busUriPattern = ".*/buses/.*/sib-bus.xml";
    private static final ServerConfigChangeListener busListener = new ServerConfigChangeListener();

    /** Listener port map */
    private final Hashtable<Integer, ListenerPortImpl> portToListenerMap;

    /** The outbound connection tracker */
    private static OutboundConnectionTracker connectionTracker = null;

    /** The possible states we could be in */
    private static enum State {
        UNINITIALISED, INITIALISED, INITIALISATION_FAILED
    };

    /** Our current state */
    private static State state = State.UNINITIALISED;

    /** Factory for creating accept listener instances */
    private static AcceptListenerFactory acceptListenerFactory;

    /**
     * Create a new server connection manager
     */
    public ServerConnectionManagerImpl() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");
        portToListenerMap = new Hashtable<Integer, ListenerPortImpl>();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * @param quiesce
     */
    @Override
    public void closeAll(boolean quiesce) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "closeAll", "" + quiesce);
        // MS:4 do we still need this? (tidyup)
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "closeAll");
    }

    /**
     * Initialises the server connection manager by getting hold of the framework.
     * 
     * @param _acceptListenerFactory
     * 
     * @see com.ibm.ws.sib.jfapchannel.ServerConnectionManager#initialise(com.ibm.ws.sib.jfapchannel.AcceptListenerFactory)
     */
    public static void initialise(AcceptListenerFactory _acceptListenerFactory) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initalise");

        acceptListenerFactory = _acceptListenerFactory;

        // Create the maintainer of the configuration.
        Framework framework = Framework.getInstance();
        if (framework == null) {
            state = State.INITIALISATION_FAILED;
        } else {
            state = State.INITIALISED;

            // Extract the chain reference.
            connectionTracker = new OutboundConnectionTracker(framework);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initalise");
    }

    /**
     * Set the AcceptListenerFactory.
     * 
     * @param _acceptListenerFactory
     */
    public static void initialiseAcceptListenerFactory(AcceptListenerFactory _acceptListenerFactory) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initialiseAcceptListenerFactory", _acceptListenerFactory);

        acceptListenerFactory = _acceptListenerFactory;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialiseAcceptListenerFactory");
    }

    /**
     * Connect to a remote ME.
     * 
     * @see com.ibm.ws.sib.jfapchannel.ServerConnectionManager#connect(java.net.InetSocketAddress, com.ibm.ws.sib.jfapchannel.ConversationReceiveListener, java.lang.String)
     */
    // begin F171173
    @Override
    public Conversation connect(InetSocketAddress remoteHost,
                                ConversationReceiveListener convRecvListener,
                                String chainName)
                    throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { remoteHost, convRecvListener, chainName });
        // begin F178022, F196678.10
        if (state == State.UNINITIALISED) {
            throw new SIErrorException(nls.getFormattedMessage("SVRCONNMGR_INTERNAL_SICJ0059", null, "SVRCONNMGR_INTERNAL_SICJ0059")); // D226223
        } else if (state == State.INITIALISATION_FAILED) {
            String nlsMsg = nls.getFormattedMessage("EXCP_CONN_FAIL_NO_CF_SICJ0007", null, null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "connection failed because comms failed to initialise");
            throw new SIResourceException(nlsMsg);
        }
        // end F178022, F196678.10

        Conversation retValue = connectionTracker.connect(remoteHost,
                                                          convRecvListener,
                                                          chainName,
                                                          Conversation.ME);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retValue);
        return retValue;
    }

    // end F171173

    /**
     * Implementation of the connect method provided by the abstract parent
     * class. This flavour of connect establishes a connection using WLM
     * endpoint data, which, can be treated as an opaque object passed to the
     * Channel Framework.
     * 
     * @see ServerConnectionManager
     * @param endpoint
     * @param convRecvListener
     */
    @Override
    public Conversation connect(CFEndPoint endpoint,
                                ConversationReceiveListener convRecvListener)
                    throws SIResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "connect", new Object[] { endpoint, convRecvListener });
        // begin F178022, F196678.10
        if (state == State.UNINITIALISED) {
            throw new SIErrorException(nls.getFormattedMessage("SVRCONNMGR_INTERNAL_SICJ0059", null, "SVRCONNMGR_INTERNAL_SICJ0059")); // D226223
        } else if (state == State.INITIALISATION_FAILED) {
            String nlsMsg = nls.getFormattedMessage("EXCP_CONN_FAIL_NO_CF_SICJ0007", null, null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "connection failed because comms failed to initialise");
            throw new SIResourceException(nlsMsg); // F173069, // F174602
        }
        // end F178022, F196678.10

        Conversation retValue = connectionTracker.connect(endpoint, convRecvListener, Conversation.ME);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "connect", retValue);
        return retValue;
    }

    // begin F189351
    protected static AcceptListenerFactory getAcceptListenerFactory() {
        return acceptListenerFactory;
    }

    // end F189351

    /**
     * Obtains a list of active outbound ME to ME conversations in this JVM.
     * 
     * @return a list of Conversations
     */
    @Override
    public List getActiveOutboundMEtoMEConversations() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getActiveOutboundMEtoMEConversations");

        List convs = null;
        if (connectionTracker != null) {
            convs = connectionTracker.getAllOutboundConversations();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getActiveOutboundMEtoMEConversations", convs);
        return convs;
    }
}
