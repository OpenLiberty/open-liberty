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
package com.ibm.ws.zos.core.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.core.thread.internal.ThreadTracker;
import com.ibm.ws.zos.core.utils.internal.NativeUtilsImpl;
import com.ibm.ws.zos.jni.internal.NativeMethodManagerImpl;
import com.ibm.ws.zos.logging.internal.NativeTraceHandler;
import com.ibm.ws.zos.registration.internal.ProductManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 * Bundle activator for the z/OS core services. This bundle starts as part of
 * the Liberty platform before declarative services is active.
 */
@Component
public class CoreBundleActivator {

    /**
     * Trace component used to issue messages.
     */
    private static final TraceComponent tc = Tr.register(CoreBundleActivator.class);

    /**
     * The hosting bundle's context.
     */
    BundleContext bundleContext;

    /**
     * The native method management code that we delegate to. The delegation
     * pattern is used to avoid a race condition between the core component
     * activation and the method manager's registration.
     */
    NativeMethodManagerImpl nativeMethodManager;

    /**
     * Reference to the &quot;component&quot; that writes native trace records
     * through the java logging system.
     */
    NativeTraceHandler nativeTraceHandler;

    /**
     * Reference to the component that registers mock services that represent
     * authorized native services.
     */
    NativeServiceTracker nativeServiceTracker;

    /**
     * Reference to the component that tracks Java thread life cycle events.
     */
    ThreadTracker threadTracker;

    /**
     * The product registration code.
     */
    ProductManager productManager;

    /**
     * Reference to the HardFailureNativeCleanup. This class provides a means of identifying
     * a Hard server failure, and thus, triggering native cleanup that may prevent Server
     * termination hangs (ex. will detect and release AsyncIO paused I/O completion Handler
     * threads).
     */
    HardFailureNativeCleanup hardFailureNativeCleanup;

    /**
     * Native utilities class instance.
     */
    NativeUtilsImpl nativeUtils;

    /**
     * Variable registry for accessing bootstrap properties.
     */
    private VariableRegistry variableRegistry = null;

    public void activate(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;

        // Instantiate the native method manager implementation that's needed
        // in order to load the native code
        nativeMethodManager = createNativeMethodManager();

        // Instantiate and activate the native trace handler so we can
        // get some tracing from the native code
        nativeTraceHandler = new NativeTraceHandler(nativeMethodManager);
        nativeTraceHandler.startTraceHandlerThread();

        // Instantiate native utilities. It registers with OSGI during start.
        nativeUtils = new NativeUtilsImpl(nativeMethodManager);
        nativeUtils.start(bundleContext);

        // Instantiate and activate the service tracker.  This will cause the
        // server to try to register with the angel.  If registration is
        // successful, the service registry will be populate with services
        // that represent the authorized native services
        nativeServiceTracker = new NativeServiceTracker(nativeMethodManager, variableRegistry);
        nativeServiceTracker.start(bundleContext);

        // Instantiate and activate the HardFailureNativeCleanup
        hardFailureNativeCleanup = new HardFailureNativeCleanup(nativeMethodManager);
        hardFailureNativeCleanup.start();

        // Create the thread event listener
        threadTracker = new ThreadTracker(nativeMethodManager);
        threadTracker.start(bundleContext);

        // Prepare product registration services
        productManager = new ProductManager(nativeMethodManager);
        productManager.start(bundleContext);

        // Make the native method manager available to external users
        nativeMethodManager.start(bundleContext);

        // Print the current UMASK associated with the server process.
        recordUmaks();
    }

    public void deactivate(BundleContext bundleContext) throws Exception {
        // Stop the product manager
        productManager.stop(bundleContext);

        // Stop the HardFailureNativeCleanup
        hardFailureNativeCleanup.stop();

        // Unregister all of the authorized services before we shut down
        nativeServiceTracker.stop(bundleContext);
        nativeServiceTracker = null;

        // Stop the thread tracker
        threadTracker.stop(bundleContext);

        // Stop native utils.
        nativeUtils.stop(bundleContext);
        nativeUtils = null;

        // Stop the native trace writer before the native environment gets
        // torn down
        nativeTraceHandler.stopTraceHandlerWriter();
        nativeTraceHandler = null;

        // Stop the native environment
        nativeMethodManager.stop(bundleContext);
        nativeMethodManager = null;
    }

    /**
     * Create the native method manager instance that we delegate to. This is
     * a layer of indirection to enable more complete unit test.
     */
    protected NativeMethodManagerImpl createNativeMethodManager() {
        return new NativeMethodManagerImpl(bundleContext);
    }

    /**
     * Records this process' UMASK under message CWWKB0121I.
     * The UMASK to be displayed is composed of 4 numeric characters of the form 'SUGO'.
     * Where:
     * S = Special permission octet.
     * U = User Octet.
     * G = Group Octet.
     * O = Other octet.
     */
    private void recordUmaks() {
        try {
            int decimalUmask = nativeUtils.getUmask();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The server curent umask in decimal is: ", decimalUmask);
            }

            // Decimal values are padded and converted to a user friendly UMASK:
            // Format(%04o): 0 = padding, 4 = length, o = octect representation.
            Tr.info(tc, "SERVER_CURRENT_UMASK", String.format("%04o", decimalUmask));
        } catch (Throwable t) {
            // Best effort approach. Record an FFDC.
        }
    }

    /**
     * @param objs
     * @return The first non-null parm, or null if they're all null.
     *
     *         Note: Used by NativeServiceTracker and ProductManager in this project.
     */
    public static <T> T firstNotNull(T... objs) {
        for (T obj : objs) {
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    @Reference
    public void setVariableRegistry(VariableRegistry variableRegistry) {
        this.variableRegistry = variableRegistry;
    }

    public void unsetVariableRegistry(VariableRegistry variableRegistry) {
        this.variableRegistry = null;
    }
}
