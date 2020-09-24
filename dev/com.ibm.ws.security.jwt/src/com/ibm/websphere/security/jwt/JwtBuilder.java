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

package com.ibm.websphere.security.jwt;

import java.security.Key;
import java.util.List;
import java.util.Map;

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

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * <p>
 * This API is used for the creation of JSON Web Token (JWT) security tokens conforming the JWT specification as defined in: <br>
 * <a href="https://tools.ietf.org/html/rfc7519">JSON Web Token (JWT)</a>. The JWT tokens are self-described and can be validated
 * locally by the resource server or the client. <br>
 * <br>
 * The code snippet that is shown here demonstrate how to use this API to generate the token. In the sample code, it is assumed
 * that the configuration id specified in the API matches the jwtBuilder element ID in the server configuration or the default id
 * that is provided in the Liberty runtime. <br>
 * <dl>
 * <dt><B>Sample code for generating JWT Token</B>
 * <dd>
 *
 * <pre>
 *
 *        // 1. Create a JWTBuilder Object.
 *         JwtBuilder jwtBuilder = JwtBuilder.create("samplebuilder");
 *        // Overwrite issuer. This is optional and if issuer is not specified either in the server configuration or here,
 *        // then the Builder will construct a default issuer Url
 *
 *         jwtBuilder = jwtBuilder.issuer("http://host:port/issuer url");
 *        // Overwrite any of the following
 *        // audience, expiration time, not before, subject, signing key or algorithm, jti
 *         jwtBuilder = jwtBuilder.audience(Arrays.asList(new String[]{"one", "two", "three"});
 *         jwtBuilder = jwtBuilder.signWith("HS256", "shared secret");
 *        // Overwrite or set any additional claims
 *         jwtBuilder = jwtBuilder.claim("custom claim", "custom value");
 *
 *
 *        // 2. Create a JWT token
 *          JwtToken jwt = jwtBuilder.buildJwt();
 *
 * </pre>
 *
 * </dl>
 * <dl>
 *
 * @author IBM Corporation
 *
 * @version 1.0
 *
 * @since 1.0
 *
 * @ibm-api
 */

@Component(service = JwtBuilder.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", name = "jwtBuilder")
public class JwtBuilder {

    private Builder builder;

    private final Object initlock = new Object() {
    };
    private static final String KEY_JWT_BUILDER_SERVICE = "builder";
    private static AtomicServiceReference<Builder> builderServiceRef = new AtomicServiceReference<Builder>(KEY_JWT_BUILDER_SERVICE);

    public JwtBuilder() {

    }

    public JwtBuilder(String builderConfigId) throws InvalidBuilderException {
        builder = getTheService().create(builderConfigId);
    }

    private Builder getTheService() {
        return builderServiceRef.getService();
    }

    @Reference(service = Builder.class, name = KEY_JWT_BUILDER_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.RELUCTANT)
    protected void setBuilder(ServiceReference<Builder> ref) {
        synchronized (initlock) {
            builderServiceRef.setReference(ref);
        }
    }

    protected void unsetBuilder(ServiceReference<Builder> ref) {
        synchronized (initlock) {
            builderServiceRef.unsetReference(ref);
        }
    }

    @Activate
    protected void activate(ComponentContext cc) {
        builderServiceRef.activate(cc);
        //active = true;
    }

    @Modified
    protected void modify(Map<String, Object> properties) {

    }

    @Deactivate
    protected void deactivate(int reason, ComponentContext cc) {
        builderServiceRef.deactivate(cc);
        //active = false;
    }

    /**
     * Creates a new {@code JwtBuilder} object using the default configuration ID {@value defaultJWT}.
     *
     * @return A new {@code JwtBuilder} object tied to the {@code jwtBuilder} server configuration element with the default ID
     *         {@value defaultJWT}.
     * @throws InvalidBuilderException
     *             Thrown if the JWT builder service is not available.
     */
    public static JwtBuilder create() throws InvalidBuilderException {
        return create("defaultJWT");
    }

    /**
     * Creates a new {@code JwtBuilder} object using the configuration ID provided.
     *
     * @param builderConfigId
     *            ID of a corresponding {@code jwtBuilder} element in the server configuration.
     *
     * @return A new {@code JwtBuilder} object tied to the {@code jwtBuilder} server configuration element whose {@code id}
     *         attribute matches the ID provided.
     * @throws InvalidConsumerException
     *             Thrown if the builderConfigId is {@code null}, or if there is no matching configuration ID in the
     *             server configuration.
     */
    public synchronized static JwtBuilder create(String builderConfigId)
            throws InvalidBuilderException {
        return new JwtBuilder(builderConfigId);
    }

    /**
     * Sets issuer claim. This claim identifies the principal that issued the JWT.
     *
     * @param issuerUrl
     *            This will be used to set the "iss" claim in the {@code JwtToken}
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the issuerUrl is {@code null}, or empty
     */

    public JwtBuilder issuer(String issuerUrl) throws InvalidClaimException {
        builder = builder.issuer(issuerUrl);
        return this;
    }

    // to whom the token is intended to be sent
    /**
     * Sets audience claim. This claim in the JWT identifies the recipients that the token is intended for.
     *
     * @param newaudiences
     *            This is a list of Strings and will be used to set the "aud" claim in the {@code JwtToken}
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the newaudiences is {@code null}, or empty
     */
    public JwtBuilder audience(List<String> newaudiences)
            throws InvalidClaimException {
        // this.AUDIENCE
        //        if (newaudiences != null && !newaudiences.isEmpty()) {
        //
        //            ArrayList<String> audiences = new ArrayList<String>();
        //            for (String aud : newaudiences) {
        //                if (aud != null && !aud.trim().isEmpty()
        //                        && !audiences.contains(aud)) {
        //                    audiences.add(aud);
        //                }
        //            }
        //            if (audiences.isEmpty()) {
        //                String err = Tr.formatMessage(tc,
        //                        "JWT_INVALID_CLAIM_VALUE_ERR", new Object[] {
        //                                Claims.AUDIENCE, newaudiences });
        //                throw new InvalidClaimException(err);
        //            }
        //            // this.audiences = new ArrayList<String>(audiences);
        //            claims.put(Claims.AUDIENCE, audiences);
        //        } else {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR",
        //                    new Object[] { Claims.AUDIENCE, newaudiences });
        //            throw new InvalidClaimException(err);
        //        }
        builder = builder.audience(newaudiences);
        return this;
    }

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    /**
     * Sets expiration claim. This claim in the JWT identifies the expiration time of the token.
     *
     * @param exp
     *            This is a "long" value representing the time in milliseconds since January 1, 1970, 00:00:00 GMT. This will be
     *            used to set the "exp" claim in the {@code JwtToken}
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the exp is before the current time
     */
    public JwtBuilder expirationTime(long exp) throws InvalidClaimException {
        builder = builder.expirationTime(exp);
        return this;
    }

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    //    private JwtBuilder issueTime(long iat) throws InvalidClaimException {
    //
    //        if (iat > 0) {
    //            claims.put(Claims.ISSUED_AT, Long.valueOf(iat));
    //        } else {
    //            // String msg = "The exp claim must be a positive number.";
    //            String err = Tr.formatMessage(tc, "JWT_INVALID_TIME_CLAIM_ERR",
    //                    new Object[] { Claims.ISSUED_AT });
    //            throw new InvalidClaimException(err);
    //        }
    //        return this;
    //    }

    // generate a unique identifier for the token. The default is "false"
    /**
     * Sets JWT ID. This claim in the JWT provides a unique identifier of the token. This ID helps prevent the token from being
     * replayed.
     *
     * @param create
     *            This is a boolean value that represents whether to generate a unique identifier. If the unique identifier is
     *            generated, then the "jti" claim is set in the {@code JwtToken}
     *
     * @return {@code JwtBuilder} object
     *
     */
    public JwtBuilder jwtId(boolean create) {
        //        if (create) {
        //            String jti = JwtUtils.getRandom(16);
        //            claims.put(Claims.ID, jti);
        //        } else {
        //            claims.remove(Claims.ID);
        //        }
        builder = builder.jwtId(create);
        return this;
    }

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    /**
     * Sets "not before" claim. This claim in the JWT identifies the time before which the JWT must not be accepted.
     *
     * @param time_from
     *            This is a "long" value representing the time in milliseconds since January 1, 1970, 00:00:00 GMT. This will be
     *            used to set the "nbf" claim in the {@code JwtToken}
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the time_from is not a positive number
     */
    public JwtBuilder notBefore(long time_from) throws InvalidClaimException {

        //        if (time_from > 0) {
        //            claims.put(Claims.NOT_BEFORE, time_from);
        //        } else {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_TIME_CLAIM_ERR",
        //                    new Object[] { Claims.NOT_BEFORE });
        //            throw new InvalidClaimException(err);
        //        }
        builder = builder.notBefore(time_from);
        return this;
    }

    // the subject/principal is whom the token is about
    /**
     * Sets "subject" claim. This claim in the JWT identifies the principal that is the subject of the token.
     *
     * @param username
     *            This String value represents the principal name. This will be used
     *            to set the "sub" claim in the {@code JwtToken}
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the username is {@code null}, or empty
     */
    public JwtBuilder subject(String username) throws InvalidClaimException {
        //        if (username != null && !username.isEmpty()) {
        //
        //            claims.put(Claims.SUBJECT, username);
        //        } else {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR",
        //                    new Object[] { Claims.SUBJECT, username });
        //            throw new InvalidClaimException(err);
        //        }
        builder = builder.subject(username);
        return this;
    }

    // private key for token signing
    /**
     * Signing key and algorithm information.
     *
     * @param algorithm
     *            This String value represents the signing algorithm. This information will be used
     *            to sign the {@code JwtToken}
     * @param key
     *            The private key {@code Key} to use for signing JWTs.
     * @return {@code JwtBuilder} object
     * @throws KeyException
     *             Thrown if the key is {@code null} or if algorithm is {@code null} or empty
     */
    public JwtBuilder signWith(String algorithm, Key key) throws KeyException {
        builder = builder.signWith(algorithm, key);
        return this;
    }

    // shared key for signing
    /**
     * Signing key and algorithm information.
     *
     * @param algorithm
     *            This String value represents the signing algorithm. This information will be used
     *            to sign the {@code JwtToken}
     * @param key
     *            This represents shared secret that can be used to create the shared key
     * @return {@code JwtBuilder} object
     * @throws KeyException
     *             Thrown if the key or algorithm is {@code null} or empty
     */
    public JwtBuilder signWith(String algorithm, String key) throws KeyException {
        builder = builder.signWith(algorithm, key);
        return this;
    }

    /**
     * Sets token encryption information for creating JSON Web Encryption tokens.
     *
     * @param keyManagementAlg
     *            Specifies the encryption algorithm that is used to encrypt the Content Encryption Key.
     * @param keyManagementKey
     *            Key used to encrypt the Content Encryption Key, which is then used to encrypt the JWT plaintext to produce
     *            the JWE ciphertext.
     * @param contentEncryptionAlg
     *            Specifies the encryption algorithm that is used to encrypt the JWT plaintext to produce the JWE ciphertext.
     * @return {@code JwtBuilder} object
     * @throws KeyException
     *             Thrown if an error is encountered using the provided key and encryption information
     */
    public JwtBuilder encryptWith(String keyManagementAlg, Key keyManagementKey, String contentEncryptionAlg) throws KeyException {
        builder = builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
        return this;
    }

    // add claims with the given name and value
    /**
     * Sets the specified claim.
     *
     * @param name
     *            This is a String and represents the name of the claim
     * @param value
     *            This is an Object and represents the value of the claim. Object can be a string, number, boolean, map, or list.
     *            Other object types will have their toString method called, which might not produce valid JSON
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the claim is {@code null}, or the value is {@code null} or the value is not the correct type for the
     *             claim
     */
    // object type restriction originates from https://bitbucket.org/b_c/jose4j/issues/160/unexpected-result-from-jwtclaimssetclaim
    public JwtBuilder claim(String name, Object value)
            throws InvalidClaimException {
        //        if (isValidClaim(name, value)) {
        //            if (name.equals(Claims.AUDIENCE)) {
        //                if (value instanceof ArrayList) {
        //                    this.audience((ArrayList) value);
        //                } else if (value instanceof String) {
        //                    String[] auds = ((String) value).split(" ");
        //                    ArrayList<String> audList = new ArrayList<String>();
        //                    for (String aud : auds) {
        //                        if (!aud.isEmpty()) {
        //                            audList.add(aud.trim());
        //                        }
        //                    }
        //                    if (!audList.isEmpty()) {
        //                        this.audience(audList);
        //                    }
        //                } else {
        //                    String msg = Tr.formatMessage(tc,
        //                            "JWT_INVALID_CLAIM_VALUE_TYPE",
        //                            new Object[] { Claims.AUDIENCE });
        //                    throw new InvalidClaimException(msg);
        //                }
        //            } else if (name.equals(Claims.EXPIRATION)) {
        //                if (value instanceof Long) {
        //                    this.expirationTime((Long) value);
        //                } else if (value instanceof Integer) {
        //                    this.expirationTime(((Integer) value).longValue());
        //                } else {
        //                    String msg = Tr.formatMessage(tc,
        //                            "JWT_INVALID_CLAIM_VALUE_TYPE",
        //                            new Object[] { Claims.EXPIRATION });
        //                    throw new InvalidClaimException(msg);
        //                }
        //            } else if (name.equals(Claims.ISSUED_AT)) {
        //                if (value instanceof Long) {
        //                    this.issueTime((Long) value);
        //                } else if (value instanceof Integer) {
        //                    this.expirationTime(((Integer) value).longValue());
        //                } else {
        //                    String msg = Tr.formatMessage(tc,
        //                            "JWT_INVALID_CLAIM_VALUE_TYPE",
        //                            new Object[] { Claims.ISSUED_AT });
        //                    throw new InvalidClaimException(msg);
        //                }
        //            } else if (name.equals(Claims.NOT_BEFORE)) {
        //                if (value instanceof Long) {
        //                    this.notBefore((Long) value);
        //                } else if (value instanceof Integer) {
        //                    this.expirationTime(((Integer) value).longValue());
        //                } else {
        //                    String msg = Tr.formatMessage(tc,
        //                            "JWT_INVALID_CLAIM_VALUE_TYPE",
        //                            new Object[] { Claims.NOT_BEFORE });
        //                    throw new InvalidClaimException(msg);
        //                }
        //            } else if (name.equals(Claims.ISSUER) || name.equals(Claims.SUBJECT)) {
        //                if (value instanceof String) {
        //                    if (name.equals(Claims.ISSUER)) {
        //                        this.issuer((String) value);
        //                    } else {
        //                        this.subject((String) value);
        //                    }
        //                } else {
        //                    String msg = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_TYPE", new Object[] { name });
        //                    throw new InvalidClaimException(msg);
        //                }
        //            } else {
        //                claims.put(name, value);
        //            }
        //        }
        builder = builder.claim(name, value);
        return this;
    }

    // Add given claims
    /**
     * Sets the specified claims.
     *
     * @param map
     *            This is a Map and represents the collection of claim name and claim value pairs to be set in the JWT.
     *
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the claim is {@code null}, or the value is {@code null} or the value is not the correct type for the
     *             claim
     */
    public JwtBuilder claim(Map<String, Object> map)
            throws InvalidClaimException {
        //        if (map == null) {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIMS_ERR");// "JWT_INVALID_CLAIMS_ERR";
        //            throw new InvalidClaimException(err);
        //        }
        builder = builder.claim(map);
        return this;
        //return copyClaimsMap(map);
        // claims.putAll(map);
        // return this;
    }

    // fetch claim from configured user registry
    /**
     * Retrieves the specified claim from the configured user registry.
     *
     * @param name
     *            This is a String and represents the name of the claim
     *
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the claim is {@code null} or empty
     *
     */
    public JwtBuilder fetch(String name) throws InvalidClaimException {
        //        if (JwtUtils.isNullEmpty(name)) {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR",
        //                    new Object[] { name });
        //            throw new InvalidClaimException(err);
        //        }
        //        String sub = claims.getSubject();
        //        if (JwtUtils.isNullEmpty(sub)) {
        //            // TODO
        //            // Tr.warning
        //            // We can not really get to registry without valid user name
        //        } else {
        //            Object obj = null;
        //            try {
        //                obj = JwtUtils.fetch(name, sub);
        //            } catch (Exception e) {
        //                // TODO Auto-generated catch block
        //                // Tr.warning
        //                // e.printStackTrace();
        //            }
        //            if (obj != null) {
        //                claims.put(name, obj);
        //            }
        //        }
        builder = builder.fetch(name);
        return this;
    }

    // remove claims associated with the name
    /**
     * Removes the specified claim.
     *
     * @param name
     *            This is a String and represents the name of the claim to remove
     *
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the claim is {@code null} or empty
     *
     */
    public JwtBuilder remove(String name) throws InvalidClaimException {
        //        if (JwtUtils.isNullEmpty(name)) {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR",
        //                    new Object[] { name });
        //            throw new InvalidClaimException(err);
        //        }
        //        claims.remove(name);
        builder = builder.remove(name);
        return this;
    }

    // fetch claim from json or jwt. The jwt can be base 64 encoded or
    // decoded.
    /**
     * Retrieves the specified claim from the given json or jwt string.
     *
     * @param jsonOrJwt
     *            This is a String and represents either base 64 encoded or decoded JWT payload in the json format or base 64
     *            encoded JWT
     *
     * @return {@code JwtBuilder} object
     * @throws InvalidClaimException
     *             Thrown if the claim is {@code null} or empty
     * @throws InvalidTokenException
     *             Thrown if the jsonOrJwt is {@code null} or if the api fails to process the string
     */
    public JwtBuilder claimFrom(String jsonOrJwt, String claim)
            throws InvalidClaimException, InvalidTokenException {
        //        if (JwtUtils.isNullEmpty(claim)) {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR",
        //                    new Object[] { claim });
        //            throw new InvalidClaimException(err);
        //        }
        //        if (isValidToken(jsonOrJwt)) {
        //            String decoded = jsonOrJwt;
        //            if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
        //                decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
        //            }
        //            boolean isJson = JwtUtils.isJson(decoded);
        //
        //            if (!isJson) {
        //                String jwtPayload = JwtUtils.getPayload(jsonOrJwt);
        //                decoded = JwtUtils.decodeFromBase64String(jwtPayload);
        //            }
        //
        //            // } else {
        //            // // either decoded payload from jwt or encoded/decoded json string
        //            // if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
        //            // decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
        //            // }
        //            // }
        //
        //            if (decoded != null) {
        //                Object claimValue = null;
        //                try {
        //                    if ((claimValue = JwtUtils.claimFromJsonObject(decoded,
        //                            claim)) != null) {
        //                        claims.put(claim, claimValue);
        //                    }
        //                } catch (IOException e) {
        //                    String err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
        //                    throw new InvalidTokenException(err);
        //                }
        //            }
        //        }

        builder = builder.claimFrom(jsonOrJwt, claim);
        return this;
    }

    // fetch all claim from json or jwt. The jwt can be base 64 encoded or
    // decoded
    /**
     * Retrieves all the claims from the given json or jwt string.
     *
     * @param jsonOrJwt
     *            This is a String and represents either base 64 encoded or decoded JWT payload in the json format or base 64
     *            encoded JWT
     *
     * @return {@code JwtBuilder} object
     *
     * @throws InvalidTokenException
     *             Thrown if the jsonOrJwt is {@code null} or if the api fails to process the jsonOrJwt string
     */
    public JwtBuilder claimFrom(String jsonOrJwt) throws InvalidClaimException,
            InvalidTokenException {
        //        isValidToken(jsonOrJwt);
        //        String decoded = jsonOrJwt;
        //        if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
        //            decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
        //        }
        //        boolean isJson = JwtUtils.isJson(decoded);
        //
        //        if (!isJson) {
        //            String jwtPayload = JwtUtils.getPayload(jsonOrJwt);
        //            decoded = JwtUtils.decodeFromBase64String(jwtPayload);
        //        }
        //
        //        // boolean isJwt = JwtUtils.isJwtToken(jsonOrJwt);
        //        // String decoded = jsonOrJwt;
        //        // if (isJwt) {
        //        // String jwtPayload = JwtUtils.getPayload(jsonOrJwt);
        //        // decoded = JwtUtils.decodeFromBase64String(jwtPayload);
        //        //
        //        // } else {
        //        // // either decoded payload from jwt or encoded/decoded json string
        //        // if (JwtUtils.isBase64Encoded(jsonOrJwt)) {
        //        // decoded = JwtUtils.decodeFromBase64String(jsonOrJwt);
        //        // }
        //        // }
        //
        //        // String formattedString = JwtUtils.fromBase64ToJsonString(jsonOrJwt);
        //        if (decoded != null) {
        //            Map claimsFromAnother = null;
        //            try {
        //                claimsFromAnother = JwtUtils.claimsFromJsonObject(decoded);
        //            } catch (IOException e) {
        //                String err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
        //                throw new InvalidTokenException(err);
        //            }
        //            if (claimsFromAnother != null && !claimsFromAnother.isEmpty()) {
        //                claims.putAll(claimsFromAnother);
        //            }
        //        }
        builder = builder.claimFrom(jsonOrJwt);
        return this;
    }

    // fetch claim from jwt.

    /**
     *
     * Retrieves the specified claim from the given JwtToken.
     *
     * @param jwt
     *            This is a {@code JwtToken} object
     *
     * @param claimName
     *            This is a String and represents the name of the claim
     *
     * @return {@code JwtBuilder} object
     *
     * @throws InvalidClaimException
     *             Thrown if the claim is {@code null} or empty
     *
     * @throws InvalidTokenException
     *             Thrown if the jwt is {@code null} or if the api fails to process the jwt
     */
    public JwtBuilder claimFrom(JwtToken jwt, String claimName)
            throws InvalidClaimException, InvalidTokenException {
        //        isValidToken(jwt);
        //        if (JwtUtils.isNullEmpty(claimName)) {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR",
        //                    new Object[] { claimName });
        //            throw new InvalidClaimException(err);
        //        }
        //        if (jwt.getClaims().get(claimName) != null) {
        //            claims.put(claimName, jwt.getClaims().get(claimName));
        //        }
        //        // else {
        //        // String err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR", new
        //        // Object[] { claimName, "null" });
        //        // throw new InvalidClaimException(err);
        //        // }
        builder = builder.claimFrom(jwt, claimName);
        return this;
    }

    // fetch all claim from jwt.
    /**
     *
     * Retrieves all the claims from the given jwt.
     *
     * @param jwt
     *            This is a {@code JwtToken} object and represents base 64 encoded JWT
     *
     *
     * @return {@code JwtBuilder} object
     *
     * @throws InvalidTokenException
     *             Thrown if the jwt is {@code null} or if the api fails to process the jwt
     */
    public JwtBuilder claimFrom(JwtToken jwt) throws InvalidTokenException {
        //        if (jwt != null && !jwt.getClaims().isEmpty()) {
        //            claims.putAll(jwt.getClaims());
        //            // copyClaimsMap(jwt.getClaims());
        //        } else {
        //            String err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");// "JWT_INVALID_TOKEN_ERR";
        //            throw new InvalidTokenException(err);
        //        }
        builder = builder.claimFrom(jwt);
        return this;
    }

    // build JWT
    /**
     *
     * Creates a new {@code JwtToken} object based on the information in this {@code JwtBuilder} object and based on
     * the configuration for the {@code jwtBuilder} element that is specified in the server configuration that matches the ID
     * used to instantiate this {@code JwtBuilder} object.
     *
     *
     * @return {@code JwtToken} object.
     *
     * @throws InvalidBuilderException
     *             Thrown if a {@code jwtBuilder} element with the ID used to instantiate this {@code JwtBuilder} object cannot
     *             be found in the server configuration.
     * @throws JwtException
     *             Thrown if there is an error while creating the JWT, which includes creating the token payload, header,
     *             or signature.
     */
    public JwtToken buildJwt() throws JwtException, InvalidBuilderException {
        //        getConfig(configId);
        //
        //        //return jwt;
        //        return null;
        return builder.buildJwt();
    }

    //    public Claims getClaims() {
    //        return claims;
    //    }
    //
    //    public Key getKey() {
    //        return privateKey;
    //    }
    //
    //    public String getSharedKey() {
    //        return sharedKey;
    //    }
    //
    //    public String getAlgorithm() {
    //        return alg;
    //    }

    //    private boolean isValidClaim(String key, Object value)
    //            throws InvalidClaimException {
    //        String err = null;
    //        if (JwtUtils.isNullEmpty(key)) {
    //            err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_ERR",
    //                    new Object[] { key });
    //            throw new InvalidClaimException(err);
    //        }
    //        if (value == null) {
    //            err = Tr.formatMessage(tc, "JWT_INVALID_CLAIM_VALUE_ERR",
    //                    new Object[] { key, "null" });
    //            throw new InvalidClaimException(err);
    //        }
    //        return true;
    //    }

    //    private boolean isValidToken(String tokenString)
    //            throws InvalidTokenException {
    //        String err = null;
    //        if (JwtUtils.isNullEmpty(tokenString)) {
    //            err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
    //            throw new InvalidTokenException(err);
    //        }
    //        return true;
    //    }

    //    private boolean isValidToken(JwtToken jwt) throws InvalidTokenException {
    //        String err = null;
    //        if (jwt == null) {
    //            err = Tr.formatMessage(tc, "JWT_INVALID_TOKEN_ERR");
    //            throw new InvalidTokenException(err);
    //        }
    //        return true;
    //    }

    //    private JwtBuilder copyClaimsMap(Map<String, Object> map)
    //            throws InvalidClaimException {
    //        Set<Entry<String, Object>> entries = map.entrySet();
    //        Iterator<Entry<String, Object>> it = entries.iterator();
    //        while (it.hasNext()) {
    //            Entry<String, Object> entry = it.next();
    //
    //            String key = entry.getKey();
    //            Object value = entry.getValue();
    //            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //                Tr.debug(tc, "Builder Claims Key : " + key + ", Value: "
    //                        + value);
    //            }
    //            claim(key, value);
    //            // JsonElement jsonElem = entry.getValue();
    //        }
    //        return this;
    //    }

}
