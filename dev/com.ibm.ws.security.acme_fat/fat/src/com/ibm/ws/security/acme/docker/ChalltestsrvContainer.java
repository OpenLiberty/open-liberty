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

import static junit.framework.Assert.fail;

import java.util.Map.Entry;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.ibm.websphere.simplicity.log.Log;

/**
 * Testcontainer implementation for the letsencrypt/pebble-challtestsrv
 * container.
 */
public class ChalltestsrvContainer extends CAContainer {

	/**
	 * The mock DNS server port.
	 */
	public static final int DNS_PORT = 8053;

	/**
	 * The REST management port.
	 */
	public static final int MANAGEMENT_PORT = 8055;

	/**
	 * Set a network, using Network.shared resulted in intermittent exceptions
	 */
	private final Network network;

	/**
	 * Log the output from this testcontainer.
	 * 
	 * @param frame
	 *            The frame containing log data.
	 */
	public static void log(OutputFrame frame) {
		String msg = frame.getUtf8String();
		if (msg.endsWith("\n")) {
			msg = msg.substring(0, msg.length() - 1);
		}
		Log.info(ChalltestsrvContainer.class, "pebble-challtestsrv", msg);
	}

	/**
	 * Instantiate a new {@link ChalltestsrvContainer} instance.
	 */
	public ChalltestsrvContainer() {
		super("letsencrypt/pebble-challtestsrv");

		network = Network.newNetwork();

		this.withCommand("pebble-challtestsrv");
		this.withExposedPorts(DNS_PORT, MANAGEMENT_PORT);
		this.withNetwork(network);
		this.withLogConsumer(ChalltestsrvContainer::log);
	}

	/**
	 * Get the IP address for this container as seen from the container network.
	 * 
	 * @return The IP address for this container on the container.
	 */
	public String getIntraContainerIP() {
		String intraContainerIpAddress = null;
		for (Entry<String, ContainerNetwork> entry : getContainerInfo().getNetworkSettings().getNetworks().entrySet()) {
			intraContainerIpAddress = entry.getValue().getIpAddress();
			break;
		}
		if (intraContainerIpAddress == null) {
			fail("Didn't find IP address for challtestsrv server.");
		}

		return intraContainerIpAddress;
	}


	@Override
	public void stop() {
		super.stop();
		network.close();
	}

	public Network getNetwork() {
		return network;
	}
}
