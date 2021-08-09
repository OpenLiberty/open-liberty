/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.intfc.internal;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 */
public class UserRegistryWrapperWithTraceTest extends UserRegistryWrapperTest {
    @BeforeClass
    public static void traceSetUp() {
        outputMgr.trace("*=all");
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        assertFalse("FAIL: should not find any passwords in messages",
                    outputMgr.checkForMessages(PWD));
        assertFalse("FAIL: should not find any passwords in trace",
                    outputMgr.checkForTrace(PWD));
    }

    @AfterClass
    public static void traceTearDown() {
        outputMgr.trace("*=all=disabled");
    }
}
