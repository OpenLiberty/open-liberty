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
package io.openliberty.crac;

import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.crac.CheckpointException;
import org.crac.Context;
import org.crac.Resource;
import org.crac.RestoreException;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component(property = { Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE,
                        CheckpointHook.MULTI_THREADED_HOOK + ":Boolean=true",
                        "io.openliberty.crac.hooks:Boolean=true" })
public class LibertyCheckpoint implements CheckpointHook {
    private static final TraceComponent tc = Tr.register(LibertyCheckpoint.class);
    private static final Deque<CheckpointHook> hooks = new ConcurrentLinkedDeque<CheckpointHook>();

    @Override
    public void prepare() {
        // called in reverse register order on prepare
        // TODO create translatable message
        callHooks(hooks.descendingIterator(), CheckpointHook::prepare, () -> new CheckpointException("One or more application resources caused checkpoint to fail."));
    }

    @Override
    public void restore() {
        // call in register order on restore
        // TODO create translatable message
        callHooks(hooks.iterator(), CheckpointHook::restore, () -> new RestoreException("One or more application resources caused restore to fail."));
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

    static public void register(Resource r, Context<? extends Resource> c) {
        debug(tc, () -> "Registering resource " + r + " from context " + c);
        CheckpointPhase phase = CheckpointPhase.getPhase();
        if (phase == CheckpointPhase.INACTIVE) {
            return;
        }
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

    public static void checkpointRestore() throws RestoreException, CheckpointException {
        debug(tc, () -> "Requesting an application initiated checkpoint.");
        String errorMessage = "Request to checkpoint is not supported.";
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
