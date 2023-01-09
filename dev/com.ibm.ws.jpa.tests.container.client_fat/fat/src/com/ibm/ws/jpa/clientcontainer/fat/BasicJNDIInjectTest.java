/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.clientcontainer.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class BasicJNDIInjectTest extends FATServletClient {

    private static final Class<?> c = BasicJNDIInjectTest.class;

    private final static String JPA_ACLI_EYECATCHER = "JPAACLI:";
    private final static String JPA_ACLI_PASS_EYECATCHER = JPA_ACLI_EYECATCHER + "PASS:";
    private final static String JPA_ACLI_FAIL_EYECATCHER = JPA_ACLI_EYECATCHER + "FAIL:";

    @Rule
    public TestName name = new TestName();

    @Server("BasicJPAServer")
    public static LibertyServer server1;

    public static LibertyClient libClient = LibertyClientFactory.getLibertyClient("BasicJPAJNDIClient");

    @BeforeClass
    public static void setUp() throws Exception {
        libClient.addIgnoreErrors("CWWJP9991W");

        String thisMethod = "setUp";
        Log.info(c, thisMethod, "setup complete ...");

        JavaArchive cli = ShrinkWrap.create(JavaArchive.class, "BasicJPAJNDIInjectClient.jar")
                        .addPackage("com.ibm.ws.jpa.clientcontainer.fat.basic.jndi")//
                        .addPackage("com.ibm.ws.jpa.clientcontainer.fat.basic.jndi.entities");
        ShrinkHelper.addDirectory(cli, "test-applications/BasicJPAJNDIInjectClient.jar/resources");

        EnterpriseArchive appCliEar = ShrinkWrap.create(EnterpriseArchive.class, "BasicJPAJNDIInj.ear").addAsModule(cli);
        ShrinkHelper.addDirectory(appCliEar, "test-applications/BasicJPAJNDIInj.ear");
        ShrinkHelper.exportAppToClient(libClient, appCliEar);
    }

    /**
     * Test description:
     * - start the client and check log results for validation of configuration.
     *
     * Expected results:
     * - We should see validation message that client application started.
     * - We should see 1 JPAACLI:PASS message
     * - We should see no JPAACLI:FAIL messages.
     */
    @Test
    @MinimumJavaLevel(javaLevel = 7)
    public void testBasicJPAJNDIInjection() {
        // the startParms is just here for future use, and to prevent unused warnings for imports
        List<String> startParms = new ArrayList<String>();
        startParms.add("BasicCalculatorClient.jar");
        try {
            Log.info(c, name.getMethodName(), "Starting the client ...");
            CommonTest.commonClientSetUp("BasicJPAJNDIClient", "BasicJPAJNDIInj", "BasicJPAJNDIInjectClient");
            assertNotNull("Client should report it found application start",
                          CommonTest.testClient.waitForStringInCopiedLog(JPA_ACLI_EYECATCHER + "Hello BasicJPAJNDIAppClient"));

            // testAnnotationInjectedEMF
            String result_testAnnotationInjectedEMF = CommonTest.testClient.waitForStringInCopiedLog(JPA_ACLI_PASS_EYECATCHER + "Test PASSED:" + "testAnnotationInjectedEMF");
            assertNotNull("Assert test testAnnotationInjectedEMF passed.", result_testAnnotationInjectedEMF);

            // testDeploymentDescriptorInjectedEMF
            String result_testDeploymentDescriptorInjectedEMF = CommonTest.testClient
                            .waitForStringInCopiedLog(JPA_ACLI_PASS_EYECATCHER + "Test PASSED:" + "testDeploymentDescriptorInjectedEMF");
            assertNotNull("Assert test testDeploymentDescriptorInjectedEMF passed.", result_testDeploymentDescriptorInjectedEMF);

            // testAnnotationDeploymentDescriptorMergeInjectedMF
            String result_testAnnotationDeploymentDescriptorMergeInjectedMF = CommonTest.testClient
                            .waitForStringInCopiedLog(JPA_ACLI_PASS_EYECATCHER + "Test PASSED:" + "testAnnotationDeploymentDescriptorMergeInjectedMF");
            assertNotNull("Assert test testAnnotationDeploymentDescriptorMergeInjectedMF passed.", result_testAnnotationDeploymentDescriptorMergeInjectedMF);

            // Find End
            assertNotNull("Client should report it found application stop",
                          CommonTest.testClient.waitForStringInCopiedLog(JPA_ACLI_EYECATCHER + "BasicJPAJNDIAppClient exiting."));

        } catch (Exception e) {
            fail("Exception was thrown: " + e);
        }
    }
}
