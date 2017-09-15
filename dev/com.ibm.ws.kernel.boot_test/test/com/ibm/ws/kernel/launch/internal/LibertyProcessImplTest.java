/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class LibertyProcessImplTest {
    static SharedOutputManager outputMgr;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.launch.internal.LibertyProcessImpl#getArgs()} .
     */
    @Test
    public void testGetArgs() {
        final String m = "testGetArgs";

        LibertyProcessImpl cmi;
        String[] list;

        try {
            cmi = new LibertyProcessImpl(null, null);
            list = cmi.getArgs();
            assertNotNull(list);
            assertEquals(0, list.length);

            cmi = new LibertyProcessImpl(new ArrayList<String>(), null);
            list = cmi.getArgs();
            assertNotNull(list);
            assertEquals(0, list.length);

            final String dummy = "dummy";

            cmi = new LibertyProcessImpl(Arrays.asList(new String[] { dummy }), null);
            list = cmi.getArgs();
            assertNotNull(list);
            assertEquals(1, list.length);
            assertEquals(dummy, list[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
