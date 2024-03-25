/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
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
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerReadyStatus;
import com.ibm.ws.kernel.productinfo.ProductInfo;
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

@Component(property = { Constants.SERVICE_RANKING + ":Integer=-10000" },
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
    private final List<CheckpointHookService> hooksMultiThreaded = Collections.synchronizedList(new ArrayList<>());
    private final List<CheckpointHookService> hooksSingleThreaded = Collections.synchronizedList(new ArrayList<>());
    private final Field checkpointPhaseRestored;
    private final Method checkpointPhaseblockAddHooks;

    private static volatile CheckpointImpl INSTANCE = null;

    static class CheckpointHookService implements Comparable<CheckpointHookService> {
        final CheckpointHook hook;
        private final ServiceReference<CheckpointHook> hookRef;
        private final boolean isCracHooks;

        @Trivial
        public CheckpointHookService(CheckpointHook hook, ServiceReference<CheckpointHook> hookRef) {
            this.hook = hook;
            this.hookRef = hookRef;
            this.isCracHooks = isCracHooks(hookRef);
        }

        @Override
        @Trivial
        public int compareTo(CheckpointHookService o) {
            if (this.isCracHooks == o.isCracHooks) {
                return hookRef.compareTo(o.hookRef);
            }
            return this.isCracHooks ? 1 : -1;
        }

        boolean isCracHooks(ServiceReference<CheckpointHook> hookRef) {
            Boolean isCracHooks = (Boolean) hookRef.getProperty("io.openliberty.crac.hooks");
            if (isCracHooks != null && isCracHooks) {
                debug(tc, () -> "Found CRaC hook:" + hookRef);
                return true;
            }
            return false;
        }

        @Trivial
        boolean isHookReference(ServiceReference<CheckpointHook> oHookRef) {
            return hookRef.getProperty(Constants.SERVICE_ID).equals(oHookRef.getProperty(Constants.SERVICE_ID));
        }

        @Trivial
        @Override
        public String toString() {
            return hook.toString() + ": " + hookRef.toString();
        }
    }

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

        try {
            this.checkpointPhaseblockAddHooks = CheckpointPhase.class.getDeclaredMethod("blockAddHooks");
            this.checkpointPhaseblockAddHooks.setAccessible(true);
            this.checkpointPhaseRestored = CheckpointPhase.class.getDeclaredField("restored");
            this.checkpointPhaseRestored.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        // Keep assignment of static INSTANCE as last thing done.
        // Technically we are escaping 'this' here but we can be confident that INSTANCE will not be used
        // until the constructor exits here given that this is an immediate component and activated early,
        // long before applications are started.
        INSTANCE = this;
        if (this.checkpointAt == CheckpointPhase.BEFORE_APP_START) {
            this.transformerReg = cc.getBundleContext().registerService(ClassFileTransformer.class, new CheckpointTransformer(),
                                                                        FrameworkUtil.asDictionary(Collections.singletonMap("io.openliberty.classloading.system.transformer",
                                                                                                                            true)));
        } else {
            this.transformerReg = null;
        }
    }

    @Reference(service = CheckpointHook.class, cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               unbind = "removeHook")
    void addHook(CheckpointHook hook, ServiceReference<CheckpointHook> hookRef) {
        CheckpointHookService hookService = new CheckpointHookService(hook, hookRef);
        boolean isMultiThreaded = isMultiThreaded(hookRef);
        debug(tc, () -> "Adding " + (isMultiThreaded ? "multi-threaded" : "single-threaded") + "hook: " + hookService);
        if (isMultiThreaded) {
            hooksMultiThreaded.add(hookService);
        } else {
            hooksSingleThreaded.add(hookService);
        }
    }

    void removeHook(ServiceReference<CheckpointHook> hookRef) {
        boolean isMultiThreaded = isMultiThreaded(hookRef);
        debug(tc, () -> "Removing " + (isMultiThreaded ? "multi-threaded" : "single-threaded") + "hook: " + hookRef);
        if (isMultiThreaded) {
            hooksMultiThreaded.removeIf(h -> h.isHookReference(hookRef));
        } else {
            hooksSingleThreaded.removeIf(h -> h.isHookReference(hookRef));
        }
    }

    private boolean isMultiThreaded(ServiceReference<CheckpointHook> hookRef) {
        Object multiThreaded = hookRef.getProperty(CheckpointHook.MULTI_THREADED_HOOK);
        return Boolean.TRUE.equals(multiThreaded);
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
        if (RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED.equals(notification.getName()) && jvmRestore.compareAndSet(true, false)) {
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
        debug(tc, () -> "Initiating checkpoint from server ready event.");
        // This gets called once the server is "ready" before the ports will be opened.
        // This is typically used for the "afterAppStart" checkpoint phase.  But if any other
        // phase is specified and they haven't caused a checkpoint yet this is the catch all.
        // This catch all is necessary because it is possible that "beforeAppStart"
        // triggers may not fire for certain applications/configuration.  For example,
        // if while starting an application no application classes are initialized.
        checkpointOrExitOnFailure();
    }

    @FFDCIgnore(CheckpointFailedException.class)
    void checkpointOrExitOnFailure() {
        try {
            checkpoint();
        } catch (CheckpointFailedException e) {
            // FFDC here to capture the causing exception (if any)
            FFDCFilter.processException(e, getClass().getName(), "checkpointOrExitOnFailure", this);
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

        // block adding hooks to CheckpointPhase
        try {
            checkpointPhaseblockAddHooks.invoke(checkpointAt);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new CheckpointFailedException(getUnknownType(), "Failed to call blockAddHooks.", e);
        } catch (InvocationTargetException e) {
            throw new CheckpointFailedException(getUnknownType(), "Failed to call blockAddHooks.", e.getTargetException());
        }
        // sorting is lowest to highest for restore
        List<CheckpointHookService> multiThreadRestoreHooks = getHooks(this.hooksMultiThreaded);
        List<CheckpointHookService> singleThreadRestoreHooks = getHooks(this.hooksSingleThreaded);

        // reverse restore hook order for prepare hooks
        List<CheckpointHookService> multiThreadPrepareHooks = new ArrayList<>(multiThreadRestoreHooks);
        Collections.reverse(multiThreadPrepareHooks);
        List<CheckpointHookService> singleThreadPrepareHooks = new ArrayList<>(singleThreadRestoreHooks);
        Collections.reverse(singleThreadPrepareHooks);

        debug(tc, () -> "Multi-threaded prepare order: " + multiThreadPrepareHooks);
        debug(tc, () -> "Single-threaded prepare order: " + singleThreadPrepareHooks);
        debug(tc, () -> "Multi-threaded restore order: " + multiThreadRestoreHooks);
        debug(tc, () -> "Single-threaded restore order: " + singleThreadRestoreHooks);

        //map phase back to the documented command line argument.
        String phaseName;
        switch (checkpointAt) {
            case AFTER_APP_START:
                phaseName = "afterAppStart";
                break;
            case BEFORE_APP_START:
                phaseName = "beforeAppStart";
                break;
            default:
                phaseName = checkpointAt.name();
                break;
        }
        Tr.audit(tc, "CHECKPOINT_DUMP_INITIATED_CWWKC0451", phaseName);

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

        waitForConfig();
        registerRunningCondition();

        Tr.audit(tc, "CHECKPOINT_RESTORE_CWWKC0452I", TimestampUtils.getElapsedTime());

        createRestoreMarker();
    }

    private void callHooksOnFailure(List<CheckpointHookService> singleThreadRestoreHooks, List<CheckpointHookService> multiThreadRestoreHooks) {
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
                    Object featureName = feature.getProperty("ibm.featureName");
                    LibertyFeature libertyFeature = (LibertyFeature) context.getService(feature);
                    if (libertyFeature != null) {
                        try {
                            ManifestElement[] instantonEnabled = ManifestElement.parseHeader(INSTANTON_ENABLED_HEADER, libertyFeature.getHeader(INSTANTON_ENABLED_HEADER));
                            if (!isInstantOnFeature(instantonEnabled, featureName)) {
                                unsupported.add(featureName);
                            }
                        } catch (BundleException e) {
                            // auto FFDC here to capture a bad header
                            unsupported.add(featureName);
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

    private boolean isInstantOnFeature(ManifestElement[] instantonEnabledHeader, Object featureName) {
        if (allowedFeatures.contains(featureName)) {
            return true;
        }
        if (instantonEnabledHeader != null && instantonEnabledHeader.length > 0) {
            if (Boolean.parseBoolean(instantonEnabledHeader[0].getValue())) {
                // check for beta
                String type = instantonEnabledHeader[0].getDirective("type");
                if ("beta".equals(type)) {
                    return ProductInfo.getBetaEdition();
                }
                return true;
            }
        }
        return false;
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

    List<CheckpointHookService> getHooks(List<CheckpointHookService> hookServices) {
        if (hookServices.isEmpty()) {
            debug(tc, () -> "No checkpoint hooks.");
            return Collections.emptyList();
        }

        // using forEach to ensure synchronized copy.
        List<CheckpointHookService> sortedHooks = new ArrayList<>(hookServices.size());
        hookServices.forEach(h -> sortedHooks.add(h));
        Collections.sort(sortedHooks);

        return sortedHooks;
    }

    @FFDCIgnore(Throwable.class)
    @Trivial
    private void callHooks(String operation,
                           List<CheckpointHookService> checkpointHooks,
                           Consumer<CheckpointHook> perform,
                           Function<Throwable, CheckpointFailedException> failed) throws CheckpointFailedException {
        for (CheckpointHookService checkpointHookService : checkpointHooks) {
            try {
                debug(tc, () -> operation + " operation on hook: " + checkpointHookService);
                perform.accept(checkpointHookService.hook);
            } catch (Throwable abortCause) {
                debug(tc, () -> operation + " failed on hook: " + checkpointHookService);
                if (failed != null) {
                    throw failed.apply(abortCause);
                }
            }
        }
    }

    @Trivial
    private void prepare(List<CheckpointHookService> checkpointHooks) throws CheckpointFailedException {
        debug(tc, () -> "Calling prepare hooks on this list: " + checkpointHooks);
        callHooks("prepare",
                  checkpointHooks,
                  CheckpointHook::prepare,
                  CheckpointImpl::failedPrepare);
    }

    private static CheckpointFailedException failedPrepare(Throwable cause) {
        return new CheckpointFailedException(Type.LIBERTY_PREPARE_FAILED, Tr.formatMessage(tc, "CHECKPOINT_FAILED_PREPARE_EXCEPTION_CWWKC0457E", cause.getMessage()), cause);
    }

    @Trivial
    private void restore(List<CheckpointHookService> checkpointHooks) throws CheckpointFailedException {
        // The first thing is to set the jvmRestore flag
        if (jvmRestore.compareAndSet(false, true)) {
            try {
                checkpointPhaseRestored.set(checkpointAt, Boolean.TRUE);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new CheckpointFailedException(getUnknownType(), "Failed to set restored flag on CheckpointPhase", e);
            }
        }
        debug(tc, () -> "Calling restore hooks on this list: " + checkpointHooks);
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

    public static void beforeAppStartCheckpoint() {
        CheckpointImpl current = INSTANCE;
        debug(tc, () -> "Initiating checkpoint from beforeAppStart: " + current);
        if (current != null && current.checkpointAt == CheckpointPhase.BEFORE_APP_START) {
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
