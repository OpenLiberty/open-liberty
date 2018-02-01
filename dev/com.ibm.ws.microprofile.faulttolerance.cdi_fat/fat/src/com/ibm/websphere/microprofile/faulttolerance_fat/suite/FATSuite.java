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
package com.ibm.websphere.microprofile.faulttolerance_fat.suite;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.TestMultiModuleClassLoading;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.TestMultiModuleConfigLoad;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDIAnnotationsDisabledTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDIAsyncTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDIBulkheadTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDICircuitBreakerTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDIFallbackTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDIRetryTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.CDITimeoutTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.validation.ValidationTest;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.SharedServer;

@RunWith(Suite.class)
@SuiteClasses({
                CDIAsyncTest.class,
                CDIBulkheadTest.class,
                CDICircuitBreakerTest.class,
                CDIFallbackTest.class,
                CDIRetryTest.class,
                CDITimeoutTest.class,
                CDIAnnotationsDisabledTest.class,
                TestMultiModuleConfigLoad.class,
                TestMultiModuleClassLoading.class,
                ValidationTest.class,
})

public class FATSuite {

    public static SharedServer MULTI_MODULE_SERVER = new SharedServer("FaultToleranceMultiModule");

    @BeforeClass
    public static void setUp() throws Exception {
        String APP_NAME = "CDIFaultTolerance";

        JavaArchive faulttolerance_jar = ShrinkWrap.create(JavaArchive.class, "faulttolerance.jar")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.util");

        WebArchive CDIFaultTolerance_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.cdi")
                        .addAsLibraries(faulttolerance_jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "persistence.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"));

        ShrinkHelper.exportArtifact(CDIFaultTolerance_war, "publish/servers/CDIFaultTolerance/dropins/");
    }

    @AfterClass
    public static void shutdownMultiModuleServer() throws Exception {
        if (MULTI_MODULE_SERVER.getLibertyServer().isStarted()) {
            MULTI_MODULE_SERVER.getLibertyServer().stopServer("CWMFT50[01][0-9]E.*badMethod",
                                                              "CWMFT5019W.*badMethod");
        }
    }

}
