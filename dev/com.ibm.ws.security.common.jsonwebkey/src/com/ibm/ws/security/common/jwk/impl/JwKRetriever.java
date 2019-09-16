/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
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
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
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

    final static String PEM_BEGIN_TOKEN = "-----BEGIN";
    final static String PEM_END_TOKEN = "--END--";
    final static String JWKS = "keys";
    final static String JSON_START = "{";

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

    String jwkClientId = null;

    String jwkClientSecret = null;

    String keyLocation = null;
    String publicKeyText = null;
    String locationUsed = null;

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
    public JwKRetriever(String configId, String sslConfigurationName, String jwkEndpointUrl, JWKSet jwkSet, SSLSupport sslSupport, boolean hnvEnabled, String jwkClientId, @Sensitive String jwkClientSecret) {
        this.configId = configId;
        this.sslConfigurationName = sslConfigurationName;
        this.jwkEndpointUrl = jwkEndpointUrl;
        this.jwkSet = jwkSet; // get the JWKSet from the Config
        this.sslSupport = sslSupport;
        this.hostNameVerificationEnabled = hnvEnabled;
        this.jwkClientId = jwkClientId;
        this.jwkClientSecret = jwkClientSecret;
    }

    public JwKRetriever(String configId, String sslConfigurationName, String jwkEndpointUrl, JWKSet jwkSet, SSLSupport sslSupport, boolean hnvEnabled, String jwkClientId, @Sensitive String jwkClientSecret,
            String publicKeyText, String keyLocation) {
        this.configId = configId;
        this.sslConfigurationName = sslConfigurationName;
        this.jwkEndpointUrl = jwkEndpointUrl;
        this.jwkSet = jwkSet; // get the JWKSet from the Config
        this.sslSupport = sslSupport;
        this.hostNameVerificationEnabled = hnvEnabled;
        this.jwkClientId = jwkClientId;
        this.jwkClientSecret = jwkClientSecret;
        this.publicKeyText = publicKeyText;
        this.keyLocation = keyLocation;
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
    @FFDCIgnore({ KeyStoreException.class })
    public PublicKey getPublicKeyFromJwk(String kid, String x5t, String use, boolean useSystemPropertiesForHttpClientConnections)
            throws PrivilegedActionException, IOException, KeyStoreException, InterruptedException {
        PublicKey key = null;
        KeyStoreException errKeyStoreException = null;
        InterruptedException errInterruptedException = null;

        boolean isHttp = remoteHttpCall(this.jwkEndpointUrl, this.publicKeyText, this.keyLocation);
        try {
            if (isHttp) {
                key = this.getJwkRemote(kid, x5t, use , useSystemPropertiesForHttpClientConnections);
            } else {
                key = this.getJwkLocal(kid, x5t, publicKeyText, keyLocation, use);
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

    protected PublicKey getJwkCache(String kid, String x5t) {
        if (kid != null) {
            return jwkSet.getPublicKeyByKid(kid);
        } else if (x5t != null) {
            return jwkSet.getPublicKeyByx5t(x5t);
        }
        return jwkSet.getPublicKeyByKid(null);
    }

    private PublicKey getJwkFromJWKSet(String setId, String kid, String x5t, String use) {
        if (kid != null) {
            return jwkSet.getPublicKeyBySetIdAndKid(setId, kid);
        } else if (x5t != null) {
            return jwkSet.getPublicKeyBySetIdAndx5t(setId, x5t);
        } else if (use != null) {
            return jwkSet.getPublicKeyBySetIdAndUse(setId, use);
        }
        return jwkSet.getPublicKeyBySetId(setId);
    }

    protected boolean remoteHttpCall(String jwksUri, String publicKeyText, String keyLocation) {
        boolean isHttp = true;
        if (jwksUri == null) {
            if (publicKeyText != null) {
                isHttp = false;
            } else if (keyLocation != null && !keyLocation.startsWith("http")) {
                isHttp = false;
            }
        }
        return isHttp;
    }
    @FFDCIgnore({  Exception.class })
    protected PublicKey getPublicKeyFromFile(String location, String kid, String x5t, String use) {
        PublicKey publicKey = null;
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
                publicKey = getJwkFromJWKSet(fileSystemCacheSelector, kid, x5t, use);  // try the cache.
                if (publicKey == null) {                    
                    publicKey = getJwkFromJWKSet(classLoadingCacheSelector, kid, x5t, use);  
                }
                if (publicKey == null) {  // cache miss, read the jwk if we can,  &  update locationUsed
                    InputStream is = getInputStream(jwkFile, fileSystemCacheSelector,  location, classLoadingCacheSelector);  
                    if(is != null) {
                        keyString = getKeyAsString(is);
                        parseJwk(keyString, null, jwkSet, sigAlg); // also adds entry to cache.
                        publicKey = getJwkFromJWKSet(locationUsed, kid, x5t, use);
                    }
                }
            }
            
        } catch (Exception e2) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception opening file from location [" + location + "]: " + e2.getMessage());
            }
        }
        return publicKey;
    }
    
    /**
     * open an input stream to either a file on the file system or a url on the classpath.
     * Update the locationUsed class variable to note where we got the stream from so results of reading it can be cached properly
     *
     */
    @FFDCIgnore({ PrivilegedActionException.class })
    protected InputStream getInputStream(final File f, String fileSystemSelector,  String location, String classLoadingSelector ) throws IOException {      
        // check file system first like we used to do
        if (f != null) {
            InputStream is = null;
            try { 
                is = (FileInputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
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
                    Tr.debug(tc, "input stream obtained from file system and locationUsed set to: "+ locationUsed);
                }
                return is;
            }
        }        
        // do the expensive classpath search
        // performant: we're avoiding calling getResource if entry was previously cached.
        URL u = Thread.currentThread().getContextClassLoader().getResource(location);  
        locationUsed = classLoadingSelector;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "input stream obtained from classloader and  locationUsed set to: "+ locationUsed);
        }
        if (u != null) {            
            return u.openStream();
        }
        return null;
    }

    protected PublicKey getJwkLocal(String kid, String x5t, String publicKeyText, String location, String use) {
        if (publicKeyText == null && location != null) {
            return getPublicKeyFromFile(location, kid, x5t, use);
        }

        if (publicKeyText != null) {
            synchronized (jwkSet) {
                PublicKey publicKey = getJwkFromJWKSet(publicKeyText, kid, x5t, use);
                if (publicKey == null) {
                    parseJwk(publicKeyText, null, jwkSet, sigAlg);
                    publicKey = getJwkFromJWKSet(publicKeyText, kid, x5t, use);
                }
                return publicKey;
            }
        }
        return null;
    }

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

    protected boolean isPEM(String key) {
        if (key != null && key.startsWith(PEM_BEGIN_TOKEN)) {
            return true;
        }
        return false;

    }

    @FFDCIgnore({ KeyStoreException.class })
    protected PublicKey getJwkRemote(String kid, String x5t, String use, boolean useSystemPropertiesForHttpClientConnections) throws KeyStoreException, InterruptedException {
        locationUsed = jwkEndpointUrl;
        if (locationUsed == null) {
            locationUsed = keyLocation;
        }
        if (locationUsed == null || !locationUsed.startsWith("http")) {
            return null;
        }
        PublicKey key = null;
        try {
            synchronized (jwkSet) {
                key = getJwkFromJWKSet(locationUsed, kid, x5t, use);
                if (key == null) {
                    key = doJwkRemote(kid, x5t, use, useSystemPropertiesForHttpClientConnections);
                }
            }
        } catch (KeyStoreException e) {
            throw e;
        }
        return key;
    }

    @FFDCIgnore({ Exception.class, KeyStoreException.class })
    protected PublicKey doJwkRemote(String kid, String x5t, String use, boolean useSystemPropertiesForHttpClientConnections) throws KeyStoreException {

        String jsonString = null;
        locationUsed = jwkEndpointUrl;
        if (locationUsed == null) {
            locationUsed = keyLocation;
        }

        try {
            // TODO - validate url
            SSLSocketFactory sslSocketFactory = getSSLSocketFactory(locationUsed, sslConfigurationName, sslSupport);            
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

        return getJwkFromJWKSet(locationUsed, kid, x5t, use);
    }

    // separate to be an independent method for unit tests
    public boolean parseJwk(String keyText, FileInputStream inputStream, JWKSet jwkset, String signatureAlgorithm) {
        boolean bJwk = false;

        if (keyText != null) {
            bJwk = parseKeyText(keyText, locationUsed, jwkset, signatureAlgorithm);
        } else if (inputStream != null) {
            String keyAsString = getKeyAsString(inputStream);
            bJwk = parseKeyText(keyAsString, locationUsed, jwkset, signatureAlgorithm);
        }

        return bJwk;
    }

    protected boolean parseKeyText(String keyText, String location, JWKSet jwkset, String signatureAlgorithm) {
        Set<JWK> jwks = new HashSet<JWK>();
        JWK jwk = null;

        if (isPEM(keyText) && "RS256".equals(signatureAlgorithm)) {
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
            if (location != null) {
                jwkSet.add(location, aJwk);
            } else {
                jwkSet.add(keyText, aJwk);
            }
        }

        return !jwks.isEmpty();
    }

    @FFDCIgnore(Exception.class)
    private JWK parsePEMFormat(String keyText, String signatureAlgorithm) {
        Jose4jRsaJWK jwk = null;

        try {
            RSAPublicKey pubKey = (RSAPublicKey) PemKeyUtil.getPublicKey(keyText);
            jwk = new Jose4jRsaJWK(pubKey);
            jwk.setAlgorithm(signatureAlgorithm);
            jwk.setUse(JwkConstants.sig);
        } catch (Exception e) {
        }

        return jwk;
    }

    private JWK parseJwkFormat(JSONObject jsonObject, String signatureAlgorithm) {
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

    private Set<JWK> parseJwksFormat(JSONObject jsonObject, String signatureAlgorithm) {
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

    @FFDCIgnore(Exception.class)
    JSONObject parseJsonObject(String jsonString) {
        JSONObject jsonObject = null;
        try {
            if (!jsonString.startsWith(JSON_START)) { //convert Base64 encoded String to JSON string               
                // jsonString=new String (Base64.getDecoder().decode(jsonString), "UTF-8");
                jsonString = new String(Base64.decodeBase64(jsonString), "UTF-8");
            }
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
    
    protected HttpClientBuilder getBuilder(boolean useSystemPropertiesForHttpClientConnections)
    {        
        return useSystemPropertiesForHttpClientConnections ? HttpClientBuilder.create().useSystemProperties() : HttpClientBuilder.create();
    }

    private HttpClient createHttpClient(boolean isSecure, boolean isHostnameVerification, SSLSocketFactory sslSocketFactory, boolean addBasicAuthHeader, BasicCredentialsProvider credentialsProvider, boolean useSystemPropertiesForHttpClientConnections) {       
        HttpClient client = null;
        if (isSecure) {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new AllowAllHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new StrictHostnameVerifier());
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