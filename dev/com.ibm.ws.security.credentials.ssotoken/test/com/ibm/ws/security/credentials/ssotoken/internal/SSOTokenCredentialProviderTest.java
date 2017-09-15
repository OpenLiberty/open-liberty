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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public class SSOTokenCredentialProviderTest {
    protected static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<TokenManager> tokenManagerRef = mock.mock(ServiceReference.class);
    private final TokenManager tokenManager = mock.mock(TokenManager.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);

    private SSOTokenCredentialProvider provider;

    @Before
    public void setUp() throws Exception {

        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(SSOTokenCredentialProvider.KEY_TOKEN_MANAGER, tokenManagerRef);
                will(returnValue(tokenManager));
                allowing(cc).locateService(SSOTokenCredentialProvider.KEY_CREDENTIALS_SERVICE, credentialsServiceRef);
                will(returnValue(credentialsService));
            }
        });

        provider = new SSOTokenCredentialProvider();
        provider.setTokenManager(tokenManagerRef);
        provider.setCredentialsService(credentialsServiceRef);
        provider.activate(cc);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        provider.unsetTokenManager(tokenManagerRef);
        provider.unsetCredentialsService(credentialsServiceRef);
        provider.deactivate(cc);
        provider = null;
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.SSOTokenCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void setCredential_noPrincipals() throws Exception {
        Subject subject = new Subject();
        provider.setCredential(subject);

        assertTrue("Principals should not be altered by the method",
                   subject.getPrincipals().isEmpty());
        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());
        assertTrue("Private credentials should be empty",
                   subject.getPrivateCredentials().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.SSOTokenCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void setCredential_notWSPrincipal() throws Exception {
        Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("notWSPrincipal"));
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     1, subject.getPrincipals().size());
        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());
        assertTrue("Private credentials should be empty",
                   subject.getPrivateCredentials().isEmpty());
    }

    /**
     * Creates and sets a WSPrincipal into the subject.
     * 
     * @param subject
     * @param realm
     * @param securityName
     */
    private void setWSPrincipal(Subject subject, String realm,
                                String securityName, String uniqueName) {
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
                                                      realm,
                                                      uniqueName);
        WSPrincipal wsPrincipal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        subject.getPrincipals().add(wsPrincipal);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.SSOTokenCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test(expected = CredentialException.class)
    public void setCredential_tooManyWSPrincipal() throws Exception {
        Subject subject = new Subject();
        setWSPrincipal(subject, "realm1", "name1", "unique1");
        setWSPrincipal(subject, "realm2", "name2", "unique2");
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     2, subject.getPrincipals().size());
        assertTrue("Public credentials should be empty",
                     subject.getPublicCredentials().isEmpty());
        assertTrue("Private credentials should be empty",
                     subject.getPrivateCredentials().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.SSOTokenCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CredentialException.class)
    public void setCredential_TokenCreationFailedException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(tokenManager).createSSOToken(with(any(Map.class)));
                will(throwException(new TokenCreationFailedException("expected")));
                allowing(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "BasicRealm", "name", "uniqueUserId");
        provider.setCredential(subject);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.SSOTokenCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void setCredentialSubject_someIgnoredPrincipals() throws Exception {
        Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("ignored1"));
        setWSPrincipal(subject, "BasicRealm", "user", "uniqueUserId");
        subject.getPrincipals().add(new SimplePrincipal("ignored2"));

        mock.checking(new Expectations() {
            {
                one(tokenManager).createSSOToken(with(any(Map.class)));
                allowing(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
            }
        });

        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     3, subject.getPrincipals().size());

        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());

        Set<Object> privCredentials = subject.getPrivateCredentials();
        assertEquals("Public credentials should have one entry",
                     1, privCredentials.size());

        Object credential = privCredentials.iterator().next();
        assertTrue("The one credential entry should be a SingleSignonToken",
                   credential instanceof SingleSignonToken);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.SSOTokenCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void setCredential_validAccessId() throws Exception {
        mock.checking(new Expectations() {
            {
                one(tokenManager).createSSOToken(with(any(Map.class)));
                allowing(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "BasicRealm", "user", "uniqueUserId");
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     1, subject.getPrincipals().size());

        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());

        Set<Object> privCredentials = subject.getPrivateCredentials();
        assertEquals("Public credentials should have one entry",
                     1, privCredentials.size());

        Object credential = privCredentials.iterator().next();
        assertTrue("The one credential entry should be a SingleSignonToken",
                   credential instanceof SingleSignonToken);
    }

    @Test
    public void setCredential_validToken() throws Exception {
        final Token ssoLtpaToken = mock.mock(Token.class);
        mock.checking(new Expectations() {
            {
                one(tokenManager).createSSOToken(with(ssoLtpaToken));
                allowing(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "BasicRealm", "user", "uniqueUserId");
        setToken(subject, ssoLtpaToken);
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     1, subject.getPrincipals().size());

        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());

        Set<Object> privCredentials = subject.getPrivateCredentials();
        assertEquals("Public credentials should have one entry",
                     1, privCredentials.size());

        Object credential = privCredentials.iterator().next();
        assertTrue("The one credential entry should be a SingleSignonToken",
                   credential instanceof SingleSignonToken);
    }

    private void setToken(Subject subject, Token ssoLtpaToken) {
        subject.getPrivateCredentials().add(ssoLtpaToken);
    }

    @Test
    public void isSubjectValid() {
        assertTrue("This method is hardcoded to return true",
                   provider.isSubjectValid(new Subject()));
    }

    static class SimplePrincipal implements Principal {

        private final String name;

        public SimplePrincipal(String name) {
            this.name = name;
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
