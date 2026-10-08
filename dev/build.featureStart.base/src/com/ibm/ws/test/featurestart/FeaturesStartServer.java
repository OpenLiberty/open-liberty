/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.simplicity.OperatingSystem;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class FeaturesStartServer {
    public static void logInfo(String m, String msg) {
        FeaturesStartTestBase.logInfo(m, msg);
    }
    
    //

    public FeaturesStartServer(String serverName) throws Exception {
        this.serverName = serverName;

        this.server = LibertyServerFactory.getLibertyServer(serverName);
        this.serverConfigPath = server.getServerConfigurationPath();
        // Disabling unexpected FFDC checking for now because there is no good way for this FAT
        // to know what feature artifacts it should wait for before the server is stopped.
        this.server.setFFDCChecking(false);

        this.serverIsZOS = isZOS(server);
        this.serverJavaLevel = getJavaLevel(server);
    }
    
    // Server APIs ...

    public String serverName;

    public String serverGetName() {
        return serverName;
    }

    public LibertyServer server;

    protected String getInstallRoot() {
        return server.getInstallRoot();
    }
    
    protected static boolean isZOS(LibertyServer useServer) throws Exception {
        return useServer.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
    }

    public static int getJavaLevel(LibertyServer useServer) throws Exception {
        return JavaInfo.forServer(useServer).majorVersion();
    }

    public boolean serverIsZOS;
    public int serverJavaLevel;

    public boolean isZOS() {
        return serverIsZOS;
    }

    public int getJavaLevel() {
        return serverJavaLevel;
    }
    
    /** Relative path from the server home to the server features directory. */
    public static final String SERVER_FEATURES_PATH = "/lib/features/";

    public File getFeaturesDir() {
        return new File( server.getInstallRoot() + SERVER_FEATURES_PATH );
    }

    public String serverConfigPath;

    public String getConfigPath() {
        return serverConfigPath;
    }

    protected void changeFeatures(List<String> featureShortNames) throws Exception {
        server.changeFeatures(featureShortNames);
    }
    
    // Control parameters values for LibertyServer.startServerAndValidate:
    
    private static final boolean PRE_CLEAN = true;
    private static final boolean CLEAN_START = true;
    private static final boolean VALIDATE_APPS = true;
    private static final boolean EXPECT_FAILURE = true;
    private static final boolean VALIDATE_TIMED_EXIT = true;
    
    protected void serverStart(String featureShortName) throws Exception {
        // Default start: Pre-clean and clean the server.
        server.setConsoleLogName(featureShortName + ".log");

        server.startServerAndValidate(
                PRE_CLEAN, CLEAN_START,
                !VALIDATE_APPS, !EXPECT_FAILURE, !VALIDATE_TIMED_EXIT);
    }
    
    protected boolean serverIsStarted() {
        return ( server.isStarted() );
    }
    
    /**
     * Stop the server. Ignore the specified failure messages. Any other failure message
     * results in a thrown exception.
     * 
     * Failure messages include startup failure messages.
     *
     * Do not transfer logs yet! These must remain in the server folders until they
     * are examined.
     * 
     * @param ignoredFailuresRegExps Regular expressions specifying failure messages which
     *     are to be ignored.
     * @throws Exception Thrown if the server stop fails with any error messages other than
     *     the specified ignored messages.
     */
    protected void stop(String... ignoredFailuresRegExps) throws Exception {
        server.stopServer(!LibertyServer.POST_ARCHIVES, ignoredFailuresRegExps);
    }
    
    protected void postLogs() throws Exception {
        server.postStopServerArchive();
    }
    
    /**
     * Attempt to kill the process which has the specified PID.
     * 
     * Do nothing if the process is not running.
     * 
     * The PID must not be null, and should be a numeric value.
     *
     * @throws Exception Thrown if the attempt to kill the process failed.
     */
    protected void killProcess(String pid) throws Exception {
        if ( pid == null ) {
            throw new IllegalArgumentException("Null PID");
        }

        int pidValue;
        try {
            pidValue = Integer.parseInt(pid);
        } catch ( NumberFormatException e ) {
            throw new IllegalArgumentException("Non-numeric PID [ " + pid + " ]", e);
        }

        server.getMachine().killProcess(pidValue);
    }

    /**
     * Answer the PID of the server.
     *
     * @return The PID of the running server.
     *
     * @throws Exception Thrown if the server PID cannot be retrieved.
     */
    protected String getPid() throws Exception {
        return server.getPid();
    }

    protected List<String> findMessages(String regex) throws Exception {
        return server.findStringsInLogs(regex);
    }

    // Performance notes: The original, unfixed 'updateServerConfiguration' took
    // just over 10s on Windows to perform an update.  That is because the
    // implementation attempted to rename the newly written configuration onto the
    // server configuration.  That failed, but was performed with full retries.
    //
    // With a fix to 'updateServerConfiguration' the update time is reduced to just
    // over 1s.  Much better, but not as good as possible.
    //
    // With either direct rewrite implementation ('changeFeatures' or 'FileRewriter')
    // the update time plummets to about 0.03s.

    // A direct rewrite API is already present in LibertyServer!  Since the server is
    // stopped for this update, a direct rewrite is usable.

    // This implementation mixes the read and write steps too tightly to
    // collect separate timings.
    //
    // Alternate, rewrite implementation.  Used before 'changeFeatures' was discovered.
    //
    // String matchLine;
    // Set<Integer> additions;
    // if (lastShortName == null) {
    //     matchLine = "<featureManager>";
    //     additions = Collections.singleton(Integer.valueOf(0));
    // } else {
    //     matchLine = "<feature>" + lastShortName + "</feature>";
    //     additions = Collections.emptySet();
    // }
    // String featureLine = "<feature>" + shortName + "</feature>";
    // String[] matchLines = new String[] { matchLine };
    // String[] updateLines = new String[] { featureLine };
    // FileRewriter.update(serverConfigPath, matchLines, updateLines, additions);
    //
    // The original implementation, which uses XML serialization.
    //
    // ServerConfiguration config = server.getServerConfiguration();
    // Set<String> features = config.getFeatureManager().getFeatures();
    // features.clear();
    // features.add(shortName);
    // server.updateServerConfiguration(config);

    /**
     * Set the server configuration to have exactly the one specified feature.
     *
     * Initially, the server configuration has an empty feature manager element.
     * The first update adds a feature into that element. Subsequent updates
     * replace the feature.
     *
     * @param lastShortName The previously configured feature.
     * @param nextShortName The feature to set in the server configuration.
     *
     * @throws Exception Thrown if the update failed.
     */
    public void updateFeature(String lastShortName, String nextShortName) throws Exception {
        String m = "updateFeature";
        logInfo(m, "Configuring server [ " + serverName + " ] for feature [ " + nextShortName + " ]");
        if ( lastShortName != null ) {
            logInfo(m, "Prior feature [ " + lastShortName + " ]");
        }
        changeFeatures(Collections.singletonList(nextShortName));
        logInfo(m, "Configured server [ " + serverName + " ] for feature [ " + nextShortName + " ]");        
    }
    
}
