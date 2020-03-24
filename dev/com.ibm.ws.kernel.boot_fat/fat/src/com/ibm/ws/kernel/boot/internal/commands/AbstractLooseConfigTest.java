/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class AbstractLooseConfigTest {
    protected static final String SERVER_NAME = "com.ibm.ws.kernel.boot.loose.config.fat";
    protected static final String SERVER_ROOT = "MyRoot";
    // For Usr and server-root, there should be no /usr in the structure
    protected static final String SERVER_PATH = SERVER_ROOT + "/servers/" + SERVER_NAME;
    protected static final String ARCHIVE_PACKAGE = "MyPackage.zip";
    protected static final String PUBLISH_RESOURCES = "publish/resources/";
    protected static final String CONFIG_SOURCE = PUBLISH_RESOURCES + "configs/";
    protected static final String TMP_SOURCE = PUBLISH_RESOURCES + "tmp/";
    protected static final String APPS_DIR = "apps";
    protected static final String DROPINS_DIR = "dropins";
    private static final String[] CONFIGS = new String[] {
                                                           "DefaultArchive.war.xml",
                                                           "SimpleElements.war.xml",
                                                           "ArchivedElements.war.xml",
                                                           "SkipInvalidEntries.war.xml",
                                                           "EarArchive.ear.xml",
    };

    protected static Collection<Object[]> getConfigsAsParameters() {
        Object[][] params = new Object[CONFIGS.length][1];
        for (int i = 0; i < CONFIGS.length; i++) {
            params[i][0] = CONFIGS[i];
        }
        return Arrays.asList(params);
    }

    /**
     * Add the given value to the value found in checkMatch at the given key if one
     * exists, otherwise make a new entry
     *
     * @param checkMatch
     * @param key
     * @param value
     */
    protected void putMatch(HashMap<String, Integer> checkMatch, String key, int value) {
        int match = checkMatch.get(key) == null ? 0 : checkMatch.get(key);
        checkMatch.put(key, match + value);
    }

    public void packageWithConfig(LibertyServer server, String[] cmd) throws Exception {
        packageWithConfig(server, "DefaultArchive.war.xml", cmd);
    }

    public void packageWithConfig(LibertyServer server, String config) throws Exception {
        String[] cmd = new String[] { "--archive=" + ARCHIVE_PACKAGE, "--include=usr", "--server-root=" + SERVER_ROOT };
        packageWithConfig(server, config, cmd);
    }

    /**
     * Copy the loose config file into the given server, with the given loose config file,
     * then run the package command with the given args.
     *
     * @param server
     * @param config
     * @param cmd
     * @throws Exception
     */
    public void packageWithConfig(LibertyServer server, String config, String[] cmd) throws Exception {
        System.out.printf("%2s-config: %s%n", "", config);

        server.getFileFromLibertyInstallRoot("lib/extract");

        // Find the config in PUBLISH_RESOURCES and move it to APPS_DIR in the server
        server.copyFileToLibertyServerRoot(CONFIG_SOURCE, getAppsTargetDir(), config);

        // Package the server and ensure it completes
        String stdout = server.executeServerScript("package", cmd).getStdout();
        assertTrue("The package command did not complete as expected. STDOUT = " + stdout,
                   stdout.contains("package complete"));
    }

    /**
     * @return
     */
    public String getAppsTargetDir() {
        return APPS_DIR;
    }
}
