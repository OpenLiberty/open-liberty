/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.async.methods.AbstractBinPushConsumer;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpConnection;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncRequester;
import org.apache.hc.core5.http.nio.AsyncClientEndpoint;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.entity.StringAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.nio.support.BasicResponseConsumer;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.http2.frame.RawFrame;
import org.apache.hc.core5.http2.impl.nio.H2StreamListener;
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2RequesterBootstrap;
import org.apache.hc.core5.http2.ssl.H2ClientTlsStrategy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.reactor.ssl.SSLSessionVerifier;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 * A simple client that can drive h2 requests and handle h2 server push. Relies on HttpClient and HttpCore.
 * JDK9+ is required for ALPN.
 */
public class SecureHttp2Client {

    private static final String CLASS_NAME = SecureHttp2Client.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    /**
     * Make secure http2 requests to the given host:port/{requestUris}. Return a set containing the string of each complete
     * response received from the server.
     *
     * @param String   host
     * @param int      port
     * @param String[] requestUris
     * @param int      expectedPushStreams number of total expected resources pushed for the request set
     * @return The set of responses received from the server
     * @throws Exception
     */
    public List<String> makeSecureRequests(String host, int port, String[] requestUris, int expectedPushStreams) throws Exception {

        StringBuilder sb = new StringBuilder();
        for (String uri : requestUris) {
            sb.append("\n" + "https://" + host + ":" + port + uri);
        }
        LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "testing requests to:" + sb.toString());

        // keep track of the text of every response received
        final List<String> responseMessages = new ArrayList<String>();
        // latch to consider expected number of streams
        final CountDownLatch latch = new CountDownLatch(requestUris.length + expectedPushStreams);

        HttpHost target = new HttpHost("https", host, port);

        H2Config h2Config = H2Config.custom().setPushEnabled(true).build();

        // Set up an SSL context that will accept our self-signed certificate
        final TrustManager[] trustAllCerts = getNaiveTrustManager();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        final HttpAsyncRequester requester = H2RequesterBootstrap.bootstrap().register("*", createAsyncPushConsumerSupplier(responseMessages,
                                                                                                                            latch)).setH2Config(h2Config).setTlsStrategy(createTlsStrategy(sslContext))
                        //.setStreamListener(createStreamListener()) // uncomment for detailed logging on each stream
                        .create();
        requester.start();
        AsyncClientEndpoint endpoint = h2Connect(target, requester);
        for (final String requestUri : requestUris) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "requesting " + requestUri);
            executeRequest(endpoint, requester, target, requestUri, responseMessages, latch);
        }

        latch.await(29, TimeUnit.SECONDS);

        LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "requests complete, shutting down client");
        requester.close(CloseMode.GRACEFUL);
        logResponseMessages(responseMessages);
        LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "client shutdown complete, returning");
        return responseMessages;
    }

    private static void logResponseMessages(List<String> messages) {
        int i = 1;
        for (String s : messages) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "response message " + i++ + ": " + s);
        }
    }

    private TrustManager[] getNaiveTrustManager() {
        return new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] dummyCert = new X509Certificate[0];
                return dummyCert;
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain,
                                           final String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain,
                                           final String authType) throws CertificateException {
            }
        } };
    }

    private H2StreamListener createStreamListener() {
        return new H2StreamListener() {
            @Override
            public void onHeaderInput(final HttpConnection connection, final int streamId, final List<? extends Header> headers) {
                for (int i = 0; i < headers.size(); i++) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", connection + " (" + streamId + ") << " + headers.get(i));
                }
            }

            @Override
            public void onHeaderOutput(final HttpConnection connection, final int streamId, final List<? extends Header> headers) {
                for (int i = 0; i < headers.size(); i++) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", connection + " (" + streamId + ") >> " + headers.get(i));
                }
            }

            @Override
            public void onFrameInput(final HttpConnection connection, final int streamId, final RawFrame frame) {
            }

            @Override
            public void onFrameOutput(final HttpConnection connection, final int streamId, final RawFrame frame) {
            }

            @Override
            public void onInputFlowControl(final HttpConnection connection, final int streamId, final int delta, final int actualSize) {
            }

            @Override
            public void onOutputFlowControl(final HttpConnection connection, final int streamId, final int delta, final int actualSize) {
            }
        };
    }

    private Supplier<AsyncPushConsumer> createAsyncPushConsumerSupplier(List<String> responseMessages, CountDownLatch latch) {
        return new Supplier<AsyncPushConsumer>() {
            @Override
            public AsyncPushConsumer get() {
                return new AbstractBinPushConsumer() {

                    // keep track of content length for pushed resources
                    int contentLength = 0;

                    @Override
                    protected void start(final HttpRequest promise, final HttpResponse response, final ContentType contentType) throws HttpException, IOException {
                        if (response.getHeader("content-length") != null) {
                            contentLength = Integer.parseInt(response.getHeader("content-length").getValue());
                            LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "receiving a new push resource of length "
                                                                                     + contentLength + ": " + promise.getPath());
                        } else {
                            LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "receiving a new push resource: " + promise.getPath());
                        }
                    }

                    @Override
                    protected int capacityIncrement() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    protected void data(final ByteBuffer data, final boolean endOfStream) throws IOException {
                        data.rewind();
                        StringBuilder sb = new StringBuilder();
                        while (data.hasRemaining()) {
                            char c = (char) data.get();
                            if (contentLength > 0) {
                                if (data.remaining() < contentLength) {
                                    sb.append(c);
                                }
                            } else {
                                sb.append(c);
                            }
                        }
                        if (sb.length() > 0) {
                            responseMessages.add(sb.toString());
                        }
                        data.clear();
                    }

                    @Override
                    protected void completed() {
                        LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "push completed");
                        latch.countDown();
                    }

                    @Override
                    public void failed(final Exception cause) {
                        LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", "push failed: " + cause);
                        final StringWriter sw = new StringWriter();
                        final PrintWriter pw = new PrintWriter(sw, true);
                        cause.printStackTrace(pw);
                        LOGGER.logp(Level.INFO, CLASS_NAME, "drivePushRequests", sw.getBuffer().toString());
                    }

                    @Override
                    public void releaseResources() {
                    }

                };
            }
        };
    }

    private H2ClientTlsStrategy createTlsStrategy(SSLContext sslContext) {
        return new H2ClientTlsStrategy(sslContext, new SSLSessionVerifier() {
            @Override
            public TlsDetails verify(final NamedEndpoint endpoint, final SSLEngine sslEngine) throws SSLException {
                // always assume we're using h2 at this point
                return new TlsDetails(sslEngine.getSession(), "h2");
                // uncomment the following line when we compile on JDK9+ and actually check protocol
                // return new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol());
            }
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeRequest(final AsyncClientEndpoint endpoint, final HttpAsyncRequester requester, HttpHost target, String requestUri,
                                List<String> responseMessages, CountDownLatch latch) {
        requester.execute(
                          new BasicRequestProducer("GET", target, requestUri),
                          new BasicResponseConsumer(new StringAsyncEntityConsumer()),
                          // the first request to a server can take more than the default timeout;
                          // we'll allow 28 seconds for the request to complete
                          Timeout.ofSeconds(28),
                          new FutureCallback<Message<HttpResponse, String>>() {

                              @Override
                              public void completed(final Message<HttpResponse, String> message) {
                                  endpoint.releaseAndReuse();
                                  String body = message.getBody();
                                  responseMessages.add(body);
                                  LOGGER.logp(Level.INFO, CLASS_NAME, "executeRequest", "completed with body: " + body);
                                  latch.countDown();
                              }

                              @Override
                              public void failed(final Exception ex) {
                                  endpoint.releaseAndDiscard();
                                  LOGGER.logp(Level.INFO, CLASS_NAME, "executeRequest", "failed: " + ex);
                                  latch.countDown();
                              }

                              @Override
                              public void cancelled() {
                                  endpoint.releaseAndDiscard();
                                  LOGGER.logp(Level.INFO, CLASS_NAME, "executeRequest", "cancelled");

                                  latch.countDown();
                              }
                          });
    }

    private AsyncClientEndpoint h2Connect(HttpHost target, HttpAsyncRequester requester) throws Exception {
        Future<AsyncClientEndpoint> future = null;
        AsyncClientEndpoint endpoint = null;
        int retryCount = 0;
        boolean retry = true;

        // retry in case of a rejected connection
        while (retry && retryCount++ < 5) {
            try {
                LOGGER.logp(Level.INFO, CLASS_NAME, "h2Connect", "connection attempt " + retryCount);
                future = requester.connect(target, Timeout.ofSeconds(10));
                endpoint = future.get();
                retry = false;
            } catch (Exception e) {
                // if connect() fails, try again in one second
                LOGGER.logp(Level.INFO, CLASS_NAME, "h2Connect", "exception caught: " + e);
                retry = true;
                Thread.sleep(1000);
            }
        }
        return endpoint;
    }
}
