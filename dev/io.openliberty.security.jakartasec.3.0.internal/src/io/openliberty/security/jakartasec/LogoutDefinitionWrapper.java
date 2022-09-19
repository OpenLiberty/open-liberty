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

import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;

/*
 * Wraps a Jakarta Security 3.0 LogoutDefinition into a feature independent implementation.
 */
public class LogoutDefinitionWrapper implements LogoutConfig {

    private static final TraceComponent tc = Tr.register(LogoutDefinitionWrapper.class);

    private final LogoutDefinition logoutDefinition;

    private final ELHelper elHelper;

    private final Boolean notifyProvider;

    private final String redirectURI;

    private final Boolean accessTokenExpiry;

    private final Boolean identityTokenExpiry;

    public LogoutDefinitionWrapper(LogoutDefinition logoutDefinition) {
        this.logoutDefinition = logoutDefinition;

        this.elHelper = new ELHelper();

        this.notifyProvider = evaluateNotifyProvider(true);
        this.redirectURI = evaluateRedirectURI(true);
        this.accessTokenExpiry = evaluateAccessTokenExpiry(true);
        this.identityTokenExpiry = evaluateIdentityTokenExpiry(true);

    }

    @Override
    public boolean isNotifyProvider() {
        return (notifyProvider != null) ? notifyProvider : evaluateNotifyProvider(false);
    }

    private Boolean evaluateNotifyProvider(boolean immediateOnly) {
        return evaluateBooleanAttribute("notifyProvider", logoutDefinition.notifyProvider(), false,
                                        logoutDefinition.notifyProviderExpression(), immediateOnly);
    }

    @Override
    public String getRedirectURI() {
        return (redirectURI != null) ? redirectURI : evaluateRedirectURI(false);
    }

    private String evaluateRedirectURI(boolean immediateOnly) {
        return evaluateStringAttribute("redirectURI", logoutDefinition.redirectURI(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public boolean isAccessTokenExpiry() {
        return (accessTokenExpiry != null) ? accessTokenExpiry : evaluateAccessTokenExpiry(false);
    }

    private Boolean evaluateAccessTokenExpiry(boolean immediateOnly) {
        return evaluateBooleanAttribute("accessTokenExpiryExpression", logoutDefinition.accessTokenExpiry(), false,
                                        logoutDefinition.accessTokenExpiryExpression(), immediateOnly);
    }

    @Override
    public boolean isIdentityTokenExpiry() {
        return (identityTokenExpiry != null) ? identityTokenExpiry : evaluateIdentityTokenExpiry(false);
    }

    private Boolean evaluateIdentityTokenExpiry(boolean immediateOnly) {
        return evaluateBooleanAttribute("identityTokenExpiryExpression", logoutDefinition.identityTokenExpiry(), false,
                                        logoutDefinition.identityTokenExpiryExpression(), immediateOnly);
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

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private Boolean evaluateBooleanAttribute(String attributeName, boolean attribute, boolean attributeDefault, String attributeExpression, boolean immediateOnly) {
        try {
            return elHelper.processBoolean(attributeName, attributeExpression, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attributeExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attributeExpression == null ? attribute : attributeExpression, attributeDefault);

            return attributeDefault;
        }
    }

    private void issueWarningMessage(String attributeName, Object valueProvided, Object attributeDefault) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            Tr.warning(tc, "JAKARTASEC_WARNING_LOGOUT_DEF_CONFIG", new Object[] { attributeName, valueProvided, attributeDefault });
        }
    }
}