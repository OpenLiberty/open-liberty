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
package com.ibm.ws.security.jwt.config;

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

    String getTrustedAlias();

    long getClockSkew();

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
}
