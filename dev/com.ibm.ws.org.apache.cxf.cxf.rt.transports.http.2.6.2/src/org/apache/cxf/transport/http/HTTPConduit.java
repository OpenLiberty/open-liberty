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
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
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
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
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
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transport.http.auth.SpnegoAuthSupplier;
import org.apache.cxf.transport.http.policy.impl.ClientPolicyCalculator;
import org.apache.cxf.transport.https.CertConstraints;
import org.apache.cxf.transport.https.CertConstraintsInterceptor;
import org.apache.cxf.transport.https.CertConstraintsJaxBUtils;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.workqueue.AutomaticWorkQueue;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import static org.apache.cxf.message.Message.DECOUPLED_CHANNEL_MESSAGE;


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
public class HTTPConduit 
    extends AbstractConduit 
    implements Configurable, Assertor, PropertyChangeListener {  

    /**
     *  This constant is the Message(Map) key for the HttpURLConnection that
     *  is used to get the response.
     */
    public static final String KEY_HTTP_CONNECTION = "http.connection";

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
     * The Logger for this class.
     */
    private static final Logger LOG = LogUtils.getL7dLogger(HTTPConduit.class);

    private static boolean hasLoggedAsyncWarning;
    
    /**
     * This constant holds the suffix ".http-conduit" that is appended to the 
     * Endpoint Qname to give the configuration name of this conduit.
     */
    private static final String SC_HTTP_CONDUIT_SUFFIX = ".http-conduit";

    private static final String HTTP_POST_METHOD = "POST"; 
    private static final String HTTP_PUT_METHOD = "PUT";
    
    /**
     * This field holds the connection factory, which primarily is used to 
     * factor out SSL specific code from this implementation.
     * <p>
     * This field is "protected" to facilitate some contrived UnitTesting so
     * that an extended class may alter its value with an EasyMock URLConnection
     * Factory. 
     */
    protected HttpsURLConnectionFactory connectionFactory;
    
    /**
     *  This field holds a reference to the CXF bus associated this conduit.
     */
    private final Bus bus;

    /**
     * This field is used for two reasons. First it provides the base name for
     * the conduit for Spring configuration. The other is to hold default 
     * address information, should it not be supplied in the Message Map, by the 
     * Message.ENDPOINT_ADDRESS property.
     */
    private final EndpointInfo endpointInfo;
    

    /**
     * This field holds the "default" URL for this particular conduit, which
     * is created on demand.
     */
    private URL defaultEndpointURL;
    private String defaultEndpointURLString;
    private boolean fromEndpointReferenceType;
    
    private ProxyFactory proxyFactory;

    // Configurable values
    
    /**
     * This field holds the QoS configuration settings for this conduit.
     * This field is injected via spring configuration based on the conduit
     * name.
     */
    private HTTPClientPolicy clientSidePolicy;

    /**
     * This field holds the password authorization configuration.
     * This field is injected via spring configuration based on the conduit 
     * name.
    */
    private AuthorizationPolicy authorizationPolicy;
    
    /**
     * This field holds the password authorization configuration for the 
     * configured proxy. This field is injected via spring configuration based 
     * on the conduit name.
     */
    private ProxyAuthorizationPolicy proxyAuthorizationPolicy;

    /**
     * This field holds the configuration TLS configuration which
     * is programmatically configured. 
     */
    private TLSClientParameters tlsClientParameters;
    
    /**
     * This field contains the MessageTrustDecider.
     */
    private MessageTrustDecider trustDecider;
    
    /**
     * Implements the authentication handling when talking to a server. If it is not set
     * it will be created from the authorizationPolicy.authType
     */
    private HttpAuthSupplier authSupplier;
    
    /**
     * Implements the proxy authentication handling. If it is not set
     * it will be created from the proxyAuthorizationPolicy.authType
     */
    private HttpAuthSupplier proxyAuthSupplier;

    private Cookies cookies;
    
    private CertConstraints certConstraints;

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
     * @param endpoint the endpoint info of the initiator.
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
        connectionFactory = new HttpsURLConnectionFactory();
        cookies = new Cookies();
        updateClientPolicy();
        CXFAuthenticator.addAuthenticator();
    }

    /**
     * updates the HTTPClientPolicy that is compatible with the assertions
     * included in the service, endpoint, operation and message policy subjects
     * if a PolicyDataEngine is installed
     * 
     * wsdl extensors are superseded by policies which in 
     * turn are superseded by injection
     */
    private void updateClientPolicy() {
        PolicyDataEngine policyEngine = bus.getExtension(PolicyDataEngine.class);
        if (policyEngine != null && endpointInfo.getService() != null) {
            clientSidePolicy = policyEngine.getClientEndpointPolicy(endpointInfo, 
                                                                    this, new ClientPolicyCalculator());
        }
    }

    /**
     * This method returns the registered Logger for this conduit.
     */
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
    protected void finalizeConfig() {
        // See if not set by configuration, if there are defaults
        // in order from the Endpoint, Service, or Bus.
        
        configureConduitFromEndpointInfo(this, endpointInfo);
        logConfig();
        
        if (getClient().getDecoupledEndpoint() != null) {
            this.endpointInfo.setProperty("org.apache.cxf.ws.addressing.replyto",
                                          getClient().getDecoupledEndpoint());
        }
        if (clientSidePolicy != null) {
            clientSidePolicy.removePropertyChangeListener(this); //make sure we aren't added twice
            clientSidePolicy.addPropertyChangeListener(this);
        }
    }
    
    /**
     * Allow access to the cookies that the conduit is maintaining
     * @return the sessionCookies map
     */
    public Map<String, Cookie> getCookies() {
        return cookies.getSessionCookies();
    }
    
    private HttpURLConnection createConnection(Message message, URL url) throws IOException {
        HTTPClientPolicy csPolicy = getClient(message);
        Proxy proxy = proxyFactory.createProxy(csPolicy , url);
        return connectionFactory.createConnection(tlsClientParameters, proxy, url);
    }

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
    public void prepare(Message message) throws IOException {
        // This call can possibly change the conduit endpoint address and 
        // protocol from the default set in EndpointInfo that is associated
        // with the Conduit.
        URL currentURL = setupURL(message);       

        // The need to cache the request is off by default
        boolean needToCacheRequest = false;
        
        HTTPClientPolicy csPolicy = getClient(message);
        HttpURLConnection connection = createConnection(message, currentURL);
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
        connection.setRequestMethod((null != httpRequestMethod) ? httpRequestMethod : "POST");
                
        boolean isChunking = false;
        int chunkThreshold = 0;
        final AuthorizationPolicy effectiveAuthPolicy = getEffectiveAuthPolicy(message);
        if (this.authSupplier == null) {
            this.authSupplier = createAuthSupplier(effectiveAuthPolicy.getAuthorizationType());
        }
        
        if (this.proxyAuthSupplier == null) {
            this.proxyAuthSupplier = createAuthSupplier(proxyAuthorizationPolicy.getAuthorizationType());
        }

        if (this.authSupplier.requiresRequestCaching()) {
            needToCacheRequest = true;
            isChunking = false;
            LOG.log(Level.FINE,
                    "Auth Supplier, but no Premeptive User Pass or Digest auth (nonce may be stale)"
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
            && isChunkingSupported(message, connection.getRequestMethod())) {
            //TODO: The chunking mode be configured or at least some
            // documented client constant.
            //use -1 and allow the URL connection to pick a default value
            isChunking = true;
            chunkThreshold = csPolicy.getChunkingThreshold();
            if (chunkThreshold <= 0) {
                chunkThreshold = 0;
                connection.setChunkedStreamingMode(-1);                    
            }
        }
        cookies.writeToMessageHeaders(message);

        // The trust decision is relegated to after the "flushing" of the
        // request headers.
        
        // We place the connection on the message to pick it up
        // in the WrappedOutputStream.
        message.put(KEY_HTTP_CONNECTION, connection);
        
        if (certConstraints != null) {
            message.put(CertConstraints.class.getName(), certConstraints);
            message.getInterceptorChain().add(CertConstraintsInterceptor.INSTANCE);
        }

        setHeadersByAuthorizationPolicy(message, currentURL);
        new Headers(message).setFromClientPolicy(getClient(message));
        message.setContent(OutputStream.class, 
                           createOutputStream(message, connection,
                                              needToCacheRequest, 
                                              isChunking,
                                              chunkThreshold));
        // We are now "ready" to "send" the message. 
    }

    protected boolean isChunkingSupported(Message message, String httpMethod) {
        if (HTTP_POST_METHOD.equals(httpMethod)) { 
            return true;
        }
        if (HTTP_PUT_METHOD.equals(httpMethod)) {
            MessageContentsList objs = MessageContentsList.getContentsList(message);
            if (objs != null && objs.size() > 0) {
                Object obj = objs.get(0);
                if (obj.getClass() != String.class 
                    || (obj.getClass() == String.class && ((String)obj).length() > 0)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected OutputStream createOutputStream(Message message, 
                                              HttpURLConnection connection,
                                              boolean needToCacheRequest, 
                                              boolean isChunking,
                                              int chunkThreshold) {
        return new WrappedOutputStream(message, connection,
                                       needToCacheRequest, 
                                       isChunking,
                                       chunkThreshold,
                                       getConduitName());
    }

    private HttpAuthSupplier createAuthSupplier(String authType) {
        if (HttpAuthHeader.AUTH_TYPE_NEGOTIATE.equals(authType)) {
            return new SpnegoAuthSupplier();
        } else if (HttpAuthHeader.AUTH_TYPE_DIGEST.equals(authType)) {
            return new DigestAuthSupplier();
        } else {
            return new DefaultBasicAuthSupplier();
        }
    }

    private static int determineReceiveTimeout(Message message,
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

    private static int determineConnectionTimeout(Message message,
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
    
    public void close(Message msg) throws IOException {
        InputStream in = msg.getContent(InputStream.class);
        try {
            if (in != null) {
                int count = 0;
                byte buffer[] = new byte[1024];
                while (in.read(buffer) != -1
                    && count < 25) {
                    //don't do anything, we just need to pull off the unread data (like
                    //closing tags that we didn't need to read
                    
                    //however, limit it so we don't read off gigabytes of data we won't use.
                    ++count;
                }
            } 
        } finally {
            super.close(msg);
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
     */
    private URL setupURL(Message message) throws MalformedURLException {
        String result = (String)message.get(Message.ENDPOINT_ADDRESS);
        String pathInfo = (String)message.get(Message.PATH_INFO);
        String queryString = (String)message.get(Message.QUERY_STRING);
        if (result == null) {
            if (pathInfo == null && queryString == null) {
                URL url = getURL();
                message.put(Message.ENDPOINT_ADDRESS, defaultEndpointURLString);
                return url;
            }
            result = getURL().toString();
            message.put(Message.ENDPOINT_ADDRESS, result);
        }
        
        // REVISIT: is this really correct?
        if (null != pathInfo && !result.endsWith(pathInfo)) { 
            result = result + pathInfo;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }        
        return new URL(result);    
    }


    /**
     * Close the conduit
     */
    public void close() {
        if (defaultEndpointURL != null) {
            try {
                URLConnection connect = defaultEndpointURL.openConnection();
                if (connect instanceof HttpURLConnection) {
                    ((HttpURLConnection)connect).disconnect();
                }
            } catch (IOException ex) {
                //ignore
            }
            //defaultEndpointURL = null;
        }
        
        if (clientSidePolicy != null) {
            clientSidePolicy.removePropertyChangeListener(this);
        }
    }

    /**
     * @return the default target address
     */
    protected String getAddress() {
        if (defaultEndpointURL != null) {
            return defaultEndpointURLString;
        } else if (fromEndpointReferenceType) {
            return getTarget().getAddress().getValue();
        }
        return endpointInfo.getAddress();
    }

    /**
     * @return the default target URL
     */
    protected URL getURL() throws MalformedURLException {
        return getURL(true);
    }

    /**
     * @param createOnDemand create URL on-demand if null
     * @return the default target URL
     */
    protected synchronized URL getURL(boolean createOnDemand)
        throws MalformedURLException {
        if (defaultEndpointURL == null && createOnDemand) {
            if (fromEndpointReferenceType && getTarget().getAddress().getValue() != null) {
                defaultEndpointURL = new URL(this.getTarget().getAddress().getValue());
                defaultEndpointURLString = defaultEndpointURL.toExternalForm();
                return defaultEndpointURL;
            }
            if (endpointInfo.getAddress() == null) {
                throw new MalformedURLException("Invalid address. Endpoint address cannot be null.");
            }
            defaultEndpointURL = new URL(endpointInfo.getAddress());
            defaultEndpointURLString = defaultEndpointURL.toExternalForm();
        }
        return defaultEndpointURL;
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
     * @param headers
     */
    private void setHeadersByAuthorizationPolicy(
            Message message,
            URL url
    ) {
        Headers headers = new Headers(message);
        AuthorizationPolicy effectiveAuthPolicy = getEffectiveAuthPolicy(message);
        String authString = authSupplier.getAuthorization(effectiveAuthPolicy, url, message, null);
        if (authString != null) {
            headers.setAuthorization(authString);
        }
        
        String proxyAuthString = authSupplier.getAuthorization(proxyAuthorizationPolicy, 
                                                               url, message, null);
        if (proxyAuthString != null) {
            headers.setProxyAuthorization(proxyAuthString);
        }
    }

    /**
     * This is part of the Configurable interface which retrieves the 
     * configuration from spring injection.
     */
    // REVISIT:What happens when the endpoint/bean name is null?
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
     * This function processes any retransmits at the direction of redirections
     * or "unauthorized" responses.
     * <p>
     * If the request was not retransmitted, it returns the given connection. 
     * If the request was retransmitted, it returns the new connection on
     * which the request was sent.
     * 
     * @param connection   The active URL connection.
     * @param message      The outgoing message.
     * @param cachedStream The cached request.
     * @return
     * @throws IOException
     */
    private HttpURLConnection processRetransmit(
        final HttpURLConnection origConnection,
        Message message,
        CacheAndWriteOutputStream cachedStream
    ) throws IOException {

        int responseCode = origConnection.getResponseCode();
        if ((message != null) && (message.getExchange() != null)) {
            message.getExchange().put(Message.RESPONSE_CODE, responseCode);
        }
        HttpURLConnection connection = origConnection;
        // Process Redirects first.
        switch(responseCode) {
        case HttpURLConnection.HTTP_MOVED_PERM:
        case HttpURLConnection.HTTP_MOVED_TEMP:
        case HttpURLConnection.HTTP_SEE_OTHER:    
        case 307:
            connection = redirectRetransmit(origConnection, message, cachedStream);
            break;
        case HttpURLConnection.HTTP_UNAUTHORIZED:
            connection = authorizationRetransmit(origConnection, message, cachedStream);
            break;
        default:
            break;
        }
        return connection;
    }

    /**
     * This method performs a redirection retransmit in response to
     * a 302 or 305 response code.
     *
     * @param connection   The active URL connection
     * @param message      The outbound message.
     * @param cachedStream The cached request.
     * @return This method returns the new HttpURLConnection if
     *         redirected. If it cannot be redirected for some reason
     *         the same connection is returned.
     *         
     * @throws IOException
     */
    private HttpURLConnection redirectRetransmit(
        HttpURLConnection connection,
        Message message,
        CacheAndWriteOutputStream cachedStream
    ) throws IOException {
        
        // If we are not redirecting by policy, then we don't.
        if (!getClient(message).isAutoRedirect()) {
            return connection;
        }
        URL newURL = extractLocation(connection.getHeaderFields());
        detectRedirectLoop(getConduitName(), connection.getURL(), newURL, message);
        if (newURL != null) {
            new Headers(message).removeAuthorizationHeaders();
            
            // If user configured this Conduit with preemptive authorization
            // it is meant to make it to the end. (Too bad that information
            // went to every URL along the way, but that's what the user 
            // wants!
            // TODO: Make this issue a security release note.
            setHeadersByAuthorizationPolicy(message, newURL);
            connection.disconnect();
            return retransmit(newURL, message, cachedStream);
        }
        return connection;
    }

    /**
     * This method performs a retransmit for authorization information.
     * 
     * @param connection The currently active connection.
     * @param message The outbound message.
     * @param cachedStream The cached request.
     * @return A new connection if retransmitted. If not retransmitted
     *         then this method returns the same connection.
     * @throws IOException
     */
    private HttpURLConnection authorizationRetransmit(
        HttpURLConnection connection,
        Message message, 
        CacheAndWriteOutputStream cachedStream
    ) throws IOException {
        HttpAuthHeader authHeader = new HttpAuthHeader(connection.getHeaderField("WWW-Authenticate"));
        URL currentURL = connection.getURL();
        String realm = authHeader.getRealm();
        detectAuthorizationLoop(getConduitName(), message, currentURL, realm);
        AuthorizationPolicy effectiveAthPolicy = getEffectiveAuthPolicy(message);
        String authorizationToken = 
            authSupplier.getAuthorization(
                effectiveAthPolicy, currentURL, message, authHeader.getFullHeader());
        if (authorizationToken == null) {
            // authentication not possible => we give up
            return connection;
        }
        try {
            closeInputStream(connection);
        } catch (Throwable t) {
            //ignore
        }
        new Headers(message).setAuthorization(authorizationToken);
        cookies.writeToMessageHeaders(message);
        return retransmit(currentURL, message, cachedStream);
    }

    /**
     * This method retransmits the request.
     * 
     * @param connection The currently active connection.
     * @param newURL     The newURL to connection to.
     * @param message    The outbound message.
     * @param stream     The cached request.
     * @return           This function returns a new connection if
     *                   retransmitted, otherwise it returns the given
     *                   connection.
     *                   
     * @throws IOException
     */
    private HttpURLConnection retransmit(
            URL                newURL,
            Message            message, 
            CacheAndWriteOutputStream stream
    ) throws IOException {
        HTTPClientPolicy cp = getClient(message);
        HttpURLConnection  connection = createConnection(message, newURL);
        connection.setDoOutput(true);        
        // TODO: using Message context to decided HTTP send properties
        connection.setConnectTimeout((int)cp.getConnectionTimeout());
        connection.setReadTimeout((int)cp.getReceiveTimeout());
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        message.put("http.retransmit.url", newURL.toString());

        // If the HTTP_REQUEST_METHOD is not set, the default is "POST".
        String httpRequestMethod = (String)message.get(Message.HTTP_REQUEST_METHOD);
        connection.setRequestMethod((null != httpRequestMethod) ? httpRequestMethod : "POST");

        message.put(KEY_HTTP_CONNECTION, connection);

        if (stream != null && stream.size() < Integer.MAX_VALUE) {
            connection.setFixedLengthStreamingMode((int)stream.size());
        }

        // Need to set the headers before the trust decision
        // because they are set before the connect().
        new Headers(message).setProtocolHeadersInConnection(connection);
        
        //
        // This point is where the trust decision is made because the
        // Sun implementation of URLConnection will not let us 
        // set/addRequestProperty after a connect() call, and 
        // makeTrustDecision needs to make a connect() call to
        // make sure the proper information is available.
        // 
        TrustDecisionUtil.makeTrustDecision(trustDecider, message, connection, getConduitName());

        // If this is a GET method we must not touch the output
        // stream as this automagically turns the request into a POST.
        if (connection.getRequestMethod().equals("GET")) {
            return connection;
        }
        
        // Trust is okay, write the cached request
        OutputStream out = connection.getOutputStream();
        stream.writeCacheTo(out);
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Conduit \""
                     + getConduitName() 
                     + "\" Retransmit message to: " 
                     + connection.getURL()
                     + ": "
                     + new String(stream.getBytes()));
        }
        return connection;
    }

    private static void detectAuthorizationLoop(String conduitName, Message message, 
                                                URL currentURL, String realm) throws IOException {
        @SuppressWarnings("unchecked")
        Set<String> authURLs = (Set<String>) message.get(KEY_AUTH_URLS);
        if (authURLs == null) {
            authURLs = new HashSet<String>();
            message.put(KEY_AUTH_URLS, authURLs);
        }
        // If we have been here (URL & Realm) before for this particular message
        // retransmit, it means we have already supplied information
        // which must have been wrong, or we wouldn't be here again.
        // Otherwise, the server may be 401 looping us around the realms.
        if (authURLs.contains(currentURL.toString() + realm)) {
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
        // Register that we have been here before we go.
        authURLs.add(currentURL.toString() + realm);
    }

    /**
     * Tracks the visited urls in the message header KEY_VISITED_URLS.
     * If a URL is to be visited twice an exception is thrown
     * 
     * @param conduitName
     * @param lastURL
     * @param newURL
     * @param message
     * @throws IOException
     */
    private static void detectRedirectLoop(String conduitName, 
                                           URL lastURL, 
                                           URL newURL,
                                           Message message) throws IOException {
        @SuppressWarnings("unchecked")
        Set<String> visitedURLs = (Set<String>) message.get(KEY_VISITED_URLS);
        if (visitedURLs == null) {
            visitedURLs = new HashSet<String>();
            message.put(KEY_VISITED_URLS, visitedURLs);
        }
        visitedURLs.add(lastURL.toString());
        if (newURL != null && visitedURLs.contains(newURL.toString())) {
            // See if we are being redirected in a loop as best we can,
            // using string equality on URL.
            // We are in a redirect loop; -- bail
            String msg = "Redirect loop detected on Conduit '" 
                + conduitName + "' on '" + newURL + "'";
            LOG.log(Level.INFO, msg);
            throw new IOException(msg);
        }
    }    
    
    /**
     * This method extracts the value of the "Location" Http
     * Response header.
     * 
     * @param headers The Http response headers.
     * @return The value of the "Location" header, null if non-existent.
     * @throws MalformedURLException 
     */
    private URL extractLocation(Map<String, List<String>> headers
                                ) throws MalformedURLException {
        
        for (Map.Entry<String, List<String>> head : headers.entrySet()) {
            if ("Location".equalsIgnoreCase(head.getKey())) {
                List<String> locs = head.getValue();
                if (locs != null && locs.size() > 0) {
                    String location = locs.get(0);
                    if (location != null) {
                        return new URL(location);
                    } else {
                        return null;
                    }
                }                
            }
        }
        return null;
    }

    /**
     * Wrapper output stream responsible for flushing headers and handling
     * the incoming HTTP-level response (not necessarily the MEP response).
     */
    protected class WrappedOutputStream extends AbstractThresholdOutputStream {
        
        /**
         * This field contains the currently active connection.
         */
        protected HttpURLConnection connection;
        
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

        protected WrappedOutputStream(
                Message outMessage, 
                HttpURLConnection connection,
                boolean possibleRetransmit,
                boolean isChunking,
                int chunkThreshold,
                String conduitName
        ) {
            super(chunkThreshold);
            this.outMessage = outMessage;
            this.connection = connection;
            this.cachingForRetransmission = possibleRetransmit;
            this.chunking = isChunking;
            this.conduitName = conduitName;
        }
        
        // This construction makes extending the HTTPConduit more easier 
        protected WrappedOutputStream(WrappedOutputStream wos) {
            super(wos.threshold);
            this.outMessage = wos.outMessage;
            this.connection = wos.connection;
            this.cachingForRetransmission = wos.cachingForRetransmission;
            this.chunking = wos.chunking;
            this.conduitName = wos.conduitName;
        }
        
        
        @Override
        public void thresholdNotReached() {
            if (chunking) {
                connection.setFixedLengthStreamingMode(buffer.size());
            }
        }

        @Override
        public void thresholdReached() {
            if (chunking) {
                connection.setChunkedStreamingMode(
                    HTTPConduit.this.getClient().getChunkLength());
            }
        }

        /**
         * Perform any actions required on stream flush (freeze headers,
         * reset output stream ... etc.)
         */
        @Override
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
                } else {
                    throw e;
                }
            }
            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sending "
                    + connection.getRequestMethod() 
                    + " Message with Headers to " 
                    + connection.getURL()
                    + " Conduit :"
                    + conduitName
                    + "\n");
            }
        }
        
        protected void handleHeadersTrustCaching() throws IOException {
            // Need to set the headers before the trust decision
            // because they are set before the connect().
            new Headers(outMessage).setProtocolHeadersInConnection(connection);

            //
            // This point is where the trust decision is made because the
            // Sun implementation of URLConnection will not let us 
            // set/addRequestProperty after a connect() call, and 
            // makeTrustDecision needs to make a connect() call to
            // make sure the proper information is available.
            // 
            TrustDecisionUtil.makeTrustDecision(trustDecider, outMessage, connection, conduitName);
            
            // Trust is okay, set up for writing the request.
            
            // If this is a GET method we must not touch the output
            // stream as this automatically turns the request into a POST.
            // Nor it should be done in case of DELETE/HEAD/OPTIONS 
            // - strangely, empty PUTs work ok 
            if (!"POST".equals(connection.getRequestMethod())
                && !"PUT".equals(connection.getRequestMethod())) {
                return;
            }
            if (outMessage.get("org.apache.cxf.post.empty") != null) {
                return;
            }
            
            // If we need to cache for retransmission, store data in a
            // CacheAndWriteOutputStream. Otherwise write directly to the output stream.
            OutputStream cout = null;
            try {
                cout = connection.getOutputStream();
            } catch (SocketException e) {
                if ("Socket Closed".equals(e.getMessage())) {
                    connection.connect();
                    cout = connection.getOutputStream();
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

        /**
         * Perform any actions required on stream closure (handle response etc.)
         */
        public void close() throws IOException {
            try {
                if (buffer != null && buffer.size() > 0) {
                    thresholdNotReached();
                    LoadingByteArrayOutputStream tmp = buffer;
                    buffer = null;
                    super.write(tmp.getRawBytes(), 0, tmp.size());
                }
                if (!written) {
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
                handleHttpRetryException(e, connection);
            } catch (IOException e) {
                String url = connection.getURL().toString();
                String origMessage = e.getMessage();
                if (origMessage != null && origMessage.contains(url)) {
                    throw e;
                }
                throw mapException(e.getClass().getSimpleName() 
                                   + " invoking " + connection.getURL() + ": "
                                   + e.getMessage(), e,
                                   IOException.class);
            } catch (RuntimeException e) {
                throw mapException(e.getClass().getSimpleName() 
                                   + " invoking " + connection.getURL() + ": "
                                   + e.getMessage(), e,
                                   RuntimeException.class);
            }
        }
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
                || ("GET".equals(connection.getRequestMethod()) && getClient().isAutoRedirect())) {

                if (LOG.isLoggable(Level.FINE) && cachedStream != null) {
                    StringBuilder b = new StringBuilder(4096);
                    b.append("Conduit \"").append(getConduitName())
                        .append("\" Transmit cached message to: ")
                        .append(connection.getURL())
                        .append(": ");
                    cachedStream.writeCacheTo(b, 16 * 1024);
                    LOG.fine(b.toString());
                }


                int maxRetransmits = getMaxRetransmits();
                cookies.readFromConnection(connection);
                int nretransmits = 0;
                HttpURLConnection oldcon = null;
                while (connection != oldcon && (maxRetransmits < 0 || nretransmits < maxRetransmits)) {
                    nretransmits++;
                    oldcon = connection;
                    connection = processRetransmit(connection, outMessage, cachedStream);
                }
            }
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
                Runnable runnable = new Runnable() {
                    public void run() {
                        try {
                            handleResponseInternal();
                        } catch (Exception e) {
                            ((PhaseInterceptorChain)outMessage.getInterceptorChain()).abort();
                            outMessage.setContent(Exception.class, e);
                            ((PhaseInterceptorChain)outMessage.getInterceptorChain()).unwind(outMessage);
                            outMessage.getInterceptorChain().getFaultObserver().onMessage(outMessage);
                        }
                    }
                };
                HTTPClientPolicy policy = getClient(outMessage);
                try {
                    Executor ex = outMessage.getExchange().get(Executor.class);
                    if (ex == null) {
                        WorkQueueManager mgr = outMessage.getExchange().get(Bus.class)
                            .getExtension(WorkQueueManager.class);
                        AutomaticWorkQueue qu = mgr.getNamedWorkQueue("http-conduit");
                        if (qu == null) {
                            qu = mgr.getAutomaticWorkQueue();
                        }
                        long timeout = 5000;
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
                        ex.execute(runnable);
                    }
                } catch (RejectedExecutionException rex) {
                    if (policy != null && policy.isSetAsyncExecuteTimeoutRejection()
                        && policy.isAsyncExecuteTimeoutRejection()) {
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

        /**
         * This predicate returns true iff the exchange indicates 
         * a oneway MEP.
         * 
         * @param exchange The exchange in question
         */
        private boolean isOneway(Exchange exchange) {
            return exchange != null && exchange.isOneWay();
        }
        
        private boolean doProcessResponse(Message message) {
            // 1. Not oneWay
            if (!isOneway(message.getExchange())) {
                return true;
            }
            // 2. Context property
            return MessageUtils.getContextualBoolean(message, Message.PROCESS_ONEWAY_RESPONSE, false);
        }

        protected void handleResponseInternal() throws IOException {
            Exchange exchange = outMessage.getExchange();
            int responseCode = connection.getResponseCode();
            if (responseCode == -1) {
                LOG.warning("HTTP Response code appears to be corrupted");
            }
            if (exchange != null) {
                exchange.put(Message.RESPONSE_CODE, responseCode);
            }
            
            logResponseInfo(responseCode);
            
            // This property should be set in case the exceptions should not be handled here
            // For example jax rs uses this
            boolean noExceptions = MessageUtils.isTrue(outMessage.getContextualProperty(
                "org.apache.cxf.http.no_io_exceptions"));
            if (responseCode >= 400 && responseCode != 500 && !noExceptions) {
                throw new HTTPException(responseCode, connection.getResponseMessage(), 
                                        connection.getURL());
            }

            InputStream in = null;
            Message inMessage = new MessageImpl();
            inMessage.setExchange(exchange);
            new Headers(inMessage).readFromConnection(connection);
            inMessage.put(Message.RESPONSE_CODE, responseCode);
            cookies.readFromConnection(connection);
            // oneway or decoupled twoway calls may expect HTTP 202 with no content
            if (isOneway(exchange) 
                || HttpURLConnection.HTTP_ACCEPTED == responseCode) {
                in = ChunkedUtil.getPartialResponse(connection, responseCode);
                if ((in == null) || (!doProcessResponse(outMessage))) {
                    // oneway operation or decoupled MEP without 
                    // partial response
                    closeInputStream(connection);
                    if (isOneway(exchange) && responseCode > 300) {
                        throw new HTTPException(responseCode, connection.getResponseMessage(), connection.getURL());
                    }
                    ClientCallback cc = exchange.get(ClientCallback.class);
                    if (null != cc) {
                        //REVISIT move the decoupled destination property name into api
                        Endpoint ep = exchange.getEndpoint();
                        if (null != ep && null != ep.getEndpointInfo() && null == ep.getEndpointInfo().
                            getProperty("org.apache.cxf.ws.addressing.MAPAggregator.decoupledDestination")) {
                            cc.handleResponse(null, null);
                        }
                    }
                    if (in != null) {
                        in.close();
                    }
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
            
            String ct = connection.getContentType();
            inMessage.put(Message.CONTENT_TYPE, ct);
            String charset = HttpHeaderHelper.findCharset(ct);
            String normalizedEncoding = HttpHeaderHelper.mapCharset(charset);
            if (normalizedEncoding == null) {
                String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG",
                                                                   LOG, charset).toString();
                LOG.log(Level.WARNING, m);
                throw new IOException(m);   
            } 
            inMessage.put(Message.ENCODING, normalizedEncoding);
            if (in == null) {
                if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
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
            }
            if (in == null) {
                // Create an empty stream to avoid NullPointerExceptions
                in = new ByteArrayInputStream(new byte[] {});
            }
            inMessage.setContent(InputStream.class, in);
            
            
            incomingObserver.onMessage(inMessage);
            
        }


        private void logResponseInfo(int responseCode) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Response Code: " + responseCode + " Conduit: " + conduitName);
                LOG.fine("Content length: " + connection.getContentLength());
                Map<String, List<String>> headerFields = connection.getHeaderFields();
                if (null != headerFields) {
                    String newLine = SystemPropertyAction.getProperty("line.separator");
                    StringBuilder buf = new StringBuilder();
                    buf.append("Header fields: " + newLine);
                    for (String headerKey : headerFields.keySet()) {
                        buf.append("    " + headerKey + ": " + headerFields.get(headerKey) + newLine);
                    }
                    LOG.fine(buf.toString());
                }
            }
        }

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
        public void onMessage(Message inMessage) {
            // disposable exchange, swapped with real Exchange on correlation
            inMessage.setExchange(new ExchangeImpl());
            inMessage.getExchange().put(Bus.class, bus);
            inMessage.put(DECOUPLED_CHANNEL_MESSAGE, Boolean.TRUE);
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
                e.printStackTrace();
            }
        }
    }
    
    public void assertMessage(Message message) {
        PolicyDataEngine policyDataEngine = bus.getExtension(PolicyDataEngine.class);
        policyDataEngine.assertMessage(message, getClient(), new ClientPolicyCalculator());
    }
    
    protected void closeInputStream(HttpURLConnection connection) throws IOException {
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

    public boolean canAssert(QName type) {
        return new ClientPolicyCalculator().equals(type);  
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == clientSidePolicy
            && "decoupledEndpoint".equals(evt.getPropertyName())) {
            this.endpointInfo.setProperty("org.apache.cxf.ws.addressing.replyto",
                                          evt.getNewValue());
        }
    }

    private void handleHttpRetryException(HttpRetryException e, HttpURLConnection connection) 
        throws IOException {
        String msg = "HTTP response '" + e.responseCode() + ": "
                     + connection.getResponseMessage() + "' invoking " + connection.getURL();
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
                if (connection.usingProxy()) {
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
        // pass cause with initCause() instead of constructor for jdk 1.5 compatibility
        throw (IOException) new IOException(msg).initCause(e);
    }
    
}
