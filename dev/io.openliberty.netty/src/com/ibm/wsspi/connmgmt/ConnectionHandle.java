/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.connmgmt;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Unique identifier for the connection (a long), and a sequence number (an
 * int).
 * These values are assigned by the TCP channel (implementation specific what
 * value
 * is used) when a new connection is created, and can then be used to uniquely
 * identify
 * the connection.
 * <p>
 * The ConnectionHandle serializes into 16 bytes: 8 byte Connection id 4 byte
 * Sequence number 1 byte Factory Id 1 byte Factory-specific flags 1 byte
 * ConnectionType 1 byte reserved for future use
 * <p>
 * The ConnectionHandleFactory can be extended to create ConnectionHandles of
 * different types. One byte in the connection handle specifies what factory was
 * used to create the handle, and a second byte is reserved for flags (and it's
 * associated ConnectionHandle implementation) can use to store connection
 * information.
 * <p>
 * The ConnectionType contains basic information about the connection (inbound
 * vs. outbound, client vs. server).
 */
public class ConnectionHandle {
    private static final TraceComponent tc = Tr.register(ConnectionHandle.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Length of exported connection handle bytes, native code may depend on
     * this.
     */
    public static final int CONNECTION_ID_LENGTH = 16;

    /** Virtual Connection Key for the unique connection key */
    protected static final String CONNECTION_HANDLE_VC_KEY = "CFW_CONNECTION_HANDLE";

    // -------- static ----------

    protected static final ConnectionHandleFactory factory = new ConnectionHandleFactory();

    /**
     * Get ConnectionHandle stored in VirtualConnection state map (if one isn't
     * there already, assign one).
     * 
     * @param vc
     * @return ConnectionHandle
     */
    public static ConnectionHandle getConnectionHandle(VirtualConnection vc) {
        if (vc == null) {
            return null;
        }

        ConnectionHandle connHandle = (ConnectionHandle) vc.getStateMap().get(CONNECTION_HANDLE_VC_KEY);

        // We can be here for two reasons:
        // a) We have one, just needed to find it in VC state map
        if (connHandle != null)
            return connHandle;

        // b) We want a new one
        connHandle = factory.createConnectionHandle();

        // For some connections (outbound, most notably), the connection type
        // will be set on the VC before connect... in which case, read it
        // and set it on the connection handle at the earliest point
        // possible.
        if (connHandle != null) {
            connHandle.setConnectionType(vc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getConnectionHandle - created new connection handle: " + connHandle);
        }

        setConnectionHandle(vc, connHandle);
        return connHandle;
    }

    /**
     * Set the connection handle on the virtual connection.
     * 
     * @param vc
     *            VirtualConnection containing simple state for this connection
     * @param handle
     *            ConnectionHandle for the VirtualConnection
     */
    public static void setConnectionHandle(VirtualConnection vc, ConnectionHandle handle) {
        if (vc == null || handle == null) {
            return;
        }
        Map<Object, Object> map = vc.getStateMap();

        // set connection handle into VC
        Object vcLock = vc.getLockObject();
        synchronized (vcLock) {
            Object tmpHandle = map.get(CONNECTION_HANDLE_VC_KEY);

            // If this connection already has a unique handle when we get here,
            // something went wrong.
            if (tmpHandle != null) {
                throw new IllegalStateException("Connection " + tmpHandle + " has already been created");
            }

            map.put(CONNECTION_HANDLE_VC_KEY, handle);
        }
    }

    // -------- instance vars ----------

    /**
     * Connection ID - may be either an address of native connection object or a
     * counter
     */
    protected final long connID;

    /** Sequence number - protects against reuse. */
    protected final int seqNum;

    /** ID of ConnectionHandleFactory, prevents inter-factory ID collisions. */
    protected final byte connHandleCreatorId;

    /** Get the "type" of connection */
    protected byte myType;

    /**
     * Flags stored in the connection handle containing information about the
     * connection
     */
    protected byte myFlags;

    /**
     * Create a ConnectionHandle using the connection id / sequence values
     * managed by this class. This constructor is used when a connection handle
     * is not set by the TCP Channel.
     */
    protected ConnectionHandle(long connID, int seqNum, byte creatorId) {
        this.connID = connID;
        this.seqNum = seqNum;
        this.myType = 0;
        this.myFlags = 0;
        this.connHandleCreatorId = creatorId;
    }

    /**
     * Creates a reference ConnectionHandle from bytes.
     * 
     * @param bytes
     */
    public ConnectionHandle(byte[] bytes) {
        if (bytes == null || bytes.length < 16) {
            throw new IllegalArgumentException("Cannot create a valid connection handle");
        }

        ByteBuffer bbuf = ByteBuffer.wrap(bytes);

        this.connID = bbuf.getLong();
        this.seqNum = bbuf.getInt();
        this.connHandleCreatorId = bbuf.get();
        this.myFlags = bbuf.get();
        this.myType = bbuf.get();
        bbuf.get(); // unused
    }

    /**
     * Set ConnectionType based on the input virtual connection.
     * 
     * @param vc
     */
    protected void setConnectionType(VirtualConnection vc) {

        if (this.myType == 0 || vc == null) {
            ConnectionType newType = ConnectionType.getVCConnectionType(vc);
            this.myType = (newType == null) ? 0 : newType.export();
        }
    }

    /**
     * Returns unique connection information as a byte array.
     * 
     * @return byte[]
     */
    public byte[] getBytes() {

        byte[] me = new byte[CONNECTION_ID_LENGTH];
        ByteBuffer bbuf = ByteBuffer.wrap(me);

        this.putBytes(bbuf);
        return me;
    }

    /**
     * Returns unique connection information as a byte array.
     * 
     * @param writeBuffer
     *            ByteBuffer to write ConnectionHandle bytes into.
     */
    public void putBytes(ByteBuffer writeBuffer) {

        if (writeBuffer.remaining() < CONNECTION_ID_LENGTH) {
            throw new IllegalArgumentException("Could not add ConnectionHandle to byte buffer: not enough space.");
        }

        writeBuffer.putLong(this.connID);
        writeBuffer.putInt(this.seqNum);
        writeBuffer.put(this.connHandleCreatorId);
        writeBuffer.put(this.myFlags);
        writeBuffer.put(this.myType);
        writeBuffer.put((byte) 0xFF); // place holder - empty flag
    }

    /**
     * Return true if this is an outbound connection.
     * 
     * @return boolean
     */
    public boolean isOutbound() {
        // Inbound connections typically do not have connection types,
        // while outbound connections always do. If this handle doesn't
        // have a connection type, assume it's an inbound connection.
        if (myType == 0)
            return false;

        return ConnectionType.isOutbound(myType);
    }

    /**
     * @return boolean
     */
    public boolean isInbound() {
        // Inbound connections typically do not have connection types,
        // while outbound connections always do. If this handle doesn't
        // have a connection type, assume it's an inbound connection.
        if (myType == 0)
            return true;

        return ConnectionType.isInbound(myType);
    }

    /**
     * Get ConnectionType.
     * 
     * @return ConnectionType
     */
    public ConnectionType getConnectionType() {
        return ConnectionType.getConnectionType(myType);
    }

    /**
     * Two connection keys are equal when
     * a) they were both created by the same factory,
     * b) the connection IDs match
     * c) the sequence numbers match
     * 
     * @param o
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {

        if (o == null || o.getClass() != this.getClass())
            return false;

        if (o == this)
            return true;

        ConnectionHandle that = (ConnectionHandle) o;

        // These are the same if the same factory created them, and the
        // connection id and sequence numbers match
        return ((that.connHandleCreatorId == this.connHandleCreatorId) && (that.connID == this.connID) && (that.seqNum == this.seqNum));
    }

    /**
     * The hashCode loses some data from the connID. Relies on equals to make
     * sure the objects compare correctly.
     */
    @Override
    public int hashCode() {
        return (int) this.connID;
    }

    /**
     * Overriding implementation of parent's method.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        String type = (myType == 0 ? "<type>" : ConnectionType.getConnectionType(myType).toString());

        return getClass().getSimpleName() + "[0x" + Long.toHexString(this.connID) + "/0x" + Integer.toHexString(this.seqNum) + "/" + type + "]";
    }

    // -----------------------------------------------------------------

    /**
     * This factory can be subclassed (the static factory instance can be
     * re-assigned to
     * point to the new ConnectionHandleFactory.
     * 
     * Any subclass should document it's own creator id (as a byte), so
     * that handles created by different factories will not be treated
     * as the same connection if their ID and sequence numbers otherwise match.
     */
    public static class ConnectionHandleFactory {

        protected static final byte ConnectionHandleFactoryCreatorId = 0x01;

        /** Next connection id: increments with every new connection */
        private static final AtomicLong nextConnectionId = new AtomicLong(0);

        /** Connection sequence id: Increments when nextConnectionId wraps. */
        private static final AtomicInteger sharedSequenceNum = new AtomicInteger(0);

        /**
         * Create a new connection handle object.
         * 
         * @return ConnectionHandle
         */
        public ConnectionHandle createConnectionHandle() {

            long connID;
            int seqNum;

            connID = nextConnectionId.incrementAndGet();

            // connID & seqNum don't have to increment at exactly the same time: the
            // point is to prevent collisions... so if the connection id wraps, the
            // sequence id should wrap at around the same time.
            if (nextConnectionId.compareAndSet(Long.MAX_VALUE, Long.MIN_VALUE)) {
                seqNum = sharedSequenceNum.incrementAndGet();
                sharedSequenceNum.compareAndSet(Integer.MAX_VALUE, Integer.MIN_VALUE);
            } else
                seqNum = sharedSequenceNum.get();

            return new ConnectionHandle(connID, seqNum, ConnectionHandleFactoryCreatorId);
        }
    }
}
