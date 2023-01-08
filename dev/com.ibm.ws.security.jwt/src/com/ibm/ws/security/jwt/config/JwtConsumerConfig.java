/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.config;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.List;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.jwk.impl.JWKSet;

public interface JwtConsumerConfig {

    String getId();

    String getIssuer();

    @Sensitive
    String getSharedKey();

    List<String> getAudiences();

    boolean ignoreAudClaimIfNotConfigured();

    String getSignatureAlgorithm();

    String getTrustStoreRef();

    String getKeyStoreRef();

    String getTrustedAlias();

    long getClockSkew();

    default long getTokenAge() {
        return 0;
    }

    default String getKeyManagementKeyAlgorithm() {
        return null;
    }

    boolean getJwkEnabled();

    String getJwkEndpointUrl();

    ConsumerUtils getConsumerUtils();

    boolean isValidationRequired();

    boolean isHostNameVerificationEnabled();

    String getSslRef();

    JWKSet getJwkSet(); // one JWKSet per one config

    boolean getTokenReuse();

    boolean getUseSystemPropertiesForHttpClientConnections();

    List<String> getAMRClaim();

    public String getKeyManagementKeyAlias();

    @Sensitive
    public Key getJweDecryptionKey() throws GeneralSecurityException;

}
