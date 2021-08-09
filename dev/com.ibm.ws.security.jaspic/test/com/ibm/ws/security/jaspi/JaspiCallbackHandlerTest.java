/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
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
import java.security.Principal;
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

    protected static final String USER_NAME = "myUserName";
    protected static final String GROUP = "mygroup";

    protected final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected final UserRegistry mockUserRegistry = mock.mock(UserRegistry.class);
    protected final JaspiService mockJaspiService = mock.mock(JaspiService.class);
    protected JaspiCallbackHandler callbackHandler;
    protected Callback[] callbacks;
    protected Subject subject;
    protected Hashtable<String, Object> hashtable;

    private final WSSecurityService wsSecurityService = mock.mock(WSSecurityService.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WSSecurityService> wsSecurityServiceRef = mock.mock(ServiceReference.class);
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private RegistryHelper registryHelper;
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
        subject = new Subject();
        hashtable = new Hashtable<String, Object>();
        callbackHandler = new JaspiCallbackHandler(mockJaspiService);
    }

    protected void setupJaspiService(final Hashtable<String, Object> ht) {
        mock.checking(new Expectations() {
            {
                allowing(mockJaspiService).getCustomCredentials(with(equal(subject)));
                will(returnValue(ht));
            }
        });
    }

    private void newPasswordCB(final Subject subj, String user, String pwd) {
        pwdCB = new PasswordValidationCallback(subj, user, pwd.toCharArray());
        callbacks = new Callback[] { pwdCB };
    }

    protected void newCallerPrincipalCB(final Subject subj, final String userName) {
        cpCB = new CallerPrincipalCallback(subj, userName);
        callbacks = new Callback[] { cpCB };
    }

    protected void newCallerPrincipalCB(final Subject subj, final Principal principal) {
        cpCB = new CallerPrincipalCallback(subj, principal);
        callbacks = new Callback[] { cpCB };
    }

    protected void newGroupPrincipalCB(final Subject subj, final String[] groups) {
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
        callbackHandler.handle(callbacks);
    }

    @Test(expected = IOException.class)
    public void testIOException() throws Exception {
        setupJaspiService(hashtable);
        newGroupPrincipalCB(subject, new String[] { GROUP });
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).isValidGroup(GROUP);
                will(throwException(new CustomRegistryException()));
            }
        });
        callbackHandler.handle(callbacks);
    }

    @Test
    public void testPasswordCheckFailure() throws Exception {
        setupJaspiService(hashtable);
        newPasswordCB(subject, "user1", "security");
        setupRegistry(mockUserRegistry, true, "user1", "security", "userName", new String[] { "group" });
        assertTrue(hashtable.isEmpty());
        callbackHandler.handle(callbacks);
        assertFalse("Password check did not fail as expected.", pwdCB.getResult());
    }

    @Test
    public void testPasswordCheckSuccess() throws Exception {
        setupJaspiService(hashtable);
        newPasswordCB(subject, "user1", "security");
        setupRegistry(mockUserRegistry, false, "user1", "security", "userName", new String[] { "group" });
        assertTrue(hashtable.isEmpty());
        callbackHandler.handle(callbacks);
        assertTrue("Password validation failed.", pwdCB.getResult());
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_REALM, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_REALM));
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_GROUPS, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_GROUPS));
        assertEquals("userName", hashtable.get(AttributeNameConstants.WSCREDENTIAL_USERID));
    }

    @Test
    public void testCallerPrincipalCallback() throws Exception {
        setupJaspiService(hashtable);
        newCallerPrincipalCB(subject, USER_NAME);
        final String[] groups = new String[] { "group1" };
        setupRegistry(USER_NAME, true, "myUniqueId", groups);
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).getUniqueGroupIds(USER_NAME);
                will(returnValue(Arrays.asList(groups)));
            }
        });
        callbackHandler.handle(callbacks);
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertTrue("Hashtable does not contain key: " + AuthenticationConstants.INTERNAL_ASSERTION_KEY, hashtable.containsKey(AuthenticationConstants.INTERNAL_ASSERTION_KEY));
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_GROUPS, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_GROUPS));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateSubjectHashtable() throws Exception {
        setupJaspiService(null);
        newCallerPrincipalCB(subject, USER_NAME);
        final String[] groups = new String[] { "group1" };
        setupRegistry(USER_NAME, true, "myUniqueId", groups);
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).getUniqueGroupIds(USER_NAME);
                will(returnValue(Arrays.asList(groups)));
            }
        });
        callbackHandler.handle(callbacks);
        Set<Hashtable> creds = subject.getPrivateCredentials(Hashtable.class);
        assertFalse(creds.isEmpty());
        Hashtable newHT = creds.iterator().next();
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_USERID, newHT.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
        String name = (String) newHT.get(AttributeNameConstants.WSCREDENTIAL_USERID);
        assertEquals(USER_NAME, name);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGroupPrincipalCallback() throws Exception {
        setupJaspiService(hashtable);
        newGroupPrincipalCB(subject, new String[] { GROUP });
        final String uniqueGroupId = "group:realm/cn=" + GROUP;
        mock.checking(new Expectations() {
            {
                allowing(mockUserRegistry).isValidGroup(GROUP);
                will(returnValue(true));
                allowing(mockUserRegistry).getUniqueGroupId(GROUP);
                will(returnValue(uniqueGroupId));
            }
        });
        assertTrue(hashtable.isEmpty());
        callbackHandler.handle(callbacks);
        assertTrue("Hashtable does not contain key: " + AttributeNameConstants.WSCREDENTIAL_GROUPS, hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_GROUPS));
        List<String> groups = (List<String>) hashtable.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        assertTrue("Hashtable does not contain mygroup", groups.contains(uniqueGroupId));
    }

}
