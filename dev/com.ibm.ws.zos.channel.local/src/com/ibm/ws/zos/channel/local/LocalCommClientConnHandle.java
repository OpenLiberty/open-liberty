/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local;

import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * Convenience wrapper around a native localcomm client connection handle.
 * 
 * The connection handle itself is a byte[], mapped in native code 
 * (com.ibm.zos.native/include/server_local_comm_client.h).
 * 
 * !! NOTE !! This mapping must be kept in sync with the native mapping (server_local_comm_client.h).
 * 
 * Note: the implementation is exported from the bundle (rather than just an
 * interface) because the wola bundle needs to be able to create instances
 * of these things.
 */
public class LocalCommClientConnHandle {
    
    /**
     * The size of the handle.
     */
    public static final int Size = 16;

    /**
     * Offset to the LHDL ptr field within the handle.
     */
    protected static final int LocalCommHandlePtrOffset = 0;
    
    /**
     * Offset to the instanceCount field within the handle.
     */
    protected static final int InstanceCountOffset = 8;
    
    /**
     * The connection handle byte data.
     */
    private byte[] rawData;
    
    /**
     * Cached hashcode, which is computed by adding up all the rawData bytes.
     */
    private Integer cachedHashCode = null;
    
    /**
     * CTOR.
     */
    public LocalCommClientConnHandle(byte[] rawData) {
        this.rawData = Arrays.copyOf(rawData, rawData.length);
    }
    
    /**
     * @return the raw bytes
     */
    public byte[] getBytes() {
        return Arrays.copyOf(rawData, rawData.length);
    }
    
    /**
     * @return the LHDL ptr
     */
    public long getLhdlPtr() {
        return ByteBuffer.wrap(rawData).getLong(LocalCommHandlePtrOffset);
    }
    
    /**
     * @return the instance count
     */
    public int getInstanceCount() {
        return ByteBuffer.wrap(rawData).getInt(InstanceCountOffset);
    }
    
    /**
     * @return hashCode for this conn handle.
     */
    public int hashCode() {
        if (cachedHashCode == null) {
            cachedHashCode = Arrays.hashCode(rawData);
        }
        
        return cachedHashCode;
    }
    
    /**
     * @return true if the given object is a LocalCommClientConnHandle and it contains
     *         the exact same rawData has this guy.
     */
    public boolean equals(Object o) {
        if (o instanceof LocalCommClientConnHandle) {
            LocalCommClientConnHandle that = (LocalCommClientConnHandle)o;
            return Arrays.equals(this.rawData, that.rawData);
        }
        return false;
    }
    
    /**
     * @return Hex-stringified conn handle
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName() + ":x");
    
        ByteBuffer bb = ByteBuffer.wrap(rawData);
        while (bb.remaining() >= 4) {
            sb.append( String.format("%1$08x", bb.getInt()) + "." );
        }
        
        return sb.toString();
    }

}
