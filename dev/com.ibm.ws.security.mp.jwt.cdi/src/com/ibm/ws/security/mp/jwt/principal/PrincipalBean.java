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
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;

/**
 * When JsonWebToken feature is enabled, this PrincipalBean provide the following:
 * 1) Overwrite the built-in PrincipalBean
 * 2) Handle @Inject private JsonWebToken jsonWebton
 */
@Alternative
@Priority(100)
@RequestScoped
public class PrincipalBean implements JsonWebToken {
    /**  */
    private static final TraceComponent tc = Tr.register(PrincipalBean.class);
    Principal principal = null;
    JsonWebToken jsonWebToken = null;
    String wsPrincipalName = null;

    public PrincipalBean() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "PrincipalBean");
        }

        Subject subject = getCallerSubject();
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
                if (principal != null) {
                    wsPrincipalName = getSecurityNameFromCredential(subject); // do this if JsonWebToken is not present, so we can match the behavior of server running w/ javaee-8.0 features
                }   
            }
        }

        if (principal == null) {
            Tr.error(tc, "MPJWT_CDI_PRINCIPAL_UNAVAILABLE"); // CWWKS5604E
            // limit info passed back to app.
            // throw new javax.enterprise.inject.CreationException();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "PrincipalBean", principal);
        }
    }

    /**
     * @param subject
     * @param principalName
     * @return
     */
    private String getSecurityNameFromCredential(Subject subject) {
        String securityName = null;
        WSCredential wsCredential = null;
        wsCredential = getWSCredential(subject);
        if (wsCredential != null) {
            try {
                securityName = wsCredential.getSecurityName();
            } catch (Exception e) {

            }
        }
        return securityName;
    }

    /**
     * @param subject
     * @return
     */
    private WSCredential getWSCredential(Subject subject) {
        SubjectHelper subjectHelper = new SubjectHelper();
        return subjectHelper.getWSCredential(subject);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> claim(String claimName) {
        T claim = (T) getClaim(claimName);
        return Optional.ofNullable(claim);
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

    /** {@inheritDoc} */
    @Override
    public String getName() {
        if (principal != null) {
            if (jsonWebToken != null) {
                return principal.getName();
            } else {
                return wsPrincipalName;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getAudience() {
        if (jsonWebToken != null)
            return jsonWebToken.getAudience();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getGroups() {
        if (jsonWebToken != null)
            return jsonWebToken.getGroups();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (jsonWebToken != null)
            return jsonWebToken.toString();
        else if (principal != null)
            return principal.toString();
        return null;
    }

    @SuppressWarnings("unchecked")
    private Subject getCallerSubject() {
        Subject s = null;
        try {
            s = (Subject) java.security.AccessController.doPrivileged(getCallerSubjectAction);
        } catch (PrivilegedActionException pae) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getCallerSubject(PrivilegedAction) Exception caught: " + pae);
        }

        return s;
    }

    @SuppressWarnings("rawtypes")
    private final PrivilegedExceptionAction getCallerSubjectAction = new PrivilegedExceptionAction() {
        @Override
        public Object run() throws Exception {
            return new SubjectManager().getCallerSubject();
        }
    };

}
