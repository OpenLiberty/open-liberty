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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

public interface JwtConfig {

    String getId();

    String getIssuerUrl();

    boolean isJwkEnabled();

    long getValidTime();

    List<String> getAudiences();

    String getSignatureAlgorithm();

    List<String> getClaims();

    String getScope();

    boolean getJti();

    String getSharedKey();

    String getTrustStoreRef();

    String getKeyStoreRef();

    String getKeyAlias();

    String getTrustedAlias();

    String getJwkJsonString();

    JSONWebKey getJSONWebKey();

    long getJwkRotationTime();

    int getJwkSigningKeySize();

    String getResolvedHostAndPortUrl();

    PrivateKey getPrivateKey();

    PublicKey getPublicKey();

    List<String> getAMRAttributes();

    long getElapsedNbfTime();

    String getKeyManagementKeyAlgorithm();

    String getKeyManagementKeyAlias();

    String getContentEncryptionAlgorithm();

}
