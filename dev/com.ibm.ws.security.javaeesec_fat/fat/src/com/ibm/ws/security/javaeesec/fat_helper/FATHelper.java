/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.javaeesec.fat_helper;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;

/**
 * Utility class that provides common FAT helper methods.
 */
public class FATHelper {
    /**
     * Reload select applications.
     *
     * @throws Exception If there was an error reloading the applications for some unforeseen reason.
     */
    public static void reloadApplications(LibertyServer server, final Set<String> appIdsToReload) throws Exception {
        ServerConfiguration config = server.getServerConfiguration().clone();

        /*
         * Get the apps to remove.
         */
        ConfigElementList<Application> toRemove = new ConfigElementList<Application>(config.getApplications().stream().filter(app -> appIdsToReload.contains(app.getId())).collect(Collectors.toList()));
        List<String> appNames = toRemove.stream().map(FATHelper::getAppName).collect(Collectors.toList());

        /*
         * Remove the applications.
         */
        config.getApplications().removeAll(toRemove);
        updateConfigDynamically(server, config, appNames, null);

        /*
         * Reload applications.
         */
        config.getApplications().addAll(toRemove);
        updateConfigDynamically(server, config, null, appNames);
    }

    /**
     * This method will the reset the log and trace marks for log and trace searches, update the
     * configuration and then wait for the server to re-initialize. Optionally it will then wait for the application to start.
     *
     * @param server       The server to update.
     * @param config       The configuration to use.
     * @param appsStopping The names of apps which are expected to stop after the config update
     * @param appsStarting The names of apps which are expected to start after the config update
     * @throws Exception If there was an issue updating the server configuration.
     */
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config, List<String> appsStopping, List<String> appsStarting) throws Exception {
        resetMarksInLogs(server);
        server.updateServerConfiguration(config);
        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
        if (appsStopping != null) {
            for (String name : appsStopping) {
                assertNotNull(name + " application did not stop", server.waitForStringInLogUsingMark("CWWKZ0009I:.*" + name));
            }
        }
        if (appsStarting != null) {
            for (String name : appsStarting) {
                assertNotNull(name + " application did not start", server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + name));
            }
        }
    }

    /**
     * Reset the marks in all Liberty logs.
     *
     * @param server The server for the logs to reset the marks.
     * @throws Exception If there was an error resetting the marks.
     */
    public static void resetMarksInLogs(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());
    }

    /**
     * Get the name of an application based on the server configuration
     *
     * @param application
     * @return
     */
    private static String getAppName(Application application) {
        if (application.getName() != null) {
            return application.getName();
        } else if (application.getLocation() != null) {
            String name = application.getLocation();
            if (name.lastIndexOf("/") != -1) {
                name = name.substring(name.lastIndexOf("/"));
            }
            if (name.toLowerCase().endsWith(".ear") || name.toLowerCase().endsWith(".war")) {
                name = name.substring(0, name.length() - 4);
            }
            return name;
        }
        throw new RuntimeException("Couldn't work out the app name for config element " + application);
    }
}
