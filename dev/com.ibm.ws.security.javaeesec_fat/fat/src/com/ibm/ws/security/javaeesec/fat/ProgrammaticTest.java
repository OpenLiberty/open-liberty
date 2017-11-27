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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 */
@MinimumJavaLevel(javaLevel = 1.8)
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ProgrammaticTest extends JavaEESecTestBase {

    protected static Class<?> logClass = ProgrammaticTest.class;
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected String queryString = "/JavaEESec/UnprotectedServlet";
    protected static String urlBase;
    protected static String urlHttps;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public ProgrammaticTest() {
        super(myServer, logClass);
    }

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESec.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.identitystores");
        myServer.setServerConfigurationFile("commonServer.xml");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("JavaEESec");

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        myServer.stopServer();
        myServer.setServerConfigurationFile("server.xml");
    }

    @SuppressWarnings("restriction")
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
    public void testRequestAuthenticate() throws Exception {
        String response = executeGetRequestBasicAuthCreds(httpclient, urlHttps + queryString + "?method=authenticate", Constants.javaeesec_basicRoleUser,
                                                          Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
//        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleUser);
    }

}
