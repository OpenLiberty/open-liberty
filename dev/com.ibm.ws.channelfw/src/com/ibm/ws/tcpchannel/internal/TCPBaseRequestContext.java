/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bytebuffer.internal.WsByteBufferImpl;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 * Base class implementing the common parts of
 * TCPReadRequestContext and TCPWriteRequestContext
 */
public class TCPBaseRequestContext implements TCPRequestContext, FFDCSelfIntrospectable {

    // the type of request object true = read, false = write
    private boolean requestTypeRead;

    private boolean aborted = false;

    // objects used for blocking calls
    protected boolean blockedThread = false;
    protected SimpleSync blockWait = null;
    protected IOException blockingIOError = null;

    // Reference to array of byte buffers
    private WsByteBuffer[] buffers;

    private boolean forceQueue = false;

    private long ioAmount;
    private long ioDoneAmt = 0;
    private long lastIOAmt = 0;
    private long ioCompleteAmt = 0;

    // max read/write size limit to the max "int" size, even though we define
    // the size variable to be "long". This saves needless complexity on some
    // functions which are bounded by "int"s.
    protected static final long maxReadSize = Integer.MAX_VALUE;
    protected static final long maxWriteSize = Integer.MAX_VALUE;

    protected TCPConnLink oTCPConnLink;
    protected TCPChannelConfiguration config;
    protected long timeoutTime;
    private int timeoutInterval;
    private WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];
    private ByteBuffer byteBufferArray[] = null;
    private ByteBuffer byteBufferArrayDirect[] = null;
    private boolean missedSet = false;

    // define reusable arrrays of most common sizes
    private ByteBuffer byteBufferArrayOf1[] = null;
    private ByteBuffer byteBufferArrayOf2[] = null;
    private ByteBuffer byteBufferArrayOf3[] = null;
    private ByteBuffer byteBufferArrayOf4[] = null;
    private ByteBuffer byteBufferArrayOf1Direct[] = null;
    private ByteBuffer byteBufferArrayOf2Direct[] = null;
    private ByteBuffer byteBufferArrayOf3Direct[] = null;
    private ByteBuffer byteBufferArrayOf4Direct[] = null;

    private static final TraceComponent tc = Tr.register(TCPBaseRequestContext.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     * 
     * @param link
     */
    protected TCPBaseRequestContext(TCPConnLink link) {
        this.oTCPConnLink = link;
        this.config = link.getConfig();
    }

    /**
     * Abort this context to trigger immediate exceptions on future IO requests.
     */
    protected void abort() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Aborting connection");
        }
        this.aborted = true;
    }

    /**
     * Check whether this context has been aborted previously or not.
     * 
     * @return boolean
     */
    protected boolean isAborted() {
        return this.aborted;
    }

    /**
     * Access the connection link for this context object.
     * 
     * @return TCPConnLink
     */
    public TCPConnLink getTCPConnLink() {
        return this.oTCPConnLink;
    }

    protected TCPChannelConfiguration getConfig() {
        return this.config;
    }

    /**
     * @see TCPRequestContext#getInterface()
     */
    public TCPConnectionContext getInterface() {
        return this.oTCPConnLink;
    }

    /* =================================================================== */
    /* PUBLIC FUNCTIONS (that implement methods in TCPConnectionContext) */
    /* =================================================================== */

    /** @see TCPRequestContext#getBuffers() */
    public WsByteBuffer[] getBuffers() {
        return this.buffers;
    }

    /** @see TCPRequestContext#setBuffers(WsByteBuffer[]) */
    public void setBuffers(WsByteBuffer[] bufs) {
        this.missedSet = false;
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

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setBuffers number of buffers is " + numBufs);
            }

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
                if ((bufs[0].getStatus() & WsByteBuffer.STATUS_TRANSFER_TO) == 0) {
                    this.byteBufferArray[0] = bufs[0].getWrappedByteBufferNonSafe();
                } else {
                    // can't do getWrappedByteBufferNonSafe in TRANSFER_TO mode
                    // or else it will revert the FCWsByteBuffer into BUFFER mode

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "setBuffers has FC buffer");

                    this.missedSet = true;
                }
            }

        } else {
            // buffers == null, so set byteBufferArray to null also
            this.byteBufferArray = null;
        }
    }

    protected void setBuffersToDirect(WsByteBuffer[] bufs) {
        this.buffers = bufs;

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

            if (numBufs == 1) {
                if (this.byteBufferArrayOf1Direct == null) {
                    this.byteBufferArrayOf1Direct = new ByteBuffer[1];
                }
                this.byteBufferArrayDirect = this.byteBufferArrayOf1Direct;
            } else if (numBufs == 2) {
                if (this.byteBufferArrayOf2Direct == null) {
                    this.byteBufferArrayOf2Direct = new ByteBuffer[2];
                }
                this.byteBufferArrayDirect = this.byteBufferArrayOf2Direct;
            } else if (numBufs == 3) {
                if (this.byteBufferArrayOf3Direct == null) {
                    this.byteBufferArrayOf3Direct = new ByteBuffer[3];
                }
                this.byteBufferArrayDirect = this.byteBufferArrayOf3;
            } else if (numBufs == 4) {
                if (this.byteBufferArrayOf4Direct == null) {
                    this.byteBufferArrayOf4Direct = new ByteBuffer[4];
                }
                this.byteBufferArrayDirect = this.byteBufferArrayOf4;

            } else {
                // more than 4 buffers in request, allocate array as needed
                this.byteBufferArrayDirect = new ByteBuffer[numBufs];
            }
            for (int i = 0; i < numBufs; i++) {
                this.byteBufferArrayDirect[i] = ((WsByteBufferImpl) bufs[i]).oWsBBDirect;
            }
        } else {
            this.byteBufferArrayDirect = null;
        }
    }

    /**
     * @return WsByteBuffer
     */
    public WsByteBuffer getBuffer() {
        if (this.buffers == null) {
            return null;
        }
        return this.buffers[0];
    }

    /**
     * @param buf
     */
    public void setBuffer(WsByteBuffer buf) {

        this.missedSet = false;

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

            if ((buf.getStatus() & WsByteBuffer.STATUS_TRANSFER_TO) == 0) {
                if (this.byteBufferArrayOf1 == null) {
                    this.byteBufferArrayOf1 = new ByteBuffer[1];
                }
                this.byteBufferArray = this.byteBufferArrayOf1;
                this.byteBufferArray[0] = buf.getWrappedByteBufferNonSafe();
            } else {
                // can't do getWrappedByteBufferNonSafe in TRANSFER_TO mode
                // or else it will revert the FCWsByteBuffer into BUFFER mode
                this.missedSet = true;
            }
        } else {
            this.buffers = null;
            this.byteBufferArray = null;
        }

    }

    /**
     * Clear any buffers that are currently stored.
     */
    public void clearBuffers() {
        if (null != this.buffers) {
            for (int i = 0; i < this.buffers.length; i++) {
                this.buffers[i].clear();
            }
        }
    }

    protected void setForceQueue(boolean forceQue) {
        this.forceQueue = forceQue;
    }

    protected boolean isForceQueue() {
        return this.forceQueue;
    }

    /**
     * @return long
     */
    public long getIOAmount() {
        return this.ioAmount;
    }

    /**
     * @return long
     */
    public long getIODoneAmount() {
        return this.ioDoneAmt;
    }

    protected long getLastIOAmt() {
        return this.lastIOAmt;
    }

    protected void setLastIOAmt(long bytes) {
        this.lastIOAmt = bytes;
    }

    protected void setIOAmount(long b) {
        this.ioAmount = b;
    }

    protected void setIODoneAmount(long b) {
        this.ioDoneAmt = b;
    }

    protected void setIOCompleteAmount(long b) {
        this.ioCompleteAmt = b;
    }

    protected long getIOCompleteAmount() {
        return this.ioCompleteAmt;
    }

    /**
     * Returns the time (measured since Jan 1st 1970 in milliseconds - so
     * comparable format to System.currentTimeMillis) when this request
     * will time out. Should only be invoked if the 'hasTimeout' method
     * returns true for this request.
     * 
     * @return long Point in time when request will timeout.
     */
    public long getTimeoutTime() {
        return this.timeoutTime;
    }

    /**
     * Sets the timeout value returned by 'getTimeoutTime'.
     * 
     * @param time
     */
    protected void setTimeoutTime(int time) {
        int timeout = time;
        if (timeout == TCPRequestContext.NO_TIMEOUT) {
            this.timeoutTime = TCPRequestContext.NO_TIMEOUT;
            this.timeoutInterval = 0;
        } else {
            if (timeout == TCPRequestContext.USE_CHANNEL_TIMEOUT) {
                timeout = getConfig().getInactivityTimeout();
            }
            if (timeout != ValidateUtils.INACTIVITY_TIMEOUT_NO_TIMEOUT) {
                this.timeoutTime = System.currentTimeMillis() + timeout;
                this.timeoutInterval = timeout;
            } else {
                this.timeoutTime = TCPRequestContext.NO_TIMEOUT;
                this.timeoutInterval = 0;
            }
        }
    }

    /**
     * Returns true if this request has an associated timeout.
     * 
     * @return boolean
     */
    protected boolean hasTimeout() {
        return this.timeoutTime != TCPRequestContext.NO_TIMEOUT;
    }

    /**
     * Returns timoutInterval of this request.
     * 
     * @return int
     */
    public int getTimeoutInterval() {
        return this.timeoutInterval;
    }

    /**
     * Introspect this object for FFDC output.
     * 
     * @return List<String>
     */
    public List<String> introspect() {
        List<String> rc = new LinkedList<String>();
        String prefix = getClass().getSimpleName() + "@" + hashCode() + ": ";
        rc.add(prefix + "aborted=" + this.aborted);
        rc.add(prefix + "forceQueue=" + this.forceQueue);
        rc.add(prefix + "ioAmount=" + this.ioAmount);
        rc.add(prefix + "ioCompleteAmt=" + this.ioCompleteAmt);
        rc.add(prefix + "ioDoneAmt=" + this.ioDoneAmt);
        rc.add(prefix + "lastIOAmt=" + this.lastIOAmt);
        rc.add(prefix + "isRead=" + this.requestTypeRead);
        rc.add(prefix + "timeoutInterval=" + this.timeoutInterval);
        rc.add(prefix + "timeoutTime=" + this.timeoutTime);
        rc.add(prefix + "link=" + this.oTCPConnLink);
        if (null != this.oTCPConnLink) {
            rc.addAll(this.oTCPConnLink.introspect());
        }
        return rc;
    }

    /*
     * @see com.ibm.ws.ffdc.FFDCSelfIntrospectable#introspectSelf()
     */
    public String[] introspectSelf() {
        List<String> rc = introspect();
        return rc.toArray(new String[rc.size()]);
    }

    /**
     * @return boolean
     */
    protected boolean isRequestTypeRead() {
        return this.requestTypeRead;
    }

    /**
     * @param b
     */
    protected void setRequestTypeRead(boolean b) {
        this.requestTypeRead = b;
    }

    /**
     * @return ByteBuffer[]
     */
    protected ByteBuffer[] getByteBufferArray() {
        if (!this.missedSet) {
            return this.byteBufferArray;
        }
        // set bytebufferarray of 1
        if (this.byteBufferArrayOf1 == null) {
            this.byteBufferArrayOf1 = new ByteBuffer[1];
        }
        this.byteBufferArray = this.byteBufferArrayOf1;
        this.byteBufferArray[0] = this.buffers[0].getWrappedByteBufferNonSafe();
        this.missedSet = false;
        return this.byteBufferArray;
    }

    /**
     * @return ByteBuffer[]
     */
    protected ByteBuffer[] getByteBufferArrayDirect() {
        return this.byteBufferArrayDirect;
    }

    /**
     * Updates the IO byte counters, checks if IO operation is complete
     * 
     * @param byteCount
     *            number of IO bytes for latest read/write
     * @param type
     *            0 for read. 1 for write
     * @return true if IO operation has read/wrote enough bytes, false if not
     */
    // IMPROVEMENT: this shouldn't be public, but needs to be for now.
    // Should be called by base TCP code after IO is done,
    // rather than in extension classes
    public boolean updateIOCounts(long byteCount, int type) {

        setLastIOAmt(byteCount);
        setIODoneAmount(getIODoneAmount() + byteCount);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            String dbString = null;
            if (type == 0) {
                dbString = "Read ";
            } else {
                dbString = "Wrote ";
            }
            SocketIOChannel channel = getTCPConnLink().getSocketIOChannel();
            Tr.event(tc, dbString + byteCount + "(" + +getIODoneAmount() + ")" + " bytes, " + getIOAmount() + " requested on local: " + channel.getSocket().getLocalSocketAddress()
                         + " remote: " + channel.getSocket().getRemoteSocketAddress());
        }

        boolean rc;
        if (getIODoneAmount() >= getIOAmount()) {
            // read is complete on current thread
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (type == 0) {
                    Tr.debug(tc, "read complete, at least minimum amount of data read");
                } else {
                    Tr.debug(tc, "write complete, at least minimum amount of data written");
                }
            }
            rc = true;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (type == 0) {
                    Tr.debug(tc, "read not complete, more data needed");
                } else {
                    Tr.debug(tc, "write not complete, more data needs to be written");
                }
            }
            rc = false;
        }
        return rc;
    }

}
