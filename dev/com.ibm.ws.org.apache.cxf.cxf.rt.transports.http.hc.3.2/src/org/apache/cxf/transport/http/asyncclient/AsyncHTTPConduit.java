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

package org.apache.cxf.transport.http.asyncclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CopyingOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory.UseAsyncPolicy;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.HeapByteBufferAllocator;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.client.component.AsyncClientRunnableWrapperManager;

/**
 *
 */
public class AsyncHTTPConduit extends URLConnectionHTTPConduit {
    public static final String USE_ASYNC = "use.async.http.conduit";

    final AsyncHTTPConduitFactory factory;
    volatile int lastTlsHash = -1;
    volatile Object sslState;
    volatile URI sslURL;
    volatile SSLContext sslContext;
    volatile SSLSession session;
    volatile CloseableHttpAsyncClient client;


    public AsyncHTTPConduit(Bus b,
                            EndpointInfo ei,
                            EndpointReferenceType t,
                            AsyncHTTPConduitFactory factory) throws IOException {
        super(b, ei, t);
        this.factory = factory;
    }

    public synchronized CloseableHttpAsyncClient getHttpAsyncClient() throws IOException {
        if (client == null) {
            client = factory.createClient(this);
        }
        if (client == null) {
            throw new IOException("HttpAsyncClient is null");
        }
        return client;
    }
    public AsyncHTTPConduitFactory getAsyncHTTPConduitFactory() {
        return factory;
    }

    @FFDCIgnore(URISyntaxException.class)
    @Override
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
        if (factory.isShutdown()) {
            message.put(USE_ASYNC, Boolean.FALSE);
            super.setupConnection(message, address, csPolicy);
            return;
        }
        boolean addressChanged = false;
        // need to do some clean up work on the URI address
        URI uri = address.getURI();
        String uriString = uri.toString();
        if (uriString.startsWith("hc://")) {
            try {
                uriString = uriString.substring(5);
                uri = new URI(uriString);
                addressChanged = true;
            } catch (URISyntaxException ex) {
                throw new MalformedURLException("unsupport uri: "  + uriString);
            }
        }
        String s = uri.getScheme();
        if (!"http".equals(s) && !"https".equals(s)) {
            throw new MalformedURLException("unknown protocol: " + s);
        }

        Object o = message.getContextualProperty(USE_ASYNC);
        if (o == null) {
            o = factory.getUseAsyncPolicy();
        }
        switch (UseAsyncPolicy.getPolicy(o)) {
        case ALWAYS:
            o = true;
            break;
        case NEVER:
            o = false;
            break;
        case ASYNC_ONLY:
        default:
            o = !message.getExchange().isSynchronous();
            break;
        }

        // check tlsClientParameters from message header
        TLSClientParameters clientParameters = message.get(TLSClientParameters.class);
        if (clientParameters == null) {
            clientParameters = tlsClientParameters;
        }
        if (uri.getScheme().equals("https")
            && clientParameters != null
            && clientParameters.getSSLSocketFactory() != null) {
            //if they configured in an SSLSocketFactory, we cannot do anything
            //with it as the NIO based transport cannot use socket created from
            //the SSLSocketFactory.
            o = false;
        }
        if (!PropertyUtils.isTrue(o)) {
            message.put(USE_ASYNC, Boolean.FALSE);
            super.setupConnection(message, addressChanged ? new Address(uriString, uri) : address, csPolicy);
            return;
        }
        if (StringUtils.isEmpty(uri.getPath())) {
            //hc needs to have the path be "/"
            uri = uri.resolve("/");
            addressChanged = true;
        }

        message.put(USE_ASYNC, Boolean.TRUE);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Asynchronous connection to " + uri.toString() + " has been set up");
        }
        message.put("http.scheme", uri.getScheme());
        String httpRequestMethod =
            (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, httpRequestMethod);
        }
        final CXFHttpRequest e = new CXFHttpRequest(httpRequestMethod);
        BasicHttpEntity entity = new BasicHttpEntity() {
            public boolean isRepeatable() {
                return e.getOutputStream().retransmitable();
            }
        };
        entity.setChunked(true);
        entity.setContentType((String)message.get(Message.CONTENT_TYPE));
        e.setURI(uri);

        e.setEntity(entity);

        RequestConfig.Builder b = RequestConfig.custom()
                .setConnectTimeout((int) csPolicy.getConnectionTimeout())
                .setSocketTimeout((int) csPolicy.getReceiveTimeout())
                .setConnectionRequestTimeout((int) csPolicy.getConnectionRequestTimeout());
        Proxy p = proxyFactory.createProxy(csPolicy, uri);
        if (p != null && p.type() != Proxy.Type.DIRECT) {
            InetSocketAddress isa = (InetSocketAddress)p.address();
            HttpHost proxy = new HttpHost(isa.getHostString(), isa.getPort());
            b.setProxy(proxy);
        }
        e.setConfig(b.build());

        message.put(CXFHttpRequest.class, e);
    }


    protected OutputStream createOutputStream(Message message,
                                              boolean needToCacheRequest,
                                              boolean isChunking,
                                              int chunkThreshold) throws IOException {

        if (Boolean.TRUE.equals(message.get(USE_ASYNC))) {
            CXFHttpRequest entity = message.get(CXFHttpRequest.class);
            AsyncWrappedOutputStream out = new AsyncWrappedOutputStream(message,
                                                needToCacheRequest,
                                                isChunking,
                                                chunkThreshold,
                                                getConduitName(),
                                                entity.getURI());
            entity.setOutputStream(out);
            return out;
        }
        return super.createOutputStream(message, needToCacheRequest, isChunking, chunkThreshold);
    }


    public class AsyncWrappedOutputStream extends WrappedOutputStream
        implements CopyingOutputStream, WritableByteChannel {
        final HTTPClientPolicy csPolicy;

        CXFHttpRequest entity;
        BasicHttpEntity basicEntity;

        boolean isAsync;
        SharedInputBuffer inbuf;
        SharedOutputBuffer outbuf;

        // Objects for the response
        volatile HttpResponse httpResponse;
        volatile Exception exception;

        private Future<Boolean> connectionFuture;

        private Object sessionLock = new Object();
        private boolean closed;

        public AsyncWrappedOutputStream(Message message,
                                        boolean needToCacheRequest,
                                        boolean isChunking,
                                        int chunkThreshold,
                                        String conduitName,
                                        URI uri) {
            super(message,
                  needToCacheRequest,
                  isChunking,
                  chunkThreshold,
                  conduitName,
                  uri);
            csPolicy = getClient(message);
            entity = message.get(CXFHttpRequest.class);
            basicEntity = (BasicHttpEntity)entity.getEntity();
            basicEntity.setChunked(isChunking);
            HeapByteBufferAllocator allocator = new HeapByteBufferAllocator();
            int bufSize = csPolicy.getChunkLength() > 0 ? csPolicy.getChunkLength() : 16320;
            inbuf = new SharedInputBuffer(bufSize, allocator);
            outbuf = new SharedOutputBuffer(bufSize, allocator);
            isAsync = outMessage != null && outMessage.getExchange() != null
                && !outMessage.getExchange().isSynchronous();
        }

        public boolean retransmitable() {
            return cachedStream != null;
        }
        public CachedOutputStream getCachedStream() {
            return cachedStream;
        }


        protected void setProtocolHeaders() throws IOException {
            Headers h = new Headers(outMessage);
            basicEntity.setContentType(h.determineContentType());
            boolean addHeaders = MessageUtils.getContextualBoolean(outMessage, Headers.ADD_HEADERS_PROPERTY, false);

            for (Map.Entry<String, List<String>> header : h.headerMap().entrySet()) {
                if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header.getKey())) {
                    continue;
                }
                if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header.getKey())) {
                    for (String s : header.getValue()) {
                        entity.addHeader(HttpHeaderHelper.COOKIE, s);
                    }
                } else if (!"Content-Length".equalsIgnoreCase(header.getKey())) {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < header.getValue().size(); i++) {
                        b.append(header.getValue().get(i));
                        if (i + 1 < header.getValue().size()) {
                            b.append(',');
                        }
                    }
                    entity.setHeader(header.getKey(), b.toString());
                }
                if (!entity.containsHeader("User-Agent")) {
                    entity.setHeader("User-Agent", Version.getCompleteVersionString());
                }
            }
        }

        protected void setFixedLengthStreamingMode(int i) {
            basicEntity.setChunked(false);
            basicEntity.setContentLength(i);
        }
        public void thresholdReached() throws IOException {
            basicEntity.setChunked(chunking);
        }

        protected void handleNoOutput() throws IOException {
            connect(false);
            outbuf.writeCompleted();
        }

        public boolean isOpen() {
            return !closed;
        }

        public int write(ByteBuffer src) throws IOException {
            int total = 0;
            if (buffer != null) {
                int pos = buffer.size();
                int len = this.threshold - pos;
                if (len > src.remaining()) {
                    len = src.remaining();
                }
                src.get(buffer.getRawBytes(), pos, len);
                buffer.setSize(buffer.size() + len);
                total += len;
                if (buffer.size() >= threshold) {
                    thresholdReached();
                    unBuffer();
                }
            }
            if (cachingForRetransmission) {
                wrappedStream.write(src.array(), src.position(), src.remaining());
                return src.remaining() + total;
            }
            return outbuf.write(src) + total;
        }

        public int copyFrom(InputStream in) throws IOException {
            int count = 0;
            while (buffer != null) {
                int pos = buffer.size();
                int i = in.read(buffer.getRawBytes(), pos,
                                this.threshold - pos);
                if (i > 0) {
                    buffer.setSize(pos + i);
                    if (buffer.size() >= threshold) {
                        thresholdReached();
                        unBuffer();
                    }
                    count += i;
                } else {
                    return count;
                }
            }

            if (cachingForRetransmission) {
                count += IOUtils.copy(in, wrappedStream);
            } else {
                count += outbuf.copy(in);
            }
            return count;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (!chunking && wrappedStream instanceof CachedOutputStream) {
                CachedOutputStream out = (CachedOutputStream)wrappedStream;
                this.basicEntity.setContentLength(out.size());
                wrappedStream = null;
                handleHeadersTrustCaching();
                out.writeCacheTo(wrappedStream);
            }
            super.close();
        }

        @Override
        protected void onFirstWrite() throws IOException {
            if (chunking) {
                super.onFirstWrite();
            } else {
                wrappedStream = new CachedOutputStream();
            }
        }

        protected void setupWrappedStream() throws IOException {
            connect(true);
            wrappedStream = new OutputStream() {
                public void write(byte b[], int off, int len) throws IOException {
                    if (exception instanceof IOException) {
                        throw (IOException) exception;
                    }
                    outbuf.write(b, off, len);
                }
                public void write(int b) throws IOException {
                    if (exception instanceof IOException) {
                        throw (IOException) exception;
                    }
                    outbuf.write(b);
                }
                public void close() throws IOException {
                    outbuf.writeCompleted();
                }
            };

            // If we need to cache for retransmission, store data in a
            // CacheAndWriteOutputStream. Otherwise write directly to the output stream.
            if (cachingForRetransmission) {
                cachedStream = new CacheAndWriteOutputStream(wrappedStream);
                wrappedStream = cachedStream;
            }
        }

        @FFDCIgnore(GeneralSecurityException.class)
        protected void connect(boolean output) throws IOException {
            if (connectionFuture != null) {
                return;
            }

            CXFResponseCallback responseCallback = new CXFResponseCallback() {
                @Override
                public void responseReceived(HttpResponse response) {
                    setHttpResponse(response);
                }

            };

            FutureCallback<Boolean> callback = new FutureCallback<Boolean>() {

                public void completed(Boolean result) {
                }

                public void failed(Exception ex) {
                    setException(ex);
                    inbuf.shutdown();
                    outbuf.shutdown();
                }
                public void cancelled() {
                    handleCancelled();
                    inbuf.shutdown();
                    outbuf.shutdown();
                }

            };

            if (!output) {
                entity.removeHeaders("Transfer-Encoding");
                entity.removeHeaders("Content-Type");
                entity.setEntity(null);
            }

            HttpClientContext ctx = HttpClientContext.create();

            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider() {

                @Override
                public Credentials getCredentials(final AuthScope authscope) {
                    Credentials creds = super.getCredentials(authscope);
                    if (creds != null) {
                        return creds;
                    }
                    if (AsyncHTTPConduit.this.proxyAuthorizationPolicy != null
                            && AsyncHTTPConduit.this.proxyAuthorizationPolicy.getUserName() != null) {
                        return new UsernamePasswordCredentials(AsyncHTTPConduit.this
                                .proxyAuthorizationPolicy.getUserName(),
                                AsyncHTTPConduit.this.proxyAuthorizationPolicy.getPassword());
                    }
                    return null;
                }

            };

            ctx.setCredentialsProvider(credsProvider);

            if ("https".equals(url.getScheme())) {
                try {
                    RegistryBuilder<SchemeIOSessionStrategy> regBuilder
                        = RegistryBuilder.<SchemeIOSessionStrategy>create()
                            .register("http", NoopIOSessionStrategy.INSTANCE);

                    // check tlsClientParameters from message header
                    TLSClientParameters tlsClientParameters = outMessage.get(TLSClientParameters.class);
                    if (tlsClientParameters == null) {
                        tlsClientParameters = getTlsClientParameters();
                    }
                    if (tlsClientParameters == null) {
                        tlsClientParameters = new TLSClientParameters();
                    }
                    final SSLContext sslcontext = getSSLContext(tlsClientParameters);
                    final HostnameVerifier verifier = org.apache.cxf.transport.https.SSLUtils
                        .getHostnameVerifier(tlsClientParameters);
                    regBuilder
                        .register("https",
                                  new SSLIOSessionStrategy(sslcontext) {
                                @Override
                                protected void initializeEngine(SSLEngine engine) {
                                    initializeSSLEngine(sslcontext, engine);
                                }
                                @Override
                                protected void verifySession(final HttpHost host,
                                                             final IOSession iosession,
                                                             final SSLSession sslsession) throws SSLException {
                                    if (!verifier.verify(host.getHostName(), sslsession)) {
                                        throw new SSLException("Could not verify host " + host.getHostName());
                                    }

                                    iosession.setAttribute("cxf.handshake.done", Boolean.TRUE);
                                    setSSLSession(sslsession);
                                }
                            });
                    ctx.setAttribute("http.iosession-factory-registry", regBuilder.build());
                } catch (GeneralSecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }


            if (sslURL != null && !sslURL.equals(url)) {
                sslURL = null;
                sslState = null;
                session = null;
            }
            if (tlsClientParameters != null && tlsClientParameters.hashCode() == lastTlsHash) {
                ctx.setUserToken(sslState);
            }

            connectionFuture = new BasicFuture<Boolean>(callback);
            HttpAsyncClient c = getHttpAsyncClient();
            Credentials creds = (Credentials)outMessage.getContextualProperty(Credentials.class.getName());
            if (creds != null) {
                credsProvider.setCredentials(AuthScope.ANY, creds);
                ctx.setUserToken(creds.getUserPrincipal());
            }
            @SuppressWarnings("unchecked")
            Registry<AuthSchemeProvider> asp = (Registry<AuthSchemeProvider>)outMessage
                .getContextualProperty(AuthSchemeProvider.class.getName());
            if (asp != null) {
                ctx.setAuthSchemeRegistry(asp);
            }

            AccessController.doPrivileged(new PrivilegedAction<Void>() { //Liberty change
                @Override
                public Void run() {
                    c.execute(new CXFHttpAsyncRequestProducer(entity, outbuf),
                              new CXFHttpAsyncResponseConsumer(AsyncWrappedOutputStream.this, inbuf, responseCallback),
                              ctx,
                              callback);
                    return null;
                }
            });
        }

        protected void retrySetHttpResponse(HttpResponse r) {
            if (httpResponse == null && isAsync) {
                setHttpResponse(r);
            }
        }
        @FFDCIgnore(Exception.class)
        protected synchronized void setHttpResponse(HttpResponse r) {
            httpResponse = r;
            if (isAsync) {
                //got a response, need to start the response processing now
                try {
                    handleResponseOnWorkqueue(false, true);
                    isAsync = false; // don't trigger another start on next block. :-)
                } catch (Exception ex) {
                    //ignore, we'll try again on the next consume;
                }
            }
            notifyAll();
        }
        @FFDCIgnore(Exception.class)
        protected synchronized void setException(Exception ex) {
            exception = ex;
            if (isAsync) {
                //got a response, need to start the response processing now
                try {
                    handleResponseOnWorkqueue(false, true);
                    isAsync = false; // don't trigger another start on next block. :-)
                } catch (Exception ex2) {
                    ex2.printStackTrace();
                }
            }
            notifyAll();
        }
        protected synchronized void handleCancelled() {
            notifyAll();
        }

        @FFDCIgnore(InterruptedException.class)
        protected synchronized HttpResponse getHttpResponse() throws IOException {
            while (httpResponse == null) {
                if (exception == null) { //already have an exception, skip waiting
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                if (httpResponse == null) {
                    outbuf.shutdown();
                    inbuf.shutdown();
                    //outbuf = null;
                    //inbuf = null;

                    if (exception != null) {
                        if (exception instanceof IOException) {
                            throw (IOException)exception;
                        }
                        if (exception instanceof RuntimeException) {
                            throw (RuntimeException)exception;
                        }
                        throw new IOException(exception);
                    }

                    throw new SocketTimeoutException("Read Timeout");
                }
            }
            return httpResponse;
        }

        protected void handleResponseAsync() throws IOException {
            AsyncClientRunnableWrapperManager.prepare(outMessage); // Liberty change
            isAsync = true;
        }

        protected void closeInputStream() throws IOException {
            byte bytes[] = new byte[1024];
            while (inbuf.read(bytes) > 0) {
                //nothing
            }
            inbuf.close();
            inbuf.shutdown();
        }

        protected synchronized InputStream getInputStream() throws IOException {
            return new InputStream() {
                public int read() throws IOException {
                    return inbuf.read();
                }
                public int read(byte[] b) throws IOException {
                    return inbuf.read(b);
                }
                public int read(byte[] b, int off, int len) throws IOException {
                    return inbuf.read(b, off, len);
                }
                public int available() throws IOException {
                    return inbuf.available();
                }
                public void close() throws IOException {
                    inbuf.close();
                }
            };
        }

        protected boolean usingProxy() {
            return this.entity.getConfig().getProxy() != null;
        }

        @FFDCIgnore(InterruptedException.class)
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            if ("http".equals(outMessage.get("http.scheme"))) {
                return null;
            }
            connect(true);
            synchronized (sessionLock) {
                if (session == null) {
                    try {
                        sessionLock.wait(csPolicy.getConnectionTimeout());
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                if (session == null) {
                    throw new IOException("No SSLSession detected");
                }
            }
            HostnameVerifier verifier = org.apache.cxf.transport.https.SSLUtils
                .getHostnameVerifier(tlsClientParameters);
            if (!verifier.verify(url.getHost(), session)) {
                throw new IOException("Could not verify host " + url.getHost());
            }

            String method = (String)outMessage.get(Message.HTTP_REQUEST_METHOD);
            String cipherSuite = null;
            Certificate[] localCerts = null;
            Principal principal = null;
            Certificate[] serverCerts = null;
            Principal peer = null;
            if (session != null) {
                cipherSuite = session.getCipherSuite();
                localCerts = session.getLocalCertificates();
                principal = session.getLocalPrincipal();
                serverCerts = session.getPeerCertificates();
                peer = session.getPeerPrincipal();
            }

            return new HttpsURLConnectionInfo(url, method, cipherSuite, localCerts, principal, serverCerts, peer);
        }

        protected int getResponseCode() throws IOException {
            return getHttpResponse().getStatusLine().getStatusCode();
        }

        protected String getResponseMessage() throws IOException {
            return getHttpResponse().getStatusLine().getReasonPhrase();
        }

        private String readHeaders(Headers h) throws IOException {
            Header headers[] = getHttpResponse().getAllHeaders();
            h.headerMap().clear();
            String ct = null;
            for (Header header : headers) {
                List<String> s = h.headerMap().get(header.getName());
                if (s == null) {
                    s = new ArrayList<>(1);
                    h.headerMap().put(header.getName(), s);
                }
                s.add(header.getValue());
                if ("Content-Type".equalsIgnoreCase(header.getName())) {
                    ct = header.getValue();
                }
            }
            return ct;
        }

        protected void updateResponseHeaders(Message inMessage) throws IOException {
            Headers h = new Headers(inMessage);
            inMessage.put(Message.CONTENT_TYPE, readHeaders(h));
            cookies.readFromHeaders(h);
        }

        @FFDCIgnore(IOException.class)
        protected InputStream getPartialResponse() throws IOException {
            InputStream in = null;
            int responseCode = getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_ACCEPTED
                || responseCode == HttpURLConnection.HTTP_OK) {

                Header head = httpResponse.getFirstHeader(HttpHeaderHelper.CONTENT_LENGTH);
                int cli = 0;
                if (head != null) {
                    cli = Integer.parseInt(head.getValue());
                }
                head = httpResponse.getFirstHeader(HttpHeaderHelper.TRANSFER_ENCODING);
                boolean isChunked = head != null &&  HttpHeaderHelper.CHUNKED.equalsIgnoreCase(head.getValue());
                head = httpResponse.getFirstHeader(HttpHeaderHelper.CONNECTION);
                boolean isEofTerminated = head != null &&  HttpHeaderHelper.CLOSE.equalsIgnoreCase(head.getValue());
                if (cli > 0) {
                    in = getInputStream();
                } else if (isChunked || isEofTerminated) {
                    // ensure chunked or EOF-terminated response is non-empty
                    try {
                        PushbackInputStream pin =
                            new PushbackInputStream(getInputStream());
                        int c = pin.read();
                        if (c != -1) {
                            pin.unread((byte)c);
                            in = pin;
                        }
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
            return in;
        }

        protected void updateCookiesBeforeRetransmit() throws IOException {
            Headers h = new Headers();
            readHeaders(h);
            cookies.readFromHeaders(h);
        }

        @FFDCIgnore(Throwable.class)
        protected boolean authorizationRetransmit() throws IOException {
            boolean b = super.authorizationRetransmit();
            if (!b) {
                //HTTPClient may be handling the authorization things instead of us, we
                //just need to make sure we set the cookies and proceed and HC
                //will do the negotiation and such.
                try {
                    closeInputStream();
                } catch (Throwable t) {
                    //ignore
                }
                cookies.writeToMessageHeaders(outMessage);
                retransmit(url.toString());
                return true;
            }
            return b;
        }

        protected void retransmitStream() throws IOException {
            cachingForRetransmission = false; //already cached
            setupWrappedStream();
            cachedStream.writeCacheTo(wrappedStream);
            wrappedStream.flush();
            wrappedStream.close();
        }

        @FFDCIgnore(URISyntaxException.class)
        protected void setupNewConnection(String newURL) throws IOException {
            httpResponse = null;
            isAsync = outMessage != null && outMessage.getExchange() != null
                && !outMessage.getExchange().isSynchronous();
            exception = null;
            connectionFuture = null;
            session = null;
            sslState = null;
            sslURL = null;

            //reset the buffers
            HeapByteBufferAllocator allocator = new HeapByteBufferAllocator();
            int bufSize = csPolicy.getChunkLength() > 0 ? csPolicy.getChunkLength() : 16320;
            inbuf = new SharedInputBuffer(bufSize, allocator);
            outbuf = new SharedOutputBuffer(bufSize, allocator);
            try {
                if (defaultAddress.getString().equals(newURL)) {
                    setupConnection(outMessage, defaultAddress, csPolicy);
                } else {
                    Address address = new Address(newURL);
                    this.url = address.getURI();
                    setupConnection(outMessage, address, csPolicy);
                }
                entity = outMessage.get(CXFHttpRequest.class);
                basicEntity = (BasicHttpEntity)entity.getEntity();
                entity.setOutputStream(this);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }


        public void setSSLSession(SSLSession sslsession) {
            session = sslsession;
            synchronized (sessionLock) {
                sslState = sslsession.getLocalPrincipal();
                sslURL = url;
                sessionLock.notifyAll();
            }
        }

    }

    public synchronized SSLContext getSSLContext(TLSClientParameters tlsClientParameters)
        throws GeneralSecurityException {

        int hash = tlsClientParameters.hashCode();
        if (hash == lastTlsHash && sslContext != null) {
            return sslContext;
        }

        String provider = tlsClientParameters.getJsseProvider();

        String protocol = tlsClientParameters.getSecureSocketProtocol() != null ? tlsClientParameters
            .getSecureSocketProtocol() : "TLS";

        SSLContext ctx = provider == null ? SSLContext.getInstance(protocol) : SSLContext
            .getInstance(protocol, provider);
        ctx.getClientSessionContext().setSessionTimeout(tlsClientParameters.getSslCacheTimeout());

        KeyManager[] keyManagers = tlsClientParameters.getKeyManagers();
        KeyManager[] configuredKeyManagers = org.apache.cxf.transport.https.SSLUtils.configureKeyManagersWithCertAlias(
            tlsClientParameters, keyManagers);

        TrustManager[] trustManagers = tlsClientParameters.getTrustManagers();
        if (trustManagers == null) {
            trustManagers = org.apache.cxf.configuration.jsse.SSLUtils.getDefaultTrustStoreManagers(LOG);
        }

        ctx.init(configuredKeyManagers, trustManagers, tlsClientParameters.getSecureRandom());

        sslContext = ctx;
        lastTlsHash = hash;
        sslState = null;
        sslURL = null;
        session = null;
        return ctx;
    }

    public void initializeSSLEngine(SSLContext sslcontext, SSLEngine sslengine) {
        TLSClientParameters tlsClientParameters = getTlsClientParameters();
        if (tlsClientParameters == null) {
            tlsClientParameters = new TLSClientParameters();
        }

        String[] cipherSuites =
            SSLUtils.getCiphersuitesToInclude(tlsClientParameters.getCipherSuites(),
                                              tlsClientParameters.getCipherSuitesFilter(),
                                              sslcontext.getSocketFactory().getDefaultCipherSuites(),
                                              SSLUtils.getSupportedCipherSuites(sslcontext),
                                              LOG);
        sslengine.setEnabledCipherSuites(cipherSuites);

        String protocol = tlsClientParameters.getSecureSocketProtocol() != null ? tlsClientParameters
            .getSecureSocketProtocol() : "TLS";

        String p[] = findProtocols(protocol, sslengine.getSupportedProtocols());
        if (p != null) {
            sslengine.setEnabledProtocols(p);
        }
    }

    private String[] findProtocols(String p, String[] options) {
        List<String> list = new ArrayList<>();
        for (String s : options) {
            if (s.equals(p)) {
                return new String[] {p};
            } else if (s.startsWith(p)) {
                list.add(s);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.toArray(new String[0]);
    }

}