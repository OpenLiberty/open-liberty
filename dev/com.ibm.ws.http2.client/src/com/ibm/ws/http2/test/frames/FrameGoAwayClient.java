/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test.frames;

import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;

/**
 *
 */
public class FrameGoAwayClient extends FrameGoAway {

    int[] expectedErrorCodes;
    int[] expectedStreamIds;

    /**
     *
     * This class is the same as com.ibm.ws.http.channel.h2internal.frames.FrameData
     * but the equals method is different (the difference is that we don't care about the value
     * of endStream).
     *
     * @param streamId
     * @param data
     * @param paddingLength
     * @param endStream
     * @param padded
     * @param reserveBit
     */
    public FrameGoAwayClient(int streamId, byte[] debugData, int[] errorCodes, int[] lastStreamIds) {
        super(streamId, debugData, errorCodes[0], lastStreamIds[0], false);
        expectedErrorCodes = errorCodes;
        expectedStreamIds = lastStreamIds;
    }

    private boolean isExpectedErrorCode(int code) {
        for (int i : expectedErrorCodes) {
            if (i == code) {
                return true;
            }
        }
        return false;
    }

    private boolean isExpectedLastStreamId(int id) {
        for (int i : expectedStreamIds) {
            if (i == id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object receivedFrame) {
        if (!(receivedFrame instanceof FrameGoAwayClient) || !(receivedFrame instanceof FrameGoAway))
            return false;

        FrameGoAwayClient frameToCompare = (FrameGoAwayClient) receivedFrame;

        if (this.getFrameType() != frameToCompare.getFrameType()) {
            return false;
        }
        if (this.getFrameReserveBit() != frameToCompare.getFrameReserveBit()) {
            return false;
        }
        if (this.getStreamId() != frameToCompare.getStreamId()) {
            return false;
        }
        if (!isExpectedLastStreamId(frameToCompare.getLastStreamId())) {
            return false;
        }
        if (!isExpectedErrorCode(frameToCompare.getErrorCode())) {
            return false;
        }
        return true;
    }

}
