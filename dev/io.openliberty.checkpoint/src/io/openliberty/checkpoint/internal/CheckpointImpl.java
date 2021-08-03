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

import io.openliberty.checkpoint.internal.CheckpointFailed.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointHookFactory;
import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;

@Component(
           reference = @Reference(name = "hookFactories", service = CheckpointHookFactory.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
                                  policyOption = ReferencePolicyOption.GREEDY),
           property = { Constants.SERVICE_RANKING + ":Integer=-10000" })
public class CheckpointImpl implements RuntimeUpdateListener, ServerReadyStatus {

    private static final TraceComponent tc = Tr.register(CheckpointImpl.class);
    private final ComponentContext cc;
    private final boolean checkpointFeatures;
    private final boolean checkpointApplications;
    private final WsLocationAdmin locAdmin;

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
        this.checkpointFeatures = "features".equals(cc.getBundleContext().getProperty(BootstrapConstants.CHECKPOINT_PROPERTY_NAME));
        this.checkpointApplications = "applications".equals(cc.getBundleContext().getProperty(BootstrapConstants.CHECKPOINT_PROPERTY_NAME));
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (checkpointFeatures && RuntimeUpdateNotification.APPLICATIONS_STARTING.equals(notification.getName())) {
            notification.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (result) {
                        try {
                            checkpoint(Phase.FEATURES);
                        } catch (CheckpointFailed e) {
                            if (e.getType() == Type.SNAPSHOT_FAILED) {
                                new Thread(() -> System.exit(e.getErrorCode()), "Checkpoint exit.").start();
                            }
                            // TODO should we always exit on failure?
                        }
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
        if (checkpointApplications) {
            try {
                checkpoint(Phase.APPLICATIONS);
            } catch (CheckpointFailed e) {
                if (e.getType() == Type.SNAPSHOT_FAILED) {
                    new Thread(() -> System.exit(e.getErrorCode()), "Checkpoint exit.").start();
                }
                // TODO should we always exit on failure?
            }
        }
    }

    void checkpoint(Phase phase) throws CheckpointFailed {
        doCheckpoint(phase,
                     locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + "checkpoint/image/").asFile());
    }

    private void doCheckpoint(Phase phase, File directory) throws CheckpointFailed {
        directory.mkdirs();
        Object[] factories = cc.locateServices("hookFactories");
        List<CheckpointHook> checkpointHooks = getHooks(factories, phase);
        prepare(checkpointHooks);
        try {
            ExecuteCRIU currentCriu = criu;
            if (currentCriu == null) {
                throw new CheckpointFailed(Type.SNAPSHOT_FAILED, "The criu command is not available.", null, 0);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "criu attempt dump to '" + directory + "' and exit process.");
                }

                int dumpCode = currentCriu.dump(directory);
                if (dumpCode < 0) {
                    throw new CheckpointFailed(Type.SNAPSHOT_FAILED, "The criu dump command failed with error: " + dumpCode, null, dumpCode);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "criu dump to " + directory + " in recovery.");
                }
            }
        } catch (Exception e) {
            abortPrepare(checkpointHooks, e);
            if (e instanceof CheckpointFailed) {
                throw (CheckpointFailed) e;
            }
            throw new CheckpointFailed(Type.SNAPSHOT_FAILED, "Failed to do checkpoint.", e, 0);
        }
        restore(phase, checkpointHooks);
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
                           Function<Exception, CheckpointFailed> failed) throws CheckpointFailed {
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

    private void prepare(List<CheckpointHook> checkpointHooks) throws CheckpointFailed {
        callHooks(checkpointHooks,
                  CheckpointHook::prepare,
                  CheckpointHook::abortPrepare,
                  CheckpointImpl::failedPrepare);
    }

    private static CheckpointFailed failedPrepare(Exception cause) {
        return new CheckpointFailed(Type.PREPARE_ABORT, "Failed to prepare for a checkpoint.", cause, 0);
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

    private void restore(Phase phase, List<CheckpointHook> checkpointHooks) throws CheckpointFailed {
        callHooks(checkpointHooks,
                  CheckpointHook::restore,
                  CheckpointHook::abortRestore,
                  CheckpointImpl::failedRestore);
    }

    private static CheckpointFailed failedRestore(Exception cause) {
        return new CheckpointFailed(Type.RESTORE_ABORT, "Failed to restore from checkpoint.", cause, 0);
    }
}
