/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SETUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.saml2.Saml20Attribute;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.saml2.UserCredentialResolver;
import com.ibm.wsspi.security.saml2.UserIdentityException;

import test.common.SharedOutputManager;

@SuppressWarnings("unchecked")
public class AssertionToSubjectTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final SsoRequest samlRequest = common.getSsoRequest();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final Saml20Token sso20Token = common.getSso20Token();
    private static final States stateMachine = common.getStateMachine();
    private static final UserCredentialResolver usrCredResolver = mockery.mock(UserCredentialResolver.class, "usrCredResolver");
    private static final Saml20Attribute saml20Attribute = mockery.mock(Saml20Attribute.class, "saml20Attribute");

    private static final List<Saml20Attribute> attributes = mockery.mock(List.class, "attributes");
    private static final List<String> newGroups = mockery.mock(List.class, "newGroups");
    private static final ServiceReference<WSSecurityService> wsSecurityServiceRef = mockery.mock(ServiceReference.class, "wsSecurityServiceRef");
    private static final ConcurrentServiceReferenceMap<String, UserCredentialResolver> activatedUserResolverRef = mockery.mock(ConcurrentServiceReferenceMap.class,
                                                                                                                               "activatedUserResolverRef");
    private static final Iterator<UserCredentialResolver> iterator = mockery.mock(Iterator.class, "iterator");
    private static final Iterator<String> group = mockery.mock(Iterator.class, "group");
    private static final ListIterator<String> listIterator = mockery.mock(ListIterator.class, "listIterator");
    private static final WSSecurityService wsSecurityService = mockery.mock(WSSecurityService.class, "wsSecurityService");
    private static final UserRegistry userRegistry = mockery.mock(UserRegistry.class, "userRegistry");

    private static final AssertionToSubject assertionToSubject = new AssertionToSubject(samlRequest, ssoConfig, sso20Token);
    private static final UserIdentityException ue = new UserIdentityException();
    static ConcurrentServiceReferenceMap<String, UserCredentialResolver> instance;

    @BeforeClass
    public static void setUp() {
        stateMachine.startsAs(SETUP);
        outputMgr.trace("*=all");
        instance = AssertionToSubject.activatedUserResolverRef;

        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));
                when(stateMachine.is(SETUP));

                allowing(activatedUserResolverRef).getServices();
                will(returnValue(iterator));

                allowing(sso20Token).getSAMLAttributes();
                will(returnValue(attributes));

                allowing(attributes).iterator();
                will(returnValue(iterator));
            }
        });

        AssertionToSubject.setActivatedUserResolverRef(activatedUserResolverRef);
    }

    @AfterClass
    public static void tearDown() {
        AssertionToSubject.setActivatedUserResolverRef(instance);
        outputMgr.trace("*=all=disabled");
    }

    @Before
    public void beforeTest() {
        stateMachine.become("runtime");
    }

    @Test
    public void testGetUser() throws UserIdentityException {
        final String USER_ID = "userid";
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(1));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(usrCredResolver).mapSAMLAssertionToUser(sso20Token);
                will(returnValue(USER_ID));
            }
        });

        try {
            String user = assertionToSubject.getUser();
            assertTrue("The expected user id '" + USER_ID + "' was not received.", user == USER_ID);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testGetUser_NullUserIdentifier() throws UserIdentityException {
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(sso20Token).getSAMLNameID();
                will(returnValue(null));

                one(ssoConfig).getUserIdentifier();
                will(returnValue(null));
            }
        });

        try {
            assertionToSubject.getUser();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetUser_NullSAMLNameID() {
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(sso20Token).getSAMLNameID();
                will(returnValue(null));

                one(ssoConfig).getUserIdentifier();
                will(returnValue("user"));

                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            assertionToSubject.getUser();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetUserFromUserResolver() throws UserIdentityException {
        mockery.checking(new Expectations() {
            {
                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(usrCredResolver).mapSAMLAssertionToUser(sso20Token);
                will(throwException(ue));
            }
        });
        try {
            assertionToSubject.getUserFromUserResolver(null);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }

    }

    @Test
    public void testGetRealm() throws UserIdentityException {
        final String REALM = "realm.test";
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(1));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(usrCredResolver).mapSAMLAssertionToRealm(sso20Token);
                will(returnValue(REALM));
            }
        });

        try {
            String realm = assertionToSubject.getRealm();
            assertTrue("The expected realm '" + REALM + "' was not received.", realm == REALM);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testGetRealm_ExistentRealm() {
        final String REALM = "realm.test";
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(ssoConfig).getRealmName();
                will(returnValue(REALM));
            }
        });

        try {
            String realm = assertionToSubject.getRealm();
            assertTrue("The expected realm '" + REALM + "' was not received.", realm == REALM);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testGetRealm_NullRealmIdentifier() {
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(sso20Token).getSAMLIssuerName();
                will(returnValue(null));

                one(ssoConfig).getRealmIdentifier();
                will(returnValue(null));
                one(ssoConfig).getRealmName();
                will(returnValue(null));
            }
        });

        try {
            assertionToSubject.getRealm();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetRealm_NullSAMLIssuerName() {
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(sso20Token).getSAMLIssuerName();
                will(returnValue(null));

                one(ssoConfig).getRealmIdentifier();
                will(returnValue("realm"));
                one(ssoConfig).getRealmName();
                will(returnValue(null));

                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            assertionToSubject.getRealm();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetRealmFromUserResolver() throws UserIdentityException {
        mockery.checking(new Expectations() {
            {
                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(usrCredResolver).mapSAMLAssertionToRealm(sso20Token);
                will(throwException(ue));
            }
        });

        try {
            assertionToSubject.getRealmFromUserResolver();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetUserUniqueIdentity() throws UserIdentityException {
        final String UID = "87F4SN5PWR";
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(1));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(usrCredResolver).mapSAMLAssertionToUserUniqueID(sso20Token);
                will(returnValue(UID));
            }
        });

        try {
            String uid = assertionToSubject.getUserUniqueIdentity("user", null);
            assertTrue("The expected uid '" + UID + "' was not received.", uid == UID);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testGetUserUniqueIdentity_NullUserUniqueIdentifier() {
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(ssoConfig).getUserUniqueIdentifier();
                will(returnValue(null));

            }
        });

        try {
            assertionToSubject.getUserUniqueIdentity(null, null);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetUserUniqueIdentity_NullUID() {
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(ssoConfig).getUserUniqueIdentifier();
                will(returnValue("name"));

                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            assertionToSubject.getUserUniqueIdentity(null, null);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetGroupUniqueIdentityFromRegistry() throws UserIdentityException {
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(1));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(usrCredResolver).mapSAMLAssertionToGroups(sso20Token);
                will(returnValue(newGroups));

                one(newGroups).size();
                will(returnValue(1));
                one(newGroups).iterator();
                will(returnValue(group));
                one(newGroups).toArray(); // called as part of trace
                will(returnValue(new Object[] { group }));

                one(group).hasNext();
                will(returnValue(false));
            }
        });

        try {
            List<String> list = assertionToSubject.getGroupUniqueIdentityFromRegistry("realm");
            assertTrue("The resulted list is not empty.", list.isEmpty());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testGetGroupUniqueIdentityFromRegistry_GroupIdentifierNotNull() throws WSSecurityException, RemoteException {
        final ComponentContext componetContext = mockery.mock(ComponentContext.class, "componetContext");
        final RegistryHelper helper = new RegistryHelper();
        final String NAME = "name";
        final String IDP_GROUP = "idpGroup";
        final String GROUP_DN = "groupDN";

        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(ssoConfig).getGroupIdentifier();
                will(returnValue(NAME));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).hasNext();
                will(returnValue(false));
                one(iterator).next();
                will(returnValue(saml20Attribute));

                one(saml20Attribute).getName();
                will(returnValue(NAME));
                atMost(2).of(saml20Attribute).getValuesAsString();
                will(returnValue(newGroups));

                one(newGroups).isEmpty();
                will(returnValue(false));
                one(newGroups).iterator();
                will(returnValue(group));
                one(newGroups).toArray(); // called as part of trace
                will(returnValue(new Object[] { group }));

                one(group).hasNext();
                will(returnValue(true));
                one(group).hasNext();
                will(returnValue(false));
                one(group).next();
                will(returnValue(IDP_GROUP));

                one(componetContext).locateService("wsSecurityService", wsSecurityServiceRef);
                will(returnValue(wsSecurityService));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(userRegistry));

                one(userRegistry).getUniqueGroupId(IDP_GROUP);
                will(returnValue(GROUP_DN));
                one(userRegistry).getUniqueGroupIds(GROUP_DN);
                will(returnValue(newGroups));

                one(newGroups).listIterator();
                will(returnValue(listIterator));

                one(listIterator).hasNext();
                will(returnValue(true));
                one(listIterator).hasNext();
                will(returnValue(false));

                one(listIterator).next();
                will(returnValue(GROUP_DN));
            }
        });

        try {
            helper.setWsSecurityService(wsSecurityServiceRef);
            helper.activate(componetContext);

            List<String> groups = assertionToSubject.getGroupUniqueIdentityFromRegistry("realm");
            assertTrue("The expected list was not received.", groups.size() > 0 && groups.get(0).contains(GROUP_DN));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testGetGroupsFromUserResolver() throws UserIdentityException {
        mockery.checking(new Expectations() {
            {
                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(newGroups).toArray(); // called as part of trace
                will(returnValue(new Object[] { group }));

                one(usrCredResolver).mapSAMLAssertionToGroups(sso20Token);
                will(throwException(ue));
            }
        });
        try {
            assertionToSubject.getGroupsFromUserResolver();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetGroupUniqueIdentity() throws UserIdentityException {
        final String GROUP = "group";
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(1));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(usrCredResolver));

                one(usrCredResolver).mapSAMLAssertionToGroups(sso20Token);
                will(returnValue(newGroups));

                one(newGroups).size();
                will(returnValue(1));
                one(newGroups).iterator();
                will(returnValue(group));
                one(newGroups).toArray(); // called as part of trace
                will(returnValue(new Object[] { group }));

                one(group).hasNext();
                will(returnValue(true));
                one(group).hasNext();
                will(returnValue(false));
                one(group).next();
                will(returnValue(GROUP));
            }
        });

        try {
            List<String> groups = assertionToSubject.getGroupUniqueIdentity("realm");
            assertTrue("The expected list was not received.", groups.size() > 0 && groups.get(0).contains(GROUP));
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }

    }

    @Test
    public void testGetGroupUniqueIdentity_ExistentGroupIdentifier() {
        final String IDENTIFIER = "group_identifier";
        final String GROUP_DN = "groupDN ";
        mockery.checking(new Expectations() {
            {
                one(activatedUserResolverRef).size();
                will(returnValue(0));

                one(ssoConfig).getGroupIdentifier();
                will(returnValue(IDENTIFIER));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(saml20Attribute));

                one(saml20Attribute).getName();
                will(returnValue(IDENTIFIER));
                atMost(2).of(saml20Attribute).getValuesAsString();
                will(returnValue(newGroups));

                one(newGroups).isEmpty();
                will(returnValue(false));
                one(newGroups).iterator();
                will(returnValue(group));
                one(newGroups).toArray(); // called as part of trace
                will(returnValue(new Object[] { group }));

                one(group).hasNext();
                will(returnValue(true));
                one(group).hasNext();
                will(returnValue(false));
                one(group).next();
                will(returnValue(GROUP_DN));
            }
        });

        try {
            List<String> groups = assertionToSubject.getGroupUniqueIdentity("realm");
            assertTrue("The expected list was not received.", groups.size() > 0 && groups.get(0).contains(GROUP_DN));
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testGetCustomCacheKeyValue() {
        final String PROVIDER_NAME = "providerName";
        mockery.checking(new Expectations() {
            {
                one(samlRequest).isDisableLtpaCookie();
                will(returnValue(false));
                one(samlRequest).setSpCookieValue(with(any(String.class)));

                one(newGroups).toArray(); // called as part of trace
                will(returnValue(new Object[] { group }));

                one(sso20Token).getSAMLAsString();
                will(returnValue("SAML_string"));
            }
        });

        String cacheKeyValue = assertionToSubject.getCustomCacheKeyValue(PROVIDER_NAME);
        assertTrue("The cacheKeyValue does not start with the string " + PROVIDER_NAME + ".", cacheKeyValue.startsWith(PROVIDER_NAME));
    }
}
