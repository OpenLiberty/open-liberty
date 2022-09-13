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

import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;

/*
 * Wraps a Jakarta Security 3.0 ClaimDefinition into a feature independent implementation.
 */
public class ClaimsDefinitionWrapper implements ClaimsMappingConfig {

    private static final TraceComponent tc = Tr.register(ClaimsDefinitionWrapper.class);

    private final ClaimsDefinition claimsDefinition;

    private final ELHelper elHelper;

    private final String callerNameClaim;

    private final String callerGroupsClaim;

    public ClaimsDefinitionWrapper(ClaimsDefinition claimsDefinition) {
        this.claimsDefinition = claimsDefinition;

        this.elHelper = new ELHelper();

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
        return evaluateStringAttribute("callerNameClaim", claimsDefinition.callerNameClaim(), OpenIdConstant.PREFERRED_USERNAME, immediateOnly);
    }

    private String evaluateCallerGroupsClaim(boolean immediateOnly) {
        return evaluateStringAttribute("callerGroupsClaim", claimsDefinition.callerGroupsClaim(), OpenIdConstant.GROUPS, immediateOnly);
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
            Tr.warning(tc, "JAKARTASEC_WARNING_CLAIM_DEF_CONFIG", new Object[] { attributeName, valueProvided, attributeDefault });
        }
    }

}