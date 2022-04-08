package com.ibm.ws.netty.jfapchannel;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.utils.ras.SibTr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class JMSClientInboundHandler extends SimpleChannelInboundHandler<WsByteBuffer>{
	
	/** Trace */
    private static final TraceComponent tc = SibTr.register(JMSClientInboundHandler.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);
	
	/** Log class info on load */
    static
    {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/JMSClientInboundHandler.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }
	
	
	protected final static AttributeKey<OutboundConnection> CONNECTION_KEY = AttributeKey.valueOf("OutboundConnection");
	protected final static AttributeKey<String> CHAIN_ATTR_KEY = AttributeKey.valueOf("CHAIN_NAME");
	
	/** Called when a new connection is established */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "channelActive", ctx.channel());
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "channelActive", ctx.channel().remoteAddress() + " connected for chain " + ctx.channel().attr(CHAIN_ATTR_KEY).get());
		}
		// Set attribute to point to the appropriate connection
		
		Attribute<OutboundConnection> attr = ctx.channel().attr(CONNECTION_KEY);
		if(attr.get() == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				SibTr.debug(this, tc, "channelActive", "could not associate an incoming conection with a Connection.");
            }
		}
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
		
		
		Attribute<OutboundConnection> attr = ctx.channel().attr(CONNECTION_KEY);
		OutboundConnection connection = attr.get();

        if (connection != null) {
        	IOReadCompletedCallback callback = connection.getReadCompletedCallback();
        	if(callback instanceof NettyConnectionReadCompletedCallback) {
        		((NettyConnectionReadCompletedCallback)callback).readCompleted(msg);
        	}else {
        		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        			SibTr.debug(this, tc, "channelRead0", "Something's wrong. Callback is not netty specific. Cry cause not sure what happened.");
                }
        	}
        	
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            	SibTr.debug(this, tc, "channelRead0", "could not associate an incoming message with a Connection. Message will be ignored.");
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
		OutboundConnection connection = ctx.channel().attr(CONNECTION_KEY).get();
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
		if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "exceptionCaught", ctx.channel());
	}

}
