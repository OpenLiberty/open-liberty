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
package com.ibm.ws.ras.instrumentation.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class SimpleTryCatchTest {

    // Set ffdc exception dir in test data
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.TEST_DATA);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    public void waitSomeTime(long millis) {
        try {
            wait(millis);
        } catch (InterruptedException ex) {
        }
    }

    @Test
    public void simpleTryCatch() {
        try {
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException iae) {
        }
    }

    @Test
    @FFDCIgnore(IllegalArgumentException.class)
    public void ignoredSimpleTryCatch() {
        try {
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException iae) {
        }
    }
}
