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
package com.ibm.ws.channel.ssl.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class SSLHandshakeErrorTrackerTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    /**
     * Test method for {@link com.ibm.ws.channel.ssl.internal.SSLHandshakeErrorTracker#SSLHandshakeErrorTracker(boolean)}.
     */
    @Test
    public void shouldLogError_true() {
        SSLHandshakeErrorTracker tracker = new SSLHandshakeErrorTracker(true, 100);
        Exception failure = new Exception("Expected");
        tracker.noteHandshakeError(failure);
        assertTrue("Expected handshake failure error message not logged",
                   outputMgr.checkForStandardErr("CWWKO0801E"));
    }

    /**
     * Test method for {@link com.ibm.ws.channel.ssl.internal.SSLHandshakeErrorTracker#SSLHandshakeErrorTracker(boolean)}.
     */
    @Test
    public void shouldLogError_atCount() {
        SSLHandshakeErrorTracker tracker = new SSLHandshakeErrorTracker(true, 2);
        Exception failure = new Exception("Expected");
        tracker.noteHandshakeError(failure);
        assertTrue("Expected handshake failure error message not logged",
                   outputMgr.checkForStandardErr("CWWKO0801E"));
        outputMgr.resetStreams();

        tracker.noteHandshakeError(failure);
        assertTrue("Expected handshake failure error message not logged",
                   outputMgr.checkForStandardErr("CWWKO0801E"));
    }

    /**
     * Test method for {@link com.ibm.ws.channel.ssl.internal.SSLHandshakeErrorTracker#SSLHandshakeErrorTracker(boolean)}.
     */
    @Test
    public void shouldLogError_aboveCount() {
        SSLHandshakeErrorTracker tracker = new SSLHandshakeErrorTracker(true, 2);
        Exception failure = new Exception("Expected");
        tracker.noteHandshakeError(failure);
        assertTrue("Expected handshake failure error message not logged",
                   outputMgr.checkForStandardErr("CWWKO0801E"));
        outputMgr.resetStreams();

        tracker.noteHandshakeError(failure);
        assertTrue("Expected handshake failure error message not logged",
                   outputMgr.checkForStandardErr("CWWKO0801E"));
        outputMgr.resetStreams();

        tracker.noteHandshakeError(failure);
        assertFalse("Handshake failure error message should not be logged after the max number of times",
                    outputMgr.checkForStandardErr("CWWKO0801E"));
        assertTrue("Expected message that logging will handshake failure will stop was not logged",
                   outputMgr.checkForMessages("CWWKO0804I"));
        outputMgr.resetStreams();

        tracker.noteHandshakeError(failure);
        assertFalse("Handshake failure error message should not be logged after the max number of times",
                    outputMgr.checkForStandardErr("CWWKO0801E"));
        assertFalse("Logging has stopped message should not be logged again",
                    outputMgr.checkForMessages("CWWKO0804I"));
    }

    /**
     * Test method for {@link com.ibm.ws.channel.ssl.internal.SSLHandshakeErrorTracker#noteHandshakeError(java.lang.Exception)}.
     */
    @Test
    public void shouldLogError_false() {
        SSLHandshakeErrorTracker tracker = new SSLHandshakeErrorTracker(false, 100);
        Exception failure = new Exception("Expected");
        tracker.noteHandshakeError(failure);
        assertFalse("Handshake failure error message unexpectedly logged",
                    outputMgr.checkForStandardErr("CWWKO0801E"));
    }

}
