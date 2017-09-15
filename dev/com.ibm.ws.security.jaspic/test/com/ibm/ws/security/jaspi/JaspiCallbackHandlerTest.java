/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.webcontainer.security.JaspiService;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class JaspiCallbackHandlerTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final WSSecurityService wsSecurityService = mock.mock(WSSecurityService.class);
    private final UserRegistry mockUserRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<WSSecurityService> wsSecurityServiceRef = mock.mock(ServiceReference.class);
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final JaspiService mockJaspiService = mock.mock(JaspiService.class);
    private RegistryHelper registryHelper;
    private JaspiCallbackHandler cbh;
    private Callback[] callbacks;
    private Subject subj;
    private Hashtable<String, Object> ht;
    private PasswordValidationCallback pwdCB;
    private CallerPrincipalCallback cpCB;
    private GroupPrincipalCallback gpCB;

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(WSSecurityService.KEY_WS_SECURITY_SERVICE, wsSecurityServiceRef);
                will(returnValue(wsSecurityService));
                allowing(wsSecurityService).getUserRegistry(with(any(String.class)));
                will(returnValue(mockUserRegistry));
            }
        });
        registryHelper = new RegistryHelper();
        registryHelper.setWsSecurityService(wsSecurityServiceRef);
        registryHelper.activate(cc);
        subj = new Subject();
        ht = new Hashtable<String, Object>();
        cbh = new JaspiCallbackHandler(mockJaspiService);
    }

    private void setupJaspiService(final Hashtable<String, Object> ht) {
        mock.checking(new Expectations() {
            {
                allowing(mockJaspiService).getCustomCredentials(with(equal(subj)));
                will(returnValue(ht));
            }
        });
    }

    private void newPasswordCB(final Subject subj, String user, String pwd) {
        pwdCB = new PasswordValidationCallback(subj, user, pwd.toCharArray());
        callbacks = new Callback[] { pwdCB };
    }

    private void newCallerPrincipalCB(final Subject subj, final String userName) {
        cpCB = new CallerPrincipalCallback(subj, userName);
        callbacks = new Callback[] { cpCB };
    }

    private void newGroupPrincipalCB(final Subject subj, final String[] groups) {
        gpCB = new GroupPrincipalCallback(subj, groups);
        callbacks = new Callback[] { gpCB };
    }

    private void setupRegistry(final String user, final boolean isValid, final String uniqueId, final String[] groups) throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).isValidUser(user);
                will(returnValue(isValid));
                allowing(mockUserRegistry).getUniqueUserId(user);
                will(returnValue(uniqueId));
                allowing(mockUserRegistry).getUniqueGroupIds(uniqueId);
                will(returnValue(Arrays.asList(groups)));
            }
        });
    }

    private void setupRegistry(final UserRegistry registry, final boolean fail, final String user, final String pwd,
                               final String userName, final String[] groups) throws Exception {
        final List<String> groupList = Arrays.asList(groups);
        if (fail) {
            mock.checking(new Expectations() {
                {
                    allowing(registry).checkPassword(user, pwd);
                    will(throwException(new PasswordCheckFailedException("Intentionally thrown")));
                    allowing(mockUserRegistry).getRealm();
                    will(returnValue("bobRealm"));
                }
            });
        } else {
            mock.checking(new Expectations() {
                {
                    allowing(registry).checkPassword(user, pwd);
                    will(returnValue(userName));
                    allowing(registry).getUniqueUserId(userName);
                    allowing(registry).getGroupsForUser(userName);
                    will(returnValue(groupList));
                    for (String name : groups) {
                        allowing(registry).getUniqueGroupId(name);
                    }
                    allowing(mockUserRegistry).getRealm();
                    will(returnValue("bobRealm"));
                }
            });
        }
    }

    /**
     * This class was ported from twas and only 2 methods were changed for liberty,
     * the constructor and setProvider, so that is all that will be unit tested on Liberty.
     */

    @Test
    public void testGetUserRegistry() throws Exception {
        final JaspiCallbackHandler jch = new JaspiCallbackHandler(mockJaspiService);
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).getUniqueUserId("bob");
                will(returnValue("user:realm/bob"));
            }
        });
        UserRegistry reg = jch.getUserRegistry();
        assertNotNull(reg);
        assertEquals(reg.getUniqueUserId("bob"), "user:realm/bob");
    }

    @Test(expected = UnsupportedCallbackException.class)
    public void testUnsupportedCallback() throws Exception {
        callbacks = new Callback[] { new NameCallback("prompt") };
        cbh.handle(callbacks);
    }

    @Test(expected = IOException.class)
    public void testIOException() throws Exception {
        setupJaspiService(ht);
        newGroupPrincipalCB(subj, new String[] { "mygroup" });
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).isValidGroup("mygroup");
                will(throwException(new CustomRegistryException()));
            }
        });
        cbh.handle(callbacks);
    }

    @Test
    public void testPasswordCheckFailure() throws Exception {
        setupJaspiService(ht);
        newPasswordCB(subj, "user1", "security");
        setupRegistry(mockUserRegistry, true, "user1", "security", "userName", new String[] { "group" });
        assertTrue(ht.isEmpty());
        cbh.handle(callbacks);
        assertFalse("Password check did not fail as expected.", pwdCB.getResult());
    }

    @Test
    public void testPasswordCheckSuccess() throws Exception {
        setupJaspiService(ht);
        newPasswordCB(subj, "user1", "security");
        setupRegistry(mockUserRegistry, false, "user1", "security", "userName", new String[] { "group" });
        assertTrue(ht.isEmpty());
        cbh.handle(callbacks);
        assertTrue("Password validation failed.", pwdCB.getResult());
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_REALM, ht.containsKey(AttributeNameConstants.WSCREDENTIAL_REALM));
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, ht.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_GROUPS, ht.containsKey(AttributeNameConstants.WSCREDENTIAL_GROUPS));
        assertEquals("userName", ht.get(AttributeNameConstants.WSCREDENTIAL_USERID));
    }

    @Test
    public void testCallerPrincipalCallback() throws Exception {
        setupJaspiService(ht);
        newCallerPrincipalCB(subj, "myUserName");
        final String[] groups = new String[] { "group1" };
        setupRegistry("myUserName", true, "myUniqueId", groups);
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).getUniqueGroupIds("myUserName");
                will(returnValue(Arrays.asList(groups)));
            }
        });
        cbh.handle(callbacks);
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, ht.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertTrue("Hashtable does not contain key: " + AuthenticationConstants.INTERNAL_ASSERTION_KEY, ht.containsKey(AuthenticationConstants.INTERNAL_ASSERTION_KEY));
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_GROUPS, ht.containsKey(AttributeNameConstants.WSCREDENTIAL_GROUPS));
    }

    @Test
    public void testCreateSubjectHashtable() throws Exception {
        setupJaspiService(null);
        newCallerPrincipalCB(subj, "myUserName");
        final String[] groups = new String[] { "group1" };
        setupRegistry("myUserName", true, "myUniqueId", groups);
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).getUniqueGroupIds("myUserName");
                will(returnValue(Arrays.asList(groups)));
            }
        });
        cbh.handle(callbacks);
        Set<Hashtable> creds = subj.getPrivateCredentials(Hashtable.class);
        assertFalse(creds.isEmpty());
        Hashtable newHT = creds.iterator().next();
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, newHT.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        String name = (String) newHT.get(AttributeNameConstants.WSCREDENTIAL_USERID);
        assertEquals("myUserName", name);
    }

    @Test
    public void testGroupPrincipalCallback() throws Exception {
        setupJaspiService(ht);
        newGroupPrincipalCB(subj, new String[] { "mygroup" });
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).isValidGroup("mygroup");
                will(returnValue(true));
                allowing(mockUserRegistry).getUniqueGroupId("mygroup");
                will(returnValue("group:realm/cn=mygroup"));
            }
        });
        assertTrue(ht.isEmpty());
        cbh.handle(callbacks);
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_GROUPS, ht.containsKey(AttributeNameConstants.WSCREDENTIAL_GROUPS));
        List<String> groups = (List<String>) ht.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        assertTrue("Hashtable does not contain mygroup", groups.contains("group:realm/cn=mygroup"));
    }

}
