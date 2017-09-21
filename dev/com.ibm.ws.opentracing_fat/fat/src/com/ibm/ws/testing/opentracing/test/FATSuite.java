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

/**
 * <p>The open tracing FAT suite.</p>
 *
 * <p>This class *must* be named "FATSuite", since the test code is hard coded
 * to look for just that class.</p>
 *
 * <p>See the file "open-liberty/dev/com.ibm.ws.opentracing_fat/generated/autoFVT/src/ant/launch.xml",
 * which has property "filesToFind" coded to "FATSuiteLite.class;FATSuite.class;FATTest.class".</p>
 */
@RunWith(Suite.class)
@SuiteClasses({
    TestSpanUtils.class,
    FATOpenTrace.class
})
public class FATSuite implements FATOpenTraceConstants {
    // Logging ...

    private static final Class<? extends FATSuite> CLASS = FATSuite.class;

    private static void info(String methodName, String text) {
        FATLogging.info(CLASS, methodName, text);
    }

    //

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        info(methodName, "ENTER / RETURN");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        String methodName = "tearDown";
        info(methodName, "ENTER / RETURN");
    }
}
