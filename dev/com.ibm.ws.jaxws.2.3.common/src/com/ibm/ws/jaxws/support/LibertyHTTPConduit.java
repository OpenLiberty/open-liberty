/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 */
public class LibertyHTTPConduit extends HTTPConduit {

    //save the bus so that we can get classloder from it.
    private final Bus bus;

    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = ThreadContextAccessor.getThreadContextAccessor();

    public LibertyHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
        this.bus = b;
    }

    @Override
	public String getAddress() {
        return super.getAddress();
    }

    @Override
	public void finalizeConfig() {
        super.finalizeConfig();
    }

    protected OutputStream createOutputStream(Message message, HttpURLConnection connection, boolean needToCacheRequest, boolean isChunking, int chunkThreshold) throws IOException {
        try {
    			return new LibertyWrappedOutputStream(message, connection, needToCacheRequest, isChunking, chunkThreshold, getConduitName());
    
        } catch (URISyntaxException e) {
        	 	throw new IOException(e);
        }
    }

    // @TJJ Added to allow for change to HTTPConduit.WrappedOutputStream constructor. 
    private static URI findURI(Message outMessage, HttpURLConnection connection) throws URISyntaxException {
        Address add = (Address)outMessage.get(KEY_HTTP_CONNECTION_ADDRESS);
        return add != null ? add.getURI() : connection.getURL().toURI();
    }
    
    protected class LibertyWrappedOutputStream extends HTTPConduit.WrappedOutputStream {

        protected LibertyWrappedOutputStream(Message outMessage, HttpURLConnection connection, boolean possibleRetransmit, boolean isChunking, int chunkThreshold,
                                             String conduitName) throws URISyntaxException {

            super(outMessage, possibleRetransmit, isChunking,
                    chunkThreshold, conduitName,
                    findURI(outMessage, connection));
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

		@Override
		protected void closeInputStream() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected InputStream getInputStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected InputStream getPartialResponse() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected int getResponseCode() throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		protected String getResponseMessage() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void handleResponseAsync() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void retransmitStream() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void setFixedLengthStreamingMode(int arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void setProtocolHeaders() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void setupNewConnection(String arg0) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void setupWrappedStream() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void updateCookiesBeforeRetransmit() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void updateResponseHeaders(Message arg0) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected boolean usingProxy() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void thresholdReached() throws IOException {
			// TODO Auto-generated method stub
			
		}

    }

	@Override
	protected OutputStream createOutputStream(Message arg0, boolean arg1, boolean arg2, int arg3) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setupConnection(Message arg0, Address arg1, HTTPClientPolicy arg2) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
