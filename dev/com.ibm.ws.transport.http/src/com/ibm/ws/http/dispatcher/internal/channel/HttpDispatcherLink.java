/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.channel;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.h2internal.H2InboundLink;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannel;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.classify.DecoratedExecutorThread;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.internal.VirtualHostImpl;
import com.ibm.ws.http.internal.VirtualHostMap;
import com.ibm.ws.http.internal.VirtualHostMap.RequestHelper;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundApplicationLink;
import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpDateFormat;
import com.ibm.wsspi.http.HttpOutputStream;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.http.HttpResponse;
import com.ibm.wsspi.http.SSLContext;
import com.ibm.wsspi.http.URLEscapingUtils;
import com.ibm.wsspi.http.WorkClassifier;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.ee7.HttpInboundConnectionExtended;
import com.ibm.wsspi.http.ee8.Http2InboundConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Connection link object that the HTTP dispatcher provides to CHFW
 * for an individual connection.
 */
public class HttpDispatcherLink extends InboundApplicationLink implements HttpInboundConnectionExtended, RequestHelper, Http2InboundConnection {
    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpDispatcherLink.class);

    /** Id used to find this link in intermediate maps */
    public static final String LINK_ID = "HttpDispatcherLink";

    private enum UsePrivateHeaders {
        unknown(true), // default
        yes(true),
        no(false);

        static UsePrivateHeaders set(boolean useHeaders) {
            if (useHeaders)
                return yes;
            else
                return no;
        }

        private final boolean enabled;

        UsePrivateHeaders(boolean enabled) {
            this.enabled = enabled;
        }

        boolean asBoolean() {
            return enabled;
        }
    };

    /** Channel that owns this link object */
    private HttpDispatcherChannel myChannel = null;
    /** Wrapper for a request */
    private HttpRequestImpl request = null;
    /** Wrapper for a response */
    private HttpResponseImpl response = null;
    /** Wrapper for possible SSL data */
    private SSLContext sslinfo = null;
    /** Reference to the HTTP channel context object */
    private HttpInboundServiceContextImpl isc = null;
    /** Cached local host name */
    private String localCanonicalHostName = null;
    /** Cached local host:port alias */
    private String localHostAlias = null;
    /** Cached remote origin */
    private String remoteContextAddress;

    private volatile boolean linkIsReady = false;
    private volatile UsePrivateHeaders usePrivateHeaders = UsePrivateHeaders.unknown;
    private volatile int configUpdate = 0;

    private final Object WebConnCanCloseSync = new Object();
    private boolean WebConnCanClose = true;
    private final String h2InitError = "com.ibm.ws.transport.http.http2InitError";

    /**
     * Constructor.
     *
     */
    public HttpDispatcherLink() {
        // nothing
    }

    /**
     * Initialize this link with the input information.
     *
     * @param inVC
     * @param channel
     */
    public void init(VirtualConnection inVC, HttpDispatcherChannel channel) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "New conn: vc=" + inVC);
        }
        super.init(inVC);
        inVC.getStateMap().put(LINK_ID, this);
        this.myChannel = channel;
        this.request = new HttpRequestImpl(HttpDispatcher.useEE7Streams());
        this.response = new HttpResponseImpl(this);

    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#close(VirtualConnection, Exception)
     */
    @Override
    public void close(VirtualConnection conn, Exception e) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Close called , vc ->" + this.vc);
        }

        if (this.vc == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Connection must be already closed since vc is null");
            }
            return;
        }

        // This is added for Upgrade Servlet3.1 WebConnection
        // The only API available from connectionLink are close and destroy ,
        // so we will have to use close API from SRTConnectionContext31 and call closeStreams.
        String closeNonUpgraded = (String) (this.vc.getStateMap().get(TransportConstants.CLOSE_NON_UPGRADED_STREAMS));
        if (closeNonUpgraded != null && closeNonUpgraded.equalsIgnoreCase("true")) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close streams from HttpDispatcherLink.close");
            }

            Exception errorinClosing = this.closeStreams();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error closing in streams" + errorinClosing);
            }

            vc.getStateMap().put(TransportConstants.CLOSE_NON_UPGRADED_STREAMS, "CLOSED_NON_UPGRADED_STREAMS");
            return;
        }

        String upgradedListener = (String) (this.vc.getStateMap().get(TransportConstants.UPGRADED_LISTENER));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "upgradedListener ->" + upgradedListener);
        }
        if (upgradedListener != null && upgradedListener.equalsIgnoreCase("true")) {
            boolean closeCalledFromWebConnection = false;

            synchronized (this) {
                //This sync block prevents both closes from happening, if they are happening at the same time.
                //This will check the new variable we have added to the VC during the WebConnection close.
                //If both the WebConnection and WebContainer close happen at the same time then only one will happen.
                //The first one will come in, check this new variable, then set it to false. The false will cause
                //the other close to not happen.

                String fromWebConnection = (String) (this.vc.getStateMap().get(TransportConstants.CLOSE_UPGRADED_WEBCONNECTION));//Add a new variable here
                if (fromWebConnection != null && fromWebConnection.equalsIgnoreCase("true")) {
                    closeCalledFromWebConnection = true;
                    this.vc.getStateMap().put(TransportConstants.CLOSE_UPGRADED_WEBCONNECTION, "false");//Add a new variable here
                }
            }

            if (!closeCalledFromWebConnection) {
                // we should not call close as this from webcontainer as Webconnection close will be called some point.

                // closeCalledFromWebConnection seems to be related to generic web connections and read async logic.
                // but we need to handle the case where webconnection logic like HTTP/2 need to ensure close is called once, and only once.
                // but we don't want to manipulate existing logic so a separate constant in the state map will be used for that below

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Connection Not to be closed here because Servlet Upgrade.");
                }
                return;
            }
        } else {
            if (upgradedListener == null) {
                String toClose = (String) (vc.getStateMap().get(TransportConstants.UPGRADED_WEB_CONNECTION_NEEDS_CLOSE));
                if ((toClose != null) && (toClose.compareToIgnoreCase("true") == 0)) {
                    // want to close down at least once, and only once, for this type of upgraded connection
                    synchronized (WebConnCanCloseSync) {
                        if (WebConnCanClose) {
                            // fall through to close logic after setting flag to only fall through once
                            // want to call close outside of the sync to avoid deadlocks.
                            WebConnCanClose = false;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Upgraded Web Connection closing Dispatcher Link");
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Upgraded Web Connection already called close; returning");
                            }
                            return;
                        }
                    }
                }
            }
        }

        // don't call close, if the channel has already seen the stop(0) signal, or else this will cause race conditions in the channels below us.
        if (myChannel.getStop0Called() == false) {
            super.close(conn, e);
            this.myChannel.decrementActiveConns();
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#destroy(java.lang.Exception)
     */
    @Override
    public void destroy(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroy with exc=" + e);
        }

        linkIsReady = false;

        // if this was an http upgrade connection, then tell it to close also.
        VirtualConnection vc = getVC();
        if (vc != null) {
            String upgraded = (String) (vc.getStateMap().get(TransportConstants.UPGRADED_CONNECTION));
            if (upgraded != null) {
                if (upgraded.compareToIgnoreCase("true") == 0) {
                    Object webConnectionObject = vc.getStateMap().get(TransportConstants.UPGRADED_WEB_CONNECTION_OBJECT);
                    if (webConnectionObject != null) {
                        if (webConnectionObject instanceof TransportConnectionAccess) {
                            TransportConnectionAccess tWebConn = (TransportConnectionAccess) webConnectionObject;
                            try {
                                tWebConn.close();
                            } catch (Exception webConnectionCloseException) {
                                //continue closing other resources
                                //I don't believe the close operation should fail - but record trace if it does
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Failed to close WebConnection {0}", webConnectionCloseException);
                                }
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "call application destroy if not done yet");
                            }
                        }
                    }
                }
            }
        }

        super.destroy();
        this.isc = null;
        this.request = null;
        this.response = null;
        this.sslinfo = null;
    }

    /**
     * Handle a new HTTP/2 link initialized via ALPN or directly via h2-with-prior-knowledge
     */
    public void directHttp2Ready() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "directHttp2Ready entry: " + this);
        }
        H2InboundLink h2link = new H2InboundLink(getHttpInboundLink2().getChannel(), vc, getTCPConnectionContext());
        h2link.reinit(this.getTCPConnectionContext(), vc, h2link);
        h2link.handleHTTP2DirectConnect(h2link);
        this.setDeviceLink(h2link);
        h2link.processRead(vc, this.getTCPConnectionContext().getReadInterface());
    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#ready(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    @FFDCIgnore(Throwable.class)
    public void ready(VirtualConnection inVC) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Received HTTP connection: " + inVC);
        }

        this.myChannel.incrementActiveConns();
        init(inVC);
        this.isc = (HttpInboundServiceContextImpl) getDeviceLink().getChannelAccessor();

        // if this is an http/2 link, process via that ready
        if (this.getHttpInboundLink2().isDirectHttp2Link(inVC)) {
            directHttp2Ready();
            return;
        }

        // Make sure to initialize the response in case of an early-return-error message
        this.response.init(this.isc);
        linkIsReady = true;

        ExecutorService executorService = HttpDispatcher.getExecutorService();
        if (null == executorService) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Missing executor service");
            }
            // If we got here, we didn't write any content.. last parameter is false
            sendResponse(StatusCodes.UNAVAILABLE, null, false);
            return;
        }

        // Initialize the request body / get the message
        this.request.init(this.isc);

        // Try to find a virtual host for the requested host/port..
        VirtualHostImpl vhost = VirtualHostMap.findVirtualHost(this.myChannel.getEndpointPid(),
                                                               this);
        if (vhost == null) {
            String url = this.isc.getRequest().getRequestURI();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                String alias = getLocalHostAlias();
                Tr.debug(tc, "No virtual host found for this alias: " + alias);
            }
            send404Message(url);
            return;
        }

        Runnable handler = null;
        try {
            handler = vhost.discriminate(this);
            if (handler == null) {
                InputStream landingPageStream = getLandingPageStream();
                if (landingPageStream != null) {
                    displayLandingPage(landingPageStream);
                } else {
                    String url = this.isc.getRequest().getRequestURI();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        String alias = getLocalHostAlias();
                        Tr.debug(tc, "The URI was not associated with the virtual host " + vhost.getName(),
                                 alias, url);
                    }

                    send404Message(url);
                }
            } else {
                wrapHandlerAndExecute(handler);
            }
        } catch (Throwable t) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Exception during dispatch; " + t);
            }

            if (t instanceof Exception) {
                sendResponse(StatusCodes.INTERNAL_ERROR, (Exception) t, true);
            } else {
                sendResponse(StatusCodes.INTERNAL_ERROR, new Exception("Dispatch error", t), true);
            }
        }
    }

    /**
     * Note if the signature of this method is changed, the signature in
     * HttpDispatcherLinkWrapHandlerAndExecuteTransformDescriptor.java
     * needs to be updated.
     */
    private void wrapHandlerAndExecute(Runnable handler) {
        // wrap handler and execute
        TaskWrapper taskWrapper = new TaskWrapper(handler, this);

        WorkClassifier workClassifier = HttpDispatcher.getWorkClassifier();
        if (workClassifier != null) {
            // Obtain the Executor from the WorkClassifier
            // TODO: the WLM classifier uses getVirtualHost and getVirtualPort, which may have
            // a different answer than what was used to find the virtual host (based on plugin headers,
            // and whether or not the Host header, etc. should be used)
            // Does it matter?
            Executor classifyExecutor = workClassifier.classify(this.request, this);

            if (classifyExecutor != null) {
                taskWrapper.setClassifiedExecutor(classifyExecutor);
                classifyExecutor.execute(taskWrapper);
            } else {
                taskWrapper.run();
            }
        } else {
            taskWrapper.run();
        }
    }

    @Override
    public TCPConnectionContext getTCPConnectionContext() {
        // give access to the tcp connection to http upgraded connections.
        // TODO: would be better not to have to do this here, but go through the WebConnection stream object - need to re-visit this
        TCPConnectionContext tcc = null;
        if (isc != null) {
            tcc = isc.getTSC();
        } else {
            // TODO: does it make sense to get the TCP conn context ourselves, if there is no isc?
        }

        return tcc;
    }

    @Override
    public VirtualConnection getVC() {
        if (isc != null) {
            return isc.getVC();
        }
        return null;
    }

    @Override
    public ConnectionLink getHttpInboundDeviceLink() {
        if ((isc != null) && (isc.getLink() != null)) {
            return isc.getLink().getDeviceLink();
        }
        return null;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpInboundConnection#getHttpInboundLink()
     */
    @Override
    public ConnectionLink getHttpInboundLink() {
        if (isc != null) {
            return isc.getLink();
        }
        return null;

    }

    @Override
    public ConnectionLink getHttpDispatcherLink() {
        return this;
    }

    private InputStream getLandingPageStream() {
        if (!!!HttpDispatcher.isWelcomePageEnabled())
            return null;

        String theURI = this.isc.getRequest().getRequestURI();

        return WelcomePageHelper.getWelcomePageStream(theURI);
    }

    private InputStream getNotFoundStream() {
        if (!!!HttpDispatcher.isWelcomePageEnabled())
            return null;
        return WelcomePageHelper.getNotFoundStream();
    }

    private void displayLandingPage(InputStream in) throws IOException {
        displayPage(in, StatusCodes.OK);
    }

    private void displayPage(InputStream inputStream, StatusCodes status) throws IOException {
        HttpOutputStream body = this.response.getBody();

        try {
            if (exists(inputStream)) {
                String theURI = this.isc.getRequest().getRequestURI();
                // for OK responses that are not index.html, set the cache-control header to a year
                // If someone assigns a web-app for the root context, whatever is in that application
                // should get picked up instead of our welcome page w/o having to clear the cache
                if (status == StatusCodes.OK && !theURI.endsWith(".html")) {
                    this.response.setHeader(HttpHeaderKeys.HDR_CACHE_CONTROL.getName(), "max-age=604800");
                }

                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    // send response
                    body.write(buffer, 0, len);
                }
            }
        } finally {
            tryToCloseStream(inputStream);
        }
        sendResponse(status, null, null, false);
    }

    @FFDCIgnore(Throwable.class)
    private void send404Message(String url) {
        String s = HttpDispatcher.getContextRootNotFoundMessage();
        boolean addAddress = false;
        if ((s == null) || (s.isEmpty())) {
            if (HttpDispatcher.isWelcomePageEnabled()) {
                InputStream notFoundPage = getNotFoundStream();
                try {
                    displayPage(notFoundPage, StatusCodes.NOT_FOUND);
                } catch (Throwable t) {
                    // no FFDC required
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Exception displaying error page; " + t);
                    }

                    if (t instanceof Exception) {
                        sendResponse(StatusCodes.INTERNAL_ERROR, (Exception) t, true);
                    } else {
                        sendResponse(StatusCodes.INTERNAL_ERROR, new Exception("Error page", t), true);
                    }
                }
                return;
            } else {
                String safeUrl = URLEscapingUtils.toSafeString(url);
                s = Tr.formatMessage(tc, "Missing.App.Or.Context.Root.No.Error.Code", safeUrl);
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "send error with following string: " + s);
        }

        if (s != null && HttpDispatcher.padContextRootNotFoundMessage()) {
            //There is a problem with some IE browsers that won't display a 404 error page if it is less than 512 bytes.
            //append some characters in a comment to make sure that this message is displayed.
            int difference = 513 - s.length();
            if (difference > 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "404 message is not 512 so pad it. Length = " + s.length());
                }
                StringBuffer sb = new StringBuffer(s);
                String beginComment = " <!--A comment to allow the error page to be greater than 512 bytes:";
                difference -= beginComment.length();
                String endComment = "--!> ";
                sb.append(beginComment);
                for (int i = 0; i < difference; i += 50) {
                    sb.append("12345678901234567890123456789012345678901234567890");
                }
                sb.append(endComment);
                s = sb.toString();
            }
        }

        // If we got here, we didn't write the page yet.. last parameter is false
        sendResponse(StatusCodes.NOT_FOUND, s, null, addAddress);
    }

    /**
     * Send the given error status code on the connection and close the socket.
     *
     * @param code
     * @param failure
     */
    private void sendResponse(StatusCodes code, Exception failure, boolean addAddress) {
        sendResponse(code, null, failure, addAddress);
    }

    /**
     * Send the given error status code on the connection and close the socket.
     *
     * @param code
     * @param failure
     * @param         message/body
     */
    @FFDCIgnore(IOException.class)
    private void sendResponse(StatusCodes code, String detail, Exception failure, boolean addAddress) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sending HTTP response: " + code);
        }

        final HttpInboundServiceContextImpl finalSc = this.isc;
        final HttpResponseImpl finalResponse = this.response;

        if (finalSc == null || finalResponse == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to send response, isc= " + finalSc + ", response=" + finalResponse);
            }
            return;
        }

        HttpResponseMessage rMsg = finalSc.getResponse();
        setResponseProperties(rMsg, code);

        HttpOutputStream body = finalResponse.getBody();

        // Only create this default/bare-bones page if there is no buffered content already..
        if (code.isBodyAllowed() && !body.hasBufferedContent()) {
            try {
                final byte bits[][] = new byte[][] { "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">".getBytes(),
                                                     "<html><head><title>".getBytes(),
                                                     "</title></head><body><h1>".getBytes(),
                                                     "</h1><p>".getBytes(),
                                                     "</p><hr /><address>".getBytes(),
                                                     "</address></body></html>".getBytes(),
                                                     "</p></body></html>".getBytes() };

                final byte[] at = " at ".getBytes();
                final byte[] port = " port ".getBytes();

                byte[] msg;

                body.write(bits[0]); // doctype
                body.write(bits[1]); // header-> title

                msg = code.getStatusWithPhrase();
                body.write(msg); // - title
                body.write(bits[2]); // title, body, h1

                msg = code.getDefaultPhraseBytes();
                body.write(msg); // - status phrase as header
                body.write(bits[3]); // h1, p

                if (detail != null) {
                    msg = detail.getBytes();
                    body.write(msg); // - detail as body
                }

                if (addAddress) {
                    body.write(bits[4]); // p, address

                    HttpChannelConfig cfg = finalSc.getHttpConfig();
                    // Only fill in the name of this server if configured to do so (true by default)
                    byte[] name = cfg.getServerHeaderValue();
                    if (!cfg.removeServerHeader() && name != null) { //PM87031 , servername is null by default
                        body.write(name);
                        body.write(at);
                    }
                    // show the host & port that were requested (potentially based on Host header)
                    // if the resource is not found, given that some translation may happen based on
                    // interjection of proxy headers, there has to be some way of showing what
                    // ended up being requested.
                    // Scrub the host header before returning it in the error response
                    msg = encodeDataString(getRequestedHost()).getBytes();
                    body.write(msg);
                    body.write(port);
                    body.write(Integer.toString(getRequestedPort()).getBytes());
                    body.write(bits[5]); // address, body, html
                } else {
                    body.write(bits[6]);
                }
            } catch (IOException e) {
            }
        }

        finish(failure);
    }

    /**
     * Set the HTTP response properties.
     *
     * @param rMsg The HttpResponseMessage to set.
     * @param code The StatusCode to return.
     */
    void setResponseProperties(HttpResponseMessage rMsg, StatusCodes code) {
        rMsg.setStatusCode(code);
        rMsg.setConnection(ConnectionValues.CLOSE);
        rMsg.setCharset(Charset.forName("UTF-8"));
        rMsg.setHeader("Content-Type", "text/html; charset=UTF-8");
    }

    /**
     * Get the requested host based on the Host and/or private headers.
     * <p>
     * per Servlet spec, this is similar to getServerName:
     * Returns the host name of the server to which the request was sent.
     * It is the value of the part before ":" in the Host header value, if any,
     * or the resolved server name, or the server IP address.
     *
     * @param request        the inbound request
     * @param remoteHostAddr the requesting client IP address
     */
    @Override
    public String getRequestedHost() {
        // Get the requested host: this takes into consideration whether or not we should trust the
        // contents of Host and $WS* headers..
        if (useTrustedHeaders()) {
            // If the plugin provided a header, prefer that..
            String pluginHost = request.getHeader(HttpHeaderKeys.HDR_$WSSN.getName());
            if (pluginHost != null)
                return pluginHost;
        }

        // find the HostName according to HTTP 1.1 spec
        String host = request.getVirtualHost();

        if (host == null) // unlikely.
            return "localhost"; // avoid leaking information

        return host;
    }

    /**
     * Get the requested port based on the Host and/or private headers.
     *
     * per Servlet spec, this is similar to getServerPort:
     * Returns the port number to which the request was sent. It is the value of
     * the part after ":" in the Host header value, if any, or the server port
     * where the client connection was accepted on.
     *
     * @param request        the inbound request
     * @param localPort      the server port where the client connection was accepted on.
     * @param remoteHostAddr the requesting client IP address
     */
    @Override
    public int getRequestedPort() {

        // Get the requested port: this takes into consideration whether or not we should trust the
        // contents of Host and $WS* headers..
        if (useTrustedHeaders()) {
            String pluginPort = request.getHeader(HttpHeaderKeys.HDR_$WSSP.getName());
            if (pluginPort != null)
                return Integer.parseInt(pluginPort);
        }

        int port = request.getVirtualPort();

        if (port > 0) {
            return port;
        } else {
            //No port found from URL or host header, infer it..

            //Scheme as defined by X-Forwarded-Proto or Forwarded "proto", or null
            //if remoteIp is not enabled or client address not verified as trusted.
            String scheme = getRemoteProto();

            if (scheme == null && isc != null && !isc.useForwardedHeaders()) {
                //if remoteIp is not enabled, still verify for the x-forwarded-proto
                scheme = request.getHeader(HttpHeaderKeys.HDR_X_FORWARDED_PROTO.getName());
            }

            if (scheme == null && request.getHeader(HttpHeaderKeys.HDR_HOST.getName()) != null) {
                scheme = request.getScheme();

            }

            if ("http".equals(scheme)) {
                return 80;
            } else if ("https".equals(scheme)) {
                return 443;
            }
        }
        return getLocalPort();
    }

    @Override
    public boolean useTrustedHeaders() {
        UsePrivateHeaders useHeaders = usePrivateHeaders;
        // We want to avoid re-processing whether or not to trust private headers
        // from the other end of this connection (i.e. the proxy itself).
        // We can avoid reprocessing as long as the HttpDispatcher (or WebContainer) configuration
        // hasn't been updated, in which case, we should try again.
        int lastUpdate = HttpDispatcher.getConfigUpdate();
        if (useHeaders == UsePrivateHeaders.unknown || configUpdate != lastUpdate) {
            useHeaders = usePrivateHeaders = UsePrivateHeaders.set(HttpDispatcher.usePrivateHeaders(contextRemoteHostAddress()));
            configUpdate = lastUpdate;
        }
        return useHeaders.asBoolean();
    }

    @Override
    public String getTrustedHeader(String headerName) {
        if (useTrustedHeaders() && request != null) {
            return request.getHeader(headerName);
        }
        return null;
    }

    @Override
    public String getLocalHostAddress() {
        return this.isc.getLocalAddr().getHostAddress();
    }

    @Override
    public String getLocalHostAlias() {
        String alias = localHostAlias;
        if (alias == null) {
            alias = localHostAlias = getRequestedHost() + ":" + getRequestedPort();
        }
        return alias;
    }

    @Override
    public String getLocalHostName(final boolean canonical) {
        String hostName = null;
        if (canonical) {
            hostName = localCanonicalHostName;
            if (hostName == null) {
                localCanonicalHostName = hostName = internalGetHostName(true);
            }
        } else {
            hostName = internalGetHostName(false);
        }

        return hostName;
    }

    private String internalGetHostName(final boolean canonical) {
        final HttpInboundServiceContextImpl finalSc = this.isc;
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                if (canonical) {
                    return finalSc.getLocalAddr().getCanonicalHostName();
                } else {
                    return finalSc.getLocalAddr().getHostName();
                }
            }
        });
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#getLocalPort()
     */
    @Override
    public int getLocalPort() {
        return this.isc.getLocalPort();
    }

    /**
     * Return the remote address, either from a trusted header,
     * or based on the inbound connection.
     *
     * @see com.ibm.websphere.http.HttpInboundConnection#getRemoteAddress()
     */
    @Override
    public String getRemoteHostAddress() {

        String remoteAddr = null;

        if (isc != null && isc.useForwardedHeaders()) {
            remoteAddr = isc.getForwardedRemoteAddress();

        }
        if (remoteAddr == null) {
            remoteAddr = getTrustedHeader(HttpHeaderKeys.HDR_$WSRA.getName());
            if (remoteAddr != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getRemoteHostAddress isTrusted --> true, addr --> " + remoteAddr);
            } else {
                remoteAddr = contextRemoteHostAddress();
            }
        }

        return remoteAddr;
    }

    /**
     * @return the remote host address of the inbound connection
     */
    private String contextRemoteHostAddress() {
        String remoteAddr = remoteContextAddress;
        if (remoteAddr == null) {
            final HttpInboundServiceContextImpl finalSc = this.isc;
            if (finalSc != null) {
                remoteAddr = remoteContextAddress = finalSc.getRemoteAddr().getHostAddress();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getRemoteAddr addr --> " + remoteAddr);
            }
        }
        return remoteAddr;
    }

    /**
     * Return the remote host name, either from a trusted header,
     * or based on the inbound connection.
     *
     * @see com.ibm.websphere.http.HttpInboundConnection#getRemoteHostName()
     */
    @Override
    public String getRemoteHostName(final boolean canonical) {
        String remoteHost = null;
        final HttpInboundServiceContextImpl finalSc = this.isc;

        if (finalSc != null && finalSc.useForwardedHeaders()) {
            remoteHost = finalSc.getForwardedRemoteHost();
        }
        if (remoteHost == null) {

            remoteHost = getTrustedHeader(HttpHeaderKeys.HDR_$WSRH.getName());
            if (remoteHost != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getRemoteHost isTrusted --> true, host --> " + remoteHost);
            } else {

                remoteHost = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        if (canonical)
                            return finalSc.getRemoteAddr().getCanonicalHostName();
                        else
                            return finalSc.getRemoteAddr().getHostName();
                    }
                });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getRemoteHost host --> " + remoteHost);
            }
        }
        return remoteHost;
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#getRemotePort()
     */
    @Override
    public int getRemotePort() {

        if (isc != null && isc.useForwardedHeaders()) {
            if (isc.getForwardedRemotePort() != -1)
                return isc.getForwardedRemotePort();
        }

        return this.isc.getRemotePort();
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#getRequest()
     */
    @Override
    public HttpRequest getRequest() {
        return this.request;
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#getResponse()
     */
    @Override
    public HttpResponse getResponse() {
        return this.response;
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#getSSLContext()
     */
    @Override
    public SSLContext getSSLContext() {
        if (this.sslinfo == null &&
            this.isc != null &&
            this.isc.getSSLContext() != null) {
            this.sslinfo = new SSLContextImpl(this.isc.getSSLContext());
        }
        return this.sslinfo;
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#finish()
     */
    @Override
    public void finish(Exception e) {

        final HttpInboundServiceContextImpl finalSc = this.isc;
        Exception error = e;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Finishing conn; " + finalSc + " error=" + e);
        }

        if (vc != null) { // This is added for Upgrade Servlet3.1 WebConnection
            String webconn = (String) (this.vc.getStateMap().get(TransportConstants.CLOSE_NON_UPGRADED_STREAMS));
            if (webconn != null && webconn.equalsIgnoreCase("CLOSED_NON_UPGRADED_STREAMS")) {
                vc.getStateMap().put(TransportConstants.CLOSE_NON_UPGRADED_STREAMS, "null");
            } else {
                synchronized (WebConnCanCloseSync) {
                    if (WebConnCanClose) {
                        error = closeStreams();
                    }
                }
            }
        } else {
            synchronized (WebConnCanCloseSync) {
                if (WebConnCanClose) {
                    error = closeStreams();
                }
            }
        }

        close(getVirtualConnection(), error);
    }

    /**
     * Searches the passed in String for any characters that could be
     * used in a cross site scripting attack (<, >, +, &, ", ', (, ), %, ;)
     * and converts them to their browser equivalent name or code specification.
     *
     * This method should stay in sync with webcontainer ResponseUtils.encodeDataString()
     *
     * @param iString contains the String to be encoded
     *
     * @return an encoded String
     */
    private static String encodeDataString(String iString) {
        if (iString == null)
            return "";

        int strLen = iString.length(), i;

        if (strLen < 1)
            return iString;

        // convert any special chars to their browser equivalent specification
        StringBuffer retString = new StringBuffer(strLen * 2);

        for (i = 0; i < strLen; i++) {
            switch (iString.charAt(i)) {
                case '<':
                    retString.append("&lt;");
                    break;

                case '>':
                    retString.append("&gt;");
                    break;

                case '&':
                    retString.append("&amp;");
                    break;

                case '\"':
                    retString.append("&quot;");
                    break;

                case '+':
                    retString.append("&#43;");
                    break;

                case '(':
                    retString.append("&#40;");
                    break;

                case ')':
                    retString.append("&#41;");
                    break;

                case '\'':
                    retString.append("&#39;");
                    break;

                case '%':
                    retString.append("&#37;");
                    break;

                case ';':
                    retString.append("&#59;");
                    break;

                default:
                    retString.append(iString.charAt(i));
                    break;
            }
        }

        return retString.toString();
    }

    private Exception closeStreams() { // This is seperated for Upgrade Servlet3.1 WebConnection
        final HttpRequestImpl finalRequest = this.request;
        final HttpResponseImpl finalResponse = this.response;

        Exception error = null;

        if (finalRequest != null) {
            error = tryToCloseStream(finalRequest.getBody());
        }

        if (finalResponse != null) {
            Exception ex = tryToCloseStream(finalResponse.getBody());
            if (null == error) {
                error = ex;
            }
        }
        return error;
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#getDateFormatter()
     */
    @Override
    public HttpDateFormat getDateFormatter() {
        return HttpDispatcher.getDateFormatter();
    }

    /*
     * @see com.ibm.websphere.http.HttpInboundConnection#getEncodingUtils()
     */
    @Override
    public EncodingUtils getEncodingUtils() {
        return HttpDispatcher.getEncodingUtils();
    }

    /**
     * Wrapper for the runnable returned by discriminate - to handle exceptions from badly-behaved containers
     */
    static class TaskWrapper implements Runnable {
        private final Runnable runnable;
        private final HttpDispatcherLink ic;
        private Executor classifiedExecutor;

        public TaskWrapper(Runnable run, HttpDispatcherLink inboundConnection) {
            this.runnable = run;
            this.ic = inboundConnection;
            this.classifiedExecutor = null;
        }

        public void setClassifiedExecutor(Executor classifiedExecutor) {
            this.classifiedExecutor = classifiedExecutor;
        }

        @Override
        @FFDCIgnore(Throwable.class)
        public void run() {
            try {
                if (this.classifiedExecutor != null) {
                    DecoratedExecutorThread.setExecutor(this.classifiedExecutor);
                }
                runnable.run();
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Unhandled exception during dispatch (bad container); " + t);
                    Tr.event(tc, "stack trace: \n" + t.getStackTrace().toString());
                }
                // if the link is ready and not destroyed, try sending a response
                if (ic.linkIsReady) {
                    if (t instanceof Exception) {
                        ic.sendResponse(StatusCodes.INTERNAL_ERROR, (Exception) t, true);
                    } else {
                        ic.sendResponse(StatusCodes.INTERNAL_ERROR, new Exception("Dispatch error", t), true);
                    }
                }
            } finally {
                if (this.classifiedExecutor != null) {
                    DecoratedExecutorThread.removeExecutor();
                }
            }
        }
    }

    @FFDCIgnore(IOException.class)
    @Trivial
    private boolean exists(InputStream inputStream) {
        try {
            return inputStream.available() > 0;
        } catch (IOException e) {
        }
        return false;
    }

    @FFDCIgnore(IOException.class)
    @Trivial
    private Exception tryToCloseStream(Closeable closeStream) {
        if (closeStream != null) {
            try {
                closeStream.close();
            } catch (IOException ioe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error closing stream; " + ioe);
                }
                return ioe;
            }
        }
        return null;
    }

    /**
     * Determine if a request is an http2 upgrade request
     */
    @Override
    public boolean isHTTP2UpgradeRequest(Map<String, String> headers, boolean checkEnabledOnly) {
        if (isc != null) {
            //Returns whether HTTP/2 is enabled for this channel/port
            if (checkEnabledOnly) {
                return isc.isHttp2Enabled();
            }
            //Check headers for HTTP/2 upgrade header
            else {
                HttpInboundLink link = isc.getLink();
                if (link != null) {

                    return link.isHTTP2UpgradeRequest(headers);
                }
            }
        }
        return false;
    }

    /**
     * Determine if a map of headers contains an http2 upgrade header.
     * If it does, upgrade that request and begin processing the header in the http2 engine
     *
     * @return false if some error occurred while servicing the upgrade request
     */
    @Override
    public boolean handleHTTP2UpgradeRequest(Map<String, String> headers) {
        HttpInboundLink link = isc.getLink();
        HttpInboundChannel channel = link.getChannel();
        VirtualConnection vc = link.getVirtualConnection();
        H2InboundLink h2Link = new H2InboundLink(channel, vc, getTCPConnectionContext());

        boolean upgraded = h2Link.handleHTTP2UpgradeRequest(headers, link);
        if (upgraded) {
            h2Link.startAsyncRead(true);
        } else {
            return false;
        }

        // wait for protocol init on stream 1, where the initial upgrade request is serviced
        boolean rc = h2Link.getStream(1).waitForConnectionInit();

        if (!rc) {
            // A problem occurred with the connection start up, a trace message will be issued from waitForConnectionInit()
            vc.getStateMap().put(h2InitError, true);
        }

        return rc;
    }

    public HttpInboundLink getHttpInboundLink2() {
        if (isc != null) {
            return isc.getLink();
        }
        return null;
    }

    /**
     * @return
     */
    @Override
    public String getRemoteProto() {

        if (isc != null && isc.useForwardedHeaders()) {
            return isc.getForwardedRemoteProto();
        }
        return null;

    }

    @Override
    public boolean useForwardedHeaders() {

        if (isc != null) {
            return isc.useForwardedHeaders();
        }
        return false;
    }

}
