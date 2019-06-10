/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.util.ThreadContextAccessor;

/**
 * LibertyHTTPConduit extends HTTPConduit so that we can set the TCCL when run the handleResponseInternal asynchronously
 *
 * @TJJ Added unimplemented methods
 * TODO Implement unimplemented @Override methods
 */
public class LibertyHTTPConduit extends HTTPConduit {

    //save the bus so that we can get classloder from it.
    private final Bus bus;

    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = ThreadContextAccessor.getThreadContextAccessor();

    public LibertyHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
        this.bus = b;
    }

    // @TJJ changed method visibility from protected to public
    @Override
    public String getAddress() {
        return super.getAddress();
    }

    // @TJJ changed method visibility from protected to public
    @Override
    public void finalizeConfig() {
        super.finalizeConfig();
    }

    // @TJJ changed method visibility from protected to public
    public OutputStream createOutputStream(Message message, HttpURLConnection connection, boolean needToCacheRequest, boolean isChunking,
                                           int chunkThreshold) throws URISyntaxException {
        // @TJJ new HTTPCoundit.WrappedOutputSteam's constructor has changed and HttpURLConnection connection has been removed
        // and getURI() is called as the constuctor now requires URI url to be passed to super()
        return new LibertyWrappedOutputStream(message, needToCacheRequest, isChunking, chunkThreshold, getConduitName(), getURI());
    }

    // @TJJ added unimplemented methods from HTTPConduit.WrappedOutputStream
    protected class LibertyWrappedOutputStream extends HTTPConduit.WrappedOutputStream {

        protected LibertyWrappedOutputStream(Message outMessage, boolean possibleRetransmit, boolean isChunking, int chunkThreshold,
                                             String conduitName, URI url) {
            // @TJJ new HTTPCoundit.WrappedOutputSteam's constructor has changed and HttpURLConnection connection has been removed
            // and getURI() is called as the constuctor now requires URI url to be passed to super()
            super(outMessage, possibleRetransmit, isChunking, chunkThreshold, conduitName, url);
        }

        //handleResponse will call handleResponseInternal either synchronously or asynchronously
        //so if call asynchronously, we set the thread context classloader because liberty executor won't set anything when run the task.
        @Override
        protected void handleResponseInternal() throws IOException {
            if (outMessage == null
                || outMessage.getExchange() == null
                || outMessage.getExchange().isSynchronous()) {
                super.handleResponseInternal();
            } else {
                ClassLoader oldCl = THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread());
                try {
                    // get the classloader from bus
                    ClassLoader cl = bus.getExtension(ClassLoader.class);
                    if (cl != null) {
                        THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), cl);
                    }
                    super.handleResponseInternal();
                } finally {
                    THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), oldCl);
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#setupWrappedStream()
         */
        @Override
        protected void setupWrappedStream() throws IOException {
           

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#getHttpsURLConnectionInfo()
         */
        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
         
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#setProtocolHeaders()
         */
        @Override
        protected void setProtocolHeaders() throws IOException {
            

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#setFixedLengthStreamingMode(int)
         */
        @Override
        protected void setFixedLengthStreamingMode(int i) {
   

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#getResponseCode()
         */
        @Override
        protected int getResponseCode() throws IOException {
        
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#getResponseMessage()
         */
        @Override
        protected String getResponseMessage() throws IOException {
           
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#updateResponseHeaders(org.apache.cxf.message.Message)
         */
        @Override
        protected void updateResponseHeaders(Message inMessage) throws IOException {
         

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#handleResponseAsync()
         */
        @Override
        protected void handleResponseAsync() throws IOException {
            
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#closeInputStream()
         */
        @Override
        protected void closeInputStream() throws IOException {

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#usingProxy()
         */
        @Override
        protected boolean usingProxy() {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#getInputStream()
         */
        @Override
        protected InputStream getInputStream() throws IOException {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#getPartialResponse()
         */
        @Override
        protected InputStream getPartialResponse() throws IOException {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#setupNewConnection(java.lang.String)
         */
        @Override
        protected void setupNewConnection(String newURL) throws IOException {

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#retransmitStream()
         */
        @Override
        protected void retransmitStream() throws IOException {

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.transport.http.HTTPConduit.WrappedOutputStream#updateCookiesBeforeRetransmit()
         */
        @Override
        protected void updateCookiesBeforeRetransmit() throws IOException {

        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.cxf.io.AbstractThresholdOutputStream#thresholdReached()
         */
        @Override
        public void thresholdReached() throws IOException {

        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.cxf.transport.http.HTTPConduit#setupConnection(org.apache.cxf.message.Message, org.apache.cxf.transport.http.Address,
     * org.apache.cxf.transports.http.configuration.HTTPClientPolicy)
     */
    @Override
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.cxf.transport.http.HTTPConduit#createOutputStream(org.apache.cxf.message.Message, boolean, boolean, int)
     */
    @Override
    protected OutputStream createOutputStream(Message message, boolean needToCacheRequest, boolean isChunking, int chunkThreshold) throws IOException {
        return null;
    }
}
