/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.util.ObjectSizer;
import com.ibm.ws.cache.util.SerializationUtility;

/**
 * This class represents an invalidation by id (either cache id or data id). The
 * entry with the id will be removed from the cache, as will any entry that has
 * a dependency on this id.
 */
public class InvalidateByIdEvent implements InvalidationEvent, Externalizable {

    private static final long serialVersionUID = 1342185474L;
    static final boolean INVOKE_INTERNAL_INVALIDATE_BY_ID = true;
    static final boolean INVOKE_DRS_RENOUNCE = true;
    private static TraceComponent tc = Tr.register(InvalidateByIdEvent.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    /**
     * The cache id to be invalidated.
     */
    private Object id = null;

    /**
     * The serialized cache id to be invalidated.
     */
    private byte[] serializedId = null;

    /**
     * The creation time of the invalidation.
     */
    private long timeStamp = -1;

    /**
     * This is the absolute time when the event leaves the local machine heading
     * for a remote machine. It is used by the receiving machine to adjust
     * timeStamp.
     */
    private long drsClock = -1;

    public boolean useServerClassLoader = false;

    public int causeOfInvalidation;
    public int source;
    transient public boolean invokeInternalInvalidateById = true;
    transient public boolean invokeDRSRenounce = INVOKE_DRS_RENOUNCE;

    private String cacheName = null;

    /**
     * Constructor with parameters.
     * 
     * @param id
     *            The cache id to be invalidated.
     * @param causeOfInvalidation
     *            The cause of this invalidation.
     */
    public InvalidateByIdEvent(Object id, int causeOfInvalidation, int source,
                               boolean invokeInternalInvalidateById, boolean invokeDRSRenounce,
                               String cacheName) {
        this.id = id;
        this.causeOfInvalidation = causeOfInvalidation;
        this.source = source;
        timeStamp = System.currentTimeMillis();
        this.invokeInternalInvalidateById = invokeInternalInvalidateById;
        this.invokeDRSRenounce = invokeDRSRenounce;
        this.cacheName = cacheName;
    }

    public InvalidateByIdEvent(Object id, int causeOfInvalidation, int source,
                               boolean invokeInternalInvalidateById, boolean invokeDRSRenounce) {
        this.id = id;
        this.causeOfInvalidation = causeOfInvalidation;
        this.source = source;
        timeStamp = System.currentTimeMillis();
        this.invokeInternalInvalidateById = invokeInternalInvalidateById;
        this.invokeDRSRenounce = invokeDRSRenounce;
    }

    /**
     * Constructor with parameters.
     * 
     * @param id
     *            The cache id to be invalidated.
     */
    public InvalidateByIdEvent(Object id) {
        this.id = id;
        timeStamp = System.currentTimeMillis();
    }

    public InvalidateByIdEvent(Object id, long timeStamp) {
        this.id = id;
        this.timeStamp = timeStamp;
    }

    // For serialization only
    public InvalidateByIdEvent() {}

    public void setClassLoaderType(boolean cl) {
        this.useServerClassLoader = cl;
    }

    // ------------------------------------------------------

    /**
     * This returns the cache id.
     * 
     * @param The
     *            cache id.
     */
    public Object getId() {
        return id;
    }

    public byte[] getSerializedId() {
        return serializedId;
    }

    /**
     * This implements the method in the InvalidationEvent interface.
     * 
     * @return The creation timestamp.
     */
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    public String getCacheName() {
        return cacheName;
    }

    public boolean prepareForSerialization() {
        boolean success = true;
        String exceptionMessage = null;
        if (serializedId == null) {
            try {
                serializedId = SerializationUtility
                                .serialize((Serializable) id);
            } catch (IOException e) {
                exceptionMessage = e.toString();
                // com.ibm.ws.ffdc.FFDCFilter.processException(e,
                // "com.ibm.ws.cache.InvalidateByIdEvent.prepareForSerialization",
                // "172", this);
            } catch (ClassCastException e) {
                exceptionMessage = e.toString();
                // com.ibm.ws.ffdc.FFDCFilter.processException(e,
                // "com.ibm.ws.cache.InvalidateByIdEvent.prepareForSerialization",
                // "174", this);
            }
            // PK34428: getting nullptr exception on Tr.error with line 265 left
            // in: //serializedId = null;
            if (serializedId == null) {
                Tr.error(tc, "DYNA0052E", new Object[] { id,
                                                        id.getClass().getName(), exceptionMessage });
                success = false;
            }
        }
        return success;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        if (!prepareForSerialization()) {
            throw new IOException(
                            "Object not serializable: " + (serializedId == null ? "null object"
                                            : serializedId.getClass().getName()));
        }
        // 246253 end
        if (serializedId != null) {
            out.writeInt(serializedId.length);
            out.write(serializedId);
        } else {
            out.writeInt(-1);
        }
        out.writeLong(timeStamp);
        out.writeLong(drsClock);
        out.writeInt(causeOfInvalidation);
        out.writeInt(source);
        out.writeBoolean(useServerClassLoader);
        out.writeObject(getCacheName());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
                    ClassNotFoundException {

        int keyLength = in.readInt();
        if (keyLength > 0) {
            serializedId = new byte[keyLength];
            in.readFully(this.serializedId);
        } else {
            serializedId = null;
        }
        id = null;
        timeStamp = in.readLong();
        drsClock = in.readLong();
        causeOfInvalidation = in.readInt();
        source = in.readInt();
        try {
            useServerClassLoader = in.readBoolean();
        } catch (Exception ex) {
            useServerClassLoader = false;
        }

        try {
            cacheName = (String) in.readObject();
        } catch (Exception ex) {
            cacheName = null;
        }

        if (serializedId != null) {
            try {

                id = SerializationUtility.deserialize(serializedId, cacheName);
                serializedId = null;
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex,
                                                            "com.ibm.ws.cache.InvalidateByIdEvent.getId", "146",
                                                            this);
            }
            // serializedId = null; //Removed this since getting NPE &
            // DYNA0052E. IdObject never had this line /* PK34428 */
        } else {
            id = null;
        }
    }

    // cache.batchUpdate() uses to this method to invoke internalInvalidateById
    // or not.
    public boolean isInvokeInternalInvalidateById() {
        return this.invokeInternalInvalidateById;
    }

    // DRSNotiificationService.buildRenounceList() uses to this method to do DRS
    // renounce or not for Push-Pull mode.
    public boolean isInvokeDRSRenounce() { // LI4337-17
        return this.invokeDRSRenounce;
    }

    /**
     * @return estimate (serialized) size of InvalidateByIdEvent. It is called
     *         by DRS to calculate the payload.
     */
    public long getSerializedSize() {
        long totalSize = 0;
        if (this.serializedId != null) {
            totalSize += ObjectSizer.getSize(this.serializedId);
        }
        // System.out.println("InvalidateByIdEvent.getSerializedSize(): id=" +
        // id + " size=" + totalSize);
        return totalSize;
    }

    @Override
    public String toString() {
        return "InvalidateByIdEvent [id=" + id + ", causeOfInvalidation="
                + causeOfInvalidation + ", invokeInternalInvalidateById="
                + invokeInternalInvalidateById + ", source=" + source
                + ", timeStamp=" + timeStamp + ", cacheName=" + cacheName + "]";
    }
}
