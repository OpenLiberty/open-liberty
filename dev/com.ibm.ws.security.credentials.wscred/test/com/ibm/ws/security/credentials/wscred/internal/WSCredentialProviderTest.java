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
package com.ibm.ws.security.credentials.wscred.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
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

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WSCredentialProviderTest {
    protected static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mock.mock(ServiceReference.class);
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);
    private final String urType = "BASIC";

    private WSCredentialProvider provider;

    @Before
    public void setUp() throws Exception {

        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(WSCredentialProvider.KEY_USER_REGISTYR_SERVICE, userRegistryServiceRef);
                will(returnValue(userRegistryService));

                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));

                allowing(cc).locateService(WSCredentialProvider.KEY_CREDENTIALS_SERVICE, credentialsServiceRef);
                will(returnValue(credentialsService));
            }
        });

        provider = new WSCredentialProvider();
        provider.setUserRegistryService(userRegistryServiceRef);
        provider.setCredentialsService(credentialsServiceRef);
        provider.activate(cc);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        provider.unsetUserRegistryService(userRegistryServiceRef);
        provider.unsetCredentialsService(credentialsServiceRef);
        provider.deactivate(cc);
        provider = null;
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject)}.
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
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject)}.
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
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject)}.
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
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test
    public void setCredential_noConfiguredRegistry() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(false));
                one(userRegistryService).getUserRegistryType();
                will(returnValue("UNKNOWN"));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "BasicRealm", "user", "uniqueUserId");
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     1, subject.getPrincipals().size());
        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());
        assertTrue("Private credentials should be empty",
                   subject.getPrivateCredentials().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test
    public void setCredential_invalidAccessId() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));
            }
        });

        Subject subject = new Subject();
        WSPrincipal wsPrincipal = new WSPrincipal("bob", "bobID", WSPrincipal.AUTH_METHOD_PASSWORD);
        subject.getPrincipals().add(wsPrincipal);
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     1, subject.getPrincipals().size());
        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());
        assertTrue("Private credentials should be empty",
                   subject.getPrivateCredentials().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test
    public void setCredential_groupAccessId() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));
            }
        });

        Subject subject = new Subject();
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP,
                                                      "BasicRealm",
                                                      "uniqueGroupId");
        WSPrincipal wsPrincipal = new WSPrincipal("group", accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        subject.getPrincipals().add(wsPrincipal);
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     1, subject.getPrincipals().size());
        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());
        assertTrue("Private credentials should be empty",
                   subject.getPrivateCredentials().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test
    public void setCredential_nonMatchingRealm() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistry).getRealm();
                will(returnValue("BasicRealm"));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "InvalidRealm", "name", "uniqueUserId");
        provider.setCredential(subject);

        assertEquals("Principals should not be altered by the method",
                     1, subject.getPrincipals().size());
        assertTrue("Public credentials should be empty",
                   subject.getPublicCredentials().isEmpty());
        assertTrue("Private credentials should be empty",
                   subject.getPrivateCredentials().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test(expected = CredentialException.class)
    public void setCredential_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));

                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistry).getRealm();
                will(returnValue("BasicRealm"));
                one(userRegistry).getUniqueGroupIdsForUser("uniqueUserId");
                will(throwException(new RegistryException("expected")));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "BasicRealm", "name", "uniqueUserId");
        provider.setCredential(subject);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test(expected = CredentialException.class)
    public void setCredential_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));

                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistry).getRealm();
                will(returnValue("BasicRealm"));
                one(userRegistry).getUniqueGroupIdsForUser("uniqueUserId");
                will(throwException(new EntryNotFoundException("expected")));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "BasicRealm", "name", "uniqueUserId");
        provider.setCredential(subject);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void setCredentialSubject_someIgnoredPrincipals() throws Exception {
        Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("ignored1"));
        setWSPrincipal(subject, "BasicRealm", "user", "uniqueUserId");
        subject.getPrincipals().add(new SimplePrincipal("ignored2"));

        final List<String> groups = new ArrayList<String>();
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));

                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistry).getRealm();
                will(returnValue("BasicRealm"));
                one(userRegistry).getUniqueGroupIdsForUser("uniqueUserId");
                will(returnValue(groups));
            }
        });

        provider.setCredential(subject);

        Set<Object> pubCredentials = subject.getPublicCredentials();
        assertEquals("Public credentials should have one entry",
                     1, pubCredentials.size());

        Object credential = pubCredentials.iterator().next();
        assertTrue("The one credential entry should be a WSCredential",
                   credential instanceof WSCredential);

        Set<Object> privCredentials = subject.getPrivateCredentials();
        assertTrue("Private credentials should be empty",
                   privCredentials.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test
    public void setCredential_validAccessId() throws Exception {
        final List<String> groups = new ArrayList<String>();
        groups.add("primaryGroup");
        groups.add("secondaryGroup");
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));

                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistry).getRealm();
                will(returnValue("BasicRealm"));
                one(userRegistry).getUniqueGroupIdsForUser("uniqueUserId");
                will(returnValue(groups));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, "BasicRealm", "user", "uniqueUserId");
        provider.setCredential(subject);

        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
                                                      "BasicRealm",
                                                      "uniqueUserId");

        Set<Object> pubCredentials = subject.getPublicCredentials();
        assertEquals("Public credentials should have one entry",
                     1, pubCredentials.size());

        Object credential = pubCredentials.iterator().next();
        assertTrue("The one credential entry should be a WSCredential",
                   credential instanceof WSCredential);
        WSCredential wsCredential = (WSCredential) credential;
        assertEquals("The WSCredential realm should come from the accessId",
                     "BasicRealm", wsCredential.getRealmName());
        assertEquals("The WSCredential securityName should come from the accessId",
                     "user", wsCredential.getSecurityName());
        assertEquals("The WSCredential realm should be the uniqueId for the securityName",
                     "uniqueUserId", wsCredential.getUniqueSecurityName());
        assertEquals("The WSCredential primaryGroup should come from the uniqueId",
                     "group:BasicRealm/primaryGroup", wsCredential.getPrimaryGroupId());
        assertEquals("The WSCredential accessId should be the one specified",
                     accessId, wsCredential.getAccessId());

        final List<String> uniqueGroupAccessIds = new ArrayList<String>();
        uniqueGroupAccessIds.add("group:BasicRealm/primaryGroup");
        uniqueGroupAccessIds.add("group:BasicRealm/secondaryGroup");
        assertEquals("The WSCredential groupIds should come from the uniqueId",
                     uniqueGroupAccessIds, wsCredential.getGroupIds());

        Set<Object> privCredentials = subject.getPrivateCredentials();
        assertTrue("Private credentials should be empty",
                   privCredentials.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test
    public void setCredential_unauthenticatedAccessId() throws Exception {
        mock.checking(new Expectations() {
            {
                one(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));
            }
        });

        Subject subject = new Subject();
        setWSPrincipal(subject, UNAUTHENTICATED, UNAUTHENTICATED, UNAUTHENTICATED);
        provider.setCredential(subject);

        Set<Object> pubCredentials = subject.getPublicCredentials();
        assertEquals("Public credentials should have one entry",
                     1, pubCredentials.size());

        Object credential = pubCredentials.iterator().next();
        assertTrue("The one credential entry should be a WSCredential",
                   credential instanceof WSCredential);
        WSCredential wsCredential = (WSCredential) credential;
        assertTrue("WSCredential should represent an unauthenticated subject",
                   wsCredential.isUnauthenticated());

        Set<Object> privCredentials = subject.getPrivateCredentials();
        assertTrue("Private credentials should be empty",
                   privCredentials.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.internal.WSCredentialProvider#setCredential(javax.security.auth.Subject, java.lang.String)}.
     */
    @Test
    public void setCredential_serverAccessId() throws Exception {
        Subject subject = new Subject();
        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue(urType));
            }
        });
        String serverDN = "cn=myServer,l=%2Fwlp%2Fusr,l=myHost,ou=member,o=UUID,dc=Atlas";
        String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_SERVER,
                                                      "Atlas",
                                                      serverDN);
        WSPrincipal wsPrincipal = new WSPrincipal(serverDN, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        subject.getPrincipals().add(wsPrincipal);
        provider.setCredential(subject);

        Set<Object> pubCredentials = subject.getPublicCredentials();
        assertEquals("Public credentials should have one entry",
                     1, pubCredentials.size());

        Object credential = pubCredentials.iterator().next();
        assertTrue("The one credential entry should be a WSCredential",
                   credential instanceof WSCredential);
        WSCredential wsCredential = (WSCredential) credential;
        assertEquals("The WSCredential realm should come from the accessId",
                     "Atlas", wsCredential.getRealmName());
        assertEquals("The WSCredential securityName should come from the accessId",
                     serverDN, wsCredential.getSecurityName());
        assertEquals("The WSCredential realm should be the uniqueId for the securityName",
                     serverDN, wsCredential.getUniqueSecurityName());
        assertEquals("The WSCredential accessId should be the one specified",
                     accessId, wsCredential.getAccessId());

        Set<Object> privCredentials = subject.getPrivateCredentials();
        assertTrue("Private credentials should be empty",
                   privCredentials.isEmpty());
    }

    /**
     * Test subject with credential that does not expire
     */
    @Test
    public void isSubjectValid_noWSCredential() throws Exception {
        assertFalse("The subject must have a WSCredential.",
                    provider.isSubjectValid(new Subject()));
    }

    /**
     * Test subject with credential that does not expire
     */
    @Test
    public void isSubjectValid_CredentialThatDoesNotExpire() throws Exception {
        WSCredentialImpl credential = createTestWSCredential();
        Subject subject = new Subject();
        subject.getPublicCredentials().add(credential);
        assertTrue("The subject must be valid.", provider.isSubjectValid(subject));
    }

    /**
     * Test subject with credential that is expired
     */
    @Test
    public void isSubjectValid_ExpiredCredential() throws Exception {
        WSCredentialImpl credential = createTestWSCredential();
        long expirationInMilliseconds = 1000L; // 1 second after epoch, expired a long time ago.
        credential.setExpiration(expirationInMilliseconds);
        Subject subject = new Subject();
        subject.getPublicCredentials().add(credential);
        assertFalse("The subject must be invalid.", provider.isSubjectValid(subject));
    }

    private WSCredentialImpl createTestWSCredential() {
        final List<String> roles = new ArrayList<String>();
        final List<String> groupIds = new ArrayList<String>();
        WSCredentialImpl credential = new WSCredentialImpl("real", "securityName", "uniqueSecurityName", "UNAUTHENTICATED", "primaryGroupId", "accessId", roles, groupIds);
        return credential;
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
