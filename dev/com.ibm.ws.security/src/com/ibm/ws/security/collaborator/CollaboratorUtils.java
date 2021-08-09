/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.collaborator;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.mp.jwt.proxy.MpJwtHelper;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Utility class for functions that are common between all collaborators.
 */
public class CollaboratorUtils {
    private static final TraceComponent tc = Tr.register(CollaboratorUtils.class);

    protected SubjectManager subjectManager;

    /**
     *
     */
    public CollaboratorUtils(SubjectManager subjectManager) {
        this.subjectManager = subjectManager;
    }

    /**
     * Returns a java.security.Principal object containing the name of the current authenticated user.
     * If the user has not been authenticated, the method returns null.
     * Look at the Subject on the thread only.
     * We will extract, security name from WSCredential and the set of Principals, our WSPrincipal type. If a property has been set, then
     * the realm is prepended to the security name.
     *
     * @param useRealm whether to prepend the security name with the realm name
     * @param realm the realm name to prefix the security name with
     * @param web call by the webRequest
     * @param isJaspiEnabled TODO
     * @return a java.security.Principal containing the name of the user making this request; null if the user has not been authenticated
     */
    public Principal getCallerPrincipal(boolean useRealm, String realm, boolean web, boolean isJaspiEnabled) {
        Subject subject = subjectManager.getCallerSubject();
        if (subject == null) {
            return null;
        }

        SubjectHelper subjectHelper = new SubjectHelper();
        if (subjectHelper.isUnauthenticated(subject) && web) {
            return null;
        }

        if (isJaspiEnabled) {
            Principal principal = getPrincipalFromWSCredential(subjectHelper, subject);
            if (principal != null) {
                return principal;
            }
        }

        String securityName = getSecurityNameFromWSCredential(subjectHelper, subject);
        if (securityName == null) {
            return null;
        }

        Principal jsonWebToken = MpJwtHelper.getJsonWebTokenPricipal(subject);
        if (jsonWebToken != null) {
            return jsonWebToken;
        }

        Set<WSPrincipal> principals = subject.getPrincipals(WSPrincipal.class);
        if (principals.size() > 1) {
            multiplePrincipalsError(principals);
        }

        WSPrincipal wsPrincipal = null;
        if (!principals.isEmpty()) {
            String principalName = createPrincipalName(useRealm, realm, securityName);
            wsPrincipal = principals.iterator().next();
            wsPrincipal = new WSPrincipal(principalName, wsPrincipal.getAccessId(), wsPrincipal.getAuthenticationMethod());
        }
        return wsPrincipal;
    }

    /**
     * @param useRealm
     * @param realm
     * @param securityName
     * @return
     */
    private String createPrincipalName(boolean useRealm, String realm, String securityName) {
        String principalName;
        if (useRealm && realm != null) {
            principalName = realm + AccessIdUtil.REALM_SEPARATOR + securityName;
        } else {
            principalName = securityName;
        }
        return principalName;
    }

    private String getSecurityNameFromWSCredential(SubjectHelper subjectHelper, Subject subject) {
        String securityName = null;
        WSCredential wscredential = subjectHelper.getWSCredential(subject);
        if (wscredential != null) {
            try {
                securityName = wscredential.getSecurityName();
            } catch (CredentialExpiredException e) {
                //do nothing
            } catch (CredentialDestroyedException e) {
                //do nothing
            }
        }
        return securityName;
    }

    private Principal getPrincipalFromWSCredential(SubjectHelper subjectHelper, Subject subject) {
        Principal principal = null;
        WSCredential wscredential = subjectHelper.getWSCredential(subject);
        if (wscredential != null) {
            try {
                principal = (Principal) wscredential.get("com.ibm.wsspi.security.cred.jaspi.principal");
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Internal error getting JASPIC Principal from credential", e);
                }
            }
        }
        return principal;
    }

    /**
     * @param principals
     */
    private void multiplePrincipalsError(Set<WSPrincipal> principals) {
        String principalNames = null;
        for (WSPrincipal principal : principals) {
            if (principalNames == null)
                principalNames = principal.getName();
            else
                principalNames = principalNames + ", " + principal.getName();
        }
        throw new IllegalStateException(Tr.formatMessage(tc, "SEC_TOO_MANY_PRINCIPALS", principalNames));
    }

    /**
     * Attempt to retrieve the registry realm for the configured registry.
     * <p>
     * It is possible that no registry is configured. If that is the case,
     * return the default realm name.
     *
     * @return realm name. {@code null} is not returned.
     */
    public String getUserRegistryRealm(AtomicServiceReference<SecurityService> securityServiceRef) {
        String realm = "defaultRealm";
        try {
            SecurityService securityService = securityServiceRef.getService();
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            if (userRegistryService.isUserRegistryConfigured()) {
                realm = userRegistryService.getUserRegistry().getRealm();
            }
        } catch (RegistryException re) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegistryException while trying to get the realm", re);
            }
        }
        return realm;
    }
}
