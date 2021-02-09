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

package com.ibm.ws.security.jaspic11.fat;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * The test verifies the JASPI AuthConfigFactory registerConfigProvider
 * supports the construction and registration of an AuthConfigProvider
 * from a persistent and declarative representation in the jaspiConfiguration.xml
 * file in the server directory. Below is a sample of the format of that file
 *
 * The tests start with an empty jaspiConfiguration.xml and access a protected servlet to make SPI calls to
 * register, get provider information and remove a provider. The servlet communicates the status of the calls back to the test in the
 * servlet response.
 *
 * Register provider:
 * - factory.registerConfigProvider(className, null, msgLayer, appContext, null);
 *
 * Get provider registration:
 * - factory.getConfigProvider(msgLayer, appContext, null)
 * - factory.getRegistrationIDs(provider)
 * - factory.getRegistrationContext(regIDs[0])
 *
 * Remove provider registration:
 * - factory.removeRegistration(regIDs[0])
 *
 * A sample jaspiConfiguration.xml file is shown below:
 * <?xml version="1.0" encoding="UTF-8"?>
 * <jaspiConfig>
 * <jaspiProvider description="test provider 1" providerName="bob"
 * className="com.ibm.ws.security.jaspi.test.AuthProvider">
 * <appContext>default_host / context-root</appContext>
 * <msgLayer>HttpServlet</msgLayer>
 * <option name="prop1" value="value1"/>
 * <option name="prop2" value="value2"/>
 * </jaspiProvider>
 * <jaspiProvider className="com.ibm.ws.security.jaspi.test.AuthProvider_1" providerName="joe" description="joe">
 * <appContext>joe_context</appContext>
 * <msgLayer>localhost</msgLayer>
 * </jaspiProvider>
 * </jaspiConfig>
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIServerPersistenceTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.persistence");
    protected static Class<?> logClass = JASPIServerPersistenceTest.class;
    protected static String contextString = "/JASPIRegistrationTestServlet/JASPIBasic";

    protected static String urlBase;
    private static final String PERSISTENT_FILE_LOCATION = "resources/security/";

    protected DefaultHttpClient httpclient;

    public JASPIServerPersistenceTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIRegistrationTestServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_REGISTRATION_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeature(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JASPIFatUtils.uninstallJaspiUserFeature(myServer);
        }
    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access protected test servlet to 1) Register a valid JASPI persistent provider with message layer and appContext,
     * <LI> 2) then get the provider information, 3) remove the provider and 4) finally get the provider to verify it was removed.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from all servlet calls
     * <LI> After registering the provider, the servlet returns: Successfully registered provider.
     * <LI> After getting the provider information, the servlet returns:
     * <LI> Message layer: HttpServlet
     * <LI> App Context: testApp1Context
     * <LI> IsPersistent: true
     * <LI> Provider Class: class com.ibm.ws.security.jaspi.test.AuthProvider_1
     * <LI> After removing the provider, the servlet returns: Successfully removed provider registration.
     * <LI> Getting the provider information after removal returns: Failed to get registered provider...
     * </OL>
     */
    @Test
    public void testJaspiSinglePersistentProvider_RegisteredAndRemoveNewAppContext_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(REGISTER, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                                 PERSISTENT_PROVIDER_CLASS),
                                                          jaspi_basicRoleUser,
                                                          jaspi_basicRolePwd, HttpServletResponse.SC_OK);

        verifyPersistentProviderRegisteredSuccessfully(response);
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          PERSISTENT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);

        verifyPersistentProviderInformation(response, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT, PERSISTENT_PROVIDER_CLASS);
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(REMOVE, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          PERSISTENT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser,
                                                   jaspi_basicRolePwd, HttpServletResponse.SC_OK);

        verifyPersistentProviderRemovedSuccessfully(response);

        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          PERSISTENT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);

        verifyPersistentProviderNotRegistered(response, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access protected test servlet to 1) Register a valid JASPI persistent provider with message layer and appContext,
     * <LI> 2) then get the provider information, 3) re-register the same message layer and app context with a different class.
     * <LI> The registration information should be replaced with the new provider class.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from all servlet calls
     * <LI> After registering the provider initially, the servlet returns showing the AppContext and message layer are registered for
     * <LI> the provider class com.ibm.ws.security.jaspi.test.AuthProvider_1.
     * <LI>
     * <LI> After registering the same message layer and App Context with a different provider, the servlet returns
     * <LI> showing that the registration is replaced with provider class com.ibm.ws.security.jaspi.test.AuthProvider.
     * </OL>
     */
    @Test
    public void testJaspiSinglePersistentProvider_RegisterContextWithDifferentProvider_ReplacesRegistration() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.copyFileToLibertyServerRoot(PERSISTENT_FILE_LOCATION, "jaspiPersistentConfig/jaspiConfiguration.xml");

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(REGISTER, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                                 PERSISTENT_PROVIDER_CLASS),
                                                          jaspi_basicRoleUser,
                                                          jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyPersistentProviderRegisteredSuccessfully(response);
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          PERSISTENT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        verifyPersistentProviderInformation(response, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT, PERSISTENT_PROVIDER_CLASS);
        // Re-register same appContext and messageLayer with different provider and verify registration updated.
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(REGISTER, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          DEFAULT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          DEFAULT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access protected test servlet to 1) Register two valid JASPI persistent provider with message layer and appContext,
     * <LI> 2) then get the provider information, 3) remove the provider and 4) finally get the provider to verify it was removed.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from all servlet calls
     * <LI> After registering the provider, the servlet returns: Successfully registered provider.
     * <LI> After getting the provider information, the servlet returns:
     * <LI> Message layer: HttpServlet
     * <LI> App Context: testApp1Context
     * <LI> IsPersistent: true
     * <LI> Provider Class: class com.ibm.ws.security.jaspi.test.AuthProvider_1
     * <LI> After removing the providers, the servlet returns: Successfully removed provider registration.
     * <LI> Getting the provider information after removal returns: Failed to get registered provider...
     * </OL>
     */
    @Test
    public void testJaspiMultiplePersistentProvider_RegisteredAndRemoveNewAppContext_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        //Register 2 persistent providers
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(REGISTER, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                                 DEFAULT_PROVIDER_CLASS),
                                                          jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyPersistentProviderRegisteredSuccessfully(response);

        response = executeGetRequestNoAuthCreds(httpclient, urlBase + contextString + buildQueryString(REGISTER, PROFILE_SERVLET_MSG_LAYER, TEST_APP2_CONTEXT,
                                                                                                       PERSISTENT_PROVIDER_CLASS),
                                                HttpServletResponse.SC_OK);
        verifyPersistentProviderRegisteredSuccessfully(response);

        //Get information for 2 persistent providers
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          DEFAULT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        verifyPersistentProviderInformation(response, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT, DEFAULT_PROVIDER_CLASS);
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP2_CONTEXT,
                                                                                                          PERSISTENT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);

        verifyPersistentProviderInformation(response, PROFILE_SERVLET_MSG_LAYER, TEST_APP2_CONTEXT, PERSISTENT_PROVIDER_CLASS);
        // Remove both persistent provider registrations.
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(REMOVE, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          DEFAULT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        verifyPersistentProviderRemovedSuccessfully(response);
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                          DEFAULT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        verifyPersistentProviderNotRegistered(response, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT);

        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(REMOVE, PROFILE_SERVLET_MSG_LAYER, TEST_APP2_CONTEXT,
                                                                                                          PERSISTENT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        verifyPersistentProviderRemovedSuccessfully(response);
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, PROFILE_SERVLET_MSG_LAYER, TEST_APP2_CONTEXT,
                                                                                                          PERSISTENT_PROVIDER_CLASS),
                                                   jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        verifyPersistentProviderNotRegistered(response, PROFILE_SERVLET_MSG_LAYER, TEST_APP2_CONTEXT);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access protected test servlet to register a provider with invalid class name.
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from the servlet and servlet response contains
     * <LI> Unable to create a provider, class name: BogusProviderClass
     * <LI> Servlet catches the java.Lang.SecurityException (Spec Appendix D: AuthConfigFactory, registerConfigProvider. The registerConfigProvider
     * <LI> should throw java.lang.SecurityException if "the caller does not have permission to register a provider at the
     * <LI> factory, or if the provider construction (given a non-null className) or registration fails."
     * </OL>
     */
    @AllowedFFDC("java.lang.ClassNotFoundException")
    @Test
    public void testJaspiPersistentProvider_RegisterInvalidClassName_ThrowsSecurityException() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient,
                                                          urlBase + contextString + buildQueryString(REGISTERINVALIDCLASS, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                     "BogusProviderClass"),
                                                          jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyPersistentProviderNotRegisteredWithInvalidClass(response, "BogusProviderClass");
        verifyException(response, EXCEPTION_JAVA_LANG_SECURITY, null);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access protected test servlet to register an invalid message layer and app context.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from call to servlet
     * <LI> After attempting to register the invalid layer and context, the servlet response contains the message:
     * <LI> Failed to get registered provider...
     * </OL>
     */
    @Test
    public void testJaspiPersistentProvider_GetInvalidLayerAndContext_ReturnsError() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + buildQueryString(GET, "BogusLayer", "BogusContext",
                                                                                                                 PERSISTENT_PROVIDER_CLASS),
                                                          jaspi_basicRoleUser,
                                                          jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyPersistentProviderNotRegistered(response, "BogusLayer", "BogusContext");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access protected test servlet to remove an invalid message layer and app context.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from all servlet calls
     * <LI> After attempting to remove the registration with invalid layer and context, the servlet response
     * <LI> contains the message -- Failed to remove registered provider....
     * </OL>
     */
    @Test
    public void testJaspiPersistentProvider_RemoveInvalidLayerAndContext_ReturnsError() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String response = executeGetRequestBasicAuthCreds(httpclient,
                                                          urlBase + contextString + buildQueryString(REMOVEINVALID, "BogusLayer", "BogusContext", PERSISTENT_PROVIDER_CLASS),
                                                          jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyPersistentProviderRemovalFailed(response, "BogusLayer", "BogusContext");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
