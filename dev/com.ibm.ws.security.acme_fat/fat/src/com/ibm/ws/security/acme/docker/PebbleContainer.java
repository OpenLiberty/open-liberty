/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.docker;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

/**
 * Testcontainer implementation for the letsencrypt/pebble container.
 */
public class PebbleContainer extends CAContainer {

	/** The port used to listen for incoming ACME requests. */
	public static final int LISTEN_PORT = 14000;

	/** The REST management API port. */
	public static final int MANAGEMENT_PORT = 15000;

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
	public PebbleContainer(String dnsServer, Network network) {
		super(new ImageFromDockerfile()
				.withDockerfileFromBuilder(builder -> builder.from("letsencrypt/pebble")
						.copy("pebble-config.json", "/test/config/pebble-config.json").build())
				.withFileFromFile("pebble-config.json", new File("lib/LibertyFATTestFiles/pebble-config.json")), MANAGEMENT_PORT);

		this.withCommand("pebble", "-dnsserver", dnsServer, "-config", "/test/config/pebble-config.json", "-strict",
				"false");
		this.withExposedPorts(MANAGEMENT_PORT, LISTEN_PORT);
		this.withNetwork(network);
		this.withLogConsumer(PebbleContainer::log);
	}

	

	/**


	 * Get the URI to the ACME CA's directory.
	 * 
	 * @param usePebbleURI
	 *            Use the "acme://pebble" style URI instead of the generic
	 *            "https:" URI.
	 * @return The URI to the ACME CA's directory.
	 */
	public String getAcmeDirectoryURI(boolean usePebbleURI) {

		if (usePebbleURI) {
			/*
			 * The "acme://pebble/<host>:<port>" will tell acme4j to load the
			 * PebbleAcmeProvider and PebbleHttpConnector, which will trust
			 * Pebble's static self-signed certificate.
			 */
			return "acme://pebble/" + this.getContainerIpAddress() + ":" + this.getMappedPort(LISTEN_PORT);
		} else {
			/*
			 * This will cause acme4j to use the GenericAcmeProvider.
			 */
			return "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(LISTEN_PORT) + "/dir";
		}
	}
}
