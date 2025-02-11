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

    private volatile int fileUpdateIntevalMilliseconds = 0;

//    private final String

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

    /*
     * Stop all timers.
     * Server shut down.
     * Invoked if References are being deregistered
     */
    private void stopTimers() {
        if (startedTimer != null) {
            startedTimer.cancel();
        }

        if (liveTimer != null) {
            liveTimer.cancel();
        }
        if (readyTimer != null) {
            readyTimer.cancel();
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
        resolveDefaultStatues();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "HealthCheckServiceImpl is activated");

        System.out.println("The properties are: " + properties);

        /*
         * During activation.
         * If no server.xml config, check server env.
         */
        String serverFileUpdateIntervalConfig;
        if ((serverFileUpdateIntervalConfig = (String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_FILE_UPDATE_INTERVAL)) != null) {
            processUpdateIntervalConfig(serverFileUpdateIntervalConfig);
        } else {
            processUpdateIntervalConfig(System.getenv(HealthCheckConstants.HEALTH_ENV_CONFIG_FILE_UPDATE_INTERVAL));
        }

        if (fileUpdateIntevalMilliseconds > 0) {
            //Validate system for File Health Checks (i.e., File I/O)
            try {
                isValidSystemForFileHealthCheck = HealthFileUtils.initHealthFileValidation();
                System.out.println("isValid? " + isValidSystemForFileHealthCheck);
            } catch (IOException e) {
                //TODO: something
                e.printStackTrace();
            }
        }

    }

    /**
     * Dissects the value read from server.xml or serveer.env sets the fileUpdateIntervalMilliseconds
     *
     * @param configValue The (possibly null) value read from server.xml or env var.
     */
    protected void processUpdateIntervalConfig(String configValue) {
        if (configValue != null && !configValue.isEmpty()) {

            configValue = configValue.trim();
            if (configValue.matches("^\\d+(ms|s)?$")) {
                if (configValue.endsWith("ms")) {
                    fileUpdateIntevalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 2));
                } else if (configValue.endsWith("s")) {
                    fileUpdateIntevalMilliseconds = Integer.parseInt(configValue.substring(0, configValue.length() - 1)) * 1000;
                } else {
                    fileUpdateIntevalMilliseconds = Integer.parseInt(configValue) * 1000;
                }

                //validate
            } else {
                //TODO: log warning
                System.out.println("The value provided does not adhere to <number>[ms/s], defaulting to 10s");
                fileUpdateIntevalMilliseconds = 10000;
            }
            System.out.println("The value is " + fileUpdateIntevalMilliseconds);
        }

    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        /*
         * During server.xml update, never check for server env.
         */
        processUpdateIntervalConfig((String) properties.get(HealthCheckConstants.HEALTH_SERVER_CONFIG_FILE_UPDATE_INTERVAL));
    }

    /** {@inheritDoc} */
    @Override
    public void startFileHealthCheckProcesses() {
        if (isValidSystemForFileHealthCheck && fileUpdateIntevalMilliseconds > 0) {

            File startFile = HealthFileUtils.getStartFile();
            File readyFile = HealthFileUtils.getReadyFile();
            File liveFile = HealthFileUtils.getLiveFile();

            System.out.println("startinggg");
            //TODO: if no instance found, will need to fail.

            if (performFileHealthCheck(startFile, HealthCheckConstants.HEALTH_CHECK_START).equals(Status.DOWN)) {
                startedTimer = new Timer(false);
                startedTimer.schedule(new FileUpdateProcess(startFile, HealthCheckConstants.HEALTH_CHECK_START, true), 0, 1000);
            }

            //TODO: Read Config. Substitute values.

            /*
             * Below are the perpertual tasks.
             */

            performFileHealthCheck(readyFile, HealthCheckConstants.HEALTH_CHECK_READY).equals(Status.DOWN);
            readyTimer = new Timer(false);
            readyTimer.schedule(new FileUpdateProcess(readyFile, HealthCheckConstants.HEALTH_CHECK_READY), 0, fileUpdateIntevalMilliseconds);

            performFileHealthCheck(liveFile, HealthCheckConstants.HEALTH_CHECK_LIVE).equals(Status.DOWN);
            liveTimer = new Timer(false);
            liveTimer.schedule(new FileUpdateProcess(liveFile, HealthCheckConstants.HEALTH_CHECK_LIVE), 0, fileUpdateIntevalMilliseconds);

        }

    }

    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "HealthCheckServiceImpl is deactivated");
    }

    /**
     * RF
     * Resolve MP Config properties at startup and set default status.
     */
    private void resolveDefaultStatues() {
        String mpConfig_defaultReadiness = ConfigProvider.getConfig().getOptionalValue(HealthCheckConstants.DEFAULT_OVERALL_READINESS_STATUS, String.class).orElse("");
        String mpConfig_defaultStartup = ConfigProvider.getConfig().getOptionalValue(HealthCheckConstants.DEFAULT_OVERALL_STARTUP_STATUS, String.class).orElse("");

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "In performHealthCheck(): The default overall Readiness status was configured to be overriden: mp.health.default.readiness.empty.response="
                         + mpConfig_defaultReadiness);
            Tr.debug(tc, "In performHealthCheck(): The default overall Startup status was configured to be overriden: mp.health.default.startup.empty.response="
                         + mpConfig_defaultStartup);
        }

        DEFAULT_READINESS_STATUS = mpConfig_defaultReadiness.equalsIgnoreCase("UP") ? Status.UP : Status.DOWN;
        DEFAULT_STARTUP_STATUS = mpConfig_defaultStartup.equalsIgnoreCase("UP") ? Status.UP : Status.DOWN;

    }

    /**
     * RF: Get the current set of visible apps
     */
    private Set<String> validateApplicationSet() throws NullPointerException {
        Set<String> apps = appTracker.getAllAppNames();
        Set<String> configApps = appTracker.getAllConfigAppNames();

        Iterator<String> configAppsIt = configApps.iterator();

        while (configAppsIt.hasNext()) {
            String nextAppName = configAppsIt.next();
            if (apps.contains(nextAppName)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "In performHealthCheck(): configAdmin found an application that the applicationStateListener already found. configAdminAppName = " + nextAppName);
            } else {
                if (tc.isDebugEnabled())
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

        HealthCheck30HttpResponseBuilder hcHttpResponseBuilder = new HealthCheck30HttpResponseBuilder();
        Set<String> appSet = validateApplicationSet();
        Set<String> unstartedAppSet = new HashSet<String>();

        runHealthChecks(appSet, healthCheckProcedure, unstartedAppSet, status -> hcHttpResponseBuilder.setOverallStatus(status),
                        x -> hcHttpResponseBuilder.handleUndeterminedResponse(httpResponse),
                        responses -> hcHttpResponseBuilder.addResponses(responses));

        unstartedShenanigans(unstartedAppSet, healthCheckProcedure);

        hcHttpResponseBuilder.setHttpResponse(httpResponse);
    }

    @Override
    public Status performFileHealthCheck(File file, String healthCheckProcedure) {

        //TODO catch shut down timing npes
        Set<String> appSet = validateApplicationSet();
        Set<String> unstartedAppSet = new HashSet<String>();

        //run health checks

        FileHealthCheckBuilder fhc = new FileHealthCheckBuilder(file);

        runHealthChecks(appSet, healthCheckProcedure, unstartedAppSet,
                        status -> fhc.setOverallStatus(status),
                        x -> fhc.handleUndeterminedResponse(),
                        responses -> fhc.addResponses(responses));

        fhc.updateFile();

        unstartedShenanigans(unstartedAppSet, healthCheckProcedure);

        return fhc.getOverallStatus();

    }

    public void unstartedShenanigans(Set<String> unstartedAppsSet, String healthCheckProcedure) {
        if (unstartedAppsSet.isEmpty()) {
            // If all applications are started, reset counter
            unstartedAppsCounter.set(0);
        } else if (!unstartedAppsSet.isEmpty() && unstartedAppsCounter.get() != unstartedAppsSet.size()) {
            // Update the new number of unstarted applications, since some applications may have already started.
            unstartedAppsCounter.set(unstartedAppsSet.size());
            if (tc.isDebugEnabled())
                Tr.debug(tc, "In performHealthCheck(): numOfUnstartedApps after unstarted app set was updated. = " + unstartedAppsCounter.get());

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
            if (tc.isDebugEnabled())
                Tr.debug(tc, "In performHealthCheck(): numOfUnstartedApps = " + unstartedAppsCounter.get());

            if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_START) && startupWarningAlreadyShown.compareAndSet(false, true)
                && !DEFAULT_STARTUP_STATUS.equals(Status.UP)) {
                Tr.warning(tc, "startup.healthcheck.applications.not.started.down.CWMMH0054W", new Object[] { unstartedAppsSet });
            } else if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_READY) && readinessWarningAlreadyShown.compareAndSet(false, true)
                       && !DEFAULT_READINESS_STATUS.equals(Status.UP)) {
                Tr.warning(tc, "readiness.healthcheck.applications.not.started.down.CWMMH0053W", new Object[] { unstartedAppsSet });
            }
        }
    }

    /*
     * RF: returns any unstarted Apps
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
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "In performHealthCheck(): Application : " + appName + " has not started yet.");
                if (!(healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE))) {
                    if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_START)) {

                        setOverallStatusFx.accept(DEFAULT_STARTUP_STATUS);
                        //hcHttpResponseBuilder.setOverallStatus(DEFAULT_STARTUP_STATUS);
                    } else if (healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_READY)) {
                        setOverallStatusFx.accept(DEFAULT_READINESS_STATUS);
                        //hcHttpResponseBuilder.setOverallStatus(DEFAULT_READINESS_STATUS);
                    } else {
                        // If the /health is hit, it should have the aggregated status of the individual health check procedures
//                        hcHttpResponseBuilder.setOverallStatus((DEFAULT_STARTUP_STATUS.equals(Status.UP)
//                                                                && DEFAULT_READINESS_STATUS.equals(Status.UP)) ? Status.UP : Status.DOWN);
                        setOverallStatusFx.accept((DEFAULT_STARTUP_STATUS.equals(Status.UP)
                                                   && DEFAULT_READINESS_STATUS.equals(Status.UP)) ? Status.UP : Status.DOWN);
                    }

                    // Keep track of the unstarted applications names
                    if (!unstartedAppsSet.contains(appName)) {
                        unstartedAppsSet.add(appName);
                    }
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "In performHealthCheck(): unstartedAppsSet after adding the unstarted app : " + unstartedAppsSet);
                } else {
                    // for liveness check
                    //hcHttpResponseBuilder.setOverallStatus(Status.UP);
                    setOverallStatusFx.accept(Status.UP);
                }
            } else {
                Set<String> modules = appTracker.getModuleNames(appName);
                if (modules != null) {
                    Iterator<String> moduleIt = modules.iterator();

                    while (moduleIt.hasNext()) {
                        String moduleName = moduleIt.next();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "In performHealthCheck(): appName = " + appName + ", moduleName = " + moduleName);

                        try {

                            hcResponses = hcExecutor.runHealthChecks(appName, moduleName, healthCheckProcedure);

                        } catch (HealthCheckBeanCallException e) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "In performHealthCheck(): Caught the exception " + e + " for appName = " + appName + ", moduleName = " + moduleName);
                            handleUndeterminedFx.accept(null);
                            //hcHttpResponseBuilder.handleUndeterminedResponse(httpResponse);
                            return;
                        }

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "In performHealthCheck(): hcResponses = " + hcResponses);

                        if (!hcResponses.isEmpty()) {
                            //hcHttpResponseBuilder.addResponses(hcResponses);
                            evaluatedStatusFx.accept(hcResponses);
                        }
                    }
                }
            }
        }

        //RF originally last w/ unstarted shenanigans
        if (anyAppsInstalled && !(healthCheckProcedure.equals(HealthCheckConstants.HEALTH_CHECK_LIVE))) {
            setOverallStatusFx.accept(Status.DOWN);
            //hcHttpResponseBuilder.setOverallStatus(Status.DOWN);
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

        public FileUpdateProcess(File file, String healthCheckProcedure, boolean isStopOnCreate) {
            //this.healthCheckService = inst;
            this.file = file;
            this.isStopOnCreate = isStopOnCreate;
        }

        @Override
        public void run() {

            //TODO delete sysout
            System.out.println("hi running for file " + file.getName());
            performFileHealthCheck(file, HealthCheckConstants.HEALTH_CHECK_START);
            if (isStopOnCreate && file.exists()) {
                System.out.println("Misson accomplished, canceleling for file " + file.getName());
                cancel();
            }

        }
    }
}
