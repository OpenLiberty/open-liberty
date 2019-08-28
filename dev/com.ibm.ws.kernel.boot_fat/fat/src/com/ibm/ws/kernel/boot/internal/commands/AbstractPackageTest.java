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

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class AbstractPackageTest {
	protected static final String SERVER_NAME = "com.ibm.ws.kernel.boot.loose.config.fat";
	protected static final String SERVER_ROOT = "MyRoot";
	// For Usr and server-root, there should be no /usr in the structure
	protected static final String SERVER_PATH = SERVER_ROOT + "/servers/" + SERVER_NAME;
	protected static final String ARCHIVE_PACKAGE = "MyPackage.zip";
	protected static final String PUBLISH_RESOURCES = "publish/resources/configs";
	protected static final String APPS_DIR = "apps";
	protected static final String DEFAULT_CONFIG = "AppsLooseWeb.war.xml";

	/**
	 * Represents a test designed to be run on several different loose configurations
	 */
	public interface LooseConfigTest {

		/**
		 * Test a feature of the server made with the given loose config
		 * 
		 * @param server
		 * @param config
		 * @throws Exception
		 */
		public void testConfig(LibertyServer server, String config) throws Exception;
	}

	/**
	 * Each config in configs is used to make and package a LibertyServer,
	 * that is then tested by the LooseConfigTest
	 * 
	 * @param test
	 * @param server
	 * @param configs
	 * @throws Exception
	 */
	public static void packageAndTestConfigs(LooseConfigTest test, LibertyServer server, String[] configs)
			throws Exception {

		server.getFileFromLibertyInstallRoot("lib/extract");

		for (String config : configs) {
			// Find the config in PUBLISH_RESOURCES and move it to APPS_DIR in the server
			server.copyFileToLibertyServerRoot(PUBLISH_RESOURCES, APPS_DIR, config);

			String[] cmd = new String[] { "--archive=" + ARCHIVE_PACKAGE, "--include=usr",
					"--server-root=" + SERVER_ROOT };
			// Ensure package completes
			String stdout = server.executeServerScript("package", cmd).getStdout();
			assertTrue("The package command did not complete as expected. STDOUT = " + stdout,
					stdout.contains("package complete"));
			
			try {
				test.testConfig(server, config);
			} catch (Exception ex) {
			}

			// Delete the generated zip and the loose config file
			server.deleteFileFromLibertyServerRoot(ARCHIVE_PACKAGE);
			server.deleteFileFromLibertyServerRoot(APPS_DIR + "/" + config);
		}
	}
}
