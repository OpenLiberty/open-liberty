/*******************************************************************************
 * Copyright (c) 2005, 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.tcpchannel.internal.NBAccept.EndPointActionInfo;

/**
 * Non blocking accept selector.
 *
 */
public class NBAcceptChannelSelector extends ChannelSelector implements FFDCSelfIntrospectable {

    /** RAS trace variable */
    private static final TraceComponent tc = Tr.register(NBAcceptChannelSelector.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /** Number of listening ports registered with this selector */
    protected int usageCount = 0;
    /** Selector idle timeout */
    private long selectorTimeout = 0L;
    /** Total number of exceptions in a row during accept sequences */
    protected int numExceptions = 0;
    /** Timestamp of first error */
    private long firstErrorTime = 0L;
    /** Number of exceptions during accept() */
    private int numAcceptIOExceptions = 0;
    /** Number of null sockets back from accept() */
    private int numAcceptNulls = 0;
    /** Number of exceptions configuring a new socket */
    private int numConfigureIOExceptions = 0;
    /** Number of iterations with cancelled key exceptions */
    private int numCancelledKeys = 0;

    private boolean checkStartup = false;

    /**
     * Constructor.
     *
     * @throws IOException
     */
    public NBAcceptChannelSelector(boolean argCheckStartup) throws IOException {
        super(false, argCheckStartup);
        this.checkStartup = argCheckStartup;
        this.selectorTimeout = TCPFactoryConfiguration.getChannelSelectorIdleTimeout();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Created Accept selector: " + this);
        }
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#updateSelector()
     */
    @Override
    protected void updateSelector() {
        boolean trace = (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled());
        if (trace) Tr.entry(this,tc,"updateSelector");
        final Queue<Object> queue = getWorkQueue();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "updateSelector - processing " + queue.size() + " items");
        }
        EndPointActionInfo work = null;

        while (!queue.isEmpty()) {
            work = (EndPointActionInfo) queue.remove();

            if (work.action == NBAccept.REGISTER_ENDPOINT) {
                try {
                    ServerSocket serverSocket = work.endPoint.getServerSocket();

                    // because opening a port during startup is now a two step process, the serverSocket
                    // could have been destroyed before we get here.  Therefore we need to check if it is still valid
                    if (serverSocket == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "REGISTER_ENDPOINT: ServerSocket for this port has already been destroyed");
                        }
                    } else {
                        // Configure all inbound channels to be non-blocking
                        serverSocket.getChannel().configureBlocking(false);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(this, tc, "Registering: " + serverSocket);
                        }
                        // Add a new listener socket in to the selector.
                        serverSocket.getChannel().register(selector, SelectionKey.OP_ACCEPT, work.endPoint);
                        ++usageCount;
                    }
                } catch (Throwable t) {
                    if (com.ibm.wsspi.kernel.service.utils.FrameworkState.isStopping() == false) {
                        FFDCFilter.processException(t, getClass().getName() + ".updateSelector", "101", this, new Object[] { work });
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Error registering port(" + work.endPoint.getListenPort() + "); " + t);
                    }
                } finally {
                    synchronized (work.syncObject) {
                        work.syncObject.notifyAll();
                    }
                }

            } else if (work.action == NBAccept.REMOVE_ENDPOINT) {
                try {
                    ServerSocket serverSocket = work.endPoint.getServerSocket();

                    // because opening a port during startup is now a two step process, the serverSocket
                    // could have been destroyed before we get here.  Therefore we need to check if it is still valid.
                    if (serverSocket == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "REMOVE_ENDPOINT: ServerSocket for this port has already been destroyed");
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(this, tc, "Removing: " + serverSocket);
                        }
                        // cancel the key. Shouldn't need to do this, but there is a bug on HP
                        // so we will just do it regardless of OS
                        serverSocket.getChannel().keyFor(selector).cancel();
                        // force selector to process key cancel before continuing
                        selector.selectNow();
                        --usageCount;
                        if (usageCount <= 0) {
                            shutDown();
                        }
                    }
                } catch (Throwable t) {
                    if (com.ibm.wsspi.kernel.service.utils.FrameworkState.isStopping() == false) {
                        FFDCFilter.processException(t, getClass().getName() + ".updateSelector", "102", this, new Object[] { work });
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Error removing port(" + work.endPoint.getListenPort() + "); " + t);
                    }
                } finally {
                    synchronized (work.syncObject) {
                        work.syncObject.notifyAll();
                    }
                }
            }
        } // process all items on the work queue
        if (trace) Tr.exit(this,tc,"updateSelector");
    }

    /**
     * Query the number of listening ports using this selector.
     *
     * @return int
     */
    protected int getUsageCount() {
        return this.usageCount;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#channelSelectorClose()
     */
    @Override
    protected void channelSelectorClose() {
        try {
            this.selector.close();
        } catch (IOException e) {
            // No FFDC code needed
        }
    }

    /**
     * Increment the number of accept() error conditions that have happened.
     *
     */
    private void incrementExceptions() {
        // if the gap between the first exception and this newest one is over
        // 10 minutes, reset the counter since we're not on a tight loop of
        // errors
        long now = CHFWBundle.getApproxTime();
        if ((now - this.firstErrorTime) >= TEN_MINUTES) {
            this.numExceptions = 1;
            this.firstErrorTime = now;
            // reset the individual exception counters too
            this.numAcceptIOExceptions = 0;
            this.numAcceptNulls = 0;
            this.numCancelledKeys = 0;
            this.numConfigureIOExceptions = 0;
        } else {
            this.numExceptions++;
        }
    }

    /**
     * Reset the looping exception handling variables.
     *
     */
    private void resetExceptions() {
        this.numExceptions = 0;
        this.firstErrorTime = 0L;
        // reset the individual exception counters too
        this.numAcceptIOExceptions = 0;
        this.numAcceptNulls = 0;
        this.numCancelledKeys = 0;
        this.numConfigureIOExceptions = 0;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#performRequest()
     */
    @Override
    protected boolean performRequest() {

        SocketChannel sc = null;
        boolean closeOnError = false;

        // If we were woken up because we have work to do, do it.
        Set<SelectionKey> keySet = this.selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = keySet.iterator();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "performRequest - processing " + keySet.size() + " items");
        }

        while (keyIterator.hasNext()) {
            closeOnError = false;

            // If we get exceptions repeatedly, we may need to take a break for a number of reasons
            if (numExceptions >= 3100) {
                // if we don't recover after 290 more seconds, wait for 10 minutes
                if (pauseAccept()) {
                    resetExceptions(); // clear exceptions if we weren't interrupted
                }
                continue;
            } else if (numExceptions >= 200) {
                // if we get exceptions repeatedly, wait a tenth to give
                // other threads some time. It should take about 10 seconds for
                // us to get up to 200 exceptions (via trace analysis).
                if (numExceptions == 1500) {
                    FFDCFilter.processException(new Exception("TCP channel has received 1500 exceptions in a row on the accept selector"),
                                                getClass().getName(), "101", this);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // ignore it
                }
            }

            // PK40415 - change audit handling of errors
            SelectionKey key = keyIterator.next();
            TCPPort endPoint = (TCPPort) key.attachment();
            TCPChannel tcpChannel = endPoint.getTCPChannel();

            try {
                // safe remove from set while iterating
                keyIterator.remove();

                // perform accept
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

                try {
                    sc = serverSocketChannel.accept();

                    // Configure all inbound channels to be non-blocking
                    if (sc != null) {
                        sc.configureBlocking(false);
                    }

                } catch (IOException ioe) {
                    incrementExceptions();
                    this.numAcceptIOExceptions++;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "TCP Channel: " + tcpChannel.getExternalName() + " caught IOException doing accept: " + ioe + " total=" + numExceptions + " count="
                                           + numAcceptIOExceptions);
                    }
                    // Since we could be monitoring several serverSocketChannels, we need
                    // to continue processing even though we got an IOException on this
                    // serverSocketChannel.
                    continue;
                }

                // sometimes the accept can return null. This seems to happen when
                // there is a network problem, and we got notified it was ready to
                // accept, but when we went to do the accept we couldn't complete the accept. If
                // this happens, just skip this and go on
                if (sc == null) {
                    incrementExceptions();
                    this.numAcceptNulls++;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "TCP Channel: " + tcpChannel.getExternalName() + " accept() returned null, total=" + numExceptions + " count=" + numAcceptNulls);
                    }
                    continue;
                }

                Socket socket = sc.socket();
                if (!testSocket(sc)) {
                    continue;
                }
                closeOnError = true;

                if (!tcpChannel.verifyConnection(socket)) {
                    closeSocketChannel(sc);

                    // if we made it this far, then the accept worked without exceptions
                    if (0 < this.numExceptions) {
                        resetExceptions();
                    }
                    continue;
                }

                SocketIOChannel ioSocket = null;
                try {
                    ioSocket = tcpChannel.createInboundSocketIOChannel(sc);
                } catch (IOException ioe) {
                    // no FFDC required
                    incrementExceptions();
                    this.numConfigureIOExceptions++;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "IOException caught while configuring socket: " + ioe + ", total=" + numExceptions + " count=" + numConfigureIOExceptions);
                    }
                    closeSocketChannel(sc);
                    continue;
                }
                endPoint.processNewConnection(ioSocket);

            } catch (CancelledKeyException cke) {
                // no FFDC required
                // Should only get this if the socket was closed. Therefore
                // log error and continue processing
                incrementExceptions();
                this.numCancelledKeys++;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Cancelled key exception found, cke=" + cke + " total=" + numExceptions + " count=" + numCancelledKeys);
                }
                continue;
            } catch (Throwable t) {
                // don't leak sockets if we can't proceed after the accept
                if (closeOnError) {
                    closeSocketChannel(sc);
                }
                // the catcher of the RTE will do the FFDC
                throw new RuntimeException(t);
            }

            // if we made it this far, then the accept worked without exceptions
            if (0 < this.numExceptions) {
                resetExceptions();
            }
        } // end-while
        return false;
    }

    /**
     * Handle closing the socket channel with appropriate debug and error
     * protection.
     *
     * @param sc
     */
    private void closeSocketChannel(SocketChannel sc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            try {
                Tr.event(this, tc, "Closing connection, local: " + sc.socket().getLocalSocketAddress() + " remote: " + sc.socket().getRemoteSocketAddress());
            } catch (Throwable t) {
                // protect against JDK PK42970 throwing illegal arg
            }
        }

        try {
            sc.close();
        } catch (IOException ioe) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "IOException caught while closing connection " + ioe);
            }
        }
    }

    /**
     * Test the viability of the newly accepted socket. This is to detect the
     * timing window in various JDKs, namely AIX and HPUX, where getLocalPort()
     * or getLocalSocketAddress() may throw a runtime error if the client has
     * already closed the socket prior to these calls.
     *
     * @param sc
     * @return boolean - true means the socket is valid
     */
    private boolean testSocket(SocketChannel sc) {
        // PK61206 - HP JDK will not change their runtime error from getLocal*
        // in the situations where the client closes the socket quickly, so
        // we must protect WAS from that error here
        try {
            sc.socket().getLocalPort();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "SocketChannel accepted, local: " + sc.socket().getLocalSocketAddress() + " remote: " + sc.socket().getRemoteSocketAddress());
            }
        } catch (Throwable t) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Closing invalid socket (getLocalPort failure)");
            }
            try {
                sc.close();
            } catch (Throwable t2) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Error caught while closing connection " + t2);
                }
            }
            if (0 < this.numExceptions) {
                resetExceptions();
            }
            return false;
        }
        return true;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#checkForTimeouts()
     */
    @Override
    protected void checkForTimeouts() {
        this.nextTimeoutTime = this.currentTime + this.selectorTimeout;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#updateCount()
     */
    @Override
    void updateCount() {
        // nothing to do
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#introspectSelf()
     */
    @Override
    public String[] introspectSelf() {
        List<String> rc = new ArrayList<String>();
        rc.add(Thread.currentThread().getName());
        rc.add("selectorTimeout: " + this.selectorTimeout);
        rc.add("usageCount: " + this.usageCount);
        rc.add("quit: " + this.quit);
        rc.add("waitingToQuit: " + this.waitingToQuit);
        rc.add("firstErrorTime: " + this.firstErrorTime + "=" + new Date(this.firstErrorTime));
        rc.add("Total exceptions: " + this.numExceptions);
        rc.add("\tIOExceptions on accept: " + this.numAcceptIOExceptions);
        rc.add("\tnull sockets on accept: " + this.numAcceptNulls);
        rc.add("\tIOExceptions on configure: " + this.numConfigureIOExceptions);
        rc.add("\tnumber of cancelled keys: " + this.numCancelledKeys);

        rc.add("# of keys=" + this.selector.keys().size());
        try {
            for (SelectionKey key : this.selector.keys()) {
                rc.add("key: " + key.channel());
            }
        } catch (Throwable x) {
            // If we get any exception, just return what we have so far.
            // This routine is not thread safe, and could get an exception, but
            // since it is only invoked when we are dumping data, due to a problem or
            // during debug this should be acceptable.
            rc.add("Exception Occurred Gathering Dump Data: " + x);
        }

        return rc.toArray(new String[rc.size()]);
    }

}
