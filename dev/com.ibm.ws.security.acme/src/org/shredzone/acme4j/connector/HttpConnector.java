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

package org.shredzone.acme4j.connector;

import static com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_DIRECTION;
import static com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_REMOTE_HOST;
import static com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_REMOTE_PORT;
import static com.ibm.websphere.ssl.JSSEHelper.DIRECTION_OUTBOUND;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.acme.internal.AcmeConfigService;
import com.ibm.ws.security.acme.internal.AcmeProviderImpl;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.ssl.provider.AbstractJSSEProvider;

/**
 * Liberty specific generic ACME4J HttpConnector. This implementation will
 * replace the ACME4J HttpConnector of the same name. We replace the ACME4J
 * version so that Liberty can control SSL outside of the JRE defaults.
 * 
 * <br/>
 * Be aware that overriding this class means ACME4J upgrades can lead to compile
 * issues in this class, and perhaps unanticipated changes in behavior (new
 * provider URIs that we don't anticipate as of now). It might be better to
 * introduce a feature in ACME4J that would give us a plug point to inject our
 * own SSLSocketFactory in the generic provider for HTTPS.
 */
public class HttpConnector {

	private static final TraceComponent tc = Tr.register(HttpConnector.class);

	private static final String USER_AGENT;

	static {
		StringBuilder agent = new StringBuilder("acme4j");

		try (InputStream in = HttpConnector.class.getResourceAsStream("/org/shredzone/acme4j/version.properties")) {
			Properties prop = new Properties();
			prop.load(in);
			agent.append('/').append(prop.getProperty("version"));
		} catch (Exception ex) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Could not read library version", ex);
			}
		}

		agent.append(" Java/").append(System.getProperty("java.version"));
		USER_AGENT = agent.toString();
	}

	/**
	 * Returns the default User-Agent to be used.
	 *
	 * @return User-Agent
	 */
	public static String defaultUserAgent() {
		return USER_AGENT;
	}

	/**
	 * Opens a {@link HttpURLConnection} to the given {@link URL}.
	 *
	 * @param url
	 *            {@link URL} to connect to
	 * @param proxy
	 *            {@link Proxy} to be used
	 * @return {@link HttpURLConnection} connected to the {@link URL}
	 */
	public HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
		configure(conn, url);
		return conn;
	}

	/**
	 * Configures the new {@link HttpURLConnection}.
	 * <p>
	 * This implementation sets reasonable timeouts, forbids caching, and sets
	 * an user agent.
	 *
	 * @param conn
	 *            {@link HttpURLConnection} to configure.
	 * @param url
	 *            {@link URL} to connect to
	 */
	protected void configure(HttpURLConnection conn, URL url) throws IOException {
		int connectTimeout;
		int readTimeout;
		if (AcmeConfigService.getThreadLocalAcmeConfig() != null) {
			connectTimeout = AcmeConfigService.getThreadLocalAcmeConfig().getHTTPConnectTimeout().intValue();
			readTimeout = AcmeConfigService.getThreadLocalAcmeConfig().getHTTPReadTimeout().intValue();
		} else {
			connectTimeout = AcmeProviderImpl.getAcmeConfig().getHTTPConnectTimeout();
			readTimeout = AcmeProviderImpl.getAcmeConfig().getHTTPReadTimeout();
		}
		conn.setConnectTimeout(connectTimeout);
		conn.setReadTimeout(readTimeout);
		conn.setUseCaches(false);
		conn.setRequestProperty("User-Agent", USER_AGENT);

		/*
		 * Add our own custom trust material via a custom SSLSocketFactory.
		 * 
		 * Currently there are only 2 custom ACME providers (acme://pebble and
		 * acme://letsencrypt.org) besides the default provider (http and
		 * https).
		 * 
		 * Pebble uses a static self-signed certificate for the trust. Don't use
		 * any configured trust.
		 * 
		 * Letsencrypt should be in the CACERTS, which we load in our custom
		 * TrustManager.
		 * 
		 * Generic providers (https only) can use either CACERTS or a configured
		 * trust via our custom TrustManager.
		 */
		String scheme = url.getProtocol();
		String host = url.getHost();
		if (conn instanceof HttpsURLConnection
				&& !("acme".equalsIgnoreCase(scheme) && "pebble".equalsIgnoreCase(host))) {
			HttpsURLConnection conns = (HttpsURLConnection) conn;
			conns.setSSLSocketFactory(createSocketFactory(url));
			conns.setHostnameVerifier((h, s) -> true);
		}
	}

	/**
	 * Create an {@link SSLSocketFactory} to use with HTTPS connections to ACME
	 * CA servers. The {@link SSLSocketFactory} will use either the configured
	 * trust store or the JRE default (CACERTS).
	 * 
	 * @param providerURL
	 *            The provider URL.
	 * @return A {@link SSLSocketFactory}.
	 * @throws IOException
	 *             if the {@link SSLSocketFactory} could not be created.
	 */
	protected synchronized SSLSocketFactory createSocketFactory(URL providerURL) throws IOException {

		final String methodName = "createSocketFactory(URL)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, providerURL);
		}

		/*
		 * Set out bound connection info.
		 */
		final Map<String, Object> connectionInfo = new HashMap<String, Object>();
		connectionInfo.put(CONNECTION_INFO_DIRECTION, DIRECTION_OUTBOUND);
		connectionInfo.put(CONNECTION_INFO_REMOTE_HOST, providerURL.getHost());
		connectionInfo.put(CONNECTION_INFO_REMOTE_PORT,
				providerURL.getPort() == -1 ? "434" : Integer.toString(providerURL.getPort()));

		/*
		 * Get the configured SSL properties.
		 */
		SSLConfig sslConfig;
		if (AcmeConfigService.getThreadLocalAcmeConfig() != null) {
			/*
			 * ThreadLocal indicates we are probably testing configuration.
			 */
			sslConfig = AcmeConfigService.getThreadLocalAcmeConfig().getSSLConfig();
		} else {
			/*
			 * Normal 'configured' path.
			 */
			sslConfig = AcmeProviderImpl.getSSLConfig();
		}

		/*
		 * Use the SSLConfig if one is provided, otherwise just use the default
		 * TrustManager.
		 */
		if (sslConfig.getProperty(Constants.SSLPROP_TRUST_STORE) != null) {

			/*
			 * If a truststore is configured, generate an SSLContext and get a
			 * SSLSocktFactory using JSSEHelper.
			 */
			SSLContext ctx;
			try {
				ctx = JSSEHelper.getInstance().getSSLContext(connectionInfo, sslConfig);

				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.exit(tc, methodName, ctx.getClass());
				}
				return ctx.getSocketFactory();
			} catch (SSLException e) {
				throw new IOException("Failed to generate SSLContext with custom TrustManager.", e);
			}
		} else {
			/*
			 * If no truststore is configured, we will use CACERTS. JSSEHelper
			 * doesn't support using CACERTS alone, so we will create our own
			 * SSLSocketFactory and load it with the default TrustManager.
			 */
			try {
				String protocol = sslConfig.getProperty(Constants.SSLPROP_PROTOCOL);
				SSLContext sslContext = null;

				/*
				 * Get an SSLContext instance, and initialize it with the
				 * default TrustManager.
				 */
				if (protocol != null) {
					sslContext = SSLContext.getInstance(protocol);
					sslContext.init(null, AbstractJSSEProvider.getDefaultTrustManager(), null);
				} else {
					sslContext = SSLContext.getDefault();
				}

				return sslContext.getSocketFactory();

			} catch (Exception e) {
				throw new IOException("Failed to generate SSLSocketFactory with default TrustManager.", e);
			}
		}
	}
}
