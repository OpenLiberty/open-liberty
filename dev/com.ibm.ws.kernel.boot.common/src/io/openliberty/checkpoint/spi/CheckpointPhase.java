/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Phase which a checkpoint of the running process is being taken.
 * This enum is registered as a service when running in an OSGi framework.
 *
 */
public enum CheckpointPhase {
    /**
     * Phase of startup after all configured applications have been started
     * or have timed out starting. No ports are opened yet
     */
    AFTER_APP_START,
    /**
     * Phase of startup after the applications have been discovered but no
     * code from the application has been executed.
     */
    BEFORE_APP_START,
    /**
     * Phase that indicates there is no checkpoint being done.
     */
    INACTIVE(true);

    /**
     * The checkpoint phase service property used to store the checkpoint phase when registered as a service.
     */
    public static final String CHECKPOINT_PROPERTY = "io.openliberty.checkpoint";

    /**
     * A service filter to lookup an active checkpoint phase.
     */
    public static final String CHECKPOINT_ACTIVE_FILTER = "(!(" + CHECKPOINT_PROPERTY + "=INACTIVE))";

    /**
     * The checkpoint phase service property used to store if the checkpoint process has been restored.
     * Initially this property will be set to false. When the process is restored this will change to true.
     */
    public static final String CHECKPOINT_RESTORED_PROPERTY = "io.openliberty.checkpoint.restored";

    /**
     * The ID of the condition service that indicates the Liberty process is running. A Liberty process
     * is considered running if Liberty was launched with no checkpoint or if the Liberty process
     * has been restored from a checkpoint image. When Liberty is launched to create a checkpoint image
     * this condition is not registered until the Liberty process is restored from that checkpoint
     * image.
     */
    public static final String CONDITION_PROCESS_RUNNING_ID = "io.openliberty.process.running";

    /**
     *
     */
    private CheckpointPhase() {
        this(false);
    }

    private CheckpointPhase(boolean inactive) {
        if (inactive) {
            // if inactive then set restored and noMoreAddHooks to true;
            restored = true;
            noMoreAddHooks = true;
        }
    }

    private volatile boolean restored = false;
    private volatile boolean noMoreAddHooks = false;
    private final List<CheckpointHook> singleThreadedHooks = new ArrayList<>();
    private final List<CheckpointHook> multiThreadedHooks = new ArrayList<>();

    /**
     * Convert a String to a checkpoint phase and then set the phase
     * as the checkpoint phase in progress.
     *
     * @param p The string value
     * @return The matching phase or null if there is no match. String comparison
     *         is case insensitive.
     */
    static synchronized void setPhase(String p) {
        if (THE_PHASE != INACTIVE) {
            // This should only be called once. Ignore if called multiple times.
            // For embedded launches this may be called multiple times.
            return;
        }
        debug(() -> "phase set to: " + p);
        CheckpointPhase phase = INACTIVE;
        if (p != null) {
            try {
                phase = CheckpointPhase.valueOf(p.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore, will default to INACTIVE
            }
        }
        THE_PHASE = phase;
    }

    static CheckpointPhase THE_PHASE = CheckpointPhase.INACTIVE;

    /**
     * Returns true if the process has been restored
     *
     * @return true if the process has been restored
     */
    final public boolean restored() {
        return this == INACTIVE || restored;
    }

    /**
     * Adds a {@code CheckpointHook} for this checkpoint phase.
     * The hook will be run to {@link CheckpointHook#prepare() prepare}
     * and {@link CheckpointHook#restore() restore} a checkpoint
     * process while the JVM is in single-threaded mode. If the
     * checkpoint process is already {@link #restored() restored}
     * then the hook is not added and {@code false} is returned,
     * indicating that the hook will not be called.
     * A return value of {@code true} indicates that the hook will
     * be called for both {@link CheckpointHook#prepare() prepare}
     * and {@link CheckpointHook#restore() restore}.
     *
     *
     * @param hook the hook to be used to prepare and restore
     *                 a checkpoint process.
     * @throws IllegalStateException if the phase is not in progress.
     * @return true if the hook is successfully added; otherwise false
     *         is returned.
     * @see CheckpointHook#MULTI_THREADED_HOOK
     */
    final public boolean addSingleThreadedHook(CheckpointHook hook) {
        return addHook(hook, false);
    }

    /**
     * Adds a {@code CheckpointHook} for this checkpoint phase.
     * The hook will be run to {@link CheckpointHook#prepare() prepare}
     * and {@link CheckpointHook#restore() restore} a checkpoint
     * process while the JVM is in multi-threaded mode. If the
     * checkpoint process is already {@link #restored() restored}
     * then the hook is not added and {@code false} is returned,
     * indicating that the hook will not be called.
     * A return value of {@code true} indicates that the hook will
     * be called for both {@link CheckpointHook#prepare() prepare}
     * and {@link CheckpointHook#restore() restore}.
     *
     * @param hook the hook to be used to prepare and restore
     *                 a checkpoint process.
     * @throws IllegalStateException if the phase is not in progress.
     * @return true if the hook is successfully added; otherwise false
     *         is returned.
     * @see CheckpointHook#MULTI_THREADED_HOOK
     */
    final public boolean addMultiThreadedHook(CheckpointHook hook) {
        return addHook(hook, true);
    }

    private synchronized boolean addHook(CheckpointHook hook, boolean multiThreaded) {
        if (this != THE_PHASE) {
            throw new IllegalStateException("Cannot add hooks to a checkpoint phase that is not in progress.");
        }
        if (this == INACTIVE) {
            return false;
        }

        if (noMoreAddHooks) {
            return false;
        }

        if (restored) {
            return false;
        }
        debug(() -> "Hook added: " + hook + " " + multiThreaded);
        if (multiThreaded) {
            multiThreadedHooks.add(hook);
        } else {
            singleThreadedHooks.add(hook);
        }
        return true;
    }

    private synchronized List<CheckpointHook> getAndClearHooks(boolean multiThreaded) {
        debug(() -> "Calling getHooks: " + multiThreaded);
        // once we get any hooks do not allow more adds
        noMoreAddHooks = true;
        List<CheckpointHook> current = multiThreaded ? multiThreadedHooks : singleThreadedHooks;
        ArrayList<CheckpointHook> hooks = new ArrayList<>(current);
        current.clear();
        return hooks;
    }

    /**
     * Returns the checkpoint phase in progress.
     * The phase returned is constant for the lifetime of the process.
     *
     * @return the current checkpoint phase in progress.
     */
    public static synchronized CheckpointPhase getPhase() {
        return THE_PHASE;
    }

    /**
     * A function to call on restore after the JVM has reentered multi-threaded
     * mode.
     *
     * @param <T> Exception thrown by the call method
     */
    @FunctionalInterface
    public static interface OnRestore<T extends Throwable> {
        /**
         * Called after on restore after the JVM has reentered multi-threaded
         * mode.
         *
         * @throws T on a restore error. Any exception throw will result in a failure
         *               to restore the process.
         */
        public void call() throws T;
    }

    /**
     * Runs the given function on restore if the runtime has been configured to perform
     * a checkpoint, the function is run after the JVM has re-entered multi-threaded mode;
     * otherwise the function is run immediately from the calling thread synchronously.
     *
     * @param <T>   The type of throwable the function may throw
     * @param toRun The function to run on restore.
     * @throws T any errors that occur while running the function. If an exception is thrown during
     *               restore then the process restore will fail.
     */
    public static <T extends Throwable> void onRestore(OnRestore<T> toRun) throws T {
        CheckpointPhase phase = getPhase();
        if (phase.restored()) {
            // Already restored or not doing a checkpoint; call toRun now
            toRun.call();
            return;
        }
        // On the checkpoint side; try to add the hook to call toRun on restore
        if (!phase.addMultiThreadedHook(new CheckpointHook() {
            @Override
            public void restore() {
                try {
                    toRun.call();
                } catch (Throwable e) {
                    sneakyThrow(e);
                }
            }
        })) {
            // Hook did not get added successfully; call toRun now
            toRun.call();
        }
    }

    @SuppressWarnings("unchecked")
    static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    private final static boolean DEBUG = debugEnabled();

    private static boolean debugEnabled() {
        return System.getProperty("io.openliberty.checkpoint.debug") != null;
    }

    /**
     * Print debug message to system out. The normal trace in liberty is not available
     * this low.
     *
     * @param message the message supplier
     */
    private static void debug(Supplier<String> message) {
        if (DEBUG) {
            System.out.println("DEBUG CheckpointPhase: current phase - " + String.valueOf(THE_PHASE) + " - " + message.get());
        }
    }

    final CheckpointHook createCheckpointHook(boolean multiThreaded) {
        return new CheckpointPhaseHookImpl(multiThreaded);
    }

    final static class CheckpointPhaseHookImpl implements CheckpointHook {
        private volatile List<CheckpointHook> hooks = Collections.emptyList();
        private final boolean multiThreaded;

        CheckpointPhaseHookImpl(boolean multiThreaded) {
            this.multiThreaded = multiThreaded;
        }

        @Override
        public void prepare() {
            CheckpointPhase phase = CheckpointPhase.getPhase();
            debug(() -> "prepare phase: " + phase);

            hooks = phase.getAndClearHooks(multiThreaded);

            for (CheckpointHook hook : hooks) {
                debug(() -> "prepare operation on static " + (multiThreaded ? "multi-threaded" : "single-threaded") + " hook: " + hook);
                hook.prepare();
            }
            // reverse for restore call during checkpoint
            Collections.reverse(hooks);
        }

        @Override
        public void checkpointFailed() {
            for (CheckpointHook hook : hooks) {
                debug(() -> "checkpointFailed operation on static hook: " + hook);
                try {
                    hook.checkpointFailed();
                } catch (Throwable t) {
                    // ignore
                }
            }
        }

        @Override
        public void restore() {
            for (CheckpointHook hook : hooks) {
                debug(() -> "restore operation on static " + (multiThreaded ? "multi-threaded" : "single-threaded") + " hook: " + hook);
                hook.restore();
            }
            // release references to hooks now
            hooks.clear();
        }
    }
}