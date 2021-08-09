/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;

// import java.util.Base64; // or could use
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.crypto.KeyAlgorithmChecker;
import com.ibm.ws.security.common.jwk.impl.PemKeyUtil.KeyType;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.common.jwk.internal.JwkConstants;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 *
 */
public class JwKRetriever {
    private static final TraceComponent tc = Tr.register(JwKRetriever.class);

    final static String PEM_BEGIN_TOKEN = "-----BEGIN";
    final static String PEM_END_TOKEN = "--END--";
    final static String JWKS = "keys";
    final static String JSON_START = "{";

    public enum JwkKeyType {
        PUBLIC, PRIVATE
    }

    String configId = null;
    String sslConfigurationName = null;
    String jwkEndpointUrl = null; // jwksUri

    String sigAlg = null;
    JWKSet jwkSet = null; // using the JWKSet from the JwtConsumerConfig. Do not create it every time
    SSLSupport sslSupport = null;//JwtUtils.getSSLSupportService();

    String keyFileName = null;

    boolean hostNameVerificationEnabled = true;

    String jwkClientId = null;

    String jwkClientSecret = null;

    @Sensitive
    String keyLocation = null;
    @Sensitive
    String keyText = null;
    String locationUsed = null;

    KeyAlgorithmChecker keyAlgChecker = new KeyAlgorithmChecker();

    public JwKRetriever(JWKSet jwkSet) {
        this.jwkSet = jwkSet;
    }

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
    public JwKRetriever(String configId, String sslConfigurationName, String jwkEndpointUrl, JWKSet jwkSet, SSLSupport sslSupport, boolean hnvEnabled, String jwkClientId, @Sensitive String jwkClientSecret, String signatureAlgorithm) {
        this(configId, sslConfigurationName, jwkEndpointUrl, jwkSet, sslSupport, hnvEnabled, jwkClientId, jwkClientSecret, signatureAlgorithm, null, null);
    }

    public JwKRetriever(String configId, String sslConfigurationName, String jwkEndpointUrl, JWKSet jwkSet, SSLSupport sslSupport, boolean hnvEnabled, String jwkClientId, @Sensitive String jwkClientSecret,
            String signatureAlgorithm, @Sensitive String keyText, @Sensitive String keyLocation) {
        this.configId = configId;
        this.sslConfigurationName = sslConfigurationName;
        this.jwkEndpointUrl = jwkEndpointUrl;
        this.jwkSet = jwkSet; // get the JWKSet from the Config
        this.sslSupport = sslSupport;
        this.hostNameVerificationEnabled = hnvEnabled;
        this.jwkClientId = jwkClientId;
        this.jwkClientSecret = jwkClientSecret;
        this.sigAlg = signatureAlgorithm;
        this.keyText = keyText;
        this.keyLocation = keyLocation;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.sigAlg = signatureAlgorithm;
    }

    public void setKeyText(@Sensitive String keyText) {
        this.keyText = keyText;
    }

    public void setKeyLocation(@Sensitive String keyLocation) {
        this.keyLocation = keyLocation;
    }

    @Sensitive
    public PrivateKey getPrivateKeyFromJwk(String kid, boolean useSystemPropertiesForHttpClientConnections)
            throws PrivilegedActionException, IOException, KeyStoreException, InterruptedException {
        return (PrivateKey) getKeyFromJwk(kid, null, null, useSystemPropertiesForHttpClientConnections, JwkKeyType.PRIVATE);
    }

    /**
     * Either kid or x5t will work. But not both
     */
    public PublicKey getPublicKeyFromJwk(String kid, String x5t, boolean useSystemPropertiesForHttpClientConnections)
            throws PrivilegedActionException, IOException, KeyStoreException, InterruptedException {
        return getPublicKeyFromJwk(kid, x5t, null, useSystemPropertiesForHttpClientConnections);
    }

    /**
     * Either kid, x5t, or use will work, but not all
     */
    public PublicKey getPublicKeyFromJwk(String kid, String x5t, String use, boolean useSystemPropertiesForHttpClientConnections)
            throws PrivilegedActionException, IOException, KeyStoreException, InterruptedException {
        return (PublicKey) getKeyFromJwk(kid, x5t, use, useSystemPropertiesForHttpClientConnections, JwkKeyType.PUBLIC);
    }

    /**
     * Either kid, x5t, or use will work, but not all
     */
    @Sensitive
    @FFDCIgnore({ KeyStoreException.class })
    Key getKeyFromJwk(String kid, String x5t, String use, boolean useSystemPropertiesForHttpClientConnections, JwkKeyType keyType)
            throws PrivilegedActionException, IOException, KeyStoreException, InterruptedException {
        Key key = null;
        KeyStoreException errKeyStoreException = null;
        InterruptedException errInterruptedException = null;

        boolean isHttp = remoteHttpCall(this.jwkEndpointUrl, keyText, this.keyLocation);
        try {
            if (isHttp) {
                key = this.getJwkRemote(kid, x5t, use, useSystemPropertiesForHttpClientConnections, keyType);
            } else {
                key = this.getJwkLocal(kid, x5t, keyText, keyLocation, use, keyType);
            }
        } catch (KeyStoreException e) {
            errKeyStoreException = e;
        } catch (InterruptedException e) {
            errInterruptedException = e;
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

    @Sensitive
    private Key getJwkFromJWKSet(@Sensitive String setId, String kid, String x5t, String use, @Sensitive String keyText, JwkKeyType keyType) {
        boolean isKeyIdentifierUsed = (kid != null || x5t != null || use != null);
        Key key = null;
        if (kid != null) {
            key = jwkSet.getKeyBySetIdAndKid(setId, kid, keyType);
        } else if (x5t != null) {
            key = jwkSet.getKeyBySetIdAndx5t(setId, x5t, keyType);
        } else if (use != null) {
            key = jwkSet.getKeyBySetIdAndUse(setId, use, keyType);
        }
        if (key != null) {
            return key;
        }
        if (keyText != null) {
            key = jwkSet.getKeyBySetIdAndKeyText(setId, keyText, keyType);
        }
        if (key == null && !isKeyIdentifierUsed) {
            key = jwkSet.getKeyBySetId(setId, keyType);
        }
        return key;
    }

    protected boolean remoteHttpCall(String jwksUri, @Sensitive String keyText, @Sensitive String keyLocation) {
        boolean isHttp = true;
        if (jwksUri == null) {
            if (keyText != null) {
                isHttp = false;
            } else if (keyLocation != null && !keyLocation.startsWith("http")) {
                isHttp = false;
            }
        }
        return isHttp;
    }

    @Sensitive
    @FFDCIgnore({ Exception.class })
    protected Key getKeyFromFile(@Sensitive String location, String kid, String x5t, String use, JwkKeyType keyType) {
        Key key = null;
        String keyString = null;
        String classLoadingCacheSelector = null;
        String fileSystemCacheSelector = null;

        File jwkFile = null;
        try {
            // figure out which cache to use for jwk from classloading
            classLoadingCacheSelector = Thread.currentThread().getContextClassLoader().toString() + location;
            //figure out which cache to use for jwk from file system
            final String keyFile;
            if (location.startsWith("file:")) {
                URI uri = new URI(location);
                keyFile = uri.getPath();
            } else {
                keyFile = location;
            }
            jwkFile = new File(keyFile);
            fileSystemCacheSelector = jwkFile.getCanonicalPath();

            synchronized (jwkSet) {
                key = getJwkFromJWKSet(fileSystemCacheSelector, kid, x5t, use, null, keyType); // try the cache.
                if (key == null) {
                    key = getJwkFromJWKSet(classLoadingCacheSelector, kid, x5t, use, null, keyType);
                }
                if (key == null) { // cache miss, read the jwk if we can,  &  update locationUsed
                    InputStream is = null;
                    try {
                        is = getInputStream(jwkFile, fileSystemCacheSelector, location, classLoadingCacheSelector);
                        if (is != null) {
                            keyString = getKeyAsString(is);
                            parseJwk(keyString, null, jwkSet, sigAlg); // also adds entry to cache.
                            key = getJwkFromJWKSet(locationUsed, kid, x5t, use, keyString, keyType);
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception opening file from location [" + location + "]: " + e);
            }
        }
        return key;
    }

    /**
     * open an input stream to either a file on the file system or a url on the classpath.
     * Update the locationUsed class variable to note where we got the stream from so results of reading it can be cached properly
     *
     */
    @FFDCIgnore({ PrivilegedActionException.class })
    protected InputStream getInputStream(@Sensitive final File f, @Sensitive String fileSystemSelector, @Sensitive String location, @Sensitive String classLoadingSelector) throws IOException {
        // check file system first like we used to do
        if (f != null) {
            InputStream is = null;
            try {
                // TODO - this may still trace a private key if the file specified is actually an inlined key
                is = (FileInputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Sensitive
                    @Override
                    public Object run() throws Exception {
                        if (f.exists()) {
                            return new FileInputStream(f);
                        } else {
                            return null;
                        }
                    }
                });

            } catch (PrivilegedActionException e1) {
            }
            if (is != null) {
                locationUsed = fileSystemSelector;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "input stream obtained from file system and locationUsed set to: " + getSafeTraceableString(locationUsed));
                }
                return is;
            }
        }
        // do the expensive classpath search
        // performant: we're avoiding calling getResource if entry was previously cached.
        URL u = Thread.currentThread().getContextClassLoader().getResource(location);
        locationUsed = classLoadingSelector;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "input stream obtained from classloader and  locationUsed set to: " + getSafeTraceableString(locationUsed));
        }
        if (u != null) {
            return u.openStream();
        }
        return null;
    }

    /**
     * Returns a truncated string that is safe to trace if the input string includes a PEM key.
     */
    @Trivial
    private String getSafeTraceableString(@Sensitive String input) {
        if (input == null || !input.contains(PEM_BEGIN_TOKEN)) {
            return input;
        }
        return locationUsed.substring(0, locationUsed.indexOf(PEM_BEGIN_TOKEN));
    }

    @Sensitive
    protected Key getJwkLocal(String kid, String x5t, @Sensitive String keyText, @Sensitive String location, String use, JwkKeyType keyType) {
        if (keyText == null && location != null) {
            return getKeyFromFile(location, kid, x5t, use, keyType);
        }

        if (keyText != null) {
            synchronized (jwkSet) {
                Key key = getJwkFromJWKSet(keyText, kid, x5t, use, keyText, keyType);
                if (key == null) {
                    parseJwk(keyText, null, jwkSet, sigAlg);
                    key = getJwkFromJWKSet(keyText, kid, x5t, use, keyText, keyType);
                }
                return key;
            }
        }
        return null;
    }

    @Sensitive
    protected String getKeyAsString(InputStream fis) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStreamReader r = new InputStreamReader(fis, "UTF-8");
            int ch = r.read();
            while (ch >= 0) {
                sb.append((char) ch);
                ch = r.read();
            }
        } catch (UnsupportedEncodingException UEE) {

        } catch (IOException ioe) {
        }
        return sb.toString();
    }

    protected boolean isPEM(@Sensitive String key) {
        if (key != null && key.startsWith(PEM_BEGIN_TOKEN)) {
            return true;
        }
        return false;

    }

    @Sensitive
    @FFDCIgnore({ KeyStoreException.class })
    protected Key getJwkRemote(String kid, String x5t, String use, boolean useSystemPropertiesForHttpClientConnections, JwkKeyType keyType) throws KeyStoreException, InterruptedException {
        locationUsed = jwkEndpointUrl;
        if (locationUsed == null) {
            locationUsed = keyLocation;
        }
        if (locationUsed == null || !locationUsed.startsWith("http")) {
            return null;
        }
        Key key = null;
        try {
            synchronized (jwkSet) {
                key = getJwkFromJWKSet(locationUsed, kid, x5t, use, null, keyType);
                if (key == null) {
                    key = doJwkRemote(kid, x5t, use, useSystemPropertiesForHttpClientConnections, keyType);
                }
            }
        } catch (KeyStoreException e) {
            throw e;
        }
        return key;
    }

    @FFDCIgnore({ Exception.class, KeyStoreException.class })
    protected Key doJwkRemote(String kid, String x5t, String use, boolean useSystemPropertiesForHttpClientConnections, JwkKeyType keyType) throws KeyStoreException {

        String jsonString = null;
        locationUsed = jwkEndpointUrl;
        if (locationUsed == null) {
            locationUsed = keyLocation;
        }

        try {
            // TODO - validate url
            SSLSocketFactory sslSocketFactory = null;
            if (locationUsed != null && locationUsed.toLowerCase().startsWith("https")) {
                sslSocketFactory = getSSLSocketFactory(locationUsed, sslConfigurationName, sslSupport);
            }
            HttpClient client = createHTTPClient(sslSocketFactory, locationUsed, hostNameVerificationEnabled, useSystemPropertiesForHttpClientConnections);
            jsonString = getHTTPRequestAsString(client, locationUsed);
            boolean bJwk = parseJwk(jsonString, null, jwkSet, sigAlg);

            if (!bJwk) {
                // can not get back any JWK from OP
                // since getJwkLocal will be called later and NO key exception
                // will be handled in the parent callers
                // debug here only
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "No JWK can be found through '" + locationUsed + "'");
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

        return getJwkFromJWKSet(locationUsed, kid, x5t, use, jsonString, keyType);
    }

    // separate to be an independent method for unit tests
    public boolean parseJwk(@Sensitive String keyText, FileInputStream inputStream, JWKSet jwkset, String signatureAlgorithm) {
        boolean bJwk = false;

        if (keyText != null) {
            bJwk = parseKeyText(keyText, locationUsed, jwkset, signatureAlgorithm);
        } else if (inputStream != null) {
            String keyAsString = getKeyAsString(inputStream);
            bJwk = parseKeyText(keyAsString, locationUsed, jwkset, signatureAlgorithm);
        }

        return bJwk;
    }

    protected boolean parseKeyText(@Sensitive String keyText, String location, JWKSet jwkset, String signatureAlgorithm) {
        Set<JWK> jwks = new HashSet<JWK>();
        JWK jwk = null;

        if (isPEM(keyText) && isPemSupportedAlgorithm(signatureAlgorithm)) {
            jwk = parsePEMFormat(keyText, signatureAlgorithm);
        } else {
            JSONObject jsonObject = parseJsonObject(keyText);
            if (jsonObject != null) {
                jwk = parseJwkFormat(jsonObject, signatureAlgorithm);
                if (jwk == null && jsonObject.containsKey(JWKS)) {
                    jwks.addAll(parseJwksFormat(jsonObject, signatureAlgorithm));
                }
            }
        }

        if (jwk != null) {
            jwks.add(jwk);
        }

        for (JWK aJwk : jwks) {
            if (isPEM(keyText)) {
                jwkSet.addPemKey(location, keyText, jwk);
            }
            if (location != null) {
                jwkSet.add(location, aJwk);
            }
            if (keyText != null) {
                jwkSet.add(keyText, aJwk);
            }
        }

        return !jwks.isEmpty();
    }

    boolean isPemSupportedAlgorithm(String signatureAlgorithm) {
        return keyAlgChecker.isRSAlgorithm(signatureAlgorithm) || keyAlgChecker.isESAlgorithm(signatureAlgorithm);
    }

    @Sensitive
    @FFDCIgnore(Exception.class)
    private JWK parsePEMFormat(@Sensitive String keyText, String signatureAlgorithm) {
        Jose4jRsaJWK jwk = null;
        try {
            KeyType keyType = PemKeyUtil.getKeyType(keyText);
            if (isPublicKeyJwk(keyType)) {
                return parsePublicKeyJwk(keyText, signatureAlgorithm);
            } else if (keyType == KeyType.PRIVATE) {
                return parsePrivateKeyJwk(keyText, signatureAlgorithm);
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception parsing PEM file: " + e);
            }
        }
        return jwk;
    }

    boolean isPublicKeyJwk(KeyType keyType) {
        return keyType == KeyType.RSA_PUBLIC || keyType == KeyType.EC_PUBLIC || keyType == KeyType.PUBLIC || keyType == KeyType.UNKNOWN;
    }

    JWK parsePublicKeyJwk(String keyText, String signatureAlgorithm) throws Exception {
        PublicKey pubKey = PemKeyUtil.getPublicKey(keyText);
        if (keyAlgChecker.isESAlgorithm(signatureAlgorithm)) {
            return getEcJwkPublicKey(pubKey, signatureAlgorithm);
        } else {
            return getRsaJwkPublicKey(pubKey, signatureAlgorithm);
        }
    }

    @Sensitive
    JWK parsePrivateKeyJwk(@Sensitive String keyText, String signatureAlgorithm) throws Exception {
        PrivateKey privateKey = PemKeyUtil.getPrivateKey(keyText);
        if (keyAlgChecker.isESAlgorithm(signatureAlgorithm)) {
            return getEcJwkPrivateKey(privateKey, signatureAlgorithm);
        } else {
            return getRsaJwkPrivateKey(privateKey, signatureAlgorithm);
        }
    }

    @FFDCIgnore(Exception.class)
    private Jose4jEllipticCurveJWK getEcJwkPublicKey(PublicKey publicKey, String signatureAlgorithm) {
        if (!(publicKey instanceof ECPublicKey)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Provided public key was not of type ECPublicKey");
            }
            return null;
        }
        Jose4jEllipticCurveJWK jwk = null;
        try {
            jwk = Jose4jEllipticCurveJWK.getInstance((ECPublicKey) publicKey, signatureAlgorithm, JwkConstants.sig);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception instantiating EC JWK object: " + e);
            }
        }
        return jwk;
    }

    @Sensitive
    @FFDCIgnore(Exception.class)
    private Jose4jEllipticCurveJWK getEcJwkPrivateKey(@Sensitive PrivateKey privateKey, String signatureAlgorithm) {
        if (!(privateKey instanceof ECPrivateKey)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Provided private key was not of type ECPrivateKey");
            }
            return null;
        }
        Jose4jEllipticCurveJWK jwk = null;
        try {
            jwk = Jose4jEllipticCurveJWK.getInstance(null, signatureAlgorithm, JwkConstants.sig);
            jwk.setPrivateKey(privateKey);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception instantiating EC JWK object: " + e);
            }
        }
        return jwk;
    }

    @FFDCIgnore(Exception.class)
    private Jose4jRsaJWK getRsaJwkPublicKey(PublicKey pubKey, String signatureAlgorithm) {
        Jose4jRsaJWK jwk = null;
        try {
            jwk = new Jose4jRsaJWK((RSAPublicKey) pubKey);
            jwk.setAlgorithm(signatureAlgorithm);
            jwk.setUse(JwkConstants.sig);
        } catch (Exception e) {
        }
        return jwk;
    }

    @Sensitive
    @FFDCIgnore(Exception.class)
    private Jose4jRsaJWK getRsaJwkPrivateKey(@Sensitive PrivateKey privateKey, String signatureAlgorithm) {
        Jose4jRsaJWK jwk = null;
        try {
            jwk = Jose4jRsaJWK.getInstance(signatureAlgorithm, null, null, privateKey, null);
        } catch (Exception e) {
        }
        return jwk;
    }

    @Sensitive
    private JWK parseJwkFormat(@Sensitive JSONObject jsonObject, String signatureAlgorithm) {
        JWK jwk = null;

        Object ktyEntry = jsonObject.get("kty");
        if (ktyEntry == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JSON object is missing 'kty' entry");
            }
            return null;
        }
        if (!(ktyEntry instanceof String)) {
            return null;
        }
        String kty = (String) ktyEntry;
        jwk = createJwkBasedOnKty(kty, jsonObject, signatureAlgorithm);
        if (jwk != null) {
            jwk.parse();
        }
        return jwk;
    }

    @Sensitive
    private Set<JWK> parseJwksFormat(@Sensitive JSONObject jsonObject, String signatureAlgorithm) {
        Set<JWK> jwks = Collections.emptySet();
        JSONArray keys = new JSONArray();
        Object keysEntry = jsonObject.get(JWKS);

        if (keysEntry != null) {
            jwks = new HashSet<JWK>();
            keys = parseJsonArray(keysEntry.toString());

            for (Object element : keys) {
                JSONObject jwkJson = parseJsonObject(element.toString());
                if (jwkJson == null) {
                    continue;
                }

                JWK jwk = parseJwkFormat(jwkJson, signatureAlgorithm);
                if (jwk != null) {
                    jwks.add(jwk);
                }
            }
        }

        return jwks;
    }

    @Sensitive
    @FFDCIgnore(Exception.class)
    JSONObject parseJsonObject(@Sensitive String jsonString) {
        JSONObject jsonObject = null;
        try {
            if (!jsonString.startsWith(JSON_START)) { //convert Base64 encoded String to JSON string
                // jsonString=new String (Base64.getDecoder().decode(jsonString), "UTF-8");
                jsonString = new String(Base64.decodeBase64(jsonString), "UTF-8");
            }
            jsonObject = JSONObject.parse(jsonString);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception parsing JSON string [" + jsonString + "]: " + e);
            }
        }
        return jsonObject;
    }

    @Sensitive
    @FFDCIgnore(Exception.class)
    JSONObject parseJsonObject(InputStream is) {
        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.parse(is);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception parsing input stream [" + is.toString() + "]: " + e);
            }
        }
        return jsonObject;
    }

    @Sensitive
    @FFDCIgnore(Exception.class)
    JSONArray parseJsonArray(@Sensitive String jsonString) {
        JSONArray jsonArray = null;
        try {
            jsonArray = JSONArray.parse(jsonString);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception parsing JSON string [" + jsonString + "]: " + e);
            }
        }
        return jsonArray;
    }

    @Sensitive
    JWK createJwkBasedOnKty(String kty, @Sensitive JSONObject keyEntry, String signatureAlgorithm) {
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

    @Sensitive
    JWK getRsaJwk(@Sensitive JSONObject thing) {
        // interop: Azure does not emit the sig.alg attribute, so do not check
        // for it.
        // if (signatureAlgorithm.startsWith("RS")) {// RS256, RS384, RS512
        return Jose4jRsaJWK.getInstance(thing);
    }

    @Sensitive
    JWK getEllipticCurveJwk(@Sensitive JSONObject thing, String signatureAlgorithm) {
        // let get the map<String, Object> from keyObject
        if (signatureAlgorithm != null && signatureAlgorithm.startsWith("ES")) { // ES256, ES384, ES512
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
            throw new SSLException(e);
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

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useSystemPropertiesForHttpClientConnections) {

        HttpClient client = null;
        boolean addBasicAuthHeader = false;

        if (jwkClientId != null && jwkClientSecret != null) {
            addBasicAuthHeader = true;
        }

        BasicCredentialsProvider credentialsProvider = null;
        if (addBasicAuthHeader) {
            credentialsProvider = createCredentialsProvider();
        }

        client = createHttpClient(url.startsWith("https:"), isHostnameVerification, sslSocketFactory, addBasicAuthHeader, credentialsProvider, useSystemPropertiesForHttpClientConnections);
        return client;

    }

    protected HttpClientBuilder getBuilder(boolean useSystemPropertiesForHttpClientConnections) {
        return useSystemPropertiesForHttpClientConnections ? HttpClientBuilder.create().useSystemProperties() : HttpClientBuilder.create();
    }

    private HttpClient createHttpClient(boolean isSecure, boolean isHostnameVerification, SSLSocketFactory sslSocketFactory, boolean addBasicAuthHeader, BasicCredentialsProvider credentialsProvider, boolean useSystemPropertiesForHttpClientConnections) {
        HttpClient client = null;
        if (isSecure) {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new NoopHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new DefaultHostnameVerifier());
            }
            if (addBasicAuthHeader) {
                client = getBuilder(useSystemPropertiesForHttpClientConnections).setDefaultCredentialsProvider(credentialsProvider).setSSLSocketFactory(connectionFactory).build();
            } else {
                client = getBuilder(useSystemPropertiesForHttpClientConnections).setSSLSocketFactory(connectionFactory).build();
            }
        } else {
            if (addBasicAuthHeader) {
                client = getBuilder(useSystemPropertiesForHttpClientConnections).setDefaultCredentialsProvider(credentialsProvider).build();
            } else {
                client = getBuilder(useSystemPropertiesForHttpClientConnections).build();
            }
        }
        return client;
    }

    private BasicCredentialsProvider createCredentialsProvider() {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(jwkClientId, jwkClientSecret));
        return credentialsProvider;
    }
}
