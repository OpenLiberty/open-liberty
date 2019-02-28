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
package com.ibm.ws.bytebuffer.internal;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

// note: only methods that get or change data associated with a ByteBuffer
//       need to call checkValidity.  Others could be a problem as well,
//       but are probably not worth the performance overhead.
/**
 * Basic WAS implementation of a ByteBuffer wrapper.
 */
public class WsByteBufferImpl implements WsByteBuffer, Externalizable {

    private static final long serialVersionUID = 3794216509672320032L;
    private static final TraceComponent tc = Tr.register(WsByteBufferImpl.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    /** Actual byte buffer wrapped by this class */
    protected ByteBuffer oByteBuffer = null;
    /** Optional backing direct buffer if this wraps an indirect buffer */
    public ByteBuffer oWsBBDirect = null;

    protected final AtomicBoolean booleanReleaseCalled = new AtomicBoolean(false); // LIDB1891-78.1C
    private WsByteBufferPoolManagerImpl oWsByteBufferPoolManager = null;

    private boolean isDirectPool = false;
    private boolean readOnly = false;
    private boolean trusted = false;
    private boolean removedFromLeakDetection = false;

    private String ownerID = null;
    private long lastAccessTime = 0;

    protected PooledWsByteBufferImpl wsBBRoot = null;

    // RefCount only used when we allocate a direct ByteBuffer using a DLL and not Java
    // (aka on z/OS). This instance needs to be reference counted before we delete the
    // backing memory
    protected RefCountWsByteBufferImpl wsBBRefRoot = null;

    // this is a fast check, to see if sync logic needs to be used to fully check the
    // bufferAction above. The assumption made about checking the buffer action is that
    // an algorithm will only be "ACTIVATED" (my calling setBufferAction(...))
    // when the calling user code
    // knows that no other thread is using the buffer. This allows us to avoid mainline
    // sync logic when checking this variable. The timing windows created when this
    // values goes from "ACTIVATED" to "NOT_ACTIVATED" when multi-threads are being
    // used is ok, since we do a sync check if we see that the state is ACTIVATED.
    protected static final int NOT_ACTIVATED = 0;
    protected static final int ACTIVATED = 1;
    protected int quickBufferAction = NOT_ACTIVATED;

    // min/max are inclusive, so threshold will be this number + 1
    private int GET_THRESHOLD = 1023; // minimum amount to copy from Direct to NonDirect
    private int PUT_THRESHOLD = 2047; // maximum number of bytes between non-contiguous puts that will be bridged.

    private int status = WsByteBuffer.STATUS_BUFFER;

    /**
     * Constructor.
     */
    public WsByteBufferImpl() {
        // if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
        // Tr.event(tc, "Created " + this);
        // }
    }

    /**
     * Query the root buffer of this one, may or may not exist
     * depending on if this instance is a duplicate or a slice.
     * 
     * @return PooledWsByteBufferImpl
     */
    public PooledWsByteBufferImpl getWsBBRoot() {
        return this.wsBBRoot;
    }

    /**
     * Store the parent pooled buffer that spawned this one.
     * 
     * @param wsbb
     */
    public void setWsBBRoot(PooledWsByteBufferImpl wsbb) {
        this.wsBBRoot = wsbb;
    }

    /**
     * Query the RefCount buffer owner of this one, which may or may
     * not exist depending on if this instance was a duplicate or a
     * slice.
     * 
     * @return RefCountWsByteBufferImpl
     */
    public RefCountWsByteBufferImpl getWsBBRefRoot() {
        return this.wsBBRefRoot;
    }

    /**
     * Store the parent refcount buffer that spawned this one.
     * 
     * @param wsbb
     */
    public void setWsBBRefRoot(RefCountWsByteBufferImpl wsbb) {
        this.wsBBRefRoot = wsbb;
    }

    public byte[] array() {
        if (!this.trusted)
            checkValidity();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "array(): " + (null != this.oByteBuffer.array()));
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.array();
    }

    public int arrayOffset() {
        if (!this.trusted)
            checkValidity();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "arrayOffset(): " + this.oByteBuffer.arrayOffset());
        }
        return this.oByteBuffer.arrayOffset();
    }

    public WsByteBuffer compact() {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "compact(): before oByteBuffer.compact()", this);
        }

        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }

        this.oByteBuffer.compact();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "compact(): after oByteBuffer.compact()", this);
        }
        return this;
    }

    /*
     * @see com.ibm.wsspi.bytebuffer.WsByteBuffer#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Object obj) {
        if (!this.trusted)
            checkValidity();
        final ByteBuffer other = ((WsByteBuffer) obj).getWrappedByteBufferNonSafe();
        return this.oByteBuffer.compareTo(other);
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        // native bytebuffer comparisons look at content, so two empty buffers
        // are the same, which really messes up List or Map usage of buffers
        return (this == o);
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public char getChar() {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getChar();
    }

    public char getChar(int index) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getChar(index);
    }

    public WsByteBuffer putChar(char value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putChar(value);
        return this;
    }

    public WsByteBuffer putChar(int index, char value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putChar(index, value);
        return this;
    }

    public WsByteBuffer putChar(char[] values) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return putChar(values, 0, values.length);
    }

    public WsByteBuffer putChar(char[] values, int off, int len) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        if ((off + len) > values.length) {
            throw new IllegalArgumentException("Invalid Parameters: " +
                                               "off=" + off + ", len=" + len + ", values.length=" + values.length);
        }
        for (int i = off; i < off + len; i++) {
            this.oByteBuffer.putChar(values[i]);
        }
        return this;
    }

    public double getDouble() {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getDouble();
    }

    public double getDouble(int index) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getDouble(index);
    }

    public WsByteBuffer putDouble(double value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putDouble(value);
        return this;
    }

    public WsByteBuffer putDouble(int index, double value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putDouble(index, value);
        return this;
    }

    public float getFloat() {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getFloat();
    }

    public float getFloat(int index) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getFloat(index);
    }

    public WsByteBuffer putFloat(float value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putFloat(value);
        return this;
    }

    public WsByteBuffer putFloat(int index, float value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putFloat(index, value);
        return this;
    }

    public int getInt() {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getInt();
    }

    public int getInt(int index) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getInt(index);
    }

    public WsByteBuffer putInt(int value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putInt(value);
        return this;
    }

    public WsByteBuffer putInt(int index, int value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putInt(index, value);
        return this;
    }

    public long getLong() {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getLong();
    }

    public long getLong(int index) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getLong(index);
    }

    public WsByteBuffer putLong(long value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putLong(value);
        return this;
    }

    public WsByteBuffer putLong(int index, long value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putLong(index, value);
        return this;
    }

    public short getShort() {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getShort();
    }

    public short getShort(int index) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.getShort(index);
    }

    public WsByteBuffer putShort(short value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putShort(value);
        return this;
    }

    public WsByteBuffer putShort(int index, short value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.putShort(index, value);
        return this;
    }

    public WsByteBuffer putString(String value) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.put(value.getBytes());
        return this;
    }

    public boolean hasArray() {
        // checkValidity();
        return this.oByteBuffer.hasArray();
    }

    public ByteOrder order() {
        // checkValidity();
        return this.oByteBuffer.order();
    }

    public WsByteBuffer order(ByteOrder bo) {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.order(bo);
        return this;
    }

    public WsByteBuffer clear() {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.clear();
        return this;
    }

    public int capacity() {
        // checkValidity();
        return (this.oByteBuffer.capacity());
    }

    public WsByteBuffer flip() {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.flip();
        return this;
    }

    private void copyRangeToDirectBuffer(int min, int max) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "copyRangeToDirectBuffer(min,max) " + min + "," + max);
        }

        int directPosition = this.oWsBBDirect.position();
        int directLimit = this.oWsBBDirect.limit();

        // set the position and limit for copying
        // the limit may have narrowed, but
        // we need to move all the data down that we have.
        this.oWsBBDirect.limit(max + 1);
        this.oWsBBDirect.position(min);

        // copy the bytes from min to max, inclusive
        this.oWsBBDirect.put(this.oByteBuffer.array(),
                             min + this.oByteBuffer.arrayOffset(),
                             max - min + 1);

        // restore the position and limit
        this.oWsBBDirect.limit(directLimit);
        this.oWsBBDirect.position(directPosition);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "copyRangeToDirectBuffer");
        }
    }

    public boolean setBufferAction(int newAction) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setBufferAction(newAction): " + newAction);
        }

        boolean returnValue = false;
        if (this.wsBBRoot == null) {
            // buffer did not come from a pool, but we need a root entry to be able
            // to use the common buffer action variables, so create
            // a pooled entry object here.
            this.wsBBRoot = new PooledWsByteBufferImpl();

            // NOTE: The default value for "wsBBRoot.pool" is null, and null will now
            // denote that this WsByteBufferImpl, though it has a root, is not
            // from a pool nor does it "extend" a WsByteBuferImpl that is being used,
            // or was initialized properly.
        }

        synchronized (this.wsBBRoot.actionAccess) {
            if (newAction == BUFFER_MGMT_COPY_WHEN_NEEDED) {
                // Can only invoke the BUFFER_MGMT_COPY_WHEN_NEEDED algorithm on an
                // initialized nonDirect buffer.
                // !! To avoid needless sync logic through-out this class, it is
                // assume that if the caller is changing the buffer action to
                // BUFFER_MGMT_COPY_WHEN_NEEDED, then this is the only thread accessing
                // this buffer and no other duplicate/slices exist or are being accessed
                // at this time.
                if ((!oByteBuffer.isDirect())
                    && (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_ALL_INIT)) {
                    this.wsBBRoot.actionState = PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1;
                    this.quickBufferAction = ACTIVATED;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "actionState set to COPY_WHEN_NEEDED_STATE1: " + wsBBRoot.actionState);
                    }

                    // set up a direct byte buffer to be shared by dupes/slices
                    if (this.oWsBBDirect == null) {
                        this.oWsBBDirect = ByteBuffer.allocateDirect(oByteBuffer.capacity());
                    }

                    returnValue = true;
                }
            }

            // Change the copy algorithm to the old "all" way, and
            // it can no longer change to a different algorithm
            else if (newAction == BUFFER_MGMT_COPY_ALL_FINAL) {

                if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                    || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                    // update nonDirect Buffer with any read data it doesn't have.
                    // if it is in STATE1 then all read data will be moved up to the
                    // nonDirect buffer.
                    // if it is in STATE2 then only the read data that was not replaced
                    // by "put" data will be moved up to the nonDirect buffer.
                    if ((wsBBRoot.readMin != -1) && (wsBBRoot.readMax != -1)) {
                        moveUpUsingGetMinMax(wsBBRoot.readMin, wsBBRoot.readMax, 0);
                    }
                }

                this.wsBBRoot.actionState = PooledWsByteBufferImpl.COPY_ALL_FINAL;
                this.quickBufferAction = NOT_ACTIVATED;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "actionState set to COPY_ALL_FINAL: " + this.wsBBRoot.actionState);
                }
                returnValue = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setBufferAction returning: " + returnValue);
        }

        return returnValue;
    }

    private void moveDownUsingPutMinMax(int newPutMin, int newPutMax) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "moveDownUsingPutMinMax(min,max) " + newPutMin + "," + newPutMax);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "current putMin and putMax are: " + wsBBRoot.putMin + "," + wsBBRoot.putMax);
        }

        // routine should only be called under the protection of the
        // wsBBRoot.actionAccess synchronization lock, and the buffer
        // needs to have the put done while continuing to hold this lock

        if (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "actionState set to COPY_WHEN_NEEDED_STATE2: " + wsBBRoot.actionState);
            }

            wsBBRoot.actionState = PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2;
        }

        if (((newPutMin >= wsBBRoot.putMin)
               && (newPutMax <= wsBBRoot.putMax))
            || (wsBBRoot.putMax == -1)) {

            // if new put is completely inside putMin/putMax, or putMin/putMax
            // have not been set, then do not copy data down to the direct buffer

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (wsBBRoot.putMax == -1) {
                    Tr.debug(tc, "putMin/putMax are in initial state, set them to the new values");
                } else {
                    Tr.debug(tc, "new values are inside putMin/putMax, nothing will be done");
                }
            }

            if (wsBBRoot.putMax == -1) {
                // putMin/putMax have not been set yet, so do it now
                wsBBRoot.putMin = newPutMin;
                wsBBRoot.putMax = newPutMax;
                reduceReadMinMax(newPutMin, newPutMax);
            }

        } else if ((newPutMax < wsBBRoot.putMin - 1) || (newPutMin > wsBBRoot.putMax + 1)) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "new min/max completely outside putMin/putMax");
                Tr.debug(tc, "copy putMin/putMax to Direct buffer, then set putMin/putMax to new values");
            }

            // if new put is completely outside putMin/putMax
            boolean avoidCopy = false;

            // see if we can avoid pushing data down by expanding the push area
            if ((wsBBRoot.readMin == -1)
                || ((wsBBRoot.readMin > newPutMax) && (wsBBRoot.readMin > wsBBRoot.putMax))
                || ((wsBBRoot.readMax < newPutMin) && (wsBBRoot.readMax < wsBBRoot.putMin))) {

                // both put areas, and the area between them,
                // are outside of the current read data in the Direct buffer
                if (wsBBRoot.putMin > newPutMin) {
                    if (wsBBRoot.putMin - newPutMax <= PUT_THRESHOLD) {
                        // avoid the copy, by expanding the put area
                        wsBBRoot.putMin = newPutMin;
                        avoidCopy = true;
                    }
                } else {
                    if (newPutMin - wsBBRoot.putMax <= PUT_THRESHOLD) {
                        // avoid the copy, by expanding the put area
                        wsBBRoot.putMax = newPutMax;
                        avoidCopy = true;
                    }
                }
            }

            if (!avoidCopy) {
                // push down to the Direct buffer the current data in the
                // NonDirect buffer from putMin to putMax.
                copyRangeToDirectBuffer(wsBBRoot.putMin, wsBBRoot.putMax);

                // update putMin and putMax to the new values
                wsBBRoot.putMin = newPutMin;
                wsBBRoot.putMax = newPutMax;

                reduceReadMinMax(newPutMin, newPutMax);
            }

        } else if ((newPutMax <= wsBBRoot.putMax) || (newPutMax == wsBBRoot.putMin - 1)) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "new min/max runs into putMin/putMax, set putMin to new value only");
            }

            // new put begins outside putMin/putMax, but either ends inside putMin/putMax
            // or ends one byte before putMin, therefore update putMin to expand put area
            wsBBRoot.putMin = newPutMin;

            reduceReadMinMax(wsBBRoot.putMin, wsBBRoot.putMax);

        } else if ((newPutMin >= wsBBRoot.putMin) || (newPutMin == wsBBRoot.putMax + 1)) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "new min/max runs out from putMin/putMax, set putMax to new value only");
            }

            // new put begins inside putMin/putMax, but ends outside putMin/putMax
            // or end one byte after putMax, therefore update putMin to expand put area
            wsBBRoot.putMax = newPutMax;

            reduceReadMinMax(wsBBRoot.putMin, wsBBRoot.putMax);

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "new min/max contains putMin/putMax, set putMin/putMax to new values");
            }

            // new put contains putMin/putMax, update putMin/putMax to the new values
            wsBBRoot.putMin = newPutMin;
            wsBBRoot.putMax = newPutMax;

            reduceReadMinMax(newPutMin, newPutMax);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "moveDownUsingPutMinMax");
        }

    }

    private void copyRangeFromDirectBuffer(int min, int max) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "copyRangeFromDirectBuffer(min,max) " + min + "," + max);
        }

        if (min > max) {
            // error condition that should not happen.
            Exception ex = new Exception("minumum is greater than maximum");
            FFDCFilter.processException(ex, getClass().getName() + ".copyRangeFromDirectBuffer", "837", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ERROR: min > max");
            }
        } else {

            // copying should not change position/limit in either buffer
            int directPosition = this.oWsBBDirect.position();
            int directLimit = this.oWsBBDirect.limit();

            // set the starting point of the copy
            this.oWsBBDirect.limit(max + 1);
            this.oWsBBDirect.position(min);

            this.oWsBBDirect.get(oByteBuffer.array(), min + oByteBuffer.arrayOffset(), max - min + 1);

            // restore position back and limit to where new data should go
            this.oWsBBDirect.limit(directLimit);
            this.oWsBBDirect.position(directPosition);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "copyRangeFromDirectBuffer");
        }
    }

    private void reduceReadMinMax(int reduceMin, int reduceMax) {
        // routine should only be called under the protection of the
        // wsBBRoot.actionAccess synchronization lock.

        // reduce readMin/readMax, if possible
        if (reduceMin <= wsBBRoot.readMin) {
            if (reduceMax >= wsBBRoot.readMax) {
                // all "read" data in the Direct buffer has been superceded by
                // data in the nonDirect buffer
                wsBBRoot.readMin = -1;
                wsBBRoot.readMax = -1;
            } else if (reduceMax >= wsBBRoot.readMin) {
                // "read" data from the front of the Direct buffer has superceded by
                // data in the nonDirect buffer
                wsBBRoot.readMin = reduceMax + 1;
            }
        } else if ((reduceMax > wsBBRoot.readMax)
                   && (reduceMin <= wsBBRoot.readMax)) {
            // "read" data from the tail of the Direct buffer has been
            // superceded by data in the nonDirect buffer
            wsBBRoot.readMax = reduceMin - 1;
        }
    }

    private void moveUpUsingGetMinMax(int newGetMin, int newGetMax, int recursing) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "moveUpUsingGetMinMax(min,max) " + newGetMin + "," + newGetMax);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "current getMin, getMax: " + wsBBRoot.getMin + "," + wsBBRoot.getMax + " actionState: " + wsBBRoot.actionState);
        }

        // expand to get at least 1K of data, if possible, if we are not recursing
        if ((recursing == 0) && (newGetMax - newGetMin < GET_THRESHOLD)) {
            if (newGetMin + GET_THRESHOLD < this.limit()) {
                newGetMax = newGetMin + GET_THRESHOLD;
            } else {
                newGetMax = this.limit() - 1;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "expanded min,max: " + newGetMin + "," + newGetMax);
            }
        }

        // assume that the wsBBRoot.actionAccess lock has be obtained.
        if (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1) {
            if ((newGetMax < wsBBRoot.getMin)
                || (newGetMin > wsBBRoot.getMax)) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get is outside current getMin/getMax, so pull up data into NonDirect buffer");
                }

                // new get is outside old get, or getMin/getMax has not been set
                // therefore pull min/max data up to nonDirect buffer
                copyRangeFromDirectBuffer(newGetMin, newGetMax);

                // update getMax, getMin
                wsBBRoot.getMin = newGetMin;
                wsBBRoot.getMax = newGetMax;

                reduceReadMinMax(newGetMin, newGetMax);

            } else if ((newGetMax <= wsBBRoot.getMax)
                       && (newGetMin >= wsBBRoot.getMin)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get is inside current getMin/getMax, nothing done");
                }

                // new get is completely inside old get
                // no need to copy any data

            } else if (newGetMax <= wsBBRoot.getMax) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get runs into current getMin/getMax, pull up new data only");
                }

                // new get begins outside old, but ends inside old get, therefore pull
                // new data up to nonDirect buffer.

                // getMin must be greater than 1 for us to be here, so "-1" is safe
                newGetMax = wsBBRoot.getMin - 1;

                copyRangeFromDirectBuffer(newGetMin, newGetMax);

                // only newGetMin is outside of getMin/getMax
                wsBBRoot.getMin = newGetMin;

                reduceReadMinMax(wsBBRoot.getMin, wsBBRoot.getMax);

            } else if (newGetMin >= wsBBRoot.getMin) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get runs out of current getMin/getMax, pull up new data only");
                }

                // new get begins inside old, but ends outside old get therfore pull
                // new data up to nonDirect buffer

                // getMax must be less than the last index for us to be here, so "+1" is safe
                newGetMin = wsBBRoot.getMax + 1;
                copyRangeFromDirectBuffer(newGetMin, newGetMax);

                // only newGetMax is outside of getMin/getMax
                wsBBRoot.getMax = newGetMax;

                reduceReadMinMax(wsBBRoot.getMin, wsBBRoot.getMax);

            } else {
                // new get min/max contains getMin/getMax

                // copy the begining data up to the NonDirect buffer
                // getMin must be greater than 1 for us to be here, so "-1" is safe
                copyRangeFromDirectBuffer(newGetMin, wsBBRoot.getMin - 1);

                // copy the trailing data up to the NonDirect buffer
                // getMax must be less than the last index for us to be here, so "+1" is safe
                copyRangeFromDirectBuffer(wsBBRoot.getMax + 1, newGetMax);

                // enlarge getMin/getMax to new size
                wsBBRoot.getMin = newGetMin;
                wsBBRoot.getMax = newGetMax;

                reduceReadMinMax(newGetMin, newGetMax);
            }

        } else if (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2) {

            // in STATE2 new rules apply to the get, becuase newer "put" data will be
            // in the nonDirect buffer, compared to the older "read" data in the direct buffer
            if (wsBBRoot.putMax != -1) {

                boolean returnNow = false;

                // if get is completely within the putMin/putMax, then no need to copy data
                if ((newGetMax <= wsBBRoot.putMax) && (newGetMin >= wsBBRoot.putMin)) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "new get is inside putMin/putMax, nothing done");
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "moveUpUsingGetMinMax, exit point 1");
                    }
                    return;
                }

                // if Get is partly within putMin/putMax, then recursively call
                // this routine with the Get parts that are outside the Put range.
                if ((newGetMin < wsBBRoot.putMin) && (newGetMax >= wsBBRoot.putMin)) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "new get begins before putMin/putMax, move up to NonDirect buffer this beginning data");
                    }

                    // copy up to the NonDirect buffer parts that are before the putMin
                    moveUpUsingGetMinMax(newGetMin, wsBBRoot.putMin - 1, 1);

                    // want to return after checking for any data on the tail end
                    returnNow = true;
                }

                if ((newGetMax > wsBBRoot.putMax) && (newGetMin <= wsBBRoot.putMax)) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "new get ends after putMin/putMax, move up to NonDirect buffer this trailing data");
                    }

                    // copy up to the NonDirect buffer parts that after the putMax
                    moveUpUsingGetMinMax(wsBBRoot.putMax + 1, newGetMax, 1);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "moveUpUsingGetMinMax, exit point 2");
                    }
                    return;
                }

                if (returnNow) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "moveUpUsingGetMinMax, exit point 3");
                    }
                    return;
                }
            }

            // We can now assume that the new get is completely outside putMin/putMax.
            if ((newGetMax <= wsBBRoot.getMax) && (newGetMin >= wsBBRoot.getMin)) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get is inside getMin/getMax, nothing done");
                }

                // if new get is within getMin/getMax, then no need to copy any data
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "moveUpUsingGetMinMax, exit point 4");
                }
                return;

            } else if ((newGetMax < wsBBRoot.getMin) || (newGetMin > wsBBRoot.getMax)) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get ends outside getMin/getMax, move up to NonDirect this data");
                }

                // new get is completely outside getMin/getMax, or getMin/getMax has not been set
                // pull data up to nonDirect buffer
                copyRangeFromDirectBuffer(newGetMin, newGetMax);

                // update getMax, getMin
                wsBBRoot.getMin = newGetMin;
                wsBBRoot.getMax = newGetMax;

                reduceReadMinMax(newGetMin, newGetMax);

            } else if (newGetMax <= wsBBRoot.getMax) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get runs into getMin/getMax, move up new data only");
                }
                // new get begins outside old getMin/getMax, but ends inside.
                // pull new data up to nonDirect buffer

                // getMin must be greater than 1 for us to be here, so "-1" is safe
                newGetMax = wsBBRoot.getMin - 1;
                copyRangeFromDirectBuffer(newGetMin, newGetMax);

                // only newGetMin is outside of getMin/getMax
                wsBBRoot.getMin = newGetMin;

                reduceReadMinMax(wsBBRoot.getMin, wsBBRoot.getMax);

            } else if (newGetMin >= wsBBRoot.getMin) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "new get runs out of getMin/getMax, move up new data only");
                }
                // new get begins inside old getMin/getMax, but ends outside.
                // pull new data up to nonDirect buffer

                // getMax must be less than the last index for us to be here, so "+1" is safe
                newGetMin = wsBBRoot.getMax + 1;
                copyRangeFromDirectBuffer(newGetMin, newGetMax);

                // only newGetMax is outside of getMin/getMax
                wsBBRoot.getMax = newGetMax;

                reduceReadMinMax(wsBBRoot.getMin, wsBBRoot.getMax);

            } else {
                // new get min/max contains getMin/getMax

                // copy the begining data up to the NonDirect buffer
                // getMin must be greater than 1 for us to be here, so "-1" is safe
                copyRangeFromDirectBuffer(newGetMin, wsBBRoot.getMin - 1);

                // copy the trailing data up to the NonDirect buffer
                // getMax must be less than the last index for us to be here, so "+1" is safe
                copyRangeFromDirectBuffer(wsBBRoot.getMax + 1, newGetMax);

                // enlarge getMin/getMax to new size
                wsBBRoot.getMin = newGetMin;
                wsBBRoot.getMax = newGetMax;

                reduceReadMinMax(newGetMin, newGetMax);
            }

        } else {
            // wsBBRoot.actionState has an invalid value.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "actionState is invalid, continuing on.");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "moveUpUsingGetMinMax, exit point 5");
        }
    }

    public byte get() {
        if (!this.trusted)
            checkValidity();
        byte b = 0;
        if (this.quickBufferAction == NOT_ACTIVATED) {
            b = this.oByteBuffer.get();
        } else {
            synchronized (wsBBRoot.actionAccess) {
                if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                    || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                    int min = this.oByteBuffer.position();

                    // just one byte, so min = max
                    moveUpUsingGetMinMax(min, min, 0);
                    b = this.oByteBuffer.get();
                } else {
                    // buffer algorithm was changed by another thread and/or a buffer duplicate/slics
                    this.quickBufferAction = NOT_ACTIVATED;
                    b = this.oByteBuffer.get();
                }
            } // end-sync
        }
        return b;
    }

    public int position() {
        // checkValidity();
        return (this.oByteBuffer.position());
    }

    public WsByteBuffer position(int p) {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.position(p);
        return this;
    }

    public WsByteBuffer limit(int l) {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.limit(l);
        return this;
    }

    public int limit() {
        // checkValidity();
        return (this.oByteBuffer.limit());
    }

    public int remaining() {
        // checkValidity();
        return this.oByteBuffer.remaining();
    }

    public WsByteBuffer mark() {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.mark();
        return this;
    }

    public WsByteBuffer reset() {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.reset();
        return this;
    }

    public WsByteBuffer rewind() {
        if (!this.trusted)
            checkValidity();
        this.oByteBuffer.rewind();
        return this;
    }

    public boolean isReadOnly() {
        // checkValidity();
        return this.oByteBuffer.isReadOnly();
    }

    public boolean hasRemaining() {
        // checkValidity();
        return this.oByteBuffer.hasRemaining();
    }

    public WsByteBuffer get(byte[] dst) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == NOT_ACTIVATED) {
            this.oByteBuffer.get(dst);
        } else {
            synchronized (wsBBRoot.actionAccess) {

                if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                    || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                    int min = oByteBuffer.position();
                    int max = min + dst.length - 1;

                    if (max >= oByteBuffer.limit()) {
                        // this is an error, so let the JDK throw it, can't move the data
                        // up to the NonDirect buffer without an error, so don't try
                        this.oByteBuffer.get(dst);
                    } else {
                        // move the data up to the buffer before doing the get on the buffer
                        moveUpUsingGetMinMax(min, max, 0);
                        this.oByteBuffer.get(dst);
                    }
                } else {
                    // buffer algorithm was changed by another thread.
                    this.quickBufferAction = NOT_ACTIVATED;
                    this.oByteBuffer.get(dst);
                }
            }
        }
        return this;
    }

    public WsByteBuffer get(byte[] dst, int offset, int length) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == NOT_ACTIVATED) {
            this.oByteBuffer.get(dst, offset, length);
        } else {
            synchronized (this.wsBBRoot.actionAccess) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "wsBBRoot.actionState: " + wsBBRoot.actionState);
                }

                if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                    || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                    int min = oByteBuffer.position();
                    int max = min + length - 1;

                    if (max >= oByteBuffer.limit()) {
                        // this is an error, so let the JDK throw it, can't move the data
                        // up to the NonDirect buffer without an error, so don't try
                        this.oByteBuffer.get(dst, offset, length);
                    } else {
                        // move the data up to the buffer before doing the get on the buffer
                        moveUpUsingGetMinMax(min, max, 0);
                        this.oByteBuffer.get(dst, offset, length);
                    }
                } else {
                    // buffer algorithm was changed by another thread
                    this.quickBufferAction = NOT_ACTIVATED;
                    this.oByteBuffer.get(dst, offset, length);
                }
            } // end-sync
        }
        return this;
    }

    public byte get(int index) {
        if (!this.trusted)
            checkValidity();
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        return this.oByteBuffer.get(index);
    }

    public boolean isDirect() {
        // checkValidity();
        return this.oByteBuffer.isDirect();
    }

    public WsByteBuffer put(byte b) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == NOT_ACTIVATED) {
            this.oByteBuffer.put(b);
        } else {
            synchronized (wsBBRoot.actionAccess) {
                if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                    || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                    int min = oByteBuffer.position();

                    // only 1 byte, so max = min
                    moveDownUsingPutMinMax(min, min);
                    this.oByteBuffer.put(b);
                } else {
                    // buffer algorithm was changed by another thread and/or a buffer duplicate/slics
                    this.quickBufferAction = NOT_ACTIVATED;
                    this.oByteBuffer.put(b);
                }
            } // end-sync
        }
        return this;
    }

    public WsByteBuffer put(byte[] src) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == NOT_ACTIVATED) {
            this.oByteBuffer.put(src);
        } else {
            synchronized (wsBBRoot.actionAccess) {
                if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                    || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                    int min = oByteBuffer.position();
                    int max = min + src.length - 1;

                    if (max >= oByteBuffer.limit()) {
                        // this will give an error, so let the JDK throw it
                        this.oByteBuffer.put(src);
                    } else {
                        moveDownUsingPutMinMax(min, max);
                        this.oByteBuffer.put(src);
                    }
                } else {
                    // buffer algorithm was changed by another thread and/or a buffer
                    // duplicate/slice
                    this.quickBufferAction = NOT_ACTIVATED;
                    this.oByteBuffer.put(src);
                }
            } // end-sync
        }
        return this;
    }

    public WsByteBuffer put(byte[] src, int offset, int length) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == NOT_ACTIVATED) {
            this.oByteBuffer.put(src, offset, length);
        } else {
            synchronized (wsBBRoot.actionAccess) {
                if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                    || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                    int min = oByteBuffer.position();
                    int max = min + length - 1;

                    if (max >= oByteBuffer.limit()) {
                        // this will give an error, so let the JDK throw it
                        this.oByteBuffer.put(src, offset, length);
                    } else {
                        moveDownUsingPutMinMax(min, max);
                        this.oByteBuffer.put(src, offset, length);
                    }

                } else {
                    // buffer algorithm was changed by another thread
                    // and/or a buffer duplicate/slices
                    this.quickBufferAction = NOT_ACTIVATED;
                    this.oByteBuffer.put(src, offset, length);
                }
            } // end-sync
        }
        return this;
    }

    public WsByteBuffer put(int index, byte b) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.put(index, b);
        return this;
    }

    public WsByteBuffer put(ByteBuffer src) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.put(src);
        return this;
    }

    public WsByteBuffer put(WsByteBuffer src) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        this.oByteBuffer.put(src.getWrappedByteBuffer());
        return this;
    }

    public WsByteBuffer put(WsByteBuffer[] src) {
        if (!this.trusted) {
            checkValidity();
            checkReadOnly();
        }
        if (this.quickBufferAction == ACTIVATED) {
            setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        }
        for (int i = 0; i < src.length && src[i] != null; i++) {
            put(src[i]);
        }
        return this;
    }

    public ByteBuffer getWrappedByteBuffer() {
        if (!this.trusted)
            checkValidity();
        return getWrappedByteBufferCommon(false);
    }

    public ByteBuffer getWrappedByteBufferNonSafe() {
        return getWrappedByteBufferCommon(true);
    }

    private ByteBuffer getWrappedByteBufferCommon(boolean internal) {
        if (!internal) {
            if (this.quickBufferAction == ACTIVATED) {
                setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
            }
        }
        return this.oByteBuffer;
    }

    public WsByteBuffer duplicate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "duplicate");
        }
        if (!this.trusted)
            checkValidity();
        // call the pool manager to create a new WsByteBuffer from this one
        if (this.oWsByteBufferPoolManager != null) {
            return this.oWsByteBufferPoolManager.duplicate(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Did not find pool manager, returning null");
        }
        return null;
    }

    protected void updateDuplicate(WsByteBufferImpl wsbytebufferNew) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateDuplicate");
        }
        if (!this.trusted)
            checkValidity();
        if (this.readOnly) {
            wsbytebufferNew.setReadOnly(true);
        }
        wsbytebufferNew.setByteBufferNonSafe(this.oByteBuffer.duplicate());
        wsbytebufferNew.quickBufferAction = this.quickBufferAction;

        if (!oByteBuffer.isDirect()) {

            if (this.quickBufferAction == ACTIVATED) {
                synchronized (wsBBRoot.actionAccess) {
                    if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                        || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "buffer optimization on, duplicating shadow direct buffer");
                        }

                        // want to duplicate the direct buffer with the
                        // same position as the nonDirect buffer.
                        int savedPosition = oWsBBDirect.position();
                        this.oWsBBDirect.position(oByteBuffer.position());
                        wsbytebufferNew.setDirectShadowBuffer(oWsBBDirect.duplicate());
                        this.oWsBBDirect.position(savedPosition);
                    }
                } // end-sync
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateDuplicate");
        }
    }

    public WsByteBuffer slice() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "slice");
        }
        if (!this.trusted)
            checkValidity();
        // call the pool manager to create a new WsByteBuffer from this one
        if (this.oWsByteBufferPoolManager != null) {
            return this.oWsByteBufferPoolManager.slice(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Did not find pool manager, returning null");
        }
        return null;
    }

    protected void updateSlice(WsByteBufferImpl wsbytebufferNew) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateSlice");
        }

        if (!this.trusted)
            checkValidity();

        if (this.readOnly) {
            wsbytebufferNew.setReadOnly(true);
        }

        wsbytebufferNew.setByteBufferNonSafe(this.oByteBuffer.slice());
        wsbytebufferNew.quickBufferAction = this.quickBufferAction;

        if (!oByteBuffer.isDirect()) {
            if (this.quickBufferAction == ACTIVATED) {
                synchronized (wsBBRoot.actionAccess) {
                    if ((wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1)
                        || (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2)) {

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "buffer optomization on, slicing shadow direct buffer");
                        }

                        // want to slice the direct buffer at the same
                        // place as the nonDirect buffer was sliced
                        int savedPosition = oWsBBDirect.position();
                        this.oWsBBDirect.position(oByteBuffer.position());
                        wsbytebufferNew.setDirectShadowBuffer(oWsBBDirect.slice());
                        this.oWsBBDirect.position(savedPosition);
                    }
                } // end-sync
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateSlice");
        }
    }

    protected void resetReleaseCalled() {
        this.booleanReleaseCalled.set(false);
        this.readOnly = false;
    }

    /**
     * Debug method to print part of the current stack.
     */
    public void printStackToDebug() {
        Throwable t = new Throwable();
        StackTraceElement[] ste = t.getStackTrace();

        int start = (ste.length > 6) ? 6 : ste.length;
        for (int i = start; i >= 1; i--) {
            Tr.debug(tc, "Calling Stack Element[" + i + "]: " + ste[i]);
        }
    }

    public void release() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Throwable t = new Throwable();
            StackTraceElement[] ste = t.getStackTrace();
            if (ste.length >= 2) {
                if ((wsBBRoot == null) || (wsBBRoot.pool == null)) {
                    Tr.debug(tc, "BUFFER RELEASED: Calling Element: " + ste[1] + " Main ID: none " + this);
                } else {
                    Tr.debug(tc, "BUFFER RELEASED: Calling Element: " + ste[1] + " Main ID: " + wsBBRoot.getID() + " " + this);
                }
            }
        }

        if ((wsBBRoot == null) || (wsBBRoot.pool == null)) {
            if (wsBBRefRoot != null) {
                synchronized (wsBBRefRoot) {

                    checkValidity();

                    // Decrement the reference count and release the native buffer
                    // if refCount is zero BEFORE we set the boolean release flag. The
                    // getWrappedByteBuffer call below was blowing up
                    // because we had already marked the buffer as deleted.
                    wsBBRefRoot.intReferenceCount--;
                    if (wsBBRefRoot.intReferenceCount == 0) {

                        // We are counting on the last buffer to reduce the ref count to zero,
                        // to also mark the buffer as released. Otherwise, if the root is
                        // released before all its slices are released the slices fail to free
                        // the native storage because it attempts to reference a previously
                        // released buffer
                        this.oWsByteBufferPoolManager.releasing(wsBBRefRoot.getWrappedByteBuffer());

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Marking wsBBRefRoot as released");
                        }
                        this.wsBBRefRoot.booleanReleaseCalled.set(true); // @495358A
                    }

                    // If this is a root buffer, then don't mark us as released, our slices
                    // will do that when our ref count goes to zero. If there are no slices,
                    // then we would have marked ourselves as freed in the above block when
                    // ref count went to zero
                    if (wsBBRefRoot != this) {
                        this.booleanReleaseCalled.set(true);
                    }
                } // end-sync
            }
            return;
        } // end-non-pool scenario

        synchronized (wsBBRoot) {
            checkValidity();

            // passed the check so....

            if (oWsByteBufferPoolManager.getLeakDetectionInterval() > -1) {
                synchronized (oWsByteBufferPoolManager.getLeakDetectionSyncObject()) {
                    this.booleanReleaseCalled.set(true);
                    this.wsBBRoot.intReferenceCount--;
                    releaseFromLeakDetection();
                    if (wsBBRoot.intReferenceCount == 0) {
                        this.oWsByteBufferPoolManager.release(wsBBRoot, isDirectPool, wsBBRoot.pool);
                    }
                } // end-sync
            } else {
                this.booleanReleaseCalled.set(true);
                this.wsBBRoot.intReferenceCount--;
                if (wsBBRoot.intReferenceCount == 0) {
                    this.oWsByteBufferPoolManager.release(wsBBRoot, isDirectPool, wsBBRoot.pool);
                }
            }
        } // end-sync
    }

    private void releaseFromLeakDetection() {
        if ((wsBBRoot != null) && (wsBBRoot.pool != null)) {
            this.wsBBRoot.removeWsByteBuffer(this);
        }
        if (ownerID != null) {
            // remove this entry from the buffer's owner list.
            this.wsBBRoot.removeOwner(ownerID);
        }
    }

    protected void setIsDirectPool(boolean value) {
        this.isDirectPool = value;
    }

    protected boolean getIsDirectPool() {
        return this.isDirectPool;
    }

    public void setReadOnly(boolean value) {
        this.readOnly = value;
    }

    public boolean getReadOnly() {
        return this.readOnly;
    }

    protected void setOwnerID(String value) {
        this.ownerID = value;
        this.lastAccessTime = System.currentTimeMillis();
    }

    protected String getOwnerID() {
        return this.ownerID;
    }

    protected long getLastAccessTime() {
        return this.lastAccessTime;
    }

    /**
     * Set the PoolManager reference.
     * 
     * @param oManagerRef
     */
    public void setPoolManagerRef(WsByteBufferPoolManagerImpl oManagerRef) {
        this.oWsByteBufferPoolManager = oManagerRef;
        this.trusted = oManagerRef.isTrustedUsers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setPoolManagerRef:  trusted=" + this.trusted);
        }
    }

    /**
     * Set the NIO ByteBuffer wrapped by this WsByteBuffer to the input.
     * 
     * @param buffer
     */
    public void setByteBuffer(ByteBuffer buffer) {
        if (!this.trusted)
            checkValidity();
        setByteBufferCommon(buffer, false);
    }

    /**
     * Set the NIO ByteBuffer wrapped by this WsByteBuffer to the input
     * and avoid the buffer validity checks.
     * 
     * @param buffer
     */
    public void setByteBufferNonSafe(ByteBuffer buffer) {
        setByteBufferCommon(buffer, true);
    }

    private void setByteBufferCommon(ByteBuffer buffer, boolean internal) {
        if (!internal) {
            if (this.quickBufferAction == ACTIVATED) {
                setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
            }
        }
        this.oByteBuffer = buffer;
    }

    /**
     * Set the direct bytebuffer that backs an indirect heap buffer to
     * the input buffer.
     * 
     * @param buffer
     */
    public void setDirectShadowBuffer(ByteBuffer buffer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setDirectShadowBuffer");
        }

        if (!this.trusted)
            checkValidity();

        this.oWsBBDirect = buffer;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setDirectShadowBuffer");
        }
    }

    private void checkReadOnly() {
        if (readOnly) {
            String id = "none";
            if ((wsBBRoot != null) && (wsBBRoot.pool != null)) {
                id = (wsBBRoot.getID().toString());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to update a read only WsByteBuffer."
                             + "\nWsByteBuffer: ID: " + id
                             + "\nBuffer: " + this.oByteBuffer);
            }
            ReadOnlyBufferException robe = new ReadOnlyBufferException();
            FFDCFilter.processException(robe, getClass().getName() + ".checkReadOnly", "1", this);
            throw robe;
        }
    }

    private void checkValidity() {
        if (booleanReleaseCalled.get()) {
            String id = "none";
            if ((wsBBRoot != null) && (wsBBRoot.pool != null)) {
                id = (wsBBRoot.getID().toString());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "Attempt to access WsByteBuffer that was already released."
                                         + "\nWsByteBuffer: ID: "
                                         + id
                                         + " Sub ID: "
                                         + this.oByteBuffer);
            }
            RuntimeException iae =
                            new RuntimeException(
                                            "Invalid call to WsByteBuffer method.  Buffer has already been released."
                                                            + "\nWsByteBuffer: ID: "
                                                            + id
                                                            + "\nBuffer: "
                                                            + this.oByteBuffer);
            FFDCFilter.processException(iae, getClass().getName() + ".checkValidity", "1", this);
            throw iae;
        }

        if (ownerID != null) {
            // if there is a ownerID, then leak detection is on, so record
            // that the buffer has been accessed.
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * Copy any data from indirect/heap buffers to the backing shadow
     * direct buffers.
     */
    public void copyToDirectBuffer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "copyToDirectBuffer");
        }

        if (oByteBuffer.isDirect()) {
            this.oWsBBDirect = this.oByteBuffer;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "copyToDirectBuffer, exit point 1");
            }
            return;
        }

        if (this.quickBufferAction == ACTIVATED) {

            synchronized (wsBBRoot.actionAccess) {
                if (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "bufferAction in STATE1, do not copy");
                    }

                    // no put activity has been done on the NonDirect Buffer, so
                    // the data in the Direct buffer can be written as is
                    this.oWsBBDirect.limit(oByteBuffer.limit());
                    this.oWsBBDirect.position(oByteBuffer.position());

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "copyToDirectBuffer, exit point 2");
                    }
                    return;
                }

                if (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "bufferAction in STATE2, copy only changed data");
                    }

                    // put activity has been done on the NonDirect Buffer, so
                    // the new data in the nonDirect buffer needs to be copied
                    // down to the Direct Buffer
                    if ((wsBBRoot.putMin != -1) && (wsBBRoot.putMax != -1)) {

                        copyRangeToDirectBuffer(wsBBRoot.putMin, wsBBRoot.putMax);
                        wsBBRoot.putMin = -1;
                        wsBBRoot.putMax = -1;
                    }

                    this.oWsBBDirect.limit(oByteBuffer.limit());
                    this.oWsBBDirect.position(oByteBuffer.position());

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "copyToDirectBuffer, exit point 3");
                    }
                    return;
                }
            } // end-sync
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "buffer optimization is off, copy all data");
        }

        // we are here because either Optimization was never on, or it just switched off.
        if (oWsBBDirect == null) {
            this.oWsBBDirect = ByteBuffer.allocateDirect(oByteBuffer.capacity());
        }

        int cachePosition = this.oByteBuffer.position();
        int cacheLimit = this.oByteBuffer.limit();
        int offset = this.oByteBuffer.arrayOffset();

        // set the position and limit for copying
        this.oWsBBDirect.limit(cacheLimit);
        this.oWsBBDirect.position(cachePosition);

        // copy the bytes from position to limit
        this.oWsBBDirect.put(oByteBuffer.array(), cachePosition + offset, cacheLimit - cachePosition);

        // reset the position
        this.oWsBBDirect.position(cachePosition);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "copyToDirectBuffer, exit point 4");
        }
    }

    /**
     * Copy the targeted number of bytes from the backing direct buffers.
     * 
     * @param bytesRead
     */
    public void copyFromDirectBuffer(int bytesRead) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "copyFromDirectBuffer: " + bytesRead);
        }

        int newPosition = this.oWsBBDirect.position();
        int cachePosition = this.oByteBuffer.position();

        if (this.quickBufferAction == NOT_ACTIVATED) {

            // copy the bytes read from the old position
            this.oWsBBDirect.position(cachePosition);

            if (bytesRead == -1) {
                // copy all the bytes up to the limit
                this.oWsBBDirect.get(oByteBuffer.array(), cachePosition + oByteBuffer.arrayOffset(), oByteBuffer.remaining());
            } else if (bytesRead > 0) {
                // copy just the bytes that were read
                this.oWsBBDirect.get(oByteBuffer.array(), cachePosition + oByteBuffer.arrayOffset(), bytesRead);
            }

            // set to the position
            this.oByteBuffer.position(newPosition);

        } else {
            synchronized (wsBBRoot.actionAccess) {
                if (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE1) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "actionState: STATE1 - nothing will be copied");
                    }

                    // don't do the copy now, just update the position pointer
                    this.oByteBuffer.position(newPosition);

                    int min = cachePosition;
                    int max = newPosition - 1;

                    // in STATE1 readMin and readMax keep track of the data that will
                    // need to be moved up to the nonDirect buffer upon entering STATE2.
                    // This area can be expanded to contain some bytes which were not
                    // read into, since nothing has been put into the NonDirect buffer,
                    // so any gaps in the read data will be uninitialized data anyway.
                    if ((wsBBRoot.readMin == -1) || (min < wsBBRoot.readMin)) {
                        wsBBRoot.readMin = min;
                    }
                    if ((wsBBRoot.readMax == -1) || (max > wsBBRoot.readMax)) {
                        wsBBRoot.readMax = max;
                    }

                    // if getMin and getMax are not outside of the new read, then reset them,
                    // so that the next get(...) will pull up the new data.
                    if ((wsBBRoot.getMax >= wsBBRoot.readMin)
                        && (wsBBRoot.getMin <= wsBBRoot.readMax)) {

                        wsBBRoot.getMax = -1;
                        wsBBRoot.getMin = -1;
                    }

                } else {
                    // actionState must equal COPY_WHEN_NEEDED_STATE2 or
                    // BUFFER_MGMT_COPY_ALL_FINAL (if another thread just changed it).
                    //

                    if (wsBBRoot.actionState == PooledWsByteBufferImpl.COPY_WHEN_NEEDED_STATE2) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "actionState: STATE2, first copy up any previously read data");
                        }
                        // first copy up to the nonDirect buffer any data that was read before
                        // entering STATE 2
                        if ((wsBBRoot.readMin != -1) && (wsBBRoot.readMax != -1)) {
                            moveUpUsingGetMinMax(wsBBRoot.readMin, wsBBRoot.readMax, 0);
                        }
                    }

                    this.wsBBRoot.actionState = PooledWsByteBufferImpl.COPY_ALL_FINAL;
                    this.quickBufferAction = NOT_ACTIVATED;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "actionState: COPY_ALL_FINAL, copy currently read data to NonDirect buffer ");
                    }

                    // copy the bytes read from the old position
                    this.oWsBBDirect.position(cachePosition);

                    if (bytesRead == -1) {
                        // copy all the bytes up to the limit
                        this.oWsBBDirect.get(oByteBuffer.array(), cachePosition + oByteBuffer.arrayOffset(), oByteBuffer.remaining());
                    } else if (bytesRead > 0) {
                        // copy just the bytes that were read
                        this.oWsBBDirect.get(oByteBuffer.array(), cachePosition + oByteBuffer.arrayOffset(), bytesRead);
                    }

                    // set to the position
                    this.oByteBuffer.position(newPosition);

                }
            } // end-sync
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "copyFromDirectBuffer");
        }
    }

    /**
     * Copy the buffer parameters from the indirect to the backing direct buffer,
     * if necessary.
     */
    public void setParmsToDirectBuffer() {

        if (oByteBuffer.isDirect()) {
            this.oWsBBDirect = this.oByteBuffer;
            return;
        }

        if (oWsBBDirect == null) {
            this.oWsBBDirect = ByteBuffer.allocateDirect(oByteBuffer.capacity());
        }

        // set the position and limit
        this.oWsBBDirect.limit(oByteBuffer.limit());
        this.oWsBBDirect.position(oByteBuffer.position());
    }

    /**
     * Copy the buffer parameters from the backing direct buffer to the
     * indirect layer, if necessary.
     */
    public void setParmsFromDirectBuffer() {
        this.oByteBuffer.position(oWsBBDirect.position());
    }

    public int getType() {
        return WsByteBuffer.TYPE_WsByteBuffer;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int value) {
        this.status = value;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (null == this.oByteBuffer) {
            return "[null]";
        }
        return this.oByteBuffer.toString();
    }

    public void removeFromLeakDetection() {
        // turn off leak detection for this WsByteBuffer
        if (ownerID != null) {
            // if there is an ownerID, then leak detection is on, so release it, and
            // remove the InUse entry so the InUse pool does not leak memory.
            releaseFromLeakDetection();
            if ((wsBBRoot != null) && (wsBBRoot.pool != null)) {
                this.wsBBRoot.pool.removeFromInUse(wsBBRoot);
            }
        }
        this.removedFromLeakDetection = true;
    }

    /*
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput s) throws IOException {
        if (!removedFromLeakDetection) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc,
                         "Buffer being serialized but removeFromLeakDetection has not been called: " + this);
            }
        }

        if (oByteBuffer.isDirect()) {
            // hard code as local strings to help performance.
            // Strings are used here and and in the readObject routines
            s.writeObject("D");
        } else {
            s.writeObject("ND");
        }
        if (oByteBuffer.order() == java.nio.ByteOrder.BIG_ENDIAN) {
            s.writeObject("B");
        } else {
            s.writeObject("L");
        }

        int startPosition = oByteBuffer.position();
        int startLimit = oByteBuffer.limit();

        // set position to 0 and limit to capacity, so we can serialize
        // the entire buffer
        this.oByteBuffer.position(0);
        this.oByteBuffer.limit(this.oByteBuffer.capacity());

        if (oByteBuffer.hasArray() && oByteBuffer.arrayOffset() == 0) {
            s.writeObject(oByteBuffer.array());
        } else {
            byte[] bytes = new byte[oByteBuffer.limit()];
            this.oByteBuffer.get(bytes);
            s.writeObject(bytes);
        }

        this.oByteBuffer.position(startPosition);
        this.oByteBuffer.limit(startLimit);

        s.writeObject(Integer.toString(startPosition));
        s.writeObject(Integer.toString(startLimit));

        if (this.readOnly) {
            s.writeObject("R");
        } else {
            s.writeObject("RW");
        }
    }

    /*
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput s) throws IOException, ClassNotFoundException {
        String type = (String) s.readObject();
        String endian = (String) s.readObject();

        // read in serialized data
        byte[] bytes = (byte[]) s.readObject();

        if (type.equals("ND")) {
            // wrap the bytes into a non-direct buffer. We don't want to
            // get the ByteBuffer from the pool since we would have to copy the
            // bytes from this byte array into the new ByteBuffer.
            // wrap will use a non-direct ByteBuffer
            this.oByteBuffer = ByteBuffer.wrap(bytes);
            if (endian.equals("B")) {
                this.oByteBuffer.order(java.nio.ByteOrder.BIG_ENDIAN);
            } else {
                this.oByteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            }
        } else {
            // allocate a Direct Buffer. Do not allocate from a pool since
            // determining the code responsible for releasing the buffer is
            // not clear.
            this.oByteBuffer = ByteBuffer.allocateDirect(bytes.length);

            // Set endian before reading from the buffer
            if (endian.equals("B")) {
                this.oByteBuffer.order(java.nio.ByteOrder.BIG_ENDIAN);
            } else {
                this.oByteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            }
            this.oByteBuffer.put(bytes);
        }

        this.oByteBuffer.position(Integer.parseInt((String) s.readObject()));
        this.oByteBuffer.limit(Integer.parseInt((String) s.readObject()));

        String rOnly = (String) s.readObject();
        if (rOnly.equals("R")) {
            this.readOnly = true;
        } else {
            this.readOnly = false;
        }

        // 262860 New buffer will need the Pool Manager for duplicates/slices
        this.oWsByteBufferPoolManager = (WsByteBufferPoolManagerImpl) WsByteBufferPoolManagerImpl.getRef();
        this.trusted = this.oWsByteBufferPoolManager.isTrustedUsers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readExternal:  trusted set to: " + this.trusted);
        }
        // If we can serialize, then this was/is true.
        this.removedFromLeakDetection = true;
    }
}
