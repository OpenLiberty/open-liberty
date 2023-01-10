/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.security.jaspi.test.AuthModule;
/**
 * Test Description:
 *
 * The test verifies the JASPI AuthConfigFactory registerServerAuthModule
 * supports the construction and registration of a server auth module
 * from a persistent and declarative representation in the jaspiConfiguration.xml
 * file in the server directory. Below is a sample of the format of that file
 *
 * The tests start with an empty jaspiConfiguration.xml and access a protected servlet to make SPI calls to
 * register a server auth module, and remove a server auth modue. The servlet communicates the status of the calls back to the test in the
 * servlet response.
 *
 * Register server auth module:
 * - factory.registerServerAuthModule(serverAuthModule, context);
 *
 * Remove server auth module registration:
 * - factory.removeServerAuthModule(context)
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
public class JASPIServerAuthRegistrationModuleTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.serverauthregistration");
    protected static Class<?> logClass = JASPIServerAuthRegistrationModuleTest.class;
    protected static String contextString = "/JASPIServerAuthRegistrationTestServlet/JASPIBasic";

    protected static String urlBase;
    protected static String TEST_APP_CONTEXT_ID = "HttpServlet[default_host".concat(" /").concat("JASPIServerAuthRegistrationTestServlet]");
    protected static String UNKNOWN_CONTEXT_ID = "unknownContextID";

    protected DefaultHttpClient httpclient;

    public JASPIServerAuthRegistrationModuleTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIServerAuthRegistrationTestServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_SERVERAUTH_REGISTRATION_APP);

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
     * <LI> Access protected test servlet to 1) Register a valid JASPI server auth module with a context,
     * <LI> 2) verify the server auth module successfully registered 3) remove the server auth provider with 
     * <LI> the registration ID returned from the registration and 4) finally verify the server auth module was removed.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from all servlet calls
     * <LI> After registering the server auth module , the servlet returns: Successfully registered server auth module.
     * <LI> ServerAuthModule Class: class com.ibm.ws.security.jaspi.test.AuthModule
     * <LI> After removing the server auth module, the servlet returns: Successfully removed server auth module.
     * <LI> Getting the server auth module information after removal returns: Failed to get registered server auth module...
     * </OL>
     */
    @Test
    public void testJaspiRegisteredServerAuthModuleAndRemoveNewAppContext_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        myServer.restartServer();
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + 
                                                          buildServerAuthQueryString(REGISTER, PROFILE_SERVLET_MSG_LAYER, TEST_APP1_CONTEXT,
                                                                                                                           DEFAULT_AUTHMODULE_CLASS),
                                                          jaspi_basicRoleUser,
                                                          jaspi_basicRolePwd, HttpServletResponse.SC_OK);

        verifyServerAuthModuleRegisteredSuccessfully(response);
        Log.info(logClass, getCurrentTestName(), "Successfully registered server auth module");
        verifyServerAuthModuleRemovedSuccessfully(response);
        Log.info(logClass, getCurrentTestName(), "Successfully removed server auth module");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
 


    /**
     * Verify the following:
     * <OL>
     * <LI> 1) Call registerServerAuthModule with a context ID that does not exist, "unknownContextID".
     * <LI> 2) verify a new server auth module successfully registered 
     * <LI> 3) remove the server auth provider with the registration ID returned from the registration and
     * <LI> 4) finally verify the server auth module was removed.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 from all servlet calls
     * <LI> After registering the server auth module with a context ID that does not exist, the servlet returns: Successfully registered server auth module.
     * <LI> ServerAuthModule Class: class com.ibm.ws.security.jaspi.test.AuthModule
     * <LI> After removing the server auth module, the servlet returns: Successfully removed server auth module.
     * </OL>
     */
    @Test
    public void testJaspiRegisteredServerAuthModuleAndRemoveUnknownAppContext_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.restartServer();
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + contextString + 
                                                          buildServerAuthQueryString(REGISTER, PROFILE_SERVLET_MSG_LAYER, UNKNOWN_CONTEXT_ID,
                                                                                                                           DEFAULT_AUTHMODULE_CLASS),
                                                          jaspi_basicRoleUser,
                                                          jaspi_basicRolePwd, HttpServletResponse.SC_OK);

        verifyServerAuthModuleRegisteredSuccessfully(response);
        Log.info(logClass, getCurrentTestName(), "Successfully registered server auth module");
        verifyServerAuthModuleRemovedSuccessfully(response);
        Log.info(logClass, getCurrentTestName(), "Successfully removed server auth module");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}







