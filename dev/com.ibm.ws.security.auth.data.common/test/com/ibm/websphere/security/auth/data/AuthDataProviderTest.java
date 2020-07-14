/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.security.auth.login.LoginException;

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

public class AuthDataProviderTest {

    private final Mockery mockery = new JUnit4Mockery();

    private static final String AUTH_DATA_ALIAS = "authData";
    private static final String ANOTHER_AUTH_DATA_ALIAS = "anotherAuthData";
    private static final String AUTH_DATA_ALIAS_NULL_USER = "authDataWithNullUser";
    private static final String AUTH_DATA_ALIAS_EMPTY_PASSWORD = "authDataWithEmptyPassword";
    private static final String AUTH_DATA_ALIAS_DEFAULT_ID = "default-0";
    private static final String AUTH_DATA_ALIAS_DISPLAY_ID1 = "dataSource[ds1]/containerAuthData[default-0]";
    private static final String AUTH_DATA_ALIAS_DISPLAY_DEFAULT = "authData[default-0]";

    private static final String TEST_USER = "testUser";
    private static final String ANOTHER_TEST_USER = "anotherTestUser";
    private static final String TEST_USER_PWD = "testUserPwd";
    private static final String ANOTHER_TEST_USER_PWD = "anotherTestUserPwd";

    private AuthDataProvider authDataProvider;
    private final AuthData authDataConfig = createAuthData(TEST_USER, TEST_USER_PWD);
    private final AuthData anotherAuthData = createAuthData(ANOTHER_TEST_USER, ANOTHER_TEST_USER_PWD);
    private final AuthData authDataNullUser = createAuthData(null, ANOTHER_TEST_USER_PWD);
    private final AuthData authDataEmptyPassword = createAuthData(TEST_USER, "");
    private final AuthData authDataConfigWithDefaultId = createAuthData(TEST_USER, "somePwdForAuthDataWithConfigId");
    private final String user = "testUser";
    private final char[] password = "testPassword".toCharArray();
    private final String alias = "myAuthData";

    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private ServiceReference<AuthData> authDataRef;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private AuthData createAuthData(final String name, final String password) {
        final AuthData authData = mockery.mock(AuthData.class, "AuthData:" + name + "/" + password);
        mockery.checking(new Expectations() {
            {
                allowing(authData).getUserName();
                will(returnValue(name));
                allowing(authData).getPassword();
                will(returnValue(password.toCharArray()));
                allowing(authData).getKrb5Keytab();
                will(returnValue(null));
                allowing(authData).getKrb5Principal();
                will(returnValue(null));
            }
        });
        return authData;
    }

    @SuppressWarnings("unchecked")
    private ServiceReference<AuthData> createAuthDataReference(final String authDataAlias, final String displayId, final AuthData authDataConfig) {
        final ServiceReference<AuthData> authDataRef = mockery.mock(ServiceReference.class, authDataAlias + displayId);
        mockery.checking(new Expectations() {
            {
                allowing(authDataRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(authDataRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(authDataRef).getProperty("id");
                will(returnValue(authDataAlias));
                allowing(authDataRef).getProperty("config.displayId");
                will(returnValue(displayId));
                allowing(cc).locateService("authData", authDataRef);
                will(returnValue(authDataConfig));
            }
        });
        return authDataRef;
    }

    @Before
    public void setUp() {
        authDataProvider = new AuthDataProvider();

        authDataRef = createAuthDataReference(AUTH_DATA_ALIAS, createDisplayId(AUTH_DATA_ALIAS), authDataConfig);
        ServiceReference<AuthData> anotherAuthDataRef = createAuthDataReference(ANOTHER_AUTH_DATA_ALIAS, createDisplayId(ANOTHER_AUTH_DATA_ALIAS),
                                                                                anotherAuthData);

        authDataProvider.activate(cc);
        authDataProvider.setAuthData(authDataRef);
        authDataProvider.setAuthData(anotherAuthDataRef);
        authDataProvider.setAuthData(createAuthDataReference(AUTH_DATA_ALIAS_NULL_USER, createDisplayId(AUTH_DATA_ALIAS_NULL_USER), authDataNullUser));
        authDataProvider.setAuthData(createAuthDataReference(AUTH_DATA_ALIAS_EMPTY_PASSWORD, createDisplayId(AUTH_DATA_ALIAS_EMPTY_PASSWORD), authDataEmptyPassword));
        authDataProvider.setAuthData(createAuthDataReference(AUTH_DATA_ALIAS_DEFAULT_ID, AUTH_DATA_ALIAS_DISPLAY_ID1, authDataConfigWithDefaultId));
        authDataProvider.setAuthData(createAuthDataReference(null, AUTH_DATA_ALIAS_DISPLAY_DEFAULT, authDataConfigWithDefaultId));
    }

    private String createDisplayId(String alias) {
        return "authData[" + alias + "]";
    }

    @After
    public void tearDown() {
        authDataProvider.deactivate(cc);
        mockery.assertIsSatisfied();
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAuthData() throws Exception {
        AuthData actualAuthData = authDataProvider.getAuthData(AUTH_DATA_ALIAS);
        assertSame("There must be an AuthData object created for the specified auth data alias.",
                   authDataConfig, actualAuthData);
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAuthData_DifferentAuthDataAlias() throws Exception {
        AuthData actualAuthData = authDataProvider.getAuthData(ANOTHER_AUTH_DATA_ALIAS);
        assertSame("There must be an AuthData object created for the specified auth data alias.",
                   anotherAuthData, actualAuthData);
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAuthData_AuthDataWithConfigDisplayId() throws Exception {
        AuthData actualAuthData = authDataProvider.getAuthData(AUTH_DATA_ALIAS_DISPLAY_ID1);
        assertSame("There must be an AuthData object created for the specified auth data alias.",
                   authDataConfigWithDefaultId, actualAuthData);
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAuthData_AuthDataWithNoId() throws Exception {
        AuthData actualAuthData = authDataProvider.getAuthData(AUTH_DATA_ALIAS_DISPLAY_DEFAULT);
        assertSame("There must be an AuthData object created for the specified auth data alias.",
                   authDataConfigWithDefaultId, actualAuthData);
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAuthData_AuthDataAliasDoesNotExist() throws Exception {
        try {
            authDataProvider.getAuthData("authDataAliasDoesNotExist");
            fail("Expected a LoginException since the auth data alias does not exist.");
        } catch (LoginException e) {
            assertNoSuchAliasMessage(e, "authDataAliasDoesNotExist");
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAuthData_AuthDataRemoved() throws Exception {
        try {
            authDataProvider.unsetAuthData(authDataRef);
            authDataProvider.getAuthData(AUTH_DATA_ALIAS);
            fail("Expected a LoginException since the auth data alias does not exist.");
        } catch (LoginException e) {
            assertNoSuchAliasMessage(e, AUTH_DATA_ALIAS);
        }
    }

    private void assertNoSuchAliasMessage(LoginException e, String authDataAlias) {
        String noSuchAliasMessage = "CWWKS1300E: A configuration exception has occurred. The requested authentication data alias " + authDataAlias + " could not be found.";
        assertEquals("Recieved wrong exception message", noSuchAliasMessage, e.getMessage());
        assertTrue("Expected error message was not logged", outputMgr.checkForStandardErr(noSuchAliasMessage));
    }

    @SuppressWarnings("static-access")
    @Test
    public void getSubject_NullUserName() throws Exception {
        try {
            authDataProvider.getAuthData(AUTH_DATA_ALIAS_NULL_USER);
            fail("Expected a LoginException since the auth data has a null user.");
        } catch (LoginException e) {
            assertConfigErrorIncomplete(e, "user");
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void getSubject_EmptyPassword() throws Exception {
        try {
            authDataProvider.getAuthData(AUTH_DATA_ALIAS_EMPTY_PASSWORD);
            fail("Expected a LoginException since the auth data has an empty password.");
        } catch (LoginException e) {
            assertConfigErrorIncomplete(e, "password");
        }
    }

    private void assertConfigErrorIncomplete(LoginException e, String attribute) {
        String configErrorIncompleteMessage = "CWWKS1301E: A configuration error has occurred. The attribute " + attribute + " must be defined.";
        assertEquals("Recieved wrong exception message", configErrorIncompleteMessage, e.getMessage());
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForMessages(configErrorIncompleteMessage));
    }

}
