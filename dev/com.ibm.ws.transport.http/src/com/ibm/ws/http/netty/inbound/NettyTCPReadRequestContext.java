/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.inbound;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import io.netty.channel.Channel;

/**
 *
 */
public class NettyTCPReadRequestContext implements TCPReadRequestContext {

    private final NettyTCPConnectionContext connectionContext;
    private final Channel nettyChannel;

    private WsByteBuffer[] buffers;
    private ByteBuffer byteBufferArray[] = null;
    private ByteBuffer byteBufferArrayDirect[] = null;

    // define reusable arrrays of most common sizes
    private ByteBuffer byteBufferArrayOf1[] = null;
    private ByteBuffer byteBufferArrayOf2[] = null;
    private ByteBuffer byteBufferArrayOf3[] = null;
    private ByteBuffer byteBufferArrayOf4[] = null;
    private final WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];

    private final boolean jitAllocateAction = false;
    private int jitAllocateSize = 0;

    private VirtualConnection vc = null;

    public NettyTCPReadRequestContext(NettyTCPConnectionContext connectionContext, Channel nettyChannel) {

        this.connectionContext = connectionContext;
        this.nettyChannel = nettyChannel;

    }

    @Override
    public void clearBuffers() {
        if (this.buffers != null) {
            for (int i = 0; i < this.buffers.length; i++) {
                this.buffers[i].clear();
            }
        }

    }

    @Override
    public TCPConnectionContext getInterface() {
        return this.connectionContext;
    }

    /**
     * Performs reads on the connection until at least the specified number of
     * bytes have been read.
     * This call is always synchronous, and will result in blocking the thread
     * until the
     * minimum number of bytes has been read. A numBytes value of 0 will cause the
     * read to return
     * immediately. Upon completion of the read, WsByteBuffer(s) position will be
     * set
     * to the end of the data. If timeout is set equal to IMMED_TIMEOUT, then an
     * attempt
     * to immediately timeout the previous read will be made, and this read will
     * return 0.
     * 
     * @param numBytes
     *            - minimum number of bytes to read. Max value for numBytes is
     *            2147483647
     * @param timeout
     *            - timeout value to associate with this request (milliseconds)
     * @return long - number of bytes read
     * @throws IOException
     */
    @Override
    public long read(long numBytes, int timeout) throws IOException {
        System.out.println("NETTY TCP SYNC READ - numBytes: "+ numBytes + " timeout: " +timeout);
        
        if (!nettyChannel.isActive()) {
            throw new IOException("Netty channel is not active.");
        }

                
        if (nettyChannel.pipeline().get(NettyServletUpgradeHandler.class) == null) {
            MSP.log("upgradeHandler not present, adding now");
            NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(nettyChannel);

            nettyChannel.pipeline().addLast("ServletUpgradeHandler", upgradeHandler);

        }

        final NettyServletUpgradeHandler upgradeHandler = nettyChannel.pipeline().get(NettyServletUpgradeHandler.class);
        upgradeHandler.setTCPReadContext(this);
        upgradeHandler.setTCPReadContext(this);
        upgradeHandler.setVC(vc);
        
        
        if (upgradeHandler == null) {
            throw new IOException("Upgrade handler not present in pipeline.");
        }

        ExecutorService blockingTaskExecutor = HttpDispatcher.getExecutorService();
        Future<Long> readFuture = blockingTaskExecutor.submit(() -> {
            boolean dataAvailable = upgradeHandler.containsQueuedData() || upgradeHandler.awaitReadReady(numBytes, timeout, TimeUnit.MILLISECONDS);
            if (!dataAvailable || !nettyChannel.isActive()) {
                throw new SocketTimeoutException("Failed to read data within the specified timeout.");
            }

            return upgradeHandler.setToBuffer();
            
            
//            if(bytesRead == 0) {
//                new IOException("MSP -> read failed, no bytes to read.");
//            }
            
            
            
            //return (long) this.getBuffer().limit();
        });

        try {
            // Wait for the read operation to complete or timeout
            return readFuture.get(60000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while reading.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Error occurred during read operation.", cause);
        } catch (TimeoutException e) {
            throw new SocketTimeoutException("Read operation timed out.");
        }
    }

    @Override
    public VirtualConnection read(long numBytes, TCPReadCompletedCallback callback, boolean forceQueue, int timeout) {
        
        //TODO: fix forceQueue
        
        // minBytes = (numBytes<=0) ? 1: numBytes;

        if (!nettyChannel.isActive()) {
            // Channel is not active, do not proceed with the callback
            MSP.log("Netty channel is not active. Skipping callback execution.");
            return vc; // Return
        }

        //Start a new thread that waits to be notified by the handler when enough data is accumulated. On completion, use the callback complete and return null

        if (nettyChannel.pipeline().get(NettyServletUpgradeHandler.class) == null) {
            MSP.log("upgradeHandler not present, adding now");
            NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(nettyChannel);

            nettyChannel.pipeline().addLast("ServletUpgradeHandler", upgradeHandler);

        }

        NettyServletUpgradeHandler upgrade = this.nettyChannel.pipeline().get(NettyServletUpgradeHandler.class);
        

        MSP.log("setting callback for read");

        if (Objects.nonNull(callback)) {
            upgrade.setReadListener(callback);
        }
        upgrade.setTCPReadContext(this);
        upgrade.setVC(vc);
        
        
        

        

        MSP.log("TCP READ REQUEST CONTEXT - Before read: had data? " + upgrade.containsQueuedData());
        MSP.log("TCP READ REQUEST CONTEXT - Before read: data size: " + upgrade.queuedDataSize());
        MSP.log("TCP READ REQUEST CONTEXT - numBytes requested: " + numBytes);
        
        
//        if(upgrade.queuedDataSize()>=minBytes) {
//            upgrade.awaitReadReady(minBytes, timeout, TimeUnit.MILLISECONDS);
//            upgrade.setToBuffer();
//            return vc;
//            
//        } else {
        
//        if (upgrade.containsQueuedData()) {
//            upgrade.awaitReadReady(minBytes, timeout, TimeUnit.MILLISECONDS);
//            upgrade.setToBuffer();
//            return vc;
//        }

        //TODO Change to liberty's executor
        ExecutorService blockingTaskExecutor = HttpDispatcher.getExecutorService();

        blockingTaskExecutor.submit(() -> {
            boolean dataAvailable = upgrade.containsQueuedData() || upgrade.awaitReadReady(numBytes, timeout, TimeUnit.MILLISECONDS);

            if (!nettyChannel.isActive()) {
                // Channel became inactive while waiting for data, skip callback execution
                MSP.log("Netty channel became inactive. Skipping callback execution.");
                return; // Exit the task execution
            }

            if (dataAvailable) {

                upgrade.setToBuffer();
                //TODO: if -1 do infinite
                MSP.log("TCP READ REQUEST - SHOULD HAVE STORED DATA: " + this.getBuffer().limit());

                if (callback != null) {

                    if (!forceQueue) {
                        callback.complete(vc, this);
                    } else {

                        //TODO change to liberty executor, dont use netty for this.

                        blockingTaskExecutor.submit(() -> {
                            try {
                                callback.complete(vc, this);
                            } catch (Exception e) {
                                // Log or handle the exception
                                e.printStackTrace();
                            }
                        });
                    }
                } else {

                    //TODO: !isActive shoudl have its own clause/return
                    MSP.log("CALLBACK IS NULL - NOT SUPPORTED");
                    //throw new IOException ("BETA - unexpected null callback provided");
                }
            } else {
                MSP.log("BETA TIMED OUT");
                StringBuilder error = new StringBuilder();
                error.append("Socket operation timed out before it could be completed local=");
                error.append(connectionContext.getLocalAddress().getHostName()).append("/");
                error.append(connectionContext.getLocalAddress().getHostAddress()).append(":");
                error.append(connectionContext.getLocalPort());
                error.append(" remote=");
                error.append(connectionContext.getRemoteAddress().getHostName()).append("/");
                error.append(connectionContext.getRemoteAddress().getHostAddress()).append(":");
                error.append(connectionContext.getRemotePort());

                //throw new IOException("BETA - Timed out waiting on read");
                HttpDispatcher.getExecutorService().execute(() -> {
                    try {
                        upgrade.getReadListener().error(vc, this, new SocketTimeoutException(error.toString()));
                    } catch (Exception e) {
                        // Log or handle the exception
                        e.printStackTrace();
                    }
                });
            }

        });
   //     }
        
        return null;
    }

    @Override
    public void setJITAllocateSize(int numBytes) {
        this.jitAllocateSize = numBytes;
    }

    @Override
    public boolean getJITAllocateAction() {
        return this.jitAllocateAction;
    }

    @Override
    public WsByteBuffer[] getBuffers() {
        return this.buffers;
    }

    @Override
    public void setBuffers(WsByteBuffer[] bufs) {
        this.buffers = bufs;

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }

        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }

        if (bufs != null) {
            int numBufs;
            // reuse an existing byteBufferArray if one was already created
            // kind of hokey, but this allows us to avoid construction of a
            // new array object unless absolutely neccessary

            // following loop will count the number of buffers in
            // the input array rather than relying on the array length
            for (numBufs = 0; numBufs < bufs.length; numBufs++) {
                if (bufs[numBufs] == null) {
                    break;
                }
            }

//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "setBuffers number of buffers is " + numBufs);
//            }

            if (numBufs == 1) {
                if (this.byteBufferArrayOf1 == null) {
                    this.byteBufferArrayOf1 = new ByteBuffer[1];
                }
                this.byteBufferArray = this.byteBufferArrayOf1;
            } else if (numBufs == 2) {
                if (this.byteBufferArrayOf2 == null) {
                    this.byteBufferArrayOf2 = new ByteBuffer[2];
                }
                this.byteBufferArray = this.byteBufferArrayOf2;
            } else if (numBufs == 3) {
                if (this.byteBufferArrayOf3 == null) {
                    this.byteBufferArrayOf3 = new ByteBuffer[3];
                }
                this.byteBufferArray = this.byteBufferArrayOf3;
            } else if (numBufs == 4) {
                if (this.byteBufferArrayOf4 == null) {
                    this.byteBufferArrayOf4 = new ByteBuffer[4];
                }
                this.byteBufferArray = this.byteBufferArrayOf4;

            } else {
                // more than 4 buffers in request, allocate array as needed
                this.byteBufferArray = new ByteBuffer[numBufs];
            }

            if (numBufs > 1) {
                for (int i = 0; i < numBufs; i++) {
                    this.byteBufferArray[i] = bufs[i].getWrappedByteBufferNonSafe();
                }
            } else if (numBufs == 1) {
                this.byteBufferArray[0] = bufs[0].getWrappedByteBufferNonSafe();
            }

        } else {
            // buffers == null, so set byteBufferArray to null also
            this.byteBufferArray = null;
        }
    }

    @Override
    public WsByteBuffer getBuffer() {
        if (this.buffers == null) {
            return null;
        }
        return this.buffers[0];
    }

    @Override
    public void setBuffer(WsByteBuffer buf) {

        //this.missedSet = false;

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }
        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }
        this.defaultBuffers[0] = null; // reset reference

        if (buf != null) {
            this.buffers = this.defaultBuffers;
            this.buffers[0] = buf;

            if (this.byteBufferArrayOf1 == null) {
                this.byteBufferArrayOf1 = new ByteBuffer[1];
            }
            this.byteBufferArray = this.byteBufferArrayOf1;
            this.byteBufferArray[0] = buf.getWrappedByteBufferNonSafe();
        } else {
            this.buffers = null;
            this.byteBufferArray = null;
        }
    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

}
