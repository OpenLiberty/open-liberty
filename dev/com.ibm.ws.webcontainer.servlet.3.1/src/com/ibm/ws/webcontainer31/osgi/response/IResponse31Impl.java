/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.osgi.response;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet31.response.IResponse31;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.webcontainer.osgi.response.IResponseImpl;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.ee7.HttpInboundConnectionExtended;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Implementation of a servlet response wrapping the HTTP dispatcher provided
 * response message.
 */
public class IResponse31Impl extends IResponseImpl implements IResponse31
{
    private TCPConnectionContext tcc = null;
    private VirtualConnection vc = null;
    private ConnectionLink deviceConnLink, connLink, dispatcherLink = null;    
    
    /**
     * Constructor.
     * 
     * @param req
     * @param connection
     */
    public IResponse31Impl(IRequest req, HttpInboundConnection inConnection)
    {
        super(req, inConnection);
        
        HttpInboundConnectionExtended connection = (HttpInboundConnectionExtended) inConnection;
        this.tcc = connection.getTCPConnectionContext();
        this.vc  = connection.getVC();
        this.deviceConnLink = connection.getHttpInboundDeviceLink();
        this.connLink = connection.getHttpInboundLink();
        this.dispatcherLink = connection.getHttpDispatcherLink();
    }
    
    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {     
        if (null == this.outStream)
        {
            this.outStream = new WCOutputStream31((HttpOutputStreamConnectWeb) this.response.getBody(), this.request);                                      
             }
        return this.outStream;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.websphere.servlet.response.IResponse#flushHeaders()
     */
    public void flushHeaders() throws IOException
      {
          this.response.getBody().flushHeaders();
      }
    
    /**
     * Sets the length of the content body in the response In HTTP servlets, this method sets the HTTP Content-Length header.
     * @param length
     */
    public void setContentLengthLong(long length) {
        
        this.response.setContentLength(length);
    }
    
    /**
     * @return the connLink
     */
    public ConnectionLink getConnLink() {
        return connLink;
    }
    public TCPConnectionContext getTCPConnectionContext() {
        return tcc;
    }

    public VirtualConnection getVC() {
        return vc;
    }

    public ConnectionLink getDeviceConnectionLink() {
        return deviceConnLink;
    }
    
    public ConnectionLink getHttpDispatcherLink() {
        return dispatcherLink;
    }
    

}
