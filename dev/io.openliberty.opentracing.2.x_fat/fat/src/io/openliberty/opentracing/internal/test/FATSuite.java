/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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
    FATOpentracingHelloWorldLRC.class,
    FATOpentracingHelloWorldNoTracer.class,
    FATMPOpenTracing.class
// Comment out this test class until microprofile-4.0 is available
//    MicroProfile40NoTracer.class  
    
})
public class FATSuite implements FATOpentracingConstants {
    
    private static final Class<? extends FATSuite> CLASS = FATSuite.class;
    private static final String FEATURE_NAME1 = "io.openliberty.opentracing.mock-2.0.mf";
    private static final String BUNDLE_NAME1 = "io.openliberty.opentracing.mock-2.0.jar";

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
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String methodName = "tearDown";
        info(methodName, "ENTER / RETURN");
    }
}
