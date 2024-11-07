/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
     * The ID of the condition service that indicates the Liberty process will have a checkpoint done
     * in the future. This condition will be unregistered just before the JVM goes into single-thread
     * mode to prepare for the checkpoint.
     */
    public static final String CONDITION_BEFORE_CHECKPOINT_ID = "io.openliberty.before.checkpoint";

    private static final String SERVICE_RANKING = "service.ranking";

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
        }
    }

    private volatile boolean restored = false;
    private boolean blockAddHooks = false;
    final Map<Integer, StaticCheckpointHook> singleThreadedHooks = new HashMap<>();
    final Map<Integer, StaticCheckpointHook> multiThreadedHooks = new HashMap<>();
    WeakReference<Object> contextRef = new WeakReference<>(null);
    Method registerService;
    Method unregisterService;

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
        if (phase != INACTIVE) {
            phase.addMultiThreadedHook(Integer.MIN_VALUE, new CheckpointHook() {
                @Override
                public void prepare() {
                    debug(() -> "Locking checkpoint write lock for prepare");
                    checkpointLock.writeLock().lock();
                };

                @Override
                public void restore() {
                    debug(() -> "Unlocking checkpoint write lock for restore");
                    try {
                        checkpointLock.writeLock().unlock();
                    } catch (IllegalMonitorStateException e) {
                        // ignore
                    }
                }

                @Override
                public void checkpointFailed() {
                    debug(() -> "Unlocking checkpoint write lock for checkpointFailed");
                    try {
                        checkpointLock.writeLock().unlock();
                    } catch (IllegalMonitorStateException e) {
                        // ignore
                    }
                }
            });
        }
    }

    static CheckpointPhase THE_PHASE = CheckpointPhase.INACTIVE;

    /**
     * Returns true if the process has been restored or if Liberty was
     * launched with no checkpoint action (the {@link #INACTIVE} checkpoint phase).
     *
     * @return true if the process has been restored or this is the
     *         {@link #INACTIVE} checkpoint phase.
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
        return addHook(hook, false, 0);
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
     * @param rank The rank to run the hook, lower rank is run last
     *                 on checkpoint and first on restore
     * @param hook the hook to be used to prepare and restore
     *                 a checkpoint process.
     * @throws IllegalStateException if the phase is not in progress.
     * @return true if the hook is successfully added; otherwise false
     *         is returned.
     * @see CheckpointHook#MULTI_THREADED_HOOK
     */
    final public boolean addSingleThreadedHook(int rank, CheckpointHook hook) {
        return addHook(hook, false, rank);
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
        return addHook(hook, true, 0);
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
     * @param rank The rank to run the hook, lower rank is run last
     *                 on checkpoint and first on restore
     * @param hook the hook to be used to prepare and restore
     *                 a checkpoint process.
     * @throws IllegalStateException if the phase is not in progress.
     * @return true if the hook is successfully added; otherwise false
     *         is returned.
     * @see CheckpointHook#MULTI_THREADED_HOOK
     */
    final public boolean addMultiThreadedHook(int rank, CheckpointHook hook) {
        return addHook(hook, true, rank);
    }

    private synchronized boolean addHook(CheckpointHook hook, boolean multiThreaded, int rank) {
        if (this != THE_PHASE) {
            throw new IllegalStateException("Cannot add hooks to a checkpoint phase that is not in progress.");
        }
        if (this == INACTIVE) {
            return false;
        }
        if (restored) {
            return false;
        }
        if (blockAddHooks) {
            return false;
        }

        // Make sure the multi-thread max rank is created to unregister hooks on prepare.
        // This is done in a single multi-thread hook to avoid deadlocks on unregister.
        StaticCheckpointHook unregisterStaticHooks = multiThreadedHooks.computeIfAbsent(Integer.MAX_VALUE, (r) -> createStaticCheckpointHook(Integer.MAX_VALUE, true, null));

        debug(() -> "Adding hook: " + hook + (multiThreaded ? " multi-threaded" : " single-threaded") + " rank: " + rank);
        Map<Integer, StaticCheckpointHook> addToHooks = multiThreaded ? multiThreadedHooks : singleThreadedHooks;
        StaticCheckpointHook staticCheckpointHook = addToHooks.computeIfAbsent(rank, (r) -> createStaticCheckpointHook(rank, multiThreaded, unregisterStaticHooks));
        return staticCheckpointHook.addHook(hook);
    }

    private StaticCheckpointHook createStaticCheckpointHook(int rank, boolean multiThreaded, StaticCheckpointHook unregisterStaticHooks) {
        final StaticCheckpointHook newHook = new StaticCheckpointHook(rank, multiThreaded);
        Object context = contextRef.get();
        if (context != null) {
            newHook.register(context, unregisterStaticHooks);
        }
        return newHook;
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
     * A function to call with the checkpoint lock
     *
     * @param <T> Exception thrown by the call method
     */
    @FunctionalInterface
    public static interface WithCheckpointLock<R, T extends Throwable> {
        /**
         * Called with the checkpoint lock to prevent the JVM from entering single-threaded
         * mode.
         *
         * @throws T with checkpoint lock exception.
         */
        public R call() throws T;
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
        onRestore(0, toRun);
    }

    /**
     * Runs the given function on restore if the runtime has been configured to perform
     * a checkpoint, the function is run after the JVM has re-entered multi-threaded mode;
     * otherwise the function is run immediately from the calling thread synchronously.
     *
     * @param <T>   The type of throwable the function may throw
     * @param rank  The rank to run the hook on restore, lower rank is run first
     * @param toRun The function to run on restore.
     * @throws T any errors that occur while running the function. If an exception is thrown during
     *               restore then the process restore will fail.
     */
    public static <T extends Throwable> void onRestore(int rank, OnRestore<T> toRun) throws T {
        CheckpointPhase phase = getPhase();
        if (phase.restored()) {
            debug(() -> "Already restored, calling hook now: " + toRun);
            // Already restored or not doing a checkpoint; call toRun now
            toRun.call();
            return;
        }
        // On the checkpoint side; try to add the hook to call toRun on restore
        if (!phase.addMultiThreadedHook(rank, new CheckpointHook() {
            @Override
            public void restore() {
                try {
                    toRun.call();
                } catch (Throwable e) {
                    sneakyThrow(e);
                }
            }

            @Override
            public String toString() {
                return toRun.toString();
            }
        })) {
            debug(() -> "Hook not added, calling hook now: " + toRun);
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
            System.out.println("DEBUG CheckpointPhase:" + message.get());
        }
    }

    final synchronized void setContext(Object context) throws NoSuchMethodException {
        if (this == INACTIVE) {
            // don't need anything unless we know we are going to do a checkpoint
            return;
        }
        this.contextRef = new WeakReference<Object>(context);
        registerService = context.getClass().getMethod("registerService", Class.class, Object.class, Dictionary.class);
        unregisterService = registerService.getReturnType().getMethod("unregister");

        if (multiThreadedHooks.isEmpty() && singleThreadedHooks.isEmpty()) {
            // nothing to do
            return;
        }
        // Need to register any static hooks added before the context is set.
        // The MAX rank hook for unregistering static hook registrations must exist already
        StaticCheckpointHook unregisterStaticHooks = multiThreadedHooks.get(Integer.MAX_VALUE);
        if (unregisterStaticHooks == null) {
            throw new IllegalStateException("Expected max rank multi-thread hook to exist already.");
        }
        // register any hooks already added
        for (Map.Entry<Integer, StaticCheckpointHook> hook : multiThreadedHooks.entrySet()) {
            hook.getValue().register(context, unregisterStaticHooks);
        }
        for (Map.Entry<Integer, StaticCheckpointHook> hook : singleThreadedHooks.entrySet()) {
            hook.getValue().register(context, unregisterStaticHooks);
        }
    }

    final synchronized void blockAddHooks() {
        this.blockAddHooks = true;
    }

    private static final ReentrantReadWriteLock checkpointLock = new ReentrantReadWriteLock();

    /**
     * Runs the given function while holding the checkpoint lock. This will prevent
     * the JVM from entering single-thread mode when preparing for a checkpoint.
     *
     * @param <R>                The type of value returned by the function
     * @param <T>                The type of throwable the function may throw
     * @param withCheckpointLock The function to run
     * @return The value returned by the function
     * @throws T Any errors that occur while running the function
     */
    public <R, T extends Throwable> R runWithCheckpointLock(WithCheckpointLock<R, T> withCheckpointLock) throws T {
        if (restored()) {
            debug(() -> "Calling with no checkpoint lock: " + withCheckpointLock);
            // call with no read lock on checkpoint
            return withCheckpointLock.call();
        }
        checkpointLock.readLock().lock();
        try {
            // call with checkpoint lock to prevent the JVM from going into single-thread mode while running
            debug(() -> "Calling with checkpoint lock: " + withCheckpointLock);
            return withCheckpointLock.call();
        } finally {
            checkpointLock.readLock().unlock();
        }
    }

    final class StaticCheckpointHook implements CheckpointHook {
        private final List<CheckpointHook> addedHooks = new ArrayList<>();
        private final int rank;
        private final boolean multiThreaded;
        private volatile List<CheckpointHook> preparedHooks = Collections.emptyList();
        private boolean lockHooks = false;
        private final List<Object> registrations = new ArrayList<>(0);

        public StaticCheckpointHook(int rank, boolean multiThreaded) {
            this.rank = rank;
            this.multiThreaded = multiThreaded;
        }

        void addRegistration(Object serviceRegistration) {
            registrations.add(serviceRegistration);
        }

        void register(Object context, StaticCheckpointHook unregisterStaticHooks) {
            Dictionary<String, Object> props = new Hashtable<>();
            props.put(SERVICE_RANKING, Integer.valueOf(rank));
            props.put(CheckpointHook.MULTI_THREADED_HOOK, Boolean.valueOf(multiThreaded));
            try {
                debug(() -> "Registering CheckpointHook service for rank :" + props.get(SERVICE_RANKING)
                            + (props.get(CheckpointHook.MULTI_THREADED_HOOK) == Boolean.TRUE ? " multi-threaded" : " single-threaded"));
                Object serviceRegistration = registerService.invoke(context, CheckpointHook.class, this, props);
                if (unregisterStaticHooks != null) {
                    unregisterStaticHooks.addRegistration(serviceRegistration);
                } else {
                    if (rank != Integer.MAX_VALUE || !multiThreaded) {
                        throw new IllegalStateException("Missing unregisterStaticHook to add registration.");
                    }
                    // This is the max ranked multiThreaded hook; add the registration to self for unregistering
                    addRegistration(serviceRegistration);
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                sneakyThrow(e.getTargetException());
            }
        }

        boolean addHook(CheckpointHook hook) {
            synchronized (addedHooks) {
                if (lockHooks) {
                    debug(() -> "Did not add hook: " + hook);
                    return false;
                }
                boolean added = addedHooks.add(hook);
                debug(() -> "Added hook: " + added + " - " + addedHooks.size());
                return added;
            }
        }

        @Override
        public void prepare() {
            synchronized (addedHooks) {
                preparedHooks = new ArrayList<>(addedHooks);
                addedHooks.clear();
                lockHooks = true;
            }

            // Only max rank, multi-thread hook will have registrations to unregister
            for (Object serviceRegistration : registrations) {
                // clean up registration; a snapshot has been done, no need to stay registered
                try {
                    debug(() -> "unregistering static hook in prepare: " + serviceRegistration);
                    unregisterService.invoke(serviceRegistration);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getTargetException());
                }
            }

            // clean up map entry for this rank
            synchronized (CheckpointPhase.this) {
                Map<Integer, StaticCheckpointHook> toRemoveMap = multiThreaded ? multiThreadedHooks : singleThreadedHooks;
                toRemoveMap.remove(rank);
                debug(() -> "removed static " + (multiThreaded ? "multi-threaded" : "single-threaded") + " hook rank: " + rank + " number hooks left: " + toRemoveMap.size());
            }

            for (CheckpointHook hook : preparedHooks) {
                debug(() -> "prepare operation on static " + (multiThreaded ? "multi-threaded" : "single-threaded") + " rank: " + rank + " hook: " + hook);
                hook.prepare();
            }
            // reverse for restore call during checkpoint
            Collections.reverse(preparedHooks);
        }

        @Override
        public void checkpointFailed() {
            for (CheckpointHook hook : preparedHooks) {
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
            for (CheckpointHook hook : preparedHooks) {
                debug(() -> "restore operation on static " + (multiThreaded ? "multi-threaded" : "single-threaded") + " rank: " + rank + " hook: " + hook);
                hook.restore();
            }
            // release references to hooks now
            preparedHooks = Collections.emptyList();
        }
    }
}
