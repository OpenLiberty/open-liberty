package com.ibm.ws.http.netty.pipeline;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class CRLFValidationHandler extends ChannelInboundHandlerAdapter {

    private static final int MAX_CRLF_ALLOWED = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof ByteBuf){
            ByteBuf buffer = (ByteBuf) msg;
            buffer.markReaderIndex();

            int leadingCRFLCount = 0;
            boolean nonCRLFFound = false;
            byte b;
            byte nextByte;

            while(buffer.isReadable() && !nonCRLFFound){
                b = buffer.readByte();

                if(b == '\r'){
                    if(buffer.isReadable()){
                        nextByte = buffer.readByte();
                        if(nextByte == '\n'){
                            leadingCRFLCount++;
                            if (leadingCRFLCount > MAX_CRLF_ALLOWED){
                                throw new IllegalArgumentException("Too many leading CRLF characters");
                            }
                        } else {
                            nonCRLFFound = true;
                            buffer.readerIndex(buffer.readerIndex() -1);
                        }
                    } else{
                        nonCRLFFound = true;
                    }
                } else{
                    nonCRLFFound = true;
                }
            }

            buffer.resetReaderIndex();
            super.channelRead(ctx, msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }


}
