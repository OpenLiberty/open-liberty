/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.enterprise.SecurityContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.intfc.SubjectManagerService;
import com.ibm.ws.security.javaeesec.SecurityContextHelperTestWrapper;

public class SecurityContextImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private SecurityContext secContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private final static String subject1 = "user1";
    private final static String accessId1 = "user:BasicRealm/user1";
    private final static String authMethod = WSPrincipal.AUTH_METHOD_PASSWORD;
    private final static String simple = "simple";
    private final SubjectManagerService subjectManagerService = mockery.mock(SubjectManagerService.class);
    private SecurityContextHelperTestWrapper secContextHelperWrapper;
    private final WSCredential wsCred = mockery.mock(WSCredential.class);
    private final Set<Principal> principals = new HashSet<Principal>();
    private final HashSet<Object> pubCredentials = new HashSet<Object>();
    private final HashSet<Object> privCredentials = new HashSet<Object>();
    private WSPrincipal princ = null;
    private SimplePrincipal simplePrinc = null;
    private final AuthorizationService authService = mockery.mock(AuthorizationService.class);
    private final ComponentMetaData compMetaData = mockery.mock(ComponentMetaData.class);

    @Before
    public void setUp() throws Exception {
        secContext = new SecurityContextImpl();
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        pubCredentials.add(wsCred);

        princ = new WSPrincipal(subject1, accessId1, authMethod);
        principals.add(princ);

        secContextHelperWrapper = new SecurityContextHelperTestWrapper(mockery);
        secContextHelperWrapper.setSubjectManagerService(subjectManagerService);
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testGetCallerPrincipal_userInCallerSubjectFromWsCred() throws Exception {
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(subject));
                one(wsCred).isUnauthenticated();
                will(returnValue(false));
                one(wsCred).get("com.ibm.wsspi.security.cred.jaspi.principal");
                will(returnValue(null));
                one(wsCred).getSecurityName();
                will(returnValue(accessId1));
            }
        });
        Principal callerPrinc = secContext.getCallerPrincipal();
        assertEquals("Not expected princiapal.", callerPrinc, new WSPrincipal(accessId1, princ.getAccessId(), princ.getAuthenticationMethod()));

    }

    @Test
    public void testGetCallerPrincipal_userInCallerSubjectFromPrincipal() throws Exception {
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(subject));
                one(wsCred).isUnauthenticated();
                will(returnValue(false));
                one(wsCred).get("com.ibm.wsspi.security.cred.jaspi.principal");
                will(returnValue(null));
                one(wsCred).getSecurityName();
                will(returnValue(null));
            }
        });
        Principal callerPrinc = secContext.getCallerPrincipal();
        assertEquals("Not expected princiapal.", callerPrinc, princ);

    }

    @Test
    public void testGetCallerPrincipal_userInCallerSubjectFromJaspiPrincipal() throws Exception {
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(subject));
                one(wsCred).isUnauthenticated();
                will(returnValue(false));
                one(wsCred).get("com.ibm.wsspi.security.cred.jaspi.principal");
                will(returnValue(subject1));
                one(wsCred).getSecurityName();
                will(returnValue(subject1));
            }
        });
        Principal callerPrinc = secContext.getCallerPrincipal();
        assertEquals("Not expected princiapal.", callerPrinc, princ);
    }

    @Test
    public void testGetCallerPrincipal_nullCallerSubject() throws Exception {
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(null));
            }
        });
        Principal callerPrinc = secContext.getCallerPrincipal();
        assertNull("callPrinc should be null", callerPrinc);

    }

    @Test
    public void testGetPrincipalsByType_WSPrincipalType() throws Exception {
        simplePrinc = new SimplePrincipal(simple);
        principals.add(simplePrinc);
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(subject));
            }
        });
        Set<WSPrincipal> principalsByType = secContext.getPrincipalsByType(WSPrincipal.class);
        assertEquals("There should be one WSPrincipal", principalsByType.size(), 1);
        assertEquals("Not expected prinicpal", principalsByType.iterator().next(), princ);

    }

    @Test
    public void testGetPrincipalsByType_SimplePrincipalType() throws Exception {
        simplePrinc = new SimplePrincipal(simple);
        principals.add(simplePrinc);
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(subject));
            }
        });
        Set<SimplePrincipal> principalsByType = secContext.getPrincipalsByType(SimplePrincipal.class);
        assertEquals("There should be one SimplePrincipal", principalsByType.size(), 1);
        assertEquals("Not expected prinicpal", principalsByType.iterator().next(), simplePrinc);

    }

    @Test
    public void testGetPrincipalsByType_getPrincipalType() throws Exception {
        simplePrinc = new SimplePrincipal(simple);
        principals.add(simplePrinc);
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(subject));
            }
        });
        Set<Principal> principalsByType = secContext.getPrincipalsByType(Principal.class);
        assertEquals("Should get 2 prinicipals", principalsByType.size(), 2);

    }

    @Test
    public void testGetPrincipalsByType_principalTypeNotIncluded() throws Exception {
        final Subject subject = new Subject(false, principals, pubCredentials, privCredentials);

        mockery.checking(new Expectations() {
            {
                one(subjectManagerService).getCallerSubject();
                will(returnValue(subject));
            }
        });
        Set<SimplePrincipal> principalsByType = secContext.getPrincipalsByType(SimplePrincipal.class);
        assertEquals("There are no SimplePrincipals so should return 0", principalsByType.size(), 0);

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
