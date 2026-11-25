/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.jni.internal;

import java.util.Arrays;

/**
 * Class to hold information about registered classes.
 */
final class NativeMethodInfo {

    /**
     * The class instance that the native methods are registered against.
     */
    final Class<?> clazz;

    /**
     * The name of the {@code NativeMethodDescriptor} exported from the
     * DLL that will be used during registration and deregistration.
     */
    final String nativeDescriptorName;

    /**
     * Extra context or information that the caller can provide to the
     * register/initialize and unregister/destroy functions. This can
     * be used to save state or communicate data across the boundary.
     */
    final Object[] extraInfo;

    /**
     * The handle returned by dlopen for this registration.
     */
    final long dllHandle;

    NativeMethodInfo(Class<?> clazz, String nativeDescriptorName, Object[] extraInfo, long dllHandle) {
        this.clazz = clazz;
        this.nativeDescriptorName = nativeDescriptorName;
        this.extraInfo = extraInfo;
        this.dllHandle = dllHandle;
    }

    Class<?> getClazz() {
        return clazz;
    }

    String getNativeDescriptorName() {
        return nativeDescriptorName;
    }

    Object[] getExtraInfo() {
        return extraInfo;
    }

    long getDllHandle() {
        return dllHandle;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());

        sb.append(";clazz=").append(clazz);
        sb.append(",nativeDescriptorName=").append(nativeDescriptorName);
        sb.append(",extraInfo=").append(extraInfo == null ? null : Arrays.asList(extraInfo));
        sb.append(",dllHandle=").append(dllHandle);

        return sb.toString();
    }
}