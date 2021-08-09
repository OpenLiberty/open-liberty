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
package com.ibm.ws.kernel.boot.delegated.zos;

import java.io.File;
import java.text.MessageFormat;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelUtils;

/**
 * The {@code NativeMethodHelper} is responsible for managing the resolution
 * and linking of native methods to classes.
 */
public final class NativeMethodHelper {

    /**
     * The name of the DLL containing the z/OS native service implementations.
     */
    private final static String DLL_NAME = "zNativeServices";

    /**
     * The library load failure that we last encountered.
     */
    private static Throwable loadFailure = null;

    /**
     * The library name used during initialization.
     */
    static String libraryName = null;

    /**
     * Flag to indicate that the native code has been successfully initialized.
     * If false, an initialization failure occurred.
     */
    private final static boolean initialized = loadAndInitialize();

    /**
     * Register native methods from the core DLL for the specified class and drive the
     * registration hooks.
     * 
     * @param clazz the class to link native methods for
     * @param nativeDescriptorName the name of the exported {@code NativeMethodDescriptor} structure
     * @param extra information provided to the registration callback by the caller
     * 
     * @throws UnsatisfiedLinkError if an error occurs during resolution or registration
     * 
     * @return the native DLL handle reference for this class or the error return code
     */
    public static long registerNatives(Class<?> clazz, String nativeDescriptorName, Object[] extraInfo) {
        if (!initialized) {
            if (loadFailure != null) {
                Error e = new UnsatisfiedLinkError();
                e.initCause(loadFailure);
                throw e;
            } else {
                return 0;
            }
        }

        return ntv_registerNatives(clazz, nativeDescriptorName, extraInfo);
    }

    /**
     * Drive the deregistration hooks the native methods from the core DLL for the specified class.
     * 
     * @param clazz the class cleanup should occur for
     * @param nativeDescriptorName the name of the structure used for registration
     * @param dllHandle the DLL handle that was used during registration
     * @param extraInfo extra information provided to the callback by the caller
     * 
     * @return the return code
     */
    public static long deregisterNatives(long dllHandle, Class<?> clazz, String nativeDescriptorName, Object[] extraInfo) {
        if (!initialized) {
            if (loadFailure != null) {
                Error e = new UnsatisfiedLinkError();
                e.initCause(loadFailure);
                throw e;
            } else {
                return 0;
            }
        }

        return ntv_deregisterNatives(dllHandle, clazz, nativeDescriptorName, extraInfo);
    }

    private final static boolean loadAndInitialize() {
        // Try System.loadLibrary first.  This should accommodate overriding
        // the path with java.library.path.
        try {
            libraryName = System.mapLibraryName(DLL_NAME);
            System.loadLibrary(DLL_NAME);
            return true;
        } catch (Throwable t) {
        }

        // System.loadLibrary failed, try to load the DLL from the well known
        // location under our lib directory.
        try {
            String wasLibPath = KernelUtils.getBootstrapLibDir().getAbsolutePath();
            String wasLibrary = wasLibPath + "/native/zos/s390x/" + System.mapLibraryName(DLL_NAME);
            if ((new File(wasLibrary)).exists()) {
                libraryName = wasLibrary;
                System.load(wasLibrary);
                return true;
            }
        } catch (Throwable t) {
            loadFailure = t;
            System.err.println(MessageFormat.format(BootstrapConstants.messages.getString("error.loadNativeLibrary"), libraryName));
        }

        libraryName = null;
        return false;
    }

    private final static native long ntv_registerNatives(Class<?> clazz, String nativeDescriptorName, Object[] extraInfo);

    private final static native long ntv_deregisterNatives(long dllHandle, Class<?> clazz, String nativeDescriptorName, Object[] extraInfo);

}
