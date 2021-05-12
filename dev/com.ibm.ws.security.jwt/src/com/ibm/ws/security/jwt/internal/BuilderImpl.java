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

package com.ibm.ws.security.jwt.internal;

import java.security.Key;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.lang.JoseException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.jwt.Builder;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidBuilderException;
import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.security.jwt.JwtException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.websphere.security.jwt.KeyException;
import com.ibm.ws.security.common.crypto.KeyAlgorithmChecker;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.utils.Constants;
import com.ibm.ws.security.jwt.utils.IssuerUtils;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.ssl.SSLSupport;

/*
 * @author IBM Corporation
 *
 * @version 1.0
 *
 * @since 1.0
 *
 * @ibm-api
 */
@Component(service = Builder.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", name = "builder")
public class BuilderImpl implements Builder {

    private static final TraceComponent tc = Tr.register(BuilderImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private Claims claims;
    private static boolean active = false;
    private String alg;
    private String sharedKey;
    private Key privateKey;
    private String configId;

    // JWE fields
    private String keyManagementAlg;
    private Key keyManagementKey;
    private String contentEncryptionAlg;

    private final KeyAlgorithmChecker keyAlgChecker = new KeyAlgorithmChecker();

    private final static String DEFAULT_ID = "defaultJWT";
    private final static String KEY_JWT_SERVICE = "jwtConfig";
    private static final String CFG_KEY_ID = "id";

    public static final String DEFAULT_KEY_MANAGEMENT_ALGORITHM = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
    public static final String DEFAULT_CONTENT_ENCRYPTION_ALGORITHM = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;

    private final Object initlock = new Object() {
    };

    private static ConcurrentServiceReferenceMap<String, JwtConfig> jwtServiceMapRef = new ConcurrentServiceReferenceMap<String, JwtConfig>(KEY_JWT_SERVICE);

    private final String KEY_VMM_SERVICE = "vmmService";

    private final AtomicServiceReference<VMMService> vmmServiceRef = new AtomicServiceReference<VMMService>(KEY_VMM_SERVICE);
    public static final String KEY_SSL_SUPPORT = "sslSupport";
    private final AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);
    public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);

    public BuilderImpl() {
    }

    //
    public BuilderImpl(String builderConfigId) throws InvalidBuilderException {
        claims = new ClaimsImpl();
        configId = builderConfigId;
        JwtConfig jwtConfig = getConfig(builderConfigId);
        setClaimsUsingTheConfig(jwtConfig);
    }

    private JwtConfig getConfig(String configId) throws InvalidBuilderException {

        JwtConfig jwtConfig = getTheServiceConfig(configId);
        if (jwtConfig == null) {
            String err = Tr.formatMessage(tc, "JWT_BUILDER_INVALID", new Object[] { configId });
            throw new InvalidBuilderException(err);
        }
        return jwtConfig;
    }

    private void setClaimsUsingTheConfig(JwtConfig jwtConfig) throws InvalidBuilderException {

        // String issuer = jwtConfig.getId();
        // getClaims().put(Claims.ISSUER, issuer);
        String issuer = IssuerUtils.getIssuerUrl(jwtConfig);// jwtConfig.getIssuerUrl();
        if (issuer != null) {
            claims.put(Claims.ISSUER, issuer);
        }
        // token type
        claims.put(Claims.TOKEN_TYPE, "Bearer");

        // long valid = jwtConfig.getValidTime();//
        // getProperty(JwtUtils.CFG_KEY_VALID);
        // long exp = JwtUtils.calculate(valid);
        long exp = -2;
        claims.put(Claims.EXPIRATION, Long.valueOf(exp));

        long iat = -2;
        claims.put(Claims.ISSUED_AT, Long.valueOf(iat));

        boolean isJti = jwtConfig.getJti();// getProperty(JwtUtils.CFG_KEY_JTI);
        if (isJti) {
            String jti = JwtUtils.getRandom(16);
            claims.put(Claims.ID, jti);
        }
        List<String> aud = jwtConfig.getAudiences();// getProperty(AUDIENCE);

        if (aud != null) {
            claims.put(Claims.AUDIENCE, aud);
        }
        String scope = jwtConfig.getScope();
        if (scope != null) {
            claims.put(JwtUtils.SCOPE, scope);
        }
        // if (jwtConfig.getClaims() != null) {
        // Iterator<String> claimsIt = jwtConfig.getClaims().iterator();
        // while (claimsIt.hasNext()) {
        // fetch(claimsIt.next()); // TODO
        // }
        // }

        if (jwtConfig.getSignatureAlgorithm() != null) {
            alg = jwtConfig.getSignatureAlgorithm();
        }

        if (jwtConfig.getSharedKey() != null) {
            sharedKey = jwtConfig.getSharedKey();
        }

        List<String> amrAttr = jwtConfig.getAMRAttributes();// getProperty(amrAttributes);
        if (amrAttr != null) {
            try {
                checkAmrAttrInSubject(amrAttr);
            } catch (Exception e) {
                // TODO Auto-generated catch block
            }
        }
    }

    private JwtConfig getTheServiceConfig(String builderConfigId) {
        return jwtServiceMapRef.getService(builderConfigId);
    }

    @Reference(service = JwtConfig.class, name = KEY_JWT_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.RELUCTANT)
    protected void setJwtConfig(ServiceReference<JwtConfig> ref) {
        synchronized (initlock) {

            jwtServiceMapRef.putReference((String) ref.getProperty(CFG_KEY_ID), ref);
            // System.out.println("@AV999, key id = " + (String)
            // ref.getProperty(CFG_KEY_ID) + ", and the service = " +
            // jwtServiceMapRef.getService((String)
            // ref.getProperty(CFG_KEY_ID)));

        }
    }

    protected void unsetJwtConfig(ServiceReference<JwtConfig> ref) {
        synchronized (initlock) {
            jwtServiceMapRef.removeReference((String) ref.getProperty(CFG_KEY_ID), ref);
        }
    }

    @Reference(service = VMMService.class, name = KEY_VMM_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setVmmService(ServiceReference<VMMService> ref) {
        vmmServiceRef.setReference(ref);
    }

    protected void unsetVmmService(ServiceReference<VMMService> ref) {
        vmmServiceRef.unsetReference(ref);
    }

    @Reference(service = KeyStoreService.class, name = KEY_KEYSTORE_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.setReference(ref);
        //keyStoreServiceMapRef.putReference((String) ref.getProperty(ID), ref);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.unsetReference(ref);
        //keyStoreServiceMapRef.removeReference((String) ref.getProperty(ID), ref);
    }

    @Reference(service = SSLSupport.class, name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
    }

    protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
    }

    protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        jwtServiceMapRef.activate(cc);
        vmmServiceRef.activate(cc);
        keyStoreServiceRef.activate(cc);
        sslSupportRef.activate(cc);
        JwtUtils.setVMMService(vmmServiceRef);
        JwtUtils.setKeyStoreService(keyStoreServiceRef);
        JwtUtils.setSSLSupportService(sslSupportRef);
        active = true;
    }

    @Modified
    protected void modify(Map<String, Object> properties) {

    }

    @Deactivate
    protected void deactivate(int reason, ComponentContext cc) {
        jwtServiceMapRef.deactivate(cc);
        vmmServiceRef.deactivate(cc);
        keyStoreServiceRef.deactivate(cc);
        sslSupportRef.deactivate(cc);
        JwtUtils.setVMMService(null);
        JwtUtils.setKeyStoreService(null);
        JwtUtils.setSSLSupportService(null);
        active = false;
    }

    // Create a builder with default configuration
    @Override
    public Builder create() throws InvalidBuilderException {
        if (!active) {
            // Tr.error(tc, "JWT_BUILDER_NOT_ACTIVE", new Object[] {
            // "defaultJWT" });
            // return null;
            String err = Tr.formatMessage(tc, "JWT_BUILDER_NOT_ACTIVE", new Object[] { "defaultJWT" });
            throw new InvalidBuilderException(err);
        }
        return create("defaultJWT");
    }

    // Create a builder with specified builder
    @Override
    public synchronized Builder create(String builderConfigId) throws InvalidBuilderException {
        if (builderConfigId == null || builderConfigId.isEmpty()) {
            String err = Tr.formatMessage(tc, "JWT_BUILDER_INVALID", new Object[] { builderConfigId });
            throw new InvalidBuilderException(err);
        }
        if (!active) {
            String err = Tr.formatMessage(tc, "JWT_BUILDER_NOT_ACTIVE", new Object[] { builderConfigId });
            throw new InvalidBuilderException(err);
        }

        // Tr.error(tc, "JWT_BUILDER_NOT_ACTIVE", new Object[] { builderConfigId
        // });
        // return null;
        return new BuilderImpl(builderConfigId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#issuer(java.lang.String)
     */
    @Override
    public Builder issuer(String issuerUrl) throws InvalidClaimException {
        if (issuerUrl != null && !issuerUrl.isEmpty()) {
            claims.put(Claims.ISSUER, issuerUrl);
        } else {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR", new Object[] { Claims.ISSUER, issuerUrl });
            throw new InvalidClaimException(err);
        }
        return this;
    }

    // to whom the token is intended to be sent
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#audience(java.util.List)
     */
    @Override
    public Builder audience(List<String> newaudiences) throws InvalidClaimException {
        // this.AUDIENCE
        if (newaudiences != null && !newaudiences.isEmpty()) {

            ArrayList<String> audiences = new ArrayList<String>();
            for (String aud : newaudiences) {
                if (aud != null && !aud.trim().isEmpty() && !audiences.contains(aud)) {
                    audiences.add(aud);
                }
            }
            if (audiences.isEmpty()) {
                String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR", new Object[] { Claims.AUDIENCE, newaudiences });
                throw new InvalidClaimException(err);
            }
            // this.audiences = new ArrayList<String>(audiences);
            claims.put(Claims.AUDIENCE, audiences);
        } else {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR", new Object[] { Claims.AUDIENCE, newaudiences });
            throw new InvalidClaimException(err);
        }
        return this;
    }

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#expirationTime(long)
     */
    @Override
    public Builder expirationTime(long exp) throws InvalidClaimException {

        long currTime = System.currentTimeMillis() / 1000;
        if (exp >= currTime) {
            // if (exp > (new Date()).getTime()) {
            claims.put(Claims.EXPIRATION, Long.valueOf(exp));
        } else {
            // Expiration must be greater than the current time
            String err = Tr.formatMessage(tc, "JWT_INVALID_EXP_CLAIM_ERR", new Object[] { Claims.EXPIRATION, exp, JwtUtils.getDate(exp * 1000), JwtUtils.getDate(currTime * 1000) });
            throw new InvalidClaimException(err);
        }
        return this;
    }

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    private Builder issueTime(long iat) throws InvalidClaimException {

        if (iat > 0) {
            claims.put(Claims.ISSUED_AT, Long.valueOf(iat));
        } else {
            // String msg = "The exp claim must be a positive number.";
            String err = Tr.formatMessage(tc, "JWT_INVALID_TIME_CLAIM_ERR", new Object[] { Claims.ISSUED_AT });
            throw new InvalidClaimException(err);
        }
        return this;
    }

    // generate a unique identifier for the token. The default is "false"
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#jwtId(boolean)
     */
    @Override
    public Builder jwtId(boolean create) {
        if (create) {
            String jti = JwtUtils.getRandom(16);
            claims.put(Claims.ID, jti);
        } else {
            claims.remove(Claims.ID);
        }

        return this;
    }

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#notBefore(long)
     */
    @Override
    public Builder notBefore(long time_from) throws InvalidClaimException {

        if (time_from > 0) {
            claims.put(Claims.NOT_BEFORE, time_from);
        } else {
            String err = Tr.formatMessage(tc, "JWT_INVALID_TIME_CLAIM_ERR", new Object[] { Claims.NOT_BEFORE });
            throw new InvalidClaimException(err);
        }
        return this;
    }

    // the subject/principal is whom the token is about
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#subject(java.lang.String)
     */
    @Override
    public Builder subject(String username) throws InvalidClaimException {
        if (username != null && !username.isEmpty()) {

            claims.put(Claims.SUBJECT, username);
        } else {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR", new Object[] { Claims.SUBJECT, username });
            throw new InvalidClaimException(err);
        }
        return this;
    }

    // private key for token signing
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#signWith(java.lang.String, java.security.Key)
     */
    @Override
    public Builder signWith(String algorithm, Key key) throws KeyException {
        if (!isValidAlgorithmForJavaSecurityKey(algorithm)) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_ALGORITHM_ERR", new Object[] { algorithm, getValidAlgorithmListForJavaSecurityKey() });
            throw new KeyException(err);
        }
        if (!isValidKeyType(key, algorithm)) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_KEY_ERR", new Object[] { algorithm, key });
            throw new KeyException(err);
        }
        alg = algorithm;
        privateKey = key;
        return this;
    }

    boolean isValidAlgorithmForJavaSecurityKey(String algorithm) {
        return (keyAlgChecker.isRSAlgorithm(algorithm) || keyAlgChecker.isESAlgorithm(algorithm));
    }

    String getValidAlgorithmListForJavaSecurityKey() {
        return Constants.SIGNATURE_ALG_RS256 + ", " +
                Constants.SIGNATURE_ALG_RS384 + ", " +
                Constants.SIGNATURE_ALG_RS512 + ", " +
                Constants.SIGNATURE_ALG_ES256 + ", " +
                Constants.SIGNATURE_ALG_ES384 + ", " +
                Constants.SIGNATURE_ALG_ES512;
    }

    boolean isValidKeyType(Key key, String algorithm) {
        if (key == null) {
            return false;
        }
        return keyAlgChecker.isPrivateKeyValidType(key, algorithm);
    }

    // shared key for signing
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#signWith(java.lang.String, java.lang.String)
     */
    @Override
    public Builder signWith(String algorithm, String key) throws KeyException {
        if (!isValidAlgorithmForStringKey(algorithm)) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_ALGORITHM_ERR", new Object[] { algorithm, getValidAlgorithmListForStringKey() });
            throw new KeyException(err);
        }
        if (key == null || key.isEmpty()) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_KEY_ERR", new Object[] { algorithm, key });
            throw new KeyException(err);
        }
        alg = algorithm;
        sharedKey = key;
        return this;
    }

    boolean isValidAlgorithmForStringKey(String algorithm) {
        return keyAlgChecker.isHSAlgorithm(algorithm);
    }

    String getValidAlgorithmListForStringKey() {
        return Constants.SIGNATURE_ALG_HS256 + ", " +
                Constants.SIGNATURE_ALG_HS384 + ", " +
                Constants.SIGNATURE_ALG_HS512;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.jwt.Builder#encryptWith(java.lang.String, java.security.Key, java.lang.String)
     */
    @Override
    public Builder encryptWith(String keyManagementAlg, Key keyManagementKey, String contentEncryptionAlg) throws KeyException {
        if (keyManagementKey == null) {
            String err = Tr.formatMessage(tc, "KEY_MANAGEMENT_KEY_MISSING", new Object[] { configId });
            throw new KeyException(err);
        }
        if (keyManagementAlg == null || keyManagementAlg.isEmpty()) {
            keyManagementAlg = DEFAULT_KEY_MANAGEMENT_ALGORITHM;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null or empty key management algorithm provided; defaulting to " + keyManagementAlg);
            }
        }
        if (contentEncryptionAlg == null || contentEncryptionAlg.isEmpty()) {
            contentEncryptionAlg = DEFAULT_CONTENT_ENCRYPTION_ALGORITHM;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null or empty content encryption algorithm provided; defaulting to " + contentEncryptionAlg);
            }
        }
        this.keyManagementAlg = keyManagementAlg;
        this.keyManagementKey = keyManagementKey;
        this.contentEncryptionAlg = contentEncryptionAlg;
        return this;
    }

    // add claims with the given name and value
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#claim(java.lang.String, java.lang.Object)
     */
    @Override
    public Builder claim(String name, Object value) throws InvalidClaimException {
        if (isValidClaim(name, value)) {
            if (name.equals(Claims.AUDIENCE)) {
                if (value instanceof ArrayList) {
                    this.audience((ArrayList) value);
                } else if (value instanceof String) {
                    String[] auds = ((String) value).split(" ");
                    ArrayList<String> audList = new ArrayList<String>();
                    for (String aud : auds) {
                        if (!aud.isEmpty()) {
                            audList.add(aud.trim());
                        }
                    }
                    if (!audList.isEmpty()) {
                        this.audience(audList);
                    }
                } else {
                    String msg = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_TYPE", new Object[] { Claims.AUDIENCE });
                    throw new InvalidClaimException(msg);
                }
            } else if (name.equals(Claims.EXPIRATION)) {
                if (value instanceof Long) {
                    this.expirationTime((Long) value);
                } else if (value instanceof Integer) {
                    this.expirationTime(((Integer) value).longValue());
                } else {
                    String msg = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_TYPE", new Object[] { Claims.EXPIRATION });
                    throw new InvalidClaimException(msg);
                }
            } else if (name.equals(Claims.ISSUED_AT)) {
                if (value instanceof Long) {
                    this.issueTime((Long) value);
                } else if (value instanceof Integer) {
                    this.expirationTime(((Integer) value).longValue());
                } else {
                    String msg = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_TYPE", new Object[] { Claims.ISSUED_AT });
                    throw new InvalidClaimException(msg);
                }
            } else if (name.equals(Claims.NOT_BEFORE)) {
                if (value instanceof Long) {
                    this.notBefore((Long) value);
                } else if (value instanceof Integer) {
                    this.expirationTime(((Integer) value).longValue());
                } else {
                    String msg = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_TYPE", new Object[] { Claims.NOT_BEFORE });
                    throw new InvalidClaimException(msg);
                }
            } else if (name.equals(Claims.ISSUER) || name.equals(Claims.SUBJECT)) {
                if (value instanceof String) {
                    if (name.equals(Claims.ISSUER)) {
                        this.issuer((String) value);
                    } else {
                        this.subject((String) value);
                    }
                } else {
                    String msg = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_TYPE", new Object[] { name });
                    throw new InvalidClaimException(msg);
                }
            } else {
                claims.put(name, value);
            }
        }
        return this;
    }

    // Add given claims
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#claim(java.util.Map)
     */
    @Override
    public Builder claim(Map<String, Object> map) throws InvalidClaimException {
        if (map == null) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIMS_ERR");// "JWT_INVALID_CLAIMS_ERR";
            throw new InvalidClaimException(err);
        }
        return copyClaimsMap(map);
        // claims.putAll(map);
        // return this;
    }

    // fetch claim from configured user registry
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#fetch(java.lang.String)
     */
    @Override
    public Builder fetch(String name) throws InvalidClaimException {
        if (JwtUtils.isNullEmpty(name)) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR", new Object[] { name });
            throw new InvalidClaimException(err);
        }
        String sub = claims.getSubject();
        if (JwtUtils.isNullEmpty(sub)) {
            // TODO
            // Tr.warning
            // We can not really get to registry without valid user name
        } else {
            Object obj = null;
            try {
                obj = JwtUtils.fetch(name, sub);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                // Tr.warning
                // e.printStackTrace();
            }
            if (obj != null) {
                claims.put(name, obj);
            }
        }
        return this;
    }

    // remove claims associated with the name
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#remove(java.lang.String)
     */
    @Override
    public Builder remove(String name) throws InvalidClaimException {
        if (JwtUtils.isNullEmpty(name)) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR", new Object[] { name });
            throw new InvalidClaimException(err);
        }
        claims.remove(name);
        return this;
    }

    // fetch claim from json or jwt. The jwt can be base 64 encoded or
    // decoded.
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#claimFrom(java.lang.String, java.lang.String)
     */
    @Override
    public Builder claimFrom(String jsonOrJwt, String claim) throws InvalidClaimException, InvalidTokenException {
        if (JwtUtils.isNullEmpty(claim)) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR", new Object[] { claim });
            throw new InvalidClaimException(err);
        }
        if (isValidToken(jsonOrJwt)) {
            String decoded = jsonOrJwt;
            if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
                decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
            }
            boolean isJson = JwtUtils.isJson(decoded);

            if (!isJson) {
                String jwtPayload = JwtUtils.getPayload(jsonOrJwt);
                decoded = JwtUtils.decodeFromBase64String(jwtPayload);
            }

            // } else {
            // // either decoded payload from jwt or encoded/decoded json string
            // if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
            // decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
            // }
            // }

            if (decoded != null) {
                Object claimValue = null;
                try {
                    if ((claimValue = JwtUtils.claimFromJsonObject(decoded, claim)) != null) {
                        claims.put(claim, claimValue);
                    }
                } catch (JoseException e) {
                    String err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
                    throw new InvalidTokenException(err);
                }
            }
        }

        return this;
    }

    // fetch all claim from json or jwt. The jwt can be base 64 encoded or
    // decoded
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#claimFrom(java.lang.String)
     */
    @Override
    public Builder claimFrom(String jsonOrJwt) throws InvalidTokenException {
        isValidToken(jsonOrJwt);
        String decoded = jsonOrJwt;
        if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
            decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
        }
        boolean isJson = JwtUtils.isJson(decoded);

        if (!isJson) {
            String jwtPayload = JwtUtils.getPayload(jsonOrJwt);
            decoded = JwtUtils.decodeFromBase64String(jwtPayload);
        }

        // boolean isJwt = JwtUtils.isJwtToken(jsonOrJwt);
        // String decoded = jsonOrJwt;
        // if (isJwt) {
        // String jwtPayload = JwtUtils.getPayload(jsonOrJwt);
        // decoded = JwtUtils.decodeFromBase64String(jwtPayload);
        //
        // } else {
        // // either decoded payload from jwt or encoded/decoded json string
        // if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
        // decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
        // }
        // }

        // String formattedString = JwtUtils.fromBase64ToJsonString(jsonOrJwt);
        if (decoded != null) {
            Map claimsFromAnother = null;
            try {
                claimsFromAnother = JwtUtils.claimsFromJsonObject(decoded);
            } catch (JoseException e) {
                String err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
                throw new InvalidTokenException(err);
            }
            if (claimsFromAnother != null && !claimsFromAnother.isEmpty()) {
                claims.putAll(claimsFromAnother);
            }
        }
        return this;
    }

    // fetch claim from jwt.
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#claimFrom(com.ibm.websphere.security.jwt.JwtToken, java.lang.String)
     */
    @Override
    public Builder claimFrom(JwtToken jwt, String claimName) throws InvalidClaimException, InvalidTokenException {
        isValidToken(jwt);
        if (JwtUtils.isNullEmpty(claimName)) {
            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR", new Object[] { claimName });
            throw new InvalidClaimException(err);
        }
        if (jwt.getClaims().get(claimName) != null) {
            claims.put(claimName, jwt.getClaims().get(claimName));
        }
        // else {
        // String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR", new
        // Object[] { claimName, "null" });
        // throw new InvalidClaimException(err);
        // }
        return this;
    }

    // fetch all claim from jwt.
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#claimFrom(com.ibm.websphere.security.jwt.JwtToken)
     */
    @Override
    public Builder claimFrom(JwtToken jwt) throws InvalidTokenException {
        if (jwt != null && !jwt.getClaims().isEmpty()) {
            claims.putAll(jwt.getClaims());
            // copyClaimsMap(jwt.getClaims());
        } else {
            String err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");// "JWT_INVALID_TOKEN_ERR";
            throw new InvalidTokenException(err);
        }
        return this;
    }

    // build JWT
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.jwt.internal.Builder#buildJwt()
     */
    @Override
    public JwtToken buildJwt() throws JwtException, InvalidBuilderException {
        // Create JWT here
        // TODO check for default claims?
        JwtConfig config = getConfig(configId);
        JwtToken jwt = new TokenImpl(this, config);

        return jwt;
    }

    public Claims getClaims() {
        return claims;
    }

    public Key getKey() {
        return privateKey;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public String getAlgorithm() {
        return alg;
    }

    public String getKeyManagementAlg() {
        return keyManagementAlg;
    }

    public Key getKeyManagementKey() {
        return keyManagementKey;
    }

    public String getContentEncryptionAlg() {
        return contentEncryptionAlg;
    }

    private boolean isValidClaim(String key, Object value) throws InvalidClaimException {
        String err = null;
        if (JwtUtils.isNullEmpty(key)) {
            err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR", new Object[] { key });
            throw new InvalidClaimException(err);
        }
        if (value == null) {
            err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR", new Object[] { key, "null" });
            throw new InvalidClaimException(err);
        }
        return true;
    }

    private boolean isValidToken(String tokenString) throws InvalidTokenException {
        String err = null;
        if (JwtUtils.isNullEmpty(tokenString)) {
            err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
            throw new InvalidTokenException(err);
        }
        return true;
    }

    private boolean isValidToken(JwtToken jwt) throws InvalidTokenException {
        String err = null;
        if (jwt == null) {
            err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
            throw new InvalidTokenException(err);
        }
        return true;
    }

    private Builder copyClaimsMap(Map<String, Object> map) throws InvalidClaimException {
        Set<Entry<String, Object>> entries = map.entrySet();
        Iterator<Entry<String, Object>> it = entries.iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();

            String key = entry.getKey();
            Object value = entry.getValue();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Builder Claims Key : " + key + ", Value: " + value);
            }
            claim(key, value);
            // JsonElement jsonElem = entry.getValue();
        }
        return this;
    }

    /**
     * Checks the attributes provided exists in the subject, if so add it to the
     * claims as "amr" values
     *
     * @param amrAttr
     * @throws Exception
     */
    private void checkAmrAttrInSubject(List<String> amrAttr) throws Exception {
        Subject subj = WSSubject.getRunAsSubject();
        List<Object> amrValues = new ArrayList<Object>();
        if (subj != null) {
            WSCredential wscred = getWSCredential(subj);
            for (String attr : amrAttr) {
                Object subjValue = wscred != null ? wscred.get(attr) : null;
                if (subjValue != null) {
                    amrValues.add(subjValue);
                }
            }
        }
        if (amrValues.size() > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Builder Claims Key: amr: [" + amrValues + "]");
            }
            claims.put("amr", amrValues);
        }
    }

    private WSCredential getWSCredential(Subject subject) {
        WSCredential wsCredential = null;
        Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
        Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
        if (wsCredentialsIterator.hasNext()) {
            wsCredential = wsCredentialsIterator.next();
        }
        return wsCredential;
    }

}
