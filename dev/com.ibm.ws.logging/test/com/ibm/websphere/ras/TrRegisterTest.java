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
package com.ibm.websphere.ras;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.LoggingTestUtils;
import test.common.SharedOutputManager;

/**
 * Test TraceComponent registration methods
 */
public class TrRegisterTest {
    static {
        LoggingTestUtils.ensureLogManager();
    }
    static final Class<?> myClass = TrRegisterTest.class;
    static final String myName = TrRegisterTest.class.getName();

    static SharedOutputManager outputMgr;

    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        SharedTr.clearComponents();
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    @Test
    public void testRegisterClass() {
        final String m = "testRegisterClass";
        try {

            // TraceOptions annotation present on that package:
            // groups = [[logging]]
            TraceComponent tc = Tr.register(myClass);
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            System.out.println(tc.toString());

            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[logging],com.ibm.ws.logging.internal.resources.LoggingMessages,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRegisterClassGroup() {
        final String m = "testRegisterClassGroup";

        try {
            TraceComponent tc = Tr.register(myClass, "group");
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle

            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[group],null,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRegisterClassGroupBundle() {
        final String m = "testRegisterClassGroupBundle";
        try {
            TraceComponent tc = Tr.register(myClass, "group", "bundle");
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle

            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[group],bundle,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
