/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.LocalLdapServer;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class NoIdentityStoreTest extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = NoIdentityStoreTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_NAME = "JavaEESecMultipleIS";
    protected static String WAR_NAME = APP_NAME + ".war";
    protected static String XML_NAME = "multipleIS.xml";
    protected String queryString = "/" + APP_NAME + "/NoIDStoreServlet";

    protected DefaultHttpClient httpclient;

    public NoIdentityStoreTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        WCApplicationHelper.addWarToServerApps(myServer, WAR_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.noidstore", "web.war.identitystorehandler",
                                               "web.war.mechanisms.applbasic");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_NAME);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
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
     * <LI> Authentication is not done due to no identitystore.
     * <LI> CustomIdentityStore and CustomIdentityStoreHandler are used in order to prevent falling back to the user registry.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 since role is assigned to EVERYONE.
     * <LI> Veirfy the customidentitystorehandler is used.
     * <LI> Veirfy that CWWKS1930W message is logged.
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testNoIDStoreWithCustomIDSandIDSHandler_NoOp() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        String response = executeGetRequestBasicAuthCredsPreemptive(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                                    LocalLdapServer.PASSWORD,
                                                                    HttpServletResponse.SC_OK);
        verifyMessageReceivedInMessageLog("CustomIdentityStoreHandler is being used.");
        verifyMessageReceivedInMessageLog("Number of identityStore : 0");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
