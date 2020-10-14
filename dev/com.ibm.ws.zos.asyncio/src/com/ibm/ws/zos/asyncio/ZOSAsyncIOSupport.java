/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.asyncio;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.io.async.IAsyncProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.jni.AngelUtils;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 * Used to determine whether the ZOSAIO native services are registered.
 */
@Component(service = { IAsyncProvider.AsyncIOHelper.class })
//, WsByteBufferPoolManager.DirectByteBufferHelper.class })
public class ZOSAsyncIOSupport implements IAsyncProvider.AsyncIOHelper {
    // , WsByteBufferPoolManager.DirectByteBufferHelper {

    //TODO: Disabled support for native routines for allocateDirectByteBuffer and releaseDirectByteBuffer.  This is
    // linked to a native heap leak from users of WsByteBuffer support.  Until the leak(s) have been fixed with this
    // support we can not enable it.  FWIW, prior to the Open Liberty split this support was not enabled.  It
    // may have been disabled by accident but it was not enabled when AsyncIO support was released (pre-17002, the OSGI
    // dependencies between the ByteBufferConfiguration and CHFWBundle prevents setting of the NativeMethodManager
    // service into ChannelFrameworkImpl -- which is the trigger on whether to use the ZOSByteBufferPoolManager code).

    //    @Override
    //    public native ByteBuffer allocateDirectByteBuffer(long size);
    //
    //    @Override
    //    public native void releaseDirectByteBuffer(ByteBuffer buffer);

    /** Trace service */
    private static final TraceComponent tc = Tr.register(ZOSAsyncIOSupport.class);

    // All resources and config are available and activated for the use of AsyncIO on z/OS.
    private static boolean isZosaioEnabled = false;
    private static final Object LOCK = new Object();

    // Property to disable AsyncIO even if all other conditions are meet (ex. racf, angel running, )
    private static boolean asyncIOEnabledProperty = true;

    public static final String AIOENABLE_PROPERTYNAME = "com.ibm.ws.tcpchannel.useZosAio";

    private AngelUtils angelUtils;
    private NativeMethodManager nativeMethodManager;
    private VariableRegistry variableRegistry;

    /**
     * Recovery requirement. The AsyncIO native code has a logical dependency on the
     * HardFailureNativeCleanup service. When AsyncIO is init'd it will register a native
     * AsyncIO cleanup routine into the server_process_data structure. Our native task-level resmgr
     * will check for a "marked" Thread/TCB going through termination. If it sees such a "marked"
     * Thread it will drive the registered native cleanup routines. Seeing the "marked" Thread
     * indicates that this server has taken a Hard failure (such as a Kill -9 or a Runtime.halt(),
     * or a unrecoverable native abend). So, we need this Service dependency on HardFailureNativeCleanup
     * which is the Service that manages (starts and stops) the native "marked" Thread to trigger
     * the cleanup to prevent hung servers.
     */

    /**
     * @return true if ZOSAIO is enabled
     */
    public static boolean isZosAioRegistered() {
        synchronized (LOCK) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "isZosaioEnabled = " + isZosaioEnabled);
            }
            return isZosaioEnabled;
        }
    }

    /**
     * DS method for activating this component.
     *
     * @param context
     */
    @Activate
    protected void activate(Map<String, Object> props) {
        synchronized (LOCK) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Activating ZOSAIO");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "props: " + props);
            }

            String asyncIOEnabledString = variableRegistry.resolveRawString("${" + AIOENABLE_PROPERTYNAME + "}");
            if (asyncIOEnabledString != null && asyncIOEnabledString.equalsIgnoreCase("false")) {
                asyncIOEnabledProperty = false;
            }

            if (asyncIOEnabledProperty) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "ZOSAIO config is enabled");
                }

                // Check for authorized services
                if (this.isAsyncIOAuthorized()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "ZOSAIO is authorized and enabled");
                    }

                    isZosaioEnabled = true;

                    // Issue ZOSAIO activated message
                    Tr.info(tc, "ZOSAIO_ACTIVATED");
                }
            }
        }
    }

    protected boolean isAsyncIOAuthorized() {
        // Note: The following list must match the list defined in native code server_authorized_functions.def for
        //       AsyncIO services.
        Set<String> asyncIOServiceNames = new HashSet<String>(Arrays.asList("AIOINIT",
                                                                            "AIOCONN",
                                                                            "AIOIOEV2",
                                                                            "AIOCALL",
                                                                            "AIOCLEAR",
                                                                            "AIOCANCL",
                                                                            "AIOSHDWN",
                                                                            "AIOCPORT",
                                                                            "AIOGSOC"));

        // Check that each required authorized service for AsyncIO is present
        return angelUtils.areServicesAvailable(asyncIOServiceNames);
    }

    /**
     * DS method for deactivating this component.
     *
     * @param context
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        synchronized (LOCK) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Deactivating ZOSAIO");
            }

            // If we activated then issue the deactivated message
            if (isZosaioEnabled) {
                // Issue ZOSAIO deactivated message
                Tr.info(tc, "ZOSAIO_DEACTIVATED");
            }

            isZosaioEnabled = false;
        }
    }

    @Reference
    protected void setAngelUtils(AngelUtils angelUtils) {
        this.angelUtils = angelUtils;
    }

    protected void unsetAngelUtils(AngelUtils angelUtils) {
        this.angelUtils = null;
    }

    @Reference
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
        // Disable the native support for allocateDirectByteBuffer and releaseDirectByteBuffer
        //nativeMethodManager.registerNatives(ZOSAsyncIOSupport.class);
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = null;
    }

    @Reference
    protected void setVariableRegistry(VariableRegistry variableRegistry) {
        this.variableRegistry = variableRegistry;
    }

    protected void unsetVariableRegistry(VariableRegistry variableRegistry) {
        this.variableRegistry = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean enableAsyncIO() {
        return ZOSAsyncIOSupport.isZosAioRegistered();
    }

    /** {@inheritDoc} */
    @Override
    public void loadLibrary(Class<? extends IAsyncProvider> providerClass, String libraryName) {
        nativeMethodManager.registerNatives(providerClass);
    }
}
