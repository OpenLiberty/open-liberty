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
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
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
import io.openliberty.checkpoint.internal.openj9.J9CRIUSupport;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component(
           reference = @Reference(name = CheckpointImpl.HOOKS_REF_NAME, service = CheckpointHook.class, cardinality = ReferenceCardinality.MULTIPLE,
                                  policy = ReferencePolicy.DYNAMIC,
                                  policyOption = ReferencePolicyOption.GREEDY),
           property = { Constants.SERVICE_RANKING + ":Integer=-10000" })
public class CheckpointImpl implements RuntimeUpdateListener, ServerReadyStatus {
    private static final String CHECKPOINT_STUB_CRIU = "io.openliberty.checkpoint.stub.criu";
    static final String HOOKS_REF_NAME = "hooks";
    private static final String DIR_CHECKPOINT = "checkpoint/";
    private static final String FILE_RESTORE_MARKER = DIR_CHECKPOINT + ".restoreMarker";
    private static final String FILE_ENV_PROPERTIES = DIR_CHECKPOINT + ".env.properties";
    private static final String DIR_CHECKPOINT_IMAGE = DIR_CHECKPOINT + "image/";
    private static final String CHECKPOINT_LOG_FILE = "checkpoint.log";

    private static final TraceComponent tc = Tr.register(CheckpointImpl.class);
    private final ComponentContext cc;
    private final CheckpointPhase checkpointAt;
    private final WsLocationAdmin locAdmin;

    private final AtomicBoolean checkpointCalled = new AtomicBoolean(false);
    private final ServiceRegistration<ClassFileTransformer> transformerReg;

    private final ExecuteCRIU criu;

    private static volatile CheckpointImpl INSTANCE = null;

    @Activate
    public CheckpointImpl(ComponentContext cc, @Reference WsLocationAdmin locAdmin, @Reference CheckpointPhase phase) {
        this(cc, null, locAdmin, phase);
    }

    // only for unit tests
    CheckpointImpl(ComponentContext cc, ExecuteCRIU criu, WsLocationAdmin locAdmin, CheckpointPhase phase) {
        this.cc = cc;
        if (Boolean.valueOf(cc.getBundleContext().getProperty(CHECKPOINT_STUB_CRIU))) {
            /*
             * This is useful for development if you want to debug through a checkpoint/restore
             * operation without actually doing the criu dump in between. You can do this with
             * the following:
             * bin/server checkpoint <server name> --at=<phase> -Dio.openliberty.checkpoint.stub.criu=true
             */
            this.criu = new ExecuteCRIU() {
            };
        } else {
            this.criu = (criu == null) ? J9CRIUSupport.create() : criu;
        }
        this.locAdmin = locAdmin;
        this.checkpointAt = phase;
        if (this.checkpointAt == CheckpointPhase.DEPLOYMENT) {
            this.transformerReg = cc.getBundleContext().registerService(ClassFileTransformer.class, new CheckpointTransformer(),
                                                                        FrameworkUtil.asDictionary(Collections.singletonMap("io.openliberty.classloading.system.transformer",
                                                                                                                            true)));
            INSTANCE = this;
        } else {
            this.transformerReg = null;
            INSTANCE = null;
        }
    }

    @Deactivate
    void deactivate() {
        if (INSTANCE == this) {
            INSTANCE = null;
        }
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (checkpointAt == CheckpointPhase.FEATURES && RuntimeUpdateNotification.APPLICATIONS_STARTING.equals(notification.getName())) {
            notification.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (result) {
                        checkpointOrExitOnFailure();
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
        // This gets called once once the server is "ready" before the ports will be opened.
        // This is typically used for the "applications" checkpoint phase.  But if any other
        // phase is specified and they haven't caused a checkpoint yet this is the catch all.
        // This catch all is necessary because it is possible that "features" and "deployment"
        // tiggers may not fire for certain applications/configuration.  For example,
        // if while deploying an application no application classes are initialized.
        checkpointOrExitOnFailure();
    }

    void checkpointOrExitOnFailure() {
        try {
            checkpoint();
        } catch (CheckpointFailedException e) {
            // Allow auto FFDC here
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

    @FFDCIgnore({ IllegalStateException.class, Exception.class })
    void checkpoint() throws CheckpointFailedException {
        debug(tc, () -> "Checkpoint for : " + checkpointAt);

        // Checkpoint can only be called once
        if (checkpointCalledAlready()) {
            debug(tc, () -> "Trying to checkpoint a second time" + checkpointAt);
            return;
        }
        if (transformerReg != null) {
            try {
                transformerReg.unregister();
            } catch (IllegalStateException e) {
                // do nothing
            }
        }

        Object[] hookRefs = cc.locateServices(HOOKS_REF_NAME);
        List<CheckpointHook> checkpointHooks = getHooks(hookRefs);

        if (tc.isInfoEnabled()) {
            Tr.info(tc, "CHECKPOINT_DUMP_INITIATED_CWWKC0451");
        }

        prepare(checkpointHooks);
        Collections.reverse(checkpointHooks);
        try {
            try {
                criu.checkpointSupported();
            } catch (CheckpointFailedException cpfe) {
                debug(tc, () -> "ExecuteCRIU service does not support checkpoint.");
                //TODO log a more helpful message based on cpfe Type
                System.out.println(cpfe.getMessage());
                throw cpfe;
            }
            File imageDir = getImageDir();
            debug(tc, () -> "criu attempt dump to '" + imageDir + "' and exit process.");

            criu.dump(imageDir, CHECKPOINT_LOG_FILE,
                      getLogsCheckpoint(),
                      getEnvProperties());

            debug(tc, () -> "criu dumped to " + imageDir + ", now in recovered process.");
        } catch (Exception e) {
            if (e instanceof CheckpointFailedException) {
                throw (CheckpointFailedException) e;
            }
            throw new CheckpointFailedException(Type.UNKNOWN, "Failed to do checkpoint.", e, 0);
        }

        restore(checkpointHooks);
        if (tc.isInfoEnabled()) {
            Tr.info(tc, "CHECKPOINT_RESTORE_CWWKC0452I");
        }
        createRestoreMarker();
    }

    /**
     * @return
     */
    private File getImageDir() {
        File imageDir = locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + DIR_CHECKPOINT_IMAGE).asFile();
        imageDir.mkdirs();
        return imageDir;
    }

    private File getLogsCheckpoint() {
        WsResource logsCheckpoint = locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_LOGS_DIR + DIR_CHECKPOINT);
        logsCheckpoint.create();
        return logsCheckpoint.asFile();
    }

    private void createRestoreMarker() {
        // create a marker to indicate that another restore needs to restore the workarea
        locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + FILE_RESTORE_MARKER).create();
    }

    private File getEnvProperties() {
        return locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + FILE_ENV_PROPERTIES).asFile();
    }

    List<CheckpointHook> getHooks(Object[] hooks) {
        if (hooks == null) {
            debug(tc, () -> "No checkpoint hooks.");
            return Collections.emptyList();
        }
        debug(tc, () -> "Found checkpoint hook factories: " + hooks);
        List<CheckpointHook> hookList = new ArrayList<>(hooks.length);
        for (Object o : hooks) {
            // if o is anything other than a CheckpointHook then
            // there is a bug in SCR
            CheckpointHook hook = (CheckpointHook) o;
            hookList.add(hook);

        }
        return hookList;
    }

    @FFDCIgnore(Exception.class)
    private void callHooks(String operation, List<CheckpointHook> checkpointHooks,
                           Consumer<CheckpointHook> perform,
                           Function<Exception, CheckpointFailedException> failed) throws CheckpointFailedException {
        for (CheckpointHook checkpointHook : checkpointHooks) {
            try {
                debug(tc, () -> operation + " operation on hook: " + checkpointHook);
                perform.accept(checkpointHook);
            } catch (Exception abortCause) {
                debug(tc, () -> operation + " failed on hook: " + checkpointHook);
                throw failed.apply(abortCause);
            }
        }
    }

    private void prepare(List<CheckpointHook> checkpointHooks) throws CheckpointFailedException {
        callHooks("prepare",
                  checkpointHooks,
                  CheckpointHook::prepare,
                  CheckpointImpl::failedPrepare);
    }

    private static CheckpointFailedException failedPrepare(Exception cause) {
        return new CheckpointFailedException(Type.PREPARE_ABORT, "Failed to prepare for a checkpoint.", cause, 0);
    }

    private void restore(List<CheckpointHook> checkpointHooks) throws CheckpointFailedException {
        callHooks("restore",
                  checkpointHooks,
                  CheckpointHook::restore,
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

    public static void deployCheckpoint() {
        CheckpointImpl current = INSTANCE;
        if (current != null && current.checkpointAt == CheckpointPhase.DEPLOYMENT) {
            current.checkpointOrExitOnFailure();
        }
    }

    static void debug(TraceComponent trace, Supplier<String> message) {
        if (TraceComponent.isAnyTracingEnabled() && trace.isDebugEnabled()) {
            Tr.debug(trace, message.get());
        }
    }
}
