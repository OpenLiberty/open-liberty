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
package com.ibm.ws.security.authentication.jaas.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.wsspi.security.auth.callback.Constants;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Base test class for the subclasses of CommonLoginModule requiring shared state validation.
 */
public abstract class LoginModuleTester {

    protected Principal principalOverride;
    protected WSCredential wsCredentialOverride;
    protected SingleSignonToken ssoTokenOverride;
    private final Mockery mock = new JUnit4Mockery();

    @Test
    public void loginSetsSharedState() throws Exception {
        Map<String, Object> sharedState = createSharedState();
        ServerCommonLoginModule module = createInitializedModule(null, sharedState);
        module.login();
        assertNotNull("The WSPrincipal must be set in the shared state", sharedState.get(Constants.WSPRINCIPAL_KEY));
        assertNotNull("The WSCredential must be set in the shared state", sharedState.get(Constants.WSCREDENTIAL_KEY));
        assertNotNull("The SSO token must be set in the shared state", sharedState.get(Constants.WSSSOTOKEN_KEY));
    }

    @Test
    public void commitSetsSubject() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();
        ServerCommonLoginModule module = createInitializedModule(subject, sharedState);
        module.login();
        module.commit();
        assertTrue("The subject must contain all the principals in the temporary subject",
                   subject.getPrincipals().containsAll(module.temporarySubject.getPrincipals()));
        assertTrue("The subject must contain all the public credentials in the temporary subject",
                   subject.getPublicCredentials().containsAll(module.temporarySubject.getPublicCredentials()));
        assertTrue("The subject must contain all the private credentials in the temporary subject",
                   subject.getPrivateCredentials().containsAll(module.temporarySubject.getPrivateCredentials()));
        assertSubjectPrincipalsAndCredentialsSize(subject);
    }

    @Test
    public void sharedStateEntriesHavePrecedence() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();
        ServerCommonLoginModule module = createInitializedModule(subject, sharedState);

        module.login();
        sharedState.put(Constants.WSPRINCIPAL_KEY, principalOverride);
        sharedState.put(Constants.WSCREDENTIAL_KEY, wsCredentialOverride);
        sharedState.put(Constants.WSSSOTOKEN_KEY, ssoTokenOverride);

        module.commit();

        assertTrue("The subject must contain the principal override from the shared state",
                   subject.getPrincipals().contains(principalOverride));
        assertTrue("The subject must contain the credential override from the shared state",
                   subject.getPublicCredentials().contains(wsCredentialOverride));
        assertTrue("The subject must contain the SSO token override from the shared state",
                   subject.getPrivateCredentials().contains(ssoTokenOverride));
        assertSubjectPrincipalsAndCredentialsSize(subject);
    }

    private void assertSubjectPrincipalsAndCredentialsSize(Subject subject) {
        assertEquals("The subject must contain only one WSPrincipal",
                     1, subject.getPrincipals(WSPrincipal.class).size());
        assertEquals("The subject must contain only one WSCredential",
                     1, subject.getPublicCredentials(WSCredential.class).size());
    }

    @Test
    public void abortCleansSubject() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();
        ServerCommonLoginModule module = createInitializedModule(subject, sharedState);
        module.login();
        module.commit();
        module.abort();

        assertTrue("The subject must not contain any principals", subject.getPrincipals().isEmpty());
        assertTrue("The subject must not contain any public credentials", subject.getPublicCredentials().isEmpty());
        assertTrue("The subject must not contain any private credentials", subject.getPrivateCredentials().isEmpty());
    }

    protected abstract ServerCommonLoginModule createInitializedModule(Subject subject, Map<String, Object> sharedState) throws Exception;

    protected abstract Map<String, Object> createSharedState();
}
