/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.principal;

import java.security.Principal;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.context.SubjectManager;

/**
 * When JsonWebToken feature is enabled, this PrincipalBean will overwrite the built-in PrincipalBean
 */
@Alternative
@Priority(100)
@RequestScoped
public class PrincipalBean implements JsonWebToken {
    @Produces
    private static final TraceComponent tc = Tr.register(PrincipalBean.class);
    Principal principal = null;
    JsonWebToken jsonWebToken = null;

    public PrincipalBean() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "PrincipalBean");
        }

        Subject subject = new SubjectManager().getCallerSubject();
        if (subject != null) {
            Set<JsonWebToken> jsonWebTokens = subject.getPrincipals(JsonWebToken.class);
            if (!jsonWebTokens.isEmpty()) {
                principal = jsonWebToken = jsonWebTokens.iterator().next();
            }

            if (jsonWebToken == null) {
                Set<Principal> principals = subject.getPrincipals(Principal.class);
                if (!principals.isEmpty()) {
                    principal = principals.iterator().next();
                }
            }
        }

        if (principal == null) {
            Tr.error(tc, "MPJWT_CDI_PRINCIPAL_UNAVAILABLE"); // CWWKS5604E
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "PrincipalBean", principal);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        if (principal != null)
            return principal.getName();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Object getClaim(String claimName) {
        if (jsonWebToken != null)
            return jsonWebToken.getClaim(claimName);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getClaimNames() {
        if (jsonWebToken != null)
            return jsonWebToken.getClaimNames();
        return null;
    }
}
