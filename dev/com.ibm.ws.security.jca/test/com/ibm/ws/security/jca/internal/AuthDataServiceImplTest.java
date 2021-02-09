/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jca.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.websphere.security.auth.data.AuthDataProvider;
import com.ibm.websphere.security.auth.data.AuthDataProviderTestHelper;
import com.ibm.ws.security.auth.data.internal.AuthDataTestHelper;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.intfc.SubjectManagerService;

import test.common.SharedOutputManager;

public class AuthDataServiceImplTest {

    private static final String DEFAULT_PRINCIPAL_MAPPING = "DefaultPrincipalMapping";
    private static final String AUTH_DATA_ALIAS = "authData";
    private static final String ANOTHER_AUTH_DATA_ALIAS = "anotherAuthData";
    private static final String YET_ANOTHER_AUTH_DATA_ALIAS = "yetAnotherAuthData";
    private static final String AUTH_DATA_ALIAS_NULL_USER = "authDataWithNullUser";
    private static final String AUTH_DATA_ALIAS_NULL_PASSWORD = "authDataWithNullPassword";
    private static final String AUTH_DATA_ALIAS_EMPTY_USER = "authDataWithEmptyUser";
    private static final String AUTH_DATA_ALIAS_EMPTY_PASSWORD = "authDataWithEmptyPassword";
    private static final String AUTH_DATA_ALIAS_DEFAULT_ID = "default-0";
    private static final String AUTH_DATA_ALIAS_DISPLAY_ID1 = "dataSource[ds1]/containerAuthData[default-0]";
    private static final String AUTH_DATA_ALIAS_DISPLAY_ID2 = "dataSource[ds2]/containerAuthData[default-0]";
    private static final String TEST_USER = "testUser";
    private static final String ANOTHER_TEST_USER = "anotherTestUser";
    private static final String TEST_USER_PWD = "testUserPwd";
    private static final String ANOTHER_TEST_USER_PWD = "anotherTestUserPwd";
    private static final String JAAS_ENTRY_NAME = "MyJAASEntry";

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private final AuthDataTestHelper authDataTestHelper = new AuthDataTestHelper(mockery, cc);
    private final AuthData authDataConfig = authDataTestHelper.createAuthData(TEST_USER, TEST_USER_PWD);
    private final AuthData anotherAuthDataConfig = authDataTestHelper.createAuthData(ANOTHER_TEST_USER, ANOTHER_TEST_USER_PWD);
    private final AuthData authDataConfigWithNullUser = authDataTestHelper.createAuthData(null, TEST_USER_PWD);
    private final AuthData authDataConfigWithNullPassword = authDataTestHelper.createAuthData(TEST_USER, null);
    private final AuthData authDataConfigWithEmptyUser = authDataTestHelper.createAuthData("", TEST_USER_PWD);
    private final AuthData authDataConfigWithEmptyPassword = authDataTestHelper.createAuthData(TEST_USER, "");
    private final AuthData authDataConfigWithDefaultId1 = authDataTestHelper.createAuthData(TEST_USER, TEST_USER_PWD);
    private final AuthData authDataConfigWithDefaultId2 = authDataTestHelper.createAuthData(ANOTHER_TEST_USER, ANOTHER_TEST_USER_PWD);

    private final Configuration testJaasConfiguration = mockery.mock(Configuration.class);
    private Configuration previousJaasConfiguration;
    private ServiceReference<AuthData> authDataConfigRef;
    private AuthDataServiceImpl authDataServiceImpl;
    private AuthDataProvider authDataProvider;
    private AuthDataProviderTestHelper authDataProviderTestHelper;
    private final String jaasEntryName = null;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @BeforeClass
    public static void traceSetUp() {
        outputMgr.trace("*.*=all=enabled");
    }

    @AfterClass
    public static void traceTearDown() {
        outputMgr.trace("*.*=all=disabled");
    }

    @Before
    public void setUp() throws Exception {
        authDataServiceImpl = new AuthDataServiceImpl();
        authDataProvider = new AuthDataProvider();
        authDataProviderTestHelper = new AuthDataProviderTestHelper(authDataProvider, cc);

        authDataConfigRef = authDataTestHelper.createAuthDataRef(AUTH_DATA_ALIAS, createDisplayId(AUTH_DATA_ALIAS), authDataConfig);
        ServiceReference<AuthData> anotherAuthDataConfigRef = authDataTestHelper.createAuthDataRef(ANOTHER_AUTH_DATA_ALIAS, createDisplayId(ANOTHER_AUTH_DATA_ALIAS),
                                                                                                   anotherAuthDataConfig);
        ServiceReference<AuthData> yetAnotherAuthDataConfigRef = authDataTestHelper.createAuthDataRef(YET_ANOTHER_AUTH_DATA_ALIAS, createDisplayId(YET_ANOTHER_AUTH_DATA_ALIAS),
                                                                                                      anotherAuthDataConfig);
        ServiceReference<AuthData> authDataConfigWithNullUserRef = authDataTestHelper.createAuthDataRef(AUTH_DATA_ALIAS_NULL_USER, createDisplayId(AUTH_DATA_ALIAS_NULL_USER),
                                                                                                        authDataConfigWithNullUser);
        ServiceReference<AuthData> authDataConfigWithEmptyUserRef = authDataTestHelper.createAuthDataRef(AUTH_DATA_ALIAS_EMPTY_USER, createDisplayId(AUTH_DATA_ALIAS_EMPTY_USER),
                                                                                                         authDataConfigWithEmptyUser);
        ServiceReference<AuthData> authDataConfigWithNullPasswordRef = authDataTestHelper.createAuthDataRef(AUTH_DATA_ALIAS_NULL_PASSWORD,
                                                                                                            createDisplayId(AUTH_DATA_ALIAS_NULL_PASSWORD),
                                                                                                            authDataConfigWithNullPassword);
        ServiceReference<AuthData> authDataConfigWithEmptyPasswordRef = authDataTestHelper.createAuthDataRef(AUTH_DATA_ALIAS_EMPTY_PASSWORD,
                                                                                                             createDisplayId(AUTH_DATA_ALIAS_EMPTY_PASSWORD),
                                                                                                             authDataConfigWithEmptyPassword);
        ServiceReference<AuthData> authDataConfigWithDefaultIdRef1 = authDataTestHelper.createAuthDataRef(AUTH_DATA_ALIAS_DEFAULT_ID, AUTH_DATA_ALIAS_DISPLAY_ID1,
                                                                                                          authDataConfigWithDefaultId1);
        ServiceReference<AuthData> authDataConfigWithDefaultIdRef2 = authDataTestHelper.createAuthDataRef(AUTH_DATA_ALIAS_DEFAULT_ID, AUTH_DATA_ALIAS_DISPLAY_ID2,
                                                                                                          authDataConfigWithDefaultId2);

        Map<String, Object> props = createProps(true, 1);
        authDataServiceImpl.activate(cc, props);

        authDataProviderTestHelper.setAuthData(authDataConfigRef);
        authDataProviderTestHelper.setAuthData(anotherAuthDataConfigRef);
        authDataProviderTestHelper.setAuthData(yetAnotherAuthDataConfigRef);
        authDataProviderTestHelper.setAuthData(authDataConfigWithNullUserRef);
        authDataProviderTestHelper.setAuthData(authDataConfigWithEmptyUserRef);
        authDataProviderTestHelper.setAuthData(authDataConfigWithNullPasswordRef);
        authDataProviderTestHelper.setAuthData(authDataConfigWithEmptyPasswordRef);
        authDataProviderTestHelper.setAuthData(authDataConfigWithDefaultIdRef1);
        authDataProviderTestHelper.setAuthData(authDataConfigWithDefaultIdRef2);

        authDataServiceImpl.authDataProvider = authDataProvider;
        authDataProviderTestHelper.activate();

        previousJaasConfiguration = Configuration.getConfiguration();
        Configuration.setConfiguration(testJaasConfiguration);
    }

    private String createDisplayId(String alias) {
        return "authData[" + alias + "]";
    }

    @After
    public void tearDown() {
        authDataProviderTestHelper.deactivate();
        Configuration.setConfiguration(previousJaasConfiguration);
        mockery.assertIsSatisfied();
    }

    @Test
    public void getSubject_AuthDataAlias() throws Exception {
        Subject subject = getSubjectFromAuthDataService(jaasEntryName, AUTH_DATA_ALIAS);
        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        assertTrue("There must not be a principal in the subject.", subject.getPrincipals().isEmpty());
    }

    @Test
    public void getSubject_AuthDataAlias_PrincipalInSubject() throws Exception {
        Principal invocationSubjectPrincipal = prepareSubjectManagerServiceReturningPrincipal();
        Subject subject = getSubjectFromAuthDataService(jaasEntryName, AUTH_DATA_ALIAS);
        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        assertEquals("There must be one password credential in the subject's private credentials.", 1, passwordCredentials.size());
        Set<WSPrincipal> wsPrincipals = subject.getPrincipals(WSPrincipal.class);
        WSPrincipal wsPrincipal = wsPrincipals.iterator().next();
        assertEquals("There invocation subject principal must be in the subject.", invocationSubjectPrincipal, wsPrincipal);
    }

    @Test
    public void getSubject_AuthDataAliasNull() throws Exception {
        try {
            getSubjectFromAuthDataService(jaasEntryName, null);
            fail("Expected a LoginException since the auth data alias is null.");
        } catch (LoginException e) {
            assertNoSuchAliasMessage(e, null);
        }
    }

    @Test
    public void getSubject_AuthDataAliasDoesNotExist() throws Exception {
        try {
            getSubjectFromAuthDataService(jaasEntryName, "authDataAliasDoesNotExist");
            fail("Expected a LoginException since the auth data alias does not exist.");
        } catch (LoginException e) {
            assertNoSuchAliasMessage(e, "authDataAliasDoesNotExist");
        }
    }

    @Test
    public void getSubject_LoginDataDoesNotExist() throws Exception {
        try {
            ManagedConnectionFactory managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class);
            authDataServiceImpl.getSubject(managedConnectionFactory, jaasEntryName, null);
            fail("Expected a LoginException since the auth data alias does not exist because the login data is null.");
        } catch (LoginException e) {
            assertNoSuchAliasMessage(e, null);
        }
    }

    private void assertNoSuchAliasMessage(LoginException e, String authDataAlias) {
        String noSuchAliasMessage = "CWWKS1300E: A configuration exception has occurred. The requested authentication data alias " + authDataAlias + " could not be found.";
        assertEquals("Recieved wrong exception message", noSuchAliasMessage, e.getMessage());
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr(noSuchAliasMessage));
    }

    @Test
    public void getSubject_UserName() throws Exception {
        String actualUserName = getUserName(jaasEntryName, AUTH_DATA_ALIAS);
        assertEquals("There must be a user in the password credential.", TEST_USER, actualUserName);
    }

    @Test
    public void getSubject_Password() throws Exception {
        char[] actualPassword = getPassword(jaasEntryName, AUTH_DATA_ALIAS);
        assertEquals("There must be a user in the password credential.",
                     TEST_USER_PWD, String.valueOf(actualPassword));
    }

    @Test
    public void getSubject_DifferentUserName() throws Exception {
        String actualUserName = getUserName(jaasEntryName, ANOTHER_AUTH_DATA_ALIAS);
        assertEquals("There must be a user in the password credential.", ANOTHER_TEST_USER, actualUserName);
    }

    @Test
    public void getSubject_NullUserName() throws Exception {
        try {
            getSubjectFromAuthDataService(jaasEntryName, AUTH_DATA_ALIAS_NULL_USER);
            fail("Expected a LoginException since the auth data has a null user.");
        } catch (LoginException e) {
            assertConfigErrorIncomplete(e, "user");
        }
    }

    @Test
    public void getSubject_EmptyUserName() throws Exception {
        try {
            getSubjectFromAuthDataService(jaasEntryName, AUTH_DATA_ALIAS_EMPTY_USER);
            fail("Expected a LoginException since the auth data has an empty user.");
        } catch (LoginException e) {
            assertConfigErrorIncomplete(e, "user");
        }
    }

    @Test
    public void getSubject_NullPassword() throws Exception {
        try {
            getSubjectFromAuthDataService(jaasEntryName, AUTH_DATA_ALIAS_NULL_PASSWORD);
            fail("Expected a LoginException since the auth data has a null password.");
        } catch (LoginException e) {
            assertConfigErrorIncomplete(e, "password");
        }
    }

    @Test
    public void getSubject_EmptyPassword() throws Exception {
        try {
            getSubjectFromAuthDataService(jaasEntryName, AUTH_DATA_ALIAS_EMPTY_PASSWORD);
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

    @Test
    public void getSubject_DifferentPassword() throws Exception {
        char[] actualPassword = getPassword(jaasEntryName, ANOTHER_AUTH_DATA_ALIAS);
        assertEquals("There must be a user in the password credential.",
                     ANOTHER_TEST_USER_PWD, String.valueOf(actualPassword));
    }

    @Test
    public void getSubject_ManagedConnectionFactory() throws Exception {
        ManagedConnectionFactory managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class);
        Map<String, Object> loginData = new HashMap<String, Object>();
        loginData.put("com.ibm.mapping.authDataAlias", AUTH_DATA_ALIAS);
        Subject subject = authDataServiceImpl.getSubject(managedConnectionFactory, jaasEntryName, loginData);
        PasswordCredential passwordCredential = getPasswordCredential(subject);
        assertSame("There must be a managed connection factory in the password credential.",
                   managedConnectionFactory, passwordCredential.getManagedConnectionFactory());
    }

    @Test
    public void getSubject_SubjectIsOptimized() throws Exception {
        ManagedConnectionFactory mcf = mockery.mock(ManagedConnectionFactory.class);
        Subject subject = getSubjectFromAuthDataService(mcf, jaasEntryName, AUTH_DATA_ALIAS);

        assertTrue("The subject must be read only.", subject.isReadOnly());
    }

    @Test
    public void getSubject_DynamicConfigUpdate_ReturnsNewSubject() throws Exception {
        ManagedConnectionFactory mcf = mockery.mock(ManagedConnectionFactory.class);
        Subject firstSubject = getSubjectFromAuthDataService(mcf, jaasEntryName, AUTH_DATA_ALIAS);
        authDataProviderTestHelper.unsetAuthData(authDataConfigRef);
        authDataProviderTestHelper.setAuthData(authDataConfigRef);
        Subject secondSubject = getSubjectFromAuthDataService(mcf, jaasEntryName, AUTH_DATA_ALIAS);

        assertNotSame("A new subject must be created.", firstSubject, secondSubject);
    }

    @Test
    public void getSubject_AuthDataAliasWithConfigDisplayId() throws Exception {
        String actualUserName = getUserName(jaasEntryName, AUTH_DATA_ALIAS_DISPLAY_ID1);
        assertEquals("There must be a user in the password credential.", TEST_USER, actualUserName);
        actualUserName = getUserName(jaasEntryName, AUTH_DATA_ALIAS_DISPLAY_ID2);
        assertEquals("There must be a different user in the password credential.", ANOTHER_TEST_USER, actualUserName);
    }

    /*
     * Test that a JAAS login is performed and that the callback handler is used to retrieve
     * the ManagedConnectionFactory and properties. The properties are being set in the subject
     * for this test only and a regular login modules does not need to do it.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getSubjectUsingJAAS() throws Exception {
        createJAASLoginExpectations();
        ManagedConnectionFactory managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class);
        Map<String, Object> loginData = new HashMap<String, Object>();
        loginData.put("com.ibm.mapping.authDataAlias", "someAlias");

        Subject subject = authDataServiceImpl.getSubject(managedConnectionFactory, JAAS_ENTRY_NAME, loginData);
        PasswordCredential passwordCredential = getPasswordCredential(subject);
        Map<String, Object> properties = getProperties(subject);

        assertSame("There must be a managed connection factory in the password credential.",
                   managedConnectionFactory, passwordCredential.getManagedConnectionFactory());
        assertSame("The properties must be set in the subject as obtained from the WSMappingPropertiesCallback", loginData, properties);
        assertTrue("There must not be a principal in the subject.", subject.getPrincipals().isEmpty());
    }

    @Test
    public void getSubjectUsingJAAS_PrincipalInSubject() throws Exception {
        createJAASLoginExpectations();
        Principal invocationSubjectPrincipal = prepareSubjectManagerServiceReturningPrincipal();

        ManagedConnectionFactory managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class);
        Map<String, Object> loginData = new HashMap<String, Object>();
        loginData.put("com.ibm.mapping.authDataAlias", "someAlias");

        Subject subject = authDataServiceImpl.getSubject(managedConnectionFactory, JAAS_ENTRY_NAME, loginData);

        Set<WSPrincipal> wsPrincipals = subject.getPrincipals(WSPrincipal.class);
        WSPrincipal wsPrincipal = wsPrincipals.iterator().next();
        assertEquals("There invocation subject principal must be in the subject.", invocationSubjectPrincipal, wsPrincipal);
    }

    @SuppressWarnings("unchecked")
    private Principal prepareSubjectManagerServiceReturningPrincipal() {
        final ServiceReference<SubjectManagerService> subjectManagerServiceReference = mockery.mock(ServiceReference.class, "subjectManagerServiceReference");
        final SubjectManagerService subjectManagerService = mockery.mock(SubjectManagerService.class);
        final Subject invocationSubject = new Subject();
        Principal invocationSubjectPrincipal = new WSPrincipal("user1securityName", "userAccessId", "test");
        invocationSubject.getPrincipals().add(invocationSubjectPrincipal);

        mockery.checking(new Expectations() {
            {
                one(cc).locateService(SubjectManagerService.KEY_SUBJECT_MANAGER_SERVICE, subjectManagerServiceReference);
                will(returnValue(subjectManagerService));
                one(subjectManagerService).getInvocationSubject();
                will(returnValue(invocationSubject));
            }
        });
        authDataServiceImpl.setSubjectManagerService(subjectManagerServiceReference);
        return invocationSubjectPrincipal;
    }

    private void createJAASLoginExpectations() {
        final AppConfigurationEntry[] appConfigurationEntries = new AppConfigurationEntry[1];
        Map<String, Object> options = new HashMap<String, Object>();
        final DoneMarker loginModuleInitializationDoneMarker = mockery.mock(DoneMarker.class, "loginModuleInitializationDoneMarker");
        options.put("loginModuleInitializationDoneMarker", loginModuleInitializationDoneMarker);
        final DoneMarker loginDoneMarker = mockery.mock(DoneMarker.class, "loginDoneMarker");
        options.put("loginDoneMarker", loginDoneMarker);
        appConfigurationEntries[0] = new AppConfigurationEntry("com.ibm.ws.security.jca.internal.TestLoginModule", LoginModuleControlFlag.REQUIRED, options);
        mockery.checking(new Expectations() {
            {
                one(testJaasConfiguration).getAppConfigurationEntry(JAAS_ENTRY_NAME);
                will(returnValue(appConfigurationEntries));
                one(loginModuleInitializationDoneMarker).mark();
                one(loginDoneMarker).mark();
            }
        });
    }

    /*
     * The properties must be set in the subject in the TestLoginModule to test that they can be obtained
     * from the WSMappingPropertiesCallback. Normal login modules are not required to do this.
     */
    @SuppressWarnings("rawtypes")
    private Map getProperties(Subject subject) {
        Set<Map> propertiesSet = subject.getPrivateCredentials(Map.class);
        Iterator<Map> propertiesSetIterator = propertiesSet.iterator();
        if (propertiesSetIterator.hasNext()) {
            return propertiesSetIterator.next();
        }
        return null;
    }

    private Subject getSubjectFromAuthDataService(String jaasEntryName, String authDataAlias) throws Exception {
        ManagedConnectionFactory managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class, authDataAlias);
        return getSubjectFromAuthDataService(managedConnectionFactory, jaasEntryName, authDataAlias);
    }

    private Subject getSubjectFromAuthDataService(ManagedConnectionFactory mcf, String jaasEntryName, String authDataAlias) throws Exception {
        Map<String, Object> loginData = new HashMap<String, Object>();
        if (authDataAlias != null) {
            loginData.put("com.ibm.mapping.authDataAlias", authDataAlias);
        }
        Subject subject = authDataServiceImpl.getSubject(mcf, jaasEntryName, loginData);
        return subject;
    }

    private String getUserName(String jaasEntryName, String authDataAlias) throws Exception {
        Subject subject = getSubjectFromAuthDataService(jaasEntryName, authDataAlias);
        PasswordCredential passwordCredential = getPasswordCredential(subject);
        return passwordCredential != null ? passwordCredential.getUserName() : null;
    }

    private char[] getPassword(String jaasEntryName, String authDataAlias) throws Exception {
        Subject subject = getSubjectFromAuthDataService(jaasEntryName, authDataAlias);
        PasswordCredential passwordCredential = getPasswordCredential(subject);
        return passwordCredential != null ? passwordCredential.getPassword() : null;
    }

    private PasswordCredential getPasswordCredential(Subject subject) {
        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        Iterator<PasswordCredential> passwordCredentialsIterator = passwordCredentials.iterator();
        return passwordCredentialsIterator.next();
    }

    private Map<String, Object> createProps(boolean enabled, int maxSize) {
        Map<String, Object> props = new HashMap<String, Object>();
        return props;
    }

    interface DoneMarker {
        public void mark();
    }

}
