/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ReadOnlyBufferException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channel.ssl.internal.SSLAlpnNegotiator.ThirdPartyAlpnNegotiator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.OutboundProtocolLink;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * Main Connection Link and TCPConnectionContext interface.
 */
public class SSLConnectionLink extends OutboundProtocolLink implements ConnectionLink, TCPConnectionContext {

    /** Trace component for WAS. Protect for use by inner classes. */
    protected static final TraceComponent tc = Tr.register(SSLConnectionLink.class,
                                                           SSLChannelConstants.SSL_TRACE_NAME,
                                                           SSLChannelConstants.SSL_BUNDLE);

    /** VC statemap key used for the connlink configuration */
    public static final String LINKCONFIG = "SSLLINKCONFIG";

    /** SSL channel that created this link. */
    private SSLChannel sslChannel = null;
    /** Config of this specific connection */
    private SSLLinkConfig linkConfig = null;
    /** SSL engine associated with this connection. */
    private SSLEngine sslEngine = null;
    /** my Read interface for channels to use. */
    private SSLReadServiceContext readInterface = null;
    /** my write interface for channels to use. */
    private SSLWriteServiceContext writeInterface = null;
    /** Device side service context. */
    private TCPConnectionContext deviceServiceContext = null;
    /** Device side read interface. */
    protected TCPReadRequestContext deviceReadInterface = null;
    /** Device side write interface. */
    private TCPWriteRequestContext deviceWriteInterface = null;
    /** SSL connection context queriable via the TCP interface. */
    private SSLConnectionContext sslConnectionContext = null;
    /** Result from discrimination if it happened. */
    private SSLDiscriminatorState discState = null;
    /** Flag to indicate already connected. */
    private volatile boolean connected = false;
    /** Flag to indicate conn link is closed. */
    private volatile boolean closed = false;
    /** Reference flag on whether this is an inbound connection or not */
    private boolean isInbound = false;
    /** Flag on whether there was a sync connect failure or not */
    private boolean syncConnectFailure = false;
    /** Hash code of the VC used in debug messages. */
    private int vcHashCode = 0;
    /** SSL Context associated with this connection. */
    private SSLContext sslContext = null;
    /** Target address for outbound connects. */
    private TCPConnectRequestContext targetAddress = null;
    /** ALPN protocol negotiated for this link */
    private String alpnProtocol;
    /** Keep track of HTTP/2 support on this link */
    private boolean http2Enabled = false;
    /** The third party ALPN negotiator used for this link */
    private ThirdPartyAlpnNegotiator alpnNegotiator = null;

    private final Lock cleanupLock = new ReentrantLock();

    /**
     * Constructor. Fields assigned here stay the same for the life of
     * the connection link, even across uses from the connection pool. Before
     * the link is used, the init method will be called.
     *
     * @param inputChannel
     */
    public SSLConnectionLink(SSLChannel inputChannel) {
        this.sslChannel = inputChannel;
        this.isInbound = inputChannel.getConfig().isInbound();
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#init(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    public void init(VirtualConnection inVC) {

        this.vcHashCode = inVC.hashCode();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "init, vc=" + getVCHash());
        }

        // Create the read and write interfaces for this connection link.
        super.init(inVC);
        initInterfaces(new SSLConnectionContextImpl(this, !isInbound),
                       new SSLReadServiceContext(this),
                       new SSLWriteServiceContext(this));

        // Check to see if http/2 is enabled for this connection and save the result
        if (CHFWBundle.getServletConfiguredHttpVersionSetting() != null) {
            if (CHFWBundle.isHttp2DisabledByDefault()) {
                if (getChannel().getUseH2ProtocolAttribute() != null && getChannel().getUseH2ProtocolAttribute()) {
                    http2Enabled = true;
                    this.sslChannel.checkandInitALPN();
                }
            } else if (CHFWBundle.isHttp2EnabledByDefault()) {
                if (getChannel().getUseH2ProtocolAttribute() == null || getChannel().getUseH2ProtocolAttribute()) {
                    http2Enabled = true;
                    this.sslChannel.checkandInitALPN();
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "init");
        }
    }

    void initInterfaces(SSLConnectionContext sslConnectionContext, SSLReadServiceContext readInterface, SSLWriteServiceContext writeInterface) {
        this.sslConnectionContext = sslConnectionContext;
        this.readInterface = readInterface;
        this.writeInterface = writeInterface;
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#close(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Exception)
     */
    @Override
    public void close(VirtualConnection inVC, Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "close, vc=" + getVCHash());
        }

        // Set closed flag so that ready can't be called again in an error condition.
        // This is a protective measure.
        closed = true;

        // Clean up the read and write interfaces as well as the SSL engine.

        // cleanup has logic to avoid writing if stop(0) has been called
        cleanup();

        //If the channel has already processed the close signal, it is too late to try and clean up the individual connection links here.
        //This race condition should not happen if channels above us are well behaved, so not using synchronize logic here, so as not to
        //impact mainline performance.
        if (this.sslChannel.getstop0Called() != true) {
            if (getDeviceLink() != null) {
                getDeviceLink().close(inVC, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "close");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#destroy(java.lang.Exception)
     */
    @Override
    public void destroy(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy, vc=" + getVCHash());
        }

        // Clean up the read and write interfaces as well as the SSL engine.
        this.connected = false;
        cleanup();
        getVirtualConnection().getStateMap().remove(LINKCONFIG);
        if (this.syncConnectFailure) {
            // sync connect failure needs cleanup below us but not above
            // us on the connlink chain
            super.destroy();
        } else {
            // otherwise use the destroy that goes up the chain
            super.destroy(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /**
     * This method is called from both close and destroy to clean up local resources.
     * Avoid object synchronization, but ensure only one thread does cleanup at a time.
     */
    public void cleanup() {
        cleanupLock.lock();
        try {
            // Clean up the write interface.
            if (null != writeInterface) {
                this.writeInterface.close();
                this.writeInterface = null;
            }
            // Clean up the read interface.
            if (null != readInterface) {
                this.readInterface.close();
                this.readInterface = null;
            }
            // Clean up the SSL engine.
            if (null != getSSLEngine()) {
                //If the channel has already processed the stop signal, it is too late to try and send the write handshake
                if (this.sslChannel.getstop0Called() == true) {
                    this.connected = false;
                }

                SSLUtils.shutDownSSLEngine(this, isInbound, this.connected);

                this.sslEngine = null;
            }
            // mark that we have disconnected
            this.connected = false;
        } finally {
            cleanupLock.unlock();
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getChannelAccessor()
     */
    @Override
    public Object getChannelAccessor() {
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#setDeviceLink(com.ibm.wsspi.channelfw.ConnectionLink)
     */
    @Override
    public void setDeviceLink(ConnectionLink next) {
        super.setDeviceLink(next);
        this.deviceServiceContext = (TCPConnectionContext) getDeviceLink().getChannelAccessor();
        this.deviceReadInterface = this.deviceServiceContext.getReadInterface();
        this.deviceWriteInterface = this.deviceServiceContext.getWriteInterface();
    }

    /**
     * This method will be called at one of two times. If this connection link
     * is part of an inbound chain, then this will be called when the device side
     * channel has accepted a new connection and determined that this is the next
     * channel in the chain. Note, that the Discriminator may have already been
     * run. The second case where this method may be called is if this connection
     * link is part of an outbound chain. In that case, this method will be called
     * when the initial outbound connect reports back success.
     *
     * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#ready(VirtualConnection)
     */
    @Override
    public void ready(VirtualConnection inVC) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "ready, vc=" + getVCHash());
        }
        // Double check for error condition where close already happened. Protective measure.
        if (!closed && FrameworkState.isValid()) {
            try {
                // Outbound connections took care of sslContext and sslEngine creation already.
                // If inbound, discrimination may have already created the engine and context
                if (isInbound) {
                    // See if discrimination ran already. Get the state map from the VC.
                    Map<Object, Object> stateMap = inVC.getStateMap();
                    // Extract and remove result of discrimination, if it happened.
                    discState = (SSLDiscriminatorState) stateMap.remove(SSLChannel.SSL_DISCRIMINATOR_STATE);
                    if (discState != null) {
                        // Discrimination has happened. Save already existing sslEngine.
                        sslEngine = discState.getEngine();
                        sslContext = discState.getSSLContext();
                        setLinkConfig((SSLLinkConfig) stateMap.get(SSLConnectionLink.LINKCONFIG));
                    } else if (sslContext == null || getSSLEngine() == null) {
                        // Create a new SSL context based on the current properties in the ssl config.
                        sslContext = getChannel().getSSLContextForInboundLink(this, inVC);
                        // Discrimination has not happened yet. Create new SSL engine.
                        sslEngine = SSLUtils.getSSLEngine(sslContext,
                                                          sslChannel.getConfig().getFlowType(),
                                                          getLinkConfig(),
                                                          this);
                    }
                } else {
                    // Outbound connect is ready. Ensure we have an sslContext and sslEngine.
                    if (sslContext == null || getSSLEngine() == null) {
                        // Create a new SSL context based on the current properties in the ssl config.
                        sslContext = getChannel().getSSLContextForOutboundLink(this, inVC, targetAddress);
                        // PK46069 - use engine that allows session id re-use
                        sslEngine = SSLUtils.getOutboundSSLEngine(
                                                                  sslContext, getLinkConfig(),
                                                                  targetAddress.getRemoteAddress().getHostName(),
                                                                  targetAddress.getRemoteAddress().getPort(),
                                                                  this);
                    }
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL engine hc=" + getSSLEngine().hashCode() + " associated with vc=" + getVCHash());
                }

                // Flag that connection has been established.
                // Need to set this to true for inbound and outbound so close will work right.
                connected = true;
                // Determine if this is an inbound or outbound connection.
                if (isInbound) {
                    readyInbound(inVC);
                } else {
                    readyOutbound(inVC, true);
                }
            } catch (Exception e) {
                if (FrameworkState.isStopping()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Ignoring exception during server shutdown: " + e);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught exception during ready, " + e, e);
                    }
                    FFDCFilter.processException(e, getClass().getName(), "238", this);
                }
                close(inVC, e);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ready called after close so do nothing");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "ready");
        }
    }

    /**
     * This callback is used to handle the initial SSL handshake when a
     * connection is ready to be handled and an asynchronous callback
     * is needed once an action is complete.
     */
    class MyHandshakeCompletedCallback implements SSLHandshakeCompletedCallback {
        /** Connection using this callback */
        private final SSLConnectionLink connLink;
        /** Buffer used at the network layer */
        private WsByteBuffer netBuffer;
        /** Buffer used to decrypt network data */
        private WsByteBuffer decryptedNetBuffer;
        /** Buffer used to encrypt application data */
        private WsByteBuffer encryptedAppBuffer;
        /** Inbound or outbound */
        private final FlowType flowType;
        /** allow other code to tell this class if they changed netBuffer */
        private WsByteBuffer updatedNetBuffer = null;

        /**
         * Constructor.
         *
         * @param _connLink           SSLConnectionLink associated with this callback.
         * @param _netBuffer          Buffer from the network / device side.
         * @param _decryptedNetBuffer Buffer containing results of decrypting netbuffer
         * @param _encryptedAppBuffer Encrypted buffer to be sent out through network / device side.
         * @param _flowType           inbound or outbound
         */
        public MyHandshakeCompletedCallback(
                                            SSLConnectionLink _connLink,
                                            WsByteBuffer _netBuffer,
                                            WsByteBuffer _decryptedNetBuffer,
                                            WsByteBuffer _encryptedAppBuffer,
                                            FlowType _flowType) {
            // Copy over the variables needed during complete and error.
            this.connLink = _connLink;
            this.netBuffer = _netBuffer;
            this.decryptedNetBuffer = _decryptedNetBuffer;
            this.encryptedAppBuffer = _encryptedAppBuffer;
            this.flowType = _flowType;
        }

        @Override
        public void updateNetBuffer(WsByteBuffer newBuffer) {
            netBuffer = newBuffer;
            updatedNetBuffer = newBuffer;
        }

        @Override
        public WsByteBuffer getUpdatedNetBuffer() {
            return updatedNetBuffer;
        }

        /*
         * @see com.ibm.ws.channel.ssl.internal.SSLHandshakeCompletedCallback#complete(javax.net.ssl.SSLEngineResult)
         */
        @Override
        public void complete(SSLEngineResult sslResult) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "complete (handshake), vc=" + getVCHash());
            }
            HandshakeStatus sslStatus = sslResult.getHandshakeStatus();
            if (flowType == FlowType.INBOUND) {
                connLink.readyInboundPostHandshake(netBuffer, decryptedNetBuffer,
                                                   encryptedAppBuffer, sslStatus);
            } else {
                try {
                    connLink.readyOutboundPostHandshake(netBuffer, decryptedNetBuffer,
                                                        encryptedAppBuffer, sslStatus, true);
                } catch (IOException e) {
                    close(getVirtualConnection(), e);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "complete (handshake), vc=" + getVCHash());
            }
        }

        /*
         * @see com.ibm.ws.channel.ssl.internal.SSLHandshakeCompletedCallback#error(java.io.IOException)
         */
        @Override
        public void error(IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, "error (handshake), vc=" + getVCHash());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during unwrap, " + ioe);
            }

            // cleanup possible ALPN resources
            if (flowType == FlowType.INBOUND) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cleanup possible ALPN resources - error callback");
                }
                AlpnSupportUtils.getAlpnResult(getSSLEngine(), this.connLink);
            }

            if (decryptedNetBuffer != null) {
                decryptedNetBuffer.release();
                decryptedNetBuffer = null;
            }
            if (netBuffer != null) {
                netBuffer.release();
                netBuffer = null;
                getDeviceReadInterface().setBuffers(null);
            }
            if (encryptedAppBuffer != null) {
                encryptedAppBuffer.release();
                encryptedAppBuffer = null;
            }

            if (flowType == FlowType.INBOUND) {
                close(connLink.getVirtualConnection(), ioe);
            } else {
                if (ioe == null) {
                    close(getVirtualConnection(), null);
                } else {
                    close(getVirtualConnection(), ioe);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "error (handshake), vc=" + getVCHash());
            }
        }
    }

    /**
     * Handle work required by the ready method for inbound connections.
     *
     * @param inVC
     */
    private void readyInbound(VirtualConnection inVC) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readyInbound, vc=" + getVCHash());
        }

        // Encrypted buffer from the network.
        WsByteBuffer netBuffer = getDeviceReadInterface().getBuffer();
        // Verify the buffer is not null first.
        if (netBuffer == null) {
            // Unable to handle this condition. TCP could not get any more data
            // and looked to punt to the only available channel above.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received null buffer so closing connection.");
            }
            close(inVC, null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "readyInbound, vc=" + getVCHash());
            }
            return;
        }
        netBuffer.flip();
        // Unencrypted buffer from the ssl engine output to be handed up to the application.
        WsByteBuffer decryptedNetBuffer = null;
        // Encrypted buffer from the ssl engine to be sent sent out on the network.
        WsByteBuffer encryptedAppBuffer = null;
        // Result output from the SSL engine.
        SSLEngineResult result = null;
        // Indicate that an error occurred and buffers should be cleaned up.
        boolean errorOccurred = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Initial read bytes: " + netBuffer.limit());
        }

        // Note, net and decNet buffers will always be one buffer for discrimination.
        // See if discrimination happened. Init method saved discState if so.
        if (discState == null) {
            // Allocate a new buffer for the results of decrypting the network buffer.
            decryptedNetBuffer = SSLUtils.allocateByteBuffer(
                                                             getAppBufferSize(), sslChannel.getConfig().getDecryptBuffersDirect());
        } else {
            // Extract the results of the call to the engine during discrimination.
            result = discState.getEngineResult();
            // Extract output buffer used during discrimination.
            decryptedNetBuffer = discState.getDecryptedNetBuffer();
            // Determine the resulting position and limit of networkBuffer after unwrap in discrimination.
            netBuffer.position(discState.getNetBufferPosition());
            netBuffer.limit(discState.getNetBufferLimit());
        }
        // Line up buffers needed for the SSL handshake. These are temporary.
        encryptedAppBuffer = SSLUtils.allocateByteBuffer(getPacketBufferSize(), true);

        try {
            if (discState == null) {
                // Since data is ready now, we can't call handleHandshake yet, unwrap first.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Before unwrap\r\n\tnetBuf: " + SSLUtils.getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecBuf: " + SSLUtils.getBufferTraceInfo(decryptedNetBuffer));
                }
                // Should not get any app data until handshake is done
                // Protect JSSE from potential SSL packet sizes that are too big.
                int savedLimit = SSLUtils.adjustBufferForJSSE(netBuffer, getPacketBufferSize());

                // Have the SSL engine inspect the first packet.
                result = getSSLEngine().unwrap(
                                               netBuffer.getWrappedByteBuffer(),
                                               decryptedNetBuffer.getWrappedByteBuffer());
                if (0 < result.bytesProduced()) {
                    decryptedNetBuffer.flip();
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc,
                             "After unwrap\r\n\tnetBuf: " + SSLUtils.getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecBuf: " + SSLUtils.getBufferTraceInfo(decryptedNetBuffer)
                                 + "\r\n\tstatus=" + result.getStatus()
                                 + " HSstatus=" + result.getHandshakeStatus()
                                 + " consumed=" + result.bytesConsumed()
                                 + " produced=" + result.bytesProduced());
                }

                // If adjustments were made for the JSSE, restore them.
                if (-1 != savedLimit) {
                    netBuffer.limit(savedLimit);
                }
                // If all data was consumed, clear the buffer.
                if (netBuffer.remaining() == 0) {
                    netBuffer.clear();
                }
            }

            // Build a callback for the asynchronous SSL handshake
            MyHandshakeCompletedCallback callback = new MyHandshakeCompletedCallback(this, netBuffer, decryptedNetBuffer, encryptedAppBuffer, FlowType.INBOUND);
            // Continue the SSL handshake. Do this with asynchronous handShake
            result = SSLUtils.handleHandshake(this, netBuffer, decryptedNetBuffer,
                                              encryptedAppBuffer, result, callback, false);

            // Check to see if the work was able to be done synchronously.
            if (result != null) {
                // Handshake is done.

                if ((callback != null) && (callback.getUpdatedNetBuffer() != null)) {
                    netBuffer = callback.getUpdatedNetBuffer();
                }

                readyInboundPostHandshake(netBuffer, decryptedNetBuffer,
                                          encryptedAppBuffer, result.getHandshakeStatus());
            } else {
                // Handshake is being done asynchronously.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "readyInbound");
                }
                return;
            }

        } catch (IOException ioe) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught ioexception, " + ioe);
            }
            errorOccurred = true;
            // Handle the handshake error.
            getChannel().getHandshakeErrorTracker().noteHandshakeError(ioe);
            close(inVC, ioe);
        } catch (ReadOnlyBufferException robe) {
            FFDCFilter.processException(robe, getClass().getName(), "359", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught read-only exception, " + robe);
            }
            errorOccurred = true;
            close(inVC, robe);
        }

        // Clean up buffers if an error occurred. Note, connection was already closed.
        if (errorOccurred) {
            if (decryptedNetBuffer != null) {
                decryptedNetBuffer.release();
                decryptedNetBuffer = null;
            }
            netBuffer.release();
            netBuffer = null;
            getDeviceReadInterface().setBuffers(null);
            if (encryptedAppBuffer != null) {
                encryptedAppBuffer.release();
                encryptedAppBuffer = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readyInbound");
        }
    }

    /**
     * This callback is used after the initial read is done inbound and after the
     * SSL handshake. It is only used when there is no data beyond the handshake.
     * Once data is ready, the discrimination process is kicked off from here to
     * determine the next channel.
     */
    public class MyReadCompletedCallback implements TCPReadCompletedCallback {
        /** Buffer used for decrypted data */
        private WsByteBuffer decryptedNetBuffer;

        /**
         * Constructor.
         *
         * @param _decryptedNetBuffer
         */
        public MyReadCompletedCallback(WsByteBuffer _decryptedNetBuffer) {
            this.decryptedNetBuffer = _decryptedNetBuffer;
        }

        /*
         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
         */
        @Override
        public void complete(VirtualConnection inVC, TCPReadRequestContext rsc) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "complete (read), vc=" + getVCHash());
            }
            determineNextChannel();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "complete (read), vc=" + getVCHash());
            }
        }

        /*
         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext,
         * java.io.IOException)
         */
        @Override
        public void error(VirtualConnection inVC, TCPReadRequestContext rsc, IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "error (read), vc=" + getVCHash());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught IOException during read, " + ioe);
            }
            // Clean up buffer allocated for read. Note, if this happened on an early read
            // where decryptedNetBuffer was null, we must protect from an NPE here.
            if (decryptedNetBuffer != null) {
                decryptedNetBuffer.release();
            }
            decryptedNetBuffer = null;
            close(inVC, ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "error (read), vc=" + getVCHash());
            }
        }
    }

    /**
     * This method is called after the SSL handshake has taken place.
     *
     * @param netBuffer
     * @param decryptedNetBuffer
     * @param encryptedAppBuffer
     * @param hsStatus
     */
    protected void readyInboundPostHandshake(
                                             WsByteBuffer netBuffer,
                                             WsByteBuffer decryptedNetBuffer,
                                             WsByteBuffer encryptedAppBuffer,
                                             HandshakeStatus hsStatus) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readyInboundPostHandshake, vc=" + getVCHash());
        }

        // Release the no longer needed buffers.
        encryptedAppBuffer.release();

        if (hsStatus == HandshakeStatus.FINISHED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cleanup possible ALPN resources - handshake finished");
            }
            AlpnSupportUtils.getAlpnResult(getSSLEngine(), this);

            // PK16095 - take certain actions when the handshake completes
            getChannel().onHandshakeFinish(getSSLEngine());

            // Handshake complete. Now get the request. Use our read interface so unwrap already done.
            // Check if data exists in the network buffer still. This would be app data beyond handshake.
            if (netBuffer.remaining() == 0 || netBuffer.position() == 0) {
                // No app data. Release the netBuffer as it will no longer be used.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Releasing netBuffer: " + netBuffer.hashCode());
                }
                netBuffer.release();
                getDeviceReadInterface().setBuffers(null);
            } else {
                // Found encrypted app data. Don't release the network buffer yet. Let the read decrypt it.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "App data exists in netBuffer after handshake: " + netBuffer.remaining());
                }
            }

            readInterface.setBuffer(decryptedNetBuffer);
            // No need to save number of bytes read.
            MyReadCompletedCallback readCallback = new MyReadCompletedCallback(decryptedNetBuffer);
            if (null != readInterface.read(1, readCallback, false, TCPRequestContext.USE_CHANNEL_TIMEOUT)) {
                // Read was handled synchronously.
                determineNextChannel();
            }
        } else {
            // Unknown result from handshake. All other results should have thrown exceptions.
            // Clean up buffers used during read.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unhandled result from SSL engine: " + hsStatus);
                Tr.debug(tc, "Cleanup possible ALPN resources on unhandled results");
            }
            AlpnSupportUtils.getAlpnResult(getSSLEngine(), this);

            netBuffer.release();
            getDeviceReadInterface().setBuffers(null);
            decryptedNetBuffer.release();

            SSLException ssle = new SSLException("Unhandled result from SSL engine: " + hsStatus);
            FFDCFilter.processException(ssle, getClass().getName(), "401", this);
            close(getVirtualConnection(), ssle);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readyInboundPostHandshake");
        }
    }

    /**
     * Handle work required by the ready method for outbound connections. When called, the
     * outbound socket has been established. Establish the SSL connection before reporting
     * to the next channel. Note, this method is called in both sync and async flows.
     *
     * @param inVC  virtual connection associated with this request
     * @param async flag for asynchronous (true) or synchronous (false)
     * @throws IOException
     */
    private void readyOutbound(VirtualConnection inVC, boolean async) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readyOutbound, vc=" + getVCHash());
        }

        final SSLChannelData config = this.sslChannel.getConfig();
        // Encrypted buffer from the network.
        WsByteBuffer netBuffer = SSLUtils.allocateByteBuffer(getPacketBufferSize(), config.getEncryptBuffersDirect());
        // Unencrypted buffer from the ssl engine output to be handed up to the application.
        WsByteBuffer decryptedNetBuffer = SSLUtils.allocateByteBuffer(getAppBufferSize(), config.getDecryptBuffersDirect());
        // Encrypted buffer from the ssl engine to be sent sent out on the network.
        WsByteBuffer encryptedAppBuffer = SSLUtils.allocateByteBuffer(getPacketBufferSize(), true);
        // Result from the SSL engine.
        SSLEngineResult sslResult = null;
        // Build the required callback.
        MyHandshakeCompletedCallback callback = null;
        // Flag if an error took place
        IOException exception = null;

        // Only create the callback if this is for an async request.
        if (async) {
            callback = new MyHandshakeCompletedCallback(this, netBuffer, decryptedNetBuffer, encryptedAppBuffer, FlowType.OUTBOUND);
        }

        try {
            // Start the aynchronous SSL handshake.
            sslResult = SSLUtils.handleHandshake(this, netBuffer, decryptedNetBuffer,
                                                 encryptedAppBuffer, sslResult, callback, false);
            // Check to see if the work was able to be done synchronously.
            if (sslResult != null) {
                // Handshake was done synchronously.
                if ((callback != null) && (callback.getUpdatedNetBuffer() != null)) {
                    netBuffer = callback.getUpdatedNetBuffer();
                }

                readyOutboundPostHandshake(netBuffer, decryptedNetBuffer,
                                           encryptedAppBuffer, sslResult.getHandshakeStatus(), async);
            }
        } catch (IOException e) {
            exception = e;
        } catch (ReadOnlyBufferException e) {
            exception = new IOException("Caught exception: " + e);
        }

        if (exception != null) {
            FFDCFilter.processException(exception, getClass().getName(), "540", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during handshake after connect, " + exception);
            }
            // Release the buffers.
            if (netBuffer != null) {
                netBuffer.release();
                netBuffer = null;
                getDeviceReadInterface().setBuffers(null);
            }
            if (decryptedNetBuffer != null) {
                decryptedNetBuffer.release();
                decryptedNetBuffer = null;
            }
            if (encryptedAppBuffer != null) {
                encryptedAppBuffer.release();
                encryptedAppBuffer = null;
            }
            if (async) {
                close(inVC, exception);
            } else {
                this.syncConnectFailure = true;
                close(inVC, exception);
                throw exception;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readyOutbound");
        }
        return;
    }

    /**
     * This method is called to handle the results of an SSL handshake. This may be called
     * by a callback or in the same thread as the connect request.
     *
     * @param netBuffer          buffer for data flowing in fron the net
     * @param decryptedNetBuffer buffer for decrypted data from the net
     * @param encryptedAppBuffer buffer for encrypted data flowing from the app
     * @param hsStatus           output from the last call to the SSL engine
     * @param async              whether this is for an async (true) or sync (false) request
     * @throws IOException
     */
    protected void readyOutboundPostHandshake(
                                              WsByteBuffer netBuffer,
                                              WsByteBuffer decryptedNetBuffer,
                                              WsByteBuffer encryptedAppBuffer,
                                              HandshakeStatus hsStatus,
                                              boolean async) throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readyOutboundPostHandshake, vc=" + getVCHash());
        }

        // Exception to call destroy with in case of bad return code from SSL engine.
        IOException exception = null;

        if (hsStatus != HandshakeStatus.FINISHED) {
            // Handshake failed.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected results of handshake after connect, " + hsStatus);
            }
            exception = new IOException("Unexpected results of handshake after connect, " + hsStatus);
        }

        // PK16095 - take certain actions when the handshake completes
        getChannel().onHandshakeFinish(getSSLEngine());

        // Null out the buffer references on the device side so they don't wrongly reused later.
        getDeviceReadInterface().setBuffers(null);

        // Clean up the buffers.
        // PI48725 Start
        // Handshake complete.  Now get the request.  Use our read interface so unwrap already done.
        // Check if data exists in the network buffer still.  This would be app data beyond handshake.
        if (netBuffer.remaining() == 0 || netBuffer.position() == 0) {
            // No app data.  Release the netBuffer as it will no longer be used.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Releasing netBuffer: " + netBuffer.hashCode());
            }
            // Null out the buffer references on the device side so they don't wrongly reused later.
            netBuffer.release();

        } else {
            // Found encrypted app data.  Don't release the network buffer yet.  Let the read decrypt it.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "App data exists in netBuffer after handshake: " + netBuffer.remaining());
            }
            this.readInterface.setNetBuffer(netBuffer);
        }
        // PI48725 Finish

        // Clean up the buffers.
        decryptedNetBuffer.release();
        encryptedAppBuffer.release();

        // Call appropriate callback if async
        if (async) {
            if (exception != null) {
                close(getVirtualConnection(), exception);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling ready method.");
                }
                super.ready(getVirtualConnection());
            }
        } else {
            if (exception != null) {
                throw exception;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readyOutboundPostHandshake");
        }
    }

    /**
     * This method is called if connect or connectAsync are called redundantly, after
     * the connection is already established. It cleans up the SSL engine. The connect
     * methods will then pass the connect on down the chain where, eventually, a new
     * socket will be established with this virtual connection.
     */
    private void handleRedundantConnect() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "handleRedundantConnect, vc=" + getVCHash());
        }
        // This conn link has already been connected.
        // Need to shut get a new SSL engine.
        cleanup();
        // PK46069 - use engine that allows session id re-use
        sslEngine = SSLUtils.getOutboundSSLEngine(
                                                  sslContext, getLinkConfig(),
                                                  targetAddress.getRemoteAddress().getHostName(),
                                                  targetAddress.getRemoteAddress().getPort(),
                                                  this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "New SSL engine=" + getSSLEngine().hashCode() + " for vc=" + getVCHash());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "handleRedundantConnect");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#connectAsynch(java.lang.Object)
     */
    @Override
    public void connectAsynch(Object address) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "connectAsynch, vc=" + getVCHash());
        }
        // Determine if this is a redundant connect.
        if (connected) {
            handleRedundantConnect();
        }
        this.targetAddress = (TCPConnectRequestContext) address;
        // Nothing specific to SSL on connect. Pass through.
        ((OutboundConnectionLink) getDeviceLink()).connectAsynch(address);
        // The SSL handshake will happen in the ready method path.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "connectAsynch");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#connect(java.lang.Object)
     */
    @Override
    public void connect(Object address) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "connect, vc=" + getVCHash());
        }
        // Determine if this is a redundant connect.
        if (connected) {
            handleRedundantConnect();
        }
        this.targetAddress = (TCPConnectRequestContext) address;
        // Nothing specific to SSL on connect. Pass through.
        ((OutboundConnectionLink) getDeviceLink()).connect(address);
        // PK13349 - mark that we are now connected
        this.connected = true;

        // First check if the sslContext and sslEngine have already been set (discrimination case)
        if (sslContext == null || getSSLEngine() == null) {
            // Create a new SSL context based on the current properties in the ssl config.
            this.sslContext = getChannel().getSSLContextForOutboundLink(this, getVirtualConnection(), address);
            // Discrimination has not happened yet. Create new SSL engine.
            // PK46069 - use engine that allows session id re-use
            this.sslEngine = SSLUtils.getOutboundSSLEngine(sslContext, getLinkConfig(),
                                                           targetAddress.getRemoteAddress().getHostName(),
                                                           targetAddress.getRemoteAddress().getPort(),
                                                           this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "SSL engine hc=" + getSSLEngine().hashCode() + " associated with vc=" + getVCHash());
        }

        // Now do the SSL handshake.
        readyOutbound(getVirtualConnection(), false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "connect");
        }
    }

    /**
     * When determining the next channel on an inbound chain, a result of MAYBE
     * can result from calling discrmination forcing more data to be read in. This
     * callback is used so that read can be asynchronous.
     */
    public class MoreDataNeededCallback implements TCPReadCompletedCallback {
        /*
         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
         */
        @Override
        public void complete(VirtualConnection inVC, TCPReadRequestContext rsc) {
            determineNextChannel();
        }

        /*
         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext,
         * java.io.IOException)
         */
        @Override
        public void error(VirtualConnection inVC, TCPReadRequestContext rsc, IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception reading more data to determine next channel, " + ioe);
            }
            FFDCFilter.processException(ioe, getClass().getName(), "2360", this);
            close(inVC, ioe);
        }
    }

    /**
     * This method is called from ready() when the SSL engine responds with a fully unencypted
     * packet of data to be sent up the chain. Discrimination will be handled here.
     */
    protected void determineNextChannel() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "determineNextChannel, vc=" + getVCHash());
        }

        // Pass the buffers up to the application side channel.
        ConnectionReadyCallback linkOnApplicationSide = getApplicationCallback();
        if (linkOnApplicationSide != null) {
            linkOnApplicationSide.ready(getVirtualConnection());
        } else {
            int discriminationResult = Discriminator.YES;
            try {
                // Need to go all the way back to the SSL channel to get the
                // latest disc process in case it has changed.
                discriminationResult = getChannel().getDiscriminationProcess().discriminate(getVirtualConnection(), readInterface.getBuffers(), this);
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception caught doing discriminate, " + e);
                }
                FFDCFilter.processException(e, getClass().getName(), "346", this);
                throw new RuntimeException("Exception caught doing discriminate, " + e);
            }

            switch (discriminationResult) {
                case DiscriminationProcess.SUCCESS: {
                    // Call the next channel, assigned during discrimination.
                    getApplicationCallback().ready(getVirtualConnection());
                    break;
                }
                case DiscriminationProcess.FAILURE: {
                    // No next channel will be called. Therefore, read interface buffers must be freed here.
                    WsByteBuffer[] buffers = getDeviceReadInterface().getBuffers();
                    // clear out the readInterface buffer so that we only close (release) it once
                    for (int i = 0; i < buffers.length; i++) {
                        if (buffers[i] == readInterface.netBuffer) {
                            buffers[i] = null;
                        }
                    }
                    WsByteBufferUtils.releaseBufferArray(buffers);
                    // Close down the vc with an exception
                    close(getVirtualConnection(), new Exception("Failure response from discrimination process."));
                    break;
                }
                case DiscriminationProcess.AGAIN: {
                    // TODO: this doesn't set up for a read...
                    // Need to read more in order to determine next channel. ForceQueue is true.
                    readInterface.setBuffer(readInterface.getBuffer());
                    // The read will be asynchronous so a callback is needed.
                    MoreDataNeededCallback callback = new MoreDataNeededCallback();
                    // No need to save number of bytes read. If problem occurs here, IOException is thrown.
                    // Do the asychronous read. Forcequeue is false, so the callback may not be necessary.
                    if (null != readInterface.read(1, callback, false, TCPRequestContext.USE_CHANNEL_TIMEOUT)) {
                        // Data is ready. The callback will not be called.
                        // Recursively call this method to determine the next channel.
                        determineNextChannel();
                    }
                    break;
                }
                default: {
                    // No next channel will be called. Therefore, read interface buffers must be freed here.
                    WsByteBufferUtils.releaseBufferArray(getDeviceReadInterface().getBuffers());
                    // Close down the vc with an exception
                    close(getVirtualConnection(), new Exception("Unknown response from discrimination process, " + discriminationResult));
                    break;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "determineNextChannel");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.OutboundProtocolLink#postConnectProcessing(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    public void postConnectProcessing(VirtualConnection inVC) {
        // Nothing to be done here.
    }

    /**
     * Fetch the SSL channel that created this link.
     *
     * @return SSL channel
     */
    public SSLChannel getChannel() {
        return this.sslChannel;
    }

    /**
     * Fetch the SSL engine associated with this link.
     *
     * @return SSL engine
     */
    public SSLEngine getSSLEngine() {
        return this.sslEngine;
    }

    /**
     * Query the appropriate application buffer size for this connection.
     *
     * @return int
     */
    public int getAppBufferSize() {
        return getSSLEngine().getSession().getApplicationBufferSize();
    }

    /**
     * Query the appropriate packet buffer size for this connection.
     *
     * @return int
     */
    public int getPacketBufferSize() {
        return getSSLEngine().getSession().getPacketBufferSize();
    }

    /**
     * Fetch the read interface for the channel on the device side
     *
     * @return device side channel read interface
     */
    public TCPReadRequestContext getDeviceReadInterface() {
        return this.deviceReadInterface;
    }

    /**
     * Fetch the write interface for the channel on the device side
     *
     * @return device side channel write interface
     */
    public TCPWriteRequestContext getDeviceWriteInterface() {
        return this.deviceWriteInterface;
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPConnectionContext#getReadInterface()
     */
    @Override
    public TCPReadRequestContext getReadInterface() {
        return this.readInterface;
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPConnectionContext#getWriteInterface()
     */
    @Override
    public TCPWriteRequestContext getWriteInterface() {
        return this.writeInterface;
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPConnectionContext#getRemoteAddress()
     */
    @Override
    public InetAddress getRemoteAddress() {
        return this.deviceServiceContext.getRemoteAddress();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPConnectionContext#getRemotePort()
     */
    @Override
    public int getRemotePort() {
        return this.deviceServiceContext.getRemotePort();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPConnectionContext#getLocalAddress()
     */
    @Override
    public InetAddress getLocalAddress() {
        return this.deviceServiceContext.getLocalAddress();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPConnectionContext#getLocalPort()
     */
    @Override
    public int getLocalPort() {
        return this.deviceServiceContext.getLocalPort();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPConnectionContext#getSSLContext()
     */
    @Override
    public SSLConnectionContext getSSLContext() {
        return this.sslConnectionContext;
    }

    /**
     * Set the connection specific configuration to the input value.
     *
     * @param config
     */
    protected void setLinkConfig(SSLLinkConfig config) {
        this.linkConfig = config;
    }

    /**
     * Query the current connection specific configuration.
     *
     * @return SSLLinkConfig
     */
    public SSLLinkConfig getLinkConfig() {
        return this.linkConfig;
    }

    /**
     * Query the hash of the virtual connection, used for debug.
     *
     * @return int
     */
    protected int getVCHash() {
        return this.vcHashCode;
    }

    /**
     * Set the ALPN protocol negotiated for this link
     *
     * @param String protocol
     */
    public void setAlpnProtocol(String protocol) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setAlpnProtocol: " + protocol + " " + this);
        }
        this.alpnProtocol = protocol;
        this.sslConnectionContext.setAlpnProtocol(protocol);
    }

    /**
     * The ALPN protocol negotiated for this link
     *
     * @return the protocol String, or null if ALPN was not used
     */
    public String getAlpnProtocol() {
        return this.alpnProtocol;
    }

    /**
     * @return true if http/2 and ALPN are enabled
     */
    protected boolean isAlpnEnabled() {
        return this.http2Enabled;
    }

    /**
     * Set the negotiator object that will be used during protocol negotiation. Only needed for grizzly-npn and jetty-alpn
     *
     * @param ThirdPartyAlpnNegotiator to use for this connection
     */
    protected void setAlpnNegotiator(ThirdPartyAlpnNegotiator negotiator) {
        this.alpnNegotiator = negotiator;
    }

    /**
     * @return ThirdPartyAlpnNegotiator used for this connection
     */
    protected ThirdPartyAlpnNegotiator getAlpnNegotiator() {
        return this.alpnNegotiator;
    }
}
