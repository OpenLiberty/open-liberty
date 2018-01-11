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
package com.ibm.ws.ffdc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.TestConstants;
import test.common.SharedOutputManager;

public class FFDCTest {
    static SharedOutputManager outputMgr;

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

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    @Test
    public void testDiagnosticModule() throws DiagnosticModuleRegistrationFailureException {
        final String m = "testDiagnosticModule";
        try {
            int result = 0;
            Map<String, DiagnosticModule> map;

            map = FFDC.getDiagnosticModuleMap();
            map.clear();

            result = FFDC.registerDiagnosticModule(new DiagnosticModule(), "test.package");
            assertEquals("Successful add should have rc=0", 0, result);

            result = FFDC.registerDiagnosticModule(new DiagnosticModule(), "test.package");
            assertEquals("Key already exists add should have rc=1", 1, result);

            final Exception e = new Exception("unittest exception - test4");

            assertEquals("One entry in map: ", 1, map.size());

            FFDC.deregisterDiagnosticModule("test.package");
            assertEquals("Empty map: ", 0, map.size());

            boolean present = FFDC.deregisterDiagnosticModule("test.package");
            assertFalse("Key should already have been removed", present);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
