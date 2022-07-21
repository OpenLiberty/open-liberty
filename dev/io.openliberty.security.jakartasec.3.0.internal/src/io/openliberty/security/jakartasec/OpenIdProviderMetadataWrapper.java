/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec;

import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;

/*
 * Wraps a Jakarta Security 3.0 OpenIdProviderMetadata into a feature independent implementation.
 */
public class OpenIdProviderMetadataWrapper implements OidcProviderMetadata {

    private final OpenIdProviderMetadata providerMetadata;

    public OpenIdProviderMetadataWrapper(OpenIdProviderMetadata providerMetadata) {
        this.providerMetadata = providerMetadata;
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getAuthorizationEndpoint() {
        return providerMetadata.authorizationEndpoint();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getTokenEndpoint() {
        return providerMetadata.tokenEndpoint();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getUserinfoEndpoint() {
        return providerMetadata.userinfoEndpoint();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getEndSessionEndpoint() {
        return providerMetadata.endSessionEndpoint();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getJwksURI() {
        return providerMetadata.jwksURI();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getIssuer() {
        return providerMetadata.issuer();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getSubjectTypeSupported() {
        return providerMetadata.subjectTypeSupported();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getIdTokenSigningAlgorithmsSupported() {
        return providerMetadata.idTokenSigningAlgorithmsSupported();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getResponseTypeSupported() {
        return providerMetadata.responseTypeSupported();
    }

}