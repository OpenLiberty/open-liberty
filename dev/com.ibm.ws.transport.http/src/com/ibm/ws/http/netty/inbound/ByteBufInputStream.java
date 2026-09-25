package com.ibm.ws.http.netty.inbound;

import java.io.IOException;
import java.io.InputStream;

import com.ibm.ws.http.netty.message.BodyQueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ByteBufInputStream extends InputStream{

    private final BodyQueue queue;
    private final ChannelHandlerContext context;
    private final boolean autoRead;
    private ByteBuf current;

    public ByteBufInputStream(BodyQueue queue, ChannelHandlerContext context, boolean autoRead){
        this.queue = queue;
        this.context = context;
        this.autoRead = autoRead;
    }

    @Override
    public int read() throws IOException{
        byte[] b = new byte[1];
        int n = read(b, 0, 1);
        return (n<0) ? -1 : (b[0] & 0xFF);
    }

    @Override 
    public int read(byte[] b, int off, int len) throws IOException{
        for(;;){
            if(current != null && current.isReadable()){
                int x = Math.min(len, current.readableBytes());
                current.readBytes(b,off, x);
                if(!current.isReadable()){
                    current.release();
                    current = null;
                }
                return x;
            }
            current = queue.poll();
            if(current !=null){
                continue;
            }
            if(queue.isEos()){
                return -1;
            }
            if(!autoRead && queue.wantsInput() && context != null){
                context.read();
            }
            try{
                Thread.sleep(1);
            }catch(InterruptedException ie){
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public int available(){
        return (current !=null) ? current.readableBytes(): 0;
    }

    @Override
    public void close() throws IOException{
        if (current != null){
            current.release();
            current = null;
        }
    }

}
