/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.suite;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.CDIFallbackTest;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.MetricRemovalTest;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(Suite.class)
@SuiteClasses({
                CDIFallbackTest.class,
                MetricRemovalTest.class
})
public class FATSuite {

    @BeforeClass
    public static void setUp() throws Exception {
        String APP_NAME = "CDIFaultToleranceMetrics";

        JavaArchive faulttolerance_jar = ShrinkWrap.create(JavaArchive.class, "faulttolerancemetrics.jar")
                        .addPackages(true, "com.ibm.websphere.microprofile.faulttolerance.metrics.utils");

        WebArchive CDIFaultTolerance_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.websphere.microprofile.faulttolerance.metrics.app")
                        .addAsLibraries(faulttolerance_jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"));

        ShrinkHelper.exportArtifact(CDIFaultTolerance_war, "publish/servers/CDIFaultToleranceMetrics/dropins/");
    }
}
