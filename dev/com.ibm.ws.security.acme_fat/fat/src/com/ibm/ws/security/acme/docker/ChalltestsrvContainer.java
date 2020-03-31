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

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

/**
 * Testcontainer implementation for the letsencrypt/pebble-challtestsrv
 * container.
 */
public class ChalltestsrvContainer extends GenericContainer<ChalltestsrvContainer> {

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
	 * Add an AAAA record to the mock DNS server. This will allow us to redirect
	 * requests to a named domain to the IPv6 address of our choice.
	 * 
	 * @param host
	 *            The host / domain to redirect requests for.
	 * @param address
	 *            The address to direct the requests for that host to.
	 * @throws IOException
	 */
	public void addAAAARecord(String host, String address) throws IOException {
		final String METHOD_NAME = "addAAAARecord";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			/*
			 * Generate the JSON request. The request can support multiple
			 * addresses but for the time being we will only support sending
			 * one.
			 */
			String jsonString = "{\"host\":\"" + host + "\",\"addresses\":[\"" + address + "\"]}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getManagementAddress() + "/add-aaaa");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(ChalltestsrvContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
	}

	/**
	 * Add an A record to the mock DNS server. This will allow us to redirect
	 * requests to a named domain to the IPv4 address of our choice.
	 * 
	 * @param host
	 *            The host / domain to redirect requests for.
	 * @param address
	 *            The address to direct the requests for that host to.
	 * @throws IOException
	 */
	public void addARecord(String host, String address) throws IOException {
		final String METHOD_NAME = "addARecord";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			/*
			 * Generate the JSON request. The request can support multiple
			 * addresses but for the time being we will only support sending
			 * one.
			 */
			String jsonString = "{\"host\":\"" + host + "\",\"addresses\":[\"" + address + "\"]}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getManagementAddress() + "/add-a");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(ChalltestsrvContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
	}

	/**
	 * Clear a previously added AAAA record that was added to the mock DNS
	 * server.
	 * 
	 * @param host
	 *            The host / domain to redirect requests for.
	 * @throws IOException
	 */
	public void clearAAAARecord(String host) throws IOException {
		final String METHOD_NAME = "clearAAAARecord(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"host\":\"" + host + "\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getManagementAddress() + "/clear-aaaa");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(ChalltestsrvContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
	}

	/**
	 * Clear a previously added A record that was added to the mock DNS server.
	 * 
	 * @param host
	 *            The host / domain to redirect requests for.
	 * @throws IOException
	 */
	public void clearARecord(String host) throws IOException {
		final String METHOD_NAME = "clearMockARecord(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"host\":\"" + host + "\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getManagementAddress() + "/clear-a");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(ChalltestsrvContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
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

	/**
	 * The HTTP address that can be used the reach the REST management API for
	 * the server.
	 * 
	 * @return The HTTP address to the REST management endpoint.
	 */
	public String getManagementAddress() {
		return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT);
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
