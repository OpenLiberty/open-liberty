/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.http;

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
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;

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
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * 
 */
public class URLConnectionHTTPConduit extends HTTPConduit {
    public static final String HTTPURL_CONNECTION_METHOD_REFLECTION = "use.httpurlconnection.method.reflection";
    public static final String SET_REASON_PHRASE_NOT_NULL = "set.reason.phrase.not.null";

    private static final boolean DEFAULT_USE_REFLECTION;
    private static final boolean SET_REASON_PHRASE;
    static {
        DEFAULT_USE_REFLECTION = 
            Boolean.valueOf(SystemPropertyAction.getProperty(HTTPURL_CONNECTION_METHOD_REFLECTION, "false"));
        SET_REASON_PHRASE = 
            Boolean.valueOf(SystemPropertyAction.getProperty(SET_REASON_PHRASE_NOT_NULL, "false"));
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
        
    
    public URLConnectionHTTPConduit(Bus b, EndpointInfo ei) throws IOException {
        super(b, ei);
        connectionFactory = new HttpsURLConnectionFactory();
        CXFAuthenticator.addAuthenticator();
    }

    public URLConnectionHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
        connectionFactory = new HttpsURLConnectionFactory();
        CXFAuthenticator.addAuthenticator();
    }
    
    /**
     * Close the conduit
     */
    @FFDCIgnore(IOException.class)
    public void close() {
        super.close();
        if (defaultAddress != null) {
            try {
                URLConnection connect = defaultAddress.getURL().openConnection();
                if (connect instanceof HttpURLConnection) {
                    ((HttpURLConnection)connect).disconnect();
                }
            } catch (IOException ex) {
                //ignore
            }
            //defaultEndpointURL = null;
        }
    }    
    
    private HttpURLConnection createConnection(Message message, Address address, HTTPClientPolicy csPolicy)
        throws IOException {
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
    
    //Liberty start    
    public void setAddress(String address) throws IOException {
        try {
            defaultAddress = new Address(address);
    
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
    //Liberty end

    @FFDCIgnore({java.net.ProtocolException.class, Throwable.class, Throwable.class})
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
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
        String httpRequestMethod = 
            (String)message.get(Message.HTTP_REQUEST_METHOD);
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
						    //Liberty start
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
                                HttpsURLConnection c2 = (HttpsURLConnection)ReflectionUtil.setAccessible(f2)
                                                .get(c);

                                ReflectionUtil.setAccessible(f).set(c2, httpRequestMethod);
                            }
							//Liberty end
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

    @FFDCIgnore(URISyntaxException.class)
    protected OutputStream createOutputStream(Message message, 
                                              boolean needToCacheRequest, 
                                              boolean isChunking,
                                              int chunkThreshold) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)message.get(KEY_HTTP_CONNECTION);
        
        if (isChunking && chunkThreshold <= 0) {
            chunkThreshold = 0;
            connection.setChunkedStreamingMode(-1);                    
        }
        try {
            return new URLConnectionWrappedOutputStream(message, connection,
                                           needToCacheRequest, 
                                           isChunking,
                                           chunkThreshold,
                                           getConduitName());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
    
    private static URI computeURI(Message message, HttpURLConnection connection) throws URISyntaxException {
        Address address = (Address)message.get(KEY_HTTP_CONNECTION_ADDRESS);
        return address != null ? address.getURI() : connection.getURL().toURI();
    }
    
    class URLConnectionWrappedOutputStream extends WrappedOutputStream {
        HttpURLConnection connection;
        URLConnectionWrappedOutputStream(Message message, HttpURLConnection connection,
                                         boolean needToCacheRequest, boolean isChunking,
                                         int chunkThreshold, String conduitName) throws URISyntaxException {
            super(message, needToCacheRequest, isChunking,
                  chunkThreshold, conduitName,
                  computeURI(message, connection));
            this.connection = connection;
        }
        
        // This construction makes extending the HTTPConduit more easier 
        protected URLConnectionWrappedOutputStream(URLConnectionWrappedOutputStream wos) {
            super(wos);
            this.connection = wos.connection;
        }

        @FFDCIgnore(Throwable.class)
        private OutputStream connectAndGetOutputStream(Boolean b) throws IOException {
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

        @FFDCIgnore({PrivilegedActionException.class, ProtocolException.class, SocketException.class})
        protected void setupWrappedStream() throws IOException {
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
                    Boolean b =  (Boolean)outMessage.get(HTTPURL_CONNECTION_METHOD_REFLECTION);
                    cout = connectAndGetOutputStream(b); 
                }
            } catch (SocketException e) { //Liberty
                if ("Socket Closed".equals(e.getMessage())
                    || "HostnameVerifier, socket reset for TTL".equals(e.getMessage())) {
                    connection.connect();
                    cout = connectAndGetOutputStream((Boolean)outMessage.get(HTTPURL_CONNECTION_METHOD_REFLECTION)); 
                } else {
                    throw e;
                }
            }
            if (cachingForRetransmission) {
                cachedStream =
                    new CacheAndWriteOutputStream(cout);
                wrappedStream = cachedStream;
            } else {
                wrappedStream = cout;
            }
        }

        @Override
        public void thresholdReached() {
            if (chunking) {
                connection.setChunkedStreamingMode(
                    URLConnectionHTTPConduit.this.getClient().getChunkLength());
            }
        }
        @Override
        protected void onFirstWrite() throws IOException {
            super.onFirstWrite();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sending "
                    + connection.getRequestMethod() 
                    + " Message with Headers to " 
                    + url
                    + " Conduit :"
                    + conduitName
                    + "\n");
            }
        }
        protected void setProtocolHeaders() throws IOException {
            new Headers(outMessage).setProtocolHeadersInConnection(connection);
        }

        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            connection.connect();
            return new HttpsURLConnectionInfo(connection);
        }
        protected void updateResponseHeaders(Message inMessage) {
            Headers h = new Headers(inMessage);
            h.readFromConnection(connection);
            inMessage.put(Message.CONTENT_TYPE, connection.getContentType());
            cookies.readFromHeaders(h);
        }
        protected void handleResponseAsync() throws IOException {
            handleResponseOnWorkqueue(true, false);
        }
        protected void updateCookiesBeforeRetransmit() {
            Headers h = new Headers();
            h.readFromConnection(connection);
            cookies.readFromHeaders(h);
        }
        
        @FFDCIgnore(IOException.class)
        protected InputStream getInputStream() throws IOException {
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

        
        protected void closeInputStream() throws IOException {
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

        @FFDCIgnore(PrivilegedActionException.class)
        protected int getResponseCode() throws IOException {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Integer>() {

                    @Override
                    public Integer run() throws IOException {
                        return connection.getResponseCode();
                    } });
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof IOException) {
                    throw (IOException) t;
                }
                throw new RuntimeException(t);
            }
        }
        protected String getResponseMessage() throws IOException {
            boolean b = MessageUtils.getContextualBoolean(this.outMessage,
                                                          SET_REASON_PHRASE_NOT_NULL,
                                                          SET_REASON_PHRASE);
            if (connection.getResponseMessage() == null && b) {
                //some http server like tomcat 8.5+ won't return the
                //reason phrase in response, return a informative value
                //to tell user no reason phrase in the response instead of null
                return "no reason phrase in the response";
            } else { //Liberty
                return connection.getResponseMessage();
            }
        }
        protected InputStream getPartialResponse() throws IOException {
            return ChunkedUtil.getPartialResponse(connection, connection.getResponseCode());
        }
        protected boolean usingProxy() {
            return connection.usingProxy();
        }
        protected void setFixedLengthStreamingMode(int i) {
            // [CXF-6227] do not call connection.setFixedLengthStreamingMode(i)
            // to prevent https://bugs.openjdk.java.net/browse/JDK-8044726
        }
        @FFDCIgnore(PrivilegedActionException.class)
        protected void handleNoOutput() throws IOException {
            if ("POST".equals(getMethod())) {
			    //Liberty start
                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Void>)() -> {
                        connection.getOutputStream().close(); 
                        return null;
                    });
                } catch (PrivilegedActionException pae) {
                    Throwable t = pae.getCause();
                    if (t instanceof IOException) {
                        throw (IOException) t;
                    }
                    throw new RuntimeException(t);
                }
				//Liberty end
            }
        }
        @FFDCIgnore(URISyntaxException.class)
        protected void setupNewConnection(String newURL) throws IOException {
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
            connection = (HttpURLConnection)outMessage.get(KEY_HTTP_CONNECTION);
        }

        @Override
        protected void retransmitStream() throws IOException {
            Boolean b =  (Boolean)outMessage.get(HTTPURL_CONNECTION_METHOD_REFLECTION);
            OutputStream out = connectAndGetOutputStream(b); 
            cachedStream.writeCacheTo(out);
        }
    }
    
}
