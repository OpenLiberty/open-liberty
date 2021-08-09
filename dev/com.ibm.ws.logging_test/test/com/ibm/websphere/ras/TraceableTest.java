/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

/**
 *
 */
public class TraceableTest {
    static {
        LoggingTestUtils.ensureLogManager();
    }

    static SharedOutputManager outputMgr;

    private class TraceableImpl implements Traceable {

        @Override
        public String toTraceString() {
            return "alpha";
        }

        @Override
        public String toString() {
            return "bravo";
        }
    }

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
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @Test
    public void testRASInvokesToTraceString() {
        final String m = "testRASInvokesToTraceString";

        Map<String, Object> map = new HashMap<String, Object>();
        try {
            // Turn off copy of System.out and System.err to system streams
            map.put("traceFileName", "stdout");
            TrConfigurator.update(map);
            LoggingTestUtils.setTraceSpec("*=all");

            // Because of how SharedOutputManager / Captured streams work, we won't see system out
            // (comparison to the original output stream will fail)

            TraceComponent tc = Tr.register(TraceableTest.class);
            Tr.error(tc, m); // print something
            Tr.error(tc, "TraceableImpl: {0}", new TraceableImpl());
            assertTrue("Expected trace string should be present in system err (for Tr.error)", outputMgr.checkForStandardErr("TraceableImpl.*alpha"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);

            map.put("traceFileName", "trace.log");
            TrConfigurator.update(map);
            LoggingTestUtils.setTraceSpec("*=info");
        }
    }
}
