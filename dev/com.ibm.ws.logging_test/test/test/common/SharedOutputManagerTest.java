/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import test.TestConstants;

import com.ibm.websphere.ras.SharedTraceComponent;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.logprovider.TrService;

/**
 *
 */
public class SharedOutputManagerTest {
    Mockery context = new JUnit4Mockery();
    final TrService mockTrService = context.mock(TrService.class);
    SharedOutputManager outputMgr;

    /**
     * Test method for {@link test.common.SharedOutputManager#zapTrConfig(java.lang.String)}.
     */
    @Test
    public void testDefaultCreate() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);

        try {
            System.out.println("pre.system.out");
            assertFalse("Output streams not captured, should not find key (stdout)", outputMgr.checkForStandardOut("pre.system.out"));

            System.err.println("pre.system.err");
            assertFalse("Output streams not captured, should not find key (stderr)", outputMgr.checkForStandardErr("pre.system.err"));

            // CAPTURE STREAMS
            outputMgr.captureStreams();

            System.out.println("captured.out");
            assertTrue("Output streams captured, should find key (stdout)", outputMgr.checkForStandardOut("captured.out"));

            System.err.println("captured.err");
            assertTrue("Output streams captured, should find key (stderr)", outputMgr.checkForStandardErr("captured.err"));

            TraceComponent tc = Tr.register(getClass());
            SharedTraceComponent.setTraceSpec(tc, getClass().getName() + "=all=enabled");

            // debug: only to stdout
            Tr.debug(tc, "debug");
            assertFalse("Debug should not go to standard out", outputMgr.checkForStandardOut("debug"));
            assertFalse("Debug should not go to standard err", outputMgr.checkForStandardErr("debug"));

            // info: only to stdout
            Tr.info(tc, "info");
            assertFalse("Info should not go to standard out", outputMgr.checkForStandardOut("info"));
            assertFalse("Info should not go to standard err", outputMgr.checkForStandardErr("info"));

            // audit: only to stdout
            Tr.audit(tc, "audit");
            assertTrue("Audit goes to stdout", outputMgr.checkForStandardOut("audit"));
            assertFalse("Audit does not go to stderr", outputMgr.checkForStandardErr("audit"));

            // error: to standard out AND un-redirected standard err
            Tr.error(tc, "error");
            assertFalse("Error should not go to standard out", outputMgr.checkForStandardOut("error"));
            assertTrue("Error should go to to standard err", outputMgr.checkForStandardErr("error"));

            // fail: to standard out AND un-redirected standard err
            Tr.fatal(tc, "fatal");
            assertFalse("Fatal should not go to standard out", outputMgr.checkForStandardOut("fatal"));
            assertTrue("Fatal goes to stderr", outputMgr.checkForStandardErr("fatal"));

            System.out.println("post.system.out");
            System.err.println("post.system.err");
        } finally {
            outputMgr.restoreStreams();
        }
    }
}
