/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.checkpoint.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
public class EjbStartCheckpointTest extends FATServletClient {
    private static final Logger logger = Logger.getLogger(EjbStartCheckpointTest.class.getName());

    private static final List<String> CHECKPOINT_INACTIVE = Collections.emptyList();
    private static final List<String> CHECKPOINT_DEPLOYMENT = Arrays.asList("--internal-checkpoint-at=deployment");
    private static final List<String> CHECKPOINT_APPLICATIONS = Arrays.asList("--internal-checkpoint-at=applications");

    private static final String MSG_INIT_ON_START = "will be initialized at Application start";
    private static final String MSG_DEFERRED_INIT = "will be deferred until it is first used";

    private static final String[] DEFAULT_INIT_ON_START = new String[] { "SGCheckpointBeanD", "SLCheckpointBeanC", "SLCheckpointBeanD",
                                                                         "SGCheckpointBeanJ", "SLCheckpointBeanI", "SLCheckpointBeanJ",
                                                                         "SGCheckpointBeanP", "SLCheckpointBeanO", "SLCheckpointBeanP",
                                                                         "SGCheckpointBeanV", "SLCheckpointBeanU", "SLCheckpointBeanV" };

    private static final String[] DEFAULT_DEFERRED_INIT = new String[] { "SGCheckpointBeanA", "SGCheckpointBeanB", "SGCheckpointBeanC", "SGCheckpointBeanE",
                                                                         "SLCheckpointBeanA", "SLCheckpointBeanB", "SLCheckpointBeanE", "SLCheckpointBeanF",
                                                                         "SGCheckpointBeanG", "SGCheckpointBeanH", "SGCheckpointBeanI", "SGCheckpointBeanK",
                                                                         "SLCheckpointBeanG", "SLCheckpointBeanH", "SLCheckpointBeanK", "SLCheckpointBeanL",
                                                                         "SGCheckpointBeanM", "SGCheckpointBeanN", "SGCheckpointBeanO", "SGCheckpointBeanQ",
                                                                         "SLCheckpointBeanM", "SLCheckpointBeanN", "SLCheckpointBeanQ", "SLCheckpointBeanR",
                                                                         "SGCheckpointBeanS", "SGCheckpointBeanT", "SGCheckpointBeanU", "SGCheckpointBeanW",
                                                                         "SLCheckpointBeanS", "SLCheckpointBeanT", "SLCheckpointBeanW", "SLCheckpointBeanX" };

    private static final String[] CHECKPOINT_INIT_ON_START = new String[] { "SGCheckpointBeanA", "SGCheckpointBeanB", "SGCheckpointBeanC", "SGCheckpointBeanD",
                                                                            "SLCheckpointBeanA", "SLCheckpointBeanB", "SLCheckpointBeanC", "SLCheckpointBeanD",
                                                                            "SGCheckpointBeanG", "SGCheckpointBeanH", "SGCheckpointBeanI", "SGCheckpointBeanJ",
                                                                            "SLCheckpointBeanG", "SLCheckpointBeanH", "SLCheckpointBeanI", "SLCheckpointBeanJ",
                                                                            "SGCheckpointBeanM", "SGCheckpointBeanN", "SGCheckpointBeanO", "SGCheckpointBeanP",
                                                                            "SLCheckpointBeanM", "SLCheckpointBeanN", "SLCheckpointBeanO", "SLCheckpointBeanP",
                                                                            "SGCheckpointBeanS", "SGCheckpointBeanT", "SGCheckpointBeanU", "SGCheckpointBeanV",
                                                                            "SLCheckpointBeanS", "SLCheckpointBeanT", "SLCheckpointBeanU", "SLCheckpointBeanV" };

    private static final String[] FORCED_DEFERRED_INIT = new String[] { "SGCheckpointBeanE", "SLCheckpointBeanE", "SLCheckpointBeanF",
                                                                        "SGCheckpointBeanK", "SLCheckpointBeanK", "SLCheckpointBeanL",
                                                                        "SGCheckpointBeanQ", "SLCheckpointBeanQ", "SLCheckpointBeanR",
                                                                        "SGCheckpointBeanW", "SLCheckpointBeanW", "SLCheckpointBeanX" };

    @Server("EjbStartCheckpointMockServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("EjbStartCheckpointMockServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("EjbStartCheckpointMockServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("EjbStartCheckpointMockServer")).andWith(FeatureReplacementAction.EE10_FEATURES().forServers("EjbStartCheckpointMockServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the Ears & Wars

        //#################### CheckpointApp.ear
        JavaArchive CheckpointEJB = ShrinkHelper.buildJavaArchive("CheckpointEJB.jar", "io.openliberty.ejbcontainer.fat.checkpoint.ejb.");
        JavaArchive CheckpointOtherEJB = ShrinkHelper.buildJavaArchive("CheckpointOtherEJB.jar", "io.openliberty.ejbcontainer.fat.checkpoint.other.ejb.");
        WebArchive CheckpointWeb = ShrinkHelper.buildDefaultApp("CheckpointWeb.war", "io.openliberty.ejbcontainer.fat.checkpoint.web.");
        WebArchive CheckpointOtherWeb = ShrinkHelper.buildDefaultApp("CheckpointOtherWeb.war", "io.openliberty.ejbcontainer.fat.checkpoint.other.web.");
        EnterpriseArchive CheckpointApp = ShrinkWrap.create(EnterpriseArchive.class, "CheckpointApp.ear");
        CheckpointApp.addAsModule(CheckpointEJB).addAsModule(CheckpointOtherEJB);
        CheckpointApp.addAsModule(CheckpointWeb).addAsModule(CheckpointOtherWeb);
        CheckpointApp = (EnterpriseArchive) ShrinkHelper.addDirectory(CheckpointApp, "test-applications/CheckpointApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, CheckpointApp, DeployOptions.SERVER_ONLY);

        //server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        //server.stopServer();
    }

    @Test
    public void testEjbStartCheckpointInactive() throws Exception {
        setCheckpointPhase(CheckpointPhase.INACTIVE);
        try {
            server.startServer();
            assert_12_BeanMetaDataInitilzedAtStart();
            runTest(server, "CheckpointWeb/EjbStartCheckpointServlet", getTestMethodSimpleName());
        } finally {
            if (server.isStarted()) {
                server.stopServer();
            }
        }
    }

    @Test
    public void testEjbStartCheckpointDeployment() throws Exception {
        setCheckpointPhase(CheckpointPhase.DEPLOYMENT);
        try {
            server.startServer();
            assert_32_BeanMetaDataInitilzedAtStart();
            runTest(server, "CheckpointWeb/EjbStartCheckpointServlet", getTestMethodSimpleName());
        } finally {
            if (server.isStarted()) {
                server.stopServer();
            }
        }
    }

    // Temporarily disable while investigating bean startup behavior
    //@Test
    public void testEjbStartCheckpointApplications() throws Exception {
        setCheckpointPhase(CheckpointPhase.APPLICATIONS);
        try {
            server.startServer();
            assert_32_BeanMetaDataInitilzedAtStart();
            runTest(server, "CheckpointWeb/EjbStartCheckpointServlet", getTestMethodSimpleName());
        } finally {
            if (server.isStarted()) {
                server.stopServer();
            }
        }
    }

    private void setCheckpointPhase(CheckpointPhase phase) throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        switch (phase) {
            case DEPLOYMENT:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(CHECKPOINT_DEPLOYMENT);
                break;
            case APPLICATIONS:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(CHECKPOINT_APPLICATIONS);
                break;
            default:
                jvmOptions.remove("-Dio.openliberty.checkpoint.stub.criu");
                server.setExtraArgs(CHECKPOINT_INACTIVE);
        }
        server.setJvmOptions(jvmOptions);
    }

    private void assert_12_BeanMetaDataInitilzedAtStart() throws Exception {
        logger.info("Looking for \"" + MSG_INIT_ON_START + "\"");
        List<String> initialized = server.findStringsInTrace(MSG_INIT_ON_START);
        for (String line : initialized) {
            logger.info("    found : " + line);
        }
        assertEquals("Unexpected number of beans initialized at application start", 12, initialized.size());
        for (String beanName : DEFAULT_INIT_ON_START) {
            assertTrue(beanName + " not initialized on start", initialized.removeIf(line -> line.contains(beanName)));
        }

        logger.info("Looking for \"" + MSG_DEFERRED_INIT + "\"");
        List<String> deferred = server.findStringsInTrace(MSG_DEFERRED_INIT);
        for (String line : deferred) {
            logger.info("    found : " + line);
        }
        assertEquals("Unexpected number of beans deferred until first use", 32, deferred.size());
        for (String beanName : DEFAULT_DEFERRED_INIT) {
            assertTrue(beanName + " not deferred initialize", deferred.removeIf(line -> line.contains(beanName)));
        }
    }

    private void assert_32_BeanMetaDataInitilzedAtStart() throws Exception {
        logger.info("Looking for \"" + MSG_INIT_ON_START + "\"");
        List<String> initialized = server.findStringsInTrace(MSG_INIT_ON_START);
        for (String line : initialized) {
            logger.info("    found : " + line);
        }
        assertEquals("Unexpected number of beans initialized at application start", 32, initialized.size());
        for (String beanName : CHECKPOINT_INIT_ON_START) {
            assertTrue(beanName + " not initialized on start", initialized.removeIf(line -> line.contains(beanName)));
        }

        logger.info("Looking for \"" + MSG_DEFERRED_INIT + "\"");
        List<String> deferred = server.findStringsInTrace(MSG_DEFERRED_INIT);
        for (String line : deferred) {
            logger.info("    found : " + line);
        }
        assertEquals("Unexpected number of beans deferred until first use", 12, deferred.size());
        for (String beanName : FORCED_DEFERRED_INIT) {
            assertTrue(beanName + " not deferred initialize", deferred.removeIf(line -> line.contains(beanName)));
        }
    }

}
