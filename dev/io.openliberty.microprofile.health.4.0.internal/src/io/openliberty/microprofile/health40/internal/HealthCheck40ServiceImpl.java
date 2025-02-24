/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
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

import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;
import io.openliberty.microprofile.health30.internal.HealthCheck30HttpResponseBuilder;
import io.openliberty.microprofile.health40.services.HealthCheck40Executor;

/**
 * Microprofile Health Check Service Implementation
 */
@Component(service = HealthCheck40Service.class, property = { "service.vendor=IBM" }, configurationPid = "io.openliberty.microprofile.health", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true)

public class HealthCheck40ServiceImpl implements HealthCheck40Service {

    private static final TraceComponent tc = Tr.register(HealthCheck40ServiceImpl.class);

    private AppTracker appTracker;
    private HealthCheck40Executor hcExecutor;

    private Timer startedTimer;
    private Timer liveTimer;
    private Timer readyTimer;

    /**
     * The value (in ms) defined for the file update interval.
     * Value of 0 means functionality is disabled.
     *
     * INITIAL STARTING VALUE is -1.
     * Cheapo way of indicating that this config was never configured before to avoid using another variable.
     */
    private volatile int fileUpdateIntevalMilliseconds = -1;

    /**
     *
     * Instead of relying on checking if fileUpdateIntevalMilliseconds is > 0,
     * we'll use this method for readability.
     *
     * @return If the server is configured to use file health check
     */
    private boolean isFileHealthCheckingEnabled() {
        return fileUpdateIntevalMilliseconds > 0;
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
            stopTimers();
        }
    }

    /**
     * Stop all the timers.
     * Potential use: Server is shutting down and references are being deregistered
     */
    private synchronized void stopTimers() {
        /*
         * Beta-guard
         * Not really needed, but just in case.
         */
        if (ProductInfo.getBetaEdition()) {
            if (startedTimer != null) {
                startedTimer.cancel();
                startedTimer = null;
            }

            if (liveTimer != null) {
                liveTimer.cancel();
                liveTimer = null;
            }
            if (readyTimer != null) {
                readyTimer.cancel();
                readyTimer = null;
            }
        }
    }

    @Reference(service = HealthCheck40Executor.class)
    protected void setHealthExecutor(HealthCheck40Executor service) {
        this.hcExecutor = service;
    }

    protected void unsetHealthExecutor(HealthCheck40Executor service) {
        if (this.hcExecutor == service) {
            this.hcExecutor = null;
            stopTimers();
        }
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {

        /*
         * Beta guard
         */
        if (ProductInfo.getBetaEdition()) {
            /*
             * Activation time is only time when check env var
             * for the MP_HEALTH_FILE_UPDATE_INTERVAL only if server.xml
             * does not exist (server.xml overrides everything once server starts).
             */
            String serverFileUpdateIntervalConfig;
            if ((serverFileUpdateIntervalConfig = (String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_FILE_UPDATE_INTERVAL)) != null) {
                processUpdateIntervalConfig(serverFileUpdateIntervalConfig);
            } else {
                processUpdateIntervalConfig(System.getenv(HealthCheckConstants.HEALTH_ENV_CONFIG_FILE_UPDATE_INTERVAL));
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

                /*
                 * Handle special startup case(s)
                 */
                if (isValidSystemForFileHealthCheck) {

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

            } // end isFileHealthCheckingEnabled

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "HealthCheckServiceImpl is activated");
        }

    }

    /**
     * Processes the configuration value for the file update interval.
     * Either from server.xml or read through an environment variable.
     *
     * @param configValue The (possibly null) value read from server.xml or env var.
     */
    protected void processUpdateIntervalConfig(String configValue) {
        if (configValue != null && !configValue.isEmpty()) {

            int prevConfigIntervalMilliseconds = fileUpdateIntevalMilliseconds;
            configValue = configValue.trim();
            if (configValue.matches("^\\d+(ms|s)?$")) {
                if (configValue.endsWith("ms")) {
                    fileUpdateIntevalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 2));
                } else if (configValue.endsWith("s")) {
                    fileUpdateIntevalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 1)) * 1000;
                } else {
                    fileUpdateIntevalMilliseconds = Integer.parseInt(configValue) * 1000;
                }
            } else {
                //TODO: Create warning message with ID
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid value provided for the file update interval"
                                 + "The value provided must consist of a number followed by an optional time unit of 'ms' or 's'."
                                 + "Defaulting to 10 seconds.");
                }
                //Default of 10 seconds.
                fileUpdateIntevalMilliseconds = 10000;
            }

            String updateValueMessage = String.format("The fileUpdateInterval is read in as [%s] and is resolved to be [%d] milliseconds", configValue,
                                                      fileUpdateIntevalMilliseconds);
            /*
             * Check if value has been updated
             * If so, we must stop the existing Timers and start new ones based on the new config (as long as the config isn't 0).
             *
             * If prevConfigIntervalMilliseconds is < 0 that means this is the first read in of the config and we don't need to run the below logic.
             * (We can either be starting the server (with the config) or updating the server.xml during runtime with the fileUpdateInterval config for the first time)
             * Updating during runtime isn't really something you should do, but we must support dynamic server updates.
             *
             */
            if ((!(prevConfigIntervalMilliseconds < 0) && (prevConfigIntervalMilliseconds != fileUpdateIntevalMilliseconds))) {

                updateValueMessage = "The configuration has been updated. " + updateValueMessage;
                stopTimers();

                //Only start new timer processes if config value is not 0.
                if (isFileHealthCheckingEnabled()) {
                    startFileHealthCheckProcesses();
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, updateValueMessage);
            }
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
            processUpdateIntervalConfig((String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_FILE_UPDATE_INTERVAL));
        }

    }

    /** {@inheritDoc} */
    @Override
    public void startFileHealthCheckProcesses() {

        /*
         * Last flag in the if is the beta guard
         */
        if (isValidSystemForFileHealthCheck && isFileHealthCheckingEnabled() && ProductInfo.getBetaEdition()) {

            File startFile = HealthFileUtils.getStartFile();
            File readyFile = HealthFileUtils.getReadyFile();
            File liveFile = HealthFileUtils.getLiveFile();

            /*
             * Start health check process.
             * First check if start file exists, if it does then we must be updating the fileUpdateInterval during runtime after a started file
             * has been created. In no way should this exist from a previous run of this server (i.e. crash) as validation of the system would have
             * failed and this we would never get here if that was the case.
             *
             *
             * Perform a check immediately and if status is DOWN, start the process.
             */
            if (!startFile.exists() && performFileHealthCheck(startFile, HealthCheckConstants.HEALTH_CHECK_START).equals(Status.DOWN)) {
                startedTimer = new Timer(false);
                startedTimer.schedule(new FileUpdateProcess(startFile, HealthCheckConstants.HEALTH_CHECK_START, true), 0, 1000);
            }

            /*
             * Perform an immediate check and then start the processes for checking ready and live status.
             */
            performFileHealthCheck(readyFile, HealthCheckConstants.HEALTH_CHECK_READY);
            readyTimer = new Timer(false);
            readyTimer.schedule(new FileUpdateProcess(readyFile, HealthCheckConstants.HEALTH_CHECK_READY), 0, fileUpdateIntevalMilliseconds);

            performFileHealthCheck(liveFile, HealthCheckConstants.HEALTH_CHECK_LIVE);
            liveTimer = new Timer(false);
            liveTimer.schedule(new FileUpdateProcess(liveFile, HealthCheckConstants.HEALTH_CHECK_LIVE), 0, fileUpdateIntevalMilliseconds);

        }

    }

    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "HealthCheckServiceImpl is deactivated");
        }
        stopTimers();
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

        resolveDefaultStatuses();

        FileHealthCheckBuilder fhc = new FileHealthCheckBuilder(file);

        Set<String> appSet = validateApplicationSet();
        Set<String> unstartedAppSet = new HashSet<String>();

        runHealthChecks(appSet, healthCheckProcedure, unstartedAppSet,
                        status -> fhc.setOverallStatus(status),
                        x -> fhc.handleUndeterminedResponse(),
                        responses -> fhc.addResponses(responses));

        fhc.updateFile();

        issueMessagesForUnstartedApps(unstartedAppSet, healthCheckProcedure);

        return fhc.getOverallStatus();

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

                            hcResponses = hcExecutor.runHealthChecks(appName, moduleName, healthCheckProcedure);

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

                        if (!hcResponses.isEmpty()) {
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

    /**
     * Only used here in this class.
     */
    public class FileUpdateProcess extends TimerTask {

        File file;
        String healthCheckProcedure;
        boolean isStopOnCreate = false;

        public FileUpdateProcess(File file, String healthCheckProcedure) {
            this(file, healthCheckProcedure, false);
        }

        /**
         * Timer task that will execute a performHealthCheck() call on the supplied file.
         *
         * @param file                 The file that this TimerTask will perform file updates on (i.e., update last modified access time).
         * @param healthCheckProcedure The health check procedure.
         * @param isStopOnCreate       Stop this timer if the file exists.
         */
        public FileUpdateProcess(File file, String healthCheckProcedure, boolean isStopOnCreate) {
            this.file = file;
            this.isStopOnCreate = isStopOnCreate;
        }

        @Override
        public void run() {
            performFileHealthCheck(file, HealthCheckConstants.HEALTH_CHECK_START);
            if (isStopOnCreate && file.exists()) {
                cancel();
            }

        }
    }
}
