/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.crac;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Deque;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/*
 * Using package private class here to avoid adding to the API JAR.
 * NOTE: package private classes cannot be service components;
 *       therefore this class programmatically registers the CheckpointHook
 */
class LibertyCheckpoint implements CheckpointHook {
    private static final TraceComponent tc = Tr.register(LibertyCheckpoint.class);
    private static final Deque<CheckpointHook> hooks = new ConcurrentLinkedDeque<CheckpointHook>();
    private static final AtomicBoolean registerHookService = new AtomicBoolean(false);

    @Override
    public void prepare() {
        // called in reverse register order on prepare
        callHooks(hooks.descendingIterator(), CheckpointHook::prepare, () -> new CheckpointException(Tr.formatMessage(tc, "CRAC_RESOURCE_CHECKPOINT_FAIL_CWWKC0551")));
    }

    @Override
    public void checkpointFailed() {
        // CRaC says to call the afterRestore on each resource when checkpoint fails;
        // simply doing the restore calls for this case.
        restore();
    }

    @Override
    public void restore() {
        // call in register order on restore
        callHooks(hooks.iterator(), CheckpointHook::restore, () -> new RestoreException(Tr.formatMessage(tc, "CRAC_RESOURCE_RESTORE_FAIL_CWWKC0552")));
        // after restore we can clear the hooks because they are no longer needed.
        hooks.clear();
    }

    @FFDCIgnore(Exception.class)
    private void callHooks(Iterator<CheckpointHook> toCall, Consumer<CheckpointHook> method, Supplier<Exception> toThrow) {
        Exception createdToThrow = null;
        while (toCall.hasNext()) {
            try {
                method.accept(toCall.next());
            } catch (Exception e) {
                if (createdToThrow == null) {
                    createdToThrow = toThrow.get();
                }
                createdToThrow.addSuppressed(e);
            }
        }
        if (createdToThrow != null) {
            createdToThrow.fillInStackTrace();
            sneakyThrow(createdToThrow);
        }
    }

    static void register(Resource r, Context<? extends Resource> c) {
        debug(tc, () -> "Registering resource " + r + " from context " + c);
        CheckpointPhase phase = CheckpointPhase.getPhase();
        if (phase == CheckpointPhase.INACTIVE) {
            return;
        }
        registerHookService();
        final WeakReference<Resource> ref = new WeakReference<>(r);
        CheckpointHook hook = new CheckpointHook() {
            @Override
            @FFDCIgnore(Exception.class)
            public void prepare() {
                try {
                    Resource current = ref.get();
                    if (current != null) {
                        debug(tc, () -> "Calling beforeCheckpoint on " + current);
                        current.beforeCheckpoint(c);
                    }
                } catch (Exception e) {
                    sneakyThrow(e);
                }
            }

            @Override
            @FFDCIgnore(Exception.class)
            public void restore() {
                try {
                    Resource current = ref.get();
                    if (current != null) {
                        debug(tc, () -> "Calling afterRestore on " + current);
                        current.afterRestore(c);
                    }
                } catch (Exception e) {
                    sneakyThrow(e);
                }
            }
        };
        hooks.addLast(hook);
    }

    static void checkpointRestore() throws RestoreException, CheckpointException {
        debug(tc, () -> "Requesting an application initiated checkpoint.");
        String errorMessage = Tr.formatMessage(tc, "CRAC_RESOURCE_REQUEST_CHECKPOINT_CWWKC0553");
        final Exception fail = CheckpointPhase.INACTIVE == CheckpointPhase.getPhase() ? new UnsupportedOperationException(errorMessage) : new CheckpointException(errorMessage);
        fail.fillInStackTrace();
        // Add a hook to fail checkpoint in case afterAppStart checkpoint;
        // We don't want to risk a bad application state after throwing the following exception
        CheckpointPhase.getPhase().addMultiThreadedHook(new CheckpointHook() {
            @Override
            public void prepare() {
                sneakyThrow(fail);
            }
        });
        sneakyThrow(fail);
    }

    private static void registerHookService() {
        if (registerHookService.compareAndSet(false, true)) {
            Bundle b = FrameworkUtil.getBundle(LibertyCheckpoint.class);
            if (b == null) {
                throw new IllegalStateException("No bundle for LibertyCheckpoint class.");
            }
            BundleContext bc = AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> b.getBundleContext());
            if (bc == null) {
                throw new IllegalStateException("No bundle context for LibertyCheckpoint class.");
            }
            Hashtable<String, Object> serviceProps = new Hashtable<>();
            serviceProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
            serviceProps.put(CheckpointHook.MULTI_THREADED_HOOK, Boolean.TRUE);
            serviceProps.put("io.openliberty.crac.hooks", Boolean.TRUE);
            bc.registerService(CheckpointHook.class, new LibertyCheckpoint(), serviceProps);
        }
    }

    @Trivial
    static void debug(TraceComponent trace, Supplier<String> message) {
        if (TraceComponent.isAnyTracingEnabled() && trace.isDebugEnabled()) {
            Tr.debug(trace, message.get());
        }
    }

    @SuppressWarnings("unchecked")
    @Trivial
    static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
