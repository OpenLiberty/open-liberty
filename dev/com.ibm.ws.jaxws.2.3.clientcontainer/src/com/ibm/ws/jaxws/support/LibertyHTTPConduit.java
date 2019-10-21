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
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.net.ssl.HttpsURLConnection;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.ChunkedUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * LibertyHTTPConduit extends HTTPConduit so that we can set the TCCL when run the handleResponseInternal asynchronously
 */
public class LibertyHTTPConduit extends HTTPConduit {

    // <Ported from URLConnectionHTTPConduit class>
    // Most of these variables are used at setupConnection method of this class
    public static final String HTTPURL_CONNECTION_METHOD_REFLECTION = "use.httpurlconnection.method.reflection";
    public static final String SET_REASON_PHRASE_NOT_NULL = "set.reason.phrase.not.null";
    private static final boolean DEFAULT_USE_REFLECTION;
    private static final boolean SET_REASON_PHRASE;
    static {
        DEFAULT_USE_REFLECTION = Boolean.valueOf(SystemPropertyAction.getProperty(HTTPURL_CONNECTION_METHOD_REFLECTION, "false"));
        SET_REASON_PHRASE = Boolean.valueOf(SystemPropertyAction.getProperty(SET_REASON_PHRASE_NOT_NULL, "false"));
    }

    /**
     * This field holds the connection factory, which primarily is used to
     * factor out SSL specific code from this implementation.
     * <p>
     * This field is "protected" to facilitate some contrived UnitTesting so
     * that an extended class may alter its value with an EasyMock URLConnection
     * Factory.
     */
    protected HttpsURLConnectionFactory connectionFactory;
    // End of <Ported from URLConnectionHTTPConduit class>

    //save the bus so that we can get classloder from it.
    private final Bus bus;

    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    public LibertyHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
        this.bus = b;
        // Ported from URLConnectionHTTPConduit class to create a connection at createConnection method
        // since new version of CXF does not allow to pass HttpURLConnection trough createOutputStream method
        connectionFactory = new HttpsURLConnectionFactory();
    }

    @Override
    public String getAddress() {
        return super.getAddress();
    }

    @Override
    public void finalizeConfig() {
        super.finalizeConfig();
    }

    protected class LibertyWrappedOutputStream extends HTTPConduit.WrappedOutputStream {
        // Ported from URLConnectionHTTPConduit class
        HttpURLConnection connection;

        protected LibertyWrappedOutputStream(Message outMessage, HttpURLConnection connection, boolean possibleRetransmit, boolean isChunking, int chunkThreshold,
                                             String conduitName) {
            super(outMessage, possibleRetransmit, isChunking, chunkThreshold, conduitName, computeURI(outMessage, connection));
            // Ported from URLConnectionHTTPConduit class. With new CXF connection is being stored at sub class of HTTPConduit class
            this.connection = connection;
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
                try {
                    // get the classloader from bus
                    ClassLoader cl = bus.getExtension(ClassLoader.class);
                    if (cl != null) {
                        THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), cl);
                    }
                    super.handleResponseInternal();
                } finally {
                    ClassLoader oldCl = THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread());
                    THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), oldCl);
                }
            }
        }

        @Override
        @FFDCIgnore({ PrivilegedActionException.class, ProtocolException.class, SocketException.class })
        protected void setupWrappedStream() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            // If we need to cache for retransmission, store data in a
            // CacheAndWriteOutputStream. Otherwise write directly to the output stream.
            OutputStream cout = null;
            try {
                try {
//                    cout = connection.getOutputStream();
                    if (System.getSecurityManager() != null) {
                        try {
                            cout = AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                                @Override
                                public OutputStream run() throws IOException {
                                    return connection.getOutputStream();
                                }
                            });
                        } catch (PrivilegedActionException e) {
                            throw (IOException) e.getException();
                        }
                    } else {
                        cout = connection.getOutputStream();
                    }
                } catch (ProtocolException pe) {
                    Boolean b = (Boolean) outMessage.get(HTTPURL_CONNECTION_METHOD_REFLECTION);
                    cout = connectAndGetOutputStream(b);
                }
            } catch (SocketException e) {
                if ("Socket Closed".equals(e.getMessage())
                    || "HostnameVerifier, socket reset for TTL".equals(e.getMessage())) {
                    connection.connect();
                    cout = connectAndGetOutputStream((Boolean) outMessage.get(HTTPURL_CONNECTION_METHOD_REFLECTION));
                } else {
                    throw e;
                }
            }
            if (cachingForRetransmission) {
                cachedStream = new CacheAndWriteOutputStream(cout);
                wrappedStream = cachedStream;
            } else {
                wrappedStream = cout;
            }
        }

        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            connection.connect();
            return new HttpsURLConnectionInfo(connection);
        }

        @Override
        protected void setProtocolHeaders() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            new Headers(outMessage).setProtocolHeadersInConnection(connection);
        }

        @Override
        protected void setFixedLengthStreamingMode(int i) {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            // [CXF-6227] do not call connection.setFixedLengthStreamingMode(i)
            // to prevent https://bugs.openjdk.java.net/browse/JDK-8044726
        }

        @Override
        @FFDCIgnore(PrivilegedActionException.class)
        protected int getResponseCode() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Integer>() {

                    @Override
                    public Integer run() throws IOException {
                        return connection.getResponseCode();
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof IOException) {
                    throw (IOException) t;
                }
                throw new RuntimeException(t);
            }
        }

        @Override
        protected String getResponseMessage() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            boolean b = MessageUtils.getContextualBoolean(this.outMessage,
                                                          SET_REASON_PHRASE_NOT_NULL,
                                                          SET_REASON_PHRASE);
            if (connection.getResponseMessage() == null && b) {
                //some http server like tomcat 8.5+ won't return the
                //reason phrase in response, return a informative value
                //to tell user no reason phrase in the response instead of null
                return "no reason phrase in the response";
            } else {
                return connection.getResponseMessage();
            }
        }

        @Override
        protected void updateResponseHeaders(Message inMessage) throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            Headers h = new Headers(inMessage);
            h.readFromConnection(connection);
            inMessage.put(Message.CONTENT_TYPE, connection.getContentType());
            cookies.readFromHeaders(h);
        }

        @Override
        protected void handleResponseAsync() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            handleResponseOnWorkqueue(true, false);
        }

        @Override
        protected void closeInputStream() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            //try and consume any content so that the connection might be reusable
            InputStream ins = connection.getErrorStream();
            if (ins == null) {
                ins = connection.getInputStream();
            }
            if (ins != null) {
                IOUtils.consume(ins);
                ins.close();
            }
        }

        @Override
        protected boolean usingProxy() {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            return connection.usingProxy();
        }

        @Override
        @FFDCIgnore(IOException.class)
        protected InputStream getInputStream() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            InputStream in = null;
            if (getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                in = connection.getErrorStream();
                if (in == null) {
                    try {
                        // just in case - but this will most likely cause an exception
                        in = connection.getInputStream();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            } else {
                in = connection.getInputStream();
            }
            return in;
        }

        @Override
        protected InputStream getPartialResponse() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            return ChunkedUtil.getPartialResponse(connection, connection.getResponseCode());
        }

        @Override
        @FFDCIgnore(URISyntaxException.class)
        protected void setupNewConnection(String newURL) throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            HTTPClientPolicy cp = getClient(outMessage);
            Address address;
            try {
                if (defaultAddress.getString().equals(newURL)) {
                    address = defaultAddress;
                } else {
                    address = new Address(newURL);
                }
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            setupConnection(outMessage, address, cp);
            this.url = address.getURI();
            connection = (HttpURLConnection) outMessage.get(KEY_HTTP_CONNECTION);
        }

        @Override
        protected void retransmitStream() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            Boolean b = (Boolean) outMessage.get(HTTPURL_CONNECTION_METHOD_REFLECTION);
            OutputStream out = connectAndGetOutputStream(b);
            cachedStream.writeCacheTo(out);
        }

        @Override
        protected void updateCookiesBeforeRetransmit() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            Headers h = new Headers();
            h.readFromConnection(connection);
            cookies.readFromHeaders(h);
        }

        @Override
        public void thresholdReached() throws IOException {
            // This method came with HTTPConduit class of new CXF version as abstract
            // Implementation is copied from URLConnectionHTTPConduit class
            // Ported from URLConnectionHTTPConduit class
            if (chunking) {
                this.connection.setChunkedStreamingMode(LibertyHTTPConduit.this.getClient().getChunkLength());
            }
        }

        @FFDCIgnore(Throwable.class)
        private OutputStream connectAndGetOutputStream(Boolean b) throws IOException {
            // This method does not exist in original liberty version
            // Ported from URLConnectionHTTPConduit class
            OutputStream cout = null;

            if (b != null && b) {
                String method = connection.getRequestMethod();
                connection.connect();
                try {
                    java.lang.reflect.Field f = ReflectionUtil.getDeclaredField(HttpURLConnection.class, "method");
                    ReflectionUtil.setAccessible(f).set(connection, "POST");
                    cout = connection.getOutputStream();
                    ReflectionUtil.setAccessible(f).set(connection, method);
                } catch (Throwable t) {
                    logStackTrace(t);
                }

            } else {
                cout = connection.getOutputStream();
            }
            return cout;
        }
    }

    private static URI computeURI(Message message, HttpURLConnection connection) {
        // This helper method does not exist in original liberty version
        // Ported from URLConnectionHTTPConduit class
        URI _uri = null;
        try {
            Address address = (Address) message.get(KEY_HTTP_CONNECTION_ADDRESS);
            _uri = address != null ? address.getURI() : connection.getURL().toURI();
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
        return _uri;
    }

    private HttpURLConnection createConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
        // This helper method does not exist in original liberty version
        // Ported from URLConnectionHTTPConduit class
        URL url = address.getURL();
        URI uri = address.getURI();
        Proxy proxy = proxyFactory.createProxy(csPolicy, uri);
        message.put("http.scheme", uri.getScheme());
        // check tlsClientParameters from message header
        TLSClientParameters clientParameters = message.get(TLSClientParameters.class);
        if (clientParameters == null) {
            clientParameters = tlsClientParameters;
        }
        return connectionFactory.createConnection(clientParameters,
                                                  proxy != null ? proxy : address.getDefaultProxy(), url);
    }

    @Override
    @FFDCIgnore({ java.net.ProtocolException.class, Throwable.class, Throwable.class })
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
        // This method is not implemented in original liberty version
        // Ported from URLConnectionHTTPConduit class
        HttpURLConnection connection = createConnection(message, address, csPolicy);
        connection.setDoOutput(true);

        int ctimeout = determineConnectionTimeout(message, csPolicy);
        connection.setConnectTimeout(ctimeout);

        int rtimeout = determineReceiveTimeout(message, csPolicy);
        connection.setReadTimeout(rtimeout);

        connection.setUseCaches(false);
        // We implement redirects in this conduit. We do not
        // rely on the underlying URLConnection implementation
        // because of trust issues.
        connection.setInstanceFollowRedirects(false);

        // If the HTTP_REQUEST_METHOD is not set, the default is "POST".
        String httpRequestMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, "POST");
        }
        try {
            connection.setRequestMethod(httpRequestMethod);
        } catch (java.net.ProtocolException ex) {
            boolean b = MessageUtils.getContextualBoolean(message,
                                                          HTTPURL_CONNECTION_METHOD_REFLECTION,
                                                          DEFAULT_USE_REFLECTION);
            if (b) {
                try {
                    java.lang.reflect.Field f = ReflectionUtil.getDeclaredField(HttpURLConnection.class, "method");
                    if (connection instanceof HttpsURLConnection) {
                        try {

                            java.lang.reflect.Field f2 = ReflectionUtil.getDeclaredField(connection.getClass(),
                                                                                         "delegate");
                            if (f2 == null) {
                                for (java.lang.reflect.Field field : ReflectionUtil.getDeclaredFields(connection.getClass())) {

                                    if (HttpURLConnection.class.isAssignableFrom(field.getType())) {
                                        f2 = field;
                                        break;
                                    }
                                }
                            }
                            Object c = null;
                            if (f2 != null) { // delegate field may not exist for all JDKs
                                c = ReflectionUtil.setAccessible(f2).get(connection);
                                if (c instanceof HttpURLConnection) {
                                    ReflectionUtil.setAccessible(f).set(c, httpRequestMethod);
                                }
                            }

                            if (c != null) {
                                f2 = ReflectionUtil.getDeclaredField(c.getClass(), "httpsURLConnection");
                            }
                            if (f2 != null && c != null) { // httpsURLConnection may not exist for all field
                                HttpsURLConnection c2 = (HttpsURLConnection) ReflectionUtil.setAccessible(f2).get(c);

                                ReflectionUtil.setAccessible(f).set(c2, httpRequestMethod);
                            }
                        } catch (Throwable t) {
                            //ignore
                            logStackTrace(t);
                        }
                    }
                    ReflectionUtil.setAccessible(f).set(connection, httpRequestMethod);
                    message.put(HTTPURL_CONNECTION_METHOD_REFLECTION, true);
                } catch (Throwable t) {
                    logStackTrace(t);
                    throw ex;
                }
            } else {
                throw ex;
            }
        }

        // We place the connection on the message to pick it up
        // in the WrappedOutputStream.
        message.put(KEY_HTTP_CONNECTION, connection);
        message.put(KEY_HTTP_CONNECTION_ADDRESS, address);
    }

    @Override
    // @FFDCIgnore(URISyntaxException.class)
    protected OutputStream createOutputStream(Message message, boolean needToCacheRequest, boolean isChunking, int chunkThreshold) throws IOException {
        // This method is not implemented in original liberty version
        // It's copied over from URLConnectionHTTPConduit as many of other method implementations
        // With the new CXF, HttpURLConnection class parameter is removed from LibertyWrappedOutputStream class constructor
        // URLConnectionHTTPConduit implementation, HttpURLConnection class is initiated as below and passed as parameter as seen below
        HttpURLConnection connection = (HttpURLConnection) message.get(KEY_HTTP_CONNECTION);

        if (isChunking && chunkThreshold <= 0) {
            chunkThreshold = 0;
            connection.setChunkedStreamingMode(-1);
        }

        return new LibertyWrappedOutputStream(message, connection, needToCacheRequest, isChunking, chunkThreshold, getConduitName());
    }
}
