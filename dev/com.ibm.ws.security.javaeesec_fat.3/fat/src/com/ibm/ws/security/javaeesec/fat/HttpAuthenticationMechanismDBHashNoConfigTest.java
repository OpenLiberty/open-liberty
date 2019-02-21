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

import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description: Test that looking up a user fails because the custom hash class is not discovered
 */
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HttpAuthenticationMechanismDBHashNoConfigTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.db.hash.bean.fat");
    protected static Class<?> logClass = HttpAuthenticationMechanismDBHashNoConfigTest.class;
    protected String queryString = "/DatabaseAnnotatedCustomHashServlet/JavaEESecAnnotatedBasic";
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected static String TEMP_DIR = "test_temp";

    protected DefaultHttpClient httpclient;

    public HttpAuthenticationMechanismDBHashNoConfigTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        try {
            WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.basic");
            WCApplicationHelper.addWarToServerApps(myServer, "DatabaseAnnotatedCustomHashBean.war", true, JAR_NAME, false, "web.jar.base", "web.war.hash.db.bean");
            WCApplicationHelper.addWarToServerApps(myServer, "dbfatCustomHash.war", true, JAR_NAME, false, "web.jar.base", "web.war.db");

            // add custom hash jar
            WCApplicationHelper.createJarAllPackages(myServer, "../../../lib/LibertyFATTestFiles/", "CustomPasswordHashBean.jar", false,
                                                     "com.ibm.ws.security.pwdhash.bean.test");
            myServer.copyFileToLibertyServerRoot("CustomPasswordHashBean.jar");

            myServer.startServer(true);
            assertNotNull("Application DBServlet does not appear to have started.",
                          myServer.waitForStringInLog("CWWKZ0001I: Application DBServlet started"));
            urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer("CWWKS1922E");
    }

    @Before
    public void setupConnection() {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        httpclient = new DefaultHttpClient(httpParams);
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
     * Should fail to find users because we can't load the custom hash class
     *
     */
    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("com.ibm.ws.security.javaeesec.identitystore.IdentityStoreRuntimeException")
    public void testUserWithCustomHashNoConfig() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // check based on user
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER1,
                                                          Constants.DB_CUSTOM_PWD1,
                                                          HttpServletResponse.SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
