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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.servlet.ReadListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeInputStream31;
import com.ibm.ws.webcontainer31.upgrade.NettyUpgradeReadCallBack;
import com.ibm.ws.webcontainer31.upgrade.UpgradedWebConnectionImpl;
import com.ibm.wsspi.webcontainer31.WCCustomProperties31;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 *
 */
public class NettyUpgradeInputByteBufferUtil extends UpgradeInputByteBufferUtil {
    
    private static final TraceComponent tc = Tr.register(NettyUpgradeInputByteBufferUtil.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    
    //Netty channel from the WebConnection which we use to do our reads
    private Channel nettyChannel;
    private NettyServletUpgradeHandler upgradeHandler;

    /**
     * @param up
     */
    public NettyUpgradeInputByteBufferUtil(UpgradedWebConnectionImpl up, Channel channel) {
        super(up);
        this.nettyChannel = channel;
        NettyServletUpgradeHandler upgradeHandler = channel.pipeline().get(NettyServletUpgradeHandler.class);
        if(upgradeHandler == null) {
            throw new UnsupportedOperationException("Can't work without upgrade handler!!");
        }
        this.upgradeHandler = upgradeHandler;
        super._isFirstRead = false;
        NettyUpgradeInputByteBufferUtil parent = this;
        channel.pipeline().addBefore(channel.pipeline().context(upgradeHandler).name(), null, new ReadTimeoutHandler((WCCustomProperties31.UPGRADE_READ_TIMEOUT<0)?0:WCCustomProperties31.UPGRADE_READ_TIMEOUT, TimeUnit.MILLISECONDS) {
            @Override
            protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
                System.out.println("Read timeout!!");
                if(parent.get_tcpChannelCallback() != null) {
                    System.out.println("Calling callback for read timeout!");
                    parent.get_tcpChannelCallback().error(null, null, new SocketTimeoutException("Socket operation timed out before it could be completed local="+ctx.channel().localAddress()+" remote="+ctx.channel().remoteAddress()));
                }
                System.out.println("Finished read timeout!!");
            }
        });
    }
    
    // Not valid in Netty Context

    @Override
    public void initialRead() {
        throw new UnsupportedOperationException("initialRead not supported in Netty context");
    }

    @Override
    public void configurePostInitialReadBuffer() {
        throw new UnsupportedOperationException("configurePostInitialReadBuffer not supported in Netty context");
    }

    @Override
    public boolean isInitialRead() {
        throw new UnsupportedOperationException("configurePostInitialReadBuffer not supported in Netty context");
    }

    @Override
    public void setIsInitialRead(boolean isInitialRead) {
        // TODO Auto-generated method stub
        System.out.println("Called in Netty context!!");
        super.setIsInitialRead(isInitialRead);
    }
    
    // Overridden to work with Netty
    
    @Override
    protected void validate() throws IOException {
        if (null != super._error) {
            throw super._error;
        }
        // TODO Check this isreadline part

////        if(!super._isReadLine && !isReady()){ //isReady gives issue, just need to read and block until available
//        if(!super._isReadLine){
//            //If there is no data available then isReady will have returned false and this throw an IllegalStateException
//            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
//                Tr.error(tc, "read.failed.isReady.false");
//            throw new IllegalStateException(Tr.formatMessage(tc, "read.failed.isReady.false"));
//        }
    }
    
    @Override
    public boolean isReady() {
//        return !queue.isEmpty();
        return upgradeHandler.containsQueuedData();
    }

    public int read() throws IOException {
        validate();
        int rc = -1;

//        rc = queue.remove(1, new VoidChannelPromise(nettyChannel, true)).getByte(0) & 0x000000FF;
        if(!upgradeHandler.containsQueuedData()) { // Block until data becomes available
            try {
                System.out.println("Waiting because data isn't available yet!");
                upgradeHandler.waitForDataRead(WCCustomProperties31.UPGRADE_READ_TIMEOUT);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // Read timeout!!
                e.printStackTrace();
                throw new SocketTimeoutException("Socket operation timed out before it could be completed local="+nettyChannel.localAddress()+" remote="+nettyChannel.remoteAddress());
            }
        }
        rc = upgradeHandler.read(1, null).getByte(0) & 0x000000FF;

        return rc;
    }

    public int read(byte[] output, int offset, int length) throws IOException {

        int size = -1;
        validate();

        if (0 == length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "read(byte[],int,int), Target length was 0");
            }
            return length;
        }

        if(!upgradeHandler.containsQueuedData()) { // Block until data becomes available
            try {
                System.out.println("Waiting because data isn't available yet!");
                upgradeHandler.waitForDataRead(WCCustomProperties31.UPGRADE_READ_TIMEOUT);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // Read timeout!!
                e.printStackTrace();
                throw new SocketTimeoutException("Socket operation timed out before it could be completed local="+nettyChannel.localAddress()+" remote="+nettyChannel.remoteAddress());
            }
        }
        ByteBuf buffer = upgradeHandler.read(length, null);
        size = buffer.readableBytes();

        System.out.println("Size: "+size);
        System.out.println("Offset: "+offset);
        System.out.println("Length: "+length);
        System.out.println("Buffer data! "+ByteBufUtil.hexDump(buffer));

        System.out.println("Buffer data after get bytes! "+ByteBufUtil.hexDump(buffer.readBytes(output, offset, size)));

        System.out.println("Copied: " + Arrays.toString(output));


        return size;
    }

    public void setupReadListener(ReadListener readListenerl, SRTUpgradeInputStream31 srtUpgradeStream){

        System.out.println("Setting up read listener!");

        if(readListenerl == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "readlistener.is.null");
            throw new NullPointerException(Tr.formatMessage(tc, "readlistener.is.null"));
        }
        if(super._rl != null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "readlistener.already.started");
            throw new IllegalStateException(Tr.formatMessage(tc, "readlistener.already.started"));

        }

        //Save off the current Thread data by creating the ThreadContextManager. Then pass it into the callback       
        ThreadContextManager tcm = new ThreadContextManager();

        super._tcpChannelCallback = new NettyUpgradeReadCallBack(readListenerl, this, tcm, srtUpgradeStream);
        super._rl = readListenerl;
        this.upgradeHandler.setReadListener(super._tcpChannelCallback);
        super._upConn.getVirtualConnection().getStateMap().put(TransportConstants.UPGRADED_LISTENER, "true");



    }    
    
}
