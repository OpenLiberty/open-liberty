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

import java.util.Arrays;

/**
 *
 */
public class FrameDataClient extends com.ibm.ws.http.channel.h2internal.frames.FrameData {

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
    public FrameDataClient(int streamId, byte[] data, int paddingLength, boolean endStream, boolean padded, boolean reserveBit) {
        super(streamId, data, paddingLength, endStream, padded, reserveBit);
    }

    @Override
    public boolean equals(Object receivedFrame) {
        if (!(receivedFrame instanceof FrameDataClient) || !(receivedFrame instanceof com.ibm.ws.http.channel.h2internal.frames.FrameData))
            return false;

        FrameDataClient frameDataToCompare = (FrameDataClient) receivedFrame;

        if (this.flagAckSet() != frameDataToCompare.flagAckSet()) {
            //System.out.println("this.flagAckSet() = " + this.flagAckSet() + " frameDataToCompare.flagAckSet() = " + frameDataToCompare.flagAckSet());
            return false;
        }
        if (this.flagPrioritySet() != frameDataToCompare.flagPrioritySet()) {
            //System.out.println("this.flagPrioritySet() = " + this.flagPrioritySet() + " frameDataToCompare.flagPrioritySet() = " + frameDataToCompare.flagPrioritySet());
            return false;
        }

        if (this.flagEndHeadersSet() != frameDataToCompare.flagEndHeadersSet()) {
            //System.out.println("this.flagEndHeadersSet() = " + this.flagEndHeadersSet() + " frameDataToCompare.flagEndHeadersSet() = " + frameDataToCompare.flagEndHeadersSet());
            return false;
        }
        if (this.flagPaddedSet() != frameDataToCompare.flagPaddedSet()) {
            //System.out.println("this.flagPaddedSet() = " + this.flagPaddedSet() + " frameDataToCompare.flagPaddedSet() = " + frameDataToCompare.flagPaddedSet());
            return false;
        }
        if (this.getFrameType() != frameDataToCompare.getFrameType()) {
            //System.out.println("getFrameType is false");
            return false;
        }
        if (this.getFrameReserveBit() != frameDataToCompare.getFrameReserveBit()) {
            //System.out.println("getFrameReserveBit is false");
            return false;
        }
        if (this.getPayloadLength() != frameDataToCompare.getPayloadLength()) {
            //System.out.println("getPayloadLength is false");
            return false;
        }
        if (this.getStreamId() != frameDataToCompare.getStreamId()) {
            //System.out.println("getStreamId is false");
            return false;
        }

        if (this.getPaddingLength() != frameDataToCompare.getPaddingLength()) {
            return false;
        }
        if (!Arrays.equals(this.getData(), frameDataToCompare.getData())) {
            // see if the expected frame data is a substring of the actual frame, which should be good enough to be equal for testing
            // (otherwise if a couple of actual frames get concatenated into one frame, it won't match, when it should)
            String me = new String(this.getData());
            String miniMe = new String(frameDataToCompare.getData());

            if (me.indexOf(miniMe) == -1) {
                return false;
            }
        }

        return true;
    }

}
