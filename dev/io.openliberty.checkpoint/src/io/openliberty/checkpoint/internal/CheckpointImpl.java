/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.internal;

import static io.openliberty.checkpoint.spi.CheckpointPhase.CHECKPOINT_PROPERTY;
import static io.openliberty.checkpoint.spi.CheckpointPhase.CONDITION_PROCESS_RUNNING_ID;
import static org.osgi.service.condition.Condition.CONDITION_ID;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.condition.Condition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerReadyStatus;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.kernel.feature.LibertyFeature;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.internal.openj9.J9CRIUSupport;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component(
           reference = { @Reference(name = CheckpointImpl.HOOKS_REF_NAME_MULTI_THREAD, service = CheckpointHook.class, cardinality = ReferenceCardinality.MULTIPLE,
                                    policy = ReferencePolicy.DYNAMIC,
                                    policyOption = ReferencePolicyOption.GREEDY,
                                    target = "(" + CheckpointHook.MULTI_THREADED_HOOK + "=true)"),
                         @Reference(name = CheckpointImpl.HOOKS_REF_NAME_SINGLE_THREAD, service = CheckpointHook.class, cardinality = ReferenceCardinality.MULTIPLE,
                                    policy = ReferencePolicy.DYNAMIC,
                                    policyOption = ReferencePolicyOption.GREEDY,
                                    target = "(|(!(" + CheckpointHook.MULTI_THREADED_HOOK + "=*))(" + CheckpointHook.MULTI_THREADED_HOOK + "=false))"),
                         @Reference(name = CheckpointImpl.BETA_FEATURE_CONITION_REF, service = Condition.class,
                                    cardinality = ReferenceCardinality.OPTIONAL,
                                    policy = ReferencePolicy.DYNAMIC,
                                    policyOption = ReferencePolicyOption.GREEDY,
                                    target = "(" + Condition.CONDITION_ID + "=io.openliberty.checkpoint.feature)")
           },
           property = { Constants.SERVICE_RANKING + ":Integer=-10000" },
           // use immediate component to avoid lazy instantiation and deactivate
           immediate = true)
public class CheckpointImpl implements RuntimeUpdateListener, ServerReadyStatus {
    private static final String INSTANTON_ENABLED_HEADER = "WLP-InstantOn-Enabled";

    private static final String CHECKPOINT_STUB_CRIU = "io.openliberty.checkpoint.stub.criu";
    private static final String CHECKPOINT_CRIU_UNPRIVILEGED = "io.openliberty.checkpoint.criu.unprivileged";
    private static final String CHECKPOINT_ALLOWED_FEATURES = "io.openliberty.checkpoint.allowed.features";
    private static final String CHECKPOINT_FORCE_FAIL_TYPE = "io.openliberty.checkpoint.fail.type";
    private static final String CHECKPOINT_ALLOWED_FEATURES_ALL = "ALL_FEATURES";
    static final String CHECKPOINT_PAUSE_RESTORE = "io.openliberty.checkpoint.pause.restore";

    static final String HOOKS_REF_NAME_SINGLE_THREAD = "hooksSingleThread";
    static final String HOOKS_REF_NAME_MULTI_THREAD = "hooksMultiThread";
    static final String BETA_FEATURE_CONITION_REF = "io.openliberty.checkpoint.beta";
    private static final String DIR_CHECKPOINT = "checkpoint/";
    private static final String FILE_RESTORE_MARKER = DIR_CHECKPOINT + ".restoreMarker";
    private static final String FILE_RESTORE_FAILED_MARKER = DIR_CHECKPOINT + ".restoreFailedMarker";
    private static final String FILE_ENV_PROPERTIES = DIR_CHECKPOINT + ".env.properties";
    private static final String DIR_CHECKPOINT_IMAGE = DIR_CHECKPOINT + "image/";
    private static final String CHECKPOINT_LOG_FILE = "checkpoint.log";

    private static final TraceComponent tc = Tr.register(CheckpointImpl.class);

    private final Set<String> allowedFeatures;
    private final ComponentContext cc;
    private final CheckpointPhase checkpointAt;
    private final WsLocationAdmin locAdmin;

    private final AtomicBoolean checkpointCalled = new AtomicBoolean(false);
    private final ServiceRegistration<ClassFileTransformer> transformerReg;

    private final AtomicBoolean jvmRestore = new AtomicBoolean(false);
    private final AtomicReference<CountDownLatch> waitForConfig = new AtomicReference<>();
    private final ExecuteCRIU criu;
    private final long pauseRestore;
    private final CheckpointFailedException forceFail;
    private static volatile CheckpointImpl INSTANCE = null;

    @Activate
    public CheckpointImpl(ComponentContext cc, @Reference WsLocationAdmin locAdmin,
                          @Reference(target = CheckpointPhase.CHECKPOINT_ACTIVE_FILTER) CheckpointPhase phase) {
        this(cc, null, locAdmin, phase);
    }

    // only for unit tests
    CheckpointImpl(ComponentContext cc, ExecuteCRIU criu, WsLocationAdmin locAdmin, CheckpointPhase phase) {
        this.cc = cc;

        allowedFeatures = getAllowedFeatures(cc);
        if (Boolean.valueOf(cc.getBundleContext().getProperty(CHECKPOINT_STUB_CRIU))) {
            /*
             * This is useful for development if you want to debug through a checkpoint/restore
             * operation without actually doing the criu dump in between. You can do this with
             * the following:
             * bin/server checkpoint <server name> --at=<phase> -Dio.openliberty.checkpoint.stub.criu=true
             */
            this.criu = new ExecuteCRIU() {
                @Override
                public void dump(Runnable prepare, Runnable restore, File imageDir, String logFileName, File workDir, File envProps,
                                 boolean unprivileged) throws CheckpointFailedException {
                    prepare.run();
                    restore.run();
                }
            };
        } else {
            this.criu = (criu == null) ? J9CRIUSupport.create(this) : criu;
        }
        this.locAdmin = locAdmin;
        this.checkpointAt = phase;

        this.pauseRestore = getPauseTime(cc.getBundleContext().getProperty(CHECKPOINT_PAUSE_RESTORE));
        this.forceFail = getForceFailCheckpointCode(cc.getBundleContext().getProperty(CHECKPOINT_FORCE_FAIL_TYPE));

        // Keep assignment of static INSTANCE as last thing done.
        // Technically we are escaping 'this' here but we can be confident that INSTANCE will not be used
        // until the constructor exits here given that this is an immediate component and activated early,
        // long before applications are started.
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

    private CheckpointFailedException getForceFailCheckpointCode(String property) {
        if (property == null) {
            return null;
        }
        CheckpointFailedException.Type type = Type.valueOf(property);
        return new CheckpointFailedException(type, "TESTING FAILURE", null);
    }

    private long getPauseTime(String pauseRestoreTime) {
        if (pauseRestoreTime == null) {
            return 0;
        }
        try {
            long result = Long.parseLong(pauseRestoreTime);
            return result < 0 ? 0 : result;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * @param cc2
     * @return
     */
    private static Set<String> getAllowedFeatures(ComponentContext cc) {
        String allowedProp = cc.getBundleContext().getProperty(CHECKPOINT_ALLOWED_FEATURES);
        if (allowedProp == null) {
            return Collections.emptySet();
        }
        String[] allowedFeatures = allowedProp.split(",");
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(allowedFeatures)));
    }

    @Deactivate
    void deactivate() {
        if (INSTANCE == this) {
            INSTANCE = null;
        }
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (jvmRestore.compareAndSet(true, false) && RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED.equals(notification.getName())) {
            debug(tc, () -> "Processing config on restore.");
            final CountDownLatch localLatch = new CountDownLatch(1);
            waitForConfig.set(localLatch);
            notification.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    debug(tc, () -> "Config has been successfully processed on restore.");
                    localLatch.countDown();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    debug(tc, () -> "Failed to process config on restore");
                    localLatch.countDown();
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
        // triggers may not fire for certain applications/configuration.  For example,
        // if while deploying an application no application classes are initialized.
        checkpointOrExitOnFailure();
    }

    void checkpointOrExitOnFailure() {
        try {
            checkpoint();
        } catch (CheckpointFailedException e) {
            // Allow auto FFDC here to capture the causing exception (if any)

            if (e.isRestore()) {
                // create restore failed marker with return code
                createRestoreFailedMarker(e);
            }
            Tr.error(tc, e.getErrorMsgKey(), e.getMessage());
            /*
             * The extra thread is needed to avoid blocking the current thread while the shutdown hooks are run
             * (which starts a server shutdown and quiesce).
             * The issue is that if we block the event thread then there is a deadlock that prevents the server
             * shutdown from happening until a 30 second timeout is reached.
             */
            new Thread(() -> System.exit(e.getErrorCode()), e.isRestore() ? "Restore failed, exiting..." : "Checkpoint failed, exiting...").start();
        }
    }

    @FFDCIgnore({ IllegalStateException.class, Throwable.class, CheckpointFailedException.class })
    void checkpoint() throws CheckpointFailedException {
        debug(tc, () -> "Checkpoint for : " + checkpointAt);

        Condition betaFeature = cc.locateService(BETA_FEATURE_CONITION_REF);
        if (betaFeature == null) {
            // TODO this should be removed for GA.  This is why we are not defining a known type nor message ID for this exception
            throw new CheckpointFailedException(Type.UNKNOWN_CHECKPOINT, "CWWKC0460E: The server command specified the checkpoint action. A checkpoint cannot be taken because the checkpoint-1.0 feature is not configured in the server.xml file.", null);
        }
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

        checkSupportedFeatures();

        List<CheckpointHook> multiThreadPrepareHooks = getHooks(cc.locateServices(HOOKS_REF_NAME_MULTI_THREAD));
        List<CheckpointHook> singleThreadPrepareHooks = getHooks(cc.locateServices(HOOKS_REF_NAME_SINGLE_THREAD));

        // reverse prepare hook order for restore hooks
        List<CheckpointHook> multiThreadRestoreHooks = new ArrayList<>(multiThreadPrepareHooks);
        Collections.reverse(multiThreadRestoreHooks);
        List<CheckpointHook> singleThreadRestoreHooks = new ArrayList<>(singleThreadPrepareHooks);
        Collections.reverse(singleThreadRestoreHooks);

        Tr.audit(tc, "CHECKPOINT_DUMP_INITIATED_CWWKC0451");

        if (forceFail != null && !forceFail.isRestore()) {
            // only done for tests
            throw (CheckpointFailedException) forceFail.fillInStackTrace();
        }

        try {
            prepare(multiThreadPrepareHooks);
            try {
                criu.checkpointSupported();
            } catch (CheckpointFailedException cpfe) {
                debug(tc, () -> "ExecuteCRIU service does not support checkpoint: " + cpfe.getMessage());
                throw cpfe;
            }
            boolean unprivileged = Boolean.valueOf(cc.getBundleContext().getProperty(CHECKPOINT_CRIU_UNPRIVILEGED));
            File imageDir = getImageDir();
            debug(tc, () -> "criu attempt dump to '" + imageDir + "' and exit process.");

            criu.dump(() -> prepare(singleThreadPrepareHooks),
                      () -> restore(singleThreadRestoreHooks),
                      imageDir, CHECKPOINT_LOG_FILE,
                      getLogsCheckpoint(),
                      getEnvProperties(),
                      unprivileged);

            debug(tc, () -> "criu dumped to " + imageDir + ", now in recovered process.");
            if (forceFail != null && forceFail.isRestore()) {
                // only done for tests
                throw (CheckpointFailedException) forceFail.fillInStackTrace();
            }
            if (pauseRestore > 0) {
                try {
                    Thread.sleep(pauseRestore);
                } catch (InterruptedException e) {
                    Thread.currentThread().isInterrupted();
                }
            }
            restore(multiThreadRestoreHooks);
        } catch (Throwable e) {
            CheckpointFailedException rethrow;
            if (e instanceof CheckpointFailedException) {
                rethrow = (CheckpointFailedException) e;
            } else {
                rethrow = new CheckpointFailedException(getUnknownType(), Tr.formatMessage(tc, "UKNOWN_FAILURE_CWWKC0455E", e.getMessage()), e);
            }
            // tell all the hooks about a checkpoint failure
            if (!rethrow.isRestore()) {
                // The restore hook order is used so we call the checkpointFailed methods in the reverse order
                // of the calls to the prepare methods.
                callHooksOnFailure(singleThreadRestoreHooks, multiThreadRestoreHooks);
            }
            throw rethrow;
        }

        registerRunningCondition();

        waitForConfig();
        Tr.audit(tc, "CHECKPOINT_RESTORE_CWWKC0452I", TimestampUtils.getElapsedTime());

        createRestoreMarker();
    }

    private void callHooksOnFailure(List<CheckpointHook> singleThreadRestoreHooks, List<CheckpointHook> multiThreadRestoreHooks) {
        callHooks("checkpointFailed", singleThreadRestoreHooks, CheckpointHook::checkpointFailed, null);
        callHooks("checkpointFailed", multiThreadRestoreHooks, CheckpointHook::checkpointFailed, null);
    }

    /**
     *
     */
    private void checkSupportedFeatures() {
        if (allowedFeatures.contains(CHECKPOINT_ALLOWED_FEATURES_ALL)) {
            // shortcut if all features area allowed
            return;
        }
        BundleContext context = cc.getBundleContext();
        if (context == null) {
            return;
        }
        try {
            ServiceReference<?>[] features = context.getServiceReferences("com.ibm.wsspi.kernel.feature.LibertyFeature", null);
            if (features != null) {
                List<Object> unsupported = new ArrayList<>(0);
                for (ServiceReference<?> feature : features) {
                    LibertyFeature libertyFeature = (LibertyFeature) context.getService(feature);
                    if (libertyFeature != null) {
                        try {
                            String instantonEnabled = libertyFeature.getHeader(INSTANTON_ENABLED_HEADER);
                            Object featureName = feature.getProperty("ibm.featureName");
                            if (!(Boolean.parseBoolean(instantonEnabled) || allowedFeatures.contains(featureName))) {
                                unsupported.add(featureName);
                            }
                        } finally {
                            context.ungetService(feature);
                        }
                    }
                }
                if (!unsupported.isEmpty()) {
                    throw new CheckpointFailedException(Type.LIBERTY_PREPARE_FAILED, Tr.formatMessage(tc, "CHECKPOINT_FAILED_UNSUPPORTED_FEATURE_CWWKC0456E", unsupported), null);
                }
            }

        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Type getUnknownType() {
        // check for the env properties file.  If it exists then assume this is a restore failure
        return getEnvProperties().exists() ? Type.UNKNOWN_RESTORE : Type.UNKNOWN_CHECKPOINT;
    }

    public String getMessage(String msgKey, Object... args) {
        return Tr.formatMessage(tc, msgKey, args);
    }

    private void registerRunningCondition() {
        BundleContext bc = cc.getBundleContext();
        Hashtable<String, Object> conditionProps = new Hashtable<>();
        conditionProps.put(CONDITION_ID, CONDITION_PROCESS_RUNNING_ID);
        conditionProps.put(CHECKPOINT_PROPERTY, checkpointAt);
        bc.registerService(Condition.class, Condition.INSTANCE, conditionProps);
    }

    private void waitForConfig() {
        CountDownLatch l = waitForConfig.getAndSet(null);
        if (l != null) {
            debug(tc, () -> "Waiting for config to be processed on restore.");
            try {
                l.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            debug(tc, () -> "Config to is done being processed on restore.");
        }
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

    private void createRestoreFailedMarker(CheckpointFailedException e) {
        // create a marker to indicate that restore failed
        WsResource failedMarker = locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + FILE_RESTORE_FAILED_MARKER);
        try (PrintStream ps = new PrintStream(failedMarker.putStream())) {
            ps.print(String.valueOf(e.getErrorCode()));
        } catch (IOException ioe) {
            // auto FFDC here
        }
    }

    private File getEnvProperties() {
        return locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + FILE_ENV_PROPERTIES).asFile();
    }

    List<CheckpointHook> getHooks(Object[] hooks) {
        if (hooks == null) {
            debug(tc, () -> "No checkpoint hooks.");
            return Collections.emptyList();
        }
        debug(tc, () -> "Found checkpoint hooks: " + hooks);
        List<CheckpointHook> hookList = new ArrayList<>(hooks.length);
        for (Object o : hooks) {
            // if o is anything other than a CheckpointHook then
            // there is a bug in SCR
            CheckpointHook hook = (CheckpointHook) o;
            hookList.add(hook);

        }
        return hookList;
    }

    @FFDCIgnore(Throwable.class)
    @Trivial
    private void callHooks(String operation,
                           List<CheckpointHook> checkpointHooks,
                           Consumer<CheckpointHook> perform,
                           Function<Throwable, CheckpointFailedException> failed) throws CheckpointFailedException {
        for (CheckpointHook checkpointHook : checkpointHooks) {
            try {
                debug(tc, () -> operation + " operation on hook: " + checkpointHook);
                perform.accept(checkpointHook);
            } catch (Throwable abortCause) {
                debug(tc, () -> operation + " failed on hook: " + checkpointHook);
                if (failed != null) {
                    throw failed.apply(abortCause);
                }
            }
        }
    }

    @Trivial
    private void prepare(List<CheckpointHook> checkpointHooks) throws CheckpointFailedException {
        callHooks("prepare",
                  checkpointHooks,
                  CheckpointHook::prepare,
                  CheckpointImpl::failedPrepare);
    }

    private static CheckpointFailedException failedPrepare(Throwable cause) {
        return new CheckpointFailedException(Type.LIBERTY_PREPARE_FAILED, Tr.formatMessage(tc, "CHECKPOINT_FAILED_PREPARE_EXCEPTION_CWWKC0457E", cause.getMessage()), cause);
    }

    @Trivial
    private void restore(List<CheckpointHook> checkpointHooks) throws CheckpointFailedException {
        // The first thing is to set the jvmRestore flag
        jvmRestore.set(true);
        callHooks("restore",
                  checkpointHooks,
                  CheckpointHook::restore,
                  CheckpointImpl::failedRestore);
    }

    private static CheckpointFailedException failedRestore(Throwable cause) {
        return new CheckpointFailedException(Type.LIBERTY_RESTORE_FAILED, Tr.formatMessage(tc, "RESTORE_FAILED_RESTORE_EXCEPTION_CWWKC0458E", cause.getMessage()), cause);
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

    @Trivial
    static void debug(TraceComponent trace, Supplier<String> message) {
        if (TraceComponent.isAnyTracingEnabled() && trace.isDebugEnabled()) {
            Tr.debug(trace, message.get());
        }
    }
}
