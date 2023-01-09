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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.javaeesec.identitystore.ELHelper;

import io.openliberty.security.jakartasec.el.ELUtils;
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
        return ELUtils.evaluateBooleanAttribute("notifyProvider", logoutDefinition.notifyProvider(), false,
                                                logoutDefinition.notifyProviderExpression(), immediateOnly);
    }

    @Override
    public String getRedirectURI() {
        return (redirectURI != null) ? redirectURI : evaluateRedirectURI(false);
    }

    private String evaluateRedirectURI(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("redirectURI", logoutDefinition.redirectURI(), JakartaSec30Constants.EMPTY_DEFAULT, immediateOnly);
    }

    @Override
    public boolean isAccessTokenExpiry() {
        return (accessTokenExpiry != null) ? accessTokenExpiry : evaluateAccessTokenExpiry(false);
    }

    private Boolean evaluateAccessTokenExpiry(boolean immediateOnly) {
        return ELUtils.evaluateBooleanAttribute("accessTokenExpiryExpression", logoutDefinition.accessTokenExpiry(), false,
                                                logoutDefinition.accessTokenExpiryExpression(), immediateOnly);
    }

    @Override
    public boolean isIdentityTokenExpiry() {
        return (identityTokenExpiry != null) ? identityTokenExpiry : evaluateIdentityTokenExpiry(false);
    }

    private Boolean evaluateIdentityTokenExpiry(boolean immediateOnly) {
        return ELUtils.evaluateBooleanAttribute("identityTokenExpiryExpression", logoutDefinition.identityTokenExpiry(), false,
                                                logoutDefinition.identityTokenExpiryExpression(), immediateOnly);
    }

}