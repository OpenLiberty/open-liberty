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
package com.ibm.ws.zos.logging.internal;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class LoggingHandlerDiagnosticsVectorTest {

    LoggingHandlerDiagnosticsVector loggingHandlerDiagnosticsVector;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        loggingHandlerDiagnosticsVector = new LoggingHandlerDiagnosticsVector();;
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        loggingHandlerDiagnosticsVector = null;
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.LoggingHandlerDiagnosticsVector#insertElementAtBegining()}.
     */
    @Test
    public void test_insertElementAtBegining() {

        loggingHandlerDiagnosticsVector.insertElementAtBegining("Message 1", 1);
        assertEquals(loggingHandlerDiagnosticsVector.savedDiagnostics.size(), 1);
        for (int i = 2; i <= LoggingHandlerDiagnosticsVector.VECTOR_LIMIT; i++) {
            loggingHandlerDiagnosticsVector.insertElementAtBegining("Message x", i);
            assertEquals(loggingHandlerDiagnosticsVector.savedDiagnostics.size(), i);
        }
        // make sure size matches the limit
        assertEquals(loggingHandlerDiagnosticsVector.savedDiagnostics.size(), LoggingHandlerDiagnosticsVector.VECTOR_LIMIT);
        // insert again and make sure size did not go over the limit
        loggingHandlerDiagnosticsVector.insertElementAtBegining("Message extra", LoggingHandlerDiagnosticsVector.VECTOR_LIMIT + 1);
        assertEquals(loggingHandlerDiagnosticsVector.savedDiagnostics.size(), LoggingHandlerDiagnosticsVector.VECTOR_LIMIT);
        // verify first element
        LoggingHandlerDianostics loggingHandlerDiagnostics = loggingHandlerDiagnosticsVector.savedDiagnostics.firstElement();
        assertEquals(loggingHandlerDiagnostics.rc, LoggingHandlerDiagnosticsVector.VECTOR_LIMIT + 1);
        assertEquals(loggingHandlerDiagnostics.msg, "Message extra");
        // verify last element
        loggingHandlerDiagnostics = loggingHandlerDiagnosticsVector.savedDiagnostics.lastElement();
        assertEquals(loggingHandlerDiagnostics.rc, 2);
        assertEquals(loggingHandlerDiagnostics.msg, "Message x");
        loggingHandlerDiagnostics = loggingHandlerDiagnosticsVector.savedDiagnostics.elementAt(LoggingHandlerDiagnosticsVector.VECTOR_LIMIT - 1);
        assertEquals(loggingHandlerDiagnostics.rc, 2);
        assertEquals(loggingHandlerDiagnostics.msg, "Message x");
    }

}
