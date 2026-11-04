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
package com.ibm.ws.zos.channel.local.queuing;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * NativeWorkRequest types.
 * 
 * !!! NOTE !!!: These values must be kept in sync with native code 
 * LocalCommWorkQueueElement "requestType" definitions in server_local_comm_queue.h
 *
 */
public enum NativeWorkRequestType {
    
    REQUESTTYPE_CONNECT(1),
    
    REQUESTTYPE_CONNECTRESPONSE(2),
    
    REQUESTTYPE_DISCONNECT(3),
    
    REQUESTTYPE_SEND(4),
    
    REQUESTTYPE_READREADY(5),
    
    REQUESTTYPE_FFDC(6);
    
    /**
     * Note: I'm deliberately not relying on Enum.ordinal() to associate
     * the native values with the Enum values.  ordinal() would be more
     * convenient but it's not safe and not good design.
     */
    private final int nativeValue;
        
    /**
     * Reverse lookup of Enum value by native value.
     */
    private static NativeWorkRequestType[] byNativeValue; 
    
    /**
     * Populate the byNativeValue lookup.
     */
    static {
        byNativeValue = new NativeWorkRequestType[NativeWorkRequestType.class.getEnumConstants().length+1]; // +1 since we start at 1, not 0
        for (NativeWorkRequestType requestType : NativeWorkRequestType.class.getEnumConstants()) {
            byNativeValue[requestType.nativeValue] = requestType; 
        }
    }

    /** 
     * @param nativeValue
     */
    private NativeWorkRequestType(int nativeValue) {
        this.nativeValue = nativeValue;
    }
    
    /**
     * @param nativeValue
     * 
     * @return The enum value for the given nativeValue.
     * 
     * @throws IllegalArgumentException if the nativeValue does not correspond to an Enum value.
     */
    public static NativeWorkRequestType forNativeValue(int nativeValue) {
        if (nativeValue >= byNativeValue.length || byNativeValue[nativeValue] == null) {
            throw new IllegalArgumentException("Invalid native value (" + nativeValue + ") for NativeWorkRequestType");
        } else {
            return byNativeValue[nativeValue];
        }
    }

    /**
     * @return The native value for this Enum value.
     */
    @Trivial
    public int getNativeValue() {
        return nativeValue;
    }
    
    /**
     * @return Stringified enum contains name and nativeValue.
     */
    public String toString() {
        return super.toString() + "(" + nativeValue + ")";
    }

}
