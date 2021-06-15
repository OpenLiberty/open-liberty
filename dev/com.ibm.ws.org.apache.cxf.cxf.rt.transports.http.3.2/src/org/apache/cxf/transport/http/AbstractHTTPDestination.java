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
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.io.CopyingOutputStream;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.policy.PolicyDataEngine;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.AbstractMultiplexDestination;
import org.apache.cxf.transport.Assertor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.policy.impl.ServerPolicyCalculator;
import org.apache.cxf.transport.https.CertConstraints;
import org.apache.cxf.transport.https.CertConstraintsInterceptor;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cxf.exceptions.InvalidCharsetException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Common base for HTTP Destination implementations.
 */
public abstract class AbstractHTTPDestination
    extends AbstractMultiplexDestination
    implements Configurable, Assertor {

    public static final String HTTP_REQUEST = "HTTP.REQUEST";
    public static final String HTTP_RESPONSE = "HTTP.RESPONSE";
    public static final String HTTP_CONTEXT = "HTTP.CONTEXT";
    public static final String HTTP_CONFIG = "HTTP.CONFIG";
    public static final String HTTP_CONTEXT_MATCH_STRATEGY = "HTTP_CONTEXT_MATCH_STRATEGY";

    public static final String RESPONSE_HEADERS_COPIED = "http.headers.copied";
    public static final String RESPONSE_COMMITED = "http.response.done";
    public static final String REQUEST_REDIRECTED = "http.request.redirected";
    public static final String CXF_CONTINUATION_MESSAGE = "cxf.continuation.message";
    public static final String CXF_ASYNC_CONTEXT = "cxf.async.context";

    public static final String SERVICE_REDIRECTION = "http.service.redirection";
    private static final String HTTP_BASE_PATH = "http.base.path";

    private static final String SSL_CIPHER_SUITE_ATTRIBUTE = "javax.servlet.request.cipher_suite";
    private static final String SSL_PEER_CERT_CHAIN_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    private static final String DECODE_BASIC_AUTH_WITH_ISO8859 = "decode.basicauth.with.iso8859";
    //private static final Logger LOG = LogUtils.getL7dLogger(AbstractHTTPDestination.class);
    private static final TraceComponent tc = Tr.register(AbstractHTTPDestination.class);

    protected final Bus bus;
    protected DestinationRegistry registry;
    protected final String path;

    // Configuration values
    protected volatile HTTPServerPolicy serverPolicy;
    protected String contextMatchStrategy = "stem";
    protected boolean fixedParameterOrder;
    protected boolean multiplexWithAddress;
    protected CertConstraints certConstraints;
    protected boolean decodeBasicAuthWithIso8859;
    protected ContinuationProviderFactory cproviderFactory;
    protected boolean enableWebSocket;

    private volatile boolean serverPolicyCalced;

    /**
     * Constructor
     *
     * @param b the associated Bus
     * @param registry the destination registry
     * @param ei the endpoint info of the destination
     * @param path the path
     * @param dp true for adding the default port if it is missing
     * @throws IOException
     */
    public AbstractHTTPDestination(Bus b,
                                   DestinationRegistry registry,
                                   EndpointInfo ei,
                                   String path,
                                   boolean dp)
        throws IOException {
        super(b, getTargetReference(getAddressValue(ei, dp), b), ei);
        this.bus = b;
        this.registry = registry;
        this.path = path;
        //Liberty change - removing refs to isServlet3
        decodeBasicAuthWithIso8859 = PropertyUtils.isTrue(bus.getProperty(DECODE_BASIC_AUTH_WITH_ISO8859));

        initConfig();
    }

    public Bus getBus() {
        return bus;
    }

    private AuthorizationPolicy getAuthorizationPolicyFromMessage(String credentials, SecurityContext sc) {
        if (credentials == null || StringUtils.isEmpty(credentials.trim())) {
            return null;
        }

        final String[] creds = credentials.split(" ");
        String authType = creds[0];
        if ("Basic".equals(authType) && creds.length == 2) {
            String authEncoded = creds[1];
            try {
                byte[] authBytes = Base64Utility.decode(authEncoded);

                if (authBytes == null) {
                    throw new Base64Exception(new Throwable("Invalid Base64 data."));
                }

                String authDecoded = decodeBasicAuthWithIso8859
                    ? new String(authBytes, StandardCharsets.ISO_8859_1) : new String(authBytes);

                int idx = authDecoded.indexOf(':');
                String username = null;
                String password = null;
                if (idx == -1) {
                    username = authDecoded;
                } else {
                    username = authDecoded.substring(0, idx);
                    if (idx < (authDecoded.length() - 1)) {
                        password = authDecoded.substring(idx + 1);
                    }
                }

                AuthorizationPolicy policy = sc.getUserPrincipal() == null
                    ? new AuthorizationPolicy() : new PrincipalAuthorizationPolicy(sc);
                policy.setUserName(username);
                policy.setPassword(password);
                policy.setAuthorizationType(authType);
                return policy;
            } catch (Base64Exception ex) {
                // Invalid authentication => treat as not authenticated or use the Principal
            }
        }
        if (sc.getUserPrincipal() != null) {
            AuthorizationPolicy policy = new PrincipalAuthorizationPolicy(sc);
            policy.setAuthorization(credentials);
            policy.setAuthorizationType(authType);
            return policy;
        }
        return null;
    }

    public static final class PrincipalAuthorizationPolicy extends AuthorizationPolicy {
        final SecurityContext sc;

        public PrincipalAuthorizationPolicy(SecurityContext sc) {
            this.sc = sc;
        }

        public Principal getPrincipal() {
            return sc.getUserPrincipal();
        }

        @Override
        public String getUserName() {
            String name = super.getUserName();
            if (name != null) {
                return name;
            }
            Principal pp = getPrincipal();
            return pp != null ? pp.getName() : null;
        }
    }

    /**
     * @param message the message under consideration
     * @return true iff the message has been marked as oneway
     */
    protected final boolean isOneWay(Message message) {
        return MessageUtils.isOneWay(message);
    }

    @FFDCIgnore({ SuspendedInvocationException.class, Fault.class, RuntimeException.class })
    public void invoke(final ServletConfig config,
                       final ServletContext context,
                       final HttpServletRequest req,
                       final HttpServletResponse resp) throws IOException {
        Message inMessage = retrieveFromContinuation(req);
        if (inMessage == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Create a new message for processing");
            }
            //Liberty perf change - avoid resize and set init size to 32 and factor 1
            inMessage = new MessageImpl(32, 1);
            ExchangeImpl exchange = new ExchangeImpl();
            exchange.setInMessage(inMessage);
            setupMessage(inMessage,
                     config,
                     context,
                     req,
                     resp);

            exchange.setSession(new HTTPSession(req));
            ((MessageImpl) inMessage).setDestination(this);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Get the message from the request for processing");
            }
        }

        copyKnownRequestAttributes(req, inMessage);

        inMessage.put(HttpServletResponse.class, resp); // Liberty change - reqd for SSE see LibertySseEventSinkImpl

        try {
            incomingObserver.onMessage(inMessage);
            invokeComplete(context, req, resp, inMessage);
        } catch (SuspendedInvocationException ex) {
            if (ex.getRuntimeException() != null) {
                throw ex.getRuntimeException();
            }
            //else nothing to do, just finishing the processing
        } catch (Fault ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw ex;
        } catch (RuntimeException ex) {
            throw ex;
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Finished servicing http request on thread: " + Thread.currentThread());
            }
            //clean up address within threadlocal of EndPointInfo   Liberty#3669
            endpointInfo.resetAddress();
        }
    }

    protected void invokeComplete(final ServletContext context,
                                  final HttpServletRequest req,
                                  final HttpServletResponse resp,
                                  Message m) throws IOException {
        ContinuationProvider p = m.get(ContinuationProvider.class);
        if (p != null) {
            p.complete();
        }
    }

    private void copyKnownRequestAttributes(HttpServletRequest request, Message message) {
        message.put(SERVICE_REDIRECTION, request.getAttribute(SERVICE_REDIRECTION));
    }

    protected void setupMessage(final Message inMessage,
                                final ServletConfig config,
                                final ServletContext context,
                                final HttpServletRequest req,
                                final HttpServletResponse resp) throws IOException {
        setupContinuation(inMessage,
                          req,
                          resp);

        final Exchange exchange = inMessage.getExchange();
        DelegatingInputStream in = new DelegatingInputStream(req.getInputStream()) {
            @Override
            public void cacheInput() {
                if (!cached && (exchange.isOneWay() || isWSAddressingReplyToSpecified(exchange))) {
                    //For one-ways and WS-Addressing invocations with ReplyTo address,
                    //we need to cache the values of the HttpServletRequest
                    //so they can be queried later for things like paths and schemes
                    //and such like that.
                    //Please note, exchange used to always get the "current" message
                    exchange.getInMessage().put(HTTP_REQUEST, new HttpServletRequestSnapshot(req));
                }
                super.cacheInput();
            }

            private boolean isWSAddressingReplyToSpecified(Exchange ex) {
                AddressingProperties map = ContextUtils.retrieveMAPs(ex.getInMessage(), false, false, false);
                return map != null && !ContextUtils.isGenericAddress(map.getReplyTo());
            }
        };

        inMessage.setContent(DelegatingInputStream.class, in);
        inMessage.setContent(InputStream.class, in);
        inMessage.put(HTTP_REQUEST, req);
        inMessage.put(HTTP_RESPONSE, resp);
        inMessage.put(HTTP_CONTEXT, context);
        inMessage.put(HTTP_CONFIG, config);
        inMessage.put(HTTP_CONTEXT_MATCH_STRATEGY, contextMatchStrategy);

        inMessage.put(Message.HTTP_REQUEST_METHOD, req.getMethod());
        String requestURI = req.getRequestURI();
        inMessage.put(Message.REQUEST_URI, requestURI);
        String requestURL = req.getRequestURL().toString();
        inMessage.put(Message.REQUEST_URL, requestURL);
        String contextPath = req.getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }
        String servletPath = req.getServletPath();
        if (servletPath == null) {
            servletPath = "";
        }
        String contextServletPath = contextPath + servletPath;
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            inMessage.put(Message.PATH_INFO, contextServletPath + pathInfo);
        } else {
            inMessage.put(Message.PATH_INFO, requestURI);
        }
        if (!StringUtils.isEmpty(requestURI)) {
            int index = requestURL.indexOf(requestURI);
            if (index > 0) {
                // Can be useful for referencing resources with URIs not covered by CXFServlet.
                // For example, if we a have web application name 'app' and CXFServlet listening
                // on "/services/*" then having HTTP_BASE_PATH pointing to say
                // http://localhost:8080/app will make it easy to refer to non CXF resources
                String schemaInfo = requestURL.substring(0, index);
                String basePathWithContextOnly = schemaInfo + contextPath;
                inMessage.put(HTTP_BASE_PATH, basePathWithContextOnly);
            }
        } else if (!StringUtils.isEmpty(servletPath) && requestURL.endsWith(servletPath)) {
            int index = requestURL.lastIndexOf(servletPath);
            if (index > 0) {
                inMessage.put(HTTP_BASE_PATH, requestURL.substring(0, index));
            }
        }
        String contentType = req.getContentType();
        inMessage.put(Message.CONTENT_TYPE, contentType);
        setEncoding(inMessage, req, contentType);

        inMessage.put(Message.QUERY_STRING, req.getQueryString());

        inMessage.put(Message.ACCEPT_CONTENT_TYPE, req.getHeader("Accept"));
        String basePath = getBasePath(contextServletPath);
        if (!StringUtils.isEmpty(basePath)) {
            inMessage.put(Message.BASE_PATH, basePath);
        }
        inMessage.put(Message.FIXED_PARAMETER_ORDER, isFixedParameterOrder());
        inMessage.put(Message.ASYNC_POST_RESPONSE_DISPATCH, Boolean.TRUE);

        SecurityContext httpSecurityContext = new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return req.getUserPrincipal();
            }

            @Override
            public boolean isUserInRole(String role) {
                return req.isUserInRole(role);
            }
        };

        inMessage.put(SecurityContext.class, httpSecurityContext);

        Headers headers = new Headers(inMessage);
        headers.copyFromRequest(req);
        String credentials = headers.getAuthorization();
        AuthorizationPolicy authPolicy = getAuthorizationPolicyFromMessage(credentials,
                                                                           httpSecurityContext);
        inMessage.put(AuthorizationPolicy.class, authPolicy);

        propogateSecureSession(req, inMessage);

        inMessage.put(CertConstraints.class.getName(), certConstraints);
        inMessage.put(Message.IN_INTERCEPTORS,
                Arrays.asList(new Interceptor[] {CertConstraintsInterceptor.INSTANCE}));

    }

    /**
     * Propogate in the message a TLSSessionInfo instance representative
     * of the TLS-specific information in the HTTP request.
     *
     * @param request the Jetty request
     * @param message the Message
     */
    private static void propogateSecureSession(HttpServletRequest request,
                                               Message message) {
        final String cipherSuite =
            (String) request.getAttribute(SSL_CIPHER_SUITE_ATTRIBUTE);
        if (cipherSuite != null) {
            final java.security.cert.Certificate[] certs =
                (java.security.cert.Certificate[]) request.getAttribute(SSL_PEER_CERT_CHAIN_ATTRIBUTE);
            message.put(TLSSessionInfo.class,
                        new TLSSessionInfo(cipherSuite,
                                           null,
                                           certs));
        }
    }

    private String setEncoding(final Message inMessage,
                               final HttpServletRequest req,
                               final String contentType) throws IOException {

        String enc = HttpHeaderHelper.findCharset(contentType);
        if (enc == null) {
            enc = req.getCharacterEncoding();
        }
        // work around a bug with Jetty which results in the character
        // encoding not being trimmed correctly.
        if (enc != null && enc.endsWith("\"")) {
            enc = enc.substring(0, enc.length() - 1);
        }
        if (enc != null || "POST".equals(req.getMethod()) || "PUT".equals(req.getMethod())) {
            //allow gets/deletes/options to not specify an encoding
            String normalizedEncoding = HttpHeaderHelper.mapCharset(enc);
            if (normalizedEncoding == null) {
                // Liberty Change Start
                String m = "Invalid MediaType encoding: " + enc;
                Tr.warning(tc, m);
                throw new InvalidCharsetException(m);
                // Liberty Change End
            }
            inMessage.put(Message.ENCODING, normalizedEncoding);
        }
        return contentType;
    }

    protected Message retrieveFromContinuation(HttpServletRequest req) {
        //Liberty change - removing refs to isServlet3
        return retrieveFromServlet3Async(req);
    }

    protected Message retrieveFromServlet3Async(HttpServletRequest req) {
        try {
            return (Message) req.getAttribute(CXF_CONTINUATION_MESSAGE);
        } catch (Throwable ex) {
            // the request may not implement the Servlet3 API
        }
        return null;
    }

    protected void setupContinuation(Message inMessage,
                                     final HttpServletRequest req,
                                     final HttpServletResponse resp) {
        try {
            if (/* Liberty change - removing refs to isServlet3 */req.isAsyncSupported()) {
                inMessage.put(ContinuationProvider.class.getName(),
                              new Servlet3ContinuationProvider(req, resp, inMessage));
            } else if (cproviderFactory != null) {
                ContinuationProvider p = cproviderFactory.createContinuationProvider(inMessage, req, resp);
                if (p != null) {
                    inMessage.put(ContinuationProvider.class.getName(), p);
                }
            }
        } catch (Throwable ex) {
            // the request may not implement the Servlet3 API
        }
    }

    protected String getBasePath(String contextPath) throws IOException {
        if (StringUtils.isEmpty(endpointInfo.getAddress())) {
            return "";
        }
        return new URL(endpointInfo.getAddress()).getPath();
    }

    protected static EndpointInfo getAddressValue(EndpointInfo ei) {
        return getAddressValue(ei, true);
    }

    protected static EndpointInfo getAddressValue(EndpointInfo ei, boolean dp) {
        if (dp) {

            String eiAddress = ei.getAddress();
            if (eiAddress == null) {
                try {
                    ServerSocket s = new ServerSocket(0);
                    ei.setAddress("http://localhost:" + s.getLocalPort());
                    s.close();
                    return ei;
                } catch (IOException ex) {
                    // problem allocating a random port, go to the default one
                    ei.setAddress("http://localhost");
                }
            }

            String addr = StringUtils.addDefaultPortIfMissing(ei.getAddress());
            if (addr != null) {
                ei.setAddress(addr);
            }
        }
        return ei;
    }

    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    @Override
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        HttpServletResponse response = (HttpServletResponse) inMessage.get(HTTP_RESPONSE);
        return new BackChannelConduit(response);
    }

    private void initConfig() {

        cproviderFactory = bus.getExtension(ContinuationProviderFactory.class);
    }

    private synchronized HTTPServerPolicy calcServerPolicyInternal(Message m) {
        HTTPServerPolicy sp = serverPolicy;
        if (!serverPolicyCalced) {
            PolicyDataEngine pde = bus.getExtension(PolicyDataEngine.class);
            if (pde != null) {
                sp = pde.getServerEndpointPolicy(m, endpointInfo, this, new ServerPolicyCalculator());
            }
            if (null == sp) {
                sp = endpointInfo.getTraversedExtensor(
                        new HTTPServerPolicy(), HTTPServerPolicy.class);
            }
            serverPolicy = sp;
            serverPolicyCalced = true;
        }
        return sp;
    }

    private HTTPServerPolicy calcServerPolicy(Message m) {
        HTTPServerPolicy sp = serverPolicy;
        if (!serverPolicyCalced) {
            sp = calcServerPolicyInternal(m);
        }
        return sp;
    }

    /**
     * On first write, we need to make sure any attachments and such that are still on the incoming stream
     * are read in.  Otherwise we can get into a deadlock where the client is still trying to send the
     * request, but the server is trying to send the response.   Neither side is reading and both blocked
     * on full buffers.  Not a good situation.
     * @param outMessage
     */
    private void cacheInput(Message outMessage) {
        if (outMessage.getExchange() == null) {
            return;
        }
        Message inMessage = outMessage.getExchange().getInMessage();
        if (inMessage == null) {
            return;
        }
        Object o = inMessage.get("cxf.io.cacheinput");
        DelegatingInputStream in = inMessage.getContent(DelegatingInputStream.class);
        if (PropertyUtils.isTrue(o)) {
            Collection<Attachment> atts = inMessage.getAttachments();
            if (atts != null) {
                for (Attachment a : atts) {
                    if (a.getDataHandler().getDataSource() instanceof AttachmentDataSource) {
                        try {
                            ((AttachmentDataSource) a.getDataHandler().getDataSource()).cache(inMessage);
                        } catch (IOException e) {
                            throw new Fault(e);
                        }
                    }
                }
            }
            if (in != null) {
                in.cacheInput();
            }
        } else if (in != null) {
            //We don't need to cache it, but we may need to consume it in order for the client
            // to be able to receive a response. (could be blocked sending)
            //However, also don't want to consume indefinitely.   We'll limit to 16M.
            try {
                IOUtils.consume(in, 16 * 1024 * 1024);
            } catch (Exception ioe) {
                //ignore
            }
        }
    }

    protected OutputStream flushHeaders(Message outMessage) throws IOException {
        return flushHeaders(outMessage, true);
    }

    protected OutputStream flushHeaders(Message outMessage, boolean getStream) throws IOException {
        if (isResponseRedirected(outMessage)) {
            return null;
        }

        cacheInput(outMessage);
        //Liberty code change start
        Headers headers = null;
        HTTPServerPolicy sp = calcServerPolicy(outMessage);
        if (sp != null) {
            headers = new Headers(outMessage);
            headers.setFromServerPolicy(sp);
        }
        //Liberty code change end

        OutputStream responseStream = null;
        boolean oneWay = isOneWay(outMessage);

        HttpServletResponse response = getHttpResponseFromMessage(outMessage);

        int responseCode = MessageUtils.getReponseCodeFromMessage(outMessage);
        if (responseCode >= 300) {
            String ec = (String) outMessage.get(Message.ERROR_MESSAGE);
            if (!StringUtils.isEmpty(ec)) {
                response.sendError(responseCode, ec);
                return null;
            }
        }
        //Liberty code change start
        if (!response.isCommitted()) {
            response.setStatus(responseCode); //Original CXF line
            if (headers == null) {
                headers = new Headers(outMessage);
            }
            headers.copyToResponse(response);
        }
        //Liberty code change end

        outMessage.put(RESPONSE_HEADERS_COPIED, "true");

        if (MessageUtils.hasNoResponseContent(outMessage)) {
            response.setContentLength(0);
            response.flushBuffer();
            closeResponseOutputStream(response);
        } else if (!getStream) {
            closeResponseOutputStream(response);
        } else {
            responseStream = response.getOutputStream();
        }

        if (oneWay) {
            outMessage.remove(HTTP_RESPONSE);
        }
        return responseStream;
    }

    @FFDCIgnore(IllegalStateException.class)
    private void closeResponseOutputStream(HttpServletResponse response) throws IOException {
        try {
            response.getOutputStream().close();
        } catch (IllegalStateException ex) {
            // response.getWriter() has already been called
        }
    }


    private HttpServletResponse getHttpResponseFromMessage(Message message) throws IOException {
        Object responseObj = message.get(HTTP_RESPONSE);
        if (responseObj instanceof HttpServletResponse) {
            return (HttpServletResponse) responseObj;
        } else if (null != responseObj) {
            String m = (new org.apache.cxf.common.i18n.Message("UNEXPECTED_RESPONSE_TYPE_MSG", tc.getLogger(), responseObj.getClass())).toString();
            Tr.warning(tc, m);
            throw new IOException(m);
        } else {
            String m = (new org.apache.cxf.common.i18n.Message("NULL_RESPONSE_MSG", tc.getLogger())).toString();
            //LOG.log(Level.WARNING, m);
            Tr.warning(tc, m);
            throw new IOException(m);
        }
    }

    private boolean isResponseRedirected(Message outMessage) {
        Exchange exchange = outMessage.getExchange();
        return exchange != null
               && Boolean.TRUE.equals(exchange.get(REQUEST_REDIRECTED));
    }

    /**
     * Backchannel conduit.
     */
    public class BackChannelConduit
        extends AbstractDestination.AbstractBackChannelConduit {

        protected HttpServletResponse response;

        BackChannelConduit(HttpServletResponse resp) {
            response = resp;
        }

        /**
         * Send an outbound message, assumed to contain all the name-value
         * mappings of the corresponding input message (if any).
         *
         * @param message the message to be sent.
         */
        @Override
        public void prepare(Message message) throws IOException {
            message.put(HTTP_RESPONSE, response);
            OutputStream os = message.getContent(OutputStream.class);
            if (os == null) {
                message.setContent(OutputStream.class,
                                   new WrappedOutputStream(message));
            }
        }

        @Override
        public void close(Message msg) throws IOException {
            super.close(msg);
            if (msg.getExchange() == null) {
                return;
            }
            Message m = msg.getExchange().getInMessage();
            if (m == null) {
                return;
            }
            InputStream is = m.getContent(InputStream.class);
            if (is != null) {
                try {
                    is.close();
                    m.removeContent(InputStream.class);
                } catch (IOException ioex) {
                    //ignore
                }
            }
        }
    }

    /**
     * Wrapper stream responsible for flushing headers and committing outgoing
     * HTTP-level response.
     */
    private class WrappedOutputStream extends AbstractWrappedOutputStream implements CopyingOutputStream {

        private final Message outMessage;

        WrappedOutputStream(Message m) {
            super();
            this.outMessage = m;
        }

        @Override
        public int copyFrom(InputStream in) throws IOException {
            if (!written) {
                onFirstWrite();
                written = true;
            }
            if (wrappedStream != null) {
                return IOUtils.copy(in, wrappedStream);
            }
            return IOUtils.copy(in, this, IOUtils.DEFAULT_BUFFER_SIZE);
        }

        /**
         * Perform any actions required on stream flush (freeze headers,
         * reset output stream ... etc.)
         */
        @Override
        protected void onFirstWrite() throws IOException {
            OutputStream responseStream = flushHeaders(outMessage);
            if (null != responseStream) {
                wrappedStream = responseStream;
            }
        }

        /**
         * Perform any actions required on stream closure (handle response etc.)
         */
        @Override
        public void close() throws IOException {
            if (!written && wrappedStream == null) {
                OutputStream responseStream = flushHeaders(outMessage, false);
                if (null != responseStream) {
                    wrappedStream = responseStream;
                }
            }
            if (wrappedStream != null) {
                // closing the stream should indirectly call the servlet response's flushBuffer
                wrappedStream.close();
            }
            /*
            try {
                //make sure the input stream is also closed in this
                //case so that any resources it may have is cleaned up
                Message m = outMessage.getExchange().getInMessage();
                if (m != null) {
                    InputStream ins = m.getContent(InputStream.class);
                    if (ins != null) {
                        ins.close();
                    }
                }
            } catch (IOException ex) {
                //ignore
            }
            */
        }

    }

    protected boolean contextMatchOnExact() {
        return "exact".equals(contextMatchStrategy);
    }

    public void finalizeConfig() {}

    @Override
    public String getBeanName() {
        String beanName = null;
        if (endpointInfo.getName() != null) {
            beanName = endpointInfo.getName().toString() + ".http-destination";
        }
        return beanName;
    }

    /*
     * Implement multiplex via the address URL to avoid the need for ws-a.
     * Requires contextMatchStrategy of stem.
     *
     * @see org.apache.cxf.transport.AbstractMultiplexDestination#getAddressWithId(java.lang.String)
     */
    @Override
    public EndpointReferenceType getAddressWithId(String id) {
        EndpointReferenceType ref = null;

        if (isMultiplexWithAddress()) {
            String address = EndpointReferenceUtils.getAddress(reference);
            ref = EndpointReferenceUtils.duplicate(reference);
            if (address.endsWith("/")) {
                EndpointReferenceUtils.setAddress(ref, address + id);
            } else {
                EndpointReferenceUtils.setAddress(ref, address + "/" + id);
            }
        } else {
            ref = super.getAddressWithId(id);
        }
        return ref;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.cxf.transport.AbstractMultiplexDestination#getId(java.util.Map)
     */
    @Override
    public String getId(Map<String, Object> context) {
        String id = null;

        if (isMultiplexWithAddress()) {
            String address = (String) context.get(Message.PATH_INFO);
            if (null != address) {
                int afterLastSlashIndex = address.lastIndexOf('/') + 1;
                if (afterLastSlashIndex > 0
                    && afterLastSlashIndex < address.length()) {
                    id = address.substring(afterLastSlashIndex);
                }
            } else {
                getLogger().log(Level.WARNING,
                                new org.apache.cxf.common.i18n.Message("MISSING_PATH_INFO", tc.getLogger()).toString());
            }
        } else {
            return super.getId(context);
        }
        return id;
    }

    public String getContextMatchStrategy() {
        return contextMatchStrategy;
    }

    public void setContextMatchStrategy(String contextMatchStrategy) {
        this.contextMatchStrategy = contextMatchStrategy;
    }

    public boolean isFixedParameterOrder() {
        return fixedParameterOrder;
    }

    public void setFixedParameterOrder(boolean fixedParameterOrder) {
        this.fixedParameterOrder = fixedParameterOrder;
    }

    public boolean isMultiplexWithAddress() {
        return multiplexWithAddress;
    }

    public void setMultiplexWithAddress(boolean multiplexWithAddress) {
        this.multiplexWithAddress = multiplexWithAddress;
    }

    public HTTPServerPolicy getServer() {
        return calcServerPolicy(null);
    }

    public void setServer(HTTPServerPolicy server) {
        this.serverPolicy = server;
        if (server != null) {
            serverPolicyCalced = true;
        }
    }

    @Override
    public void assertMessage(Message message) {
        PolicyDataEngine pde = bus.getExtension(PolicyDataEngine.class);
        pde.assertMessage(message, calcServerPolicy(message), new ServerPolicyCalculator());
    }

    @Override
    public boolean canAssert(QName type) {
        return new ServerPolicyCalculator().getDataClassName().equals(type);
    }

    public void releaseRegistry() {
        registry = null;
    }

    public String getPath() {
        return path;
    }

    @Override
    protected void activate() {
        synchronized (this) {
            if (registry != null) {
                registry.addDestination(this);
            }
        }
    }

    @Override
    protected void deactivate() {
        synchronized (this) {
            if (registry != null) {
                registry.removeDestination(path);
            }
        }
    }

    @Override
    public void shutdown() {
        synchronized (this) {
            if (registry != null) {
                registry.removeDestination(path);
            }
        }
        super.shutdown();
    }
}
