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
package com.ibm.ws.security.jaspi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class NonMappingCallbackHandlerTest extends JaspiCallbackHandlerTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        callbackHandler = new NonMappingCallbackHandler(mockJaspiService);
    }

    @Override
    @Test
    public void testCallerPrincipalCallback() throws Exception {
        setupJaspiService(hashtable);
        newCallerPrincipalCB(subject, USER_NAME);

        callbackHandler.handle(callbacks);

        assertCommonCallerPrincipalCallbackResults();
    }

    @Test
    public void testCallerPrincipalCallbackWithPrincipal() throws Exception {
        setupJaspiService(hashtable);
        final Principal principal = mock.mock(Principal.class);
        mock.checking(new Expectations() {
            {
                allowing(principal).getName();
                will(returnValue(USER_NAME));
            }
        });
        newCallerPrincipalCB(subject, principal);

        callbackHandler.handle(callbacks);

        assertEquals("The hashtable must contain the JASPIC provider principal.", principal, hashtable.get("com.ibm.wsspi.security.cred.jaspi.principal"));
        assertCommonCallerPrincipalCallbackResults();
    }

    private void assertCommonCallerPrincipalCallbackResults() {
        assertEquals("The hashtable must contain the unique id.", "user:defaultRealm/" + USER_NAME, hashtable.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertEquals("The hashtable must contain the security name.", USER_NAME, hashtable.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME));
        assertFalse("The hashtable must not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertFalse("The hashtable must not contain key: " + AuthenticationConstants.INTERNAL_ASSERTION_KEY, hashtable.containsKey(AuthenticationConstants.INTERNAL_ASSERTION_KEY));
        assertFalse("The hashtable must not contain key: " + AttributeNameConstants.WSCREDENTIAL_GROUPS, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_GROUPS));
    }

    @SuppressWarnings("rawtypes")
    @Override
    @Test
    public void testCreateSubjectHashtable() throws Exception {
        setupJaspiService(null);
        newCallerPrincipalCB(subject, USER_NAME);

        callbackHandler.handle(callbacks);
        Set<Hashtable> creds = subject.getPrivateCredentials(Hashtable.class);
        assertFalse(creds.isEmpty());
        Hashtable subjectHashtable = creds.iterator().next();

        assertFalse("The hashtable must not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, subjectHashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        String name = (String) subjectHashtable.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME);
        assertEquals("The security name must be the unmapped principal name.", USER_NAME, name);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Test
    public void testGroupPrincipalCallback() throws Exception {
        setupJaspiService(hashtable);
        newGroupPrincipalCB(subject, new String[] { GROUP });

        assertTrue(hashtable.isEmpty());
        callbackHandler.handle(callbacks);

        List<String> groups = (List<String>) hashtable.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        assertTrue("The hashtable must contain the unmapped group.", groups.contains(GROUP));
    }

    @Override
    @Test
    public void testIOException() throws Exception {
        setupJaspiService(hashtable);
        newGroupPrincipalCB(subject, new String[] { GROUP });
        mock.checking(new Expectations() {
            {
                never(mockUserRegistry).isValidGroup(GROUP);
            }
        });
        callbackHandler.handle(callbacks);
    }

}
