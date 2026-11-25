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
package com.ibm.ws.zos.thread.term.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.thread.term.TerminationHandler;
import com.ibm.ws.zos.thread.term.TerminationManager;

/**
 * Service which allows registration of terminated thread monitors.
 */
public class TerminationManagerImpl implements TerminationManager {
    private static final TraceComponent tc = Tr.register(TerminationManagerImpl.class);

    final static String WS_TERMINATION_HANDLER_REFERENCE_NAME = "terminationHandler";

    /**
     * Flag telling us that we need to register with the native method manager.
     * When the bundle is stopped, the native method manager will unregister
     * our native code. The bundle activator for this bundle will set this flag
     * back to false so that we know that we need to re-register the next time
     * our service is activated.
     */
    static boolean nativeRegistrationCompleted = false;

    /**
     * Reference to the native method manager.
     */
    private NativeMethodManager nativeMethodManager = null;

    /**
     * Currently registered TerminationHandlers.
     */
    private final Map<ServiceReference<?>, TerminationHandlerHolder> registeredHandlers = new HashMap<ServiceReference<?>, TerminationHandlerHolder>();

    /**
     * List of threads that we are monitoring.
     */
    private final List<Thread> threadsToMonitor = new LinkedList<Thread>();

    /**
     * Allow new thread termination requests.
     */
    private boolean active = false;

    /**
     * A place to store the component context for the holder inner class.
     */
    private ComponentContext componentContext = null;

    /**
     * DS method to activate the thread termination service. This active method
     * is used to initialize the service, and also to initialize the JNI and
     * native environment for thread termination events.
     *
     * This code operates on the assumption that if the service is re-activated
     * after it is deactivated, the new activation will occur in a new
     * TerminationManagerImpl object instance.
     */
    protected void activate(ComponentContext componentContext) {
        // -------------------------------------------------------------------
        // When registering the native methods, we need to pass our classloader
        // down to the native code and use it to load classes in this bundle.
        // This is because the native method registration is driven from
        // the native service tracker, which has a different class loader than
        // we do.  So we can't rely on JNI FindClass() to find our classes.
        // -------------------------------------------------------------------
        if (nativeRegistrationCompleted == false) {
            Class<?> myClass = this.getClass();
            ClassLoader myClassLoader = myClass.getClassLoader();
            Object[] o = new Object[] { myClassLoader };

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Calling registerNatives", nativeMethodManager);

            nativeMethodManager.registerNatives(TerminationManagerImpl.class, o);
            nativeRegistrationCompleted = true;
        }

        // -------------------------------------------------------------------
        // Tell the native code that we were activated.
        // -------------------------------------------------------------------
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Calling ntv_activateThreadTermMonitor");

        if (ntv_activateThreadTermMonitor() != 0) {
            throw new RuntimeException("An error occurred when activating the native component");
        }

        // We are active
        this.componentContext = componentContext;
        active = true;
    }

    /**
     * DS method to de-activate this component. This deactivate method is used
     * to stop the thread termination notification thread and destroy the
     * pthread key that the native code uses to be notified when a thread
     * terminates. The JNI and native code is not completely cleaned up until
     * the bundle is deactivated and the native method manager is driven to
     * destroy the native environment.
     */
    protected void deactivate(ComponentContext componentContext) {
        // -------------------------------------------------------------------
        // Prevent any new threads from being added to the termination
        // monitor.  While OSGi will have deactivated all services which
        // depended on us before we are deactivated, some service may have
        // cached a reference to us somewhere else, so we'll cover ourselves
        // against one of them coming in and registering a new thread or
        // handler.
        // -------------------------------------------------------------------
        active = false;
        this.componentContext = null;

        // -------------------------------------------------------------------
        // Tell the native code that we're done with this termination
        // manager.
        // -------------------------------------------------------------------
        ntv_deactivateThreadTermMonitor();

        // -------------------------------------------------------------------
        // Clean up.
        // -------------------------------------------------------------------
        synchronized (registeredHandlers) {
            registeredHandlers.clear();
        }

        synchronized (threadsToMonitor) {
            threadsToMonitor.clear();
        }
    }

    /**
     * Sets the native method manager for JNI processing.
     *
     * @param nativeMethodManager
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * Clears the native method manager instance.
     *
     * @param nativeMethodManager
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = null;
    }

    /**
     * Registers a new termination handler.
     */
    protected void setTerminationHandler(ServiceReference<?> handlerReference) {
        synchronized (registeredHandlers) {
            // Seems like we should be checking if we already have the reference in our map, but
            // we are often called twice for the same handler reference.
            registeredHandlers.put(handlerReference, new TerminationHandlerHolder(handlerReference));
        }
    }

    /**
     * Deregisters a termination handler.
     */
    protected void unsetTerminationHandler(ServiceReference<?> handlerReference) {
        synchronized (registeredHandlers) {
            registeredHandlers.remove(handlerReference);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void registerCurrentThread() {
        if (active) {
            Thread thd = Thread.currentThread();

            synchronized (threadsToMonitor) {
                if (threadsToMonitor.contains(thd) == false) {
                    if (ntv_registerThreadForTermination(thd) != 0) {
                        throw new RuntimeException("Unable to register current thread with pthreads");
                    }

                    threadsToMonitor.add(thd);
                }
            }
        }
    }

    /**
     * Notify handlers that a thread has terminated. This is called from the
     * native code.
     */
    @SuppressWarnings("unused")
    private void notifyThreadTerminated(Thread thread) {
        synchronized (registeredHandlers) {
            Collection<TerminationHandlerHolder> holders = registeredHandlers.values();
            for (TerminationHandlerHolder holder : holders) {
                TerminationHandler handler = holder.getHandler();
                if (handler != null) {
                    handler.threadTerminated(thread);
                }
            }
        }

        synchronized (threadsToMonitor) {
            threadsToMonitor.remove(thread);
        }
    }

    /**
     * A holder that lets us resolve the termination handler implementations
     * at runtime rather than at service activation time.
     */
    private class TerminationHandlerHolder {
        private final ServiceReference<?> ref;
        private TerminationHandler handler = null;

        private TerminationHandlerHolder(ServiceReference<?> ref) {
            this.ref = ref;
        }

        private TerminationHandler getHandler() {
            if (handler == null) {
                Object svc = componentContext.locateService(WS_TERMINATION_HANDLER_REFERENCE_NAME, ref);
                handler = (TerminationHandler) svc;
            }

            return handler;
        }
    }

    /**
     * Activates the native portion of the thread termination manager.
     *
     * @return 0 on success, nonzero on failure.
     */
    private native int ntv_activateThreadTermMonitor();

    /**
     * Deactivates the native portion of the thread termination manager.
     * The thread termination monitor thread will continue to run until the
     * bundle is deactivated, but no terminated thread notifications will be
     * generated.
     */
    private native void ntv_deactivateThreadTermMonitor();

    /**
     * Registers a thread with pthreads so that we are notified when
     * the thread terminates. The thread must be the current thread.
     *
     * @param thread The thread object to register for termination. This
     *                   must represent the current thread.
     *
     * @return 0 on success, nonzero on failure.
     */
    private native int ntv_registerThreadForTermination(Thread thd);
}
