/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.jwt;

import java.net.SocketTimeoutException;
import java.security.Key;
import java.util.Arrays;

import org.apache.http.conn.ConnectTimeoutException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.jwt.jwk.RemoteJwkData;
import io.openliberty.security.common.jwt.jws.JwsSignatureVerifier;
import io.openliberty.security.common.jwt.jws.JwsVerificationKeyHelper;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.exceptions.VerificationKeyException;

public class JwtUtils {

    public static final TraceComponent tc = Tr.register(JwtUtils.class);

    /**
     * Verifies the "alg" header in the JWT matches one of the allowed algorithms and sets the appropriate key to use to validate
     * the signature of the token.
     */
    public static io.openliberty.security.common.jwt.jws.JwsSignatureVerifier.Builder verifyJwsAlgHeaderAndCreateJwsSignatureVerifierBuilder(JwtContext jwtContext,
                                                                                                                                             OidcClientConfig clientConfig,
                                                                                                                                             String[] signingAlgsAllowed) throws Exception {
        JwsSignatureVerifier.verifyJwsAlgHeaderOnly(jwtContext, Arrays.asList(signingAlgsAllowed));

        JsonWebStructure jws = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);
        Key jwtVerificationKey = JwtUtils.getJwsVerificationKey(jws, clientConfig);

        io.openliberty.security.common.jwt.jws.JwsSignatureVerifier.Builder verifierBuilder = new JwsSignatureVerifier.Builder();
        verifierBuilder.signatureAlgorithmsSupported(signingAlgsAllowed);
        verifierBuilder.key(jwtVerificationKey);
        return verifierBuilder;
    }

    @FFDCIgnore({ ConnectTimeoutException.class, SocketTimeoutException.class })
    public static Key getJwsVerificationKey(JsonWebStructure jws, OidcClientConfig clientConfig) throws Exception {
        JwsVerificationKeyHelper.Builder keyHelperBuilder = new JwsVerificationKeyHelper.Builder();
        keyHelperBuilder.configId(clientConfig.getClientId());

        setKeyData(jws, clientConfig, keyHelperBuilder);

        JwsVerificationKeyHelper keyHelper = keyHelperBuilder.build();

        try {
            return keyHelper.getVerificationKey(jws);
        } catch (ConnectTimeoutException e) {
            throw new VerificationKeyException(clientConfig.getClientId(), Tr.formatMessage(tc, "JWK_CONNECTION_TIMED_OUT", keyHelper.getRemoteJwkData().getJwksUri(),
                                                                                            keyHelper.getRemoteJwkData().getJwksConnectTimeout()));
        } catch (SocketTimeoutException e) {
            throw new VerificationKeyException(clientConfig.getClientId(), Tr.formatMessage(tc, "JWK_READ_TIMED_OUT", keyHelper.getRemoteJwkData().getJwksUri(),
                                                                                            keyHelper.getRemoteJwkData().getJwksReadTimeout()));
        }
    }

    static void setKeyData(JsonWebStructure jws, OidcClientConfig clientConfig,
                           JwsVerificationKeyHelper.Builder keyHelperBuilder) throws OidcDiscoveryException, OidcClientConfigurationException {
        String algFromHeader = jws.getAlgorithmHeaderValue();
        if (algFromHeader == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JWT is missing the \"alg\" header");
            }
            return;
        }
        if (algFromHeader.equalsIgnoreCase("none")) {
            return;
        }
        if (algFromHeader.startsWith("HS")) {
            keyHelperBuilder.sharedSecret(clientConfig.getClientSecret());
        } else {
            RemoteJwkData jwkData = initializeRemoteJwkData(clientConfig);
            keyHelperBuilder.remoteJwkData(jwkData).jwkSet(Client.getJwkSet());
        }
    }

    private static RemoteJwkData initializeRemoteJwkData(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        RemoteJwkData jwkData = new RemoteJwkData();
        String jwksUri = MetadataUtils.getJwksUri(oidcClientConfig);
        jwkData.setJwksUri(jwksUri);
        jwkData.setSslSupport(OidcMetadataService.getSSLSupport());
        jwkData.setJwksConnectTimeout(oidcClientConfig.getJwksConnectTimeout());
        jwkData.setJwksReadTimeout(oidcClientConfig.getJwksReadTimeout());
        return jwkData;
    }

}
