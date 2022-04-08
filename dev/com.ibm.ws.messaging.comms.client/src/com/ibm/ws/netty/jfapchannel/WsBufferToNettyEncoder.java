package com.ibm.ws.netty.jfapchannel;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.richclient.buffer.impl.RichByteBufferImpl;
import com.ibm.ws.sib.utils.ras.SibTr;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class WsBufferToNettyEncoder extends MessageToByteEncoder<WsByteBuffer> {
	
	/** Trace */
    private static final TraceComponent tc = SibTr.register(WsBufferToNettyEncoder.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);
	
	/** Log class info on load */
    static
    {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/WsBufferToNettyEncoder.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }
	
	@Override
	public void encode(ChannelHandlerContext ctx, WsByteBuffer msg, ByteBuf out) throws Exception {
		if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "encode", ctx.channel());
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "encode", ctx.channel().remoteAddress() + " encoding message [ " + msg.toString() + " ] from WSByteBuffer to Netty ByteBuf");
		}
		com.ibm.wsspi.bytebuffer.WsByteBuffer wrappedBuffer = (com.ibm.wsspi.bytebuffer.WsByteBuffer)((RichByteBufferImpl)msg).getUnderlyingBuffer();
		out.writeBytes(wrappedBuffer.getWrappedByteBuffer());
		// TODO Check this reset if necessary or what it does
//		msg.reset();
		if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "encode", ctx.channel());
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "exceptionCaught", new Object[] {cause, ctx.channel()});
		super.exceptionCaught(ctx, cause);
		if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "exceptionCaught", new Object[] {cause, ctx.channel()});
	}

}
