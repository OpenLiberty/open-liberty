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
package com.ibm.ws.security.authorization.builtin.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authorization.AccessDecisionService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.context.SubjectManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class BuiltinAuthorizationServiceTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final static String resourceName = "myApp";
    private final static String authMethod = WSPrincipal.AUTH_METHOD_PASSWORD;
    private final static String role1 = "Manager";
    private final static String subject1 = "user1";
    private final static String accessId1 = "user:BasicRealm/user1";
    private final static String groupAccessId1 = "group:realm/group1";
    private final static String roleAllAuthen = "Employee";

    private final Mockery context = new JUnit4Mockery();
    private final ComponentContext cc = context.mock(ComponentContext.class);
    private final ServiceReference<AccessDecisionService> accessDecisionServiceRef = context.mock(ServiceReference.class, "accessDecisionServiceRef");
    private final AccessDecisionService accessDecisionService = context.mock(AccessDecisionService.class);
    private final ServiceReference<AuthorizationTableService> authzTableServiceRef = context.mock(ServiceReference.class, "authzTableServiceRef");
    private final AuthorizationTableService authzTableService = context.mock(AuthorizationTableService.class, "authzTableService");
    private final ServiceReference<AuthorizationTableService> authzTableService2Ref = context.mock(ServiceReference.class, "authzTableService2Ref");
    private final AuthorizationTableService authzTableService2 = context.mock(AuthorizationTableService.class, "authzTableService2");
    private final ServiceReference<AuthorizationTableService> authzTableService3Ref = context.mock(ServiceReference.class, "authzTableService3Ref");
    private final AuthorizationTableService authzTableService3 = context.mock(AuthorizationTableService.class, "authzTableService3");
    private final WSCredential wsCred = context.mock(WSCredential.class);
    private final Set<String> requiredRoles = new HashSet<String>();
    private final SubjectManager subjManager = new SubjectManager();
    private RoleSet assignedRoles;
    private BuiltinAuthorizationService builtinAuthz;

    @Before
    public void setUp() {
        context.checking(new Expectations() {
            {
                allowing(accessDecisionServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(accessDecisionServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService(BuiltinAuthorizationService.KEY_ACCESS_DECISION_SERVICE, accessDecisionServiceRef);
                will(returnValue(accessDecisionService));

                allowing(authzTableServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(authzTableServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService(BuiltinAuthorizationService.KEY_AUTHORIZATION_TABLE_SERVICE, authzTableServiceRef);
                will(returnValue(authzTableService));

                allowing(authzTableService2Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(2L));
                allowing(authzTableService2Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService(BuiltinAuthorizationService.KEY_AUTHORIZATION_TABLE_SERVICE, authzTableService2Ref);
                will(returnValue(authzTableService2));

                allowing(authzTableService3Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(3L));
                allowing(authzTableService3Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService(BuiltinAuthorizationService.KEY_AUTHORIZATION_TABLE_SERVICE, authzTableService3Ref);
                will(returnValue(authzTableService3));
                allowing(authzTableService).isAuthzInfoAvailableForApp("myApp");
                will(returnValue(true));

            }
        });

        builtinAuthz = new BuiltinAuthorizationService();
        builtinAuthz.setAccessDecisionService(accessDecisionServiceRef);
        builtinAuthz.setAuthorizationTableService(authzTableServiceRef);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("useRoleAsGroupName", Boolean.TRUE);
        builtinAuthz.activate(cc, props);
    }

    @After
    public void tearDown() throws Exception {
        builtinAuthz.deactivate(cc);
        builtinAuthz.unsetAccessDecisionService(accessDecisionServiceRef);
        builtinAuthz.unsetAuthorizationTableService(authzTableServiceRef);

        context.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_trueForUser() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        requiredRoles.add(role1);
        assignedRoles = new RoleSet(requiredRoles);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getAccessId();
                will(returnValue(accessId1));

                one(authzTableService).getRolesForAccessId(resourceName, accessId1);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
                will(returnValue(Boolean.TRUE));
            }
        });

        assertTrue("isAuthorized should return true",
                   builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_trueForGroup() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        final List<String> groupIds = new ArrayList<String>();
        groupIds.add("group:realm/group1");
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        requiredRoles.add(role1);
        assignedRoles = new RoleSet(requiredRoles);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getAccessId();
                will(returnValue(accessId1));

                one(authzTableService).getRolesForAccessId(resourceName, accessId1);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getGroupIds();
                will(returnValue(groupIds));

                one(authzTableService).getRolesForAccessId(resourceName, groupAccessId1);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
                will(returnValue(Boolean.TRUE));
            }
        });

        assertTrue("isAuthorized should return true because group has access",
                   builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     */
    @Test
    public void testIsAuthorized_trueEmptyRequiredRoles() {
        assertTrue("isAuthorized should return true when requiredRoles is empty.",
                   builtinAuthz.isAuthorized(resourceName, new ArrayList<String>(), null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     */
    @Test
    public void testIsAuthorized_trueEveryone() {
        requiredRoles.add(role1);
        assignedRoles = new RoleSet(requiredRoles);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, null);
                will(returnValue(Boolean.TRUE));
            }
        });

        assertTrue("isAuthorized should return true when requiredRole is mapped to Everyone",
                   builtinAuthz.isAuthorized(resourceName, requiredRoles, null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     */
    @Test
    public void testIsAuthorized_trueAllAuthenticated() {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        requiredRoles.add(roleAllAuthen);
        assignedRoles = new RoleSet(requiredRoles);

        context.checking(new Expectations() {
            {
                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
                will(returnValue(Boolean.TRUE));
            }
        });

        assertTrue("isAuthorized should return true when the authenticated subject is mapped to ALL_AUTHENTICATED_USERS",
                   builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     */
    @Test
    public void testIsAuthorized_trueSubjectFromThread() {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        requiredRoles.add(roleAllAuthen);
        assignedRoles = new RoleSet(requiredRoles);

        subjManager.setCallerSubject(subject);
        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
                will(returnValue(Boolean.TRUE));
            }
        });

        assertTrue("isAuthorized should return true when getting the subject from the thread",
                   builtinAuthz.isAuthorized(resourceName, requiredRoles,
                                             null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_false() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        final List<String> groupIds = new ArrayList<String>();
        groupIds.add("group:realm/group1");
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials,
                        privCredentials);

        requiredRoles.add(role1);
        assignedRoles = new RoleSet(requiredRoles);

        context.checking(new Expectations() {
            {

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getAccessId();
                will(returnValue(accessId1));

                one(authzTableService).getRolesForAccessId(resourceName, accessId1);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getGroupIds();
                will(returnValue(groupIds));

                one(authzTableService).getRolesForAccessId(resourceName, groupAccessId1);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
                will(returnValue(Boolean.FALSE));
            }
        });

        assertFalse("isAuthorized should return false",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_falseWSCredThrowsCredentialExpiredException() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials,
                        privCredentials);

        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getAccessId();
                will(throwException(new CredentialExpiredException("expected")));
                one(wsCred).getGroupIds();
                will(throwException(new CredentialExpiredException("expected")));

                one(authzTableService).getRolesForAccessId(resourceName, null);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

            }
        });

        assertFalse("isAuthorized should return false when getAccessId throws exception",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_falseWSCredThrowsCredentialDestroyedException() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials,
                        privCredentials);

        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getAccessId();
                will(throwException(new CredentialDestroyedException("expected")));
                one(wsCred).getGroupIds();
                will(throwException(new CredentialDestroyedException("expected")));

                one(authzTableService).getRolesForAccessId(resourceName, null);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));

            }
        });

        assertFalse("isAuthorized should return false when getAccessId throws exception",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_falseNullSubject() throws Exception {
        requiredRoles.add(role1);

        subjManager.setCallerSubject(null);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));
            }
        });

        assertFalse("isAuthorized should return false when the subject is null",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_unknownResource() throws Exception {
        requiredRoles.add(role1);

        subjManager.setCallerSubject(null);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(null));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, null, null);
                will(returnValue(Boolean.FALSE));
            }
        });

        assertFalse("isAuthorized should return false when the subject is null",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     */
    @Test
    public void testIsAuthorized_falseNoWSCred() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials,
                        privCredentials);

        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));
            }
        });

        assertFalse("isAuthorized should return false when the subject has no wsCred",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_falseUnauthenticatedSubject() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials,
                        privCredentials);

        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.TRUE));
            }
        });

        assertFalse("isAuthorized should return false when wsCred is unauthenticated",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_falseBasicAuthSubject() throws Exception {
        Set<Principal> principals = new HashSet<Principal>();
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));

                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.TRUE));
            }
        });

        assertFalse("isAuthorized should return false when wsCred is a basic auth credential",
                    builtinAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     */
    @Test
    public void testIsAuthorized_nullResource() {
        try {
            builtinAuthz.isAuthorized(null, requiredRoles, null);
            fail("isAuthorized with null resource should throw a NullPointerException");
        } catch (NullPointerException e) {
            // expected if null resource name is specified
            assertTrue("Exception should contain message stating null: " + e, e
                            .getMessage().contains("resource"));
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     */
    @Test
    public void testIsAuthorized_nullRequiredRoles() {
        try {
            builtinAuthz.isAuthorized(resourceName, null, null);
            fail("isAuthorized with null required roles should throw a NullPointerException");
        } catch (NullPointerException e) {
            // expected if null required roles is specified
            assertTrue(
                       "Exception should contain message stating null: "
                                       + e.getMessage(),
                       e.getMessage().contains("requiredRoles"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_true() {
        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, null);
                will(returnValue(Boolean.TRUE));
            }
        });

        assertTrue("isEveryoneGranted should return true for Employee",
                   builtinAuthz.isEveryoneGranted(resourceName, requiredRoles));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_true_emptyRequiredRoles() {
        assertTrue("isEveryoneGranted should return true when requiredRoles is empty.",
                   builtinAuthz.isEveryoneGranted(resourceName, new ArrayList<String>()));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_false() {
        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, null);
                will(returnValue(Boolean.FALSE));
            }
        });
        assertFalse("isEveryoneGranted should return false for Manager",
                    builtinAuthz.isEveryoneGranted(resourceName, requiredRoles));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_unknownResource() {
        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(null));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, null, null);
                will(returnValue(Boolean.FALSE));
            }
        });
        assertFalse("isEveryoneGranted should return false for Manager",
                    builtinAuthz.isEveryoneGranted(resourceName, requiredRoles));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_falseForAssignedRole() {
        Set<String> parsedRoles = new HashSet<String>();
        assignedRoles = new RoleSet(parsedRoles);

        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, null);
                will(returnValue(Boolean.FALSE));
            }
        });

        assertFalse("isEveryoneGranted should return false for Manager",
                    builtinAuthz.isEveryoneGranted(resourceName, requiredRoles));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_nullResource() {
        try {
            builtinAuthz.isEveryoneGranted(null, requiredRoles);
            fail("isEveryoneGranted with null resource should throw a NullPointerException");
        } catch (NullPointerException e) {
            // expected if null resource name is specified
            assertTrue("Exception should contain message stating null: " + e, e
                            .getMessage().contains("resource"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_nullRequiredRoles() {
        try {
            builtinAuthz.isEveryoneGranted(resourceName, null);
            fail("isEveryoneGranted with null required roles should throw a NullPointerException");
        } catch (NullPointerException e) {
            // expected if null required roles is specified
            assertTrue(
                       "Exception should contain message stating null: "
                                       + e.getMessage(),
                       e.getMessage().contains("requiredRoles"));
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAllAuthenticatedGranted(java.lang.Object, java.util.List, javax.security.auth.Subject)}
     * .
     */
    @Test
    public void testIsAllAuthenticatedGranted_true() {
        final Subject subject = new Subject();
        requiredRoles.add(roleAllAuthen);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
                will(returnValue(Boolean.TRUE));
            }
        });

        assertTrue("isAllAuthenticatedGranted should return true",
                   builtinAuthz.isAllAuthenticatedGranted(resourceName,
                                                          requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAllAuthenticatedGranted(java.lang.Object, java.util.List, javax.security.auth.Subject)}
     * .
     */
    @Test
    public void testIsAllAuthenticatedGranted_falseNoAssignedRole() {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials,
                        privCredentials);

        requiredRoles.add(role1);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, RoleSet.EMPTY_ROLESET, subject);
                will(returnValue(Boolean.FALSE));
            }
        });

        assertFalse("isAllAuthenticatedGranted should return false",
                    builtinAuthz.isAllAuthenticatedGranted(resourceName, requiredRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAllAuthenticatedGranted(java.lang.Object, java.util.List, javax.security.auth.Subject)}
     * .
     */
    @Test
    public void testIsAllAuthenticatedGranted_false() {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        requiredRoles.add(role1);

        Set<String> parsedRoles = new HashSet<String>();
        assignedRoles = new RoleSet(parsedRoles);
        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(assignedRoles));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
            }
        });

        builtinAuthz.isAllAuthenticatedGranted(resourceName, requiredRoles, subject);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_trueMultipleTables() {
        requiredRoles.add(role1);
        assignedRoles = new RoleSet(requiredRoles);

        builtinAuthz.setAuthorizationTableService(authzTableService2Ref);
        builtinAuthz.setAuthorizationTableService(authzTableService3Ref);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(null));
                one(authzTableService2).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(assignedRoles));
                one(authzTableService3).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(null));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, null);
            }
        });

        builtinAuthz.isEveryoneGranted(resourceName, requiredRoles);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isEveryoneGranted(java.lang.Object, java.util.List)} .
     */
    @Test
    public void testIsEveryoneGranted_multipleTablesAnswer() {
        requiredRoles.add(role1);
        assignedRoles = new RoleSet(requiredRoles);

        builtinAuthz.setAuthorizationTableService(authzTableService2Ref);
        builtinAuthz.setAuthorizationTableService(authzTableService3Ref);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(assignedRoles));
                one(authzTableService2).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(assignedRoles));
                one(authzTableService3).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(null));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, null, null);
            }
        });

        builtinAuthz.isEveryoneGranted(resourceName, requiredRoles);

        assertTrue("Expected message was not logged",
                   outputMgr.checkForMessages("CWWKS2100E: Multiple resources have the name " + resourceName + ". Authorization policy can not be determined."));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAuthorizationService#isAuthorized(java.lang.Object, java.util.List, javax.security.auth.Subject)} .
     * 
     * @throws CredentialDestroyedException
     * @throws CredentialExpiredException
     */
    @Test
    public void testIsAuthorized_multipleTablesAnswer() throws Exception {
        WSPrincipal princ = new WSPrincipal(subject1, accessId1, authMethod);
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(princ);
        HashSet<Object> pubCredentials = new HashSet<Object>();
        pubCredentials.add(wsCred);
        HashSet<Object> privCredentials = new HashSet<Object>();
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        requiredRoles.add(role1);
        assignedRoles = new RoleSet(requiredRoles);

        builtinAuthz.setAuthorizationTableService(authzTableService2Ref);
        builtinAuthz.setAuthorizationTableService(authzTableService3Ref);

        context.checking(new Expectations() {
            {
                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(authzTableService2).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(authzTableService3).getRolesForSpecialSubject(resourceName, AuthorizationTableService.EVERYONE);
                will(returnValue(null));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, null, null);
                will(returnValue(Boolean.FALSE));

                one(wsCred).isUnauthenticated();
                will(returnValue(Boolean.FALSE));
                one(wsCred).isBasicAuth();
                will(returnValue(Boolean.FALSE));

                one(authzTableService).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(authzTableService2).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(RoleSet.EMPTY_ROLESET));
                one(authzTableService3).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                will(returnValue(null));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, null, subject);
                will(returnValue(Boolean.FALSE));

                one(wsCred).getAccessId();
                will(returnValue(accessId1));

                one(authzTableService).getRolesForAccessId(resourceName, accessId1);
                will(returnValue(assignedRoles));
                one(authzTableService2).getRolesForAccessId(resourceName, accessId1);
                will(returnValue(assignedRoles));
                one(authzTableService3).getRolesForAccessId(resourceName, accessId1);
                will(returnValue(null));
                one(accessDecisionService).isGranted(resourceName, requiredRoles, null, subject);
                will(returnValue(Boolean.TRUE));
            }
        });

        builtinAuthz.isAuthorized(resourceName, requiredRoles, subject);

        assertTrue("Expected message was not logged",
                   outputMgr.checkForMessages("CWWKS2100E: Multiple resources have the name " + resourceName + ". Authorization policy can not be determined."));
    }
}
