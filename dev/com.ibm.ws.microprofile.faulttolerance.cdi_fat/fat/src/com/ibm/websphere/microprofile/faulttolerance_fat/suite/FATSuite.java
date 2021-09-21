/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.suite;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.TestMultiModuleClassLoading;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.TestMultiModuleConfigLoad;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDIAnnotationsDisabledTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.FallbackMethodTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.FaultToleranceMainTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.TxRetryReorderedTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.TxRetryTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.async.AsyncRequestScopedContextTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.async.AsyncReturnNullTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.completionstage.CDICompletionStageTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.ejb.AsyncEJBTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableClient;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.InterceptorTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.jaxrs.JaxRsTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.validation.ValidationTest;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                // Core functionality
                FaultToleranceMainTest.class,
                CDICompletionStageTest.class,

                // FULL mode tests
                CDIAnnotationsDisabledTest.class,
                FallbackMethodTest.class,
                ValidationTest.class,
                TestMultiModuleConfigLoad.class,
                TestMultiModuleClassLoading.class,
                AsyncReturnNullTest.class,
                AsyncRequestScopedContextTest.class,
                InterceptorTest.class,

                // Integration with other features
                AsyncEJBTest.class,
                JaxRsTest.class,
                TxRetryTest.class,
                TxRetryReorderedTest.class,
})

public class FATSuite {

    /**
     * Export the CDIFaultTolerance app to the given server's dropins folder
     *
     * @param server the server to export to
     * @throws Exception
     */
    public static void exportCDIFaultToleranceAppToServer(LibertyServer server) throws Exception {
        String APP_NAME = "CDIFaultTolerance";

        JavaArchive faulttolerance_jar = ShrinkWrap.create(JavaArchive.class, "faulttolerance.jar")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.util");

        WebArchive CDIFaultTolerance_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.cdi")
                        .addAsLibraries(faulttolerance_jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"));

        ShrinkHelper.exportDropinAppToServer(server, CDIFaultTolerance_war, DeployOptions.SERVER_ONLY);
    }

    /**
     * Export the TxFaultTolerance app to the given server's dropins folder
     *
     * @param server the server to export to
     * @throws Exception
     */
    public static void exportTxFaultToleranceAppToServer(LibertyServer server) throws Exception {
        JavaArchive faulttolerance_jar = ShrinkWrap.create(JavaArchive.class, "faulttolerance.jar")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.util");

        String TX_APP_NAME = "TxFaultTolerance";

        WebArchive txFaultTolerance_war = ShrinkWrap.create(WebArchive.class, TX_APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.tx")
                        .addAsLibraries(faulttolerance_jar);

        ShrinkHelper.exportDropinAppToServer(server, txFaultTolerance_war, DeployOptions.SERVER_ONLY);
    }

    /**
     * Export the DisableEnable app to the given server's dropins folder
     *
     * @param server the server to export to
     * @throws Exception
     */
    public static void exportDisableEnableAppToServer(LibertyServer server) throws Exception {
        String ENABLE_DISABLE_APP_NAME = "DisableEnable";

        StringBuilder config = new StringBuilder();
        config.append("com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableClient/Retry/enabled=false\n");
        config.append("com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableClient/failWithOneRetry/Retry/enabled=true\n");
        config.append("com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableClassAnnotatedClient/Retry/enabled=false\n");

        WebArchive EnableDisable_war = ShrinkWrap.create(WebArchive.class, ENABLE_DISABLE_APP_NAME + ".war")
                        .addClasses(DisableEnableServlet.class, DisableEnableClient.class, ConnectException.class)
                        .addAsResource(new StringAsset(config.toString()), "META-INF/microprofile-config.properties")
                        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, EnableDisable_war, DeployOptions.SERVER_ONLY);
    }

}
