/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.credentials.ssotoken.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public class SSOTokenCredentialProvider implements CredentialProvider {

    static final String KEY_TOKEN_MANAGER = "tokenManager";
    public static final String KEY_CREDENTIALS_SERVICE = "credentialsService";
    private final AtomicServiceReference<TokenManager> tokenManagerRef = new AtomicServiceReference<TokenManager>(KEY_TOKEN_MANAGER);
    private final AtomicServiceReference<CredentialsService> credentialsServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIALS_SERVICE);

    protected void setTokenManager(ServiceReference<TokenManager> reference) {
        tokenManagerRef.setReference(reference);
    }

    protected void unsetTokenManager(ServiceReference<TokenManager> reference) {
        tokenManagerRef.unsetReference(reference);
    }

    public void setCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialsServiceRef.setReference(ref);
    }

    public void unsetCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialsServiceRef.unsetReference(ref);
    }

    protected void activate(ComponentContext cc) {
        tokenManagerRef.activate(cc);
        credentialsServiceRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        tokenManagerRef.deactivate(cc);
        credentialsServiceRef.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public void setCredential(Subject subject) throws CredentialException {
        Set<WSPrincipal> principals = subject.getPrincipals(WSPrincipal.class);
        if (principals.isEmpty()) {
            return;
        }
        if (principals.size() != 1) {
            throw new CredentialException("Too many WSPrincipals in the subject");
        }
        WSPrincipal principal = principals.iterator().next();
        CredentialsService cs = credentialsServiceRef.getService();
        String unauthenticatedUserid = cs.getUnauthenticatedUserid();
        if (principal.getName() != null && unauthenticatedUserid != null &&
            principal.getName().equals(unauthenticatedUserid)) {
            return;
        }
        setSsoTokenCredential(subject, principal.getAccessId());
    }

    /**
     * Create an SSO token for the specified accessId.
     * 
     * @param subject
     * @param principalAccessId
     * @throws TokenCreationFailedException
     */
    private void setSsoTokenCredential(Subject subject, String principalAccessId)
                    throws CredentialException {
        try {
            TokenManager tokenManager = tokenManagerRef.getService();
            SingleSignonToken ssoToken = null;
            Set<Token> tokens = subject.getPrivateCredentials(Token.class);
            if (tokens.isEmpty() == false) {
                Token ssoLtpaToken = tokens.iterator().next();
                subject.getPrivateCredentials().remove(ssoLtpaToken);
                ssoToken = tokenManager.createSSOToken(ssoLtpaToken);
            } else {
                Map<String, Object> tokenData = new HashMap<String, Object>();
                tokenData.put("unique_id", principalAccessId);
                ssoToken = tokenManager.createSSOToken(tokenData);
            }

            subject.getPrivateCredentials().add(ssoToken);
        } catch (TokenCreationFailedException e) {
            throw new CredentialException(e.getLocalizedMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSubjectValid(Subject subject) {
        return true;
    }

}
