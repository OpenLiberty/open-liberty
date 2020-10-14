/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.jose4j.lang.JoseException;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.config.JwtConfigUtil;
import com.ibm.ws.security.jwt.registry.RegistryClaims;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.ssl.SSLSupport;

public class JwtUtils {
	private static final TraceComponent tc = Tr.register(JwtUtils.class);

	public static final String CFG_KEY_ID = "id";
	public static final String CFG_KEY_ISSUER = "issuer";
	public static final String CFG_KEY_JWK_ENABLED = "jwkEnabled";
	public static final String CFG_KEY_VALID = "expiry";
	public static final String CFG_KEY_JTI = "jti"; // builder
	public static final String CFG_KEY_JTI_CHECK_ENABLED = "jtiCheckEnabled"; // consumer
	public static final String CFG_KEY_SCOPE = "scope";
	public static final String CFG_KEY_AUDIENCES = "audiences";
	public static final String CFG_KEY_SIGNATURE_ALGORITHM = "signatureAlgorithm";
	public static final String CFG_KEY_CLAIMS = "claims";
	public static final String CFG_KEY_KEYSTORE_REF = "keyStoreRef";
	public static final String CFG_KEY_KEY_ALIAS_NAME = "keyAlias";
	public static final String CFG_KEY_TRUSTSTORE_REF = "trustStoreRef";
	public static final String CFG_KEY_TRUSTED_ALIAS = "trustedAlias";
	public static final String CFG_KEY_SHARED_KEY = "sharedKey";
	public static final String CFG_KEY_JWK_ROTATION_TIME = "jwkRotationTime";
	public static final String CFG_KEY_JWK_SIGNING_KEY_SIZE = "jwkSigningKeySize";
	public static final String CFG_KEY_JWK_ENDPOINT_URL = "jwkEndpointUrl";
	public static final String CFG_KEY_CLOCK_SKEW = "clockSkew";
	public static final String CFG_KEY_VALIDATION_REQUIRED = "validationRequired";
	public static final String CFG_KEY_SSL_REF = "sslRef";
	public static final String CFG_KEY_EXPIRES_IN_SECONDS = "expiresInSeconds";
	public static final String CFG_KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS = "useSystemPropertiesForHttpClientConnections";
	public static final String CFG_KEY_ELAPSED_NBF = "elapsedNBF";
	public static final String CFG_AMR_CLAIM = "amrValues";
	public static final String CFG_AMR_ATTR = "amrInclude";

	public static final String JCEPROVIDER_IBM = "IBMJCE";
	public static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";
	public static final String SECRANDOM_IBM = "IBMSecureRandom";

	public static final String ISSUER = "iss";
	public static final String SUBJECT = "sub";
	public static final String AUDIENCE = "aud";
	public static final String SCOPE = "scope";
	public static final String EXPIRATION = "exp";
	public static final String NOT_BEFORE = "nbf";
	public static final String ISSUED_AT = "iat";
	public static final String ID = "jti";
	public static final String KEY = "signKey";
	public static final String ALG = "signAlg";
	public static final String KS = "KeyStore";
	public static final String KS_ALIAS = "KeyStore_ALIAS";
	public static final String TS = "TrustStore";
	public static final String TS_ALIAS = "TrustStore_ALIAS";

	public static final String DELIMITER = ".";

	private static final String KEY_VMM_SERVICE = "vmmService";
	public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
	private static AtomicServiceReference<VMMService> vmmServiceRef = new AtomicServiceReference<VMMService>(
			KEY_VMM_SERVICE);
	private static AtomicServiceReference<KeyStoreService> keyStoreServiceRef;
	private static ConcurrentServiceReferenceMap<String, KeyStoreService> keyStoreServiceMapRef = new ConcurrentServiceReferenceMap<String, KeyStoreService>(
			KEY_KEYSTORE_SERVICE);
	private static AtomicServiceReference<SSLSupport> sslSupportRef;

	// private static AtomicServiceReference<VirtualHost> virtualHostRef;

	// some selective message management for expired token flows where we don't
	// want
	// a lot of stuff in the log
	private static ThreadLocal<Boolean> isJwtSsoValidationPath = new ThreadLocal<Boolean>();
	private static ThreadLocal<Boolean> isJwtSsoValidationPathExpiredToken = new ThreadLocal<Boolean>();

	public static void setJwtSsoValidationPath() {
		isJwtSsoValidationPath.set(true);
	}

	public static boolean setJwtSsoValidationPathExiredToken() {
		if (isJwtSsoValidationPath.get() != null && isJwtSsoValidationPath.get() == true) {
			isJwtSsoValidationPathExpiredToken.set(true);
			return true;
		}
		return false;
	}

	public static boolean isJwtSsoValidationExpiredTokenCodePath() {
		boolean result = isJwtSsoValidationPathExpiredToken.get() != null ? isJwtSsoValidationPathExpiredToken.get()
				: false;

		return result;
	}

	public static String convertToBase64(String source) {
		return Base64.encodeBase64URLSafeString(StringUtils.getBytesUtf8(source));
	}

	public static String decodeFromBase64String(String encoded) {
		return new String(Base64.decodeBase64(encoded));
	}

	public static boolean isBase64Encoded(String str) {
		if (!isNullEmpty(str)) {
			return Base64.isArrayByteBase64(StringUtils.getBytesUtf8(str));
		}
		return false;
	}

	public static String fromBase64ToJsonString(String source) {
		return StringUtils.newStringUtf8(Base64.decodeBase64(source));
	}

	public static String toJson(String source) {
		try {
			return StringUtils.newStringUtf8(source.getBytes("UTF8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return null;
	}

	public static boolean isNullEmpty(String value) {
		return value == null || value.isEmpty();
	}

	public static boolean isJson(String tokenString) {
		boolean result = false;
		if (!isNullEmpty(tokenString)) {
			if ((tokenString.startsWith("{") && tokenString.endsWith("}"))
					|| (tokenString.startsWith("[") && tokenString.endsWith("]"))) {
				result = true;
			}
		}
		return result;
	}

	// either from header or payload
	public static Object claimFromJsonObject(String jsonFormattedString, String claimName) throws JoseException {
		Object claim = null;

		// JSONObject jobj = JSONObject.parse(jsonFormattedString);
		Map<String, Object> jobj = org.jose4j.json.JsonUtil.parseJson(jsonFormattedString);
		if (jobj != null) {
			claim = jobj.get(claimName);
		}

		return claim;
	}

	// assuming payload not the whole token string
	public static Map claimsFromJsonObject(String jsonFormattedString) throws JoseException {
		Map claimsMap = new ConcurrentHashMap<String, Object>();

		// JSONObject jobj = JSONObject.parse(jsonFormattedString);
		Map<String, Object> jobj = org.jose4j.json.JsonUtil.parseJson(jsonFormattedString);
		Set<Entry<String, Object>> entries = jobj.entrySet();
		Iterator<Entry<String, Object>> it = entries.iterator();

		while (it.hasNext()) {
			Entry<String, Object> entry = it.next();

			String key = entry.getKey();
			Object value = entry.getValue();
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Key : " + key + ", Value: " + value);
			}
			if (!isNullEmpty(key) && value != null) {
				claimsMap.put(key, value);
			}

			// JsonElement jsonElem = entry.getValue();
		}

		// claims.putAll(jobj.entrySet());

		return claimsMap;
	}

	public static Map<String, Object> claimsFromJson(String jsonFormattedString) throws Exception {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "JSON String =" + jsonFormattedString);
		}
		JSONObject object = JSONObject.parse(jsonFormattedString);

		HashMap<String, Object> map = new HashMap<String, Object>();
		Set<Entry<String, Object>> set = object.entrySet();
		Iterator<Entry<String, Object>> iterator = set.iterator();

		while (iterator.hasNext()) {
			Entry<String, Object> entry = iterator.next();
			String key = entry.getKey();
			Object value = entry.getValue();

			if (isJsonArray(value)) {
				handleJsonArray(value, key, map);
			} else if (isJsonObject(value)) {
				map.put(key, claimsFromJson(value.toString()));
			} else {
				map.put(key, value.toString());
			}
		}
		return map;
	}

	@FFDCIgnore(Exception.class)
	public static void handleJsonArray(Object value, String key, HashMap<String, Object> map) {
		if (!isJsonArray(value)) {
			map.put(key, value.toString());
			return;
		}
		JSONArray jArray = null;
		try {
			jArray = JSONArray.parse(value.toString());
		} catch (Exception e) {
			// Shouldn't happen since we verified the value should be a JSON
			// array if we get here
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Caught exception handling provided object [" + value.toString() + "]: " + e.getMessage());
			}
			map.put(key, value.toString());
			return;
		}
		if (jArray != null && !jArray.isEmpty()) {
			map.put(key, jArray);
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "array list of objects, Key : " + key + ", Value: " + jArray.toString());
			}
		}
	}

	@FFDCIgnore(Exception.class)
	static boolean isJsonObject(Object value) {
		if (value == null) {
			return false;
		}
		try {
			JSONObject.parse(value.toString());
		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Value [" + value + "] is not a valid JSON object: " + e.getMessage());
			}
			return false;
		}
		return true;
	}

	@FFDCIgnore(Exception.class)
	static boolean isJsonArray(Object value) {
		if (value == null) {
			return false;
		}
		try {
			JSONArray.parse(value.toString());
		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Value [" + value + "] is not a valid JSON array: " + e.getMessage());
			}
			return false;
		}
		return true;
	}

	/**
	 * Trims each of the strings in the array provided and returns a new list
	 * with each string added to it. If the trimmed string is empty, that string
	 * will not be added to the final array. If no entries are present in the
	 * final array, null is returned.
	 *
	 * @param strings
	 * @return
	 */
	public static List<String> trimIt(String[] strings) {
		if (strings == null || strings.length == 0) {
			return null;
		}

		List<String> results = new ArrayList<String>();

		for (int i = 0; i < strings.length; i++) {
			String result = trimIt(strings[i]);
			if (result != null) {
				results.add(result);
			}
		}

		if (results.size() > 0) {
			return results;
		} else {
			return null;
		}
	}

	public static String trimIt(String str) {
		if (str == null) {
			return null;
		}
		str = str.trim();
		if (str.isEmpty()) {
			return null;
		}
		return str;
	}

	public static JwtConfig getTheAtomicService(String builderConfigId,
			ConcurrentServiceReferenceMap<String, JwtConfig> jwtServiceMapRef) {
		Iterator<JwtConfig> jwtServices = jwtServiceMapRef.getServices();
		while (jwtServices.hasNext()) {
			JwtConfig jwtService = jwtServices.next();
			if (builderConfigId.equals(jwtService.getId())) {
				return jwtService;
			}
		}
		return null;

	}

	public static JwtConfig getTheService(String builderConfigId,
			ConcurrentServiceReferenceMap<String, JwtConfig> jwtServiceMapRef) {
		Iterator<JwtConfig> jwtServices = jwtServiceMapRef.getServices();
		while (jwtServices.hasNext()) {
			JwtConfig jwtService = jwtServices.next();
			if (builderConfigId.equals(jwtService.getId())) {
				return jwtService;
			}
		}
		return null;

	}

	public static String[] splitTokenString(String tokenString) {
		boolean isPlainTextJWT = false;
		if (tokenString.endsWith(".")) {
			isPlainTextJWT = true;
		}
		String[] pieces = tokenString.split(Pattern.quote(DELIMITER));
		if (!isPlainTextJWT && pieces.length != 3) {
			// Tr.warning("Expected JWT to have 3 segments separated by '" +
			// DELIMITER + "', but it has " + pieces.length + " segments");
			return null;
		}
		return pieces;
	}

	public static String getPayload(String jwt) {
		String[] jwtInfo = splitTokenString(jwt);
		if (jwtInfo != null) {
			return jwtInfo[1];
		}
		return null;
	}

	public static String getRandom(int length) {
		StringBuffer result = new StringBuffer(length);
		final char[] chars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
				'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
				'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
				'v', 'w', 'x', 'y', 'z' };
		Random r = getRandom();

		for (int i = 0; i < length; i++) {
			int n = r.nextInt(62);
			result.append(chars[n]);
		}

		return result.toString();
	}

	static Random getRandom() {
		Random result = null;
		try {
			if (Security.getProvider(JCEPROVIDER_IBM) != null) {
				result = SecureRandom.getInstance(SECRANDOM_IBM);
			} else {
				result = SecureRandom.getInstance(SECRANDOM_SHA1PRNG);
			}
		} catch (Exception e) {
			result = new Random();
		}
		return result;
	}

	public static long calculate(long valid) {
		// TODO Auto-generated method stub
		long lifetimeSeconds = valid * 60 * 60;
		long timeInSeconds = System.currentTimeMillis() / 1000;

		return timeInSeconds + lifetimeSeconds;
	}

	public static String toJson(Map<String, Object> claimsMap) {
		// TODO Auto-generated method stub
		return org.jose4j.json.JsonUtil.toJson(claimsMap);
	}

	public static void setVMMService(AtomicServiceReference<VMMService> vmmServiceRef) {
		JwtUtils.vmmServiceRef = vmmServiceRef;
	}

	public static VMMService getVMMService() {
		return vmmServiceRef.getService();
	}

	public static Object fetch(String claim, String subject) throws Exception {
		RegistryClaims claims = new RegistryClaims(subject);
		return claims.fetchClaim(claim);
	}

	public static void setSSLSupportService(AtomicServiceReference<SSLSupport> sslSupportRef) {
		// TODO Auto-generated method stub
		JwtUtils.sslSupportRef = sslSupportRef;
	}

	public static SSLSupport getSSLSupportService() {
		// TODO Auto-generated method stub
		return (sslSupportRef.getService());
	}

	public static void setKeyStoreService(AtomicServiceReference<KeyStoreService> kssRef) {
		// TODO Auto-generated method stub
		JwtUtils.keyStoreServiceRef = kssRef;
	}

	public static void setKeyStoreService2(ConcurrentServiceReferenceMap<String, KeyStoreService> kssMapRef) {
		// TODO Auto-generated method stub
		JwtUtils.keyStoreServiceMapRef = kssMapRef;
	}

	public static KeyStoreService getKeyStoreService() {
		if (keyStoreServiceRef != null) {
			return keyStoreServiceRef.getService();
		}
		return null;
	}

	public static String getDefaultKeyStoreName(String propKey) {
		String keyStoreName = null;
		// config does not specify keystore, so try to get one from servers
		// default ssl config.
		SSLSupport sslSupport = getSSLSupportService();
		JSSEHelper helper = null;
		if (sslSupport != null) {
			helper = sslSupport.getJSSEHelper();
		}
		Properties props = null;
		final JSSEHelper jsseHelper = helper;
		final Map<String, Object> connectionInfo = new HashMap<String, Object>();
		connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
		if (jsseHelper != null) {
			try {
				// props = jsseHelper.getProperties("", null, null, true);
				props = (Properties) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws Exception {
						return jsseHelper.getProperties("", connectionInfo, null, true);
					}
				});

			} catch (PrivilegedActionException pae) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Exception getting properties from jssehelper!!!");
				}
				// throw (SSLException) pae.getCause();
			}

			if (props != null) {
				keyStoreName = props.getProperty(propKey);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "KeyStore name from default ssl config = " + keyStoreName);
				}
			}
		}
		return keyStoreName;
	}

	@Sensitive
	public static PrivateKey getPrivateKey(String keyAlias, String keyStoreRef)
			throws KeyStoreException, CertificateException {
		return getPrivateKey(keyAlias, keyStoreRef, getKeyStoreService());
	}

	@Sensitive
	public static PrivateKey getPrivateKey(String keyAlias, String keyStoreRef, KeyStoreService kss)
			throws KeyStoreException, CertificateException {
		if (kss != null) {
			if (keyStoreRef == null) {
				keyStoreRef = getDefaultKeyStoreName("com.ibm.ssl.keyStoreName");
			}
			if (keyStoreRef != null) {
				if (keyAlias != null) {
					return kss.getPrivateKeyFromKeyStore(keyStoreRef, keyAlias, null);
				} else {
					return kss.getPrivateKeyFromKeyStore(keyStoreRef);
				}
			}
		}
		return null;
	}

	public static PublicKey getPublicKey(String keyAlias, String trustStoreRef)
			throws KeyStoreException, CertificateException, InvalidTokenException {
		return getPublicKey(keyAlias, trustStoreRef, getKeyStoreService());
	}

	public static PublicKey getPublicKey(String keyAlias, String trustStoreRef, KeyStoreService kss)
			throws KeyStoreException, CertificateException, InvalidTokenException {
		if (kss == null) {
			return null;
		}
		if (trustStoreRef == null) {
			trustStoreRef = getDefaultKeyStoreName("com.ibm.ssl.trustStoreName");
			if (trustStoreRef == null) {
				return null;
			}
		}
		if (keyAlias != null) {
			X509Certificate cert = kss.getX509CertificateFromKeyStore(trustStoreRef, keyAlias);
			if (cert == null) {
				return null;
			}
			return cert.getPublicKey();
		} else {
			Collection<String> aliases = kss.getTrustedCertEntriesInKeyStore(trustStoreRef);
			// check for NO aliases in the trust store
			if (aliases == null || aliases.size() == 0) {
				X509Certificate cert = kss.getX509CertificateFromKeyStore(trustStoreRef);

				if (cert != null) {
					return cert.getPublicKey();
				}
				String errorMsg = Tr.formatMessage(tc, "JWT_SIGNER_CERT_NOT_AVAILABLE");
				throw new InvalidTokenException(errorMsg);

			}
			// check for more than 1 alias in the trust store (with more than 1,
			// we need to have one key/cert pair available or trustAlias
			// specified (this is part of the no
			// trustAlais path))
			if (aliases.size() > 1) {
				X509Certificate cert = kss.getX509CertificateFromKeyStore(trustStoreRef);

				if (cert != null) {
					return cert.getPublicKey();
				}
				String errorMsg = Tr.formatMessage(tc, "JWT_SIGNER_CERT_AMBIGUOUS");
				throw new InvalidTokenException(errorMsg);

			}
			// We now have 1 alias, we'll get the cert
			String alias = aliases.iterator().next();
			X509Certificate cert = kss.getX509CertificateFromKeyStore(trustStoreRef, alias);
			// if NO cert, fail, otherwise get and return the public key
			if (cert != null) {
				return cert.getPublicKey();
			} else {
				String errorMsg = Tr.formatMessage(tc, "JWT_SIGNER_CERT_NOT_AVAILABLE");
				throw new InvalidTokenException(errorMsg);

			}
		}

	}

	public static String getDate(long current) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date resultdate = new Date(current);
		return sdf.format(resultdate);
	}

	@Sensitive
	public static String processProtectedString(Map<String, Object> props, String cfgKey) {
		return JwtConfigUtil.processProtectedString(props, cfgKey);
	}

}
