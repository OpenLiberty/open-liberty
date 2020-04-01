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
package com.ibm.websphere.microprofile.faulttolerance_fat.suite;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.FallbackMethodTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.TxRetryReorderedTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.TxRetryTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.async.AsyncRequestScopedContextTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.async.AsyncReturnNullTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.completionstage.CDICompletionStageTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.ejb.AsyncEJBTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.InterceptorTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.jaxrs.JaxRsTest;
import com.ibm.websphere.microprofile.faulttolerance_fat.validation.ValidationTest;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(Suite.class)
@SuiteClasses({
                TxRetryTest.class,
                TxRetryReorderedTest.class,
                CDIAsyncTest.class,
                CDIBulkheadTest.class,
                CDICircuitBreakerTest.class,
                CDIFallbackTest.class,
                CDIRetryTest.class,
                CDITimeoutTest.class,
                CDIAnnotationsDisabledTest.class,
                AsyncEJBTest.class,
                TestMultiModuleConfigLoad.class,
                TestMultiModuleClassLoading.class,
                ValidationTest.class,
                FallbackMethodTest.class,
                DisableEnableTest.class,
                CDICompletionStageTest.class,
                InterceptorTest.class,
                JaxRsTest.class,
                AsyncReturnNullTest.class,
                AsyncRequestScopedContextTest.class
})

public class FATSuite {

    @BeforeClass
    public static void setUp() throws Exception {
        String APP_NAME = "CDIFaultTolerance";

        JavaArchive faulttolerance_jar = ShrinkWrap.create(JavaArchive.class, "faulttolerance.jar")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.util");

        WebArchive CDIFaultTolerance_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.cdi")
                        .addAsLibraries(faulttolerance_jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"));

        ShrinkHelper.exportArtifact(CDIFaultTolerance_war, "publish/servers/CDIFaultTolerance/dropins/");
        ShrinkHelper.exportArtifact(CDIFaultTolerance_war, "publish/servers/FTFallback/dropins/");

        String TX_APP_NAME = "TxFaultTolerance";

        WebArchive txFaultTolerance_war = ShrinkWrap.create(WebArchive.class, TX_APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.faulttolerance_fat.tx")
                        .addAsLibraries(faulttolerance_jar);

        ShrinkHelper.exportArtifact(txFaultTolerance_war, "publish/servers/TxFaultTolerance/dropins/");
        ShrinkHelper.exportArtifact(txFaultTolerance_war, "publish/servers/TxFaultToleranceReordered/dropins/");
    }

}
