/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.server.impl;

import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jfap.inbound.channel.NettyInboundChain;
import com.ibm.ws.sib.comms.server.GenericTransportAcceptListener;
import com.ibm.ws.sib.jfapchannel.AcceptListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.jfapchannel.impl.ConversationImpl;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.netty.NettyIOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.netty.NettyNetworkConnection;
import com.ibm.ws.sib.jfapchannel.netty.NettyNetworkConnectionContext;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.openliberty.netty.internal.exception.NettyException;

public class NettyJMSServerHandler extends SimpleChannelInboundHandler<WsByteBuffer>{

	/** Trace */
	private static final TraceComponent tc = SibTr.register(NettyJMSServerHandler.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	/** Log class info on load */
	static
	{
		if (tc.isDebugEnabled())
			SibTr.debug(tc,
					"@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/server/impl/NettyJMSServerHandler.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
	}


	protected final static AttributeKey<InboundConnection> CONNECTION_KEY = AttributeKey.valueOf("InboundConnection");
	public final static AttributeKey<String> CHAIN_ATTR_KEY = AttributeKey.valueOf("CHAIN_NAME");
	public final static AttributeKey<NettyInboundChain> ATTR_KEY = AttributeKey.valueOf("CHAIN");

	/** Called when a new connection is established */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "channelActive", ctx.channel());
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "channelActive", ctx.channel().remoteAddress() + " connected for chain " + ctx.channel().attr(CHAIN_ATTR_KEY).get() + " running: " + ctx.channel().attr(ATTR_KEY).get().isRunning());
		}
		AcceptListener acceptListener = new GenericTransportAcceptListener();

		// begin F196678.10
		// Heartbeat passing in Null since hasn't been implemented to be configurable per endpoint properties
		int heartbeatInterval = determineHeartbeatInterval(null);
		int heartbeatTimeout = determineHeartbeatTimeout(null);
		// end F196678.10

		NettyNetworkConnection conn = new NettyNetworkConnection(ctx.channel(), ctx.channel().attr(CHAIN_ATTR_KEY).get(), true);

		InboundConnection connection = null;
		try {
            connection = new InboundConnection(new NettyNetworkConnectionContext(conn),
                                            conn,
                                            acceptListener,
                                            heartbeatInterval,
                                            heartbeatTimeout,
                                            true);
        } catch (FrameworkException fe) {
            //At this point the underlying TCP/IP connection has gone away.
            //We can't throw an Exception so there is little we can do here other than FFDC.
            //The channel framework should close everything down gracefully.
            FFDCFilter.processException(fe, "com.ibm.ws.sib.jfapchannel.impl.JFapInboundConnLink", JFapChannelConstants.JFAPINBOUNDCONNLINK_READY_03);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Exception occurred creating InboundConnection. Closing channel.");
            ctx.close();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(this, tc, fe);
        }

        if (connection != null) {
        	ctx.channel().attr(CONNECTION_KEY).set(connection);
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

		// At this point here we leave explicit channel framework land and trot into the land of
        // abstraction. As such, create the channel framework implementation classes directly and
        // pass them into the JFap channel common code.

		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "channelActive", ctx.channel());

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WsByteBuffer msg) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "channelRead0", ctx.channel());
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "channelRead0", ctx.channel() + ". [" + msg.array() + "] bytes received");
		}

		Attribute<InboundConnection> attr = ctx.channel().attr(CONNECTION_KEY);
		InboundConnection connection = attr.get();

		//TODO: Check if connection is closed
		if (connection != null) {
			IOReadCompletedCallback callback = connection.getReadCompletedCallback();
			IOReadRequestContext readCtx = connection.getReadRequestContext();
			NetworkConnection networkConnection = connection.getNetworkConnection();
			if(
					callback instanceof NettyConnectionReadCompletedCallback && 
					readCtx instanceof NettyIOReadRequestContext && 
					networkConnection instanceof NettyNetworkConnection) {
				((NettyConnectionReadCompletedCallback)callback).readCompleted(msg, readCtx, (NettyNetworkConnection)networkConnection);
			}else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
        			SibTr.warning(tc, "channelRead0: Something's wrong. Callback, network connection, or read context is not netty specific. This shouldn't happen.", new Object[] {connection, callback, readCtx, networkConnection});
                }
        		exceptionCaught(ctx, new NettyException("Illegal callback type for channel."));
        		return;
			}

		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
				SibTr.warning(tc, "channelRead0: could not associate an incoming message with a Connection. Message will be ignored and channel will be closed.", new Object[] {ctx.channel()});
				ctx.close();
			}
		}
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "channelRead0", ctx.channel());

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "channelInactive", ctx.channel());
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "channelInactive", ctx.channel().remoteAddress() + " has been disconnected");
		}
		// TODO: Check how to manage inactive channels
		InboundConnection connection = ctx.channel().attr(CONNECTION_KEY).get();
		ctx.channel().attr(CONNECTION_KEY).set(null);
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "channelInactive", ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "exceptionCaught", ctx.channel());
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "exceptionCaught", cause);
		}
		// TODO: Check how to manage an exception
		ctx.close();
		super.exceptionCaught(ctx, cause);
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "exceptionCaught", ctx.channel());
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

}

