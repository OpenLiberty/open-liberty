/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme;

import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.Testcontainers;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.ChalltestsrvContainer;
import com.ibm.ws.security.acme.docker.PebbleContainer;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({ AcmeClientTest.class })
public class FATSuite {

	public static ChalltestsrvContainer challtestsrv = null;

	public static PebbleContainer pebble = null;

	/*
	 * This static block should be the first static initialization in this class
	 * so that the testcontainers config is cleared before we start our new
	 * testcontainers.
	 */
	static {
		ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
	}

	@AfterClass
	public static void afterClass() {
		if (pebble != null) {
			pebble.stop();
		}
		if (challtestsrv != null) {
			challtestsrv.stop();
		}
	}

	/**
	 * We need to start up the containers in an orderly fashion so that we can
	 * pass the IP address of the DNS server to the Pebble server.
	 * 
	 * @throws UnknownHostException
	 */
	@BeforeClass
	public static void beforeClass() throws UnknownHostException {
		final String METHOD_NAME = "beforeClass()";
		
		String os = System.getProperty("os.name").toLowerCase();
		Assume.assumeTrue(!os.startsWith("z/os"));

		/*
		 * Need to expose the HTTP port that is used to answer the HTTP-01
		 * challenge.
		 */
		Testcontainers.exposeHostPorts(PebbleContainer.HTTP_PORT);

		/*
		 * Startup the challtestsrv container first. This container will serve
		 * as a mock DNS server to the Pebble server that starts on the other
		 * container.
		 */
		challtestsrv = new ChalltestsrvContainer();
		challtestsrv.start();

		Log.info(FATSuite.class, METHOD_NAME,
				"Challtestserv ContainerIpAddress: " + challtestsrv.getContainerIpAddress());
		Log.info(FATSuite.class, METHOD_NAME, "Challtestserv DockerImageName:    " + challtestsrv.getDockerImageName());
		Log.info(FATSuite.class, METHOD_NAME, "Challtestserv ContainerInfo:      " + challtestsrv.getContainerInfo());

		/*
		 * Startup the pebble server.
		 */
		pebble = new PebbleContainer(challtestsrv.getIntraContainerIP() + ":" + ChalltestsrvContainer.DNS_PORT);
		pebble.start();

		Log.info(FATSuite.class, METHOD_NAME, "Pebble ContainerIpAddress: " + pebble.getContainerIpAddress());
		Log.info(FATSuite.class, METHOD_NAME, "Pebble DockerImageName:    " + pebble.getDockerImageName());
		Log.info(FATSuite.class, METHOD_NAME, "Pebble ContainerInfo:      " + pebble.getContainerInfo());
	}
}
