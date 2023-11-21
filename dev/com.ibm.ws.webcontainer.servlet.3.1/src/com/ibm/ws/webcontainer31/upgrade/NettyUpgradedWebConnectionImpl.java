/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer31.upgrade;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import com.ibm.ws.webcontainer31.srt.SRTUpgradeInputStream31;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeOutputStream31;
import com.ibm.ws.webcontainer31.util.NettyUpgradeInputByteBufferUtil;
import com.ibm.ws.webcontainer31.util.NettyUpgradeOutputByteBufferUtil;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

import io.netty.channel.Channel;

/**
 *
 */
public class NettyUpgradedWebConnectionImpl extends UpgradedWebConnectionImpl {
    
    private Channel nettyChannel;
    
    public NettyUpgradedWebConnectionImpl(IExtendedRequest req, IExtendedResponse res, HttpUpgradeHandlerWrapper upgradeHandler, Channel channel) {
        super(req, upgradeHandler);
        this.nettyChannel = channel;
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.WebConnection#getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {    

        if(super._in == null){

            //create the inputstream 
            super._in = new SRTUpgradeInputStream31();       
            super._inbb = new NettyUpgradeInputByteBufferUtil(this, nettyChannel);
            super._in.init(super._inbb);
        }
        return super._in;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.WebConnection#getOutputStream()
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {

        if(_out == null ){
            //create the outputstream 
            _out = new SRTUpgradeOutputStream31();       
            _outbb = new NettyUpgradeOutputByteBufferUtil(this, nettyChannel);
            _out.init(_outbb,super._req);
        }
        return _out;
    }

    @Override
    public TCPConnectionContext getTCPConnectionContext() {
        throw new UnsupportedOperationException("getTCPConnectionContext not valid in Netty context!!");
    }

    @Override
    protected void closeOutputandConnection() {
        // TODO Auto-generated method stub
        super.closeOutputandConnection();
//        System.out.println("Finished! Closing connection!");
//        this.nettyChannel.close();
    }
    
    
}
