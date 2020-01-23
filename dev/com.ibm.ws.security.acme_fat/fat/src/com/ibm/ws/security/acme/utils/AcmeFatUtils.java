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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import com.ibm.websphere.simplicity.log.Log;

/**
 * A collection of utility method for the ACME FAT component.
 */
public class AcmeFatUtils {

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
	 *             If the client coulnd't be created for some unforeseen reason.
	 */
	public static CloseableHttpClient getInsecureHttpClient() throws Exception {

		SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
		HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
		SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
		return HttpClients.custom().setSSLSocketFactory(connectionFactory).build();
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
}
