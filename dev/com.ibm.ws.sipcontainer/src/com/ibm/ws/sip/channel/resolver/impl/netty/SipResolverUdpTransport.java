/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.openliberty.netty.internal.*;
import io.openliberty.netty.internal.exception.NettyException;

/**
 * 
 * This class handles the details of using the UDP channel for network
 * transport for the SipResolver
 *
 */
class SipResolverUdpTransport implements SipResolverTransport {

    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipResolverUdpTransport.class);

    private static NettyFramework _framework;

    private static final int WRITE_STATE_DISCONNECTED = 0;
    private static final int WRITE_STATE_CONNECTING = 1;
    private static final int WRITE_STATE_IDLE = 2;
    private static final int WRITE_STATE_WRITE_ACTIVE = 3;
    private static final int WRITE_STATE_SHUTDOWN = 4;

    private static final int READ_STATE_READY = 1;
    private static final int READ_STATE_DISCONNECTED = 2;
    private static final int READ_STATE_SHUTDOWN = 3;

    private static final int MAX_WRITE_QUEUE_SIZE = 5000;

    private SipResolverTransportListener _transportListener = null;
    private Vector<InetSocketAddress> _nameServers = null;
    private Iterator<InetSocketAddress> _nameServerIterator = null;
    private int _writeState = WRITE_STATE_DISCONNECTED;
    private int _readState = READ_STATE_DISCONNECTED;
    private boolean _shutdown = false;
    private Queue<ByteBuf> _requestQueue = new LinkedList<ByteBuf>();

    /** Netty channel associated with the resolver connection */
    protected Channel channel;
    private BootstrapExtended bootstrap;

    private static boolean _channelInitialized = false;
    protected InetSocketAddress _currentSocketAddress = null;

    private String CHAINNAME = "SipResolver-udp-outbound";

    private boolean reConnectAllowed = true;
    private int _connectionFailedCount = -1;
    private int _transportErrorCount = 0;
    private boolean connectDone = false;

    // these allowed thresholds will be re-determined, using the number of available
    // DNS servers, when the object is instantiated
    private int _ConnectFailuresAllowed = 2;
    private int _TransportErrorsAllowed = 3;

    // if DNS has not responded to our request for TIMEOUT_TIME, then try to
    // rollover to new DNS
    protected static final int TIMEOUT_TIME = 3000;

    protected SipResolverUdpTransport() {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "SipResolverUdpTransport: constructor()");
            c_logger.traceExit(this, "SipResolverUdpTransport: constructor()");
        }
    }

    synchronized protected void initialize(NettyFramework framework) {

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this,
                    "SipResolverUdpTransport: initialize() _channelInitialized:" + _channelInitialized);

        if (_channelInitialized == false) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: initialize: getChannelFramewor()");

            /* Create the channel configuration */
            _framework = framework;
            try {
                Map<String, Object> options = new HashMap<String, Object>();
                options.put(ConfigConstants.EXTERNAL_NAME, this.CHAINNAME);
                bootstrap = _framework.createUDPBootstrapOutbound(options);
                _writeState = WRITE_STATE_DISCONNECTED;
                _readState = READ_STATE_DISCONNECTED;
                reConnectAllowed = true;
                _channelInitialized = true;
            } catch (NettyException e) {
                if (c_logger.isTraceEntryExitEnabled())
                    c_logger.traceExit(this, "SipResolverUdpTransport: initialize failed due to: " + e);
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: initialize()");
    }

    protected SipResolverUdpTransport(Vector<InetSocketAddress> nameServers,
            SipResolverTransportListener transportListener, NettyFramework framework) {

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this,
                    "SipResolverUdpTransport: constructor(Vector, SipResolverTransportListener): entry: id="
                            + hashCode() + " " + framework);

        /* setup Netty */
        initialize(framework);

        _nameServers = nameServers;
        _nameServerIterator = _nameServers.iterator();
        _transportListener = transportListener;

        _ConnectFailuresAllowed = _nameServers.size() * 2;
        _TransportErrorsAllowed = _nameServers.size() * 3;

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        bootstrap.handler(new SipResolverUDPInitializer(bootstrap.getBaseInitializer()));

        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(
                    "SipResolverTcpTransport: contructor: _ConnectFailuresAllowed: " + _ConnectFailuresAllowed);
            c_logger.traceDebug(
                    "SipResolverTcpTransport: contructor: _TransportErrorsAllowed: " + _TransportErrorsAllowed);
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this,
                    "SipResolverUdpTransport: constructor(Vector, SipResolverTransportListener): entry: ");
    }

    synchronized protected void shutdown() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: shutdown: entry: id=" + hashCode());

        _shutdown = true;
        _requestQueue.clear();
        _writeState = WRITE_STATE_SHUTDOWN;
        _readState = READ_STATE_SHUTDOWN;

        if (channel != null) {
            try {
                // tell the channel to close; the framework will be notified
                channel.close().sync();
            } catch (InterruptedException e) {
                if (c_logger.isTraceEntryExitEnabled())
                    c_logger.traceExit(this,
                            "SipResolverUdpTransport: shutdown: interrupted waiting for channel close: " + e);
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: shutdown: exit: ");
    }

    /**
     * 
     */
    private void talkToDNS() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: talkToDNS: entry: id=" + hashCode());

        // move to next name server if last one failed, or the first server if
        // this is the first time in here.
        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: talkToDNS: Find DNS Server in list");

        if (_nameServerIterator.hasNext() == false) {
            _nameServerIterator = _nameServers.iterator();
            _currentSocketAddress = _nameServerIterator.next();
        } else {
            _currentSocketAddress = _nameServerIterator.next();
        }

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug(
                    "SipResolverUdpTransport: talkToDNS: SIP Resolver nameserver target: " + _currentSocketAddress);

        if (connectDone) {

            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: talkToDNS: connectAsynch called go right to ready()");
            ready();

        } else {

            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: talkToDNS: connectAsynch not called yet, do so now");

            /** open the listener socket */
            try {
                _framework.startOutbound(bootstrap, "*", 0, f -> {
                    if (f.isCancelled() || !f.isSuccess()) {
                        if (c_logger.isWarnEnabled()) {
                            c_logger.warn("Resolver channel exception during connect: " + f.cause().getMessage());
                        }
                        destroy((Exception) f.cause());
                    } else {
                        channel = f.channel();
                        ready();
                    }
                });
            } catch (NettyException e) {
                e.printStackTrace();
                if (c_logger.isWarnEnabled()) {
                    c_logger.warn("Resolver channel exception during connect: " + e.getMessage());
                }
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: talkToDNS: exit: ");
    }

    public void ready() {

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: ready: entry: id=" + hashCode());

        connectDone = true;
        _writeState = WRITE_STATE_IDLE;
        _readState = READ_STATE_READY;

        _connectionFailedCount = 0;
        reConnectAllowed = false;

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: ready: UDP read request");

        drainRequestQueue();

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: ready: exit: ");

    }

    /**
     * This method is called by the connection callback when the connectAsynch fails
     * 
     * @param e
     */
    public void destroy(Exception e) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: destroy(Exception e)");

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: destroy: Socket destroyed " + e);

        _readState = READ_STATE_DISCONNECTED;
        _writeState = WRITE_STATE_DISCONNECTED;

        _connectionFailedCount++;

        if (_connectionFailedCount <= _ConnectFailuresAllowed) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: error: calling transportError - _connectionFailedCount: "
                        + _connectionFailedCount);

            // try to rollover to the next name server
            _transportListener.transportError(e, this);
        } else {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: error: calling transportFailed - _connectionFailedCount: "
                        + _connectionFailedCount);

            // can't connect to any name serves.
            _connectionFailedCount = 0;
            _transportErrorCount = 0;
            _transportListener.transportFailed(e, this);
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: destroy(Exception e)");
    }

    /**
     * Handle write request based on _writeState.
     */
    synchronized public void writeRequest(ByteBuf requestBuffer) throws IOException {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: writeRequest: entry id=" + hashCode());

        if (_shutdown == true)
            throw new IllegalStateException("SIP UDP Resolver transport is shutdown.");

        switch (_writeState) {
        case WRITE_STATE_SHUTDOWN:
            // unreachable? debug only
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_SHUTDOWN");

            break;

        case WRITE_STATE_IDLE:
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_IDLE");

            ChannelFuture writeFuture = writeBufferAsDatagram(requestBuffer);

            if (!writeFuture.isDone()) {
                _writeState = WRITE_STATE_WRITE_ACTIVE;
                writeFuture.addListener(f -> {
                    if (f.isSuccess()) {
                        writeComplete();
                    } else {
                        writeError((Exception) f.cause());
                    }
                });
            } else {
                // write completed so call complete from here
                writeComplete();
            }
            break;

        case WRITE_STATE_WRITE_ACTIVE:
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_WRITE_ACTIVE");

            if (_requestQueue.size() > MAX_WRITE_QUEUE_SIZE)
                throw new IOException("Maximum write queue size is being exceeded");
            _requestQueue.add(requestBuffer);
            break;

        case WRITE_STATE_CONNECTING:
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_CONNECTING");

            _requestQueue.add(requestBuffer);
            break;

        case WRITE_STATE_DISCONNECTED:
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport:writeRequest: WRITE_STATE_DISCONNECTED");

            _requestQueue.add(requestBuffer);

            // only rollover, or try again, once the request queue has been reset
            if (reConnectAllowed) {
                talkToDNS();
            }
            break;
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: writeRequest: exit: ");
    }

    /**
     *  This method is called by the timeout watcher when no response has been
     *  received.
     */  
    public void destroyFromTimeout(Exception e) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: destroyFromTimeout(Exception e)");

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: destroyFromTimeout: Socket destroyed " + e);

        _readState = READ_STATE_DISCONNECTED;
        _writeState = WRITE_STATE_DISCONNECTED;

        _transportErrorCount++;

        if (_transportErrorCount <= _TransportErrorsAllowed) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug(
                        "SipResolverUdpTransport: destroyFromTimeout: calling transportError - _transprtErrorCount: "
                                + _transportErrorCount);

            _transportListener.transportError(e, this);
        } else {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug(
                        "SipResolverUdpTransport: destroyFromTimeout: calling transportFailed - _transprtErrorCount: "
                                + _transportErrorCount);

            _transportErrorCount = 0;
            _transportListener.transportFailed(e, this);
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: destroyFromTimeout(Exception e)");
    }

    /**
     * clear the request queue of all outstanding requests
     */
    public void prepareForReConnect() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: prepareForReConnect");

        // Clear the request queue of all outstanding requests.
        // Only clear when the object using us tells us to clear,
        // otherwise we risk timing windows whereby we clear out requests
        // which were meant for rollover, or we fail to clear out an attempt
        // that will later be retried.
        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: prepareForReConnect: clear out _requestQueue: # of items: "
                    + _requestQueue.size());
        _requestQueue.clear();

        // only allow reconnects once we know the request queue will only hold
        // new valid attempts.
        reConnectAllowed = true;

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: prepareForReConnect");
    }

    protected void messageRead(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this,
                    "SipResolverUdpTransport: complete(VirtualConnection vc, UDPReadRequestContext rsc) _readState: "
                            + _readState);

        _transportErrorCount = 0;
        _transportListener.responseReceived(msg);

        if (_readState != READ_STATE_DISCONNECTED) {
            _readState = READ_STATE_READY;

            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: complete: read message body");

            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverUdpTransport: complete: UDP read request");
            channel.read();
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this,
                    "SipResolverUdpTransport: complete(VirtualConnection vc, UDPReadRequestContext rsc)");
    }

    public void readError(ChannelHandlerContext ctx, Throwable e) throws Exception {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: readError(Exception e)");

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: Read error: " + e);

        if (_shutdown == false) {
            _readState = READ_STATE_DISCONNECTED;
            _writeState = WRITE_STATE_DISCONNECTED;

            _transportErrorCount++;
            if (_transportErrorCount < _TransportErrorsAllowed) {
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport: error: calling transportError - _transprtErrorCount: "
                            + _transportErrorCount);

                _transportListener.transportError((Exception) e, this);

            } else {
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug(
                            "SipResolverUdpTransport: error: calling transportFailed - _transprtErrorCount: "
                                    + _transportErrorCount);

                _transportErrorCount = 0;
                _transportListener.transportFailed((Exception) e, this);
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this,
                    "SipResolverUdpTransport: error(VirtualConnection vc, UDPReadRequestContext rrc, IOException ioe)");
    }

    /**
     * Complete the write by clearing error counts and draining
     * the request queue.
     */
    public void writeComplete() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this,
                    "SipResolverUdpTransport: complete(VirtualConnection vc, UDPWriteRequestContext wrc)");

        _transportErrorCount = 0;

        drainRequestQueue();

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this,
                    "SipResolverUdpTransport: complete(VirtualConnection vc, UDPWriteRequestContext wrc)");
    }

    /**
     * Handle error in write.
     */
    public void writeError(Exception e) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: writeError(Exception e)");
        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverUdpTransport: Write error: " + e);

        if (_shutdown == false) {
            _readState = READ_STATE_DISCONNECTED;
            _writeState = WRITE_STATE_DISCONNECTED;

            _transportErrorCount++;
            if (_transportErrorCount < _TransportErrorsAllowed) {
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport: error: calling transportError - _transprtErrorCount: "
                            + _transportErrorCount);

                _transportListener.transportError(e, this);

            } else {
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug(
                            "SipResolverUdpTransport: error: calling transportFailed - _transprtErrorCount: "
                                    + _transportErrorCount);

                _transportErrorCount = 0;
                _transportListener.transportFailed(e, this);
            }
        }

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this,
                    "SipResolverUdpTransport: error(VirtualConnection vc, UDPWriteRequestContext wrc, IOException ioe)");
        }
    }

    /**
     * Handle requests in queue
     */
    synchronized private void drainRequestQueue() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverUdpTransport: drainRequestQueue: entry _writeState: " + _writeState);

        while (true) {
            ByteBuf requestBuffer = _requestQueue.poll();

            if (requestBuffer != null) {
                InetSocketAddress local = (InetSocketAddress) channel.localAddress();
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverUdpTransport:drainRequestQueue: writing new message, length = "
                            + requestBuffer.readableBytes() + " content as string: "
                            + requestBuffer.toString(Charset.defaultCharset()) + " remote addr: "
                            + _currentSocketAddress + " local addr: " + local);

                ChannelFuture writeFuture = writeBufferAsDatagram(requestBuffer);

                if (!writeFuture.isDone()) {
                    _writeState = WRITE_STATE_WRITE_ACTIVE;
                    writeFuture.addListener(f -> {
                        if (f.isSuccess()) {
                            writeComplete();
                        } else {
                            writeError((Exception) f.cause());
                        }
                    });
                    break;
                } else {
                    // write completed so call complete from here
                    writeComplete();
                }
            } else {
                _writeState = WRITE_STATE_IDLE;
                break;
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverUdpTransport: drainRequestQueue: exit _writeState: " + _writeState);
    }

    private class SipResolverUdpTransportHandler extends SimpleChannelInboundHandler<ByteBuf> {

        public SipResolverUdpTransportHandler() {
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceEntry(this, "channelRead0 message length: " + msg.readableBytes());
            }
            messageRead(ctx, msg.retain());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceEntry(this, "exceptionCaught: " + e.getMessage());
            }
            readError(ctx, e);
            // tell the channel to close; the netty framework will be notified
            ctx.close();
        }

        /** Called when a new connection is established */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceEntry(this, "channelActive local: " + ctx.channel().localAddress() + " remote: "
                        + ctx.channel().remoteAddress());
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceEntry(this, "channelInactive remote disconnected: " + ctx.channel().remoteAddress());
            }
            channel = null;
        }
    }

    private final class SipMessageBufferDatagramDecoder extends MessageToMessageDecoder<DatagramPacket> {

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
            final ByteBuf content = packet.content();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("SipMessageBufferDatagramDecoder decode packet length: " + content.readableBytes());
            }
            out.add(content.retain());
        }
    }

    /**
     * ChannelInitializer for the SIP UDP DNS Resolver
     */
    private class SipResolverUDPInitializer extends ChannelInitializerWrapper {

        ChannelInitializerWrapper parent;

        public SipResolverUDPInitializer(ChannelInitializerWrapper parent) {
            this.parent = parent;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            if (parent != null) {
                parent.init(ch);
            }
            ChannelPipeline pipeline = ch.pipeline();
            // TODO: revisit write and read timeouts
//          pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(WRITE_TIMEOUT, TimeUnit.MILLISECONDS));
//          pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS));
            pipeline.addLast("decoder", new SipMessageBufferDatagramDecoder());
            pipeline.addLast(CHAINNAME, new SipResolverUdpTransportHandler());
        }
    }

    private ChannelFuture writeBufferAsDatagram(ByteBuf requestBuffer) {
        return channel.writeAndFlush(new DatagramPacket(requestBuffer, this._currentSocketAddress,
                (InetSocketAddress) channel.localAddress()));
    }

}
