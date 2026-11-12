/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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

package com.ibm.ws.security.context_fat;

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import componenttest.topology.impl.LibertyServer;

@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class TestConfiguration {
	
	private final LibertyServer server;
	private final Set<String> appNames;
	
	public TestConfiguration(LibertyServer server,  Set<String> appNames) {
        this.server = server;
        this.appNames = appNames;
    }
	
	   /**
     * Starts the server if it was not previously started.
     * 
     * @param forceRestart Forces restart if server started.
     * 
     * @throws Exception
     */
    public void startServerIfNotStarted(boolean forceRestart) throws Exception {
    	boolean startServerCalled = false;
    	
        if (!server.isStarted()) {
        	server.startServer(true, true);
        	startServerCalled = true;
        } else {
            if (forceRestart) {
            	server.restartServer();
            	startServerCalled = true;
            }
        }

        if (startServerCalled) {
            assertNotNull("Message CWWKT0016I indicating web app available was not found in message log.", server.waitForStringInLog("CWWKT0016I.*"));
            assertNotNull("There should be a CWWKS4105I string in the trace log indicating that LTPA configuration is ready.", server.waitForStringInLog("CWWKS4105I:", 300000));
        }
    }
    
    /**
     * Updates the server configuration (server.xml).
     * 
     * @param configFile The path/name to the config file to replace the existing one.
     * 
     * @throws Exception
     */
    public void updateServerConfig(String newConfigFile, boolean restartServer) throws Exception {
        // If this is truly a config file change, there is a bit more to do.
        if (server.isStarted()) {
            if (restartServer) {
            	server.stopServer("CWWKS0800E", "CWWKG0033W");
            } else {
            	server.setMarkToEndOfLog();
            }
        }

        server.setServerConfigurationFile(newConfigFile);

        if (server.isStarted()) {
        	server.waitForConfigUpdateInLogUsingMark(appNames);
        } else {
            startServerIfNotStarted(false);
        }
    }
}
