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

import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;

/*
 * Wraps a Jakarta Security 3.0 ClaimDefinition into a feature independent implementation.
 */
public class ClaimsDefinitionWrapper implements ClaimsMappingConfig {

    private final ClaimsDefinition claimsDefinition;

    public ClaimsDefinitionWrapper(ClaimsDefinition claimsDefinition) {
        this.claimsDefinition = claimsDefinition;
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getCallerNameClaim() {
        return claimsDefinition.callerNameClaim();
    }

    // TODO: Evaluate EL expression.
    @Override
    public String getCallerGroupsClaim() {
        return claimsDefinition.callerGroupsClaim();
    }

}