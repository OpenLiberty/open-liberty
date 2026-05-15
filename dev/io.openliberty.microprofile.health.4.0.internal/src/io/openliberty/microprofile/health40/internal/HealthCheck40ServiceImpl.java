/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
package io.openliberty.microprofile.health40.internal;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.microprofile.health.internal.AppTracker;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.wsspi.wab.configure.WABConfiguration;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;
import io.openliberty.microprofile.health30.internal.HealthCheck30HttpResponseBuilder;
import io.openliberty.microprofile.health40.services.HealthCheck40Executor;

/**
 * Microprofile Health Check Service Implementation
 */
@Component(service = HealthCheck40Service.class, property = { "service.vendor=IBM" }, configurationPid = "io.openliberty.microprofile.health", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true)

public class HealthCheck40ServiceImpl implements HealthCheck40Service {

    private static final TraceComponent tc = Tr.register(HealthCheck40ServiceImpl.class);

    private final AtomicBoolean isAllFilesCreated = new AtomicBoolean(false);
    private final AtomicBoolean isLiveUp = new AtomicBoolean(false);
    private final AtomicBoolean isReadyUp = new AtomicBoolean(false);

    private AppTracker appTracker;
    private HealthCheck40Executor hcExecutor;

    ComponentContext componentContext;

    private Timer createStartedTimer;
    private Timer createLiveTimer;
    private Timer createReadyTimer;
    private Timer updateLiveTimer;
    private Timer updateReadyTimer;

    /**
     * The value (in ms) defined for the (file) check interval.
     * Value of 0 means functionality is disabled.
     *
     * INITIAL STARTING VALUE is -1.
     * Cheapo way of indicating that this config was never configured before to avoid using another variable.
     */
    private volatile int checkIntervalMilliseconds = HealthCheckConstants.CONFIG_NOT_SET;

    /**
     * USE getStartupCheckInterval() to get value.
     * It will provide logic to resolve <= 0 values to the default 100ms.
     * The value (in ms) defined for the (file) startup check interval.
     * Value of 0 defaults back to 100ms.
     *
     */
    private volatile int startupCheckIntervalMilliseconds = HealthCheckConstants.CONFIG_NOT_SET;

    /**
     * Controls whether health endpoints are enabled.
     * Default is true. Only effective when file-based health checks are enabled.
     * Using AtomicBoolean to prevent race conditions during configuration updates.
     */
    private final AtomicBoolean enableEndpoints = new AtomicBoolean(true);

    /**
     * WAB configuration manager for health endpoints
     */
    private HealthWABConfigManager wabConfigManager;

    /**
     * ExecutorService for asynchronous WAB configuration updates
     */
    private final AtomicReference<ExecutorService> executorServiceRef = new AtomicReference<>();

    protected volatile boolean isCheckPointFinished = false;

    /**
     *
     * Instead of relying on checking if checkIntervalMilliseconds is > 0,
     * we'll use this method for readability.
     *
     * @return If the server is configured to use file health check
     */
    private boolean isFileHealthCheckingEnabled() {
        return checkIntervalMilliseconds > 0;
    }

    /**
     * Returns (file) startupCheckInterval; default to 100ms if 0 or not configured or invalid (negative values)
     *
     * @return value of fileCreateInterval
     */
    private int getStartupCheckInterval() {
        return (startupCheckIntervalMilliseconds <= 0) ? HealthCheckConstants.DEFAULT_STARTUP_CHECK_INTERVAL_MILLI : startupCheckIntervalMilliseconds;
    }

    protected boolean isValidSystemForFileHealthCheck = false;
    final AtomicBoolean readinessWarningAlreadyShown = new AtomicBoolean(false);
    final AtomicBoolean startupWarningAlreadyShown = new AtomicBoolean(false);
    AtomicInteger unstartedAppsCounter = new AtomicInteger(0);

    static Status DEFAULT_READINESS_STATUS;

    static Status DEFAULT_STARTUP_STATUS;

    @Reference(service = AppTracker.class)
    protected void setAppTracker(AppTracker service) {
        this.appTracker = service;
        appTracker.setHealthCheckService(this);
    }

    protected void unsetAppTracker(AppTracker service) {
        if (this.appTracker == service) {
            this.appTracker = null;
            stopAllTimers();
        }
    }

    @Reference(service = ExecutorService.class)
    protected void setExecutorService(ExecutorService service) {
        executorServiceRef.set(service);
    }

    protected void unsetExecutorService(ExecutorService service) {
        executorServiceRef.compareAndSet(service, null);
    }

    /**
     * Stop all the timers.
     * Potential use: Server is shutting down and references are being deregistered
     */
    private synchronized void stopAllTimers() {
        if (createStartedTimer != null) {
            createStartedTimer.cancel();
            createStartedTimer = null;
        }

        if (createLiveTimer != null) {
            createLiveTimer.cancel();
            createLiveTimer = null;
        }

        if (createReadyTimer != null) {
            createReadyTimer.cancel();
            createReadyTimer = null;
        }

        if (updateLiveTimer != null) {
            updateLiveTimer.cancel();
            updateLiveTimer = null;
        }
        if (updateReadyTimer != null) {
            updateReadyTimer.cancel();
            updateReadyTimer = null;
        }
    }

    private synchronized void stopUpdateTimers() {

        if (updateLiveTimer != null) {
            updateLiveTimer.cancel();
            updateLiveTimer = null;
        }
        if (updateReadyTimer != null) {
            updateReadyTimer.cancel();
            updateReadyTimer = null;
        }
    }

    private synchronized void stopCreateTimers() {
        if (createStartedTimer != null) {
            createStartedTimer.cancel();
            createStartedTimer = null;
        }

        if (createLiveTimer != null) {
            createLiveTimer.cancel();
            createLiveTimer = null;
        }

        if (createReadyTimer != null) {
            createReadyTimer.cancel();
            createReadyTimer = null;
        }
    }

    @Reference(service = HealthCheck40Executor.class)
    protected void setHealthExecutor(HealthCheck40Executor service) {
        this.hcExecutor = service;
    }

    protected void unsetHealthExecutor(HealthCheck40Executor service) {
        if (this.hcExecutor == service) {
            this.hcExecutor = null;
            stopAllTimers();
        }
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {

        componentContext = cc;

        // Initialize WAB configuration manager
        wabConfigManager = new HealthWABConfigManager(HealthCheckConstants.HEALTH_CONTEXT_PATH_VAR_NAME, HealthCheckConstants.HEALTH_CONTEXT_PATH);

        processConfig();

        // Process enableEndpoints and register/unregister WAB accordingly
        processEnableEndpointsWithWAB(cc);

        /*
         * Handle special case during activation.
         * IF file-based HC enabled, but there are no apps, we need to explicitly
         * start file-based health check process. The invocation is kick-started
         * by applicationStarted(), but there are no apps!
         */
        if (isFileHealthCheckingEnabled() && isValidSystemForFileHealthCheck) {

            /*
             * If there are no applications deployed.
             * Kick off the file health check processes.
             *
             * These will immediately create all three files
             * and then continually run the live and ready checks.
             * (which will always be UP.. forever.. and ever..).
             */
            Set<String> apps = validateApplicationSet();
            if (apps.size() == 0) {
                startFileHealthCheckProcesses();
            }

        }
        /*
         * If createUpdateInterval is set (not -1) , but fileUpdateInterval is not set. Issue warning.
         */
        else if (!isFileHealthCheckingEnabled() && (startupCheckIntervalMilliseconds != HealthCheckConstants.CONFIG_NOT_SET)) {
            Tr.warning(tc, "startup.check.interval.config.only.set.CWMMH01012W");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "HealthCheckServiceImpl is activated");
        }

    }

    protected void processConfig() {

        Map<String, Object> properties = (Map<String, Object>) componentContext.getProperties();

        String serverCheckIntervalConfig;
        if ((serverCheckIntervalConfig = (String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_CHECK_INTERVAL)) != null) {
            processCheckIntervalConfig(serverCheckIntervalConfig);
        } else {
            processCheckIntervalConfig(System.getenv(HealthCheckConstants.HEALTH_ENV_CONFIG_CHECK_INTERVAL));
        }

        //resolve startupCheckInterval config
        String serverStartupCheckIntervalConfig;
        if ((serverStartupCheckIntervalConfig = (String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_STARTUP_CHECK_INTERVAL)) != null) {
            processStartupCheckIntervalConfig(serverStartupCheckIntervalConfig);
        } else {
            processStartupCheckIntervalConfig(System.getenv(HealthCheckConstants.HEALTH_ENV_CONFIG_STARTUP_CHECK_INTERVAL));
        }

        // Resolve enableEndpoints config (beta feature only)
        if (ProductInfo.getBetaEdition()) {
            Boolean enableEndpointsConfig = null;
            
            // Check environment variable first
            String envConfig = System.getenv(HealthCheckConstants.HEALTH_ENV_CONFIG_ENABLE_ENDPOINTS);
            if (envConfig != null && !envConfig.trim().isEmpty()) {
                String trimmedEnvConfig = envConfig.trim();
                // Only parse if it's a valid boolean string (case-insensitive)
                // Invalid values are ignored, allowing server config or default (true) to be used
                if (trimmedEnvConfig.equalsIgnoreCase("true") || trimmedEnvConfig.equalsIgnoreCase("false")) {
                    enableEndpointsConfig = Boolean.valueOf(trimmedEnvConfig);
                } else {
                    Tr.warning(tc, "enable.endpoints.env.var.invalid.value.CWMMH01014W", trimmedEnvConfig);
                }
            }
            
            // If env var not set, check server config
            // Note: properties.get() may return default value even if not explicitly set in server.xml
            // We check env var first to allow it to override the default
            if (enableEndpointsConfig == null) {
                enableEndpointsConfig = (Boolean) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_ENABLE_ENDPOINTS);
            }

            // Process the resolved config value
            if (enableEndpointsConfig != null) {
                processEnableEndpointsConfig(enableEndpointsConfig);
            }
        } else {
            // Not in beta edition - skip enableEndpoints configuration entirely
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "enableEndpoints configuration skipped - feature only available in beta edition");
            }
        }

        if (isFileHealthCheckingEnabled()) {
            //Validate system for File Health Checks (i.e., File I/O)
            try {
                isValidSystemForFileHealthCheck = HealthFileUtils.isValidSystem();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Is system valid for File health check: " + isValidSystemForFileHealthCheck);
                }
            } catch (IOException e) {
                //Let FFDC handle this.
            }
        }
    }

    /**
     * Processes the configuration value for the (file) check interval.
     * Either from server.xml or read through an environment variable.
     *
     * @param configValue The (possibly null) value read from server.xml or env var.
     */
    protected void processCheckIntervalConfig(String configValue) {
        if (configValue != null && !configValue.trim().isEmpty()) {

            int prevCheckIntervalConfigMilliseconds = checkIntervalMilliseconds;
            configValue = configValue.trim();
            if (configValue.matches("^\\d+(ms|s)?$")) {
                if (configValue.endsWith("ms")) {
                    checkIntervalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 2));
                } else if (configValue.endsWith("s")) {
                    checkIntervalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 1)) * 1000;
                } else {
                    checkIntervalMilliseconds = Integer.parseInt(configValue) * 1000; //convert to seconds; that is default time unit.
                }
            } else {
                Tr.warning(tc, "check.interval.config.invalid.CWMMH01010W", configValue);
                //Default of 10 seconds.
                checkIntervalMilliseconds = HealthCheckConstants.DEFAULT_CHECK_INTERVAL_MILLI;
            }

            String updateValueMessage = String.format("The checkInterval is read in as [%s] and is resolved to be [%d] milliseconds", configValue,
                                                      checkIntervalMilliseconds);
            /*
             * Check if value has been updated
             * If so, we must stop the existing Timers and start new ones based on the new config (as long as the config isn't 0).
             * Zero/0 means disable!
             *
             * If prevConfigIntervalMilliseconds is < 0 that means this is the first read in of the config and we don't need to run the below logic.
             * (We can either be starting the server (with the config) or updating the server.xml during runtime with the fileUpdateInterval config for the first time)
             * Updating during runtime isn't really something you should do, but we must support dynamic server updates.
             *
             */
            if ((!(prevCheckIntervalConfigMilliseconds < 0) && (prevCheckIntervalConfigMilliseconds != checkIntervalMilliseconds))) {

                updateValueMessage = "The configuration has been updated. " + updateValueMessage;
                stopUpdateTimers();

                /*
                 * If we're already in the update phase when config was modified.
                 * Call method to start the update timers.
                 *
                 * This is an very unlikely scenario. file-based/local health checks are
                 * only valid in a Kubernetes use case. Running this outside of a container environment
                 * will not occur unless somebody is "experimenting".
                 *
                 * Otherwise, nothing to do.
                 */
                if (isFileHealthCheckingEnabled() && isAllFilesCreated.get()) {
                    startUpdateHealthCheckFileProcesses();
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, updateValueMessage);
            }
        }
    }

    /**
     * Processes the configuration value for the (file) startup check interval.
     * Either from server.xml or read through an environment variable.
     *
     * @param configValue The (possibly null) value read from server.xml or env var.
     */
    protected void processStartupCheckIntervalConfig(String configValue) {

        if (configValue != null && !configValue.trim().isEmpty()) {

            configValue = configValue.trim();
            if (configValue.matches("^\\d+(ms|s)?$")) {
                if (configValue.endsWith("ms")) {
                    startupCheckIntervalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 2));
                } else if (configValue.endsWith("s")) {
                    startupCheckIntervalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 1)) * 1000;
                } else {
                    startupCheckIntervalMilliseconds = Integer.parseInt(configValue); //Parse as-is , default time-unit is ms.
                }
            } else {
                Tr.warning(tc, "startup.check.interval.config.invalid.CWMMH01011W", configValue);

                //Default of 100ms.
                startupCheckIntervalMilliseconds = HealthCheckConstants.DEFAULT_STARTUP_CHECK_INTERVAL_MILLI;
            }

            /*
             * If this is part of a config update, no need to stop timers.
             * That would imply they decided to change create interval via server.xml right as the server starts
             * or immediately after they deployed an app into an already running server.
             */

            String updateValueMessage = String.format("The startupCheckInterval is read in as [%s] and is resolved to be [%d] milliseconds", configValue,
                                                      startupCheckIntervalMilliseconds);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, updateValueMessage);
            }
        }
    }

    /**
     * Processes the resolved configuration value for enableEndpoints.
     * The value can come from either server.xml config or environment variable.
     *
     * @param configValue The resolved Boolean value for enableEndpoints
     * @return true if the value changed, false otherwise
     */
    protected boolean processEnableEndpointsConfig(Boolean configValue) {
        if (configValue != null) {
            boolean newValue = configValue.booleanValue();
            boolean previousValue = enableEndpoints.getAndSet(newValue);

            // Only show warning if user tries to disable endpoints (false) without file-based health checks enabled
            // Don't log warning for enableEndpoints=true since that's the default behavior anyway
            if (!isFileHealthCheckingEnabled() && !newValue) {
                Tr.warning(tc, "enable.endpoints.config.without.file.health.check.CWMMH01013W");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "enableEndpoints configuration resolved to: " + newValue);
            }

            return previousValue != newValue;
        }
        return false;
    }

    /**
     * Process enableEndpoints configuration and register/unregister WAB accordingly.
     * Only effective when file-based health checks are enabled.
     *
     * @param context The ComponentContext to use for WAB registration
     */
    protected void processEnableEndpointsWithWAB(ComponentContext context) {
        // Null check to prevent NPE if called before activation or after deactivation
        if (wabConfigManager == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "wabConfigManager is null, skipping WAB configuration");
            }
            return;
        }

        if (isFileHealthCheckingEnabled() && isValidSystemForFileHealthCheck) {
            // File-based health checks are enabled, respect enableEndpoints setting
            wabConfigManager.processEnableEndpoints(context, enableEndpoints.get());
        } else {
            // File-based health checks are not enabled, always enable endpoints
            wabConfigManager.processEnableEndpoints(context, true);
        }
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        /*
         * During server.xml update, never check for server env.
         */

        /*
         * If system was not valid for health check, skip on checking configuration update. We've already indicated that this system is
         * not fit for file-based health checks.
         */
        if (isValidSystemForFileHealthCheck) {
            processCheckIntervalConfig((String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_CHECK_INTERVAL));
            processStartupCheckIntervalConfig((String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_STARTUP_CHECK_INTERVAL));
            
            // processEnableEndpointsConfig returns true if the value changed
            boolean enableEndpointsChanged = processEnableEndpointsConfig((Boolean) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_ENABLE_ENDPOINTS));

            // Only execute WAB update if enableEndpoints actually changed
            if (enableEndpointsChanged) {
                // Try to execute WAB configuration update asynchronously to avoid FFDC of using invalid bundle context during component reconfiguration
                ExecutorService executor = executorServiceRef.get();
                if (executor != null) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                processEnableEndpointsWithWAB(context);
                            } catch (Exception e) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Exception during asynchronous WAB update", e);
                                }
                            }
                        }
                    });
                } else {
                    // ExecutorService not available - fall back to synchronous execution
                    // This ensures configuration changes take effect even if async execution isn't available
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ExecutorService not available, executing WAB update synchronously");
                    }
                    try {
                        processEnableEndpointsWithWAB(context);
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception during synchronous WAB update", e);
                        }
                    }
                }
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void startFileHealthCheckProcesses() {
        /*
         * If we got here, that means we've been restored (or this is a normal run)
         */
        isCheckPointFinished = true;

        /*
         * For an instantOn scenario, re-process config including for env var
         */
        if (!CheckpointPhase.getPhase().equals(CheckpointPhase.INACTIVE)) {
            System.out.println("debug: reprocess");
            processConfig();
        }

        if (isValidSystemForFileHealthCheck && isFileHealthCheckingEnabled()) {

            File startFile = HealthFileUtils.getStartFile();

            /*
             * Kick off start file creation process.
             * Queries each fileCreateInterval until UP.
             */
            if (!startFile.exists()) {

                createStartedTimer = new Timer(true);
                createStartedTimer.schedule(new StartedFileCreateProcess(), 0, getStartupCheckInterval());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Initiated startup phase for local health check funcitonality. Querying for first succesful startup status.");
                }
            } else {

                /*
                 * Case where started file already exists. Which it shouldn't since that's part of the system
                 * validation check.
                 *
                 * Something is very wrong and we should not continue.
                 */

                Tr.warning(tc, "file.healthcheck.system.inconsistency.CWMMH0107W");
            }
        }
    }

    public void startLiveFileCreateHealthCheckProcess() {
        if (isValidSystemForFileHealthCheck && isFileHealthCheckingEnabled()) {
            File liveFile = HealthFileUtils.getLiveFile();
            createLiveTimer = new Timer(true);
            createLiveTimer.schedule(new FileCreateProcess(liveFile, HealthCheckConstants.HEALTH_CHECK_LIVE), 0, getStartupCheckInterval());

        }
    }

    public void startReadyFileCreateHealthCheckProcess() {
        if (isValidSystemForFileHealthCheck && isFileHealthCheckingEnabled()) {
            File readyFile = HealthFileUtils.getReadyFile();
            createReadyTimer = new Timer(true);
            createReadyTimer.schedule(new FileCreateProcess(readyFile, HealthCheckConstants.HEALTH_CHECK_READY), 0, getStartupCheckInterval());
        }
    }

    /*
     * Applies to the update of liveness and readiness files
     */
    public void startUpdateHealthCheckFileProcesses() {
        if (isValidSystemForFileHealthCheck && isFileHealthCheckingEnabled()) {

            File readyFile = HealthFileUtils.getReadyFile();
            File liveFile = HealthFileUtils.getLiveFile();

            //Cancel again just in case.
            if (updateReadyTimer != null) {
                updateReadyTimer.cancel();
            }

            if (updateLiveTimer != null) {
                updateLiveTimer.cancel();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Initiated update phase for local health check funcitonality.");
            }
            /*
             * Timers to update the Live and ready files at the fileUpdateIntervalMilliseconds.
             */
            updateReadyTimer = new Timer(true);
            updateReadyTimer.schedule(new FileUpdateProcess(readyFile, HealthCheckConstants.HEALTH_CHECK_READY), 0, checkIntervalMilliseconds);

            updateLiveTimer = new Timer(true);
            updateLiveTimer.schedule(new FileUpdateProcess(liveFile, HealthCheckConstants.HEALTH_CHECK_LIVE), 0, checkIntervalMilliseconds);
        }
    }

    /**
     *
     * This method is called twice. Once when Ready status is UP
     * for the first time and once when Live status UP for the first time.
     * We want to ensure the second call captures the READY=UP and LIVE=UP
     * to create all three health check files.
     *
     * IF not synchronized, we could get to the check where:
     * - READY finishes, checks if Live is finished.
     */
    public synchronized void reportUpStatusFor(String healthCheckProcedure) {

        //Toggle appropriate flags
        if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_READY)) {
            isReadyUp.set(true);
        } else if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE)) {
            isLiveUp.set(true);
        }

        /*
         * If both files are created (It is implied that start is UP).
         * We create all health files.
         *
         * We kick off update processes for ready and live files.
         */
        if (isReadyUp.get() == true && isLiveUp.get() == true) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating `started`, `live` and `ready` health check files");
            }

            HealthFileUtils.createFile(HealthFileUtils.getStartFile());
            HealthFileUtils.createFile(HealthFileUtils.getLiveFile());
            HealthFileUtils.createFile(HealthFileUtils.getReadyFile());

            isAllFilesCreated.set(true);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Startup phase for local health check functionality completed.");
            }

            startUpdateHealthCheckFileProcesses();
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "HealthCheckServiceImpl is deactivated");
        }

        stopAllTimers();

        // Unregister WAB
        if (wabConfigManager != null) {
            wabConfigManager.deactivate();
        }
    }

    /**
     * Resolve MP Config properties at startup and set default status.
     */
    private void resolveDefaultStatuses() {
        String mpConfig_defaultReadiness = ConfigProvider.getConfig().getOptionalValue(HealthCheckConstants.DEFAULT_OVERALL_READINESS_STATUS, String.class).orElse("");
        String mpConfig_defaultStartup = ConfigProvider.getConfig().getOptionalValue(HealthCheckConstants.DEFAULT_OVERALL_STARTUP_STATUS, String.class).orElse("");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "In performHealthCheck(): The default overall Readiness status was configured to be overriden: mp.health.default.readiness.empty.response="
                         + mpConfig_defaultReadiness);
            Tr.debug(tc, "In performHealthCheck(): The default overall Startup status was configured to be overriden: mp.health.default.startup.empty.response="
                         + mpConfig_defaultStartup);
        }

        DEFAULT_READINESS_STATUS = mpConfig_defaultReadiness.equalsIgnoreCase("UP") ? Status.UP : Status.DOWN;
        DEFAULT_STARTUP_STATUS = mpConfig_defaultStartup.equalsIgnoreCase("UP") ? Status.UP : Status.DOWN;

    }

    /**
     * Retrieve the current set of visible apps.
     */
    private Set<String> validateApplicationSet() throws NullPointerException {
        Set<String> apps = appTracker.getAllAppNames();
        Set<String> configApps = appTracker.getAllConfigAppNames();

        Iterator<String> configAppsIt = configApps.iterator();

        while (configAppsIt.hasNext()) {
            String nextAppName = configAppsIt.next();
            if (apps.contains(nextAppName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "In performHealthCheck(): configAdmin found an application that the applicationStateListener already found. configAdminAppName = " + nextAppName);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "In performHealthCheck(): applicationStateListener couldn't find application. configAdmin added appName = " + nextAppName);
                appTracker.addAppName(nextAppName);
            }
        }

        apps = appTracker.getAllAppNames();

        return apps;
    }

    /** {@inheritDoc} */
    @Override
    public void performHealthCheck(HttpServletRequest request, HttpServletResponse httpResponse) {
        performHealthCheck(request, httpResponse, HealthCheckConstants.HEALTH_CHECK_ALL);
    }

    @Override
    public void performHealthCheck(HttpServletRequest request, HttpServletResponse httpResponse, String healthCheckProcedure) {

        resolveDefaultStatuses();

        HealthCheck30HttpResponseBuilder hcHttpResponseBuilder = new HealthCheck30HttpResponseBuilder();
        Set<String> appSet = validateApplicationSet();
        Set<String> unstartedAppSet = new HashSet<String>();

        runHealthChecks(appSet, healthCheckProcedure, unstartedAppSet, status -> hcHttpResponseBuilder.setOverallStatus(status),
                        x -> hcHttpResponseBuilder.handleUndeterminedResponse(httpResponse),
                        responses -> hcHttpResponseBuilder.addResponses(responses));

        issueMessagesForUnstartedApps(unstartedAppSet, healthCheckProcedure);

        hcHttpResponseBuilder.setHttpResponse(httpResponse);
    }

    @Override
    public Status performFileHealthCheck(File file, String healthCheckProcedure) {

        /*
         * For a checkpoint/restore environment.
         * If an application image is ever built with more than one app,
         * we need to make sure those immediate startup/started checks
         * from application started don't ever get called.
         *
         * There may a configuration where the `started` file is created
         * before all apps report "started".
         *
         * Example:
         * Apps: A, B ,C.
         * 1. App A starts. (async call held for starting health processes)
         * 2. App B starts. Startup status is UP.
         * - App C is not started, so this would traditionally return a overall DOWN Status
         * - But if MP Config is set for default START (by accident), then overall Status is UP.
         * - resulting in `started` file created for the checkpoint image.
         */
        if (!isCheckPointFinished) {
            return null;
        }

        /*
         * Entry point through AppTracker40Impl, needs to verify that system is valid, and we're enabled
         */
        if (isValidSystemForFileHealthCheck && isFileHealthCheckingEnabled()) {
            resolveDefaultStatuses();

            FileHealthCheckBuilder fhc = new FileHealthCheckBuilder(file);

            Set<String> appSet = validateApplicationSet();
            Set<String> unstartedAppSet = new HashSet<String>();

            runHealthChecks(appSet, healthCheckProcedure, unstartedAppSet,
                            status -> fhc.setOverallStatus(status),
                            x -> fhc.handleUndeterminedResponse(),
                            responses -> fhc.addResponses(responses));

            //Only update file if all health files had been created.
            if (isAllFilesCreated.get() == true) {
                fhc.updateFile();
            }

            issueMessagesForUnstartedApps(unstartedAppSet, healthCheckProcedure);

            return fhc.getOverallStatus();
        }

        return null;

    }

    public void issueMessagesForUnstartedApps(Set<String> unstartedAppsSet, String healthCheckProcedure) {
        if (unstartedAppsSet.isEmpty()) {
            // If all applications are started, reset counter
            unstartedAppsCounter.set(0);
        } else if (!unstartedAppsSet.isEmpty() && unstartedAppsCounter.get() != unstartedAppsSet.size()) {
            // Update the new number of unstarted applications, since some applications may have already started.
            unstartedAppsCounter.set(unstartedAppsSet.size());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "In performHealthCheck(): numOfUnstartedApps after unstarted app set was updated. = " + unstartedAppsCounter.get());
            }

            // If there are other applications that have not started yet, show the message again, with the updated set.
            if (!unstartedAppsSet.isEmpty()) {
                readinessWarningAlreadyShown.set(false);
                startupWarningAlreadyShown.set(false);
            } else {
                readinessWarningAlreadyShown.set(true);
                startupWarningAlreadyShown.set(true);
            }

        }

        if (!unstartedAppsSet.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "In performHealthCheck(): numOfUnstartedApps = " + unstartedAppsCounter.get());
            }

            if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_START) && startupWarningAlreadyShown.compareAndSet(false, true)
                && !DEFAULT_STARTUP_STATUS.equals(Status.UP)) {
                Tr.warning(tc, "startup.healthcheck.applications.not.started.down.CWMMH0054W", new Object[] { unstartedAppsSet });
            } else if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_READY) && readinessWarningAlreadyShown.compareAndSet(false, true)
                       && !DEFAULT_READINESS_STATUS.equals(Status.UP)) {
                Tr.warning(tc, "readiness.healthcheck.applications.not.started.down.CWMMH0053W", new Object[] { unstartedAppsSet });
            }
        }
    }

    /**
     * Method to run the specified health check
     *
     * @param <T>
     * @param appSet               Current set of visible/known applications
     * @param healthCheckProcedure The health check procedure
     * @param unstartedAppsSet     Reference to a set of unstartedApps (used by caller for further processing)
     * @param setOverallStatusFx   Consumer function to handle setting "overall status"
     * @param handleUndeterminedFx Consumer function to handle undetermined responses.
     * @param evaluatedStatusFx    Consumer function to process a set of health check statuses.
     */
    private <T> void runHealthChecks(Set<String> appSet, String healthCheckProcedure, Set<String> unstartedAppsSet, Consumer<Status> setOverallStatusFx,
                                     Consumer<T> handleUndeterminedFx,
                                     Consumer<Set<HealthCheckResponse>> evaluatedStatusFx) {

        Set<HealthCheckResponse> hcResponses = null;
        boolean anyAppsInstalled = false;

        Iterator<String> appsIt = appSet.iterator();

        while (appsIt.hasNext()) {
            String appName = appsIt.next();
            if (appTracker.isInstalled(appName)) {
                anyAppsInstalled = true;
                if (!healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE) && !unstartedAppsSet.contains(appName)) {
                    unstartedAppsSet.add(appName);
                }
            } else if (!appTracker.isUninstalled(appName) && !appTracker.isStarted(appName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "In performHealthCheck(): Application : " + appName + " has not started yet.");
                }
                if (!(healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE))) {
                    if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_START)) {

                        setOverallStatusFx.accept(DEFAULT_STARTUP_STATUS);
                    } else if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_READY)) {
                        setOverallStatusFx.accept(DEFAULT_READINESS_STATUS);
                    } else {
                        // If the /health is hit, it should have the aggregated status of the individual health check procedures
                        setOverallStatusFx.accept((DEFAULT_STARTUP_STATUS.equals(Status.UP)
                                                   && DEFAULT_READINESS_STATUS.equals(Status.UP)) ? Status.UP : Status.DOWN);
                    }

                    // Keep track of the unstarted applications names
                    if (!unstartedAppsSet.contains(appName)) {
                        unstartedAppsSet.add(appName);
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "In performHealthCheck(): unstartedAppsSet after adding the unstarted app : " + unstartedAppsSet);
                    }
                } else {
                    // for liveness check
                    setOverallStatusFx.accept(Status.UP);
                }
            } else {
                Set<String> modules = appTracker.getModuleNames(appName);
                if (modules != null) {
                    Iterator<String> moduleIt = modules.iterator();

                    while (moduleIt.hasNext()) {
                        String moduleName = moduleIt.next();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "In performHealthCheck(): appName = " + appName + ", moduleName = " + moduleName);
                        }
                        try {
                            /*
                             * Prevent calls to any apps that are "stopping".
                             */
                            if (!stoppingApplication.contains(appName)) {
                                hcResponses = hcExecutor.runHealthChecks(appName, moduleName, healthCheckProcedure);
                            }

                        } catch (HealthCheckBeanCallException e) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "In performHealthCheck(): Caught the exception " + e + " for appName = " + appName + ", moduleName = " + moduleName);
                            }

                            handleUndeterminedFx.accept(null);
                            return;
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "In performHealthCheck(): hcResponses = " + hcResponses);
                        }

                        if (hcResponses != null && !hcResponses.isEmpty()) {
                            evaluatedStatusFx.accept(hcResponses);
                        }
                    }
                }
            }
        }

        if (anyAppsInstalled && !(healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE))) {
            setOverallStatusFx.accept(Status.DOWN);
        }
    }

    @Override
    public void removeModuleReferences(String appName, String moduleName) {
        if (hcExecutor != null) {
            hcExecutor.removeModuleReferences(appName, moduleName);
        }
    }

    /*
     * Temp solution to app shutting down and a performFileHealthCheck running at the same time.
     * This leads to scenario where the application is completely shut down while the Health Check
     * process is underway. Specifically when a Contextual Proxy is created right before app stops,
     * followed by the app stopping and then the invocation of the contextual proxy. This causes an
     * ISE due to the app metadata no longer existing during the proxy invocation.
     *
     * The set and the `stopping` and `started` method calls below allow us to create a block for
     * any health checks conducted on a `stopping` App. The set is cleared when the app is redeployed.
     */
    Set<String> stoppingApplication = ConcurrentHashMap.newKeySet();

    public void stoppingApplication(String appName) {
        stoppingApplication.add(appName);
    }

    public void startedApplication(String appName) {
        stoppingApplication.remove(appName);
    }

    /**
     * Only used here in this class.
     * TimerTask used in the update phase.
     * Will run forever quering its assigned health check type until cancelled.
     */
    public class FileUpdateProcess extends TimerTask {

        File file;
        String healthCheckProcedure;

        /**
         * Timer task that will execute a performHealthCheck() call on the supplied file.
         *
         * @param file                 The file that this TimerTask will perform file updates on (i.e., update last modified access time).
         * @param healthCheckProcedure The health check procedure.
         */
        public FileUpdateProcess(File file, String healthCheckProcedure) {
            this.file = file;
            this.healthCheckProcedure = healthCheckProcedure;
        }

        @Override
        public void run() {
            performFileHealthCheck(file, healthCheckProcedure);
        }
    }

    /**
     * Only used here in this class.
     * Specific TimerTask for the creation process of started file.
     * Once a successful UP status is resolved, it will kick of the creation process
     * for the live and ready files. It will also cancel itself.
     */
    public class StartedFileCreateProcess extends TimerTask {

        File startFile = HealthFileUtils.getStartFile();
        String healthCheckProcedure = HealthCheckConstants.HEALTH_CHECK_START;

        @Override
        public void run() {
            Status retStatus = performFileHealthCheck(startFile, healthCheckProcedure);
            if (retStatus.equals(Status.UP)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "First UP Status resolved for the health check procedure: " + healthCheckProcedure);
                }
                /*
                 * Startup status is UP. This Timer is no longer useful.
                 * Defer file creation until both Ready and Live status are UP.
                 */
                cancel();
                /*
                 * Start the creation processs for live and ready files
                 */
                startLiveFileCreateHealthCheckProcess();
                startReadyFileCreateHealthCheckProcess();
            }

        }
    }

    /**
     * Only used here in this class.
     * This TimerTask is for the creation process of a live OR ready file.
     * When a UP status is resolved, it will notify the HealthCheckService
     * that the respective health check has reported an UP. This task will also cancel itself
     * at this time.
     */
    public class FileCreateProcess extends TimerTask {

        File file;
        String healthCheckProcedure;

        public FileCreateProcess(File file, String healthCheckProcedure) {
            this.file = file;
            this.healthCheckProcedure = healthCheckProcedure;
        }

        @Override
        public void run() {
            Status retStatus = performFileHealthCheck(file, healthCheckProcedure);
            if (retStatus.equals(Status.UP)) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "First UP Status resolved for the health check procedure: " + healthCheckProcedure);
                }
                /*
                 * The status is UP (either for live or ready). This Timer is no longer useful.
                 *
                 * Report the UP status to HealthCheckService.
                 */
                cancel();
                reportUpStatusFor(healthCheckProcedure);
            }

        }
    }

    /**
     * Manages the WAB (Web Application Bundle) registration for health endpoints.
     * When endpoints are disabled, the WAB is unregistered, making all health endpoints unavailable.
     *
     * The WAB bundle's Web-ContextPath is set to @healthContextPath in bnd.bnd.
     * This manager registers a WABConfiguration service with the contextName="healthContextPath"
     * and contextPath="/health" properties to configure the WAB's actual context path.
     */
    private static class HealthWABConfigManager {
        private static final TraceComponent tc = Tr.register(HealthWABConfigManager.class);

        private final String contextName;
        private final String contextPath;
        private ServiceRegistration<WABConfiguration> wabConfigReg;

        /**
         * Constructor
         *
         * @param contextName The context name matching the variable in Web-ContextPath (e.g., "healthContextPath")
         * @param contextPath The actual context path value (e.g., "/health")
         */
        public HealthWABConfigManager(String contextName, String contextPath) {
            this.contextName = contextName;
            this.contextPath = contextPath;
        }

        /**
         * Register the WAB configuration to make health endpoints available.
         *
         * @param context     Component context
         * @param contextPath The context path to use
         */
        public void pushConfiguration(ComponentContext context, String contextPath) {
            if (wabConfigReg != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Health WAB already registered", toString());
                }
                return;
            }

            BundleContext bundleContext = context.getBundleContext();

            // Check if BundleContext is still valid before attempting to register service
            // This prevents IllegalStateException during component reconfiguration
            try {
                bundleContext.getBundle();
            } catch (IllegalStateException e) {
                // BundleContext is no longer valid - skip registration
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping Health WAB registration - BundleContext no longer valid");
                }
                return;
            }

            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put(WABConfiguration.CONTEXT_NAME, contextName);
            props.put(WABConfiguration.CONTEXT_PATH, contextPath);

            // WABConfiguration is a marker interface with no methods
            // Configuration is done through service properties
            WABConfiguration wabConfig = new WABConfiguration() {
            };

            wabConfigReg = bundleContext.registerService(WABConfiguration.class, wabConfig, props);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Health WAB registered", toString());
            }
        }

        /**
         * Unregister the WAB configuration to make health endpoints unavailable.
         */
        public void deactivate() {
            if (wabConfigReg != null) {
                try {
                    wabConfigReg.unregister();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Health WAB unregistered", toString());
                    }
                } catch (IllegalStateException e) {
                    // Service already unregistered or BundleContext no longer valid
                    // This is expected during shutdown or dynamic updates
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to unregister Health WAB - already unregistered or BundleContext no longer valid", e);
                    }
                } finally {
                    wabConfigReg = null;
                }
            }
        }

        /**
         * Process the enableEndpoints configuration.
         * Register or unregister the WAB based on the enabled flag.
         *
         * @param context Component context
         * @param enabled Whether endpoints should be enabled
         */
        public void processEnableEndpoints(ComponentContext context, boolean enabled) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Health enableEndpoints attribute updated: " + enabled + " WAB config=" + wabConfigReg);
            }

            // If enabled="false" and WAB has never started, no work to do
            if (wabConfigReg == null && !enabled) {
                return;
            }

            if (enabled) {
                // Register WAB to make endpoints available
                pushConfiguration(context, contextPath);
            } else {
                // Unregister WAB to make endpoints unavailable
                deactivate();
            }
        }

        /**
         * Update the WAB configuration with new properties.
         *
         * @param props New properties
         */
        public void modified(Dictionary<String, String> props) {
            if (wabConfigReg != null) {
                wabConfigReg.setProperties(props);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Health WAB modified", toString(), "props=" + props);
                }
            }
        }

        @Override
        public String toString() {
            return "HealthWABConfigManager [contextPath=" + contextPath + ", contextName=" + contextName +
                   ", wabConfigReg=" + wabConfigReg + "]";
        }
    }

}
