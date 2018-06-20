/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

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

import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ScopedTest extends JavaEESecTestBase {

    protected static Class<?> logClass = ScopedTest.class;
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static String urlHttps;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public ScopedTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        WCApplicationHelper.addWarToServerApps(myServer, "ApplicationScopedMechanismWithRequestScopedStore.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.mechanisms", "web.war.mechanisms.scoped.application",
                                               "web.war.identitystores", "web.war.identitystores.scoped.request");

        WCApplicationHelper.addWarToServerApps(myServer, "ApplicationScopedMechanismWithSessionScopedStore.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.mechanisms", "web.war.mechanisms.scoped.application",
                                               "web.war.identitystores", "web.war.identitystores.scoped.session");

        WCApplicationHelper.addWarToServerApps(myServer, "RequestScopedMechanismWithApplicationScopedStore.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.mechanisms", "web.war.mechanisms.scoped.request",
                                               "web.war.identitystores", "web.war.identitystores.scoped.application");

        myServer.setServerConfigurationFile("scoped.xml");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("ApplicationScopedMechanismWithRequestScopedStore");
        myServer.addInstalledAppForValidation("ApplicationScopedMechanismWithSessionScopedStore");
        myServer.addInstalledAppForValidation("RequestScopedMechanismWithApplicationScopedStore");

        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        myServer.stopServer();
    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
        SSLHelper.establishSSLContext(httpclient, 0, myServer, null, null, null, null, null);
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    @Test
    public void testApplicationScopedMechanismWithRequestScopedStore() throws Exception {
        String response = executeGetRequestBasicAuthCreds(httpclient, urlHttps + "/ApplicationScopedMechanismWithRequestScopedStore/CommonServlet",
                                                          Constants.javaeesec_basicRoleUser_requestscoped, Constants.javaeesec_basicRolePwd, HttpServletResponse.SC_OK);
        verifyUser(response, Constants.javaeesec_basicRoleUser_requestscoped);
    }

    @Test
    public void testApplicationScopedMechanismWithSessionScopedStoreMultipleRequests() throws Exception {
        String response = executeGetRequestBasicAuthCreds(httpclient, urlHttps + "/ApplicationScopedMechanismWithSessionScopedStore/CommonServlet",
                                                          Constants.javaeesec_basicRoleUser_sessionscoped, Constants.javaeesec_basicRolePwd, HttpServletResponse.SC_OK);
        verifyUser(response, Constants.javaeesec_basicRoleUser_sessionscoped);
        response = executeGetRequestBasicAuthCreds(httpclient, urlHttps + "/ApplicationScopedMechanismWithSessionScopedStore/CommonServlet",
                                                   Constants.javaeesec_basicRoleUser_sessionscoped, Constants.javaeesec_basicRolePwd, HttpServletResponse.SC_OK);
        verifyUser(response, Constants.javaeesec_basicRoleUser_sessionscoped);
    }

    @Test
    public void testRequestScopedMechanismWithApplicationScopedStore() throws Exception {
        String response = executeGetRequestBasicAuthCreds(httpclient, urlHttps + "/RequestScopedMechanismWithApplicationScopedStore/CommonServlet",
                                                          Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd, HttpServletResponse.SC_OK);
        verifyUser(response, Constants.javaeesec_basicRoleUser);
    }

    // The principal name and remote user name are the same in these tests.
    private void verifyUser(String response, String name) {
        verifyUserResponse(response, Constants.getUserPrincipalFound + name, Constants.getRemoteUserFound + name);
    }

}
