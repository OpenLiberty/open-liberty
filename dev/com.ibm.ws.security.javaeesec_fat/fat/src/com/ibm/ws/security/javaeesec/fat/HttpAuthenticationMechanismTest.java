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

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.ServerHelper;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.security.javaeesec.fat_singleIS.HttpAuthenticationMechanismSingleISTest;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test Description:
 */
@MinimumJavaLevel(javaLevel = 1.7, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HttpAuthenticationMechanismTest extends HttpAuthenticationMechanismSingleISTest {

    private static EmbeddedApacheDS ldapServer = null;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ServerHelper.setupldapServer();
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.basic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecAnnotatedBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.annotatedbasic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuth.war", true, JAR_NAME, false, "web.jar.base", "web.war.formlogin");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuthRedirect.war", true, JAR_NAME, false, "web.jar.base", "web.war.redirectformlogin");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/javaeesecinternals-1.0.mf");

        myServer.startServer(true);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        ServerHelper.commonStopServer(myServer, Constants.HAS_LDAP_SERVER);
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

}
