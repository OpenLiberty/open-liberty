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
package com.ibm.ws.testing.opentracing.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * <p>The open tracing FAT suite.</p>
 *
 * <p>This class *must* be named "FATSuite", since the test code is hard coded
 * to look for just that class.</p>
 */
@RunWith(Suite.class)
@SuiteClasses({
    TestSpanUtils.class,
    FATOpentracing.class,
    FATOpentracingHelloWorld.class,
    FATMPOpenTracing.class,
    MicroProfile14NoTracer.class
})
public class FATSuite implements FATOpentracingConstants {
    
    private static Set<String> openTracing11 = new HashSet<String>(Arrays.asList("mpOpenTracing-1.1", "usr:opentracingMock-1.1", "microProfile-1.4", "servlet-3.1", "jaxrs-2.0"));
    private static Set<String> openTracing12 = new HashSet<String>(Arrays.asList("mpOpenTracing-1.2", "usr:opentracingMock-1.2", "microProfile-1.4", "servlet-3.1", "jaxrs-2.0"));
    private static Set<String> openTracing13 = new HashSet<String>(Arrays.asList("mpOpenTracing-1.3", "usr:opentracingMock-1.3", "microProfile-2.1", "servlet-4.0", "jaxrs-2.1"));
    
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction(openTracing11, openTracing12)
                             .forceAddFeatures(false)
                             .withMinJavaLevel(8))
                    .andWith(new FeatureReplacementAction(openTracing12, openTracing13)
                             .forceAddFeatures(false)
                             .withMinJavaLevel(8));
    
    private static final Class<? extends FATSuite> CLASS = FATSuite.class;
    private static final String FEATURE_NAME1 = "com.ibm.ws.opentracing.mock-1.1.mf";
    private static final String BUNDLE_NAME1 = "com.ibm.ws.opentracing.mock-1.1.jar";
    private static final String FEATURE_NAME2 = "com.ibm.ws.opentracing.mock-1.2.mf";
    private static final String BUNDLE_NAME2 = "com.ibm.ws.opentracing.mock-1.2.jar";
    private static final String FEATURE_NAME3 = "com.ibm.ws.opentracing.mock-1.3.mf";
    private static final String BUNDLE_NAME3 = "com.ibm.ws.opentracing.mock-1.3.jar";

    private static void info(String methodName, String text) {
        FATLogging.info(CLASS, methodName, text);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        info(methodName, "ENTER / RETURN");
        LibertyServer server = LibertyServerFactory.getLibertyServer(OPENTRACING_FAT_SERVER1_NAME);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/" + FEATURE_NAME1);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + BUNDLE_NAME1);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/" + FEATURE_NAME2);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + BUNDLE_NAME2);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/" + FEATURE_NAME3);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + BUNDLE_NAME3);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String methodName = "tearDown";
        info(methodName, "ENTER / RETURN");
    }
}
