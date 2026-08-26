/*******************************************************************************
 * Copyright (c) 2018,2026 IBM Corporation and others.
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
package com.ibm.ws.tests.anno.jandex;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.tests.anno.JandexIndexV12RepeatAction;
import com.ibm.ws.tests.anno.JandexIndexV13RepeatAction;
import com.ibm.ws.tests.anno.JandexV1RepeatAction;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import testservlet40.jar.jandex_v35.ComputeIntEncloser;
import testservlet40.jar.jandex_v3.MemberClass;

/**
 * Test that a Jandex index located in WEB-INF/classes/META-INF/jandex.idx
 * is correctly read by the Liberty annotation scanning infrastructure.
 *
 * The jandex.idx files under resources_v2_web_inf_classes, resources_v3_web_inf_classes
 * and resources_v3.5_web_inf_classes are copies of the corresponding resources_v2,
 * resources_v3 and resources_v3.5 index files, placed at
 * WEB-INF/classes/META-INF/jandex.idx inside the WAR rather than at the WAR-root
 * META-INF/jandex.idx location.
 */
@RunWith(FATRunner.class)
public class JandexWebInfClassesTest extends JandexAppTest {

    private static final Logger LOG = Logger.getLogger(JandexWebInfClassesTest.class.getName());
    private static final String WEB_INF_EAR_NAME = "TestServlet40_WebInf.ear";

    public static SharedServer SHARED_SERVER = new SharedServer("annoFat_webInfClasses_server", false);

    @ClassRule
    public static RepeatTests r = RepeatTests
            .with(new JandexV1RepeatAction())
            .andWith(new JandexIndexV12RepeatAction())
            .andWith(new JandexIndexV13RepeatAction());

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        setUpWebInfClasses(LOG, SHARED_SERVER);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        JandexAppTest.tearDown(LOG, SHARED_SERVER);
    }

    /**
     * Build and deploy the test application with the Jandex index placed in
     * WEB-INF/classes/META-INF/jandex.idx rather than the WAR-root META-INF/.
     */
    public static void setUpWebInfClasses(Logger logger, SharedServer sharedServer) throws Exception {

        logger.info("setUpWebInfClasses: Add TestServlet40 (WEB-INF/classes jandex) to the server applications folder");

        JavaArchive testServlet40Jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME)
                .addPackage(testservlet40.jar.servlets.ServletContainerInitializerImpl.class.getPackage())
                .addPackage(testservlet40.jar.util.Util_0.class.getPackage());

        // JAR uses standard resources (no jandex in the jar for this test)
        ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources");

        WebArchive testServlet40War = ShrinkWrap.create(WebArchive.class, WAR_NAME)
                .addPackage(testservlet40.war.servlets.MyServlet.class.getPackage())
                .addAsLibrary(testServlet40Jar);
        ShrinkHelper.addDirectory(testServlet40War, "test-applications/" + WAR_NAME + "/resources");

        // Add the jandex index under WEB-INF/classes/META-INF/jandex.idx
        // (instead of the usual WAR-root META-INF/jandex.idx location).
        String versionString = RepeatTestFilter.getMostRecentRepeatAction().getID();
        if (versionString.contains("switch_13")) {
            testServlet40Jar.addPackage(ComputeIntEncloser.class.getPackage());
            testServlet40Jar.addPackage(MemberClass.class.getPackage());
            ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources_v3.5");
            ShrinkHelper.addDirectory(testServlet40War, "test-applications/" + WAR_NAME + "/resources_v3.5_web_inf_classes");
        } else if (versionString.contains("switch_12")) {
            testServlet40Jar.addPackage(MemberClass.class.getPackage());
            ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources_v3");
            ShrinkHelper.addDirectory(testServlet40War, "test-applications/" + WAR_NAME + "/resources_v3_web_inf_classes");
        } else {
            ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources_v2");
            ShrinkHelper.addDirectory(testServlet40War, "test-applications/" + WAR_NAME + "/resources_v2_web_inf_classes");
        }

        EnterpriseArchive testServlet40Ear = ShrinkWrap.create(EnterpriseArchive.class, WEB_INF_EAR_NAME)
                .addAsModule(testServlet40War);
        ShrinkHelper.addDirectory(testServlet40Ear, "test-applications/" + EAR_NAME + "/resources");

        ShrinkHelper.exportToServer(sharedServer.getLibertyServer(), "apps", testServlet40Ear);

        logger.info("setUpWebInfClasses: Added TestServlet40 to the server applications folder");
        
        logger.info("setUpWebInfClasses: Validate application startup");
        sharedServer.getLibertyServer().addInstalledAppForValidation(WEB_INF_EAR_NAME.replace(".ear",""));
        logger.info("setUpWebInfClasses: The application has started");

        logger.info("setUpWebInfClasses: Launch server");
        sharedServer.startIfNotStarted();
        logger.info("setUpWebInfClasses: Launched server");
    }

    //

    @Test
    public void webInfClasses_testServletIsRunning() throws Exception {
        super.testServletIsRunning();
    }

    @Test
    public void webInfClasses_testServletIsRunning31() throws Exception {
        super.testServletIsRunning31();
    }

    @Test
    public void webInfClasses_testServletVersions() throws Exception {
        super.testServletVersions();
    }

    /**
     * Verify that the Jandex index located at WEB-INF/classes/META-INF/jandex.idx
     * is recognised and used by the Liberty annotation scanning infrastructure.
     * Jandex use is enabled via useJandex="true" on both the application and
     * the applicationManager elements (see jandexWebInfClasses_server.xml), so
     * the CWWKC009x message is expected to appear in the server log.
     */
    @Test
    public void webInfClasses_testJandexIndexIsRead() throws Exception {
        super.testJandex(JandexAppTest.DO_EXPECT_JANDEX);
    }
}
