/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
import java.util.concurrent.TimeUnit;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.openliberty.netty.internal.*;
import io.openliberty.netty.internal.exception.NettyException;

/**
 * 
 * This class handles the details of using the TCP channel for network
 * transport for the SipResolver
 *
 */
class SipResolverTcpTransport implements SipResolverTransport {
    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipResolverTcpTransport.class);

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 1500;
    private static final int WRITE_TIMEOUT = 60000;
    private static final int MAX_READ_TIMEOUT_COUNT = 5;
    private static final int MAX_WRITE_QUEUE_SIZE = 5000;

    private static final int WRITE_STATE_DISCONNECTED = 0;
    private static final int WRITE_STATE_CONNECTING = 1;
    private static final int WRITE_STATE_IDLE = 2;
    private static final int WRITE_STATE_WRITE_ACTIVE = 3;
    private static final int WRITE_STATE_SHUTDOWN = 4;

    private static final int READ_STATE_READING_LENGTH = 0;
    private static final int READ_STATE_READING_BODY = 1;
    private static final int READ_STATE_DISCONNECTED = 2;
    private static final int READ_STATE_SHUTDOWN = 3;

    private static String CHAINNAME = "SipResolver-tcp-outbound";
    private static NettyFramework _framework;
    private static boolean _channelInitialized = false;
    private static BootstrapExtended bootstrap;

    private boolean _shutdown = false;

    private Vector<InetSocketAddress> _nameServers = null;
    private Iterator<InetSocketAddress> _nameServerIterator = null;
    private Queue<ByteBuf> _requestQueue = new LinkedList<ByteBuf>();
    private ByteBuf[] _bufferArray = new ByteBuf[2];
    private ByteBuf _lengthBuffer;
    private int _outstandingRequestCount = 0;
    private int _readTimeoutCount = 0;
    private Queue<ByteBuf> readBuffers = new LinkedList<ByteBuf>();
    private int bytesRead = 0;
    private int bytesNeeded = 0;

    private boolean reConnectAllowed = false;

    private int _connectionFailedCount = -1;
    private int _transportErrorCount = 0;

    // these allowed thresholds will be re-determined, using the number of available
    // DNS servers, when the object is instantiated
    private int _ConnectFailuresAllowed = 2;
    private int _TransportErrorsAllowed = 3;

    /** Netty channel associated with the resolver connection */
    protected Channel m_channel;

    private SipResolverTransportListener _transportListener = null;
    private InetSocketAddress _currentSocketAddress = null;

    private int _writeState = WRITE_STATE_DISCONNECTED;
    private int _readState = READ_STATE_DISCONNECTED;

    synchronized protected static void initialize(NettyFramework framework) {
        if (_channelInitialized == false) {
            /** Create the chain configuration */
            _framework = framework;
            _channelInitialized = true;
            try {
                Map<String, Object> options = new HashMap<String, Object>();
                options.put(ConfigConstants.EXTERNAL_NAME, CHAINNAME);
                bootstrap = _framework.createTCPBootstrapOutbound(options);
                _channelInitialized = true;
            } catch (NettyException e) {
                if (c_logger.isTraceEntryExitEnabled())
                    c_logger.traceDebug("SipResolverTcpTransport: initialize failed due to: " + e);
            }

        }
    }

    protected SipResolverTcpTransport(Vector<InetSocketAddress> nameServers,
            SipResolverTransportListener transportListener, NettyFramework netty) {

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: constructor: entry: id=" + hashCode());

        /* setup Netty */
        initialize(netty);
        _lengthBuffer = ByteBufAllocator.DEFAULT.buffer(2);
        _nameServers = nameServers;
        _nameServerIterator = _nameServers.iterator();
        _transportListener = transportListener;

        _ConnectFailuresAllowed = _nameServers.size() * 2;
        _TransportErrorsAllowed = _nameServers.size() * 3;

        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(
                    "SipResolverTcpTransport: contructor: _ConnectFailuresAllowed: " + _ConnectFailuresAllowed);
            c_logger.traceDebug(
                    "SipResolverTcpTransport: contructor: _TransportErrorsAllowed: " + _TransportErrorsAllowed);
        }

        // prepare to connect on next write request, but don't connect now since
        // no request has been written yet.
        reConnectAllowed = true;
        _writeState = WRITE_STATE_DISCONNECTED;

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
        bootstrap.handler(new SipResolverTCPInitializer(bootstrap.getBaseInitializer()));

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: constructor: exit");
    }

    synchronized protected void shutdown() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: shutdown: entry: id=" + hashCode());

        _shutdown = true;
        _requestQueue.clear();
        _writeState = WRITE_STATE_SHUTDOWN;
        _readState = READ_STATE_SHUTDOWN;
        if (m_channel != null) {
            try {
                _framework.stop(m_channel).sync();
            } catch (InterruptedException e) {
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: shutdown: exit");
    }

    /**
     * 
     */
    synchronized private void connect() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: connect: entry: id=" + hashCode());

        // move to next name server if last one failed, or we have just been
        // instantiated.
        if (_connectionFailedCount != 0) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: connect: Find DNS Server in list");

            _connectionFailedCount = 0;

            if (_nameServerIterator.hasNext() == false) {
                _nameServerIterator = _nameServers.iterator();
                _currentSocketAddress = _nameServerIterator.next();
            } else
                _currentSocketAddress = _nameServerIterator.next();
        }

        _writeState = WRITE_STATE_CONNECTING;
        _readState = READ_STATE_DISCONNECTED;
        _outstandingRequestCount = 0;
        _readTimeoutCount = 0;

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverTcpTransport:connect: SIP Resolver is connecting to: "
                    + _currentSocketAddress.getHostName() + ":" + _currentSocketAddress.getPort());

        /** open the listener socket */
        try {
            _framework.startOutbound(bootstrap, _currentSocketAddress.getHostString(), _currentSocketAddress.getPort(),
                    f -> {
                        if (f.isCancelled() || !f.isSuccess()) {
                            if (c_logger.isWarnEnabled()) {
                                c_logger.warn("Resolver channel exception during connect: " + f.cause().getMessage());
                            }
                            destroy((Exception) f.cause());
                        } else {
                            m_channel = f.channel();
                            ready();
                        }
                    });
        } catch (NettyException e) {
            e.printStackTrace();
            if (c_logger.isWarnEnabled()) {
                c_logger.warn("Resolver channel exception during connect: " + e.getMessage());
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: connect: exit: id=" + hashCode());
    }

    /**
     * Handle write request based on _writeState.
     */
    synchronized public void writeRequest(ByteBuf requestBuffer) throws IOException {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: writeRequest: entry: id=" + hashCode());

        if (_shutdown == true)
            throw new IllegalStateException("SIP TCP Resolver transport is shutdown.");

        switch (_writeState) {
        case WRITE_STATE_SHUTDOWN:
            // unreachable? debug only
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_SHUTDOWN");
            break;

        case WRITE_STATE_IDLE:

            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_IDLE");

            _lengthBuffer.clear();
            _lengthBuffer.writeShort((short) requestBuffer.readableBytes());

            _bufferArray[0] = _lengthBuffer;
            _bufferArray[1] = requestBuffer;
            ByteBuf newBuf = Unpooled.copiedBuffer(_bufferArray);

            _outstandingRequestCount++;
            ChannelFuture writeFuture = m_channel.writeAndFlush(newBuf, m_channel.newPromise().addListener(f -> {
                if (f.isSuccess()) {
                    writeComplete();
                } else {
                    writeError((Exception) f.cause());
                }
            }));

            if (!writeFuture.isDone()) {
                _writeState = WRITE_STATE_WRITE_ACTIVE;
            }

            break;

        case WRITE_STATE_WRITE_ACTIVE:
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_WRITE_ACTIVE");
            if (_requestQueue.size() > MAX_WRITE_QUEUE_SIZE)
                throw new IOException("Maximum write queue size is being exceeded");
            _requestQueue.add(requestBuffer);
            break;

        case WRITE_STATE_CONNECTING:
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_CONNECTING");
            _requestQueue.add(requestBuffer);
            break;

        case WRITE_STATE_DISCONNECTED:
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport:writeRequest: WRITE_STATE_DISCONNECTED");
            _requestQueue.add(requestBuffer);

            if (reConnectAllowed) {
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverTcpTransport:writeRequest: (re)connect to DNS server");
                connect();
            }

            break;
        }
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: writeRequest: exit");
    }

    /**
     * This method is called when the socket is set up and ready to send and receive
     * data on.
     */
    public void ready() {

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: ready: entry: id=" + hashCode());

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverTcpTransport:ready: socket is ready");

        if (c_logger.isEventEnabled())
            c_logger.info("info.sip.resolver.established.connection", null, _currentSocketAddress.toString());

        reConnectAllowed = false;
        _connectionFailedCount = 0;

        // First, issue a read with a forced callback
        _readState = READ_STATE_READING_LENGTH;

        m_channel.read();

        drainRequestQueue();

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: ready: exit");
    }

    /*
     * This method is called when the socket connection fails during setup.
     * 
     * @see
     * com.ibm.wsspi.channel.ConnectionReadyCallback#ready(com.ibm.wsspi.channel.framework.VirtualConnection)
     */
    public void destroy(Exception e) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: destroy: entry: id=" + hashCode());

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverTcpTransport: Connection failed to establish: " + e);

        if (c_logger.isWarnEnabled())
            c_logger.warn("warn.sip.resolver.failed.connection", null, _currentSocketAddress.toString());

        _connectionFailedCount++;
        _writeState = WRITE_STATE_DISCONNECTED;

        if (_connectionFailedCount <= _ConnectFailuresAllowed) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: calling transportError - _connectionFailedCount: "
                        + _connectionFailedCount);

            // try to rollover to the next name server
            _transportListener.transportError(e, this);
        } else {
            // can't connect to any name serves.
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: calling transportFailed - _connectionFailedCount: "
                        + _connectionFailedCount);

            _transportErrorCount = 0;
            _connectionFailedCount = 0;
            _transportListener.transportFailed(e, this);
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: destroy: exit");
    }

    /**
     * Take action on read depending on _readState.
     */
    public void readComplete(ChannelHandlerContext ctx, ByteBuf msg) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: complete: entry: id=" + hashCode());

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverTcpTransport: complete: _readState: " + _readState);

        _readTimeoutCount = 0; // Set this back to 0 so no errors are detected.

        _connectionFailedCount = 0;
        _transportErrorCount = 0;

        boolean exit = false;
        while (exit == false) {
            switch (_readState) {
            case READ_STATE_READING_LENGTH:
                msg.resetReaderIndex();
                short length = msg.readShort();
                _readState = READ_STATE_READING_BODY;
                readBuffers.clear();
                bytesRead = 0;
                bytesNeeded = length;
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverTcpTransport: complete: doing read length of: " + length);
                break;

            case READ_STATE_READING_BODY:
                if (bytesNeeded > bytesRead) {
                    bytesRead += msg.readableBytes();
                    readBuffers.add(msg);
                }
                if (bytesNeeded == bytesRead) {
                    if (_outstandingRequestCount != 0)
                        _outstandingRequestCount--;
                    else {
                        if (c_logger.isTraceDebugEnabled())
                            c_logger.traceDebug(
                                    "SipResolverTcpTransport: complete: error: outstandingRequestCount can't decrement past 0");
                    }
                    _transportListener.responseReceived(
                            Unpooled.wrappedBuffer(readBuffers.toArray(new ByteBuf[readBuffers.size()])));
                } else {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug(
                                "SipResolverTcpTransport: READ_STATE_READING_BODY need more bytes: needed:read = " + bytesNeeded + ":" + bytesRead);
                    exit = true;
                    break;
                }
                _readState = READ_STATE_READING_LENGTH;
                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverTcpTransport: complete: doing new read for length");
                exit = true;
                break;

            case READ_STATE_DISCONNECTED:
            case READ_STATE_SHUTDOWN:
                exit = true;
                break;
            }
        }
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: complete: exit: id=" + hashCode());
    }

    /**
     * Called from exceptionCaught() in order to log more details and to 
     * clean up read and write states.
     */
    public void readError(Exception e) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: error(vc, read context, exception) ");
        if (_shutdown == false) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: read error: exception " + e);
            if ((e instanceof java.net.SocketTimeoutException)
                    || (e instanceof com.ibm.io.async.AsyncTimeoutException)) {
                if ((_outstandingRequestCount > 0) || (_readTimeoutCount > MAX_READ_TIMEOUT_COUNT)
                        || (_readState == READ_STATE_READING_BODY)) {
                    if (c_logger.isWarnEnabled() && (_outstandingRequestCount > 0))
                        c_logger.warn("warn.sip.resolver.server.not.responding", null,
                                _currentSocketAddress.toString());

                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug(
                                "SipResolverTcpTransport: error: consecutive read timeouts: " + _readTimeoutCount);

                    _transportErrorCount++;

                    /** try the next nameserver on the connect if these aren't idle timeouts */
                    if ((_outstandingRequestCount > 0) || (_readState == READ_STATE_READING_BODY)) {
                        _connectionFailedCount++;
                    }

                    _readState = READ_STATE_DISCONNECTED;
                    _writeState = WRITE_STATE_DISCONNECTED;

                    if (_transportErrorCount < _TransportErrorsAllowed) {
                        if (c_logger.isTraceDebugEnabled())
                            c_logger.traceDebug(
                                    "SipResolverTcpTransport: error: calling transportError - _transprtErrorCount: "
                                            + _transportErrorCount);

                        _transportListener.transportError(e, this);

                    } else {
                        if (c_logger.isTraceDebugEnabled())
                            c_logger.traceDebug(
                                    "SipResolverTcpTransport: error: calling transportFailed - _transprtErrorCount: "
                                            + _transportErrorCount);

                        _transportErrorCount = 0;
                        _connectionFailedCount = 0;
                        _transportListener.transportFailed(e, this);
                    }

                } else {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug(
                                "SipResolverTcpTransport: error: incrementing readTimeoutCount: " + _readTimeoutCount);

                    _readTimeoutCount++;

                    // Reissue the read and keep going
                    m_channel.read();
                }
            } else {
                _readState = READ_STATE_DISCONNECTED;
                _writeState = WRITE_STATE_DISCONNECTED;

                _transportErrorCount++;

                // increment connection failed count to force rollover.
                _connectionFailedCount++;

                if (_transportErrorCount < _TransportErrorsAllowed) {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug(
                                "SipResolverTcpTransport: error: calling transportError - _transprtErrorCount: "
                                        + _transportErrorCount);

                    _transportListener.transportError(e, this);

                } else {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug(
                                "SipResolverTcpTransport: error: calling transportFailed - _transprtErrorCount: "
                                        + _transportErrorCount);

                    _transportErrorCount = 0;
                    _connectionFailedCount = 0;
                    _transportListener.transportFailed(e, this);
                }
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: error(vc, read context, exception)");
    }

    /**
     * Clear the request queue of all outstanding requests, 
     * then allow reconnects.
     */
    public void prepareForReConnect() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: prepareForReConnect");

        // We can only clear when the object using us tell us to clear,
        // otherwise we risk timing windows whereby we clear out requests
        // which were meant for rollover, or we fail to clear out an attempt
        // that will be retried..
        _requestQueue.clear();

        // only allow reconnects once we know the request queue will only hold
        // new valid attempts.
        reConnectAllowed = true;

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: prepareForReConnect");
    }

    /**
     * Complete the write by clearing failed/error counts and draining
     * the request queue.
     */
    public void writeComplete() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: complete: write complete id=" + hashCode());

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverTcpTransport: complete: write completed sucessfully: " + hashCode());

        _connectionFailedCount = 0;
        _transportErrorCount = 0;
        drainRequestQueue();

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: complete: write complete id=" + hashCode());
    }

    /**
     * Handle error in write.
     */
    public void writeError(Exception e) {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: error: write error id=" + hashCode());
        e.printStackTrace();
        if (_shutdown == true)
            return;

        _transportErrorCount++;

        // increment connection failed count to force rollover.
        _connectionFailedCount++;

        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipResolverTcpTransport: error: write failed: " + hashCode());

        _writeState = WRITE_STATE_DISCONNECTED;

        if (_transportErrorCount < _TransportErrorsAllowed) {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: error: calling transportError - _transprtErrorCount: "
                        + _transportErrorCount);

            _transportListener.transportError(e, this);

        } else {
            if (c_logger.isTraceDebugEnabled())
                c_logger.traceDebug("SipResolverTcpTransport: error: calling transportFailed - _transprtErrorCount: "
                        + _transportErrorCount);

            _transportErrorCount = 0;
            _connectionFailedCount = 0;
            _transportListener.transportFailed(e, this);
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: error: write error id=" + hashCode());
    }

    /**
     * Handle requests in queue
     */
    synchronized private void drainRequestQueue() {
        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipResolverTcpTransport: drainRequestQueue: entry: id=" + hashCode());

        while (true) {
            ByteBuf requestBuffer = _requestQueue.poll();

            if (requestBuffer != null) {
                _lengthBuffer.clear();
                _lengthBuffer.retain();

                _lengthBuffer.writeShort((short) requestBuffer.readableBytes());

                _bufferArray[0] = _lengthBuffer;
                _bufferArray[1] = requestBuffer;
                ByteBuf newBuf = Unpooled.copiedBuffer(_bufferArray);

                if (c_logger.isTraceDebugEnabled())
                    c_logger.traceDebug("SipResolverTcpTransport:drainRequestQueue: writing new message, length = "
                            + requestBuffer.readableBytes());

                _outstandingRequestCount++;
                m_channel.write(_bufferArray[0]);
                ChannelFuture writeFuture = m_channel.writeAndFlush(_bufferArray[1],
                        m_channel.newPromise().addListener(f -> {
                            if (f.isSuccess()) {
                                writeComplete();
                            } else {
                                writeError((Exception) f.cause());
                            }
                        }));

                if (!writeFuture.isDone()) {
                    if (c_logger.isTraceDebugEnabled())
                        c_logger.traceDebug("SipResolverTcpTransport:drainRequestQueue: waiting for write to complete");

                    _writeState = WRITE_STATE_WRITE_ACTIVE;
                    break;
                }
            } else {
                _writeState = WRITE_STATE_IDLE;
                break;
            }
        }

        if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceExit(this, "SipResolverTcpTransport: drainRequestQueue: exit");
    }

    private class SipResolverTcpTransportHandler extends SimpleChannelInboundHandler<ByteBuf> {

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
            m_channel = null;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceEntry(this, "exceptionCaught: " + e.getMessage());
            }
            readError((Exception) e);
            // tell the channel to close; the netty framework will be notified
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceEntry(this, "channelRead0 message length: " + msg.readableBytes());
            }
            ByteBuf b = (ByteBuf) msg;
            readComplete(ctx, b.retain());
        }
    }

    /**
     * ChannelInitializer for the SIP TCP DNS Resolver
     */
    private class SipResolverTCPInitializer extends ChannelInitializerWrapper {

        ChannelInitializerWrapper parent;

        public SipResolverTCPInitializer(ChannelInitializerWrapper parent) {
            this.parent = parent;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            if (parent != null) {
                parent.init(ch);
            }
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(WRITE_TIMEOUT, TimeUnit.MILLISECONDS));
            pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS));
            pipeline.addLast(CHAINNAME, new SipResolverTcpTransportHandler());
        }
    }

}
