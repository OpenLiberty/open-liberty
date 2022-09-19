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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.identitystore.ELHelper;

import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;

/*
 * Wraps a Jakarta Security 3.0 OpenIdProviderMetadata into a feature independent implementation.
 */
public class OpenIdProviderMetadataWrapper implements OidcProviderMetadata {

    private static final TraceComponent tc = Tr.register(LogoutDefinitionWrapper.class);

    private final OpenIdProviderMetadata providerMetadata;

    private final ELHelper elHelper;

    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;
    private final String endSessionEndpoint;
    private final String jwksURI;
    private final String issuer;
    private final String subjectTypeSupported;
    private final String idTokenSigningAlgorithmsSupported;
    private final String responseTypeSupported;

    public OpenIdProviderMetadataWrapper(OpenIdProviderMetadata providerMetadata) {
        this.providerMetadata = providerMetadata;

        this.elHelper = new ELHelper();
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
        return evaluateStringAttribute("authorizationEndpoint", providerMetadata.authorizationEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getTokenEndpoint() {
        return (tokenEndpoint != null) ? tokenEndpoint : evaluateTokenEndpoint(false);
    }

    private String evaluateTokenEndpoint(boolean immediateOnly) {
        return evaluateStringAttribute("tokenEndpoint", providerMetadata.tokenEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getUserinfoEndpoint() {
        return (userinfoEndpoint != null) ? userinfoEndpoint : evaluateUserinfoEndpoint(false);
    }

    private String evaluateUserinfoEndpoint(boolean immediateOnly) {
        return evaluateStringAttribute("userinfoEndpoint", providerMetadata.userinfoEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getEndSessionEndpoint() {
        return (endSessionEndpoint != null) ? endSessionEndpoint : evaluateEndSessionEndpoint(false);
    }

    private String evaluateEndSessionEndpoint(boolean immediateOnly) {
        return evaluateStringAttribute("endSessionEndpoint", providerMetadata.endSessionEndpoint(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getJwksURI() {
        return (jwksURI != null) ? jwksURI : evaluateJwksURI(false);
    }

    private String evaluateJwksURI(boolean immediateOnly) {
        return evaluateStringAttribute("jwksURI", providerMetadata.jwksURI(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getIssuer() {
        return (issuer != null) ? issuer : evaluateIssuer(false);
    }

    private String evaluateIssuer(boolean immediateOnly) {
        return evaluateStringAttribute("issuer", providerMetadata.issuer(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public String getSubjectTypeSupported() {
        return (subjectTypeSupported != null) ? subjectTypeSupported : evaluateSubjectTypeSupported(false);
    }

    private String evaluateSubjectTypeSupported(boolean immediateOnly) {
        return evaluateStringAttribute("subjectTypeSupported", providerMetadata.subjectTypeSupported(), JakartaSec30Constants.SUBJECT_TYPE_SUPPORTED_DEFAULT, immediateOnly);
    }

    @Override
    public String getIdTokenSigningAlgorithmsSupported() {
        return (idTokenSigningAlgorithmsSupported != null) ? idTokenSigningAlgorithmsSupported : evaluateIdTokenSigningAlgorithmsSupported(false);
    }

    private String evaluateIdTokenSigningAlgorithmsSupported(boolean immediateOnly) {
        return evaluateStringAttribute("idTokenSigningAlgorithmsSupported", providerMetadata.idTokenSigningAlgorithmsSupported(), OpenIdConstant.DEFAULT_JWT_SIGNED_ALGORITHM,
                                       immediateOnly);
    }

    @Override
    public String getResponseTypeSupported() {
        return (responseTypeSupported != null) ? responseTypeSupported : evaluateResponseTypeSupported(false);
    }

    private String evaluateResponseTypeSupported(boolean immediateOnly) {
        return evaluateStringAttribute("responseTypeSupported", providerMetadata.responseTypeSupported(), JakartaSec30Constants.RESPONSE_TYPE_SUPPORTED_DEFAULT, immediateOnly);
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateStringAttribute(String attributeName, String attribute, String attributeDefault, boolean immediateOnly) {
        try {
            return elHelper.processString(attributeName, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attribute)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attribute, attributeDefault);

            return attributeDefault;
        }
    }

    private void issueWarningMessage(String attributeName, Object valueProvided, Object attributeDefault) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            Tr.warning(tc, "JAKARTASEC_WARNING_PROV_METADATA_CONFIG", new Object[] { attributeName, valueProvided, attributeDefault });
        }
    }
}