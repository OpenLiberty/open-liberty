/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.checkpoint.fat.tests;

import java.util.Map;

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
    @Server("EjbStartCheckpointMockServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("EjbStartCheckpointMockServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("EjbStartCheckpointMockServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("EjbStartCheckpointMockServer"));

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
    public void testEjbStartCheckpointFeatures() throws Exception {
        setCheckpointPhase(CheckpointPhase.FEATURES);
        try {
            server.startServer();
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
            runTest(server, "CheckpointWeb/EjbStartCheckpointServlet", getTestMethodSimpleName());
        } finally {
            if (server.isStarted()) {
                server.stopServer();
            }
        }
    }

    @Test
    public void testEjbStartCheckpointApplications() throws Exception {
        setCheckpointPhase(CheckpointPhase.APPLICATIONS);
        try {
            server.startServer();
            runTest(server, "CheckpointWeb/EjbStartCheckpointServlet", getTestMethodSimpleName());
        } finally {
            if (server.isStarted()) {
                server.stopServer();
            }
        }
    }

    private void setCheckpointPhase(CheckpointPhase phase) throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put("-Dio.openliberty.ejb.checkpoint.phase", phase.name());
        server.setJvmOptions(jvmOptions);
    }

}
