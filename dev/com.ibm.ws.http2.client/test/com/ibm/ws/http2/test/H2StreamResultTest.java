/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http2.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http2.test.exceptions.ReceivedFrameAfterEndOfStream;

public class H2StreamResultTest {

    @Test
    public void expectedRstStreamAfterEndStreamIsAccountedAsReceived() throws Exception {
        H2StreamResult result = new H2StreamResult(3);
        FrameRstStream expectedRstStream = new FrameRstStream(3, Constants.STREAM_CLOSED, false);

        result.addExpectedResponse(expectedRstStream);
        result.addActualRespone(new FrameHeaders(3, new byte[] { 0x00 }, true, true));
        result.addActualRespone(new FrameRstStream(3, Constants.STREAM_CLOSED, false));

        List<Exception> exceptions = result.checkResult();

        assertTrue("Expected RST_STREAM after END_STREAM must satisfy the expected RST_STREAM frame instead of being recorded as both missing and unexpected: " + exceptions,
                   exceptions.isEmpty());
    }

    @Test
    public void unexpectedRstStreamAfterEndStreamIsStillRejected() throws Exception {
        H2StreamResult result = new H2StreamResult(3);

        result.addActualRespone(new FrameHeaders(3, new byte[] { 0x00 }, true, true));

        try {
            result.addActualRespone(new FrameRstStream(3, Constants.STREAM_CLOSED, false));
            fail("An unexpected RST_STREAM after END_STREAM must still be rejected");
        } catch (ReceivedFrameAfterEndOfStream expected) {
            // Expected: only an explicitly expected terminal reset is account-able.
        }
    }

    @Test
    public void secondRstStreamAfterExpectedTerminalResetIsRejected() throws Exception {
        H2StreamResult result = new H2StreamResult(3);
        result.addExpectedResponse(new FrameRstStream(3, Constants.STREAM_CLOSED, false));
        result.addActualRespone(new FrameHeaders(3, new byte[] { 0x00 }, true, true));
        result.addActualRespone(new FrameRstStream(3, Constants.STREAM_CLOSED, false));

        try {
            result.addActualRespone(new FrameRstStream(3, Constants.STREAM_CLOSED, false));
            fail("A second RST_STREAM after the terminal reset must be rejected");
        } catch (ReceivedFrameAfterEndOfStream expected) {
            // Expected: only the first explicitly expected terminal reset is accepted.
        }
    }
}
