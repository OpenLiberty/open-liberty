/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

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
public class WCContextRootPrecedence {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    @Server("servlet40_ContextRootPrecedenceServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add applications as needed.");

        ShrinkHelper.exportAppToServer(server, ShrinkHelper.buildDefaultApp("TestContextRootAppNamePrecedence.war"));
        ShrinkHelper.exportAppToServer(server, ShrinkHelper.buildDefaultApp("TestContextRootDirOrFileNamePrecedence.war"));

        WebArchive testContextRootEARAppPrecedenceWar = ShrinkWrap.create(WebArchive.class, "TestContextRootEARAppPrecedence.war");
        ShrinkHelper.addDirectory(testContextRootEARAppPrecedenceWar, "test-applications/" + "TestContextRootEARAppPrecedence.war" + "/resources");
        EnterpriseArchive testContextRootEARAppPrecedenceEar = ShrinkWrap.create(EnterpriseArchive.class, "TestContextRootEARAppPrecedence.ear");
        testContextRootEARAppPrecedenceEar.addAsModule(testContextRootEARAppPrecedenceWar);
        ShrinkHelper.addDirectory(testContextRootEARAppPrecedenceEar, "test-applications/" + "TestContextRootEARAppPrecedence.ear" + "/resources");
        ShrinkHelper.exportDropinAppToServer(server, testContextRootEARAppPrecedenceEar);

        ShrinkHelper.exportAppToServer(server, ShrinkHelper.buildDefaultApp("TestContextRootServerXmlPrecedence.war"));
        ShrinkHelper.exportAppToServer(server, ShrinkHelper.buildDefaultApp("TestContextRootWebExtPrecedence.war"));
        ShrinkHelper.exportAppToServer(server, ShrinkHelper.buildDefaultApp("TestDefaultContextPathPrecedence.war"));
        ShrinkHelper.defaultDropinApp(server, "TestDefaultContextPathWithEndSlashInvalidCase.war");
        ShrinkHelper.defaultDropinApp(server, "TestDefaultContextPathWithoutStartSlashInvalidCase.war");

        // We can't start the server like normal as there are application startup errors.
        // Instead we'll set the log name, start the server and won't validate the application.
        // Then we'll validate the applications on our own.

        // Failures occurred while waiting for apps to start: Application TestDefaultContextPathWithoutStartSlashInvalidCase failure:
        // CWWKZ0002E: An exception occurred while starting the application TestDefaultContextPathWithoutStartSlashInvalidCase.
        // The exception message was: java.lang.IllegalStateException: com.ibm.wsspi.adaptable.module.UnableToAdaptException:
        // com.ibm.ws.javaee.ddmodel.DDParser$ParseException: CWWKC2257E: Unexpected content encountered in element default-context
        // -path in the /WEB-INF/web.xml deployment descriptor on line 18. Application TestDefaultContextPathWithEndSlashInvalidCase failure:
        // CWWKZ0002E: An exception occurred while starting the application TestDefaultContextPathWithEndSlashInvalidCase.
        // The exception message was: java.lang.IllegalStateException: com.ibm.wsspi.adaptable.module.UnableToAdaptException:
        // com.ibm.ws.javaee.ddmodel.DDParser$ParseException: CWWKC2257E: Unexpected content encountered in element default-context-path
        // in the /WEB-INF/web.xml deployment descriptor on line 18.
        server.setConsoleLogName(WCContextRootPrecedence.class.getSimpleName() + ".log");
        server.startServerAndValidate(true, true, false);

        // Validate the necessary applications have started.
        server.validateAppLoaded("AppNameContextRoot"); // TestContextRootAppNamePrecedence.war defined name in server.xml
        server.validateAppLoaded("TestContextRootDirOrFileNamePrecedence");
        server.validateAppLoaded("TestContextRootEARAppPrecedence");
        server.validateAppLoaded("TestServerXmlContextRoot"); // TestContextRootServerXmlPrecedence.war defined name in server.xml
        server.validateAppLoaded("TestWebExtContextRoot"); // TestContextRootWebExtPrecedence.war defined name in server.xml
        server.validateAppLoaded("TestDefaultContextPathPrecedence");

        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        server.deleteAllDropinConfigurations();

        // Stop the server
        // E CWWKZ0002E: An exception occurred while starting the application TestDefaultContextPathWithEndSlashInvalidCase.
        // The exception message was: java.lang.IllegalStateException: com.ibm.wsspi.adaptable.module.UnableToAdaptException:
        // com.ibm.ws.javaee.ddmodel.DDParser$ParseException: CWWKC2257E: Unexpected content encountered in element default-context-path
        // in the /WEB-INF/web.xml deployment descriptor on line 18. <br>[8/8/21, 21:38:31:657 EDT] 0000006d com.ibm.ws.app.manager.AppMessageHelper
        // E CWWKZ0002E: An exception occurred while starting the application TestDefaultContextPathWithoutStartSlashInvalidCase. The exception message
        // was: java.lang.IllegalStateException: com.ibm.wsspi.adaptable.module.UnableToAdaptException: com.ibm.ws.javaee.ddmodel.DDParser$ParseException:
        // CWWKC2257E: Unexpected content encountered in element default-context-path in the /WEB-INF/web.xml deployment descriptor on line 18.
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKZ0002E:.*");
        }
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
        HttpUtils.findStringInReadyUrl(server, "/ServerContextRoot/", "Simple HTML page - TestContextRootServerXmlPrecedence");
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
        HttpUtils.findStringInReadyUrl(server, "/ApplicationContextRoot/", "Simple HTML page - TestContextRootEARAppPrecedence");
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
        HttpUtils.findStringInReadyUrl(server, "/WebExtContextRoot/", "Simple HTML page - TestContextRootWebExtPrecedence");
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
        HttpUtils.findStringInReadyUrl(server, "/AppNameContextRoot/", "Simple HTML page - TestContextRootAppNamePrecedence");
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
        HttpUtils.findStringInReadyUrl(server, "/WebDefaultContextPath/", "Simple HTML page - TestDefaultContextPathPrecedence");
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
        HttpUtils.findStringInReadyUrl(server, "/TestContextRootDirOrFileNamePrecedence/", "Simple HTML page - TestContextRootDirOrFileNamePrecedence");
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
        server.findStringsInLogs(
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
        server.findStringsInLogs(
                                 "CWWKZ0002E:.*TestDefaultContextPathWithEndSlashInvalidCase.*CWWKC2257E:.*default-context-path");
    }
}
