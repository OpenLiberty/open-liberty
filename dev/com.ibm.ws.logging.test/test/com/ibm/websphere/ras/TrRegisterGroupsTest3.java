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

import com.ibm.websphere.ras.annotation.TraceOptions;

import test.TestConstants;
import test.common.SharedOutputManager;

/**
 * Test TraceComponent registration methods using annotations to specify group
 * only - ensure groups takes precedence over group
 */
@TraceOptions(traceGroup = "singlegroup", traceGroups = { "multigroup1", "multigroup2" })
public class TrRegisterGroupsTest3 {
    // static Class<?> myClass = TrRegisterGroupsTest.class;

    static final String myName = TrRegisterGroupsTest3.class.getName();

    static SharedOutputManager outputMgr;

    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
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
        Class<?> myClass = this.getClass();
        TraceOptions options = myClass.getAnnotation(TraceOptions.class);
        System.out.println("options are: " + options);

        final String m = "testRegisterClass";
        try {
            TraceComponent tc = Tr.register(myClass);
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[multigroup1, multigroup2],,null]", str[0]);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}