/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.srt;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.ServletConnection;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.webcontainer40.srt.SRTServletRequest40;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer60.osgi.srt.SRTConnectionContext60;

public class SRTServletRequest60 extends SRTServletRequest40 implements HttpServletRequest {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer60.srt");
    private static final String CLASS_NAME = SRTServletRequest60.class.getName();
    
    private static long startID = 1;
    private static AtomicLong counter = new AtomicLong(startID);

    public SRTServletRequest60(SRTConnectionContext60 context) {
        super(context);
    }
    
    @Override
    public void initForNextRequest(IRequest req) {
        String methodName = "initForNextRequest";

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "this [" + this + "] , req [" + req +"]");
        }
        super.initForNextRequest(req);
        
        if (req != null) {
            super.setSrtRequestId(String.valueOf(counter.getAndIncrement()));

            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "this [" + this + "] , requestID [" + this.getRequestId() +"]");
            }
            
            this.setupServletConnection();
        }
    }
    
    private void setupServletConnection() {
        SRTServletConnection servletConn = new SRTServletConnection();
        servletConn.setConnectionID(this.getRequestId());
        
        super.setSrtServletConnection(servletConn);
    }
    
    /*
     * since: servlet 6.0
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getRequestId()
     */
    @Override
    public String getRequestId() {
        String id = super.getSrtRequestId();
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getRequestId", "this [" + this + "] , requestID ["+ id + "]");
        } 
        return id;
    }
    
    /*
     * since: servlet 6.0
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getProtocolRequestId()
     */
    @Override
    public String getProtocolRequestId() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getProtocolRequestId", "this [" + this + "]");
        } 
        
        //to be implemented
        return null;
    }
    
    /*
     * since: servlet 6.0
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getServletConnection()
     */
    @Override
    public ServletConnection getServletConnection() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getServletConnection", "this [" + this + "]");
        } 
        
        return (ServletConnection) super.getSrtServletConnection();
    }
}
