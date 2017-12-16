/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.security.javaeesec.fat_helper;

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

        /*
         * Remove the applications.
         */
        config.getApplications().removeAll(toRemove);
        updateConfigDynamically(server, config, true);

        /*
         * Reload applications.
         */
        config.getApplications().addAll(toRemove);
        updateConfigDynamically(server, config, true);
    }

    /**
     * This method will the reset the log and trace marks for log and trace searches, update the
     * configuration and then wait for the server to re-initialize. Optionally it will then wait for the application to start.
     *
     * @param server The server to update.
     * @param config The configuration to use.
     * @param waitForAppToStart Wait for the application to start.
     * @throws Exception If there was an issue updating the server configuration.
     */
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config, boolean waitForAppToStart) throws Exception {
        resetMarksInLogs(server);
        server.updateServerConfiguration(config);
        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
        if (waitForAppToStart) {
            server.waitForStringInLogUsingMark("CWWKZ0003I"); //CWWKZ0003I: The application **** updated in 0.020 seconds.
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
}
