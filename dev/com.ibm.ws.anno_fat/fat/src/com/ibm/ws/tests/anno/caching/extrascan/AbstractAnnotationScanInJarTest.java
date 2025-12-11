/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.tests.anno.caching.extrascan;

import static org.junit.Assert.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.EnterpriseApplication;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import junit.framework.Assert;
import spring.test.init.jar.JarInit;
import spring.test.init.manifest.ManifestInit;
import spring.test.init.sharedlib.SharedLibInit;
import spring.test.init.war.WebInit;

@RunWith(FATRunner.class)
public abstract class AbstractAnnotationScanInJarTest extends FATServletClient {

	public static final String APP_NAME = "springTest";
	public static final String SERVER_NAME = "springTest_server";

	@Server(SERVER_NAME)
	public static LibertyServer server;

	@ClassRule
	public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, true,
			EERepeatActions.EE10,
			EERepeatActions.EE11);

	//Since there is only one test we can do the setup as part of the test to allow the use of abstraction
	public void setUp() throws Exception {

		WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
				.addPackages(true, WebInit.class.getPackage())
				.addAsManifestResource(new StringAsset("Manifest-Version: 1.0" + System.lineSeparator() +
						"Class-Path: manifestLib.jar" + System.lineSeparator()), "MANIFEST.MF") //The Class-Path will not be included without that trailing newline. See https://docs.oracle.com/javase/tutorial/deployment/jar/modman.html
				.addAsWebInfResource(new StringAsset("logging.level.org.springframework.context.annotation=DEBUG"), "application.properties");

		String userDir = System.getProperty("user.dir"); //ends with: com.ibm.ws.anno_fat/build/libs/autoFVT
		String springDir = userDir + "/publish/shared/resources/spring/";

		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar")
				.addPackages(true, JarInit.class.getPackage());

		JavaArchive sharedLib = ShrinkWrap.create(JavaArchive.class, "sharedLib.jar")
				.addPackages(true, SharedLibInit.class.getPackage());

		JavaArchive manifestJar = ShrinkWrap.create(JavaArchive.class, "manifestLib.jar")
				.addPackage(ManifestInit.class.getPackage());

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
				.addAsModule(war)
				.addAsModule(manifestJar)
				.addAsLibraries(jar);

		//Add the spring libs 
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(springDir), "*.jar")) {
			for (Path path : stream) {
				if (!Files.isDirectory(path)) {
					//Putting the spring libs in the ear file works. Putting them as a shared library reference means
					//none of the initalizers will fire.

					//However I think this worked fine if I had shared libs enabled. Possibly because I was also scanning
					//Shared libs as web fragments. Leaving this note here so I can start investigating from where I left of
					//if we ever enable scanning for shared libs

					/*
					 * The spring library itself needs to:
					 * 1. Go somewhere where we are scanning for extra annotations
					 * 2. Be visible when processing the other archive with a spring initalizer.
					 * 
					 * An ear lib is perfect for visibility but when we're not scanning ear libs in manifestClassPath mode
					 * putting it as a war lib satisfies visibility for a classpath lib.  
					 */

					if (getConfigMode().equals("manifestClassPath")) {
						war.addAsLibraries(path.toFile());
					} else {
						ear.addAsLibraries(path.toFile());
					}
				}
			}
		}

		ShrinkHelper.exportToServer(server, "libs", sharedLib);
		ShrinkHelper.exportAppToServer(server, ear, DeployOptions.SERVER_ONLY);

		//Configure the server
		ServerConfiguration config = server.getServerConfiguration();
		ConfigElementList<EnterpriseApplication> entApps = config.getEnterpriseApplications();

		//quick check
		Assert.assertEquals(1, entApps.size());

		EnterpriseApplication entApp = entApps.get(0);
		entApp.setAnnotationScanLibrary(getConfigMode());
		server.updateServerConfiguration(config);

		server.startServer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.stopServer();
	}


	@Test
	public void testSpringAnnotationFoundInWar() throws Exception {

		System.out.println("Testing extra annotation scans in mode " + getConfigMode());
		setUp();

		List<String> matching = server.findStringsInLogsAndTraceUsingMark("AnnotationScanInJarTest test output");

		String allOutput = String.join(" : ", matching);

		for (String s : getMessagesToSearchFor()) {
			assertTrue("While testing mode: " + getConfigMode() + " Did not find \"" + s + "\" in " + allOutput, allOutput.contains(s));
		}

		//Since it checks both logs and traces it will find each twice.
		int expectedSize = getMessagesToSearchFor().size() * 2;
		assertTrue("While testing mode: \" + getConfigMode() + \" Found too many entries in the logs. Expected " + expectedSize + " Found " + matching.size() + " output: " + allOutput, matching.size() == expectedSize);
	}

	public abstract List<String> getMessagesToSearchFor();

	public abstract String getConfigMode();

}
