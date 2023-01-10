/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.jwt;

import java.net.SocketTimeoutException;
import java.security.Key;

import org.apache.http.conn.ConnectTimeoutException;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.common.jwt.jwk.RemoteJwkData;
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

    @FFDCIgnore({ ConnectTimeoutException.class, SocketTimeoutException.class })
    public static Key getJwsVerificationKey(JsonWebStructure jws, OidcClientConfig clientConfig) throws Exception {
        RemoteJwkData jwkData = initializeRemoteJwkData(clientConfig);

        JwsVerificationKeyHelper.Builder keyHelperBuilder = new JwsVerificationKeyHelper.Builder();
        JwsVerificationKeyHelper keyHelper = keyHelperBuilder.configId(clientConfig.getClientId()).sharedSecret(clientConfig.getClientSecret()).remoteJwkData(jwkData).jwkSet(Client.getJwkSet()).build();

        Key jwtVerificationKey = null;
        try {
            jwtVerificationKey = keyHelper.getVerificationKey(jws);
        } catch (ConnectTimeoutException e) {
            throw new VerificationKeyException(clientConfig.getClientId(), Tr.formatMessage(tc, "JWK_CONNECTION_TIMED_OUT", jwkData.getJwksUri(), jwkData.getJwksConnectTimeout()));
        } catch (SocketTimeoutException e) {
            throw new VerificationKeyException(clientConfig.getClientId(), Tr.formatMessage(tc, "JWK_READ_TIMED_OUT", jwkData.getJwksUri(), jwkData.getJwksReadTimeout()));
        }
        return jwtVerificationKey;
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
