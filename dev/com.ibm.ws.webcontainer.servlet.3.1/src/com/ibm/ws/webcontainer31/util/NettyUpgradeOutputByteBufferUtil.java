/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer31.util;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.upgrade.NettyUpgradeReadCallBack;
import com.ibm.ws.webcontainer31.upgrade.UpgradedWebConnectionImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.webcontainer31.WCCustomProperties31;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.WriteTimeoutHandler;

/**
 *
 */
public class NettyUpgradeOutputByteBufferUtil extends UpgradeOutputByteBufferUtil {
    
    private final static TraceComponent tc = Tr.register(NettyUpgradeOutputByteBufferUtil.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private Channel nettychannel;

    private NettyServletUpgradeHandler upgradeHandler;


    /**
     * @param up
     */
    public NettyUpgradeOutputByteBufferUtil(UpgradedWebConnectionImpl up, Channel channel) {
        super(up);
        // TODO Auto-generated constructor stub
        this.nettychannel = channel;
        upgradeHandler = channel.pipeline().get(NettyServletUpgradeHandler.class);
        if(upgradeHandler == null) {
            throw new UnsupportedOperationException("Can't work without upgrade handler!!");
        }
        System.out.println("Adding write timeout handler with timeout: " + WCCustomProperties31.UPGRADE_WRITE_TIMEOUT);
        channel.pipeline().addBefore(channel.pipeline().context(upgradeHandler).name(), null, new WriteTimeoutHandler((WCCustomProperties31.UPGRADE_WRITE_TIMEOUT<0)?0:WCCustomProperties31.UPGRADE_WRITE_TIMEOUT, TimeUnit.MILLISECONDS) {
            @Override
            protected void writeTimedOut(ChannelHandlerContext ctx) throws Exception {
                System.out.println("Write timeout!!");
                if(getWriteListenerCallBack() != null) {
                    System.out.println("Calling callback for write timeout!");
                    getWriteListenerCallBack().error(get_vc(), null, new SocketTimeoutException("Socket operation timed out before it could be completed local="+ctx.channel().localAddress()+" remote="+ctx.channel().remoteAddress()));
                }
                System.out.println("Finished write timeout!!");
            }
        });
    }

    // Similar to writeToBuffers
    @Override
    protected void flushUpgradedOutputBuffers() throws IOException{
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "flushUpgraded: Flushing buffers for Upgraded output: " + this);
        }
        final boolean writingBody = (super.hasBufferedContent());
        // flip the last buffer for the write...
        if (writingBody && null != super._output[super.outputIndex]) {
            super._output[super.outputIndex].flip();
        }
        try {
            WsByteBuffer[] content = (writingBody) ? super._output : null;
            // write it out to TCP            
            if(content != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushUpgraded:: Now write the content ");
                }
                System.out.println(WsByteBufferUtils.asString(content));
                // Synchronous write
                ChannelFuture future = null;
                for(WsByteBuffer buff : content) {
                    // Bad since this is duplicating data
                    if(buff == null) { // In TCPBaseRequestContext when setting buffers it sets until the first null buffer
                        System.out.println("Found null buffer! Stopping write!");
                        break;
                    }
                    if(buff.remaining() == 0) { // No bytes to write
                        System.out.println("No bytes to write, continuing!");
                        continue;
                    }
                    System.out.println("Writing content!: "+WsByteBufferUtils.asString(buff));
                    // Skip every other handler and write from Upgrade Handler
                    future = this.nettychannel.pipeline().context(upgradeHandler).writeAndFlush(Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buff)));
                }
                if(future != null)// else nothing to write
                    future.await(WCCustomProperties31.UPGRADE_WRITE_TIMEOUT);
            }
            else{
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushUpgraded: No more data to flush ");
                } 
            }

        } catch (InterruptedException ie) {
            super.error = new IOException(ie);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "flushUpgraded: Received exception during write: " + ie);
            }
            throw super.error;
        } finally {
            super.bytesWritten += super.bufferedCount;            
            super.bufferedCount = 0;
            super.outputIndex = 0;
            // Note: this logic only works for sync writes
            if (writingBody) {
                if (null != super._output){
                    if (null != super._output[0]) {
                        super._output[0].clear();
                    }
                    for (int i = 1; i < super._output.length; i++) {
                        if (null != super._output[i]) {
                            // mark them empty so later writes don't mistake them
                            // as having content
                            super._output[i].position(0);
                            super._output[i].limit(0);
                        }
                    }
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "flushUpgraded: disconnect write buffers in TCP when done");
            }
        }
    }

    @Override
    protected void flushAsyncUpgradedOutputBuffers() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "flushAsyncUpgraded: Flushing async buffers  for Upgraded output: " + this);
        }

        final boolean writingBody = (super.hasBufferedContent());
        // flip the last buffer for the write...
        if (writingBody && null != super._output[super.outputIndex]) {
            super._output[super.outputIndex].flip();
        }
//        VirtualConnection _vcWrite = null;
        ChannelFuture writeFuture = null;
        boolean wentAsync = false;
        try {
            WsByteBuffer[] content = (writingBody) ? super._output : null;
            // write it out to TCP           
            if(content != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushAsyncUpgraded: Now write it out to TCP");
                }
                System.out.println(WsByteBufferUtils.asString(content));

                // Async write
                ChannelFuture future = null;
                for(WsByteBuffer buff : content) {
                    // Bad since this is duplicating data
                    if(buff == null) { // In TCPBaseRequestContext when setting buffers it sets until the first null buffer
                        System.out.println("Found null buffer! Stopping write!");
                        break;
                    }
                    if(buff.remaining() == 0) { // No bytes to write
                        System.out.println("No bytes to write, continuing!");
                        continue;
                    }
                    System.out.println("Writing content!: "+WsByteBufferUtils.asString(buff));
                    // Skip every other handler and write from Upgrade Handler
                    future = this.nettychannel.pipeline().context(upgradeHandler).writeAndFlush(Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buff)));
                }
                if(future != null){// else nothing to write
                    if(!future.isDone()) {
                        System.out.println("Not done yet! Need to add listener and set not ready");
                        wentAsync = true;
                        this.setInternalReady(false); // tell internal
                        this.setReadyForApp(false); // tell app
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "flushAsyncUpgraded:  wait for data to be written, async write in progress, set ready to false");
                        }
                        future.addListener(f -> {
                            if(f.isDone() && f.isSuccess()) {
                                if(getWriteListenerCallBack() != null)
                                    getWriteListenerCallBack().complete(get_vc(), null);
                                else
                                    System.out.println("Callback completed but no callback called");
                            }else {
                                if(getWriteListenerCallBack() != null)
                                    getWriteListenerCallBack().error(get_vc(), null, new IOException(f.cause()));
                                else
                                    System.out.println("Callback failed but no callback called");
                            }
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "flushAsyncUpgraded:  data already written, set ready to true");
                            }
                        });
                    }else {
                        System.out.println("All data was flushed instantly!");
                    }

                }
            }
            else{
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushAsyncUpgraded: No more data to flush ");
                } 
            }        
        } finally {
            super.bytesWritten += super.bufferedCount;            
            super.bufferedCount = 0;
            super.outputIndex = 0;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "flushAsyncUpgraded: finally, this " + this + " , bytesWritten -->" + super.bytesWritten);
            }
            if (writingBody && !wentAsync) {
                super.clearBuffersAfterWrite();
            }
        }
    }
    
}
