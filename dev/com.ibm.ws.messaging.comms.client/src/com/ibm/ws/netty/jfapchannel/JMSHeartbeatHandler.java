package com.ibm.ws.netty.jfapchannel;

import java.net.SocketTimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.utils.ras.SibTr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.handler.timeout.IdleState;

public class JMSHeartbeatHandler extends IdleStateHandler{
	

	/** Trace */
    private static final TraceComponent tc = SibTr.register(JMSHeartbeatHandler.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);
	
	/** Log class info on load */
    static
    {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/JMSHeartbeatHandler.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }
    
    public JMSHeartbeatHandler(int heartbeatTimeSeconds) {
		super(heartbeatTimeSeconds, 0, 0);
    	if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", heartbeatTimeSeconds);
		if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "channelActive", ctx.channel());
		super.channelActive(ctx);
		if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "channelActive", ctx.channel());
    }
	
	@Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "handlerAdded", ctx.channel());
		super.handlerAdded(ctx);
		if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "handlerAdded", ctx.channel());
    }
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "userEventTriggered", new Object[] {ctx.channel(), evt});
		
		if(evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			
			if(event.state() != IdleState.READER_IDLE) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) 
	    			SibTr.warning(tc, "userEventTriggered: Event triggered was not a read timeout. This shouldn't happen. Event will be ignored.", evt);
			}else {
			
				Attribute<OutboundConnection> attr = ctx.channel().attr(JMSClientInboundHandler.CONNECTION_KEY);
				OutboundConnection connection = attr.get();
		
		        if (connection != null) {
		        	IOReadCompletedCallback callback = connection.getReadCompletedCallback();
		        	IOReadRequestContext readCtx = connection.getReadRequestContext();
		        	NetworkConnection networkConnection = connection.getNetworkConnection();
		        	if(
		        			callback instanceof NettyConnectionReadCompletedCallback && 
		        			readCtx instanceof NettyIOReadRequestContext &&
		        			networkConnection instanceof NettyNetworkConnection
					) {
		        		// create timeout exception to pass to callback error method
		                // Add local and remote address information
		                String ioeMessage = "Socket operation timed out before it could be completed";
		                ioeMessage = ioeMessage + " local=" + ctx.channel().localAddress() + " remote=" + ctx.channel().remoteAddress();
		        		((NettyConnectionReadCompletedCallback)callback).error(connection.getNetworkConnection(), readCtx, new SocketTimeoutException(ioeMessage));
		        	}else {
		        		if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
		        			SibTr.warning(tc, "userEventTriggered: Something's wrong. Callback, network connection, or read context is not netty specific. This shouldn't happen. Event will be ignored.", new Object[] {connection, callback, readCtx, networkConnection});
		                }
		        	}
		        	
		        } else {
		            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
		            	SibTr.warning(tc, "userEventTriggered", "could not associate an incoming event with a Connection. Event will be ignored.");
		            }
		        }
			}
		}else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
    			SibTr.debug(this,tc, "userEventTriggered: Event triggered was not a timeout. Not managing here.", evt);
		}
		super.userEventTriggered(ctx, evt);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "userEventTriggered", ctx.channel());
	}

	

}
