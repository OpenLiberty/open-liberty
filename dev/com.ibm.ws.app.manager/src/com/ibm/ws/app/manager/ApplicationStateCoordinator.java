/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.app.manager.internal.AppManagerConstants;
import com.ibm.ws.app.manager.internal.ApplicationConfigurator;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 *
 */
public final class ApplicationStateCoordinator {
    public enum AppStatus {
        FAILED, STOPPED, STARTED, REMOVED, DUP_APP_NAME
    };

    private static final class LatchAndAppSet {
        private final Set<String> appPids;
        private final CountDownLatch latch;

        LatchAndAppSet(Set<String> appPids) {
            this.appPids = appPids;
            this.latch = new CountDownLatch(appPids.size());
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public synchronized void removeAppPid(String appPid) {
            if (appPids.remove(appPid)) {
                latch.countDown();
            }
        }

        public String[] getSlowAppPids() {
            String[] slowApps = null;
            if (!appPids.isEmpty()) {
                slowApps = new String[appPids.size()];
                appPids.toArray(slowApps);
                appPids.clear();
            }
            return slowApps;
        }
    }

    private static final CountDownLatch waitForStartingAppPidsLatch = new CountDownLatch(1);
    private static volatile ApplicationConfigurator appConfigurator;
    private static volatile LatchAndAppSet unconfiguredApps;
    private static volatile LatchAndAppSet unstartedApps;
    private static volatile LatchAndAppSet unstoppedApps;
    private static long startTimeout;
    private static long stopTimeout;

    public static void setApplicationConfigurator(ApplicationConfigurator applicationConfigurator) {
        appConfigurator = applicationConfigurator;
        ConfigurationAdmin configAdmin = (appConfigurator != null) ? appConfigurator.getConfigAdminService() : null;
        if (configAdmin != null) {
            getConfiguredStartingAppPids(configAdmin);
            waitForStartingAppPidsLatch.countDown();
        }
    }

    public static void setApplicationStartTimeout(long applicationStarttimeout) {
        startTimeout = applicationStarttimeout;
    }

    public static long getApplicationStartTimeout() {
        return startTimeout;
    }

    public static void setApplicationStopTimeout(long applicationStoptimeout) {
        stopTimeout = applicationStoptimeout;
    }

    public static long getApplicationStopTimeout() {
        return stopTimeout;
    }

    private static void getConfiguredStartingAppPids(ConfigurationAdmin configAdmin) {
        Collection<String> appPids = new ArrayList<String>();
        try {
            Configuration[] configs = configAdmin.listConfigurations(AppManagerConstants.APPLICATION_FACTORY_FILTER);
            if (configs != null) {
                for (Configuration config : configs) {
                    appPids.add(config.getPid());
                }
            }
        } catch (InvalidSyntaxException ex) {
            // Auto FFDC
        } catch (IOException ex) {
            // Auto FFDC
        }
        unconfiguredApps = new LatchAndAppSet(new HashSet<String>(appPids));
        unstartedApps = new LatchAndAppSet(new HashSet<String>(appPids));
    }

    public static String[] getSlowlyStartingApps() {
        TimeUnit unit = TimeUnit.SECONDS;
        try {
            while (true) {
                if (waitForStartingAppPidsLatch.await(1, TimeUnit.SECONDS)) {
                    break;
                }
                if (FrameworkState.isStopping()) {
                    // we are stopping so bail out
                    return null;
                }
            }
        } catch (InterruptedException e) {
            //autoFFDC
        }
        if (appConfigurator != null) {
            long endTime = System.nanoTime() + unit.toNanos(getApplicationStartTimeout());
            try {
                do {
                    if (unconfiguredApps.await(1, TimeUnit.SECONDS)) {
                        break;
                    }
                } while (System.nanoTime() < endTime);

            } catch (InterruptedException e) {
                // Auto FFDC
            }
            appConfigurator.readyForAppsToStart();
            endTime = System.nanoTime() + unit.toNanos(getApplicationStartTimeout());
            try {
                do {
                    if (unstartedApps.await(1, TimeUnit.SECONDS)) {
                        break;
                    }
                } while (System.nanoTime() < endTime);

            } catch (InterruptedException e) {
                // Auto FFDC
            }
        }
        LatchAndAppSet unstarted = unstartedApps;
        unstartedApps = null;
        unconfiguredApps = null;
        return unstarted.getSlowAppPids();
    }

    public static void updateConfiguredAppStatus(String appPid) {
        LatchAndAppSet unconfigured = unconfiguredApps;
        if (unconfigured != null) {
            unconfigured.removeAppPid(appPid);
        }
    }

    public static void updateStartingAppStatus(String appPid, AppStatus appStatus) {
        LatchAndAppSet unstarted = unstartedApps;
        if (unstarted != null) {
            unstarted.removeAppPid(appPid);
        }
        appConfigurator.unblockAppStartDependencies(appPid);
    }

    public static String[] getSlowlyStoppingApps() {
        TimeUnit unit = TimeUnit.SECONDS;
        if (appConfigurator == null) {
            return null;
        }
        appConfigurator.readyForAppsToStop();
        long endTime = System.nanoTime() + unit.toNanos(getApplicationStopTimeout());
        try {
            do {
                if (unstoppedApps.await(1, TimeUnit.SECONDS)) {
                    break;
                }
            } while (System.nanoTime() < endTime);

        } catch (InterruptedException e) {
            // Auto FFDC
        }
        LatchAndAppSet unstopped = unstoppedApps;
        unstoppedApps = null;
        return unstopped.getSlowAppPids();
    }

    public static void setStoppingAppPids(Set<String> appPids) {
        unstoppedApps = new LatchAndAppSet(appPids);
    }

    public static void updateStoppingAppStatus(String appPid, AppStatus appStatus) {
        LatchAndAppSet unstopped = unstoppedApps;
        if (unstopped != null) {
            unstopped.removeAppPid(appPid);
        }
    }
}
