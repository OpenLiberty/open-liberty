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
import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;

/*
 * Wraps a Jakarta Security 3.0 ClaimDefinition into a feature independent implementation.
 */
public class ClaimsDefinitionWrapper implements ClaimsMappingConfig {

    private final ClaimsDefinition claimsDefinition;

    private final String callerNameClaim;

    private final String callerGroupsClaim;

    public ClaimsDefinitionWrapper(ClaimsDefinition claimsDefinition) {
        this.claimsDefinition = claimsDefinition;

        callerNameClaim = evaluateCallerNameClaim(true);
        callerGroupsClaim = evaluateCallerGroupsClaim(true);
    }

    @Override
    public String getCallerNameClaim() {
        return (callerNameClaim != null) ? callerNameClaim : evaluateCallerNameClaim(false);
    }

    @Override
    public String getCallerGroupsClaim() {
        return (callerGroupsClaim != null) ? callerGroupsClaim : evaluateCallerGroupsClaim(false);
    }

    private String evaluateCallerNameClaim(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("callerNameClaim", claimsDefinition.callerNameClaim(), OpenIdConstant.PREFERRED_USERNAME, immediateOnly);
    }

    private String evaluateCallerGroupsClaim(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("callerGroupsClaim", claimsDefinition.callerGroupsClaim(), OpenIdConstant.GROUPS, immediateOnly);
    }

}