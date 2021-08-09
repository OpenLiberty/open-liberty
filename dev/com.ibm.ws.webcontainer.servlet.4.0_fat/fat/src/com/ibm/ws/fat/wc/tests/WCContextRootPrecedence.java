/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * WCContextRootPrecedence class tests the precedences of the context root. It
 * also tests the new element default-context-path to the web.xml.
 *
 * The precedence of the context-root of a deployed application is as follows: -
 * context-root in the server.xml file - application.xml, if an EAR application
 * - ibm-web-ext.xml, if a web application - name of the application in the
 * server.xml file, if a web application. If the name attribute is the same as
 * the module name, and default-context-path is set, then use the value of
 * default-context-path instead. - default-context-path element in the web.xml,
 * if a web application - Directory name or the file name relative to the
 * drop-ins directory of Liberty
 */
@RunWith(FATRunner.class)
public class WCContextRootPrecedence extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_ContextRootPrecedenceServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        // Apps defined in server.xml are not present during the first server start and this results in unwanted
        // CWKZ0014W warning messages.
        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWWC0400E:.*");
        expectedErrors.add("CWWKC2257E:.*");
        expectedErrors.add("CWWKZ0014W:.*");
        SHARED_SERVER.getLibertyServer().addIgnoredErrors(expectedErrors);

        LOG.info("Setup : add applications as needed.");

        WCApplicationHelper.addWarToServerApps(SHARED_SERVER.getLibertyServer(), "TestContextRootAppNamePrecedence.war",
                                               true);

        WCApplicationHelper.addWarToServerApps(SHARED_SERVER.getLibertyServer(),
                                               "TestContextRootDirOrFileNamePrecedence.war", true);

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(),
                                                  "TestContextRootEARAppPrecedence.ear", true, "TestContextRootEARAppPrecedence.war", true, null, false);

        WCApplicationHelper.addWarToServerApps(SHARED_SERVER.getLibertyServer(),
                                               "TestContextRootServerXmlPrecedence.war", true);

        WCApplicationHelper.addWarToServerApps(SHARED_SERVER.getLibertyServer(), "TestContextRootWebExtPrecedence.war",
                                               true);
        WCApplicationHelper.addWarToServerApps(SHARED_SERVER.getLibertyServer(), "TestDefaultContextPathPrecedence.war",
                                               true);
        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(),
                                                  "TestDefaultContextPathWithEndSlashInvalidCase.war", true);
        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(),
                                                  "TestDefaultContextPathWithoutStartSlashInvalidCase.war", true);

        WCApplicationHelper.waitForAppStart("TestContextRootDirOrFileNamePrecedence", WCContextRootPrecedence.class.getName(), SHARED_SERVER.getLibertyServer());
        WCApplicationHelper.waitForAppStart("AppNameContextRoot", WCContextRootPrecedence.class.getName(), SHARED_SERVER.getLibertyServer());
        WCApplicationHelper.waitForAppStart("TestServerXmlContextRoot", WCContextRootPrecedence.class.getName(), SHARED_SERVER.getLibertyServer());
        WCApplicationHelper.waitForAppStart("TestWebExtContextRoot", WCContextRootPrecedence.class.getName(), SHARED_SERVER.getLibertyServer());
        WCApplicationHelper.waitForAppStart("TestDefaultContextPathPrecedence", WCContextRootPrecedence.class.getName(), SHARED_SERVER.getLibertyServer());
        WCApplicationHelper.waitForAppStart("TestContextRootEARAppPrecedence", WCContextRootPrecedence.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        SHARED_SERVER.getLibertyServer().deleteAllDropinConfigurations();

        SHARED_SERVER.getLibertyServer().stopServer();
    }

    /**
     * Test the context-root attribute of application element from the sever.xml
     * The server.xml contains the context-root attribute with the value set to
     * "ServerContextRoot".
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    public void testContextRootServerXmlPrecedence() throws Exception {
        this.verifyResponse("/ServerContextRoot/", "Simple HTML page - TestContextRootServerXmlPrecedence");
    }

    /**
     * Test the context-root element from the application.xml module. The
     * application.xml contains a module with context-root element set to
     * "ApplicationContextRoot".
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    public void testContextRootEARAppPrecedence() throws Exception {
        this.verifyResponse("/ApplicationContextRoot/", "Simple HTML page - TestContextRootEARAppPrecedence");
    }

    /**
     * Test the context-root element from ibm-web-ext.xml inside the WAR
     * application The ibm-web-ext.xml contains the context-root element with
     * the uri set to "WebExtContextRoot".
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    public void testContextRootWebExtPrecedence() throws Exception {
        this.verifyResponse("/WebExtContextRoot/", "Simple HTML page - TestContextRootWebExtPrecedence");
    }

    /**
     * Test the name attribute of application element from server.xml The
     * server.xml contains the name attribute with value set to
     * "AppNameContextRoot".
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    public void testContextRootAppNamePrecedence() throws Exception {
        this.verifyResponse("/AppNameContextRoot/", "Simple HTML page - TestContextRootAppNamePrecedence");
    }

    /**
     * Test the default-context-path element from the web.xml The web.xml
     * contains the default-context-path element with the value "HelloWorld".
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    public void testDefaultContextPathElementPrecedence() throws Exception {
        this.verifyResponse("/WebDefaultContextPath/", "Simple HTML page - TestDefaultContextPathPrecedence");
    }

    /**
     * Test the context root based on directory name or the file name relative
     * to the drop-ins directory. The directory name or file name should have a
     * value of "TestContextRootDirOrFileNamePrecedence".
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    public void testContextRootDirOrFileNamePrecedence() throws Exception {
        this.verifyResponse("/TestContextRootDirOrFileNamePrecedence/",
                            "Simple HTML page - TestContextRootDirOrFileNamePrecedence");
    }

    /**
     * Test default-context-path element invalid cases. In this case, the value
     * of the default-context-path element does not start with a '/' character.
     * The application should not start and an error should be thrown.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    @Mode(TestMode.FULL)
    public void testDefaultContextPathWithoutStartSlashInvalidCase() throws Exception {
        SHARED_SERVER.getLibertyServer().findStringsInLogs(
                                                           "CWWKZ0002E:.*TestDefaultContextPathWithoutStartSlashInvalidCase.*CWWKC2257E:.*default-context-path");
    }

    /**
     * Test default-context-path element invalid cases. In this case, the value
     * of the default-context-path element ends with a '/' character. The
     * application should not start and an error should be thrown.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.wsspi.adaptable.module.UnableToAdaptException" })
    @Mode(TestMode.FULL)
    public void testDefaultContextPathWithEndSlashInvalidCase() throws Exception {
        SHARED_SERVER.getLibertyServer().findStringsInLogs(
                                                           "CWWKZ0002E:.*TestDefaultContextPathWithEndSlashInvalidCase.*CWWKC2257E:.*default-context-path");
    }

}
