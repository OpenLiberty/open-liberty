/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.utils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.config.AcmeCA;
import com.ibm.websphere.simplicity.config.AcmeCA.AcmeTransportConfig;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.fat.AcmeRevocationTest;
import com.ibm.ws.security.acme.fat.AcmeValidityAndRenewTest;
import com.ibm.ws.security.acme.internal.web.AcmeCaRestHandler;

import componenttest.topology.impl.LibertyServer;

/**
 * A collection of utility method for the ACME FAT component.
 */
public class AcmeFatUtils {

	public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
	public static final String SELF_SIGNED_KEYSTORE_PASSWORD = "acmepassword";
	public static final String CACERTS_TRUSTSTORE_PASSWORD = "acmepassword";
	public static final String DEFAULT_KEYSTORE_PASSWORD = "acmepassword";
	private static final long SCHEDULE_TIME = 30000;
	public static final String ACME_CHECKER_TRACE = "ACME automatic certificate checker verified";

	public static final String ADMIN_USER = "administrator";
	public static final String ADMIN_PASS = "adminpass";

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
	public static void configureAcmeCA(LibertyServer server, CAContainer caContainer,
			ServerConfiguration originalConfig, String... domains) throws Exception {
		configureAcmeCA(server, caContainer, originalConfig, false, domains);
	}

	/**
	 * Convenience method for dynamically configuring the acmeCA-2.0
	 * configuration.
	 * 
	 * @param server
	 *            Liberty server to update.
	 * @param originalConfig
	 *            The original configuration to update from.
	 * @param useAcmeURIs
	 *            Use "acme://" style URIs for ACME providers.
	 * @param domains
	 *            Domains to request the certificate for.
	 * @throws Exception
	 *             IF there was an error updating the configuration.
	 */
	public static void configureAcmeCA(LibertyServer server, CAContainer caContainer,
			ServerConfiguration originalConfig, boolean useAcmeURIs, String... domains) throws Exception {
		configureAcmeCA(server, caContainer, originalConfig, useAcmeURIs, false, domains);

	}

	/**
	 * Convenience method for dynamically configuring the acmeCA-2.0
	 * configuration.
	 * 
	 * @param server
	 *            Liberty server to update.
	 * @param originalConfig
	 *            The original configuration to update from.
	 * @param useAcmeURIs
	 *            Use "acme://" style URIs for ACME providers.
	 * @param disableRenewWindow
	 *            Set the disableMinRenewWindow in the server config.
	 * @param domains
	 *            Domains to request the certificate for.
	 * @throws Exception
	 *             IF there was an error updating the configuration.
	 */
	public static void configureAcmeCA(LibertyServer server, CAContainer caContainer,
			ServerConfiguration originalConfig, boolean useAcmeURIs, boolean disableRenewWindow, String... domains) throws Exception {
		configureAcmeCA(server, caContainer, originalConfig, useAcmeURIs, disableRenewWindow, true, domains);
	}
	
	/**
	 * Convenience method for dynamically configuring the acmeCA-2.0
	 * configuration.
	 * 
	 * @param server
	 *            Liberty server to update.
	 * @param originalConfig
	 *            The original configuration to update from.
	 * @param useAcmeURIs
	 *            Use "acme://" style URIs for ACME providers.
	 * @param disableRenewWindow
	 *            Set the disableMinRenewWindow in the server config.
	 * @param disableRenewOnNewHistory
	 *            Set the disableRenewOnNewHistory in the server config.
	 * @param domains
	 *            Domains to request the certificate for.
	 * @throws Exception
	 *             IF there was an error updating the configuration.
	 */
	public static void configureAcmeCA(LibertyServer server, CAContainer caContainer,
			ServerConfiguration originalConfig, boolean useAcmeURIs, boolean disableRenewWindow, boolean disableRenewOnNewHistory, String... domains) throws Exception {
		/*
		 * Choose which configuration to update.
		 */
		ServerConfiguration clone = ((originalConfig != null) ? originalConfig : server.getServerConfiguration())
				.clone();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeCA acmeCA = clone.getAcmeCA();
		acmeCA.setDomain(Arrays.asList(domains));
		ArrayList<String> accounts = new ArrayList<String>();
		accounts.add("mailto:pacman@mail.com");
		acmeCA.setAccountContact(accounts);
		configureAcmeCaConnection(caContainer.getAcmeDirectoryURI(useAcmeURIs), acmeCA);
		if (disableRenewWindow) {
			/*
			 * Allow back to back renew requests for test efficiency
			 */
			acmeCA.setDisableMinRenewWindow(true);
		}
		
		if (disableRenewOnNewHistory) {
			/*
			 * Allow back to back renew requests for test efficiency
			 */
			acmeCA.setDisableRenewOnNewHistory(true);
		}

		/*
		 * The defaultHttpEndpoint needs to point to the port the CA has been
		 * configured to send HTTP-01 challenges to.
		 */
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setId("defaultHttpEndpoint");
		endpoint.setHttpPort(String.valueOf(caContainer.getHttpPort()));
		endpoint.setHttpsPort("${bvt.prop.HTTP_default.secure}");
		endpoint.setHost("*");
		clone.getHttpEndpoints().add(endpoint);

		/*
		 * Apply the configuration.
		 */
		AcmeFatUtils.updateConfigDynamically(server, clone);
	}

	/**
	 * Configure the connection related information for the ACME CA server.
	 * 
	 * @param directoryUri
	 *            Use "acme://" style URIs for ACME providers.
	 * @param acmeCA
	 *            The {@link AcmeCA} instance to update.
	 */
	public static void configureAcmeCaConnection(String directoryUri, AcmeCA acmeCA) {
		acmeCA.setDirectoryURI(directoryUri);
		if (!directoryUri.startsWith("acme")) {
			AcmeTransportConfig acmeTransportConfig = new AcmeTransportConfig();
			acmeTransportConfig.setTrustStore("${server.config.dir}/resources/security/cacerts.p12");
			acmeTransportConfig.setTrustStorePassword(CACERTS_TRUSTSTORE_PASSWORD);
			acmeCA.setAcmeTransportConfig(acmeTransportConfig);
		}
	}

	/**
	 * Convenience method for dynamically configuring the acmeCA-2.0
	 * configuration. This method will NOT modify any ACME CA config.
	 * 
	 * @param server
	 *            Liberty server to update.
	 * @param originalConfig
	 *            The original configuration to update from.
	 * @throws Exception
	 *             IF there was an error updating the configuration.
	 */
	public static void configureAcmeCA(LibertyServer server, CAContainer caContainer,
			ServerConfiguration originalConfig) throws Exception {
		/*
		 * Choose which configuration to update.
		 */
		ServerConfiguration clone = ((originalConfig != null) ? originalConfig : server.getServerConfiguration())
				.clone();

		/*
		 * The defaultHttpEndpoint needs to point to the port the CA has been
		 * configured to send HTTP-01 challenges to.
		 */
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setId("defaultHttpEndpoint");
		endpoint.setHttpPort(String.valueOf(caContainer.getHttpPort()));
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
	public static void configureDnsForDomains(CAContainer container, String... domains) throws Exception {

		Log.info(AcmeFatUtils.class, "configureDnsForDomains(String...)",
				"Configuring DNS with the following domains: " + domains);

		for (String domain : domains) {
			/*
			 * Disable the IPv6 responses for this domain. The Pebble CA server
			 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
			 * we don't currently have the testcontainer host's IPv6 address.
			 */
			container.addDnsARecord(domain, container.getClientHost());
			container.addDnsAAAARecord(domain, "");
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
	public static void clearDnsForDomains(CAContainer dnscontainer, String... domains) throws Exception {

		Log.info(AcmeFatUtils.class, "clearDnsForDomains(String...)",
				"Clearing the following domains from the DNS: " + Arrays.toString(domains));

		for (String domain : domains) {
			/*
			 * Disable the IPv6 responses for this domain. The Pebble CA server
			 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
			 * we don't currently have the testcontainer host's IPv6 address.
			 */
			dnscontainer.clearDnsARecord(domain);
			dnscontainer.clearDnsAAAARecord(domain);
		}
	}

	/**
	 * Wait for Liberty to report that a new keystore has been generated.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForSslToCreateKeystore(LibertyServer server) {
		/*
		 * Temporary extra debug for RTC bug 277292
		 */
		if (server.waitForStringInLog("CWPKI0803A: SSL certificate created") == null) {
			Log.info(AcmeFatUtils.class, "waitForSslToCreateKeystore",
					"SSL Cert not created -- requesting javacore to see if RTC bug 277292 was recreated.");
			try {
				server.javadumpThreads();
			} catch (Exception e) {
				Log.error(AcmeFatUtils.class, "waitForSslToCreateKeystore", e,
						"Tried to request a java thread dump, but it failed.");
			}
			junit.framework.Assert.fail(
					"ACME did not create a new certificate. Issued javacore. Check if RTC bug 277292 was recreated.");
		}

		// assertNotNull("ACME did not create a new certificate.",
		// server.waitForStringInLog("CWPKI0803A: SSL certificate created"));
	}

	/**
	 * Wait for the ACME service to report that a new certificate has been
	 * created.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForAcmeToCreateCertificate(LibertyServer server) {
		assertNotNull("ACME did not fetch the certificate.", server.waitForStringInLog("CWPKI2064I", 120000));
		/* 
		 * Longer timeout for local runs, similar to timeout for builds
		 */
		assertNotNull("ACME did not create the certificate.", server.waitForStringInLogUsingMark("CWPKI2007I", 120000));
	}
	
	/**
	 * Wait for the ACME service to report that a certificate has been revoked
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForAcmeToRevokeCertificate(LibertyServer server) {
		assertNotNull("ACME did not revoke the certificate.", server.waitForStringInLog("CWPKI2038I", 120000));
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
	 * Wait for the ACME authorization web application to start.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForAcmeAppToStart(LibertyServer server) {
		assertNotNull("ACME authorization web application did not start.", server
				.waitForStringInTrace("ACME authorization web application has started and is available for requests"));
	}

	/**
	 * Wait for the ACME authorization web application to start.
	 * 
	 * @param server
	 *            The server to check.
	 */
	public static final void waitForAcmeToRevokeCert(LibertyServer server) {
		assertNotNull("ACME failed to revoke the certificate.", server.waitForStringInLog("CWPKI2038I"));
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
		certFile = creator.createDefaultSSLCertificate(filePath, SELF_SIGNED_KEYSTORE_PASSWORD, "PKCS12", null,
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
	 * @param <assertAndGetServerCertificate>
	 * 
	 * @param server
	 *            the liberty server to communicate with.
	 * @return the server's current TLS certificate chain.
	 * @throws Exception
	 *             If the server is not using a certificate signed by the ACME
	 *             CA.
	 */
	public static final <assertAndGetServerCertificate> Certificate[] assertAndGetServerCertificate(
			LibertyServer server, CAContainer container) throws Exception {
		final String methodName = "assertServerCertificate()";

		/*
		 * Get the CA's intermediate certificate.
		 */
		X509Certificate caCertificate = AcmeFatUtils.getX509Certificate(container.getAcmeCaIntermediateCertificate());

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
				AcmeFatUtils.logHttpResponse(AcmeFatUtils.class, methodName, httpGet, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != 200) {
					/*
					 * We can still get the certificate even if we get a non-200 response.
					 */
					Log.info(AcmeFatUtils.class, "assertAndGetServerCertificate", "Expected response 200, but received response: " + statusLine +". " + response);
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
	 * Wait for assertAndGetServerCertificate to complete without errors. If it
	 * fails until the timeout, run again to throw the exception back to the caller.
	 * 
	 * @param server
	 * @param container
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public static final <assertAndGetServerCertificate> Certificate[] waitForAcmeCert(LibertyServer server,
			CAContainer container, long timeout) throws Exception {
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < startTime + timeout) {
			Log.info(AcmeFatUtils.class, "waitForAcmeCert", "Cert checking to swap from self signed to acme ");
			try {
				return assertAndGetServerCertificate(server, container);
			} catch (Exception e) {
				Thread.sleep(1000);
			}
		}
		return assertAndGetServerCertificate(server, container);
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
		File accountKey = new File(server.getServerRoot() + "/resources/security/acmeAccountKey.pem");
		File domainKey = new File(server.getServerRoot() + "/resources/security/acmeDomainKey.pem");
		List<Object[]> failedFiles = new ArrayList<Object[]>();
		int attempt = 0;
		int retries = 12;
		/*
		 * Keep attempting to delete until we have either deleted all the files, or
		 * exhausted all attempts.
		 */
		while (attempt <= retries) {
			failedFiles.clear();
			try {
				Files.deleteIfExists(keystore.toPath());
			} catch (IOException e1) {
				Log.error(AcmeFatUtils.class, "Error deleting keystore: ", e1);
				failedFiles.add(new Object[] { keystore, e1 });
			}
			try {
				Files.deleteIfExists(accountKey.toPath());
			} catch (IOException e1) {
				Log.error(AcmeFatUtils.class, "Error deleting account key: ", e1);
				failedFiles.add(new Object[] { accountKey, e1 });
			}
			try {
				Files.deleteIfExists(domainKey.toPath());
			} catch (IOException e1) {
				Log.error(AcmeFatUtils.class, "Error deleting domain key: ", e1);
				failedFiles.add(new Object[] { domainKey, e1 });
			}
			/*
			 * If no failures, discontinue.
			 */
			if (failedFiles.isEmpty()) {
				break;
			}
			attempt++;
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
		}
		if (!failedFiles.isEmpty()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Failed to delete ACME files after " + retries
					+ ". Future tests may fail. If this is a Windows/OpenJDK run, may need to update the OpenJDK level in the method, isWindowsWithOpenJDK(). The following files failed: ");
			for (Object[] failure : failedFiles) {
				File f = (File) failure[0];
				IOException ioe = (IOException) failure[1];
				sb.append("[");
				sb.append(f.getAbsolutePath());
				sb.append(", Is Readable? ");
				sb.append(f.canRead());
				sb.append(", Is Writable? ");
				sb.append(f.canWrite());
				sb.append(", Exception: ");
				sb.append(ioe.getMessage());
				sb.append("]");
			}
			fail(sb.toString());
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

	/**
	 * Wait for a new certificate compared to the provided certificate using the default timeout
	 * @param server
	 * @param acmeContainer
	 * @param startingCertificateChain
	 * @throws Exception
	 */
	public static final Certificate[]  waitForNewCert(LibertyServer server, CAContainer acmeContainer, Certificate[] startingCertificateChain) throws Exception {
		return waitForNewCert(server, acmeContainer, startingCertificateChain, SCHEDULE_TIME);
	}

	/**
	 *  Wait for a new certificate compared to the provided certificate using a custom timeout
	 * @param server
	 * @param acmeContainer
	 * @param startingCertificateChain
	 * @param timeout
	 * @throws Exception
	 */
	public static final Certificate[]  waitForNewCert(LibertyServer server, CAContainer acmeContainer, Certificate[] startingCertificateChain, long timeout) throws Exception {
		Certificate[] endingCertificateChain;
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < startTime + timeout) {
			Log.info(AcmeFatUtils.class, "waitForNewCert", "Cert checking ");
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, acmeContainer);

			if (((X509Certificate) startingCertificateChain[0]).getSerialNumber()
					.equals(((X509Certificate) endingCertificateChain[0]).getSerialNumber())) {
				Thread.sleep(1000);
			} else {
				break;
			}
		}

		endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, acmeContainer);

		String serial1 = ((X509Certificate) startingCertificateChain[0]).getSerialNumber().toString(16);
		String serial2 = ((X509Certificate) endingCertificateChain[0]).getSerialNumber().toString(16);

		assertThat("Expected a new certificate.", serial1, not(equalTo(serial2)));	
		
		return endingCertificateChain;
	}
	
	/**
 	 * Check if the test is running on Windows OS.
 	 * @param methodName the name of the method being run.
 	 * @return True if the test is running on Windows.
 	 */
 	public static boolean isWindows(String methodName) {
 		if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
 			// windows not enforcing the setReadable/setWriteable
 			Log.info(AcmeFatUtils.class, methodName,
 					"Skipping unreadable/unwriteable file tests on Windows: "
 							+ System.getProperty("os.name", "unknown"));
 			return true;
 		}
 		return false;
 	}

	/**
	 * Check if the test is running on Windows OS and a specific java
	 * 
	 * @param methodName
	 * @return True if the test is running on the specific OS/JDK combo
	 */
	public static boolean isWindowsWithOpenJDK(String methodName) {
		String os = System.getProperty("os.name").toLowerCase();
		String javaVendor = System.getProperty("java.vendor").toLowerCase();
		String javaVersion = System.getProperty("java.version");
		Log.info(AcmeFatUtils.class, methodName,
				"Checking os.name: " + os + " java.vendor: " + javaVendor + " java.version: " + javaVersion);
		if (os.startsWith("win") && (javaVendor.contains("openjdk") || javaVendor.contains(("oracle")))
				&& (javaVersion.equals("14.0.1") || javaVersion.equals("1.8.0_181")
						|| javaVersion.equals("15") || javaVersion.equals("16") || javaVersion.equals("17"))) {
			/*
			 * On Windows with OpenJDK 11.0.5 (and others), we sometimes get an exception
			 * deleting the Acme related files. Later JDK11s seem to be working. 11.0.7+10, 11.0.8+10, 11.0.10+9, 11.0.12+7
			 * are fine.
			 * 
			 * "The process cannot access the file because it is being used by another
			 * process"
			 * 
			 */
			Log.info(AcmeFatUtils.class, methodName,
					"Skipping this test due to a bug with the specific OS/JDK combo: " + System.getProperty("os.name")
							+ " " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
			return true;
		}
		return false;
	}

 	/**
 	 * Handle adding CWPKI2045W as an allowed warning message to all stopServer requests.
 	 * 
 	 * @param server
 	 * @param msgs
 	 * @throws Exception
 	 */
 	public static void stopServer(LibertyServer server, String...msgs) throws Exception{
 		/*
 		 * If the test Pebble or Boulder container is slightly ahead of our test machine, we can
 		 * get a certificate that is in "the future" and that will produce a warning message.
 		 */
 		String alwaysAdd = "CWPKI2045W";
 		
 		List<String> tempList = new ArrayList<String>(Arrays.asList(msgs));
		tempList.add(alwaysAdd);
		server.stopServer(tempList.toArray(new String[tempList.size()]));
 	}

	/**
	 * Issue a POST request to the ACME REST API to renew the certificate
	 * 
	 * @return The JSON response.
	 * @throws Exception
	 *                       if the request failed.
	 */
	public static String renewCertificate(LibertyServer server) throws Exception {
		final String methodName = "renewCertificate()";
		Log.info(AcmeFatUtils.class, methodName, "RenewCertificate request");

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a POST request to the Liberty server.
			 */
			HttpPost httpPost = new HttpPost("https://localhost:" + server.getHttpDefaultSecurePort() + "/ibm/api"
					+ AcmeCaRestHandler.PATH_CERTIFICATE);
			httpPost.setHeader("Authorization", "Basic " + DatatypeConverter
					.printBase64Binary((AcmeFatUtils.ADMIN_USER + ":" + AcmeFatUtils.ADMIN_PASS).getBytes()));
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity("{\"operation\":\"renewCertificate\"}"));

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(AcmeRevocationTest.class, methodName, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", 200, statusLine.getStatusCode());

				/*
				 * Check content type header.
				 */
				Header[] headers = response.getHeaders("content-type");
				assertNotNull("Expected content type header.", headers);
				assertEquals("Expected 1 content type header.", 1, headers.length);
				assertEquals("Unexpected content type.", "application/json", headers[0].getValue());

				String contentString = EntityUtils.toString(response.getEntity());
				Log.info(AcmeValidityAndRenewTest.class, methodName, "HTTP post contents: \n" + contentString);

				return contentString;
			}
		}
	}
}
