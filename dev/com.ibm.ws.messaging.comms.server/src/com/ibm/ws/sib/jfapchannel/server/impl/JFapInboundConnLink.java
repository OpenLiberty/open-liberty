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

// NOTE: D181601 is not changed flagged as it modifies every line of trace and FFDC.

package com.ibm.ws.sib.jfapchannel.server.impl;

import java.util.Map;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.AcceptListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.MetaDataProvider;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.jfapchannel.impl.ConversationImpl;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnection;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.richclient.impl.ConversationMetaDataImpl;
import com.ibm.ws.sib.jfapchannel.richclient.impl.JFapChannelFactory;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundApplicationLink;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Object that represents a single inbound connection. This is required to
 * participate as a channel in the Channel Framework.
 */
public class JFapInboundConnLink extends InboundApplicationLink implements MetaDataProvider {
    private static final TraceComponent tc = SibTr.register(JFapInboundConnLink.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.server.impl/src/com/ibm/ws/sib/jfapchannel/impl/JFapInboundConnLink.java, SIB.comms, WASX.SIB, aa1225.01 1.38");
    }

    // Configuration information for this connlink.
    private final ChannelData config; // F177053

    private ConversationMetaData metaData; // D196678.10.1

    /**
     * Creates a new connection link.
     * 
     * @param vc
     * @param channelFactoryData
     * @param cc
     */
    public JFapInboundConnLink(VirtualConnection vc,
                               ChannelFactoryData channelFactoryData,
                               ChannelData cc) // F177053, D196678.10.1
    {
        super(); // F177053
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { vc, channelFactoryData, cc }); // F196678.10.1	

        ChannelFramework cfw = ChannelFrameworkFactory.getChannelFramework();
        // begin D196678.10.1
        config = cc;

        try {
            ChainData[] chainDataArray = null;
            String channelName = config.getName();
            chainDataArray = cfw.getInternalRunningChains(channelName); // D232185

            if (chainDataArray != null) {
                if (chainDataArray.length != 1) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "chain data contains more than one entry!");
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "channelName=" + channelName + " chainData=" + chainDataArray[0]);

                metaData = new ConversationMetaDataImpl(chainDataArray[0], this); // F206161.5
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "cannot find a running chain for channel: " + channelName);
        } catch (ChannelException e) {
            FFDCFilter.processException
                            (e, "com.ibm.ws.sib.jfapchannel.impl.JFapInboundConnLink",
                             JFapChannelConstants.JFAPINBOUNDCONNLINK_INIT_01,
                             cfw); // D232185

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, e);
        }
        // end D196678.10.1

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Notification that this inbound connection link has been established and can now be used. This
     * drives the start of the first conversation on this link.
     * 
     * @param readyVc
     */
    public void ready(VirtualConnection readyVc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "ready", readyVc);

        AcceptListener acceptListener = (AcceptListener) config.getPropertyBag()
                        .get(JFapChannelFactory.ACCEPT_LISTENER);

        // begin F189351
        if (acceptListener == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "null accept listener - obtaining one from factory");
            acceptListener =
                            ServerConnectionManagerImpl.getAcceptListenerFactory().manufactureAcceptListener();
        }

        // begin F196678.10
        Map properties = config.getPropertyBag();
        int heartbeatInterval = determineHeartbeatInterval(properties);
        int heartbeatTimeout = determineHeartbeatTimeout(properties);
        // end F196678.10

        // At this point here we leave explicit channel framework land and trot into the land of
        // abstraction. As such, create the channel framework implementation classes directly and
        // pass them into the JFap channel common code.
        CFWNetworkConnection conn = new CFWNetworkConnection(readyVc);

        InboundConnection connection = null;
        try {
            connection = new InboundConnection(new CFWNetworkConnectionContext(conn, this),
                                            conn,
                                            acceptListener,
                                            heartbeatInterval,
                                            heartbeatTimeout);
        } catch (FrameworkException fe) {
            //At this point the underlying TCP/IP connection has gone away.
            //We can't throw an Exception so there is little we can do here other than FFDC.
            //The channel framework should close everything down gracefully.
            FFDCFilter.processException(fe, "com.ibm.ws.sib.jfapchannel.impl.JFapInboundConnLink", JFapChannelConstants.JFAPINBOUNDCONNLINK_READY_03);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Exception occurred creating InboundConnection");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, fe);
        }

        if (connection != null) {
            ConversationImpl conversation = new ConversationImpl(Connection.FIRST_CONVERSATION_ID,
                                                              true,
                                                              connection,
                                                              null);

            // begin F176003
            // Try asking the user for a conversation receive listener to use
            ConversationReceiveListener rl = null;
            try {
                rl = acceptListener.acceptConnection(conversation);
            } catch (Throwable t) {
                FFDCFilter.processException
                                (t, "com.ibm.ws.sib.jfapchannel.impl.JFapInboundConnLink", JFapChannelConstants.JFAPINBOUNDCONNLINK_READY_01);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception occurred in acceptConnection callback");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.exception(this, tc, t);
            }

            // If the user supplied a null receive listener, or an exception occurred in
            // the callback, supply a receive listener of our own as a temporary mesure
            // until we get the chance to close the conversation.
            if (rl == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Null receive listener, closing conversation");
                conversation.setDefaultReceiveListener(new CloseConversationReceiveListener());
                try {
                    conversation = connection.startNewConversation(conversation);
                    conversation.close(); // D196125
                } catch (SIException e) {
                    FFDCFilter.processException
                                    (e, "com.ibm.ws.sib.jfapchannel.impl.JFapInboundConnLink", JFapChannelConstants.JFAPINBOUNDCONNLINK_READY_02);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.exception(this, tc, e);

                    // Something went badly wrong closing the convesation - take down the
                    // connection.
                    connection.invalidate(true, e, "SIConnectionLostException thrown during conversation close"); // D224570
                }
            } else {
                conversation.setDefaultReceiveListener(rl);
                try {
                    connection.startNewConversation(conversation);
                } catch (SIResourceException e) {
                    // No FFDC code needed
                    // (it will have been FFDC'ed at source)
                    try {
                        conversation.close();
                    } catch (SIConnectionLostException e2) {
                        // No FFDC code needed
                        // (it is already broken - we don't care)
                    }
                    connection.invalidate(true, e, "Resource exception thrown when starting new conversation");
                }
            }
        }
        // end F176003

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "ready");
    }

    // begin F196678.10
    private int determineHeartbeatInterval(Map properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "determineHeartbeatInterval", properties);

        // How often should we heartbeat?
        int heartbeatInterval = JFapChannelConstants.DEFAULT_HEARTBEAT_INTERVAL;
        try {
            //669424: using RuntimeInfo.getPropertyWithMsg as this would log a message
            // when the property is different to the default value passed.
            heartbeatInterval = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.RUNTIMEINFO_KEY_HEARTBEAT_INTERVAL, "" + heartbeatInterval));
        } catch (NumberFormatException nfe) {
            // No FFDC code needed
        }

        if (properties != null) {
            String intervalStr = (String) properties.get(JFapChannelConstants.CHANNEL_CONFIG_HEARTBEAT_INTERVAL_PROPERTY);
            if (intervalStr != null) {
                try {
                    heartbeatInterval = Integer.parseInt(intervalStr);
                } catch (NumberFormatException nfe) {
                    // No FFDC code needed
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "determineHeartbeatInterval", heartbeatInterval);

        return heartbeatInterval;
    }

    // end F196678.10

    // begin F196678.10
    private int determineHeartbeatTimeout(Map properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "determineHeartbeatTimeout", properties);

        // How often should we heartbeat?
        int heartbeatTimeout = JFapChannelConstants.DEFAULT_HEARTBEAT_TIMEOUT;
        try {
            //669424: using RuntimeInfo.getPropertyWithMsg as this would log a message
            // when the property is different to the default value passed.
            heartbeatTimeout = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.RUNTIMEINFO_KEY_HEARTBEAT_TIMEOUT, "" + heartbeatTimeout));
        } catch (NumberFormatException nfe) {
            // No FFDC code needed
        }

        if (properties != null) {
            String timeoutStr = (String) properties.get(JFapChannelConstants.CHANNEL_CONFIG_HEARTBEAT_TIMEOUT_PROPERTY);
            if (timeoutStr != null) {
                try {
                    heartbeatTimeout = Integer.parseInt(timeoutStr);
                } catch (NumberFormatException nfe) {
                    // No FFDC code needed
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "determineHeartbeatTimeout", heartbeatTimeout);

        return heartbeatTimeout;
    }

    // end F196678.10

    /**
     * "Dummy" conversation receive listener. This does nothing and is only registered
     * because we need a instance while we close the conversation.
     */
    // begin F176003
    private static class CloseConversationReceiveListener implements ConversationReceiveListener {
        public ConversationReceiveListener dataReceived(WsByteBuffer data,
                                                        int segmentType,
                                                        int requestNumber,
                                                        int priority,
                                                        boolean allocatedFromBufferPool,
                                                        boolean partOfExchange, // f181007
                                                        Conversation conversation) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "CloseConversationReceiveListener.dataReceived");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "CloseConversationReceiveListener.dataReceived");
            return null;
        }

        public void errorOccurred(SIConnectionLostException exception, int segmentType, int requestNumber, int priority, Conversation conversation) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "CloseConversationReceiveListener.errorOccurred");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "CloseConversationReceiveListener.errorOccurred");
        }

        // Start F201521
        public Dispatchable getThreadContext(Conversation conversation, WsByteBuffer data, int segmentType) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "CloseConversationReceiveListener.getThreadContext");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "CloseConversationReceiveListener.getThreadContext");
            return null;
        }
        // End F201521
    }

    // end F176003

    /**
     * From a functional perspective, this method is not required. All it does it invoke the
     * identical method on its superclass. It does, however, give us the opportunity to
     * trace pertinant exception information as it flows through.
     */
    // begin D181601
    @Override
    public void close(VirtualConnection vc, Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close", new Object[] { vc, e });
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && (e != null))
            SibTr.exception(this, tc, e);
        super.close(vc, e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    // end D181601

    /**
     * From a functional perspective, this method is not required. All it does it invoke the
     * identical method on its superclass. It does, however, give us the opportunity to
     * trace pertinant exception information as it flows through.
     */
    // begin D181601
    @Override
    public void destroy(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "destroy", e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && (e != null))
            SibTr.exception(this, tc, e);
        //Romil liberty changes
        //  super.destroy(e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "destroy");
    }

    // end D181601

    // begin D196678.10.1
    public ConversationMetaData getMetaData() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMetaData");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMetaData", metaData);
        return metaData;
    }
    // end D196678.10.1
}
