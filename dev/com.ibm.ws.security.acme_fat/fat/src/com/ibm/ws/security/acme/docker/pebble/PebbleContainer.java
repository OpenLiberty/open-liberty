/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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

package com.ibm.ws.security.acme.docker.pebble;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map.Entry;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;

/**
 * Testcontainer implementation for the letsencrypt/pebble container.
 * 
 * <p/>
 * This testcontainer contains both the Pebble ACME compliant CA server as well
 * as the challtestsrv server servering as a mock DNS server. The container is
 * initialized to return all HTTP-01 challenges to the client IP address.
 */
public class PebbleContainer extends CAContainer {
	/**
	 * The mock DNS server port.
	 */
	public final int DNS_PORT = 8053;

	/**
	 * The REST management port.
	 */
	public final int CHALL_MANAGEMENT_PORT = 8055;

	private Network network = Network.newNetwork();

	public final GenericContainer<?> challtestsrv = new GenericContainer<>(DockerImageName.parse("letsencrypt/pebble-challtestsrv:v2.3.1"))
			.withCommand("pebble-challtestsrv").withExposedPorts(DNS_PORT, CHALL_MANAGEMENT_PORT).withNetwork(network)
			.withLogConsumer(o -> System.out.print("[CHL] " + o.getUtf8String()));

	/*
	 * Local JSON file that contains the Pebble configuration. The location of this file is dependent on whether
	 * you're running stand-alone through the 'runPebble' Gradle task or you are running Pebble via FVT.
	 */
	private static final File PEBBLE_CONFIG_JSON_FILE;
	
	static {
		/*
		 * Support running in FVT and running stand-alone.
		 */
		if (Files.exists(Paths.get("lib/LibertyFATTestFiles/pebble-config.json"))) {
			PEBBLE_CONFIG_JSON_FILE = new File("lib/LibertyFATTestFiles/pebble-config.json");
		} else {
			PEBBLE_CONFIG_JSON_FILE = new File("com.ibm.ws.security.acme_fat/publish/files/pebble-config.json");
		}
	}
	
	/**
	 * Log the output from this testcontainer.
	 * 
	 * @param frame
	 *            The frame containing log data.
	 */
	public static void log(OutputFrame frame) {
		String msg = frame.getUtf8String();
		if (msg.endsWith("\n"))
			msg = msg.substring(0, msg.length() - 1);
		Log.info(PebbleContainer.class, "pebble", msg);
	}

	/**
	 * Instantiate a new {@link PebbleContainer} instance.
	 * 
	 * @param dnsServer
	 *            Address of the DNS server to use to make DNS lookups for
	 *            domains.
	 */
	    //TODO switch to use ghcr.io/letsencrypt/pebble:2.6.0
	    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
	public PebbleContainer() {
		super(new ImageFromDockerfile()
				.withDockerfileFromBuilder(builder -> builder.from(
						ImageNameSubstitutor.instance().apply(DockerImageName.parse("letsencrypt/pebble:v2.3.1")).asCanonicalNameString())
						.copy("pebble-config.json", "/test/config/pebble-config.json").build())
				.withFileFromFile("pebble-config.json", PEBBLE_CONFIG_JSON_FILE), 5002, 14000, 15000);
		challtestsrv.withStartupAttempts(20);
		challtestsrv.withStartupTimeout(Duration.ofSeconds(60));

		for (int i = 1; i < NUM_RESTART_ATTEMPTS_ON_EXCEPTION + 1; i ++) {
			try {
				challtestsrv.start();
				Log.info(PebbleContainer.class, "PebbleContainer", "challtestsrv started");
				break;
			} catch (Throwable t) {
				Log.info(PebbleContainer.class, "PebbleContainer", "Failed to start challtestsrv, try again. " + t);
				challtestsrv.stop();
				Throwable cause = t.getCause();
				while (cause != null) {
					if (t instanceof DockerException) {
						Log.info(PebbleContainer.class, "PebbleContainer", "Hit a Docker exception, trying a long sleep and retry");
						try {
							Thread.sleep(120000);
						} catch (InterruptedException e) {
						}
						break;
					}
					cause = cause.getCause();
				}

				if (cause == null) { // do a short sleep and retry
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		if (!challtestsrv.isRunning()) {
			challtestsrv.start();
			Log.info(PebbleContainer.class, "PebbleContainer", "challtestsrv started");
			
			/**
			 * Intermittently getting a null calling getIntraContainerIP, determine what is null
			 */
			assertNotNull("challtestsrv.getContainerInfo()", challtestsrv.getContainerInfo());
			assertNotNull("challtestsrv.getContainerInfo().getNetworkSettings()", challtestsrv.getContainerInfo().getNetworkSettings());
			assertNotNull("challtestsrv.getContainerInfo().getNetworkSettings().getNetworks().entrySet()", challtestsrv.getContainerInfo().getNetworkSettings().getNetworks().entrySet());
		}

		String dnsServer = getIntraContainerIP() + ":" + DNS_PORT;

		this.withCommand("pebble", "-dnsserver", dnsServer, "-config", "/test/config/pebble-config.json", "-strict",
				"false");
		this.withExposedPorts(getDnsManagementPort(), getAcmeListenPort());
		this.withNetwork(network);
		this.withLogConsumer(PebbleContainer::log);
		this.withStartupAttempts(20);
		this.withStartupTimeout(Duration.ofSeconds(60));

		Testcontainers.exposeHostPorts(5002);

		for (int i = 1; i < NUM_RESTART_ATTEMPTS_ON_EXCEPTION + 1; i ++) {
			try {
				start();
				break;
			} catch (Throwable t) {
				Log.info(PebbleContainer.class, "PebbleContainer", "Failed to start pebble, try again. " + t);
				super.stop();
				Throwable cause = t.getCause();
				while (cause != null) {
					if (t instanceof DockerException) {
						Log.info(PebbleContainer.class, "PebbleContainer", "Hit a Docker exception, trying a long sleep and retry");
						try {
							Thread.sleep(120000);
						} catch (InterruptedException e) {
						}
						break;
					}
					cause = cause.getCause();
				}

				if (cause == null) { // do a short sleep and retry
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
		if (!isRunning()) {
			start();
		}

		try {
			/*
			 * Default responses to the client host.
			 */
			setDnsDefaultIpv4(getClientHost());

			/*
			 * Disable the IPv6 responses. The Pebble CA server responds on AAAA
			 * (IPv6) responses before A (IPv4) responses, and we don't
			 * currently have the testcontainer host's IPv6 address.
			 */
			setDnsDefaultIpv6("");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to set default mock DNS A and AAAA record IP addresses.", e);
		}

		Log.info(PebbleContainer.class, "PebbleContainer", "ContainerIpAddress: " + getHost());
		Log.info(PebbleContainer.class, "PebbleContainer", "DockerImageName:    " + getDockerImageName());
		assertNotNull("getContainerInfo()", getContainerInfo());
		Log.info(PebbleContainer.class, "PebbleContainer", "ContainerInfo:      " + getContainerInfo());
	}

	@Override
	public void stop() {
		challtestsrv.stop();
		super.stop();
		network.close();
	}

	@Override
	public String getAcmeDirectoryURI(boolean useAcmeURI) {
		if (useAcmeURI) {
			/*
			 * The "acme://pebble/<host>:<port>" will tell acme4j to load the
			 * PebbleAcmeProvider and PebbleHttpConnector, which will trust
			 * Pebble's static self-signed certificate.
			 */
			return "acme://pebble/" + this.getHost() + ":" + this.getMappedPort(getAcmeListenPort());
		} else {
			/*
			 * This will cause acme4j to use the GenericAcmeProvider.
			 */
			return "https://" + this.getHost() + ":" + this.getMappedPort(getAcmeListenPort()) + "/dir";
		}
	}

	@Override
	protected String getIntraContainerIP() {
		/*
		 * Get the IP address for the challtestsrv container as seen from the
		 * container network.
		 */
		String intraContainerIpAddress = null;
		for (Entry<String, ContainerNetwork> entry : challtestsrv.getContainerInfo().getNetworkSettings().getNetworks()
				.entrySet()) {
			intraContainerIpAddress = entry.getValue().getIpAddress();
			break;
		}
		if (intraContainerIpAddress == null) {
			fail("Didn't find IP address for challtestsrv server.");
		}

		return intraContainerIpAddress;
	}

	@Override
	protected String getDnsManagementAddress() {
		return "http://" + challtestsrv.getHost() + ":"
				+ challtestsrv.getMappedPort(CHALL_MANAGEMENT_PORT);
	}

	@Override
	public String getOcspResponderUrl() {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not provider support for an OCSP responder.");
	}


	@Override
	public void startDNSServer() {
		challtestsrv.start();
		Log.info(PebbleContainer.class, "startDNSServer", "DNS server started.");
	}

	@Override
	public void stopDNSServer() {
		Log.info(PebbleContainer.class, "stopDNSServer", "DNS server stopping.");

		challtestsrv.stop();

		long stoptime = System.currentTimeMillis() + 60000;
		while (challtestsrv.isRunning() && (stoptime > System.currentTimeMillis())) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
			}
		}

		assertFalse("The DNS server chould have stopped", challtestsrv.isRunning());
		Log.info(PebbleContainer.class, "stopDNSServer", "DNS server stopped.");
	}
}
