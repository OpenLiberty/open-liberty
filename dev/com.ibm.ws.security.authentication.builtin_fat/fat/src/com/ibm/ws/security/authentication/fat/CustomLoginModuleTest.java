/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class CustomLoginModuleTest {

    // Keys to help readability of the test
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;

    private final String employeeUser = "user1";
    private final String employeePassword = "user1pwd";
    private final String managerUser = "user2";
    private final String managerPassword = "user2pwd";
    private final String CustomLoginModuleCredential = "Private Credential: CustomLoginModuleCredential";
    private final String CustomUserName1 = "Private Credential: username = user1";
    private final String CustomPassword1 = "Private Credential: password = user1pwd";
    private final String CustomUserName2 = "Private Credential: username = user2";
    private final String CustomPassword2 = "Private Credential: password = user2pwd";
    private final String CustomRealm = "Private Credential: realm = defaultRealm";
    private final String expectedInvalidClassException = "java.lang.RuntimeException: invalid.classname";
    private final String expectedNoDelegateException = "java.lang.IllegalArgumentException: CWWKS1108E: WSLoginModuleProxy delegate option is not set";
    private final String expectedNoOptionsException = "java.lang.IllegalArgumentException: CWWKS1107E: WSLoginModuleProxy options is null or empty";

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.authentication.fat.jaasloginmodule");
    private static Class<?> logClass = CustomLoginModuleTest.class;
    private static BasicAuthClient baServletClient;
    private static BasicAuthClient apiServletClient;
    private static BasicAuthClient jaasServletClient;
    private static FormLoginClient flServletClient;
    private static FormLoginClient flPageSessionFalseServletClient;

    private final static String DEFAULT_CONFIG_FILE = "jaasloginmodule.server.xml.orig";
    private final static String JAAS_CUSTOM_AT_END_CONFIG_FILE = "jaasConfigurationCustomAtEnd.xml";
    private final static String JAAS_LOGIN_MODULE_NONE_CONFIG_FILE = "jaasLoginModuleNone.xml";
    private final static String JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE = "jaasLoginModuleDefault.xml";
    private final static String JAAS_LOGIN_MODULE_PROXY_WSLOGIN_CONFIG_FILE = "jaasLoginModuleProxy_WSLogin.xml";
    private final static String JAAS_LOGIN_MODULE_PROXY_WEB_INBOUND_CONFIG_FILE = "jaasLoginModuleProxy_WEB_INBOUND.xml";
    private final static String JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE = "jaasLoginModuleCustom.xml";
    private final static String JAAS_LOGIN_MODULE_CUSTOM_LIBRARY_LIB2_CONFIG_FILE = "jaasLoginModuleCustomLibraryLib2.xml";
    private final static String JAAS_LOGIN_MODULE_PROXY_AND_CUSTOM_CONFIG_FILE = "jaasLoginModuleProxyAndCustom.xml";
    private final static String JAAS_LOGIN_MODULE_CUSTOM_USING_PROXY_CONFIG_FILE = "jaasLoginModuleCustomUsingProxy.xml";
    private final static String JAAS_LOGIN_MODULE_DELEGATE_TO_INVALID_CLASS_CONFIG_FILE = "jaasLoginModuleProxyDelegateToInvalidClass.xml";
    private final static String JAAS_LOGIN_MODULE_NO_DELEGATE_CONFIG_FILE = "jaasLoginModuleProxyNoDelegate.xml";
    private final static String JAAS_LOGIN_MODULE_NO_OPTIONS_CONFIG_FILE = "jaasLoginModuleProxyNoOptions.xml";
    private final static String JAAS_LOGIN_MODULE_LTPA_TOKEN_AND_AUTH_CACHE_EXPIRE_CONFIG_FILE = "LTPATokenAndAuthCacheExpire.xml";
    private final static String JAAS_LOGIN_MODULE_LTPA_TOKEN_EXPIRE_CONFIG_FILE = "LTPATokenExpire.xml";
    private final static String SKIP_LOGIN_MODULE_PROXY_PROPERTY = "-Dcom.ibm.websphere.security.skipWASProxyForJaasConfigFile";
    private final static String WSJAAS_CONF_FILE = "${server.config.dir}/resources/security/jaas/wsjaas.conf";
    private final static String AUTH_LOGIN_CONFIG_PROPERTY = "-Djava.security.auth.login.config";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static String SERVER_REALM = "BasicRealm";

    private static CommonTestHelper testHelper = new CommonTestHelper(myServer.getHostname(), String.valueOf(myServer.getHttpDefaultPort()));

    @BeforeClass
    public static void setUp() throws Exception {
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        myServer.addInstalledAppForValidation("jaasloginmodule");
        myServer.startServer(true);
        testHelper.verifyServerStarted(myServer);

        baServletClient = new BasicAuthClient(myServer);
        apiServletClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, "ProgrammaticAPIServlet", BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        jaasServletClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, "JAASServlet", BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        flServletClient = new FormLoginClient(myServer);
        flPageSessionFalseServletClient = new FormLoginClient(myServer, FormLoginClient.PROTECTED_SIMPLE, "/formloginPageSessionFalse");
    }

    @After
    public void resetConnection() {
        baServletClient.resetClientState();
        apiServletClient.resetClientState();
        jaasServletClient.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Reset server to use original configuration
        testHelper.stopServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
    }

    /**
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> CustomLoginModuleCredential, user, password, realm are found in the private credentials of the subject
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_BasicAuth() throws Exception {
        String methodName = "testCustomLoginModule_BasicAuth";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = baServletClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   baServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        verifyPrivateCredentials(response, CustomUserName1, CustomPassword1);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Attempt to access an unprotected servlet configured for basic authentication.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is allowed to the unprotected servlet.
     * <LI> The login() method is called with a valid user/password.
     * <LI> CustomLoginModuleCredential, user, password, realm are found in the private credentials of the subject
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_LoginMethod() throws Exception {
        String methodName = "testCustomLoginModule_LoginMethod";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String methods = "testMethod=login,logout";
        String queryString = BasicAuthClient.UNPROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             methods + "&user=" + managerUser + "&password=" + managerPassword;
        String response = apiServletClient.accessUnprotectedServlet(queryString);
        verifyPrivateCredentials(response, CustomUserName2, CustomPassword2);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> WSCallbackHandlerFactory is called in the servlet
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_CallBacks() throws Exception {
        String methodName = "testCustomLoginModule_CallBacks";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=callbackrealm", managerUser, managerPassword, employeeUser, employeePassword, SERVER_REALM);
        assertTrue(response.contains("Principal: WSPrincipal:" + managerUser));
        assertTrue(response.contains("request = <not null>"));
        assertTrue(response.contains("response = <not null>"));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> No JAAS login modules are configured in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for WSLogin is called in the servlet.
     * <LI> The subject should have a different user (user2) after login(), and then no subject after logout()
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_NoConfig_WSLogin() throws Exception {
        String methodName = "testCustomLoginModule_NoConfig_WSLogin";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> No JAAS login modules are configured in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for system.WEB_INBOUND is called in the servlet.
     * <LI> The subject should have a different user (user2) after login(), and then no subject after logout()
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_NoConfig_SystemWebInbound() throws Exception {
        String methodName = "testCustomLoginModule_NoConfig_SystemWebInbound";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> The default JAAS login modules are configured in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for WSLogin is called in the servlet.
     * <LI> The subject should have a different user (user2) after login(), and then no subject after logout()
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DefaultConfig_WSLogin() throws Exception {
        String methodName = "testCustomLoginModule_DefaultConfig_WSLogin";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /*
     * Config:
     * - Server.xml: <jaasLoginContextEntry id="WSLogin" name="WSLogin" loginModuleRef="proxy,hashtable,userNameAndPassword,certificate,token" />
     * - jvm.options: -Dcom.ibm.websphere.security.skipWASProxyForJaasConfigFile=true
     * - (wsjaas.conf not used)
     *
     * Description:
     * - The property being set should not affect the jaasLoginContextEntry set in the server.xml
     * - We expect for the loginModuleProxy to be found in the response
     */
    @Test
    public void testServerXmlJaas_skipWASProxyForJaasConfigFile_true() throws Exception {
        String methodName = "testServerXmlJaas_skipWASProxyForJaasConfigFile_true";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE);
        testHelper.addJVMOptionProperty(myServer, SKIP_LOGIN_MODULE_PROXY_PROPERTY, "true");

        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=jaasConfigFile&jaasContextEntry=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyLoginModuleProxyIsFound(response);

        testHelper.removeJVMOptionProperty(myServer, SKIP_LOGIN_MODULE_PROXY_PROPERTY);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /*
     * Config:
     * - Server.xml: No jaasLoginContextEntry specified
     * - jvm.options: -Dcom.ibm.websphere.security.skipWASProxyForJaasConfigFile=true
     * -Djava.security.auth.login.config=${server.config.dir}/resources/security/jaas/wsjaas.conf
     * - wsjaas.conf: CustomIdentityTest {
     * com.ibm.ws.security.authentication.fat.IdentityAssertionLoginModule required;
     * com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule required;};
     * Description:
     * - The property being set to true should cause the loginModuleProxy to be skipped
     * - We expect for the loginModuleProxy to NOT be found in the response
     * - We expect the 2 loginModules listed in the wsjaas.conf above to be found in the response
     */
    @Test
    public void testJaasConf_skipWASProxyForJaasConfigFile_true() throws Exception {
        String methodName = "testJaasConf_skipWASProxyForJaasConfigFile_true";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(SKIP_LOGIN_MODULE_PROXY_PROPERTY, "true");
        props.put(AUTH_LOGIN_CONFIG_PROPERTY, WSJAAS_CONF_FILE);
        testHelper.addJVMOptionPropeties(myServer, props);

        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=jaasConfigFile&jaasContextEntry=CustomIdentityTest", managerUser,
                                                     managerPassword,
                                                     employeeUser,
                                                     employeePassword, null);
        verifyLoginModuleProxyIsNotFound(response);
        verifyLoginModuleFoundInResponse(response, "LoginModuleName: com.ibm.ws.security.authentication.fat.IdentityAssertionLoginModule");
        verifyLoginModuleFoundInResponse(response, "LoginModuleName: com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule");

        testHelper.removeJVMOptionProperties(myServer, SKIP_LOGIN_MODULE_PROXY_PROPERTY, AUTH_LOGIN_CONFIG_PROPERTY);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /*
     * Config:
     * - Server.xml: No jaasLoginContextEntry specified
     * - jvm.options: -Djava.security.auth.login.config=${server.config.dir}/resources/security/jaas/wsjaas.conf
     * - wsjaas.conf: CustomIdentityTest {
     * com.ibm.ws.security.authentication.fat.IdentityAssertionLoginModule required;
     * com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule required;};
     * Description:
     * - Because the property is not set in the jvm.options, we expect the
     * - We expect for the loginModuleProxy to NOT be found in the response
     * - We expect the 2 loginModules listed in the wsjaas.conf above to be found in the response
     */
    @Test
    public void testJaasConf_skipWASProxyForJaasConfigFile_notSet() throws Exception {
        String methodName = "testJaasConf_skipWASProxyForJaasConfigFile_notSet";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        testHelper.addJVMOptionProperty(myServer, AUTH_LOGIN_CONFIG_PROPERTY, WSJAAS_CONF_FILE);

        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=jaasConfigFile&jaasContextEntry=CustomIdentityTest", managerUser, managerPassword,
                                                     employeeUser,
                                                     employeePassword, null);
        verifyLoginModuleProxyIsFound(response);

        testHelper.removeJVMOptionProperty(myServer, AUTH_LOGIN_CONFIG_PROPERTY);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    @Test
    public void testJaasConf_skipWASProxyForJaasConfigFile_false() throws Exception {
        String methodName = "testJaasConf_skipWASProxyForJaasConfigFile_false";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(SKIP_LOGIN_MODULE_PROXY_PROPERTY, "false");
        props.put(AUTH_LOGIN_CONFIG_PROPERTY, WSJAAS_CONF_FILE);
        testHelper.addJVMOptionPropeties(myServer, props);

        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=jaasConfigFile&jaasContextEntry=CustomIdentityTest", managerUser, managerPassword,
                                                     employeeUser,
                                                     employeePassword, null);
        verifyLoginModuleProxyIsFound(response);

        testHelper.removeJVMOptionProperties(myServer, SKIP_LOGIN_MODULE_PROXY_PROPERTY, AUTH_LOGIN_CONFIG_PROPERTY);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> The default JAAS login modules are configured in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for system.WEB_INBOUND is called in the servlet.
     * <LI> The subject should have a different user (user2) after login(), and then no subject after logout()
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DefaultConfig_SystemWebInbound() throws Exception {
        String methodName = "testCustomLoginModule_DefaultConfig_SystemWebInbound";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> WSLoginModuleProxy delegates to WSLoginModuleImpl in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for WSLogin is called in the servlet.
     * <LI> The subject should have a different user (user2) after login(), and then no subject after logout()
     * </OL>
     */
    @Test
    public void testCustomLoginModule_Proxy_WSLogin() throws Exception {
        String methodName = "testCustomLoginModule_Proxy_WSLogin";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_PROXY_WSLOGIN_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> WSLoginModuleProxy delegates to WSLoginModuleImpl in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for system.WEB_INBOUND is called in the servlet.
     * <LI> The subject should have a different user (user2) after login(), and then no subject after logout()
     * </OL>
     */
    @Test
    public void testCustomLoginModule_Proxy_SystemWebInbound() throws Exception {
        String methodName = "testCustomLoginModule_Proxy_SystemWebInbound";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_PROXY_WEB_INBOUND_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> A custom login module is configured in the server.xml
     * <LI> WSLoginModuleProxy delegates to WSLoginModuleImpl in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for WSLogin is called in the servlet.
     * <LI> With fix from task 62499, custom login module class is now available through shared lib class loader, therefore...
     * <LI> The subject should have the custom info and a different user (user2) after login(), and then no subject after logout
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_ProxyAndCustom_WSLogin() throws Exception {
        String methodName = "testCustomLoginModule_ProxyAndCustom_WSLogin";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_PROXY_AND_CUSTOM_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyPrivateCredentials(response, managerUser, managerPassword);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> WSLoginModuleProxy delegates to custom login module in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for WSLogin is called in the servlet.
     * <LI> The subject should have the custom info and a different user (user2) after login(), and then no subject after logout(
     * </OL>
     */
    @Test
    public void testCustomLoginModule_CustomUsingProxy_WSLogin() throws Exception {
        String methodName = "testCustomLoginModule_CustomUsingProxy_WSLogin";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_USING_PROXY_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        verifyPrivateCredentials(response, managerUser, managerPassword);
        verifyLoginContextCredentials(response, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /*
     * <OL>
     * <LI> WSLoginModuleProxy delegates to an invalid class in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for WSLogin is called in the servlet.
     * <LI> Since proxy delegates to an invalid class, expect an exception
     * </OL>
     */
    //TODO
    @Ignore
    //Expect an FFDC here to let users know delegate class is invalid
    @ExpectedFFDC("java.lang.ClassNotFoundException")
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_DelegateToInvalidClass_WSLogin() throws Exception {
        String methodName = "testCustomLoginModule_DelegateToInvalidClass_WSLogin";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DELEGATE_TO_INVALID_CLASS_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        assertTrue("Did NOT get expected exception: " + expectedInvalidClassException, response.contains(expectedInvalidClassException));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /*
     * <OL>
     * <LI> WSLoginModuleProxy does NOT have delegate property in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() for WSLogin is called in the servlet.
     * <LI> Since no delegate property in server.xml, expect an exception
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_NoDelegate_WSLogin() throws Exception {
        String methodName = "testCustomLoginModule_NoDelegate_WSLogin";
        Log.info(logClass, methodName, "Entering test " + methodName);

        myServer.addIgnoredErrors(Arrays.asList("CWWKS1108E"));
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NO_DELEGATE_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);
        assertTrue("Did NOT get expected exception: " + expectedNoDelegateException, response.contains(expectedNoDelegateException));

        myServer.addIgnoredErrors(Arrays.asList("CWWKS1108E"));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Attempt to access an unprotected servlet configured for basic authentication.
     * <LI> The server.xml has "custom" at the end of the jaasConfiguration
     * <LI> Only system.WEB_INBOUND is specified in the jaasConfiguration
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is allowed to the unprotected servlet.
     * <LI> The login() method is called with a valid user/password.
     * <LI> CustomLoginModuleCredential, user, password, realm are found in the private credentials of the subject
     * </OL>
     */
    @Test
    public void testCustomLoginModule_CustomAtEnd() throws Exception {
        String methodName = "testCustomLoginModule_CustomAtEnd";
        Log.info(logClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, JAAS_CUSTOM_AT_END_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        String methods = "testMethod=login,logout";
        String queryString = BasicAuthClient.UNPROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             methods + "&user=" + managerUser + "&password=" + managerPassword;
        String response = apiServletClient.accessUnprotectedServlet(queryString);
        verifyPrivateCredentials(response, CustomUserName2, CustomPassword2);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> The default jaas login modules are configured in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() with user1 for system.WEB_INBOUND is called in the servlet
     * <LI> Dynamic update of server.xml to configure with custom login module
     * <LI> Access servlet with user2 and LoginContext.login() with user2. Verify output shows custom info
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DynamicUpdate_AddCustom() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_AddCustom";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with no custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access with user1
        String response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response1, employeeUser);

        //Set config with custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user2
        String response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        verifyPrivateCredentials(response2, CustomUserName2, CustomPassword2);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> No jaas login modules are configured in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() with user1 for system.WEB_INBOUND is called in the servlet
     * <LI> Dynamic update of server.xml to configure with custom login module
     * <LI> Access servlet with user2 and LoginContext.login() with user2. Verify output shows custom info
     * <LI> Remove custom login module, then add proxy and verify
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_DynamicUpdate_AddCustomRemoveCustomAddProxy() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_AddCustomRemoveCustomAddProxy";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with no custom login module and restart so custom login module at startup
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access with user1
        String response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response1, employeeUser);

        //Add custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user2
        String response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        verifyPrivateCredentials(response2, CustomUserName2, CustomPassword2);

        //Remove custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);
        jaasServletClient.resetClientState();

        //Access with user1
        response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response1, employeeUser);

        //Add proxy login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_PROXY_WEB_INBOUND_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user2
        response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        verifyLoginContextCredentials(response2, managerUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> A custom login module is configured in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() with user1 for system.WEB_INBOUND is called in the servlet, showing custom info
     * <LI> Dynamic update of server.xml to configure with proxy but no custom login module
     * <LI> Access servlet with user2 and LoginContext.login() with user2. Verify does not show custom info
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DynamicUpdate_CustomThenProxy() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_CustomThenProxy";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config to custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access with user1
        String response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyPrivateCredentials(response1, CustomUserName1, CustomPassword1);

        //Set config with proxy
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_PROXY_WEB_INBOUND_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user2
        String response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        assertFalse(CustomLoginModuleCredential + " not found in subject", response2.contains(CustomLoginModuleCredential));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> A custom login module is configured for customLibraryLib in the server.xml
     * <LI> A valid user (user1) and password is permitted access to the protected servlet.
     * <LI> LoginContext.login() with user1 for system.WEB_INBOUND is called in the servlet, showing custom info
     * <LI> Dynamic update of server.xml to configure custom login module for customLibraryLib2
     * <LI> Access servlet with user2 and LoginContext.login() with user2. Verify shows custom info
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DynamicUpdate_ChangeLibraryRef() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_ChangeLibraryRef";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access with user1
        String response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyPrivateCredentials(response1, CustomUserName1, CustomPassword1);

        //Set config with custom login module with different customLibraryLib
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_LIBRARY_LIB2_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user2
        String response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        verifyPrivateCredentials(response2, CustomUserName2, CustomPassword2);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Form login, verify custom info in subject, then form logout to clear authCache
     * <LI> Next, update server.xml to remove custom login module, then form login again and verify that custom info
     * <LI> is not in subject since authCache should be clear after logout
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DynamicUpdate_CheckAuthCache() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_CheckAuthCache";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Form Login with user1, then form logout to clear authCache
        flServletClient.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE, employeeUser, employeePassword);

        //Set config with NO custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);

        //Form Login with user1 in a new session. Should not see custom info since it is cleared from authCache
        HttpClient client = new DefaultHttpClient();
        String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() +
                     "/formlogin" + FormLoginClient.PROTECTED_SIMPLE;
        String response = flServletClient.accessAndAuthenticate(client, url, employeeUser, employeePassword, 200);
        assertTrue("Verification of programmatic APIs failed",
                   flServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        assertFalse(CustomLoginModuleCredential + " should not be found in subject after form logout", response.contains(CustomLoginModuleCredential));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Basic auth login, verify custom info in subject, then call logout() method to clear authCache
     * <LI> Next, update server.xml to remove custom login module, then login with SSO cookie and verify that custom info
     * <LI> is not in subject since authCache should be clear after logout
     * </OL>
     */
    @ExpectedFFDC("com.ibm.websphere.security.auth.InvalidTokenException")
    @Test
    public void testCustomLoginModule_DynamicUpdate_CheckAuthCacheWithCookie() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_CheckAuthCacheWithCookie";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Log in and get the SSO Cookie
        String queryString = BasicAuthClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?testMethod=login" +
                             "&user=" + managerUser + "&password=" + managerPassword;
        String response = apiServletClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   apiServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        String ssoCookie = apiServletClient.getCookieFromLastLogin();
        assertNotNull("The SSO cookie was not set for the last login (it was null)", ssoCookie);

        //Call logout() method to clear authCache
        queryString = BasicAuthClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?testMethod=logout" +
                      "&user=" + managerUser + "&password=" + managerPassword;
        response = apiServletClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed", apiServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        //Set config with NO custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NONE_CONFIG_FILE);
        //Reset the client state so we get a new session
        apiServletClient.resetClientState();

        //Login with SSO cookie and verify we don't see custom info since it is cleared from authCache
        queryString = BasicAuthClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?testMethod=login" +
                      "&user=" + managerUser + "&password=" + managerPassword;
        response = apiServletClient.accessProtectedServletWithAuthorizedCookie(queryString, ssoCookie);
        assertTrue("Verification of programmatic APIs failed",
                   apiServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        assertFalse(CustomLoginModuleCredential + " should not be found in subject after form logout", response.contains(CustomLoginModuleCredential));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> server.xml has library nested in jaasLoginModule element
     * <LI> Dynamic update of server.xml to a different config with library nested in jaasLoginModule element
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_DynamicUpdate_NestedToNestedLibrary() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_NestedToNestedLibrary";
        Log.info(logClass, methodName, "Entering test " + methodName);

        myServer.addIgnoredErrors(Arrays.asList("CWWKS1108E"));
        //Set config with no options and library nested in jaasLoginModule
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_NO_OPTIONS_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access and expect exception
        String response = accessProtectedJAASServlet("testMethod=WSLogin", managerUser, managerPassword, employeeUser, employeePassword, null);

        //Set config with custom login module and library nested in jaasLoginModule
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user2
        String response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        verifyPrivateCredentials(response2, CustomUserName2, CustomPassword2);

        myServer.addIgnoredErrors(Arrays.asList("CWWKS1108E"));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> server.xml has library nested in jaasLoginModule element
     * <LI> Dynamic update of server.xml to a different config with library NOT nested in jaasLoginModule element
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_DynamicUpdate_NestedToNonNestedLibrary() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_NestedToNonNestedLibrary";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with custom login module and library nested in jaasLoginModule
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access with user2
        String response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        verifyPrivateCredentials(response2, CustomUserName2, CustomPassword2);

        //Set config with no custom login module and library NOT nested
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user1
        String response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response1, employeeUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> server.xml has library NOT nested in jaasLoginModule element
     * <LI> Dynamic update of server.xml to a different config with library NOT nested in jaasLoginModule element
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DynamicUpdate_NonNestedToNonNestedLibrary() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_NonNestedToNonNestedLibrary";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with custom login module and library NOT nested
        setServerConfiguration(myServer, JAAS_CUSTOM_AT_END_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access with user2
        String methods = "testMethod=login,logout";
        String queryString = BasicAuthClient.UNPROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             methods + "&user=" + managerUser + "&password=" + managerPassword;
        String response = apiServletClient.accessUnprotectedServlet(queryString);
        verifyPrivateCredentials(response, CustomUserName2, CustomPassword2);

        //Set config with no custom login module and library NOT nested
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user1
        String response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response1, employeeUser);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> server.xml has library NOT nested in jaasLoginModule element
     * <LI> Dynamic update of server.xml to a different config with library nested in jaasLoginModule element
     * </OL>
     */
    @Test
    public void testCustomLoginModule_DynamicUpdate_NonNestedToNestedLibrary() throws Exception {
        String methodName = "testCustomLoginModule_DynamicUpdate_NonNestedToNestedLibrary";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with no custom login module and library NOT nested
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_DEFAULT_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Access with user1
        String response1 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", employeeUser, employeePassword, employeeUser, employeePassword, null);
        verifyLoginContextCredentials(response1, employeeUser);

        //Set config with custom login module and library nested in jaasLoginModule
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_CUSTOM_CONFIG_FILE);
        jaasServletClient.resetClientState();

        //Access with user2
        String response2 = accessProtectedJAASServlet("testMethod=system.WEB_INBOUND", managerUser, managerPassword, managerUser, managerPassword, null);
        verifyPrivateCredentials(response2, CustomUserName2, CustomPassword2);

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Form login after LTPA token and AuthCache timeout
     * <LI> With session security enabled, we get an UnauthorizedSessionRequestException with form login after the LTPA token expires in Liberty and tWAS.
     * <LI> To workaround this exception, set <%@ page session="false"%> in the form login page so we get prompted after the LTPA token expires
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testCustomLoginModule_LTPATokenAndAuthCacheExpire() throws Exception {
        String methodName = "testCustomLoginModule_LTPATokenAndAuthCacheExpire";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_LTPA_TOKEN_AND_AUTH_CACHE_EXPIRE_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Form Login with user1 in a new session
        HttpClient client = new DefaultHttpClient();
        String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() +
                     "/formloginPageSessionFalse" + FormLoginClient.PROTECTED_SIMPLE;
        String response = flPageSessionFalseServletClient.accessAndAuthenticate(client, url, employeeUser, employeePassword, 200);
        assertTrue("Verification of programmatic APIs failed",
                   flPageSessionFalseServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        // Sleep for 120 seconds to let authCache and LTPA token expire
        Log.info(logClass, methodName, "Sleeping for 120 seconds...");
        Thread.sleep(120000);

        //Form Login with user1 in the same session. Should get prompted
        response = flPageSessionFalseServletClient.accessAndAuthenticate(client, url, employeeUser, employeePassword, 200);
        assertTrue("Verification of programmatic APIs failed",
                   flPageSessionFalseServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Form login after LTPA token timeout
     * <LI> With session security enabled, we get an UnauthorizedSessionRequestException with form login after the LTPA token expires in Liberty and tWAS.
     * <LI> To workaround this exception, set <%@ page session="false"%> in the form login page so we get prompted after the LTPA token expires
     * </OL>
     */
    @Test
    public void testCustomLoginModule_LTPATokenExpire() throws Exception {
        String methodName = "testCustomLoginModule_LTPATokenExpire";
        Log.info(logClass, methodName, "Entering test " + methodName);

        //Set config with custom login module
        setServerConfiguration(myServer, JAAS_LOGIN_MODULE_LTPA_TOKEN_EXPIRE_CONFIG_FILE);
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        restartServer();
        testHelper.verifyServerStarted(myServer);

        //Form Login with user1 in a new session
        HttpClient client = new DefaultHttpClient();
        String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() +
                     "/formloginPageSessionFalse" + FormLoginClient.PROTECTED_SIMPLE;
        String response = flPageSessionFalseServletClient.accessAndAuthenticate(client, url, employeeUser, employeePassword, 200);
        assertTrue("Verification of programmatic APIs failed",
                   flPageSessionFalseServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        // Sleep for 120 seconds to let LTPA token expire. AuthCache is also cleared when token expires
        Log.info(logClass, methodName, "Sleeping for 120 seconds...");
        Thread.sleep(120000);

        //Form Login with user1 in the same session. Should get prompted
        response = flPageSessionFalseServletClient.accessAndAuthenticate(client, url, employeeUser, employeePassword, 200);
        assertTrue("Verification of programmatic APIs failed",
                   flPageSessionFalseServletClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        Log.info(logClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Helper methods
     */
    public String accessProtectedJAASServlet(String methods, String basicAuthUser, String basicAuthPassword, String servletUser, String servletPassword, String realm) {
        String queryString = BasicAuthClient.PROTECTED_JAAS_SERVLET + "?" + methods + "&user=" + basicAuthUser + "&password=" + basicAuthPassword + "&realm=" + realm;
        String response = jaasServletClient.accessProtectedServletWithAuthorizedCredentials(queryString, servletUser, servletPassword);
        return response;
    }

    public void verifyPrivateCredentials(String response, String user, String password) {
        assertTrue(CustomLoginModuleCredential + " not found in subject", response.contains(CustomLoginModuleCredential));
        assertTrue(user + " not found in subject", response.contains(user));
        assertTrue(password + " not found in subject", response.contains(password));
        assertTrue(CustomRealm + " not found in subject", response.contains(CustomRealm));
    }

    public void verifyLoginContextCredentials(String response, String user) {
        // check values after login
        String loginText = response.substring(response.indexOf("STARTCTXLOGIN"), response.indexOf("ENDCTXLOGIN"));
        assertTrue("Did not get expected " + managerUser + " in subject after LoginContext.login()", loginText.contains("Principal: WSPrincipal:" + user));

        // check values after logout
        String logoutText = response.substring(response.indexOf("STARTCTXLOGOUT"), response.indexOf("ENDCTXLOGOUT"));
        assertFalse("Still seeing " + managerUser + " in subject after LoginContext.logout()", logoutText.contains(user));
    }

    public static void verifyLoginModuleProxyIsFound(String response) {
        assertLoginModuleIsPrinted(response);

        String LoginModuleNamesText = response.substring(response.indexOf("printLoginModuleNames Entry"), response.indexOf("printLoginModuleNames Exit"));
        assertTrue("Expected LoginModuleName: com.ibm.ws.kernel.boot.security.LoginModuleProxy in response: " + LoginModuleNamesText,
                   LoginModuleNamesText.contains("LoginModuleName: com.ibm.ws.kernel.boot.security.LoginModuleProxy"));
    }

    public static void verifyLoginModuleFoundInResponse(String response, String moduleName) {
        assertLoginModuleIsPrinted(response);

        String LoginModuleNamesText = response.substring(response.indexOf("printLoginModuleNames Entry"), response.indexOf("printLoginModuleNames Exit"));
        assertTrue("Expected LoginModuleName: com.ibm.ws.kernel.boot.security.LoginModuleProxy in response: " + LoginModuleNamesText,
                   LoginModuleNamesText.contains(moduleName));
    }

    public static void verifyLoginModuleProxyIsNotFound(String response) {
        assertLoginModuleIsPrinted(response);

        String LoginModuleNamesText = response.substring(response.indexOf("printLoginModuleNames Entry"), response.indexOf("printLoginModuleNames Exit"));
        assertTrue("Found LoginModuleName: com.ibm.ws.kernel.boot.security.LoginModuleProxy in response ",
                   !LoginModuleNamesText.contains("LoginModuleName: com.ibm.ws.kernel.boot.security.LoginModuleProxy"));
    }

    /**
     * Assert that the response contains the LoginModuleName String
     */
    private static void assertLoginModuleIsPrinted(String response) {
        assertTrue("printLoginModuleNames method was not called as expected", response.contains("printLoginModuleNames Entry"));
        assertTrue("Did not find any LoginModuleNames returned in the response", response.contains("LoginModuleName:"));
    }

    /**
     * This method is used to set the server.xml
     */
    private static void setServerConfiguration(LibertyServer server,
                                               String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            testHelper.setServerConfiguration(server, serverXML);
            serverConfigurationFile = serverXML;
        }
    }

    /**
     * This method restarts the server without clearing the workspace cache in order to same the execution time.
     *
     * @throws Exception
     */
    private void restartServer() throws Exception {
        myServer.stopServer();
        myServer.startServer(false);
    }
}
