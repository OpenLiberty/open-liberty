/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.tests.anno.JandexIndexV12RepeatAction;
import com.ibm.ws.tests.anno.JandexIndexV13RepeatAction;
import com.ibm.ws.tests.anno.JandexV1RepeatAction;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import testservlet40.jar.jandex_v35.ComputeIntEncloser;
import testservlet40.jar.jandex_v3.MemberClass;

/**
 * Test the behavior when <code>readWebInfJandex="true"</code> is configured but
 * the index at WEB-INF/classes/META-INF/jandex.idx is absent, empty, or corrupt.
 *
 * In each case the application must still start and function: the annotation
 * scan must fall back to scanning the WEB-INF/classes classes directly.  The
 * only Jandex index which is read is the (valid) index of the jar under WEB-INF/lib,
 * which is a single module location.
 *
 * Each test deploys a variation of the application and starts the server, since
 * the application content is fixed at server start.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JandexWebInfClassesBadIndexTest extends JandexAppTest {

    private static final Logger LOG = Logger.getLogger(JandexWebInfClassesBadIndexTest.class.getName());

    // Distinct archive names, so that these archives are never confused with the
    // archives of the other Jandex tests, which share the same naming scheme.
    private static final String BAD_INDEX_JAR_NAME = "TestServlet40_corrupt.jar";
    private static final String BAD_INDEX_WAR_NAME = "TestServlet40_corrupt.war";
    private static final String BAD_INDEX_EAR_NAME = "TestServlet40_WebInf_corrupt.ear";

    /** Messages which are expected when an index cannot be read. */
    private static final String[] IGNORED_ERRORS = {
        "CWWKC00\\d\\dW", "CWWKC00\\d\\dE", "JANDEX_INDEX_READ_EXCEPTION"
    };

    // Server configuration: readWebInfJandex="true", deploying BAD_INDEX_EAR_NAME.
    public static SharedServer SHARED_SERVER = new SharedServer("annoFat_webInfClasses_corrupt_server", false);

    @ClassRule
    public static RepeatTests r = RepeatTests
            .with(new JandexV1RepeatAction())
            .andWith(new JandexIndexV12RepeatAction())
            .andWith(new JandexIndexV13RepeatAction());

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @After
    public void stopServerAfterTest() throws Exception {
        stopServerIfStarted();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        stopServerIfStarted();
    }

    private static void stopServerIfStarted() throws Exception {
        if ( SHARED_SERVER.getLibertyServer().isStarted() ) {
            SHARED_SERVER.getLibertyServer().stopServer(IGNORED_ERRORS);
        }
    }

    /**
     * Build the test application, optionally with an asset placed at
     * WEB-INF/classes/META-INF/jandex.idx, and start the server.
     *
     * @param webInfIndex The content of WEB-INF/classes/META-INF/jandex.idx.
     *     Null if the file is to be absent.
     */
    private void deployAndStart(Asset webInfIndex) throws Exception {
        JavaArchive testServlet40Jar = ShrinkWrap.create(JavaArchive.class, BAD_INDEX_JAR_NAME)
                .addPackage(testservlet40.jar.servlets.ServletContainerInitializerImpl.class.getPackage())
                .addPackage(testservlet40.jar.util.Util_0.class.getPackage());
        // The sources of the jar, war, and ear are in the directories of the unrenamed archives.
        ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources");

        WebArchive testServlet40War = ShrinkWrap.create(WebArchive.class, BAD_INDEX_WAR_NAME)
                .addPackage(testservlet40.war.servlets.MyServlet.class.getPackage())
                .addAsLibrary(testServlet40Jar);
        ShrinkHelper.addDirectory(testServlet40War, "test-applications/" + WAR_NAME + "/resources");

        // The jar keeps its valid index; only the WEB-INF/classes index varies.
        String versionString = RepeatTestFilter.getMostRecentRepeatAction().getID();
        if ( versionString.contains("switch_13") ) {
            testServlet40Jar.addPackage(ComputeIntEncloser.class.getPackage());
            testServlet40Jar.addPackage(MemberClass.class.getPackage());
            ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources_v3.5");
        } else if ( versionString.contains("switch_12") ) {
            testServlet40Jar.addPackage(MemberClass.class.getPackage());
            ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources_v3");
        } else {
            ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + JAR_NAME + "/resources_v2");
        }

        if ( webInfIndex != null ) {
            // Placed under WEB-INF/classes of the war.
            testServlet40War.addAsResource(webInfIndex, "META-INF/jandex.idx");
        }

        // The application.xml of the unrenamed ear names the unrenamed war in its web-uri.
        String applicationXml = new String(
            Files.readAllBytes(Paths.get("test-applications/" + EAR_NAME + "/resources/META-INF/application.xml")),
            StandardCharsets.UTF_8).replace(WAR_NAME, BAD_INDEX_WAR_NAME);

        EnterpriseArchive testServlet40Ear = ShrinkWrap.create(EnterpriseArchive.class, BAD_INDEX_EAR_NAME)
                .addAsModule(testServlet40War)
                .addAsManifestResource(new StringAsset(applicationXml), "application.xml");

        // Overwrite: the archive differs between tests, but the name does not.
        ShrinkHelper.exportToServer(SHARED_SERVER.getLibertyServer(), "apps", testServlet40Ear,
            ShrinkHelper.DeployOptions.OVERWRITE);

        SHARED_SERVER.getLibertyServer().addInstalledAppForValidation(BAD_INDEX_EAR_NAME.replace(".ear", ""));

        LOG.info("deployAndStart: Launch server");
        LibertyServer server = SHARED_SERVER.getLibertyServer();
        if ( server.isStarted() ) {
            // Restart with --clean: stop, tolerating the expected index warnings, then start clean.
            server.stopServer(IGNORED_ERRORS);
        }
        server.startServer(true); // cleanStart
        LOG.info("deployAndStart: Launched server");
    }

    /**
     * The application must run, and only the jar index may be read.
     */
    private void verifyFallback() throws Exception {
        super.testServletIsRunning();
        super.testJandex(JandexAppTest.JandexExpectation.DO_EXPECT_JANDEX_IN_ONE_LOCATION);
    }

    /**
     * readWebInfJandex="true", but WEB-INF/classes/META-INF/jandex.idx is absent.
     */
    @Test
    public void applicationReadWebInfJandexTrue_webInfIndexAbsent_testFallbackToScan() throws Exception {
        deployAndStart(null);
        verifyFallback();
    }

    /**
     * readWebInfJandex="true", but WEB-INF/classes/META-INF/jandex.idx is empty.
     */
    @Test
    public void applicationReadWebInfJandexTrue_webInfIndexEmpty_testFallbackToScan() throws Exception {
        deployAndStart(EmptyAsset.INSTANCE);
        verifyFallback();
    }

    /**
     * readWebInfJandex="true", but WEB-INF/classes/META-INF/jandex.idx is not a valid index.
     */
    @Test
    public void applicationReadWebInfJandexTrue_webInfIndexCorrupt_testFallbackToScan() throws Exception {
        deployAndStart(new StringAsset("This is not a valid Jandex index."));
        verifyFallback();
    }
}
