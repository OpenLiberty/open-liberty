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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Hashtable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * Pooled instance of a WsByteBuffer.
 */
public class PooledWsByteBufferImpl extends WsByteBufferImpl {

    private static final TraceComponent tc = Tr.register(PooledWsByteBufferImpl.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    private static final long serialVersionUID = -7843989267561719849L;

    protected int getMin = -1;
    protected int getMax = -1;
    protected int putMin = -1;
    protected int putMax = -1;
    protected int readMin = -1;
    protected int readMax = -1;

    protected static final int COPY_ALL_INIT = 0;
    protected static final int COPY_WHEN_NEEDED_STATE1 = 1;
    protected static final int COPY_WHEN_NEEDED_STATE2 = 2;
    protected static final int COPY_ALL_FINAL = 4;
    protected int actionState = COPY_ALL_INIT;
    protected final Object actionAccess = new Object();

    private final Object identifier;
    /** number of references to this pool entry */
    transient public int intReferenceCount = 1;

    /** The pool that this entry belongs to */
    protected WsByteBufferPool pool = null;

    /**
     * ownership hashmap. To be used only when the memory leak detection
     * option is on. Otherwise, this is to remain null to optimize performance
     */
    Hashtable<String, String> owners = null;

    /** allWsByteBuffers is only used if leak detection is on */
    private Hashtable<WsByteBuffer, WsByteBuffer> allWsByteBuffers = null;

    /**
     * Constructor.
     */
    public PooledWsByteBufferImpl() {
        super();
        this.wsBBRoot = this;
        this.identifier = null;
        // if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
        // Tr.event(tc, "Created " + this);
        // }
    }

    public PooledWsByteBufferImpl(Object id) {
        super();
        this.wsBBRoot = this;
        this.identifier = id;
        // if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
        // Tr.event(tc, "Created " + this);
        // }
    }

    /**
     * Access the ID for this buffer.
     *
     * @return Object
     */
    public Object getID() {
        return this.identifier;
    }

    /**
     * If leak detection is enabled, this is the table that keeps track of
     * buffers related to this one (itself plus dupes and slices).
     *
     * @return Hashtable<WsByteBuffer,WsByteBuffer>
     */
    public Hashtable<WsByteBuffer, WsByteBuffer> getallBuffers() {
        return this.allWsByteBuffers;
    }

    /**
     * Add a related buffer to the stored list for this instance.
     *
     * @param buffer
     */
    public void addWsByteBuffer(WsByteBuffer buffer) {
        if (this.allWsByteBuffers == null) {
            this.allWsByteBuffers = new Hashtable<WsByteBuffer, WsByteBuffer>();
        }
        this.allWsByteBuffers.put(buffer, buffer);
    }

    /**
     * Remove a buffer from the stored list for this instance.
     *
     * @param buffer
     */
    public void removeWsByteBuffer(WsByteBuffer buffer) {
        this.allWsByteBuffers.remove(buffer);
    }

    /**
     * Add the following owner to the list for this buffer.
     *
     * @param owner
     */
    public void addOwner(String owner) {
        if (this.owners == null) {
            this.owners = new Hashtable<String, String>();
        }
        this.owners.put(owner, owner);
    }

    /**
     * Remove the input ID from the stored owners of this buffer.
     *
     * @param owner
     */
    public void removeOwner(String owner) {
        this.owners.remove(owner);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#toString()
     */
    @Override
    public String toString() {
        return toString(null);
    }

    /**
     * Return the debug information for this buffer, as it related to the input
     * owner.
     *
     * @param ownerID
     * @return String
     */
    public String toString(String ownerID) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(getClass().getSimpleName());
        sb.append('@');
        sb.append(Integer.toHexString(hashCode()));
        sb.append('/').append(this.intReferenceCount);
        sb.append(" ID=").append(this.identifier);
        sb.append(' ').append(super.toString());
        sb.append(' ').append(this.pool);

        if (this.owners != null) {
            sb.append("\nOwnership Entry:");
            int i = 0;
            for (String value : this.owners.values()) {
                if (null == ownerID || ownerID.equals(value)) {
                    sb.append("\n[").append(i++).append("] ").append(value);
                }
            }
        }

        return sb.toString();
    }

    /*
     * @seecom.ibm.ws.bytebuffer.internal.WsByteBufferImpl#writeExternal(java.io.
     * ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput s) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Serializing " + this);
        }
        super.writeExternal(s);
    }

    /*
     * @seecom.ibm.ws.bytebuffer.internal.WsByteBufferImpl#readExternal(java.io.
     * ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput s) throws IOException, ClassNotFoundException {
        super.readExternal(s);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deserializing " + this);
        }
    }
}
