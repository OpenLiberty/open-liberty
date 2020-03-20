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

package com.ibm.ws.security.acme.utils;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContextBuilder;

import com.ibm.websphere.simplicity.config.AcmeCA;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator;
import com.ibm.ws.security.acme.docker.PebbleContainer;
import com.ibm.ws.security.acme.fat.FATSuite;

import componenttest.topology.impl.LibertyServer;

/**
 * A collection of utility method for the ACME FAT component.
 */
public class AcmeFatUtils {

	public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
	public static final String SELF_SIGNED_KEYSTORE_PASSWORD = "acmepassword";
	public static final String PEBBLE_TRUSTSTORE_PASSWORD = "acmepassword";
	public static final String DEFAULT_KEYSTORE_PASSWORD = "acmepassword";

	/**
	 * Get an X.509 certificate from a PEM certificate.
	 * 
	 * @param pemBytes
	 *            The bytes that comprise the PEM certificate.
	 * @return The X.509 certificate.
	 * @throws CertificateException
	 *             If the certificate could not be generated from the passed in
	 *             PEM bytes.
	 */
	public static X509Certificate getX509Certificate(byte pemBytes[]) throws CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(pemBytes));
	}

	/**
	 * Get an X.509 certificate from a PEM certificate.
	 * 
	 * @param pemBytes
	 *            The bytes that comprise the PEM certificate.
	 * @return The X.509 certificate.
	 * @throws CertificateException
	 *             If the certificate could not be generated from the passed in
	 *             PEM bytes.
	 */
	public static X509Certificate getX509Certificate(InputStream in) throws CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		return (X509Certificate) cf.generateCertificate(in);
	}

	/**
	 * Get a (very) insecure HTTPs client that accepts all certificates and does
	 * no host name verification.
	 * 
	 * @return The insecure HTTPS client.
	 * @throws Exception
	 *             If the client couldn't be created for some unforeseen reason.
	 */
	public static CloseableHttpClient getInsecureHttpsClient() throws Exception {

		HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
			if (context != null) {
				ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection) context
						.getAttribute(HttpCoreContext.HTTP_CONNECTION);
				try {
					SSLSession sslSession = routedConnection.getSSLSession();
					if (sslSession != null) {

						/*
						 * Get the server certificates from the SSLSession.
						 */
						Certificate[] certificates = sslSession.getPeerCertificates();

						/*
						 * Add the certificates to the context, where we can
						 * later grab it from
						 */
						if (certificates != null) {
							context.setAttribute(PEER_CERTIFICATES, certificates);
						}
					}
				} catch (ConnectionShutdownException e) {
					/*
					 * This might occur when the request doesn't return a
					 * payload.
					 */
					Log.warning(AcmeFatUtils.class,
							"Unable to save the connection's TLS certificates to the HTTP context since the connection was closed.");
				}
			}
		};

		SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy()).build();
		SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext,
				new NoopHostnameVerifier());
		return HttpClients.custom().setSSLSocketFactory(connectionFactory).addInterceptorLast(certificateInterceptor)
				.build();
	}

	/**
	 * Log HTTP responses in a standard form.
	 * 
	 * @param clazz
	 *            The class that is asking to log the response.
	 * @param methodName
	 *            The method name that is asking to log the response.
	 * @param request
	 *            The request that was made.
	 * @param response
	 *            The response that was received.
	 */
	public static void logHttpResponse(Class<?> clazz, String methodName, HttpRequestBase request,
			CloseableHttpResponse response) {
		StatusLine statusLine = response.getStatusLine();
		Log.info(clazz, methodName, request.getMethod() + " " + request.getURI() + " ---> " + statusLine.getStatusCode()
				+ " " + statusLine.getReasonPhrase());
	}

	/**
	 * This method will the reset the log and trace marks for log and trace
	 * searches, update the configuration and then wait for the server to
	 * re-initialize.
	 *
	 * @param server
	 *            The server to update.
	 * @param config
	 *            The configuration to use.
	 * @throws Exception
	 *             If there was an issue updating the server configuration.
	 */
	public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config) throws Exception {
		updateConfigDynamically(server, config, false);
	}

	/**
	 * This method will the reset the log and trace marks for log and trace
	 * searches, update the configuration and then wait for the server to
	 * re-initialize. Optionally it will then wait for the application to start.
	 *
	 * @param server
	 *            The server to update.
	 * @param config
	 *            The configuration to use.
	 * @param waitForAppToStart
	 *            Wait for the application to start.
	 * @throws Exception
	 *             If there was an issue updating the server configuration.
	 */
	public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config,
			boolean waitForAppToStart) throws Exception {
		resetMarksInLogs(server);
		server.updateServerConfiguration(config);
		server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
		if (waitForAppToStart) {
			server.waitForStringInLogUsingMark("CWWKZ0003I");
		}
	}

	/**
	 * Reset the marks in all Liberty logs.
	 *
	 * @param server
	 *            The server for the logs to reset the marks.
	 * @throws Exception
	 *             If there was an error resetting the marks.
	 */
	public static void resetMarksInLogs(LibertyServer server) throws Exception {
		if (server.defaultLogFileExists()) {
			server.setMarkToEndOfLog(server.getDefaultLogFile());
		}
		if (server.defaultTraceFileExists()) {
			server.setMarkToEndOfLog(server.getMostRecentTraceFile());
		}
	}

	/**
	 * Convenience method for dynamically configuring the acmeCA-2.0
	 * configuration.
	 * 
	 * @param server
	 *            Liberty server to update.
	 * @param originalConfig
	 *            The original configuration to update from.
	 * @param domains
	 *            Domains to request the certificate for.
	 * @throws Exception
	 *             IF there was an error updating the configuration.
	 */
	public static void configureAcmeCA(LibertyServer server, ServerConfiguration originalConfig, String... domains)
			throws Exception {
		configureAcmeCA(server, originalConfig, false, domains);
	}

	/**
	 * Convenience method for dynamically configuring the acmeCA-2.0
	 * configuration.
	 * 
	 * @param server
	 *            Liberty server to update.
	 * @param originalConfig
	 *            The original configuration to update from.
	 * @param userAcmeURIs
	 *            Use "acme://" style URIs for ACME providers.
	 * @param domains
	 *            Domains to request the certificate for.
	 * @throws Exception
	 *             IF there was an error updating the configuration.
	 */
	public static void configureAcmeCA(LibertyServer server, ServerConfiguration originalConfig, boolean useAcmeURIs,
			String... domains) throws Exception {
		/*
		 * Choose which configuration to update.
		 */
		ServerConfiguration clone = ((originalConfig != null) ? originalConfig : server.getServerConfiguration())
				.clone();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeCA acmeCA = clone.getAcmeCA();
		acmeCA.setDirectoryURI(FATSuite.pebble.getAcmeDirectoryURI(useAcmeURIs));
		acmeCA.setDomain(Arrays.asList(domains));
		acmeCA.setAcceptTermsOfService(true);

		if (!useAcmeURIs) {
			acmeCA.getAcmeTransportConfig()
					.setTrustStore("${server.config.dir}/resources/security/pebble-truststore.p12");
			acmeCA.getAcmeTransportConfig().setTrustStorePassword(PEBBLE_TRUSTSTORE_PASSWORD);
		}

		/*
		 * The defaultHttpEndpoint needs to point to the port the CA has been
		 * configured to send HTTP-01 challenges to.
		 */
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setId("defaultHttpEndpoint");
		endpoint.setHttpPort(String.valueOf(PebbleContainer.HTTP_PORT));
		endpoint.setHttpsPort("${bvt.prop.HTTP_default.secure}");
		endpoint.setHost("*");
		clone.getHttpEndpoints().add(endpoint);

		/*
		 * Apply the configuration.
		 */
		AcmeFatUtils.updateConfigDynamically(server, clone);
	}

	/**
	 * Configure the mock DNS server to point the specified domain at the client
	 * driving the tests.
	 * 
	 * @param domains
	 *            The domain to direct.
	 * @throws Exception
	 *             If there was an error communicating with the mock DNS server.
	 */
	public static void configureDnsForDomains(String... domains) throws Exception {

		Log.info(AcmeFatUtils.class, "configureDnsForDomains(String...)",
				"Configuring DNS with the following domains: " + domains);

		for (String domain : domains) {
			/*
			 * Disable the IPv6 responses for this domain. The Pebble CA server
			 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
			 * we don't currently have the testcontainer host's IPv6 address.
			 */
			FATSuite.challtestsrv.addARecord(domain, FATSuite.pebble.getClientHost());
			FATSuite.challtestsrv.addAAAARecord(domain, "");
		}
	}

	/**
	 * Clear A and AAAA records on the mock DNS server.
	 * 
	 * @param domains
	 *            The domain to clear.
	 * @throws Exception
	 *             If there was an error communicating with the mock DNS server.
	 */
	public static void clearDnsForDomains(String... domains) throws Exception {

		Log.info(AcmeFatUtils.class, "clearDnsForDomains(String...)",
				"Clearning the following domains from the DNS: " + domains);

		for (String domain : domains) {
			/*
			 * Disable the IPv6 responses for this domain. The Pebble CA server
			 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
			 * we don't currently have the testcontainer host's IPv6 address.
			 */
			FATSuite.challtestsrv.clearARecord(domain);
			FATSuite.challtestsrv.clearAAAARecord(domain);
		}
	}

	/**
	 * Wait for Liberty to report that a new keystore has been generated.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForAcmeToCreateCertificate(LibertyServer server) {
		assertNotNull("ACME did not create a new certificate.",
				server.waitForStringInLog("CWPKI0803A: SSL certificate created"));
	}

	/**
	 * Wait for the ACME service to report that the certificate has been
	 * replaced.
	 * 
	 * @param server
	 */
	public static final void waitForAcmeToReplaceCertificate(LibertyServer server) {
		assertNotNull("ACME did not update replace the certificate.",
				server.waitForStringInLog("CWPKI2007I"));
	}

	/**
	 * Wait for the ACME service's to NOT update a keystore since it is still
	 * valid.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForAcmeToNoOp(LibertyServer server) {
		assertNotNull("ACME update did not no-op.",
				server.waitForStringInTrace("Previous certificate requested from ACME CA server is still valid"));
	}

	/**
	 * Wait for the ACME auhtorization web application to start.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForAcmeAppToStart(LibertyServer server) {
		assertNotNull("ACME authorization web application did not start.", server
				.waitForStringInTrace("ACME authorization web application has started and is available for requests"));
	}

	/**
	 * Wait for the defaultHttpEndpoint-ssl to start.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForSslEndpoint(LibertyServer server) {
		assertNotNull("Expected defaultHttpEndpoint-ssl to start.",
				server.waitForStringInTrace("CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started"));
	}

	/**
	 * Wait for the acmeCA-2.0 feature to be uninstalled.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitAcmeFeatureUninstall(LibertyServer server) {
		assertNotNull("Expected ACME authorization web application to be removed.",
				server.waitForStringInTrace("CWWKT0017I: Web application removed.*/.well-known/acme-challenge/"));
		assertNotNull("Expected acmeCA-2.0 feature to be uninstalled.",
				server.waitForStringInTrace("CWWKF0013I: The server removed the following features.*acmeCA-2.0.*"));
		assertNotNull("Expected SSL to initialize.", server.waitForStringInTrace("< initializeSSL Exit"));
	}

	/**
	 * Generate a self-signed certificate.
	 * 
	 * @param server
	 *            The liberty server to install the key.
	 * @return The certificate.
	 * @throws Exception
	 *             If there was an issue generating the self-signed certificate.
	 */
	public static Certificate[] generateSelfSignedCertificate(LibertyServer server) throws Exception {

		/*
		 * Generate any parent directories first and delete the file if it
		 * exists.
		 */
		String filePath = server.getServerRoot() + "/resources/security/key.p12";
		File certFile = new File(filePath);
		if (certFile.getParentFile() != null) {
			certFile.getParentFile().mkdirs();
		}
		if (certFile.exists()) {
			certFile.delete();
		}

		/*
		 * Generate the new keystore with the self-signed certificate.
		 */
		KeytoolSSLCertificateCreator creator = new KeytoolSSLCertificateCreator();
		certFile = creator.createDefaultSSLCertificate(filePath, SELF_SIGNED_KEYSTORE_PASSWORD,
				DefaultSSLCertificateCreator.DEFAULT_VALIDITY, "cn=localhost",
				DefaultSSLCertificateCreator.DEFAULT_SIZE, DefaultSSLCertificateCreator.SIGALG, null);

		/*
		 * Load the keystore and return the certificate.
		 */
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		keystore.load(new FileInputStream(certFile), SELF_SIGNED_KEYSTORE_PASSWORD.toCharArray());
		return keystore.getCertificateChain(DefaultSSLCertificateCreator.ALIAS);
	}

	/**
	 * Assert that the server is using a certificate signed by the ACME CA.
	 * 
	 * @param server
	 *            the liberty server to communicate with.
	 * @return the server's current TLS certificate chain.
	 * @throws Exception
	 *             If the server is not using a certificate signed by the ACME
	 *             CA.
	 */
	public static final Certificate[] assertAndGetServerCertificate(LibertyServer server) throws Exception {
		final String methodName = "assertServerCertificate()";

		/*
		 * Get the CA's intermediate certificate.
		 */
		X509Certificate caCertificate = AcmeFatUtils
				.getX509Certificate(FATSuite.pebble.getAcmeCaIntermediateCertificate());

		/*
		 * Make a request to the root context just to grab the certificate.
		 */
		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a GET request to the Liberty server.
			 */
			HttpGet httpGet = new HttpGet("https://localhost:" + server.getHttpDefaultSecurePort());
			HttpContext context = new BasicHttpContext();

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet, context)) {
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpGet, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					fail(methodName + ": Expected response 200, but received response: " + statusLine);
				}

				/*
				 * Verify that the certificate is signed by the CA.
				 */
				Certificate[] certificates = (Certificate[]) context.getAttribute(AcmeFatUtils.PEER_CERTIFICATES);
				Log.info(AcmeFatUtils.class, methodName, "Certificates: " + Arrays.toString(certificates));
				assertNotNull(
						"Expected there to be TLS certificates in the HttpContext. Did the connection abort before we could retrieve them?",
						certificates);
				certificates[0].verify(caCertificate.getPublicKey());

				return certificates;
			}
		}
	}

	/**
	 * Delete any files generated by the ACME feature. This method is currently
	 * restricted to deleting from the default location.
	 * 
	 * <p/>
	 * Currently this is restricted to the default file locations. This could be
	 * modified to look at the configuration.
	 * 
	 * @param server
	 *            the liberty server to delete the ACME CA files from.
	 */
	public static void deleteAcmeFiles(LibertyServer server) {
		Log.info(AcmeFatUtils.class, "deleteAcmeFiles(LibertyServer)", "Deleting files generated by ACME feature.");

		File keystore = new File(server.getServerRoot() + "/resources/security/key.p12");
		if (keystore.exists()) {
			keystore.delete();
		}

		File accountKey = new File(server.getServerRoot() + "/resources/security/acmeAccountKey.pem");
		if (accountKey.exists()) {
			accountKey.delete();
		}

		File domainKey = new File(server.getServerRoot() + "/resources/security/acmeDomainKey.pem");
		if (domainKey.exists()) {
			domainKey.delete();
		}
	}

	public static void checkPortOpen(int port, long timeoutMs) {

		boolean open = false;
		long stoptime = System.currentTimeMillis() + timeoutMs;

		while (!open && (stoptime > System.currentTimeMillis())) {
			ServerSocket socket = null;
			try {
				socket = new ServerSocket(); // Create unbounded socket
				socket.setReuseAddress(true); // This allows the socket to close
												// and others to bind to it even
												// if its in TIME_WAIT state
				socket.bind(new InetSocketAddress(port));
				open = true;
			} catch (Exception e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					// Not a lot to do
				}
			} finally {
				if (null != socket) {
					try {
						// With setReuseAddress set to true we should free up
						// our socket and allow
						// someone else to bind to it even if we are in
						// TIME_WAIT state.
						socket.close();
					} catch (IOException ioe) {
						// not a lot to do
					}
				}
			}
		}

		assertTrue("Expected port " + port + " to be open.", open);
	}
}
