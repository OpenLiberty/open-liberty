/*******************************************************************************
 * Copyright (c) 2004, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.inbound;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.h2internal.H2HttpInboundLinkWrap;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundProtocolLink;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.genericbnf.exception.UnsupportedMethodException;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.exception.MessageTooLargeException;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.logging.DebugLog;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * Connection channel link implementation for the inbound Http channel. This
 * will need to handle notifications of incoming requests and start the
 * processing of those requests.
 */
public class HttpInboundLink extends InboundProtocolLink implements InterChannelCallback, ConnectionLink {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpInboundLink.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** HTTP service context for this connection */
    protected HttpInboundServiceContextImpl myInterface = null;
    /** Channel owning this object */
    protected HttpInboundChannel myChannel = null;
    /** the main TCP service context reference */
    protected TCPConnectionContext myTSC = null;
    /** flag on whether this is a partially parsed request or not */
    protected boolean bPartialParsedRequest = false;
    /** Number of requests processed by this connection. */
    protected int numRequestsProcessed = 0;
    /** Flag on whether we should filter out exceptions during closes or not */
    protected boolean filterExceptions = false;
    /** Flag on whether this link object is active or not */
    protected boolean bIsActive = false;
    /** List of all unique app side callbacks used for this connection */
    protected List<ConnectionReadyCallback> appSides = null;
    /** Flag on whether this link has been marked for HTTP/2 */
    private boolean alreadyH2Upgraded = false;
    /** Flag on whether grpc is being used for this link */
    private boolean isGrpc = false;

    /**
     * Constructor for an HTTP inbound link object.
     *
     * @param channel
     * @param vc
     */
    public HttpInboundLink(HttpInboundChannel channel, VirtualConnection vc) {
        init(vc, channel);
        // allocate an empty ISC object
        this.myInterface = new HttpInboundServiceContextImpl(null, this, getVirtualConnection(), getChannel().getHttpConfig());
    }

    /**
     * Initialize this object.
     *
     * @param inVC
     * @param channel
     */
    public void init(VirtualConnection inVC, HttpInboundChannel channel) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Init on link: " + this + " " + inVC);
        }
        super.init(inVC);
        this.myChannel = channel;
        if (null != getHTTPContext()) {
            getHTTPContext().setHttpConfig(channel.getHttpConfig());
        }

        getVirtualConnection().getStateMap().put(CallbackIDs.CALLBACK_HTTPICL, this);
        this.bIsActive = true;
        this.isGrpc = false;
    }

    VirtualConnection switchedVC = null;

    @Override
    public VirtualConnection getVirtualConnection() {
        if (switchedVC != null) {
            return switchedVC;
        }
        return super.getVirtualConnection();
    }

    public void reinit(TCPConnectionContext tcc, VirtualConnection vc, HttpInboundLink wrapper) {
        myTSC = tcc;
        switchedVC = vc;
        getVirtualConnection().getStateMap().put(CallbackIDs.CALLBACK_HTTPICL, this);
        this.myInterface.reinit(tcc, vc, wrapper);

    }

    /*
     * @see com.ibm.wsspi.channelfw.base.InboundProtocolLink#destroy(java.lang.Exception)
     */
    @Override
    public void destroy(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroying inbound link: " + this + " " + getVirtualConnection());
        }
        // if this object is not active, then just return out
        synchronized (this) {
            if (!this.bIsActive) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ignoring destroy on an inactive object");
                }
                return;
            }
            this.bIsActive = false;
        }
        // 291714 - clean up the statemap
        getVirtualConnection().getStateMap().remove(CallbackIDs.CALLBACK_HTTPICL);
        if (getChannel().getHttpConfig().runningOnZOS()) {
            // 363633 - remove the buffer size value if present
            getVirtualConnection().getStateMap().remove(HttpConstants.HTTPReadBufferSize);
        }
        // now clean out any other app connlinks we may have picked up
        if (null != this.appSides) {
            // the super.destroy without an exception just nulls out values
            // the list of appside connlinks includes the current one
            super.destroy();
            for (ConnectionReadyCallback appside : this.appSides) {
                appside.destroy(e);
            }
            this.appSides = null;
        } else {
            // if we only ever got one connlink above, then call the standard
            // destroy to pass the sequence along
            super.destroy(e);
        }
        this.myInterface.clear();
        this.myInterface.destroy();
        // these are no longer pooled, dereference now
        this.myInterface = null;
        this.myTSC = null;
        this.filterExceptions = false;
        this.numRequestsProcessed = 0;
        this.myChannel = null;
    }

    /**
     * Query the interface value.
     *
     * @return Object (HttpInboundServiceContextImpl)
     */
    @Override
    public Object getChannelAccessor() {
        return this.myInterface;
    }

    /**
     * Query the HTTP service context for this connection.
     *
     * @return HttpInboundServiceContextImpl
     */
    public HttpInboundServiceContextImpl getHTTPContext() {
        return this.myInterface;
    }

    /**
     * Query the channel that owns this object.
     *
     * @return HttpInboundChannel
     */
    public HttpInboundChannel getChannel() {
        return this.myChannel;
    }

    /**
     * Find out whether we've served the maximum number of requests allowed
     * on this connection already.
     *
     * @return boolean
     */
    protected boolean maxRequestsServed() {
        // PK12235, check for a partial or full stop
        if (getChannel().isStopping()) {
            // channel has stopped, no more keep-alives
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel stopped, disabling keep-alive request");
            }
            return true;
        }
        if (!getChannel().getHttpConfig().isKeepAliveEnabled()) {
            // keep alives are disabled, no need to check the max request number
            return true;
        }
        int max = getChannel().getHttpConfig().getMaximumPersistentRequests();
        // -1 is unlimited, 0..1 is 1 request, any above that is that exact
        // number of requests
        if (0 <= max) {
            return (this.numRequestsProcessed >= max);
        }
        return false;
    }

    /**
     * Query whether this is the first request on the socket or not.
     *
     * @return boolean
     */
    protected boolean isFirstRequest() {
        // number increments to 1 after we fully parse the request, so need to
        // watch for both values, but only if headers are fully parsed
        return ((0 == this.numRequestsProcessed) || (1 == this.numRequestsProcessed && getHTTPContext().headersParsed()));
    }

    /**
     * Query whether the request is partially parsed or not.
     *
     * @return boolean (true means still parsing the request)
     */
    protected boolean isPartiallyParsed() {
        return this.bPartialParsedRequest;
    }

    /**
     * Allow the flag to be set as to whether or not the request is
     * fully parsed.
     *
     * @param b
     */
    protected void setPartiallyParsed(boolean b) {
        this.bPartialParsedRequest = b;
    }

    /**
     * Query if this link should use HTTP/2
     *
     * @param vc
     * @return
     */
    public boolean isDirectHttp2Link(VirtualConnection vc) {
        if (alreadyH2Upgraded) {
            return true;
        }
        HttpInboundServiceContextImpl sc = getHTTPContext();
        if (!sc.isH2Connection()) {
            // if ALPN has selected h2, OR if this link is not secure, check for the HTTP/2 connection preface
            if ((checkAlpnH2() || (!sc.isSecure() && sc.isHttp2Enabled())) && checkForH2MagicString(sc)) {
                alreadyH2Upgraded = true;
                return true;
            }
        }
        return false;
    }

    public final void setIsGrpcInParentLink(boolean x) {
        isGrpc = x;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "set isGprc: " + isGrpc);
        }
    }

    /**
     * @return true if SSL is in use and "h2" was chosen via ALPN
     */
    private boolean checkAlpnH2() {
        return this.myTSC.getSSLContext() != null
               && this.myTSC.getSSLContext().getAlpnProtocol() != null
               && this.myTSC.getSSLContext().getAlpnProtocol().equals("h2");
    }

    /**
     * Called by the device side channel when a new request is ready for work.
     *
     * @param inVC
     */
    @Override
    public void ready(VirtualConnection inVC) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "ready: " + this + " " + inVC);
        }
        this.myTSC = (TCPConnectionContext) getDeviceLink().getChannelAccessor();
        HttpInboundServiceContextImpl sc = getHTTPContext();

        sc.init(this.myTSC, this, inVC, getChannel().getHttpConfig());
        if (getChannel().getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.INFO)) {
            getChannel().getHttpConfig().getDebugLog().log(DebugLog.Level.INFO, HttpMessages.MSG_CONN_STARTING, sc);
        }
        processRequest();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "ready");
        }
    }

    /**
     * Process new information for an inbound request that needs to be parsed
     * and handled by channels above.
     */
    protected void processRequest() {
        final int timeout = getHTTPContext().getReadTimeout();
        final TCPReadCompletedCallback callback = HttpICLReadCallback.getRef();

        // keep looping on processing information until we fully parse the message
        // and hand it off, or until the reads for more data go async
        VirtualConnection rc = null;
        do {
            if (handleNewInformation()) {
                // new information triggered an error message, so we're done
                return;
            }
            // Note: handleNewInformation will allocate the read buffers
            if (!isPartiallyParsed()) {
                // we're done parsing at this point. Note: cannot take any action
                // with information after this call because it may go all the way
                // from the channel above us back to the persist read, must exit
                // this callstack immediately
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRequest calling handleNewRequest()");
                }

                handleNewRequest();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRequest return from handleNewRequest()");
                }

                return;
            }
            rc = this.myTSC.getReadInterface().read(1, callback, false, timeout);
        } while (null != rc);
    }

    /**
     * Handle parsing the incoming request message.
     *
     * @return whether an error happend and this connection is already done
     */
    private boolean handleNewInformation() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing new information: " + getVirtualConnection());
        }

        final HttpInboundServiceContextImpl sc = getHTTPContext();

        if (!isPartiallyParsed()) {
            // this is the first pass through on parsing this new request
            // PK12235, check for a full stop only
            if (getChannel().isStopped()) {
                // channel stopped during the initial read, send error back
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Channel stopped during initial read");
                }
                sc.setHeadersParsed();
                // since we haven't parsed the request version, force the
                // response to the minimal 1.0 version
                sc.getResponse().setVersion(VersionValues.V10);
                sendErrorMessage(StatusCodes.UNAVAILABLE);
                return true;
            }
        }

        boolean completed = false;

        // if this is an http/2 link, don't perform additional parsing
        if (this.isDirectHttp2Link(switchedVC)) {
            return false;
        }
        try {
            completed = sc.parseMessage();
        } catch (UnsupportedMethodException meth) {
            // no FFDC required
            sc.setHeadersParsed();
            sendErrorMessage(StatusCodes.NOT_IMPLEMENTED);
            setPartiallyParsed(false);
            return true;
        } catch (UnsupportedProtocolVersionException ver) {
            // no FFDC required
            sc.setHeadersParsed();
            sendErrorMessage(StatusCodes.UNSUPPORTED_VERSION);
            setPartiallyParsed(false);
            return true;
        } catch (MessageTooLargeException mtle) {
            // no FFDC required
            sc.setHeadersParsed();
            sendErrorMessage(StatusCodes.ENTITY_TOO_LARGE);
            setPartiallyParsed(false);
            return true;
        } catch (MalformedMessageException mme) {
            //no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "parseMessage encountered a MalformedMessageException : " + mme);
            }
            handleGenericHNIError(mme, sc);
            return true;
        } catch (IllegalArgumentException iae) {
            //no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "parseMessage encountered an IllegalArgumentException : " + iae);
            }
            handleGenericHNIError(iae, sc);
            return true;
        } catch (Http2Exception h2e) {
            //no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "parseMessage encountered an Http2Exception : " + h2e);
            }
            this.myInterface.getLink().close(getVirtualConnection(), h2e);
            return true;
        } catch (Throwable t) {
            FFDCFilter.processException(t,
                                        "HttpInboundLink.handleNewInformation",
                                        "2", this);
            handleGenericHNIError(t, sc);
            return true;
        }

        // partialParsed is the opposite of the complete flag
        setPartiallyParsed(!completed);
        if (isPartiallyParsed()) {
            sc.setupReadBuffers(sc.getHttpConfig().getIncomingHdrBufferSize(), false);
        }
        return false;
    }

    //A method which is exclusively called from handleNewInformation. There was three catches which do
    //the same thing so now they will just call this one method
    private void handleGenericHNIError(Throwable t, HttpInboundServiceContextImpl hisc) {
        hisc.setHeadersParsed();
        sendErrorMessage(t);
        setPartiallyParsed(false);
    }

    /**
     * Process a new request message, updating internal stats and calling the
     * discrimination to pass it along the channel chain.
     */
    private void handleNewRequest() {

        // if this is an http/2 request, skip to discrimination
        if (!isDirectHttp2Link(this.vc)) {
            final HttpInboundServiceContextImpl sc = getHTTPContext();
            // save the request info that was parsed in case somebody changes it
            sc.setRequestVersion(sc.getRequest().getVersionValue());
            sc.setRequestMethod(sc.getRequest().getMethodValue());

            // get the response message initialized. Note: in the proxy env, this
            // response message will be overwritten; however, this is the only
            // spot to init() it correctly for all other cases.
            sc.getResponseImpl().init(sc);

            this.numRequestsProcessed++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received request number " + this.numRequestsProcessed + " on link " + this);
            }

            // check for the 100-continue scenario
            if (!sc.check100Continue()) {
                return;
            }
        }
        handleDiscrimination();
    }

    /**
     * Handle the discrimination path on a new connection from the ready
     * method.
     */
    protected void handleDiscrimination() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Discrimination will be called");
        }

        // 363633 - on z/OS we need store store some information for Proxy use
        if (getChannel().getHttpConfig().runningOnZOS()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Storing buffer size on z/OS");
            }
            getVirtualConnection().getStateMap().put(HttpConstants.HTTPReadBufferSize, Integer.valueOf(getChannel().getHttpConfig().getIncomingBodyBufferSize()));
        }

        // we only allow a SUCCESS or FAILURE during discrimination
        // as no application channel above us can ask for more data
        // beyond the HTTP request.

        ConnectionReadyCallback oldAppSide = getApplicationCallback();

        DiscriminationProcess dp = getChannel().getDiscriminationProcess();
        int state = DiscriminationProcess.FAILURE;
        try {
            state = dp.discriminate(getVirtualConnection(), getHTTPContext().getRequest(), this);
        } catch (Exception e) {
            // No need to do FFDC here as the discriminate method of the
            // DiscriminationProcess implementation handles FFDC already
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception caught during discriminate: " + e);
            }
            // close the connection here with the exception from the DP
            setPartiallyParsed(false);
            sendErrorMessage(e);
            return;
        }

        // now either pass it to the application channel or close
        // the connection

        if (DiscriminationProcess.SUCCESS == state) {
            // if this is a new app channel, save it to the list
            if (null != oldAppSide && !getApplicationCallback().equals(oldAppSide)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received new appside connlink: " + oldAppSide + " vs " + getApplicationCallback());
                }
                // only keep unique connlinks so check to see if it exists
                if (null == this.appSides) {
                    // add old and new
                    this.appSides = new LinkedList<ConnectionReadyCallback>();
                    this.appSides.add(oldAppSide);
                    this.appSides.add(getApplicationCallback());
                } else {
                    // see if we need to add the new one
                    if (!this.appSides.contains(getApplicationCallback())) {
                        this.appSides.add(getApplicationCallback());
                    }
                }
            }
            try {
                getApplicationCallback().ready(getVirtualConnection());
            } catch (Throwable t) {
                FFDCFilter.processException(t, "HttpInboundLink.handleDiscrimination", "1", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "App side ready() threw exception: " + t);
                }
                if (!this.bIsActive) {
                    // close/destroy already went through on this connection
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Link not active, returning out.");
                    }
                    return;
                }
                if (getHTTPContext().headersSent()) {
                    // partial message already sent, just close
                    getDeviceLink().close(getVirtualConnection(), new Exception(t));
                    return;
                }
                // otherwise send an error back and close
                sendErrorMessage(StatusCodes.UNAVAILABLE);
                return;
            }
        } else {
            // close the connection because nobody wants it
            setPartiallyParsed(false); // keep ready() from continuing
            sendErrorMessage(new Exception("Discrimination failed"));
        }
    }

    /**
     * Send an error message when a generic throwable occurs.
     *
     * @param t
     */
    private void sendErrorMessage(Throwable t) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sending a 400 for throwable [" + t + "]");
        }
        sendErrorMessage(StatusCodes.BAD_REQUEST);
    }

    /**
     * Send an error message back to the client with a defined
     * status code, instead of an exception.
     *
     * @param code
     */
    private void sendErrorMessage(StatusCodes code) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sending an error page back [code: " + code + "]");
        }
        try {
            getHTTPContext().sendError(code.getHttpError());
        } catch (MessageSentException mse) {
            // no FFDC required
            close(getVirtualConnection(), new Exception("HTTP Message failure"));
        }
    }

    /**
     * Handle a pipelined request discovered while closing the handling of the
     * last request.
     */
    private void handlePipeLining() {
        HttpServiceContextImpl sc = getHTTPContext();
        WsByteBuffer buffer = sc.returnLastBuffer();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Pipelined request found: " + buffer);
        }
        sc.clear();
        // save it back so that we always release it
        sc.storeAllocatedBuffer(buffer);
        sc.disableBufferModification();
        EventEngine events = HttpDispatcher.getEventService();
        if (null != events) {
            Event event = events.createEvent(HttpPipelineEventHandler.TOPIC_PIPELINING);
            event.setProperty(CallbackIDs.CALLBACK_HTTPICL.getName(), this);
            events.postEvent(event);
        } else {
            // unable to dispatch work request, continue on this thread
            ready(getVirtualConnection());
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.InboundProtocolLink#close(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Exception)
     */
    @Override
    public void close(VirtualConnection inVC, Exception e) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "close() called: " + this + " " + inVC);
        }

        boolean errorState = (null != e);
        if (errorState && bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "close() in error state, " + e);
        }

        final HttpInboundServiceContextImpl sc = getHTTPContext();
        final HttpChannelConfig config = sc.getHttpConfig();
        // valid responses as well as error responses all go through here so
        // this is where we tell the factory that any large response is complete
        if (sc.containsLargeMessage()) {
            getChannel().getFactory().releaseLargeMessage();
        }

        // check to see if the message has been fully sent (unless we
        // are in an error state)

        if (!errorState && !sc.isMessageSent()) {
            try {
                VirtualConnection rc = sc.finishResponseMessage(null, this, false);
                // if the finish is done already, then continue with code below
                if (null == rc) {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "finishing response msg async");
                    }
                    return;
                }
            } catch (MessageSentException mse) {
                // no FFDC required, nothing to do here
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error: message sent exception when flag doesn't agree");
                }
                // shouldn't get here so just close the socket
                getDeviceLink().close(inVC, mse);
                return;
            }
        }

        // if the incoming request body was never read, we must purge the body
        // off the socket before we can continue with the read for the next
        // request... assuming we're not in an error condition

        if (!errorState
            // 346196.2 -- if we stopped, then don't purge the body
            && getChannel().isRunning() && !sc.isIncomingMessageFullyRead()) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Incoming request body never read.");
            }
            try {
                // 346196.4 - read/release one buffer at a time to avoid
                // unnecessary memory storage for large bodies
                VirtualConnection rc = sc.getRequestBodyBuffer(HttpIgnoreBodyCallback.getRef(), false);
                boolean reachedEnd = false;
                while (null != rc) {
                    WsByteBuffer buffer = sc.getRequestBodyBuffer();
                    if (null != buffer) {
                        buffer.release();
                        rc = sc.getRequestBodyBuffer(HttpIgnoreBodyCallback.getRef(), false);
                    } else {
                        // end of body found
                        reachedEnd = true;
                        break;
                    }
                }
                if (!reachedEnd) {
                    // wait for callback
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "close() waiting for body purge callback");
                    }
                    return;
                }
            } catch (Exception purgeException) {
                // no FFDC required
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception purging request body: " + purgeException);
                }
                errorState = true;
            }
        }

        if (this.myInterface.getLink() instanceof H2HttpInboundLinkWrap) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "we're H2, calling that close");
            }
            this.myInterface.getLink().close(inVC, e);

            return;
        }

        // If servlet upgrade processing is being used, then don't close the socket here
        if (inVC != null) {
            String upgraded = (String) (inVC.getStateMap().get(TransportConstants.UPGRADED_CONNECTION));
            if (upgraded != null) {
                if (!errorState && getChannel().isRunning() && (upgraded.compareToIgnoreCase("true") == 0)) {

                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Connection Not closed because Servlet Upgrade detected.");
                    }
                    return;
                }
            }
        }

        // now check to see if the connection should be kept open (unless
        // we are in an error state). Perform one last check against a channel
        // stop, close the socket if not running.
        boolean ignoreWriteAfterCommit = (config.ignoreWriteAfterCommit() || !errorState);
        if (ignoreWriteAfterCommit && sc.isPersistent() && getChannel().isRunning()) {

            if (config.getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
                config.getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_READ_PERSISTENT, sc);
            }

            sc.resetStartTime();
            this.myTSC.getWriteInterface().setBuffers(null);

            // check the last buffer from the previous request as
            // there might be the start of another request on it (pipelining)

            if (sc.isReadDataAvailable()) {
                handlePipeLining();
                return;
            }
            // no pipelined data, clear the SC and setup for a JIT read
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading for another request...");
            }
            if (getChannel().getHttpConfig().runningOnZOS()) {
                // 342859 - add a marker indicating the first read of a subsequent
                // request is starting. This is used for WLM type management on z/OS
                // right now with request message priority handling and monitoring
                // of "in-system" times
                getVirtualConnection().getStateMap().put(HttpConstants.HTTPFirstRead, "true");
            }

            sc.clear();
            sc.setReadBuffer(null);
            sc.setupJITRead(config.getIncomingHdrBufferSize());

            // do a forced async read to free up the thread since there will
            // no doubt be a delay until the next request -- we've already
            // checked for pipelined ones
            this.myTSC.getReadInterface().read(1, HttpICLReadCallback.getRef(), true, config.getPersistTimeout());

        } else {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Closing connection");
            }

            //PI11176 Begin
            if (getChannel().getHttpConfig().shouldAttemptPurgeData()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "attemptPurgeData true, purging the data before closing the connection");
                }
                try {
                    //Setup for a JIT read. This will allocate a buffer for X bytes if a buffer hasn't already been allocated
                    //If there is nothing to read the buffer is automatically released
                    sc.setupJITRead(10);
                    //Set the buffer to null at the TCP layer before continuing. We will be doing this after anyway, so want to do it ahead of time so we are starting with out buffer.
                    this.myTSC.getReadInterface().setBuffer(null);
                    boolean processing = true;

                    while (processing) {
                        //Do the read
                        long numBytesRead = this.myTSC.getReadInterface().read(0, 1);
                        //Get the buffer associated with the read, could return null
                        WsByteBuffer indivBuffer = this.myTSC.getReadInterface().getBuffer();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Bytes read : " + numBytesRead);
                        }
                        //Check if the bytesRead are 0. If so then we are done. If not then we have to process the data(which in this case means throw it out
                        if (numBytesRead <= 0) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "No data read");
                            }
                            if (indivBuffer != null) {
                                //Means we read more than once to set the buffer to null and release it
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "No data read, releasing any buffer that was allocated");
                                }
                                this.myTSC.getReadInterface().setBuffer(null);
                                indivBuffer.release();
                            }
                            processing = false;
                        } else {
                            if (indivBuffer != null) {
                                //Clear the buffer because we read some data
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Found a buffer after the read : " + indivBuffer);
                                }
                                indivBuffer.clear();
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Cleared the buffer, going back to read again, " + indivBuffer);
                                }
                            } else {
                                //We are done since there was no buffer
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "No buffer found, assuming we are done");
                                }
                                processing = false;
                            }

                        }
                    }
                } catch (IOException purgeException) {
                    //Encountered an exception, if there is a buffer, just release and null out
                    WsByteBuffer indivBuffer = this.myTSC.getReadInterface().getBuffer();
                    if (indivBuffer != null) {
                        this.myTSC.getReadInterface().setBuffer(null);
                        indivBuffer.release();
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "IOException during purge : " + purgeException);
                    }
                }
            }
            //PI11176 End

            if (config.getDebugLog().isEnabled(DebugLog.Level.INFO)) {
                config.getDebugLog().log(DebugLog.Level.INFO, HttpMessages.MSG_CONN_CLOSING, sc);
            }

            if (this.myTSC != null) {
                TCPWriteRequestContext writeInterface = this.myTSC.getWriteInterface();
                if (writeInterface != null)
                    writeInterface.setBuffer(null);
                TCPReadRequestContext readInterface = this.myTSC.getReadInterface();
                if (readInterface != null)
                    readInterface.setBuffer(null);
            }
            if (this.filterExceptions) {
                getDeviceLink().close(inVC, null);
            } else {
                getDeviceLink().close(inVC, e);
            }
        }
    }

    /**
     * Called when this request is complete.
     *
     * @param inVC
     */
    @Override
    public void complete(VirtualConnection inVC) {
        close(inVC, null);
    }

    /**
     * Called when an error occurs on this connection.
     *
     * @param inVC
     * @param t
     */
    @Override
    public void error(VirtualConnection inVC, Throwable t) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called on " + this + " " + inVC);
        }
        try {
            close(inVC, (Exception) t);
        } catch (ClassCastException cce) {
            // no FFDC required
            close(inVC, new Exception("Problem when finishing response"));
        }
    }

    /**
     * Whether or not to filter the exceptions on a close.
     *
     * @param b
     */
    protected void setFilterCloseExceptions(boolean b) {
        this.filterExceptions = b;
    }

    /**
     * Query the Http object factory link.
     *
     * @return HttpObjectFactory
     */
    public HttpObjectFactory getObjectFactory() {
        return getChannel().getObjectFactory();
    }

    /**
     * Determine if a request contains http2 upgrade headers
     *
     * @param headers a String map of http header key and value pairs
     * @return true if the request contains an http2 upgrade header
     */
    public boolean isHTTP2UpgradeRequest(Map<String, String> headers) {
        return checkIfUpgradeHeaders(headers);
    };

    final static String CONSTANT_upgrade = new String("upgrade");
    final static String CONSTANT_connection = new String("connection");
    final static String CONSTANT_connection_value = new String("Upgrade, HTTP2-Settings");
    final static String CONSTANT_h2c = new String("h2c");

    /**
     * Determine if a map of headers contains http2 upgrade headers
     *
     * @param headers a String map of http header key and value pairs
     * @return true if an http2 upgrade header is found
     */
    private boolean checkIfUpgradeHeaders(Map<String, String> headers) {
        // looking for two headers.
        // connection header with a value of "upgrade"
        // upgrade header with a value of "h2c"

        if (headers == Collections.EMPTY_MAP) {
            return false; // no headers passed in
        }

        boolean connection_upgrade = false;
        boolean upgrade_h2c = false;
        String headerValue = null;
        Set<Entry<String, String>> headerEntrys = headers.entrySet();

        for (Entry<String, String> header : headerEntrys) {
            String name = header.getKey();
            //check if it's an HTTP2 non-secure upgrade connection.
            if (name.equalsIgnoreCase(CONSTANT_connection)) {
                headerValue = header.getValue();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "connection header found with value: " + headerValue);
                }
                if (headerValue != null && headerValue.equalsIgnoreCase(CONSTANT_connection_value)) {
                    if (connection_upgrade == true) {
                        // should not have two of these, log debug and return false
                        // TODO: determine if we should throw an exception here, or if this error will be dealt with in subsequent processing
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "malformed: second connection header found");
                        }
                        return false;
                    }
                    connection_upgrade = true;
                }
            }
            if (name.equalsIgnoreCase(CONSTANT_upgrade)) {
                headerValue = header.getValue();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "upgrade header found with value: " + headerValue);
                }
                if (headerValue != null && headerValue.equalsIgnoreCase(CONSTANT_h2c)) {
                    if (upgrade_h2c == true) {
                        // should not have two of these, log debug and return false
                        // TODO: determine if we should throw an exception here, or if this error will be dealt with in subsequent processing
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "malformed: second upgrade header found");
                        }
                        return false;
                    }
                    upgrade_h2c = true;
                }
            }
        }

        if (connection_upgrade && upgrade_h2c) {
            return true;
        }
        return false;
    }

    /**
     * Check the beginning of the current read buffer for the HTTP/2 preface string
     *
     * @param isc the HttpInboundServiceContextImpl to use
     * @return true if the magic string was found
     */
    private boolean checkForH2MagicString(HttpInboundServiceContextImpl isc) {

        boolean hasMagicString = false;
        WsByteBuffer buffer;

        if (myTSC == null || myTSC.getReadInterface() == null ||
            (buffer = myTSC.getReadInterface().getBuffer()) == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "checkForH2MagicString: returning " + hasMagicString + " due to null read buffer");
            }
            return hasMagicString;
        }

        buffer = buffer.duplicate();
        buffer.flip();

        if (buffer.remaining() >= 24) {
            byte[] arr = new byte[24];
            buffer.get(arr);
            hasMagicString = Arrays.equals(arr, HttpConstants.HTTP2PrefaceBytes);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "checkForH2MagicString: returning " + hasMagicString);
        }
        buffer.release();
        return hasMagicString;
    }
}
