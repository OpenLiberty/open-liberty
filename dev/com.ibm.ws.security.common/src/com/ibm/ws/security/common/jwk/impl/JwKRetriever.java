/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.common.jwk.internal.JwkConstants;

import com.ibm.wsspi.ssl.SSLSupport;

/**
 *
 */
public class JwKRetriever {
	private static final TraceComponent tc = Tr.register(JwKRetriever.class);

	String configId = null;
	String sslConfigurationName = null;
	String jwkEndpointUrl = null; // jwksUri

	String sigAlg = "RS256"; // TODO may need to allow it to be set in
								// configuration
	JWKSet jwkSet = null; // using the JWKSet from the JwtConsumerConfig. Do not
							// create it every time
	SSLSupport sslSupport = null;//JwtUtils.getSSLSupportService();

	String keyFileName = null;

	boolean hostNameVerificationEnabled = true;

	/**
	 *
	 * @param configId
	 *            config ID
	 * @param sslConfigurationName
	 *            sslRef
	 * @param jwkEndpointUrl
	 *            jwksUri
	 * @param jwkSet
	 *            using the jwkSet from the config
	 * @param hnvEnabled 
	 */
	public JwKRetriever(String configId, String sslConfigurationName, String jwkEndpointUrl, JWKSet jwkSet, SSLSupport sslSupport, boolean hnvEnabled) {
		this.configId = configId;
		this.sslConfigurationName = sslConfigurationName;
		this.jwkEndpointUrl = jwkEndpointUrl;
		this.jwkSet = jwkSet; // get the JWKSet from the Config
		this.sslSupport = sslSupport;
		this.hostNameVerificationEnabled = hnvEnabled;
	}

//	public JwKRetriever(JwtConsumerConfig config) {
//		configId = config.getId();
//		sslConfigurationName = config.getSslRef();
//		jwkEndpointUrl = config.getJwkEndpointUrl();
//		jwkSet = config.getJwkSet();
//		hostNameVerificationEnabled = config.isHostNameVerificationEnabled();
//		
//	}

	/**
	 * Either kid or x5t will work. But not both
	 *
	 * @param kid
	 * @param x5t
	 * @return
	 * @throws PrivilegedActionException
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws InterruptedException
	 */
	@FFDCIgnore({ KeyStoreException.class })
	public PublicKey getPublicKeyFromJwk(String kid, String x5t)
			throws PrivilegedActionException, IOException, KeyStoreException, InterruptedException {
		PublicKey key = this.getJwkCache(kid, x5t);
		KeyStoreException errKeyStoreException = null;
		InterruptedException errInterruptedException = null;
		if (key == null) {
			try {
				key = this.getJwkRemote(kid, x5t);
			} catch (KeyStoreException e) {
				errKeyStoreException = e;
			} catch (InterruptedException e) {
				errInterruptedException = e;
			}
		}
		if (key == null) {
			if (errKeyStoreException != null) {
				throw errKeyStoreException;
			}
			if (errInterruptedException != null) {
				throw errInterruptedException;
			}
		}
		return key;
	}

	protected PublicKey getJwkCache(String kid, String x5t) {
		if (kid != null) {
			return jwkSet.getPublicKeyByKid(kid);
		} else if (x5t != null) {
			return jwkSet.getPublicKeyByx5t(x5t);
		}
		return jwkSet.getPublicKeyByKid(null);
	}

	protected PublicKey getJwkLocal(String kid, String x5t) throws PrivilegedActionException, IOException {
		// JWKSet jwkset = clientConfig.getJwkSet();
		try {
			final String keyFile = keyFileName;// clientConfig.getJsonWebKey();
			if (keyFile == null) {
				return null;
			}

			FileInputStream inputStream = (FileInputStream) AccessController
					.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
							File fileJwk = new File(keyFile);
							if (fileJwk.exists()) {
								return new FileInputStream(fileJwk);
							} else {
								return null;
							}
						}
					});
			if (inputStream == null) {
				return null;
			}
			parseJwk(null, inputStream, jwkSet, sigAlg);

			// Object obj = parser.parse(fileReader);

			// JsonObject jsonObject = (JsonObject) obj;
			// for (JsonElement element : jsonObject.getAsJsonArray("keys")) {
			// JsonObject keyObject = element.getAsJsonObject();
			// JWK jwk = Jose4jRsaJWK.getInstance(keyObject);
			// if (jwk != null) {
			// jwk.parse();
			// jwkset.addJWK(jwk);
			// if (tc.isDebugEnabled()) {
			// Tr.debug(tc, "add local key for keyid: ", jwk.getKeyID());
			// }
			// }
			// }
			// fileReader.close();
		} catch (PrivilegedActionException e) {
			throw e;
		}

		PublicKey key = null;
		if (kid != null) {
			key = jwkSet.getPublicKeyByKid(kid);
		} else if (x5t != null) {
			key = jwkSet.getPublicKeyByx5t(x5t);
		} else {
			key = jwkSet.getPublicKeyByKid(null);
		}

		return key;

	}

	@FFDCIgnore({ KeyStoreException.class })
	protected PublicKey getJwkRemote(String kid, String x5t) throws KeyStoreException, InterruptedException {
		String jwkUrl = jwkEndpointUrl;
		if (jwkUrl == null || !jwkUrl.startsWith("http")) {
			return null;
		}
		PublicKey key = null;
		try {
			synchronized (jwkSet) {
				key = this.getJwkCache(kid, x5t);
				if (key == null) {
					key = doJwkRemote(kid, x5t);
				}
			}
		} catch (KeyStoreException e) {
			throw e;
		}
		return key;
	}

	@FFDCIgnore({ Exception.class, KeyStoreException.class })
	protected PublicKey doJwkRemote(String kid, String x5t) throws KeyStoreException {

		String jsonString = null;
		String jwkUrl = jwkEndpointUrl;

		try {
			// TODO - validate url
			SSLSocketFactory sslSocketFactory = getSSLSocketFactory(jwkUrl, sslConfigurationName, sslSupport);
			HttpClient client = createHTTPClient(sslSocketFactory, jwkUrl, hostNameVerificationEnabled);
			jsonString = getHTTPRequestAsString(client, jwkUrl);
			boolean bJwk = parseJwk(jsonString, null, jwkSet, sigAlg);
			if (!bJwk) {
				// can not get back any JWK from OP
				// since getJwkLocal will be called later and NO key exception
				// will be handled in the parent callers
				// debug here only
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "No JWK can be found through '" + jwkUrl + "'");
				}
			}

		} catch (KeyStoreException e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Fail to retrieve remote key: ", e.getCause());
			}
			throw e;
		} catch (Exception e) {
			// could be ignored
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Fail to retrieve remote key: ", e.getCause());
			}
		}

		PublicKey key = null;
		if (kid != null) {
			key = jwkSet.getPublicKeyByKid(kid);
		} else if (x5t != null) {
			key = jwkSet.getPublicKeyByx5t(x5t);
		} else {
			key = jwkSet.getPublicKeyByKid(null);
		}
		return key;
	}

	// separate to be an independent method for unit tests
	public boolean parseJwk(String jsonString, FileInputStream inputStream, JWKSet jwkset, String signatureAlgorithm) {
		boolean bJwk = false;

		JSONObject jsonObject = null;
		if (jsonString != null) {
			jsonObject = parseJsonObject(jsonString);
		} else if (inputStream != null) {
			jsonObject = parseJsonObject(inputStream);
		}

		if (jsonObject == null) {
			return false;
		}
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Parsed JSON object: " + jsonObject.toString());
		}

		Object keysEntry = jsonObject.get("keys");
		if (keysEntry == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Failed to find keys array in provided JSON object");
			}
			return false;
		}
		JSONArray keyArray = parseJsonArray(keysEntry.toString());
		if (keyArray == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Failed to parse keys array in provided JSON object");
			}
			return false;
		}
		for (Object element : keyArray) {
			JSONObject jElement = parseJsonObject(element.toString());
			if (jElement == null) {
				continue;
			}
			if (jsonObjectContainsKtyForValidJwk(jElement, jwkset, signatureAlgorithm)) {
				bJwk = true;
			}
		}

		return bJwk;
	}

	@FFDCIgnore(Exception.class)
	JSONObject parseJsonObject(String jsonString) {
		JSONObject jsonObject = null;
		try {
			jsonObject = JSONObject.parse(jsonString);
		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Caught exception parsing JSON string [" + jsonString + "]: " + e.getMessage());
			}
		}
		return jsonObject;
	}

	@FFDCIgnore(Exception.class)
	JSONObject parseJsonObject(InputStream is) {
		JSONObject jsonObject = null;
		try {
			jsonObject = JSONObject.parse(is);
		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Caught exception parsing input stream [" + is.toString() + "]: " + e.getMessage());
			}
		}
		return jsonObject;
	}

	@FFDCIgnore(Exception.class)
	JSONArray parseJsonArray(String jsonString) {
		JSONArray jsonArray = null;
		try {
			jsonArray = JSONArray.parse(jsonString);
		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Caught exception parsing JSON string [" + jsonString + "]: " + e.getMessage());
			}
		}
		return jsonArray;
	}

	boolean jsonObjectContainsKtyForValidJwk(JSONObject entry, JWKSet jwkset, String signatureAlgorithm) {
		if (entry == null) {
			return false;
		}

		JWK jwk = null;
		String kty = (String) entry.get("kty");
		if (kty == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "JSON object is missing 'kty' entry");
			}
			return false;
		}

		jwk = createJwkBasedOnKty(kty, entry, signatureAlgorithm);
		if (jwk == null) {
			return false;
		}

		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Parsing JWK and adding it to JWK set");
		}
		jwk.parse();
		jwkset.addJWK(jwk);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "add remote key for keyid: ", jwk.getKeyID());
		}
		return true;
	}

	JWK createJwkBasedOnKty(String kty, JSONObject keyEntry, String signatureAlgorithm) {
		JWK jwk = null;
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "kty of JWK is '" + kty + "'");
		}
		if (JwkConstants.RSA.equalsIgnoreCase(kty)) {
			jwk = getRsaJwk(keyEntry);
		} else if (JwkConstants.EC.equalsIgnoreCase(kty)) {
			jwk = getEllipticCurveJwk(keyEntry, signatureAlgorithm);
		}
		return jwk;
	}

	JWK getRsaJwk(JSONObject thing) {
		// interop: Azure does not emit the sig.alg attribute, so do not check
		// for it.
		// if (signatureAlgorithm.startsWith("RS")) {// RS256, RS384, RS512
		return Jose4jRsaJWK.getInstance(thing);
	}

	JWK getEllipticCurveJwk(JSONObject thing, String signatureAlgorithm) {
		// let get the map<String, Object> from keyObject
		if (signatureAlgorithm.startsWith("ES")) { // ES256, ES384, ES512
			return Jose4jEllipticCurveJWK.getInstance(thing); // if implemented
																// ES256
		}
		return null;
	}

	protected JSSEHelper getJSSEHelper(SSLSupport sslSupport) throws SSLException {
		if (sslSupport != null) {
			return sslSupport.getJSSEHelper();
		}
		return null;
	}

	protected SSLSocketFactory getSSLSocketFactory(String requestUrl, String sslConfigurationName,
			SSLSupport sslSupport) throws SSLException {
		SSLSocketFactory sslSocketFactory = null;

		try {
			sslSocketFactory = sslSupport.getSSLSocketFactory(sslConfigurationName);
		} catch (javax.net.ssl.SSLException e) {
			throw new SSLException(e.getMessage());
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "sslSocketFactory (" + ") get: " + sslSocketFactory);
		}

		if (sslSocketFactory == null) {
			if (requestUrl != null && requestUrl.startsWith("https")) {
				throw new SSLException(Tr.formatMessage(tc, "JWT_HTTPS_WITH_SSLCONTEXT_NULL",
						new Object[] { "Null ssl socket factory", configId }));
			}
		}
		return sslSocketFactory;
	}

	@FFDCIgnore({ KeyStoreException.class })
	protected String getHTTPRequestAsString(HttpClient httpClient, String url) throws Exception {

		String json = null;
		try {
			HttpGet request = new HttpGet(url);
			request.addHeader("content-type", "application/json");
			HttpResponse result = null;
			try {
				result = httpClient.execute(request);
			} catch (IOException ioex) {
				logCWWKS6049E(url, 0, "IOException: " + ioex.getMessage() + " " + ioex.getCause());
				throw ioex;
			}
			StatusLine statusLine = result.getStatusLine();
			int iStatusCode = statusLine.getStatusCode();
			if (iStatusCode == 200) {
				json = EntityUtils.toString(result.getEntity(), "UTF-8");
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "Response: ", json);
				}
				if (json == null || json.isEmpty()) { // NO JWK returned
					throw new Exception(logCWWKS6049E(url, iStatusCode, json));
				}
			} else {
				String errMsg = EntityUtils.toString(result.getEntity(), "UTF-8");
				// error in getting JWK
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "status:" + iStatusCode + " errorMsg:" + errMsg);
				}
				throw new Exception(logCWWKS6049E(url, iStatusCode, errMsg));
			}
		} catch (KeyStoreException e) {
			throw e;
		}

		return json;
	}

	private String logCWWKS6049E(String url, int iStatusCode, String errMsg) {
		// TODO - Message will be added to .nlsprops file under 222394
		String defaultMessage = "CWWKS6049E: A JSON Web Key (JWK) was not returned from the URL [" + url
				+ "]. The response status was [" + iStatusCode + "] and the content returned was [" + errMsg + "].";

		String message = TraceNLS.getFormattedMessage(getClass(),
				"com.ibm.ws.security.jwt.internal.resources.JWTMessages", "JWT_JWK_RETRIEVE_FAILED",
				new Object[] { url, Integer.valueOf(iStatusCode), errMsg }, defaultMessage);
		Tr.error(JwKRetriever.tc, message, new Object[0]);
		return message;
	}

	public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification) {

		HttpClient client = null;

		if (url.startsWith("http:")) {
			client = HttpClientBuilder.create().build();
		} else {
			SSLConnectionSocketFactory connectionFactory = null;
			if (!isHostnameVerification) {
				connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new AllowAllHostnameVerifier());
			} else {
				connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new StrictHostnameVerifier());
			}
			client = HttpClientBuilder.create().setSSLSocketFactory(connectionFactory).build();
		}

		// BasicCredentialsProvider credentialsProvider = new
		// BasicCredentialsProvider();
		// credentialsProvider.setCredentials(AuthScope.ANY, new
		// UsernamePasswordCredentials("username", "mypassword"));

		return client;

	}
}
