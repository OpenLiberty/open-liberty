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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.io.AbstractThresholdOutputStream;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.policy.PolicyDataEngine;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.Assertor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.auth.CustomAuthSupplier;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transport.http.auth.SpnegoAuthSupplier;
import org.apache.cxf.transport.http.policy.impl.ClientPolicyCalculator;
import org.apache.cxf.transport.https.CertConstraints;
import org.apache.cxf.transport.https.CertConstraintsInterceptor;
import org.apache.cxf.transport.https.CertConstraintsJaxBUtils;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.workqueue.AutomaticWorkQueue;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cxf.client.component.AsyncClientRunnableWrapperManager;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/*
 * HTTP Conduit implementation.
 * <p>
 * This implementation is a based on the java.net.URLConnection interface and
 * dependent upon installed implementations of that URLConnection,
 * HttpURLConnection, and HttpsURLConnection. Currently, this implementation
 * has been known to work with the Sun JDK 1.5 default implementations. The
 * HttpsURLConnection is part of Sun's implementation of the JSSE.
 * Presently, the source code for the Sun JSSE implementation is unavailable
 * and therefore we may only lay a guess of whether its HttpsURLConnection
 * implementation correctly works as far as security is concerned.
 * <p>
 * The Trust Decision. If a MessageTrustDecider is configured/set for the
 * Conduit, it is called upon the first flush of the headers in the
 * WrappedOutputStream. This reason for this approach is two-fold.
 * Theoretically, in order to get connection information out of the
 * URLConnection, it must be "connected". We assume that its implementation will
 * only follow through up to the point at which it will be ready to send
 * one byte of data down to the endpoint, but through proxies, and the
 * commpletion of a TLS handshake in the case of HttpsURLConnection.
 * However, if we force the connect() call right away, the default
 * implementations will not allow any calls to add/setRequestProperty,
 * throwing an exception that the URLConnection is already connected.
 * <p>
 * We need to keep the semantic that later CXF interceptors may add to the
 * PROTOCOL_HEADERS in the Message. This architectual decision forces us to
 * delay the connection until after that point, then pulling the trust decision.
 * <p>
 * The security caveat is that we don't really know when the connection is
 * really established. The call to "connect" is stated to force the
 * "connection," but it is a no-op if the connection was already established.
 * It is entirely possible that an implementation of an URLConnection may
 * indeed connect at will and start sending the headers down the connection
 * during calls to add/setRequestProperty!
 * <p>
 * We know that the JDK 1.5 sun.com.net.www.HttpURLConnection does not send
 * this information before the "connect" call, because we can look at the
 * source code. However, we can only assume, not verify, that the JSSE 1.5
 * HttpsURLConnection does the same, in that it is probable that the
 * HttpsURLConnection shares the HttpURLConnection implementation.
 * <p>
 * Due to these implementations following redirects without trust checks, we
 * force the URLConnection implementations not to follow redirects. If
 * client side policy dictates that we follow redirects, trust decisions are
 * placed before each retransmit. On a redirect, any authorization information
 * dynamically acquired by a BasicAuth UserPass supplier is removed before
 * being retransmitted, as it may no longer be applicable to the new url to
 * which the connection is redirected.
 */

/**
 * This Conduit handles the "http" and "https" transport protocols. An
 * instance is governed by policies either explicitly set or by
 * configuration.
 */
@NoJSR250Annotations
public abstract class HTTPConduit
    extends AbstractConduit
    implements Configurable, Assertor, PropertyChangeListener {


    /**
     *  This constant is the Message(Map) key for the HttpURLConnection that
     *  is used to get the response.
     */
    public static final String KEY_HTTP_CONNECTION = "http.connection";
    public static final String KEY_HTTP_CONNECTION_ADDRESS = "http.connection.address";

    public static final String SET_HTTP_RESPONSE_MESSAGE = "org.apache.cxf.transport.http.set.response.message";
    public static final String HTTP_RESPONSE_MESSAGE = "http.responseMessage";

    public static final String PROCESS_FAULT_ON_HTTP_400 = "org.apache.cxf.transport.process_fault_on_http_400";
    public static final String NO_IO_EXCEPTIONS = "org.apache.cxf.transport.no_io_exceptions";
    /**
     * The Logger for this class.
     */
    protected static final Logger LOG = LogUtils.getL7dLogger(HTTPConduit.class);

    private static boolean hasLoggedAsyncWarning;
    private static final TraceComponent tc = Tr.register(HTTPConduit.class);

    /**
     * This constant holds the suffix ".http-conduit" that is appended to the
     * Endpoint Qname to give the configuration name of this conduit.
     */
    private static final String SC_HTTP_CONDUIT_SUFFIX = ".http-conduit";

    private static final String AUTO_REDIRECT_SAME_HOST_ONLY = "http.redirect.same.host.only";
    private static final String AUTO_REDIRECT_ALLOW_REL_URI = "http.redirect.relative.uri";
    private static final String AUTO_REDIRECT_ALLOWED_URI = "http.redirect.allowed.uri";
    private static final String AUTO_REDIRECT_MAX_SAME_URI_COUNT = "http.redirect.max.same.uri.count";

    private static final String HTTP_POST_METHOD = "POST";
    private static final String HTTP_GET_METHOD = "GET";
    private static final Set<String> KNOWN_HTTP_VERBS_WITH_NO_CONTENT =
        new HashSet<>(Arrays.asList(new String[]{"GET", "HEAD", "OPTIONS", "TRACE"}));
    /**
     * This constant is the Message(Map) key for a list of visited URLs that
     * is used in redirect loop protection.
     */
    private static final String KEY_VISITED_URLS = "VisitedURLs";

    /**
     * This constant is the Message(Map) key for a list of URLs that
     * is used in authorization loop protection.
     */
    private static final String KEY_AUTH_URLS = "AuthURLs";

    /**
     *  This field holds a reference to the CXF bus associated this conduit.
     */
    protected final Bus bus;

    /**
     * This field is used for two reasons. First it provides the base name for
     * the conduit for Spring configuration. The other is to hold default
     * address information, should it not be supplied in the Message Map, by the
     * Message.ENDPOINT_ADDRESS property.
     */
    protected final EndpointInfo endpointInfo;


    /**
     * This field holds the "default" URI for this particular conduit, which
     * is created on demand.
     */
    protected volatile Address defaultAddress;

    protected boolean fromEndpointReferenceType;

    protected ProxyFactory proxyFactory;

    // Configurable values

    /**
     * This field holds the QoS configuration settings for this conduit.
     * This field is injected via spring configuration based on the conduit
     * name.
     */
    protected HTTPClientPolicy clientSidePolicy;

    /**
     * This field holds the password authorization configuration.
     * This field is injected via spring configuration based on the conduit
     * name.
    */
    protected AuthorizationPolicy authorizationPolicy;

    /**
     * This field holds the password authorization configuration for the
     * configured proxy. This field is injected via spring configuration based
     * on the conduit name.
     */
    protected ProxyAuthorizationPolicy proxyAuthorizationPolicy;

    /**
     * This field holds the configuration TLS configuration which
     * is programmatically configured.
     */
    protected TLSClientParameters tlsClientParameters;

    /**
     * This field contains the MessageTrustDecider.
     */
    protected MessageTrustDecider trustDecider;

    /**
     * Implements the authentication handling when talking to a server. If it is not set
     * it will be created from the authorizationPolicy.authType
     */
    protected volatile HttpAuthSupplier authSupplier;

    /**
     * Implements the proxy authentication handling. If it is not set
     * it will be created from the proxyAuthorizationPolicy.authType
     */
    protected volatile HttpAuthSupplier proxyAuthSupplier;

    protected Cookies cookies;

    protected CertConstraints certConstraints;

    private volatile boolean clientSidePolicyCalced;


    /**
     * Constructor
     *
     * @param b the associated Bus
     * @param ei the endpoint info of the initiator
     * @throws IOException
     */
    public HTTPConduit(Bus b, EndpointInfo ei) throws IOException {
        this(b,
             ei,
             null);
    }

    /**
     * Constructor
     *
     * @param b the associated Bus.
     * @param ei the endpoint info of the initiator.
     * @param t the endpoint reference of the target.
     * @throws IOException
     */
    public HTTPConduit(Bus b,
                       EndpointInfo ei,
                       EndpointReferenceType t) throws IOException {
        super(getTargetReference(ei, t, b));

        bus = b;
        endpointInfo = ei;

        if (t != null) {
            fromEndpointReferenceType = true;
        }
        proxyFactory = new ProxyFactory();
        cookies = new Cookies();
    }

    /**
     * updates the HTTPClientPolicy that is compatible with the assertions
     * included in the service, endpoint, operation and message policy subjects
     * if a PolicyDataEngine is installed
     *
     * wsdl extensors are superseded by policies which in
     * turn are superseded by injection
     */
    private void updateClientPolicy(Message m) {
        if (!clientSidePolicyCalced) {
            PolicyDataEngine policyEngine = bus.getExtension(PolicyDataEngine.class);
            if (policyEngine != null && endpointInfo.getService() != null) {
                clientSidePolicy = policyEngine.getClientEndpointPolicy(m,
                                                                        endpointInfo,
                                                                        this,
                                                                        new ClientPolicyCalculator());
                if (clientSidePolicy != null) {
                    clientSidePolicy.removePropertyChangeListener(this); //make sure we aren't added twice
                    clientSidePolicy.addPropertyChangeListener(this);
                }
            }
        }
        clientSidePolicyCalced = true;
    }

    private void updateClientPolicy() {
        if (!clientSidePolicyCalced) {
            //do no spend time on building Message and Exchange (which basically
            //are ConcurrentHashMap instances) if the policy is already available
            Message m = new MessageImpl();
            m.setExchange(new ExchangeImpl());
            m.getExchange().put(EndpointInfo.class, this.endpointInfo);
            updateClientPolicy(m);
        }
    }

    /**
     * This method returns the registered Logger for this conduit.
     */
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     * This method returns the name of the conduit, which is based on the
     * endpoint name plus the SC_HTTP_CONDUIT_SUFFIX.
     * @return
     */
    public final String getConduitName() {
        return endpointInfo.getName() + SC_HTTP_CONDUIT_SUFFIX;
    }

    private static void configureConduitFromEndpointInfo(HTTPConduit conduit,
            EndpointInfo endpointInfo) {
        if (conduit.getClient() == null) {
            conduit.setClient(endpointInfo.getTraversedExtensor(
                    new HTTPClientPolicy(), HTTPClientPolicy.class));
        }
        if (conduit.getAuthorization() == null) {
            conduit.setAuthorization(endpointInfo.getTraversedExtensor(
                    new AuthorizationPolicy(), AuthorizationPolicy.class));

        }
        if (conduit.getProxyAuthorization() == null) {
            conduit.setProxyAuthorization(endpointInfo.getTraversedExtensor(
                    new ProxyAuthorizationPolicy(),
                    ProxyAuthorizationPolicy.class));

        }
        if (conduit.getTlsClientParameters() == null) {
            conduit.setTlsClientParameters(endpointInfo.getTraversedExtensor(
                    null, TLSClientParameters.class));
        }
        if (conduit.getTrustDecider() == null) {
            conduit.setTrustDecider(endpointInfo.getTraversedExtensor(null,
                    MessageTrustDecider.class));
        }
        if (conduit.getAuthSupplier() == null) {
            conduit.setAuthSupplier(endpointInfo.getTraversedExtensor(null,
                    HttpAuthSupplier.class));
        }
    }

    private void logConfig() {
        if (!LOG.isLoggable(Level.FINE)) {
            return;
        }
        if (trustDecider == null) {
            LOG.log(Level.FINE,
                    "No Trust Decider configured for Conduit '"
                    + getConduitName() + "'");
        } else {
            LOG.log(Level.FINE, "Message Trust Decider of class '"
                    + trustDecider.getClass().getName()
                    + "' with logical name of '"
                    + trustDecider.getLogicalName()
                    + "' has been configured for Conduit '"
                    + getConduitName()
                    + "'");
        }
        if (authSupplier == null) {
            LOG.log(Level.FINE,
                    "No Auth Supplier configured for Conduit '"
                    + getConduitName() + "'");
        } else {
            LOG.log(Level.FINE, "HttpAuthSupplier of class '"
                    + authSupplier.getClass().getName()
                    + "' has been configured for Conduit '"
                    + getConduitName()
                    + "'");
        }
        if (this.tlsClientParameters != null) {
            LOG.log(Level.FINE, "Conduit '" + getConduitName()
                    + "' has been configured for TLS "
                    + "keyManagers " + Arrays.toString(tlsClientParameters.getKeyManagers())
                    + "trustManagers " + Arrays.toString(tlsClientParameters.getTrustManagers())
                    + "secureRandom " + tlsClientParameters.getSecureRandom()
                    + "Disable Common Name (CN) Check: " + tlsClientParameters.isDisableCNCheck());

        } else {
            LOG.log(Level.FINE, "Conduit '" + getConduitName()
                    + "' has been configured for plain http.");
        }
    }

    /**
     * This call gets called by the HTTPTransportFactory after it
     * causes an injection of the Spring configuration properties
     * of this Conduit.
     */
    public void finalizeConfig() {
        // See if not set by configuration, if there are defaults
        // in order from the Endpoint, Service, or Bus.

        configureConduitFromEndpointInfo(this, endpointInfo);
        logConfig();

        if (getClient().getDecoupledEndpoint() != null) {
            this.endpointInfo.setProperty("org.apache.cxf.ws.addressing.replyto",
                                          getClient().getDecoupledEndpoint());
        }
    }

    /**
     * Allow access to the cookies that the conduit is maintaining
     * @return the sessionCookies map
     */
    public Map<String, Cookie> getCookies() {
        return cookies.getSessionCookies();
    }


    protected abstract void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy)
        throws IOException;

    /**
     * Prepare to send an outbound HTTP message over this http conduit to a
     * particular endpoint.
     * <P>
     * If the Message.PATH_INFO property is set it gets appended
     * to the Conduit's endpoint URL. If the Message.QUERY_STRING
     * property is set, it gets appended to the resultant URL following
     * a "?".
     * <P>
     * If the Message.HTTP_REQUEST_METHOD property is NOT set, the
     * Http request method defaults to "POST".
     * <P>
     * If the Message.PROTOCOL_HEADERS is not set on the message, it is
     * initialized to an empty map.
     * <P>
     * This call creates the OutputStream for the content of the message.
     * It also assigns the created Http(s)URLConnection to the Message
     * Map.
     *
     * @param message The message to be sent.
     */
    @FFDCIgnore(URISyntaxException.class)
    @Override
    public void prepare(Message message) throws IOException {
        // This call can possibly change the conduit endpoint address and
        // protocol from the default set in EndpointInfo that is associated
        // with the Conduit.
        Address currentAddress;
        try {
            currentAddress = setupAddress(message);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        // The need to cache the request is off by default
        boolean needToCacheRequest = false;

        HTTPClientPolicy csPolicy = getClient(message);
        setupConnection(message, currentAddress, csPolicy);

        // If the HTTP_REQUEST_METHOD is not set, the default is "POST".
        String httpRequestMethod =
            (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, "POST");
        }

        boolean isChunking = false;
        int chunkThreshold = 0;
        final AuthorizationPolicy effectiveAuthPolicy = getEffectiveAuthPolicy(message);
        if (this.authSupplier == null) {
            this.authSupplier = createAuthSupplier(effectiveAuthPolicy);
        }

        if (this.proxyAuthSupplier == null) {
            this.proxyAuthSupplier = createAuthSupplier(proxyAuthorizationPolicy);
        }

        if (this.authSupplier.requiresRequestCaching()) {
            needToCacheRequest = true;
            isChunking = false;
            LOG.log(Level.FINE,
                    "Auth Supplier, but no Preemptive User Pass or Digest auth (nonce may be stale)"
                    + " We must cache request.");
        }
        if (csPolicy.isAutoRedirect()) {
            needToCacheRequest = true;
            LOG.log(Level.FINE, "AutoRedirect is turned on.");
        }
        if (csPolicy.getMaxRetransmits() > 0) {
            needToCacheRequest = true;
            LOG.log(Level.FINE, "MaxRetransmits is set > 0.");
        }
        // DELETE does not work and empty PUTs cause misleading exceptions
        // if chunking is enabled
        // TODO : ensure chunking can be enabled for non-empty PUTs - if requested
        if (csPolicy.isAllowChunking()
            && isChunkingSupported(message, httpRequestMethod)) {
            //TODO: The chunking mode be configured or at least some
            // documented client constant.
            //use -1 and allow the URL connection to pick a default value
            isChunking = true;
            chunkThreshold = csPolicy.getChunkingThreshold();
        }
        cookies.writeToMessageHeaders(message);

        // The trust decision is relegated to after the "flushing" of the
        // request headers.



        if (certConstraints != null) {
            message.put(CertConstraints.class.getName(), certConstraints);
            message.getInterceptorChain().add(CertConstraintsInterceptor.INSTANCE);
        }

        setHeadersByAuthorizationPolicy(message, currentAddress.getURI());
        new Headers(message).setFromClientPolicy(getClient(message));

        // set the OutputStream on the ProxyOutputStream
        ProxyOutputStream pos = message.getContent(ProxyOutputStream.class);
        if (pos != null && message.getContent(OutputStream.class) != null) {
            pos.setWrappedOutputStream(createOutputStream(message,
                                                          needToCacheRequest,
                                                          isChunking,
                                                          chunkThreshold));
        } else {
            message.setContent(OutputStream.class,
                               createOutputStream(message,
                                                  needToCacheRequest,
                                                  isChunking,
                                                  chunkThreshold));
        }
        // We are now "ready" to "send" the message.
    }

    protected boolean isChunkingSupported(Message message, String httpMethod) {
        if (HTTP_POST_METHOD.equals(httpMethod)) {
            return true;
        } else if (!HTTP_GET_METHOD.equals(httpMethod)) {
            MessageContentsList objs = MessageContentsList.getContentsList(message);
            if (objs != null && !objs.isEmpty()) {
                Object obj = objs.get(0);
                return obj.getClass() != String.class
                    || (obj.getClass() == String.class && ((String)obj).length() > 0);
            }
        }
        return false;
    }

    protected abstract OutputStream createOutputStream(Message message,
                                                       boolean needToCacheRequest,
                                                       boolean isChunking,
                                                       int chunkThreshold) throws IOException;

    private HttpAuthSupplier createAuthSupplier(AuthorizationPolicy authzPolicy) {
        String authType = authzPolicy.getAuthorizationType();
        if (HttpAuthHeader.AUTH_TYPE_NEGOTIATE.equals(authType)) {
            return new SpnegoAuthSupplier();
        } else if (HttpAuthHeader.AUTH_TYPE_DIGEST.equals(authType)) {
            return new DigestAuthSupplier();
        } else if (authType != null && !HttpAuthHeader.AUTH_TYPE_BASIC.equals(authType)
            && authzPolicy.getAuthorization() != null) {
            return new CustomAuthSupplier();
        } else {
            return new DefaultBasicAuthSupplier();
        }
    }

    @FFDCIgnore(NumberFormatException.class)
    protected static int determineReceiveTimeout(Message message,
            HTTPClientPolicy csPolicy) {
        long rtimeout = csPolicy.getReceiveTimeout();
        if (message.get(Message.RECEIVE_TIMEOUT) != null) {
            Object obj = message.get(Message.RECEIVE_TIMEOUT);
            try {
                rtimeout = Long.parseLong(obj.toString());
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "INVALID_TIMEOUT_FORMAT", new Object[] {
                    Message.RECEIVE_TIMEOUT, obj.toString()
                });
            }
        }
        if (rtimeout > Integer.MAX_VALUE) {
            rtimeout = Integer.MAX_VALUE;
        }
        return (int)rtimeout;
    }

    @FFDCIgnore(NumberFormatException.class)
    protected static int determineConnectionTimeout(Message message,
            HTTPClientPolicy csPolicy) {
        long ctimeout = csPolicy.getConnectionTimeout();
        if (message.get(Message.CONNECTION_TIMEOUT) != null) {
            Object obj = message.get(Message.CONNECTION_TIMEOUT);
            try {
                ctimeout = Long.parseLong(obj.toString());
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "INVALID_TIMEOUT_FORMAT", new Object[] {
                    Message.CONNECTION_TIMEOUT, obj.toString()
                });
            }
        }
        if (ctimeout > Integer.MAX_VALUE) {
            ctimeout = Integer.MAX_VALUE;
        }
        return (int)ctimeout;
    }

    @Override
    public void close(Message msg) throws IOException {
        InputStream in = msg.getContent(InputStream.class);
        try {
            if (in != null) {
                int count = 0;
                byte[] buffer = new byte[1024];
                while (in.read(buffer) != -1
                    && count < 25) {
                    //don't do anything, we just need to pull off the unread data (like
                    //closing tags that we didn't need to read

                    //however, limit it so we don't read off gigabytes of data we won't use.
                    ++count;
                }
            }
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Finished servicing http request on conduit. ");
            }
            try {
                super.close(msg);
            } finally {
                //clean up address within threadlocal of EndPointInfo
                endpointInfo.resetAddress();  //Liberty #3669
            }

        }
    }

    /**
     * This function sets up a URL based on ENDPOINT_ADDRESS, PATH_INFO,
     * and QUERY_STRING properties in the Message. The QUERY_STRING gets
     * added with a "?" after the PATH_INFO. If the ENDPOINT_ADDRESS is not
     * set on the Message, the endpoint address is taken from the
     * "defaultEndpointURL".
     * <p>
     * The PATH_INFO is only added to the endpoint address string should
     * the PATH_INFO not equal the end of the endpoint address string.
     *
     * @param message The message holds the addressing information.
     *
     * @return The full URL specifying the HTTP request to the endpoint.
     *
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    private Address setupAddress(Message message) throws URISyntaxException {
        String result = (String)message.get(Message.ENDPOINT_ADDRESS);
        String pathInfo = (String)message.get(Message.PATH_INFO);
        String queryString = (String)message.get(Message.QUERY_STRING);
        setAndGetDefaultAddress();
        if (result == null) {
            if (pathInfo == null && queryString == null) {
                if (defaultAddress != null) {
                    message.put(Message.ENDPOINT_ADDRESS, defaultAddress.getString());
                }
                return defaultAddress;
            }
            if (defaultAddress != null) {
                result = defaultAddress.getString();
                message.put(Message.ENDPOINT_ADDRESS, result);
            }
        }

        // REVISIT: is this really correct?
        if (null != pathInfo && !result.endsWith(pathInfo)) {
            result = result + pathInfo;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }
        if (defaultAddress == null) {
            return setAndGetDefaultAddress(result);
        }
        return result.equals(defaultAddress.getString()) ? defaultAddress : new Address(result);
    }

    /**
     * Close the conduit
     */
    @Override
    public void close() {
        try {
            if (clientSidePolicy != null) {
                clientSidePolicy.removePropertyChangeListener(this);
            }

        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Finished servicing http request on conduit. ");
            }
            //clean up address within threadlocal of EndPointInfo
            endpointInfo.resetAddress();    //Liberty #3669
        }

    }

    /**
     * @return the default target address
     */
    public String getAddress() {
        if (defaultAddress != null) {
            return defaultAddress.getString();
        } else if (fromEndpointReferenceType) {
            return getTarget().getAddress().getValue();
        }
        return endpointInfo.getAddress();
    }

    /**
     * @return the default target URL
     */
    protected URI getURI() throws URISyntaxException {
        return setAndGetDefaultAddress().getURI();
    }

    private Address setAndGetDefaultAddress() throws URISyntaxException {
        if (defaultAddress == null) {
            synchronized (this) {
                if (defaultAddress == null) {
                    if (fromEndpointReferenceType && getTarget().getAddress().getValue() != null) {
                        defaultAddress = new Address(this.getTarget().getAddress().getValue());
                    } else if (endpointInfo.getAddress() != null) {
                        defaultAddress = new Address(endpointInfo.getAddress());
                    }
                }
            }
        }
        return defaultAddress;
    }

    private Address setAndGetDefaultAddress(String curAddr) throws URISyntaxException {
        if (defaultAddress == null) {
            synchronized (this) {
                if (defaultAddress == null) {
                    if (curAddr != null) {
                        defaultAddress = new Address(curAddr);
                    } else {
                        throw new URISyntaxException("<null>",
                                                     "Invalid address. Endpoint address cannot be null.", 0);
                    }
                }
            }
        }
        return defaultAddress;
    }
    /**
     * This call places HTTP Header strings into the headers that are relevant
     * to the Authorization policies that are set on this conduit by
     * configuration.
     * <p>
     * An AuthorizationPolicy may also be set on the message. If so, those
     * policies are merged. A user name or password set on the messsage
     * overrides settings in the AuthorizationPolicy is retrieved from the
     * configuration.
     * <p>
     * The precedence is as follows:
     * 1. AuthorizationPolicy that is set on the Message, if exists.
     * 2. Authorization from AuthSupplier, if exists.
     * 3. AuthorizationPolicy set/configured for conduit.
     *
     * REVISIT: Since the AuthorizationPolicy is set on the message by class, then
     * how does one override the ProxyAuthorizationPolicy which is the same
     * type?
     *
     * @param message
     * @param currentURI
     */
    protected void setHeadersByAuthorizationPolicy(
            Message message,
            URI currentURI
    ) {
        Headers headers = new Headers(message);
        AuthorizationPolicy effectiveAuthPolicy = getEffectiveAuthPolicy(message);
        String authString = authSupplier.getAuthorization(effectiveAuthPolicy, currentURI, message, null);
        if (authString != null) {
            headers.setAuthorization(authString);
        }

        String proxyAuthString = proxyAuthSupplier.getAuthorization(proxyAuthorizationPolicy,
                                                               currentURI, message, null);
        if (proxyAuthString != null) {
            headers.setProxyAuthorization(proxyAuthString);
        }
    }

    /**
     * This is part of the Configurable interface which retrieves the
     * configuration from spring injection.
     */
    // REVISIT:What happens when the endpoint/bean name is null?
    @Override
    public String getBeanName() {
        if (endpointInfo.getName() != null) {
            return endpointInfo.getName().toString() + ".http-conduit";
        }
        return null;
    }

    /**
     * Determines effective auth policy from message, conduit and empty default
     * with priority from first to last
     *
     * @param message
     * @return effective AthorizationPolicy
     */
    public AuthorizationPolicy getEffectiveAuthPolicy(Message message) {
        AuthorizationPolicy authPolicy = getAuthorization();
        AuthorizationPolicy newPolicy = message.get(AuthorizationPolicy.class);
        AuthorizationPolicy effectivePolicy = newPolicy;
        if (effectivePolicy == null) {
            effectivePolicy = authPolicy;
        }
        if (effectivePolicy == null) {
            effectivePolicy = new AuthorizationPolicy();
        }
        return effectivePolicy;
    }

    /**
     * This method gets the Authorization Policy that was configured or
     * explicitly set for this HTTPConduit.
     */
    public AuthorizationPolicy getAuthorization() {
        return authorizationPolicy;
    }

    /**
     * This method is used to set the Authorization Policy for this conduit.
     * Using this method will override any Authorization Policy set in
     * configuration.
     */
    public void setAuthorization(AuthorizationPolicy authorization) {
        this.authorizationPolicy = authorization;
    }

    public HTTPClientPolicy getClient(Message message) {
        ClientPolicyCalculator cpc = new ClientPolicyCalculator();
        HTTPClientPolicy pol = message.get(HTTPClientPolicy.class);
        updateClientPolicy(message);
        if (pol != null) {
            pol = cpc.intersect(pol, clientSidePolicy);
        } else {
            pol = clientSidePolicy;
        }

        PolicyDataEngine policyDataEngine = bus.getExtension(PolicyDataEngine.class);
        if (policyDataEngine == null) {
            return pol;
        }
        return policyDataEngine.getPolicy(message, pol, cpc);
    }

    /**
     * This method retrieves the Client Side Policy set/configured for this
     * HTTPConduit.
     */
    public HTTPClientPolicy getClient() {
        updateClientPolicy();
        return clientSidePolicy;
    }

    /**
     * This method sets the Client Side Policy for this HTTPConduit. Using this
     * method will override any HTTPClientPolicy set in configuration.
     */
    public void setClient(HTTPClientPolicy client) {
        if (this.clientSidePolicy != null) {
            this.clientSidePolicy.removePropertyChangeListener(this);
        }
        this.clientSidePolicyCalced = true;
        this.clientSidePolicy = client;
        clientSidePolicy.removePropertyChangeListener(this); //make sure we aren't added twice
        clientSidePolicy.addPropertyChangeListener(this);
        endpointInfo.setProperty("org.apache.cxf.ws.addressing.replyto", client.getDecoupledEndpoint());
    }

    /**
     * This method retrieves the Proxy Authorization Policy for a proxy that is
     * set/configured for this HTTPConduit.
     */
    public ProxyAuthorizationPolicy getProxyAuthorization() {
        return proxyAuthorizationPolicy;
    }

    /**
     * This method sets the Proxy Authorization Policy for a specified proxy.
     * Using this method overrides any Authorization Policy for the proxy
     * that is set in the configuration.
     */
    public void setProxyAuthorization(
            ProxyAuthorizationPolicy proxyAuthorization
    ) {
        this.proxyAuthorizationPolicy = proxyAuthorization;
    }

    /**
     * This method returns the TLS Client Parameters that is set/configured
     * for this HTTPConduit.
     */
    public TLSClientParameters getTlsClientParameters() {
        return tlsClientParameters;
    }

    /**
     * This method sets the TLS Client Parameters for this HTTPConduit.
     * Using this method overrides any TLS Client Parameters that is configured
     * for this HTTPConduit.
     */
    public void setTlsClientParameters(TLSClientParameters params) {
        this.tlsClientParameters = params;
        if (this.tlsClientParameters != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Conduit '" + getConduitName()
                    + "' has been (re) configured for TLS "
                    + "keyManagers " + Arrays.toString(tlsClientParameters.getKeyManagers())
                    + "trustManagers " + Arrays.toString(tlsClientParameters.getTrustManagers())
                    + "secureRandom " + tlsClientParameters.getSecureRandom());
            }
            CertificateConstraintsType constraints = params.getCertConstraints();
            if (constraints != null) {
                certConstraints = CertConstraintsJaxBUtils.createCertConstraints(constraints);
            }
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Conduit '" + getConduitName()
                    + "' has been (re)configured for plain http.");
            }
        }
    }

    /**
     * This method gets the Trust Decider that was set/configured for this
     * HTTPConduit.
     * @return The Message Trust Decider or null.
     */
    public MessageTrustDecider getTrustDecider() {
        return this.trustDecider;
    }

    /**
     * This method sets the Trust Decider for this HTTP Conduit.
     * Using this method overrides any trust decider configured for this
     * HTTPConduit.
     */
    public void setTrustDecider(MessageTrustDecider decider) {
        this.trustDecider = decider;
    }

    /**
     * This method gets the Auth Supplier that was set/configured for this
     * HTTPConduit.
     * @return The Auth Supplier or null.
     */
    public HttpAuthSupplier getAuthSupplier() {
        return this.authSupplier;
    }

    public void setAuthSupplier(HttpAuthSupplier supplier) {
        this.authSupplier = supplier;
    }

    public HttpAuthSupplier getProxyAuthSupplier() {
        return proxyAuthSupplier;
    }

    public void setProxyAuthSupplier(HttpAuthSupplier proxyAuthSupplier) {
        this.proxyAuthSupplier = proxyAuthSupplier;
    }


    /**
     * This method extracts the value of the "Location" Http
     * Response header.
     *
     * @param headers The Http response headers.
     * @return The value of the "Location" header, null if non-existent.
     * @throws MalformedURLException
     */
    protected String extractLocation(Map<String, List<String>> headers) throws MalformedURLException {
        for (Map.Entry<String, List<String>> head : headers.entrySet()) {
            if ("Location".equalsIgnoreCase(head.getKey())) {
                List<String> locs = head.getValue();
                if (locs != null && !locs.isEmpty()) {
                    String location = locs.get(0);
                    if (location != null) {
                        return location;
                    }
                    return null;
                }
            }
        }
        return null;
    }


    /**
     * Used to set appropriate message properties, exchange etc.
     * as required for an incoming decoupled response (as opposed
     * what's normally set by the Destination for an incoming
     * request).
     */
    protected class InterposedMessageObserver implements MessageObserver {
        /**
         * Called for an incoming message.
         *
         * @param inMessage
         */
        @Override
        @FFDCIgnore(IOException.class)
        public void onMessage(Message inMessage) {
            // disposable exchange, swapped with real Exchange on correlation
            inMessage.setExchange(new ExchangeImpl());
            inMessage.getExchange().put(Bus.class, bus);
            inMessage.put(Message.DECOUPLED_CHANNEL_MESSAGE, Boolean.TRUE);
            // REVISIT: how to get response headers?
            //inMessage.put(Message.PROTOCOL_HEADERS, req.getXXX());
            Headers.getSetProtocolHeaders(inMessage);
            inMessage.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_OK);

            // remove server-specific properties
            inMessage.remove(AbstractHTTPDestination.HTTP_REQUEST);
            inMessage.remove(AbstractHTTPDestination.HTTP_RESPONSE);
            inMessage.remove(Message.ASYNC_POST_RESPONSE_DISPATCH);

            //cache this inputstream since it's defer to use in case of async
            try {
                InputStream in = inMessage.getContent(InputStream.class);
                if (in != null) {
                    CachedOutputStream cos = new CachedOutputStream();
                    IOUtils.copy(in, cos);
                    inMessage.setContent(InputStream.class, cos.getInputStream());
                }
                incomingObserver.onMessage(inMessage);
            } catch (IOException e) {
                logStackTrace(e);
            }
        }
    }

    protected void logStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        LOG.warning(sw.toString());
    }

    @Override
    public void assertMessage(Message message) {
        PolicyDataEngine policyDataEngine = bus.getExtension(PolicyDataEngine.class);
        policyDataEngine.assertMessage(message, getClient(), new ClientPolicyCalculator());
    }

    @Override
    public boolean canAssert(QName type) {
        return type.equals(new QName("http://cxf.apache.org/transports/http/configuration", "client"));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == clientSidePolicy
            && "decoupledEndpoint".equals(evt.getPropertyName())) {
            this.endpointInfo.setProperty("org.apache.cxf.ws.addressing.replyto",
                                          evt.getNewValue());
        }
    }



    /**
     * Wrapper output stream responsible for flushing headers and handling
     * the incoming HTTP-level response (not necessarily the MEP response).
     */
    protected abstract class WrappedOutputStream extends AbstractThresholdOutputStream {
        /**
         * This boolean is true if the request must be cached.
         */
        protected boolean cachingForRetransmission;

        /**
         * If we are going to be chunking, we won't flush till close which causes
         * new chunks, small network packets, etc..
         */
        protected final boolean chunking;

        /**
         * This field contains the output stream with which we cache
         * the request. It maybe null if we are not caching.
         */
        protected CacheAndWriteOutputStream cachedStream;

        protected Message outMessage;

        protected String conduitName;

        protected URI url;

        protected WrappedOutputStream(
                Message outMessage,
                boolean possibleRetransmit,
                boolean isChunking,
                int chunkThreshold,
                String conduitName,
                URI url
        ) {
            super(chunkThreshold);
            this.outMessage = outMessage;
            this.cachingForRetransmission = possibleRetransmit;
            this.chunking = isChunking;
            this.conduitName = conduitName;
            this.url = url;
        }

        // This construction makes extending the HTTPConduit more easier
        protected WrappedOutputStream(WrappedOutputStream wos) {
            super(wos.threshold);
            this.outMessage = wos.outMessage;
            this.cachingForRetransmission = wos.cachingForRetransmission;
            this.chunking = wos.chunking;
            this.conduitName = wos.conduitName;
            this.url = wos.url;
        }

        @Override
        public void thresholdNotReached() {
            if (chunking) {
                setFixedLengthStreamingMode(buffer.size());
            }
        }

        // methods used for the outgoing side
        protected abstract void setupWrappedStream() throws IOException;
        protected abstract HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException;
        protected abstract void setProtocolHeaders() throws IOException;
        protected abstract void setFixedLengthStreamingMode(int i);


        // methods used for the incoming side
        protected abstract int getResponseCode() throws IOException;
        protected abstract String getResponseMessage() throws IOException;
        protected abstract void updateResponseHeaders(Message inMessage) throws IOException;
        protected abstract void handleResponseAsync() throws IOException;
        protected abstract void closeInputStream() throws IOException;
        protected abstract boolean usingProxy();
        protected abstract InputStream getInputStream() throws IOException;
        protected abstract InputStream getPartialResponse() throws IOException;

        //methods to support retransmission for auth or redirects
        protected abstract void setupNewConnection(String newURL) throws IOException;
        protected abstract void retransmitStream() throws IOException;
        protected abstract void updateCookiesBeforeRetransmit() throws IOException;


        protected void handleNoOutput() throws IOException {
            //For GET and DELETE and such, this will be called
            //For some implementations, this notice may be required to
            //actually execute the request
        }


        @FFDCIgnore(RejectedExecutionException.class)
        protected void handleResponseOnWorkqueue(boolean allowCurrentThread, boolean forceWQ) throws IOException {
            Runnable runnable = AsyncClientRunnableWrapperManager.wrap(outMessage, new Runnable() {
                @Override
                @FFDCIgnore(Throwable.class)
                public void run() {
                    try {
                        handleResponseInternal();
                    } catch (Throwable e) {
                        ((PhaseInterceptorChain)outMessage.getInterceptorChain()).abort();
                        outMessage.setContent(Exception.class, e);
                        ((PhaseInterceptorChain)outMessage.getInterceptorChain()).unwind(outMessage);
                        MessageObserver mo = outMessage.getInterceptorChain().getFaultObserver();
                        if (mo == null) {
                            mo = outMessage.getExchange().get(MessageObserver.class);
                        }
                        mo.onMessage(outMessage);
                    }
                }
            });
            HTTPClientPolicy policy = getClient(outMessage);
            boolean exceptionSet = outMessage.getContent(Exception.class) != null;
            if (!exceptionSet) {
                try {
                    Executor ex = outMessage.getExchange().get(Executor.class);
                    if (forceWQ && ex != null) {
                        final Executor ex2 = ex;
                        final Runnable origRunnable = runnable;
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                outMessage.getExchange().put(Executor.class.getName()
                                                             + ".USING_SPECIFIED", Boolean.TRUE);
                                ex2.execute(origRunnable);
                            }
                        };
                    }
                    if (ex == null || forceWQ) {
                        WorkQueueManager mgr = outMessage.getExchange().getBus()
                            .getExtension(WorkQueueManager.class);
                        AutomaticWorkQueue qu = mgr.getNamedWorkQueue("http-conduit");
                        if (qu == null) {
                            qu = mgr.getAutomaticWorkQueue();
                        }
                        long timeout = 1000;
                        if (policy != null && policy.isSetAsyncExecuteTimeout()) {
                            timeout = policy.getAsyncExecuteTimeout();
                        }
                        if (timeout > 0) {
                            qu.execute(runnable, timeout);
                        } else {
                            qu.execute(runnable);
                        }
                    } else {
                        outMessage.getExchange().put(Executor.class.getName()
                                                 + ".USING_SPECIFIED", Boolean.TRUE);
                        if (LOG.isLoggable(Level.FINEST)) {
                            LOG.log(Level.FINEST, "Executing with " + ex);
                        }
                        ex.execute(runnable);
                    }
                } catch (RejectedExecutionException rex) {
                    if (!allowCurrentThread
                        || (policy != null
                        && policy.isSetAsyncExecuteTimeoutRejection()
                        && policy.isAsyncExecuteTimeoutRejection())) {
                        throw rex;
                    }
                    if (!hasLoggedAsyncWarning) {
                        LOG.warning("EXECUTOR_FULL_WARNING");
                        hasLoggedAsyncWarning = true;
                    }
                    LOG.fine("EXECUTOR_FULL");
                    handleResponseInternal();
                }
            }
        }


        protected void retransmit(String newURL) throws IOException {
            setupNewConnection(newURL);
            if (cachedStream != null && cachedStream.size() < Integer.MAX_VALUE) {
                setFixedLengthStreamingMode((int)cachedStream.size());
            }
            setProtocolHeaders();

            //
            // This point is where the trust decision is made because the
            // Sun implementation of URLConnection will not let us
            // set/addRequestProperty after a connect() call, and
            // makeTrustDecision needs to make a connect() call to
            // make sure the proper information is available.
            //
            makeTrustDecision();

            // If this is a GET method we must not touch the output
            // stream as this automagically turns the request into a POST.
            if ("GET".equals(getMethod()) || cachedStream == null) {
                handleNoOutput();
                return;
            }

            // Trust is okay, write the cached request
            retransmitStream();

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Conduit \""
                         + getConduitName()
                         + "\" Retransmit message to: "
                         + newURL
                         + ": "
                         + new String(cachedStream.getBytes()));
            }
        }


        /**
         * Perform any actions required on stream flush (freeze headers,
         * reset output stream ... etc.)
         */
        @Override
        @FFDCIgnore(IOException.class)
        protected void onFirstWrite() throws IOException {
            try {
                handleHeadersTrustCaching();
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("HTTPS hostname wrong:")) {
                    throw new IOException("The https URL hostname does not match the "
                        + "Common Name (CN) on the server certificate in the client's truststore.  "
                        + "Make sure server certificate is correct, or to disable this check "
                        + "(NOT recommended for production) set the CXF client TLS "
                        + "configuration property \"disableCNCheck\" to true.");
                }
                throw e;
            }
        }
        protected String getMethod() {
            return (String)outMessage.get(Message.HTTP_REQUEST_METHOD);
        }


        protected void handleHeadersTrustCaching() throws IOException {
            // Need to set the headers before the trust decision
            // because they are set before the connect().
            setProtocolHeaders();

            //
            // This point is where the trust decision is made because the
            // Sun implementation of URLConnection will not let us
            // set/addRequestProperty after a connect() call, and
            // makeTrustDecision needs to make a connect() call to
            // make sure the proper information is available.
            //
            makeTrustDecision();

            // Trust is okay, set up for writing the request.

            String method = getMethod();
            if (KNOWN_HTTP_VERBS_WITH_NO_CONTENT.contains(method)
                || PropertyUtils.isTrue(outMessage.get(Headers.EMPTY_REQUEST_PROPERTY))) {
                handleNoOutput();
                return;
            }
            setupWrappedStream();
        }


        /**
         * Perform any actions required on stream closure (handle response etc.)
         */
        @Override
        @FFDCIgnore(value = {HttpRetryException.class, IOException.class, RuntimeException.class})
        public void close() throws IOException {
            try {
                if (buffer != null && buffer.size() > 0) {
                    thresholdNotReached();
                    LoadingByteArrayOutputStream tmp = buffer;
                    buffer = null;
                    super.write(tmp.getRawBytes(), 0, tmp.size());
                }
                boolean exceptionSet = outMessage.getContent(Exception.class) != null;
                if (!written && !exceptionSet) {
                    handleHeadersTrustCaching();
                }
                if (!cachingForRetransmission) {
                    super.close();
                } else if (cachedStream != null) {
                    super.flush();
                    cachedStream.getOut().close();
                    cachedStream.closeFlowthroughStream();
                }

                try {
                    handleResponse();
                } finally {
                    if (cachingForRetransmission && cachedStream != null) {
                        cachedStream.close();
                    }
                }
            } catch (HttpRetryException e) {
                handleHttpRetryException(e);
            } catch (IOException e) {
                String origMessage = e.getMessage();
                if (origMessage != null && origMessage.contains(url.toString())) {
                    throw e;
                }
                throw mapException(e.getClass().getSimpleName()
                                   + " invoking " + url + ": "
                                   + e.getMessage(), e,
                                   IOException.class);
            } catch (RuntimeException e) {
                throw mapException(e.getClass().getSimpleName()
                                   + " invoking " + url + ": "
                                   + e.getMessage(), e,
                                   RuntimeException.class);
            }
        }

        @FFDCIgnore(Throwable.class)
        private <T extends Exception> T mapException(String msg,
                                                     T ex, Class<T> cls) {
            T ex2 = ex;
            try {
                ex2 = cls.cast(ex.getClass().getConstructor(String.class).newInstance(msg));
                ex2.initCause(ex);
            } catch (Throwable e) {
                ex2 = ex;
            }


            return ex2;
        }

        /**
         * This procedure handles all retransmits, if any.
         *
         * @throws IOException
         */
        protected void handleRetransmits() throws IOException {
            // If we have a cachedStream, we are caching the request.
            if (cachedStream != null
                || getClient().isAutoRedirect() && KNOWN_HTTP_VERBS_WITH_NO_CONTENT.contains(getMethod())
                || authSupplier != null && authSupplier.requiresRequestCaching()) {

                if (LOG.isLoggable(Level.FINE) && cachedStream != null) {
                    StringBuilder b = new StringBuilder(4096);
                    b.append("Conduit \"").append(getConduitName())
                        .append("\" Transmit cached message to: ")
                        .append(url)
                        .append(": ");
                    cachedStream.writeCacheTo(b, 16L * 1024L);
                    LOG.fine(b.toString());
                }


                int maxRetransmits = getMaxRetransmits();
                updateCookiesBeforeRetransmit();
                int nretransmits = 0;
                while ((maxRetransmits < 0 || nretransmits < maxRetransmits) && processRetransmit()) {
                    nretransmits++;
                }
            }
        }
        /**
         * This function processes any retransmits at the direction of redirections
         * or "unauthorized" responses.
         *
         * @return true if there was a retransmit
         * @throws IOException
         */
        protected boolean processRetransmit() throws IOException {
            int responseCode = getResponseCode();
            if ((outMessage != null) && (outMessage.getExchange() != null)) {
                outMessage.getExchange().put(Message.RESPONSE_CODE, responseCode);
            }
            // Process Redirects first.
            switch(responseCode) {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_SEE_OTHER:
            case 307:
            case 308:
                return redirectRetransmit();
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_PROXY_AUTH:
                return authorizationRetransmit();
            default:
                break;
            }
            return false;
        }

        @FFDCIgnore(value = {IOException.class, URISyntaxException.class})
        protected boolean redirectRetransmit() throws IOException {
            // If we are not redirecting by policy, then we don't.
            if (!getClient(outMessage).isAutoRedirect()) {
                return false;
            }
            Message m = new MessageImpl();
            updateResponseHeaders(m);

            String newURL = extractLocation(Headers.getSetProtocolHeaders(m));
            String urlString = url.toString();

            try {
                newURL = convertToAbsoluteUrlIfNeeded(conduitName, urlString, newURL, outMessage);
                detectRedirectLoop(conduitName, urlString, newURL, outMessage);
                checkAllowedRedirectUri(conduitName, urlString, newURL, outMessage);
            } catch (IOException ex) {
                // Consider introducing ClientRedirectException instead - it will require
                // those client runtimes which want to check for it have a direct link to it
                outMessage.getExchange().put("client.redirect.exception", "true");
                throw ex;
            }

            if (newURL != null) {
                new Headers(outMessage).removeAuthorizationHeaders();

                // If user configured this Conduit with preemptive authorization
                // it is meant to make it to the end. (Too bad that information
                // went to every URL along the way, but that's what the user
                // wants!
                try {
                    setHeadersByAuthorizationPolicy(outMessage, new URI(newURL));
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
                cookies.writeToMessageHeaders(outMessage);
                outMessage.put("transport.retransmit.url", newURL);
                retransmit(newURL);
                return true;
            }
            return false;
        }

        /**
         * This method performs a retransmit for authorization information.
         *
         * @return true if there was a retransmit
         * @throws IOException
         */
        @FFDCIgnore(Throwable.class)
        protected boolean authorizationRetransmit() throws IOException {
            Message m = new MessageImpl();
            updateResponseHeaders(m);
            List<String> authHeaderValues = Headers.getSetProtocolHeaders(m).get("WWW-Authenticate");
            if (authHeaderValues == null) {
                LOG.warning("WWW-Authenticate response header is not set");
                return false;
            }
            HttpAuthHeader authHeader = new HttpAuthHeader(authHeaderValues);
            URI currentURI = url;
            String realm = authHeader.getRealm();
            detectAuthorizationLoop(getConduitName(), outMessage, currentURI, realm);
            AuthorizationPolicy effectiveAthPolicy = getEffectiveAuthPolicy(outMessage);
            String authorizationToken =
                authSupplier.getAuthorization(
                    effectiveAthPolicy, currentURI, outMessage, authHeader.getFullHeader());
            if (authorizationToken == null) {
                // authentication not possible => we give up
                return false;
            }

            try {
                closeInputStream();
            } catch (Throwable t) {
                //ignore
            }
            new Headers(outMessage).setAuthorization(authorizationToken);
            cookies.writeToMessageHeaders(outMessage);
            retransmit(url.toString());
            return true;
        }




        private int getMaxRetransmits() {
            HTTPClientPolicy policy = getClient(outMessage);
            // Default MaxRetransmits is -1 which means unlimited.
            return (policy == null) ? -1 : policy.getMaxRetransmits();
        }

        /**
         * This procedure is called on the close of the output stream so
         * we are ready to handle the response from the connection.
         * We may retransmit until we finally get a response.
         *
         * @throws IOException
         */
        protected void handleResponse() throws IOException {
            // Process retransmits until we fall out.
            handleRetransmits();

            if (outMessage == null
                || outMessage.getExchange() == null
                || outMessage.getExchange().isSynchronous()) {
                handleResponseInternal();
            } else {
                handleResponseAsync();
            }
        }

        /**
         * This predicate returns true if the exchange indicates
         * a oneway MEP.
         *
         * @param exchange The exchange in question
         */
        private boolean isOneway(Exchange exchange) {
            return exchange != null && exchange.isOneWay();
        }

        private boolean doProcessResponse(Message message, int responseCode) {
            // 1. Not oneWay
            if (!isOneway(message.getExchange())) {
                return true;
            }
            // 2. Robust OneWays could have a fault
            return responseCode == 500 && MessageUtils.getContextualBoolean(message, Message.ROBUST_ONEWAY, false);
        }

        protected int doProcessResponseCode() throws IOException {
            Exchange exchange = outMessage.getExchange();
            int rc = getResponseCode();
            if (rc == -1) {
                LOG.warning("HTTP Response code appears to be corrupted");
            }
            if (exchange != null) {
                exchange.put(Message.RESPONSE_CODE, rc);
                if (rc == 404 || rc == 503 || rc == 429) {
                    exchange.put("org.apache.cxf.transport.service_not_available", true);
                }
            }

            // "org.apache.cxf.transport.no_io_exceptions" property should be set in case the exceptions
            // should not be handled here; for example jax rs uses this

            // "org.apache.cxf.transport.process_fault_on_http_400" property should be set in case a
            // soap fault because of a HTTP 400 should be returned back to the client (SOAP 1.2 spec)

            if (rc >= 400 && rc != 500
                && !MessageUtils.getContextualBoolean(outMessage, NO_IO_EXCEPTIONS)
                && (rc > 400 || !MessageUtils.getContextualBoolean(outMessage, PROCESS_FAULT_ON_HTTP_400))) {

                throw new HTTPException(rc, getResponseMessage(), url.toURL());
            }
            return rc;
        }

        protected void handleResponseInternal() throws IOException {
            Exchange exchange = outMessage.getExchange();
            int responseCode = doProcessResponseCode();

            InputStream in = null;
            // oneway or decoupled twoway calls may expect HTTP 202 with no content

            Message inMessage = new MessageImpl();
            inMessage.setExchange(exchange);
            updateResponseHeaders(inMessage);
            inMessage.put(Message.RESPONSE_CODE, responseCode);
            if (MessageUtils.getContextualBoolean(outMessage, SET_HTTP_RESPONSE_MESSAGE, false)) {
                inMessage.put(HTTP_RESPONSE_MESSAGE, getResponseMessage());
            }
            propagateConduit(exchange, inMessage);

            if ((!doProcessResponse(outMessage, responseCode)
                || HttpURLConnection.HTTP_ACCEPTED == responseCode)
                && MessageUtils.getContextualBoolean(outMessage, 
                    Message.PROCESS_202_RESPONSE_ONEWAY_OR_PARTIAL, true)) {
                in = getPartialResponse();
                if (in == null
                    || !MessageUtils.getContextualBoolean(outMessage, Message.PROCESS_ONEWAY_RESPONSE, false)) {
                    // oneway operation or decoupled MEP without
                    // partial response
                    closeInputStream();
                    if (isOneway(exchange) && responseCode > 300) {
                        throw new HTTPException(responseCode, getResponseMessage(), url.toURL());
                    }
                    //REVISIT move the decoupled destination property name into api
                    Endpoint ep = exchange.getEndpoint();
                    if (null != ep && null != ep.getEndpointInfo() && null == ep.getEndpointInfo().
                            getProperty("org.apache.cxf.ws.addressing.MAPAggregator.decoupledDestination")) {
                        // remove callback so that it won't be invoked twice
                        ClientCallback cc = exchange.remove(ClientCallback.class);
                        if (null != cc) {
                            cc.handleResponse(null, null);
                        }
                    }
                    exchange.put("IN_CHAIN_COMPLETE", Boolean.TRUE);
                    
                    exchange.setInMessage(inMessage);
                    return;
                }
            } else {
                //not going to be resending or anything, clear out the stuff in the out message
                //to free memory
                outMessage.removeContent(OutputStream.class);
                if (cachingForRetransmission && cachedStream != null) {
                    cachedStream.close();
                }
                cachedStream = null;
            }

            String charset = HttpHeaderHelper.findCharset((String)inMessage.get(Message.CONTENT_TYPE));
            String normalizedEncoding = HttpHeaderHelper.mapCharset(charset);
            if (normalizedEncoding == null) {
                String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG",
                                                                   LOG, charset).toString();
                LOG.log(Level.WARNING, m);
                throw new IOException(m);
            }
            inMessage.put(Message.ENCODING, normalizedEncoding);
            if (in == null) {
                in = getInputStream();
            }
            if (in == null) {
                // Create an empty stream to avoid NullPointerExceptions
                in = new ByteArrayInputStream(new byte[] {});
            }
            inMessage.setContent(InputStream.class, in);


            incomingObserver.onMessage(inMessage);

        }

        protected void propagateConduit(Exchange exchange, Message in) {
            if (exchange != null) {
                Message out = exchange.getOutMessage();
                if (out != null) {
                    in.put(Conduit.class, out.get(Conduit.class));
                }
            }
        }

        protected void handleHttpRetryException(HttpRetryException e) throws IOException {
            String msg = "HTTP response '" + e.responseCode() + ": "
                + getResponseMessage() + "' invoking " + url;
            switch (e.responseCode()) {
            case HttpURLConnection.HTTP_MOVED_PERM: // 301
            case HttpURLConnection.HTTP_MOVED_TEMP: // 302
            case HttpURLConnection.HTTP_SEE_OTHER:  // 303
            case 307:
                msg += " that returned location header '" + e.getLocation() + "'";
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED: // 401
                if (authorizationPolicy == null || authorizationPolicy.getUserName() == null) {
                    msg += " with NO authorization username configured in conduit " + getConduitName();
                } else {
                    msg += " with authorization username '" + authorizationPolicy.getUserName() + "'";
                }
                break;
            case HttpURLConnection.HTTP_PROXY_AUTH: // 407
                if (proxyAuthorizationPolicy == null || proxyAuthorizationPolicy.getUserName() == null) {
                    msg += " with NO proxy authorization configured in conduit " + getConduitName();
                } else {
                    msg += " with proxy authorization username '"
                        + proxyAuthorizationPolicy.getUserName() + "'";
                }
                if (clientSidePolicy == null || clientSidePolicy.getProxyServer() == null) {
                    if (usingProxy()) {
                        msg += " using a proxy even if NONE is configured in CXF conduit "
                            + getConduitName()
                            + " (maybe one is configured by java.net.ProxySelector)";
                    } else {
                        msg += " but NO proxy was used by the connection (none configured in cxf "
                            + "conduit and none selected by java.net.ProxySelector)";
                    }
                } else {
                    msg += " using " + clientSidePolicy.getProxyServerType() + " proxy "
                        + clientSidePolicy.getProxyServer() + ":"
                        + clientSidePolicy.getProxyServerPort();
                }
                break;
            default:
                // No other type of HttpRetryException should be thrown
                break;
            }
            throw new IOException(msg, e);
        }

        /**
         * This call must take place before anything is written to the
         * URLConnection. The URLConnection.connect() will be called in order
         * to get the connection information.
         *
         * This method is invoked just after setURLRequestHeaders() from the
         * WrappedOutputStream before it writes data to the URLConnection.
         *
         * If trust cannot be established the Trust Decider implemenation
         * throws an IOException.
         *
         * @throws IOException This exception is thrown if trust cannot be
         *                     established by the configured MessageTrustDecider.
         * @see MessageTrustDecider
         */
        @FFDCIgnore(UntrustedURLConnectionIOException.class)
        protected void makeTrustDecision() throws IOException {

            MessageTrustDecider decider2 = outMessage.get(MessageTrustDecider.class);
            if (trustDecider != null || decider2 != null) {
                try {
                    // We must connect or we will not get the credentials.
                    // The call is (said to be) ignored internally if
                    // already connected.
                    HttpsURLConnectionInfo info = getHttpsURLConnectionInfo();
                    if (trustDecider != null) {
                        trustDecider.establishTrust(conduitName,
                                                    info,
                                                    outMessage);
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Trust Decider "
                                + trustDecider.getLogicalName()
                                + " considers Conduit "
                                + conduitName
                                + " trusted.");
                        }
                    }
                    if (decider2 != null) {
                        decider2.establishTrust(conduitName,
                                                info,
                                                outMessage);
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Trust Decider "
                                + decider2.getLogicalName()
                                + " considers Conduit "
                                + conduitName
                                + " trusted.");
                        }
                    }
                } catch (UntrustedURLConnectionIOException untrustedEx) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Trust Decider "
                            + (trustDecider != null ? trustDecider.getLogicalName() : decider2.getLogicalName())
                            + " considers Conduit "
                            + conduitName
                            + " untrusted.", untrustedEx);
                    }
                    throw untrustedEx;
                }
            } else {
                // This case, when there is no trust decider, a trust
                // decision should be a matter of policy.
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "No Trust Decider for Conduit '"
                        + conduitName
                        + "'. An affirmative Trust Decision is assumed.");
                }
            }
        }
    }

    private static void checkAllowedRedirectUri(String conduitName,
                                                String lastURL,
                                                String newURL,
                                                Message message) throws IOException {
        if (newURL != null) {
            URI newUri = URI.create(newURL);

            if (MessageUtils.getContextualBoolean(message, AUTO_REDIRECT_SAME_HOST_ONLY)) {

                URI lastUri = URI.create(lastURL);

                // This can be further restricted to make sure newURL completely contains lastURL
                // though making sure the same HTTP scheme and host are preserved should be enough

                if (!newUri.getScheme().equals(lastUri.getScheme())
                    || !newUri.getHost().equals(lastUri.getHost())) {
                    String msg = "Different HTTP Scheme or Host Redirect detected on Conduit '"
                        + conduitName + "' on '" + newURL + "'";
                    LOG.log(Level.INFO, msg);
                    throw new IOException(msg);
                }
            }

            String allowedRedirectURI = (String)message.getContextualProperty(AUTO_REDIRECT_ALLOWED_URI);
            if (allowedRedirectURI != null && !newURL.startsWith(allowedRedirectURI)) {
                String msg = "Forbidden Redirect URI " + newURL + "detected on Conduit '" + conduitName;
                LOG.log(Level.INFO, msg);
                throw new IOException(msg);
            }

        }
    }

    // http://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-23#section-7.1.2
    // Relative Location values are also supported
    private static String convertToAbsoluteUrlIfNeeded(String conduitName,
                                                       String lastURL,
                                                       String newURL,
                                                       Message message) throws IOException {
        if (newURL != null && !newURL.startsWith("http")) {

            if (MessageUtils.getContextualBoolean(message, AUTO_REDIRECT_ALLOW_REL_URI)) {
                return URI.create(lastURL).resolve(newURL).toString();
            }
            String msg = "Relative Redirect detected on Conduit '"
                + conduitName + "' on '" + newURL + "'";
            LOG.log(Level.INFO, msg);
            throw new IOException(msg);
        }
        return newURL;

    }

    private static void detectRedirectLoop(String conduitName,
                                           String lastURL,
                                           String newURL,
                                           Message message) throws IOException {
        Map<String, Integer> visitedURLs = CastUtils.cast((Map<?, ?>)message.get(KEY_VISITED_URLS));
        if (visitedURLs == null) {
            visitedURLs = new HashMap<>();
            message.put(KEY_VISITED_URLS, visitedURLs);
        }
        Integer visitCount = visitedURLs.get(lastURL);
        if (visitCount == null) {
            visitCount = 1;
        } else {
            visitCount++;
        }
        visitedURLs.put(lastURL, visitCount);

        Integer newURLCount = visitedURLs.get(newURL);
        if (newURL != null && newURLCount != null) {
            // See if we are being redirected in a loop as best we can,
            // using string equality on URL.
            boolean invalidLoopDetected = newURL.equals(lastURL);

            Integer maxSameURICount = PropertyUtils.getInteger(message, AUTO_REDIRECT_MAX_SAME_URI_COUNT);

            if (!invalidLoopDetected) {
                // This new URI was already recorded earlier even though it is not equal to the last URI
                // Example: a-b-a, where 'a' is the new URI. Check if a limited number of occurrences of this URI
                // is allowed, fail by default.
                if (maxSameURICount == null || newURLCount > maxSameURICount) {
                    invalidLoopDetected = true;
                }
            } else if (maxSameURICount != null && newURLCount <= maxSameURICount) {
                // This new URI was already recorded earlier and is the same as the last URI.
                // Example: a-a. But we have a property supporting a limited number of occurrences of this URI.
                // Continue the invocation.
                invalidLoopDetected = false;
            }
            if (invalidLoopDetected) {
                // We are in a redirect loop; -- bail
                String msg = "Redirect loop detected on Conduit '"
                    + conduitName + "' on '" + newURL + "'";
                LOG.log(Level.INFO, msg);
                throw new IOException(msg);
            }
        }
    }
    private static void detectAuthorizationLoop(String conduitName, Message message,
                                                URI currentURL, String realm) throws IOException {
        @SuppressWarnings("unchecked")
        Set<String> authURLs = (Set<String>) message.get(KEY_AUTH_URLS);
        if (authURLs == null) {
            authURLs = new HashSet<>();
            message.put(KEY_AUTH_URLS, authURLs);
        }
        // If we have been here (URL & Realm) before for this particular message
        // retransmit, it means we have already supplied information
        // which must have been wrong, or we wouldn't be here again.
        // Otherwise, the server may be 401 looping us around the realms.
        if (!authURLs.add(currentURL.toString() + realm)) {
            String logMessage = "Authorization loop detected on Conduit \""
                + conduitName
                + "\" on URL \""
                + currentURL
                + "\" with realm \""
                + realm
                + "\"";
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, logMessage);
            }

            throw new IOException(logMessage);
        }
    }
}
