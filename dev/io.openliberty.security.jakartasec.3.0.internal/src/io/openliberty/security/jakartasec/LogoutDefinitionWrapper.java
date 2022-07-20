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

import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;

/*
 * Wraps a Jakarta Security 3.0 LogoutDefinition into a feature independent implementation.
 */
public class LogoutDefinitionWrapper implements LogoutConfig {

    private final LogoutDefinition logoutDefinition;

    public LogoutDefinitionWrapper(LogoutDefinition logoutDefinition) {
        this.logoutDefinition = logoutDefinition;
    }

    // TODO: Evaluate EL expression.
    @Override
    public boolean isNotifyProvider() {
        return logoutDefinition.notifyProvider();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getRedirectURI() {
        return logoutDefinition.redirectURI();
    }

    // TODO: Evaluate EL expression.
    @Override
    public boolean isAccessTokenExpiry() {
        return logoutDefinition.accessTokenExpiry();
    }

    // TODO: Evaluate EL expression.
    @Override
    public boolean isIdentityTokenExpiry() {
        return logoutDefinition.identityTokenExpiry();
    }

}