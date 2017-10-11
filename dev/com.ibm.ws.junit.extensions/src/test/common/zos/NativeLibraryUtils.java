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
package test.common.zos;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.zos.core.internal.NativeServiceTracker;

/**
 * NativeLibraryUtils provides a set of services that z/OS native unit test code can use to
 * register native methods of the Liberty server code. In a running Liberty server, there
 * is code in the com.ibm.ws.zos.core project that is used to register native methods; this
 * class is a simplified version that performs most of the same tasks, and is provided so
 * that the z/OS native unit tests do not have compile dependencies on server code other
 * than the exact projects being tested.
 */
public class NativeLibraryUtils {
    /**
     * Used to hold information about classes that have had their native methods registered.
     * The information is used later on when the class' native methods are deregistered.
     */
    private final static class NativeMethodInfo {
        Class<?> clazz;
        String nativeDescriptorName;
        Object[] extraInfo;
        long dllHandle;

        NativeMethodInfo(Class<?> initClazz, String initNativeDescriptorName, Object[] initExtraInfo, long initDllHandle) {
            clazz = initClazz;
            nativeDescriptorName = initNativeDescriptorName;
            extraInfo = initExtraInfo;
            dllHandle = initDllHandle;
        }
    }

    private static final String AUTH_DLL_RELATIVE_PATH = "/lib/native/zos/s390x/bbgzsafm";
    private static final String UNAUTH_DLL_RELATIVE_PATH = "/lib/native/zos/s390x/bbgzsufm";
    private static final String NATIVE_SERVICES_DLL_RELATIVE_PATH = "/lib/native/zos/s390x/libzNativeServices.so";

    private static String libraryName;
    private static String authorizedFunctionDllPath;
    private static String unauthorizedFunctionDllPath;

    /**
     * Map of {@code Class} instance to {@code NativeMethodInfo}. This map
     * can only be used by synchronized instance methods.
     */
    private static Map<Class<?>, NativeMethodInfo> nativeInfo = null;

    static {
        // the script that launches the z/OS native unit test environment sets the following variable;
        // this class is not designed to function in any other environment
        String zosUnitTestEnvStr = System.getenv("ZOS_NATIVE_UNIT_TEST_ENV");
        if (zosUnitTestEnvStr != null && zosUnitTestEnvStr.equalsIgnoreCase("true")) {
            nativeInfo = new HashMap<Class<?>, NativeMethodInfo>();

            // load the native services DLL; the initial load of the DLL will register two of our
            // native methods, "ntv_registerNatives" and "ntv_deregisterNatives"; we then need to
            // use ntv_registerNatives method to register the remaining methods

            // Note: The manual zostestenv test environment uses ZOS_LIBERTY_HOME (it gets
            // set in ant_build/resources/bin/zostestenv.sh), while our regular CT builds use 
            // LIBERTY_HOME (it gets set in ant_build/public_imports/internal_imports/unittest.xml).
            // The reason zostestenv needs its own property is because LIBERTY_HOME depends
            // on ${async.image.output.dir}, which isn't set in the zostestenv environment.
            String libertyHome = System.getenv("ZOS_LIBERTY_HOME");
            if (libertyHome == null || libertyHome.equals("") || libertyHome.equals("null")) {
                // Must be running under our regular CT build infrastructure, not zostestenv.
                libertyHome = System.getenv("LIBERTY_HOME");
            }
            libraryName = libertyHome + NATIVE_SERVICES_DLL_RELATIVE_PATH;
            System.load(libraryName);
            registerNatives(NativeLibraryUtils.class);
            try {
                NativeLibraryTraceHelper.init();
            } catch (Exception e) {
                throw new java.lang.IllegalStateException(e);
            }

            // configure the fully qualified paths to the auth and unath function DLLs, for use
            // later if/when a user of this class wants to load them
            authorizedFunctionDllPath = (new File(libertyHome + AUTH_DLL_RELATIVE_PATH)).getAbsolutePath();
            unauthorizedFunctionDllPath = (new File(libertyHome + UNAUTH_DLL_RELATIVE_PATH)).getAbsolutePath();
        } else {
            throw new java.lang.UnsupportedOperationException("NativeLibraryUtils is only supported in a z/OS native unit test environment.");
        }
    }

    /**
     * Initialize the unittest environment.
     * 
     */
    public static void init() throws Exception {

        //Initilaize the unuttest environment. 
        int rc = 0;
        rc = NativeLibraryTraceHelper.init();
        if (rc != 0) {
            throw new IllegalStateException("NativeLibraryUtils init() failed. rc= " + rc);
        }
    }

    /**
     * Reset the unittest environment to it's initial state.
     * 
     */
    public static void reset() throws Exception {

        //Reset any trace state back to its original pristine state. 
        int rc = 0;
        rc = NativeLibraryTraceHelper.reset();
        if (rc != 0) {
            throw new IllegalStateException("NativeLibraryUtils reset() failed. rc= " + rc);
        }

    }

    /**
     * Register native methods from the core DLL for the specified class.
     * 
     * @param clazz the class to link native methods for
     * 
     * @throws UnsatisfiedLinkError if an error occurs during resolution or registration
     */
    public static void registerNatives(Class<?> clazz) {
        registerNatives(clazz, null, null);
    }

    /**
     * Register native methods from the core DLL for the specified class. The
     * specified object array will be passed along to the registration callback.
     * 
     * @param clazz the class to link native methods for
     * @param extraInfo extra information that will be passed to the registration hook
     * @param bundleVersion a String representation of the bundle version to use, if the
     *            class in question is provided by multiple versions of the same bundle
     * 
     * @throws UnsatisfiedLinkError if an error occurs during resolution or registration
     */
    public static synchronized void registerNatives(Class<?> clazz, Object[] extraInfo, String bundleVersion) {
        // Skip processing if we've already processed this class
        if (nativeInfo.containsKey(clazz)) {
            return;
        }

        // the native code defines a NativeMethodDescriptor struct that contains a set of native
        // methods to register for various classes...  the name of the descriptor follows a loose
        // pattern that starts with zJNI_ but could have various endings, i.e. just the simple name,
        // the full canonical name, an optional version, etc...  so we will create a list of possible
        // descriptor names and attempt to register each one until we succeed (or they all fail)
        final String prefix = "zJNI_";
        final String stem = prefix + clazz.getCanonicalName().replaceAll("\\.", "_");;

        List<String> candidates = new ArrayList<String>();
        if (bundleVersion != null) {
            candidates.add(stem + "__" + bundleVersion);
        }
        candidates.add(stem);
        candidates.add(prefix + clazz.getSimpleName());

        for (String candidate : candidates) {
            long dllHandle = ntv_registerNatives(clazz, candidate, extraInfo);
            if (dllHandle > 0) {
                nativeInfo.put(clazz, new NativeMethodInfo(clazz, candidate, extraInfo, dllHandle));
                return;
            }
        }

        // Failed to resolve the native method descriptor
        throw new UnsatisfiedLinkError("Native method descriptor for " + clazz.getCanonicalName() + " not found");
    }

    protected final static native long ntv_registerNatives(Class<?> clazz, String nativeDescriptorName, Object[] extraInfo);

    /**
     * Deregister native methods from the core DLL for the specified class, if they have
     * already been registered.
     * 
     * @param clazz the class to deregister native methods for
     */
    public static synchronized void deregisterNatives(Class<?> clazz) {
        NativeMethodInfo info = nativeInfo.remove(clazz);
        if (info != null) {
            ntv_deregisterNatives(info.dllHandle, info.clazz, info.nativeDescriptorName, info.extraInfo);
        }
    }

    protected final static native long ntv_deregisterNatives(long dllHandle, Class<?> clazz, String nativeDescriptorName, Object[] extraInfo);

    /**
     * Load the module containing the unauthorized native code.
     * 
     * @return the &quot;load HFS&quot; return code
     */
    public static synchronized Object loadUnauthorized() {
        return ntv_loadUnauthorized(unauthorizedFunctionDllPath);
    }

    private final static native NativeServiceTracker.ServiceResults ntv_loadUnauthorized(String unauthorizedModulePath);

    /**
     * Attempt to register this server with the angel and access the authorized
     * code infrastructure.
     * 
     * @return the return code from server registration
     */
    public static synchronized int registerServer() {
        return ntv_registerServer(authorizedFunctionDllPath, null);
    }

    private final static native int ntv_registerServer(String authorizedModulePath, String angelName);

    /**
     * Deregister this server and tear down the authorized code infrastructure.
     * 
     * @return the deregistration return code
     */
    public static synchronized int deregisterServer() {
        return ntv_deregisterServer();
    }

    private final static native int ntv_deregisterServer();

    // NativeLibraryUtils shares a set of native methods with runtime classes in the com.ibm.ws.zos.core project,
    // however we don't use them all (specifically, we don't use ntv_getNativeServiceEntries).  If we don't declare
    // the method, however, the call to ntv_registerNatives(NativeLibraryUtils.class) will fail because the native
    // code expects us to have the ntv_getNativeServiceEntries method defined.  So we need to declare the method
    // here and suppress the warning since yes, I know we aren't using this method, and yes, I have a good (enough)
    // reason for declaring it anyway.
    @java.lang.SuppressWarnings("unused")
    private final static native int ntv_getNativeServiceEntries(List<String> permittedServices, List<String> deniedServices, List<String> permittedClientServices,
                                                                List<String> deniedClientServices);

    @java.lang.SuppressWarnings("unused")
    private final static native int ntv_getAngelVersion();
}
