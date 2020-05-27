/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.docker;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

/**
 * Abstract testcontainer that contains methods to operate against both an ACME
 * CA server as well as a mock DNS server to be used to validate (and is limited
 * to) ACME HTTP-01 functionality.
 */
public abstract class CAContainer extends GenericContainer<CAContainer> {

	/**
	 * The port that is used to answer the HTTP-01 challenges
	 */
	private final int httpPort;

	/**
	 * The port on the ACME CA server used to listen for incoming ACME requests.
	 */
	private final int acmeListenPort;

	/**
	 * The port on the mock DNS server where the REST management API is
	 * reachable.
	 */
	private final int dnsManagementPort;

	/**
	 * Instantiate a new {@link CAContainer} instance.
	 * 
	 * @param image
	 *            The docker image name to build this {@link CAContainer}
	 *            instance from.
	 * @param httpPort
	 *            The HTTP port, typically on the application server, that will
	 *            respond to HTTP-01 challenges from the ACME CA server.
	 * @param acmeListenPort
	 *            The port on the ACME CA server used to listen to incoming ACME
	 *            requests.
	 * @param managementPort
	 *            The port on the mock DNS server where the REST management API
	 *            is reachable.
	 */
	protected CAContainer(String image, int httpPort, int acmeListenPort, int managementPort) {
		super(image);
		this.httpPort = httpPort;
		this.acmeListenPort = acmeListenPort;
		this.dnsManagementPort = managementPort;
	}

	/**
	 * Instantiate a new {@link CAContainer} instance.
	 * 
	 * @param image
	 *            The docker file image to build this {@link CAContainer}
	 *            instance from.
	 * @param httpPort
	 *            The HTTP port, typically on the application server, that will
	 *            respond to HTTP-01 challenges from the ACME CA server.
	 * @param acmeListenPort
	 *            The port on the ACME CA server used to listen to incoming ACME
	 *            requests.
	 * @param managementPort
	 *            The port on the mock DNS server where the REST management API
	 *            is reachable.
	 */
	protected CAContainer(ImageFromDockerfile image, int httpPort, int acmeListenPort, int dnsManagementPort) {
		super(image);
		this.httpPort = httpPort;
		this.acmeListenPort = acmeListenPort;
		this.dnsManagementPort = dnsManagementPort;
	}

	/**
	 * Get CA's intermediate certificate.
	 * 
	 * @return the CA's intermediate certificate as a byte array representation
	 *         of a PEM
	 * @throws Exception
	 *             If we failed to receive the certificate.
	 */
	public byte[] getAcmeCaIntermediateCertificate() throws Exception {
		final String METHOD_NAME = "getAcmeCaIntermediateCertificate()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(dnsManagementPort)
				+ "/intermediates/0";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
			/*
			 * Create a GET request to the ACME CA server.
			 */
			HttpGet httpGet = new HttpGet(url);

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				byte[] result = EntityUtils.toByteArray(response.getEntity());

				Log.info(CAContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}

	/**
	 * Get the root certificate.
	 * 
	 * @return the CA's root certificate as a byte array representation of a PEM
	 * @throws Exception
	 *             If we failed to receive the certificate.
	 */
	public byte[] getAcmeCaRootCertificate() throws Exception {
		final String METHOD_NAME = "getAcmeCaRootCertificate()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(dnsManagementPort)
				+ "/roots/0";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
			/*
			 * Create a GET request to the ACME CA server.
			 */
			HttpGet httpGet = new HttpGet(url);

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				byte[] result = EntityUtils.toByteArray(response.getEntity());

				Log.info(CAContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}

	/**
	 * Get the status of the certificate from the ACME CA server.
	 * 
	 * @param certificate
	 *            The certificate to check.
	 * @return The status of the certificate.
	 * @throws Exception
	 */
	public String getAcmeCertificateStatus(X509Certificate certificate) throws Exception {
		final String METHOD_NAME = "getAcmeCertificateStatus()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(dnsManagementPort)
				+ "/cert-status-by-serial/" + certificate.getSerialNumber().toString(16);

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
			/*
			 * Create a GET request to the ACME CA server.
			 */
			HttpGet httpGet = new HttpGet(url);

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				String result = EntityUtils.toString(response.getEntity());

				/*
				 * The result is in JSON, lets just parse out the status.
				 */
				Pattern p = Pattern.compile(".*\"Status\": \"(\\w+)\",.*", Pattern.DOTALL);
				Matcher m = p.matcher(result);
				if (m.find()) {
					result = m.group(1);
				} else {
					throw new Exception(
							"Certificate status response was not in expected JSON format. Response: " + result);
				}

				Log.info(CAContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}

	/**
	 * Retrieves the client host's IP address that is reachable from the
	 * container.
	 * 
	 * @return The client host's IP address that is reachable from the
	 *         container.
	 * @throws IllegalStateException
	 *             If the address was not found.
	 */
	public String getClientHost() throws IllegalStateException {
		for (String extraHost : this.getExtraHosts()) {
			if (extraHost.startsWith("host.testcontainers.internal:")) {
				return extraHost.replace("host.testcontainers.internal:", "");
			}
		}

		throw new IllegalStateException(
				"Unable to resolve local host from docker container. Could not find 'host.testcontainers.internal' property.");
	}

	/**
	 * Convenience method for adding A records to the mock DNS server, where the
	 * domain will point to the current client. This is equivalent to calling
	 * {@link #addDnsARecord(String, String)} with <code>domain</code> and
	 * {@link #getClientHost()}.
	 * 
	 * @param domain
	 *            The host / domain to redirect requests for.
	 */
	public void addDnsARecord(String domain) throws IOException {
		addDnsARecord(domain, getClientHost());
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
	public void addDnsARecord(String host, String address) throws IOException {
		final String METHOD_NAME = "addDnsARecord";

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
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/add-a");
			httpPost.setEntity(requestEntity);
			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
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
	public void addDnsAAAARecord(String host, String address) throws IOException {
		final String METHOD_NAME = "addDnsAAAARecord";

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
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/add-aaaa");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

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
	public void clearDnsAAAARecord(String host) throws IOException {
		final String METHOD_NAME = "clearDnsAAAARecord(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"host\":\"" + host + "\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/clear-aaaa");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

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
	public void clearDnsARecord(String host) throws IOException {
		final String METHOD_NAME = "clearDnsARecord(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"host\":\"" + host + "\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/clear-a");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
	}

	/**
	 * Clear the DNS request history for the specified host.
	 * 
	 * @param host
	 *            the host to clear DNS history for.
	 * @throws IOException
	 *             if there was an error communicating with the mock DNS server.
	 */
	public void clearDnsRequestHistory(String host) throws IOException {
		final String METHOD_NAME = "clearDnsRequestHistory(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"host\":\"" + host + "\",\"type\":\"dns\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/clear-request-history");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
	}

	/**
	 * Get the DNS request history for the specified host.
	 * 
	 * @param host
	 *            the host to get the DNS history for
	 * @return A JSON string suitable for debug.
	 * @throws IOException
	 *             if there was an error communicating with the mock DNS server.
	 */
	public String getDnsRequestHistory(String host) throws IOException {
		final String METHOD_NAME = "getDnsRequestHistory(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"host\":\"" + host + "\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/http-request-history");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				String result = EntityUtils.toString(response.getEntity());
				Log.info(CAContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}

	/**
	 * Return the port that the ACME CA server will send HTTP-01 challenges to.
	 * 
	 * @return the port to send HTTP-01 challenges
	 */
	public int getHttpPort() {
		return httpPort;
	}

	/**
	 * Return the mock DNS REST management API port.
	 * 
	 * @return the port for the mock DNS REST management API.
	 */
	protected int getDnsManagementPort() {
		return dnsManagementPort;
	}

	/**
	 * Return the port used by the ACME CA server to listen for incoming ACME
	 * requests.
	 * 
	 * @return
	 */
	protected int getAcmeListenPort() {
		return acmeListenPort;
	}

	/**
	 * The HTTP address that can be used the reach the REST management API for
	 * the server.
	 * 
	 * @return The HTTP address to the REST management endpoint.
	 */
	protected abstract String getDnsManagementAddress();

	/**
	 * Get the URI to the ACME CA's directory.
	 * 
	 * @param useAcmeURI
	 *            Use the "acme://pebble" style URI instead of the generic
	 *            "https:" URI. This param is ignored for Boulder.
	 * @return The URI to the ACME CA's directory.
	 */
	public abstract String getAcmeDirectoryURI(boolean useAcmeURI);

	/**
	 * Get the IP address for the container as seen from the container network.
	 * 
	 * @return The IP address for the container on the container network.
	 */
	protected abstract String getIntraContainerIP();

	/**
	 * Set the default IPv4 address that the mock DNS server will use to respond
	 * to A record queries. This address will be used when an existing record
	 * for the domain does not exist.
	 * 
	 * <p/>
	 * On startup, the mock DNS server is configured to use
	 * {@link #getClientHost()} as the default IPv4 address.
	 * 
	 * @param ip
	 *            The IP to set as default. Empty string ("") if you wish to
	 *            have no default.
	 * @throws IOException
	 *             if there was an error communicating with the mock DNS server.
	 * @see #addDnsARecord(String)
	 * @see #addDnsARecord(String, String)
	 * @see #clearDnsARecord(String)
	 */
	public void setDnsDefaultIpv4(String ip) throws IOException {
		final String METHOD_NAME = "setDnsDefaultIpv4(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"ip\":\"" + ip + "\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/set-default-ipv4");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
	}

	/**
	 * Set the default IPv6 address that the mock DNS server will use to respond
	 * to AAAA record queries. This address will be used when an existing record
	 * for the domain does not exist.
	 * 
	 * <p/>
	 * On startup, the mock DNS server is configured to have no default IPv6
	 * address.
	 * 
	 * @param ip
	 *            The IP to set as default. Empty string ("") if you wish to
	 *            have no default.
	 * @throws IOException
	 *             if there was an error communicating with the mock DNS server.
	 * @see #addDnsAAAARecord(String, String)
	 * @see #clearDnsAAAARecord(String)
	 */
	public void setDnsDefaultIpv6(String ip) throws IOException {
		final String METHOD_NAME = "setDnsDefaultIpv6(String)";

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			/*
			 * Generate the JSON request.
			 */
			String jsonString = "{\"ip\":\"" + ip + "\"}";
			StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

			/*
			 * Create a POST request to the mock DNS server.
			 */
			HttpPost httpPost = new HttpPost(getDnsManagementAddress() + "/set-default-ipv6");
			httpPost.setEntity(requestEntity);

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}
			}
		}
	}

	/**
	 * Get the OCSP responder URL for this {@link CAContainer}.
	 * 
	 * @return the OCSP responder URL.
	 * @throws UnsupportedOperationException
	 *             if the container does not support an OCSP responder.
	 */
	public abstract String getOcspResponderUrl();
	
	/**
	 * Start the DNS server for this {@link CAContainer}.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the container does not support starting the a DNS server
	 */
	public abstract void startDNSServer();

	/**
	 * Stop the DNS server for this {@link CAContainer}.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the container does not support stopping the a DNS server
	 */
	public abstract void stopDNSServer();
}
