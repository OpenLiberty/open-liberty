/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.feature.ServerReadyStatus;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointHookFactory;
import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;

@Component(
           reference = @Reference(name = "hookFactories", service = CheckpointHookFactory.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
                                  policyOption = ReferencePolicyOption.GREEDY),
           property = { Constants.SERVICE_RANKING + ":Integer=-10000" })
public class CheckpointImpl implements RuntimeUpdateListener, ServerReadyStatus {

    private static final String DIR_CHECKPOINT = "checkpoint/";
    private static final String FILE_RESTORE_MARKER = DIR_CHECKPOINT + ".restoreMarker";
    private static final String DIR_CHECKPOINT_IMAGE = DIR_CHECKPOINT + "image/";
    private static final String CHECKPOINT_LOG_FILE = "checkpoint.log";

    private static final TraceComponent tc = Tr.register(CheckpointImpl.class);
    private final ComponentContext cc;
    private final Phase checkpointAt;
    private final WsLocationAdmin locAdmin;

    private final AtomicBoolean checkpointCalled = new AtomicBoolean(false);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ExecuteCRIU criu;

    @Activate
    public CheckpointImpl(ComponentContext cc, @Reference WsLocationAdmin locAdmin) {
        this(cc, null, locAdmin);
    }

    // only for unit tests
    CheckpointImpl(ComponentContext cc, ExecuteCRIU criu, WsLocationAdmin locAdmin) {
        this.cc = cc;
        this.criu = criu;
        this.locAdmin = locAdmin;
        String phase = cc.getBundleContext().getProperty(BootstrapConstants.CHECKPOINT_PROPERTY_NAME);
        this.checkpointAt = phase == null ? null : Phase.getPhase(phase);
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (checkpointAt == Phase.FEATURES && RuntimeUpdateNotification.APPLICATIONS_STARTING.equals(notification.getName())) {
            notification.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (result) {
                        checkpointOrExitOnFailure(Phase.FEATURES);
                    }
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                }
            });
        }
    }

    @Override
    public void check() {
        if (checkpointAt == Phase.APPLICATIONS) {
            checkpointOrExitOnFailure(Phase.APPLICATIONS);
        }
    }

    void checkpointOrExitOnFailure(Phase phase) {
        try {
            checkpoint(phase);
        } catch (CheckpointFailedException e) {
            // TODO log error informing we are exiting

            // TODO is there any type of failure where we would not want to exit?

            /*
             * The extra thread is needed to avoid blocking the current thread while the shutdown hooks are run
             * (which starts a server shutdown and quiesce).
             * The issue is that if we block the event thread then there is a deadlock that prevents the server
             * shutdown from happening until a 30 second timeout is reached.
             */
            new Thread(() -> System.exit(e.getErrorCode()), "Checkpoint failed, exiting...").start();
        }
    }

    void checkpoint(Phase phase) throws CheckpointFailedException {
        // Checkpoint can only be called once
        if (checkpointCalledAlready()) {
            return;
        }
        File imageDir = locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + DIR_CHECKPOINT_IMAGE).asFile();
        imageDir.mkdirs();
        Object[] factories = cc.locateServices("hookFactories");
        List<CheckpointHook> checkpointHooks = getHooks(factories, phase);
        prepare(checkpointHooks);
        try {
            ExecuteCRIU currentCriu = criu;
            if (currentCriu == null) {
                throw new CheckpointFailedException(Type.UNSUPPORTED, "The criu command is not available.", null, 0);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "criu attempt dump to '" + imageDir + "' and exit process.");
                }

                WsResource logsCheckpoint = locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_LOGS_DIR + DIR_CHECKPOINT);
                logsCheckpoint.create();
                currentCriu.dump(imageDir, CHECKPOINT_LOG_FILE,
                                 logsCheckpoint.asFile());

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "criu dump to " + imageDir + " in recovery.");
                }
            }
        } catch (Exception e) {
            abortPrepare(checkpointHooks, e);
            if (e instanceof CheckpointFailedException) {
                throw (CheckpointFailedException) e;
            }
            throw new CheckpointFailedException(Type.UNKNOWN, "Failed to do checkpoint.", e, 0);
        }
        restore(phase, checkpointHooks);
        createRestoreMarker();
    }

    /**
     *
     */
    private void createRestoreMarker() {
        // create a marker to indicate that another restore needs to restore the workarea
        locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + FILE_RESTORE_MARKER).create();
    }

    List<CheckpointHook> getHooks(Object[] factories, Phase phase) {
        if (factories == null) {
            return Collections.emptyList();
        }
        List<CheckpointHook> hooks = new ArrayList<>(factories.length);
        for (Object o : factories) {
            // if o is anything other than a CheckpointHookFactory then
            // there is a bug in SCR
            CheckpointHook hook = ((CheckpointHookFactory) o).create(phase);
            if (hook != null) {
                hooks.add(hook);
            }
        }
        return hooks;
    }

    private void callHooks(List<CheckpointHook> checkpointHooks,
                           Consumer<CheckpointHook> perform,
                           BiConsumer<CheckpointHook, Exception> abort,
                           Function<Exception, CheckpointFailedException> failed) throws CheckpointFailedException {
        List<CheckpointHook> called = new ArrayList<>(checkpointHooks.size());
        for (CheckpointHook checkpointHook : checkpointHooks) {
            try {
                perform.accept(checkpointHook);
                called.add(checkpointHook);
            } catch (Exception abortCause) {
                for (CheckpointHook abortHook : called) {
                    try {
                        abort.accept(abortHook, abortCause);
                    } catch (Exception unexpected) {
                        // auto FFDC is fine here
                    }
                }
                throw failed.apply(abortCause);
            }
        }
    }

    private void prepare(List<CheckpointHook> checkpointHooks) throws CheckpointFailedException {
        callHooks(checkpointHooks,
                  CheckpointHook::prepare,
                  CheckpointHook::abortPrepare,
                  CheckpointImpl::failedPrepare);
    }

    private static CheckpointFailedException failedPrepare(Exception cause) {
        return new CheckpointFailedException(Type.PREPARE_ABORT, "Failed to prepare for a checkpoint.", cause, 0);
    }

    private void abortPrepare(List<CheckpointHook> checkpointHooks, Exception cause) {
        for (CheckpointHook hook : checkpointHooks) {
            try {
                hook.abortPrepare(cause);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void restore(Phase phase, List<CheckpointHook> checkpointHooks) throws CheckpointFailedException {
        callHooks(checkpointHooks,
                  CheckpointHook::restore,
                  CheckpointHook::abortRestore,
                  CheckpointImpl::failedRestore);
    }

    private static CheckpointFailedException failedRestore(Exception cause) {
        return new CheckpointFailedException(Type.RESTORE_ABORT, "Failed to restore from checkpoint.", cause, 0);
    }

    boolean checkpointCalledAlready() {
        return !checkpointCalled.compareAndSet(false, true);
    }

    // used by tests only
    void resetCheckpointCalled() {
        checkpointCalled.set(false);
    }
}
