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
package io.openliberty.security.jakartasec;

import io.openliberty.security.jakartasec.el.ELUtils;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;

/*
 * Wraps a Jakarta Security 3.0 OpenIdProviderMetadata into a feature independent implementation.
 */
public class OpenIdProviderMetadataWrapper implements OidcProviderMetadata {

    private final OpenIdProviderMetadata providerMetadata;

    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;
    private final String endSessionEndpoint;
    private final String jwksURI;
    private final String issuer;
    private final String subjectTypeSupported;
    private final String[] idTokenSigningAlgorithmsSupported;
    private final String responseTypeSupported;

    public OpenIdProviderMetadataWrapper(OpenIdProviderMetadata providerMetadata) {
        this.providerMetadata = providerMetadata;

        this.authorizationEndpoint = evaluateAuthorizationEndpoint(true);
        this.tokenEndpoint = evaluateTokenEndpoint(true);
        this.userinfoEndpoint = evaluateUserinfoEndpoint(true);
        this.endSessionEndpoint = evaluateEndSessionEndpoint(true);
        this.jwksURI = evaluateJwksURI(true);
        this.issuer = evaluateIssuer(true);
        this.subjectTypeSupported = evaluateSubjectTypeSupported(true);
        this.idTokenSigningAlgorithmsSupported = evaluateIdTokenSigningAlgorithmsSupported(true);
        this.responseTypeSupported = evaluateResponseTypeSupported(true);
    }

    @Override
    public String getAuthorizationEndpoint() {
        return (authorizationEndpoint != null) ? authorizationEndpoint : evaluateAuthorizationEndpoint(false);
    }

    private String evaluateAuthorizationEndpoint(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("authorizationEndpoint", providerMetadata.authorizationEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getTokenEndpoint() {
        return (tokenEndpoint != null) ? tokenEndpoint : evaluateTokenEndpoint(false);
    }

    private String evaluateTokenEndpoint(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("tokenEndpoint", providerMetadata.tokenEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getUserinfoEndpoint() {
        return (userinfoEndpoint != null) ? userinfoEndpoint : evaluateUserinfoEndpoint(false);
    }

    private String evaluateUserinfoEndpoint(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("userinfoEndpoint", providerMetadata.userinfoEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getEndSessionEndpoint() {
        return (endSessionEndpoint != null) ? endSessionEndpoint : evaluateEndSessionEndpoint(false);
    }

    private String evaluateEndSessionEndpoint(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("endSessionEndpoint", providerMetadata.endSessionEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getJwksURI() {
        return (jwksURI != null) ? jwksURI : evaluateJwksURI(false);
    }

    private String evaluateJwksURI(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("jwksURI", providerMetadata.jwksURI(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getIssuer() {
        return (issuer != null) ? issuer : evaluateIssuer(false);
    }

    private String evaluateIssuer(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("issuer", providerMetadata.issuer(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getSubjectTypeSupported() {
        return (subjectTypeSupported != null) ? subjectTypeSupported : evaluateSubjectTypeSupported(false);
    }

    private String evaluateSubjectTypeSupported(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("subjectTypeSupported", providerMetadata.subjectTypeSupported(), JakartaSec30Constants.SUBJECT_TYPE_SUPPORTED_DEFAULT,
                                               immediateOnly);
    }

    @Override
    public String[] getIdTokenSigningAlgorithmsSupported() {
        return (idTokenSigningAlgorithmsSupported != null) ? idTokenSigningAlgorithmsSupported : evaluateIdTokenSigningAlgorithmsSupported(false);
    }

    private String[] evaluateIdTokenSigningAlgorithmsSupported(boolean immediateOnly) {
        return ELUtils.evaluateStringArrayAttribute("idTokenSigningAlgorithmsSupported", providerMetadata.idTokenSigningAlgorithmsSupported(),
                                                    new String[] { OpenIdConstant.DEFAULT_JWT_SIGNED_ALGORITHM },
                                                    immediateOnly);
    }

    @Override
    public String getResponseTypeSupported() {
        return (responseTypeSupported != null) ? responseTypeSupported : evaluateResponseTypeSupported(false);
    }

    private String evaluateResponseTypeSupported(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("responseTypeSupported", providerMetadata.responseTypeSupported(), JakartaSec30Constants.RESPONSE_TYPE_SUPPORTED_DEFAULT,
                                               immediateOnly);
    }

}