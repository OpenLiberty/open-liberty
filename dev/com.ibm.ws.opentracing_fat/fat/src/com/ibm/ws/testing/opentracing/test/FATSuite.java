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
    FATOpentracingHelloWorld.class,
    FATMPOpenTracing.class,
    MicroProfile13NoTracer.class
})
public class FATSuite implements FATOpentracingConstants {
    private static final Class<? extends FATSuite> CLASS = FATSuite.class;
    
    private static final String FEATURE_NAME = "com.ibm.ws.opentracing.mock-0.30.mf";
    private static final String BUNDLE_NAME = "com.ibm.ws.opentracing.mock.jar";

    private static void info(String methodName, String text) {
        FATLogging.info(CLASS, methodName, text);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        info(methodName, "ENTER / RETURN");
        LibertyServer server = LibertyServerFactory.getLibertyServer(OPENTRACING_FAT_SERVER1_NAME);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/" + FEATURE_NAME);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + BUNDLE_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String methodName = "tearDown";
        info(methodName, "ENTER / RETURN");
    }
}
