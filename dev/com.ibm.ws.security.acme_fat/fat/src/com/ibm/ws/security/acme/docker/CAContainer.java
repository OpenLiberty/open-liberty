package com.ibm.ws.security.acme.docker;

import static junit.framework.Assert.fail;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map.Entry;
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

import com.github.dockerjava.api.model.ContainerNetwork;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

public class CAContainer extends GenericContainer<BoulderContainer> {


	public static final int HTTP_PORT = 5002;

	/** The REST management API port. */
	public int MANAGEMENT_PORT = 8055;
	
	public CAContainer(String image) {
		super(image);
	}

	public CAContainer(ImageFromDockerfile image, int managementPort) {
		super(image);
		MANAGEMENT_PORT = managementPort;
	}


	/**
	 * Get Pebble's intermediate certificate.
	 * 
	 * @param fileName
	 *            The name of the file to save the certificate to.
	 * @return Pebble's root CA certificate in the form of a PEM file.
	 * @throws Exception
	 *             If we failed to receive the certificate.
	 */
	public byte[] getAcmeCaIntermediateCertificate() throws Exception {
		final String METHOD_NAME = "getAcmeCaIntermediateCertificate()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT)
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
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				byte[] result = EntityUtils.toByteArray(response.getEntity());

				Log.info(PebbleContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}

	/**
	 * Get Pebble's root certificate.
	 * 
	 * @param fileName
	 *            The name of the file to save the certificate to.
	 * @return Pebble's root CA certificate in the form of a PEM file.
	 * @throws Exception
	 *             If we failed to receive the certificate.
	 */
	public byte[] getAcmeCaRootCertificate() throws Exception {
		final String METHOD_NAME = "getAcmeCaRootCertificate()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT) + "/roots/0";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
			/*
			 * Create a GET request to the ACME CA server.
			 */
			HttpGet httpGet = new HttpGet(url);

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(BoulderContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				byte[] result = EntityUtils.toByteArray(response.getEntity());

				Log.info(BoulderContainer.class, METHOD_NAME, new String(result));
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
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT)
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
				AcmeFatUtils.logHttpResponse(BoulderContainer.class, METHOD_NAME, httpGet, response);

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

				Log.info(BoulderContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}
	
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
	public String getManagementAddress() {
		return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT);
	}

	public String getAcmeDirectoryURI() {
		return null;
	}

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
}
