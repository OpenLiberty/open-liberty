/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http2.test.exceptions.MissingExpectedFramesException;
import com.ibm.ws.http2.test.exceptions.ReceivedFrameAfterEndOfStream;
import com.ibm.ws.http2.test.exceptions.ReceivedHeadersFrameAfterEndOfHeaders;
import com.ibm.ws.http2.test.exceptions.StreamDidNotReceivedEndOfStreamException;

public class H2StreamResult {

    private int streamId;
    private final ArrayList<Frame> expectedResponse, actualResponse;
    private final ArrayList<FrameTypes> actualResponseTypes, expectedResponseTypes;

    private boolean endOfStreamFlagReceived = false;
    private boolean endOfHeadersFlagReceived = false;
    private boolean frameHasEndOfStreamFlag = false;
    private boolean rstStreamReceived = false;
    private boolean goawayReceived = false;
    private final boolean removalOfEmptyDataFrameNeeded = true;
    private final FrameData eosDataFrame;
    private final FrameSettings ackSettingsFrame;
    private final int eosDataFrameIndex = 0;
    private boolean streamClosureRequired = true;
    private boolean onlyCheckFrameTypes = false;

    private static final String CLASS_NAME = H2StreamResult.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    /**
     * Use only if this is for a promised stream (promised by a push promise frame)
     */
    public H2StreamResult() {
        this(-1);
    }

    public H2StreamResult(int streamId) {

        this.streamId = streamId;
        this.expectedResponse = new ArrayList<Frame>();
        this.actualResponse = new ArrayList<Frame>();
        this.expectedResponseTypes = new ArrayList<FrameTypes>();
        this.actualResponseTypes = new ArrayList<FrameTypes>();

        eosDataFrame = new FrameData(streamId, new byte[0], 0, true, false, false);
        ackSettingsFrame = new FrameSettings();
        ackSettingsFrame.setAckFlag();
    }

    public H2StreamResult(int streamId, Frame[] expectedResponse) {
        this(streamId);
        for (int i = 0; i < expectedResponse.length; i++) {
            addExpectedResponse(expectedResponse[i]);
        }

    }

    private boolean continuationExpected = false;

    protected boolean isContinuationExpected() {
        return this.continuationExpected;
    }

    private boolean goawayExpected = false;

    protected boolean isgoawayExpected() {
        return this.goawayExpected;
    }

    public void addActualRespone(Frame frame) throws ReceivedFrameAfterEndOfStream, ReceivedHeadersFrameAfterEndOfHeaders {

        //TODO: Process RST_STREAM (once sent, the server should close such stream). However,
        //we need to process frames sent before the server receives the RST_STREAM... but we don't have a way to know
        //which of this frames where sent before the server got the RST_STREAM

        //We can receive PRIORITY on a closed stream
        if (!(frame.getFrameType() == FrameTypes.PRIORITY))
            if (rstStreamReceived || (endOfStreamFlagReceived && !continuationExpected))
                throw new ReceivedFrameAfterEndOfStream("The following frame was received on streamId = " + streamId + " after an end of stream flag was received: " + frame);
            else if (endOfHeadersFlagReceived && frame.getFrameType() == FrameTypes.HEADERS)
                throw new ReceivedHeadersFrameAfterEndOfHeaders("The following frame was received on streamId = " + streamId + " after an end of headers flag was received: "
                                                                + frame);

        if (frame.flagEndStreamSet()) {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "addActualRespone", "Frame = " + frame.getFrameType() + " has end of stream flag on.");
            endOfStreamFlagReceived = true;
            //If we got an empty data frame to signal EOS, add it to the expected frames
            if (streamId != 0 && frame.getPayloadLength() == 0)
                expectedResponse.add(eosDataFrame);
        }
        if ((frame.getFrameType() == FrameTypes.HEADERS || frame.getFrameType() == FrameTypes.CONTINUATION)
            && frame.flagEndHeadersSet()) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.logp(Level.INFO, CLASS_NAME, "addActualRespone", "Frame = " + frame.getFrameType() + " has end of headers flag on.");
            endOfHeadersFlagReceived = true;
            continuationExpected = false;
        } else if (frame.getFrameType() == FrameTypes.HEADERS && endOfStreamFlagReceived) {
            continuationExpected = true;
        }
        if (frame.getFrameType() == FrameTypes.RST_STREAM) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.logp(Level.INFO, CLASS_NAME, "addActualRespone", "Received RST_FRAME on streamID: " + streamId);
            rstStreamReceived = true;
        }
        if (frame.getFrameType() == FrameTypes.GOAWAY) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.logp(Level.INFO, CLASS_NAME, "addActualRespone", "Received GOAWAY on streamID: " + streamId);
            goawayReceived = true;
        }

        if (frame.getFrameType() == FrameTypes.SETTINGS && frame.flagAckSet())
            this.expectedResponse.add(ackSettingsFrame);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addActualRespone", "adding frame to actualResponse Frame Type: " + frame.getFrameType() + " stream-id: " + frame.getStreamId());
        }

        // don't add DATA frames the have "DoNotAdd" as part of the data payload
        if (frame.getFrameType() == FrameTypes.DATA) {
            FrameData fd = (FrameData) frame;
            String s = new String(fd.getData());
            if (s.toLowerCase().contains("donotadd")) {
                return;
            }
        }

        this.actualResponse.add(frame);
        this.actualResponseTypes.add(frame.getFrameType());
    }

    public void addExpectedResponse(Frame frame) {
        if (frame.flagEndStreamSet())
            frameHasEndOfStreamFlag = true;
        this.expectedResponse.add(frame);
        this.expectedResponseTypes.add(frame.getFrameType());
        if (frame.getFrameType().equals(FrameTypes.GOAWAY)) {
            this.goawayExpected = true;
        }
    }

    /**
     * Add an expected Frame Type. If this method is called, then only stream types will be verified for this stream.
     *
     * @param type
     */
    public void addExpectedResponse(FrameTypes type) {
        this.onlyCheckFrameTypes = true;
        this.expectedResponseTypes.add(type);
        if (type.equals(FrameTypes.GOAWAY)) {
            this.goawayExpected = true;
        }
    }

    /**
     * If this stream is only concerned with FrameTypes, we should minimally process the received Frames
     *
     * @return
     */
    public boolean shouldConvertFrame() {
        return !onlyCheckFrameTypes;
    }

    public int getStreamId() {
        return this.streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    /**
     *
     * @return True if this stream has received the same number of expected streams.
     */
    public boolean receivedExpectedNumberOfFrames() {
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedExpectedNumberOfFrames",
                        "receivedExpectedNumberOfFrames: actualResponse.size() = " + actualResponse.size() + " expectedResponse.size() = " + expectedResponse.size());
        if (onlyCheckFrameTypes) {
            return actualResponseTypes.size() == expectedResponseTypes.size();
        }
        return actualResponse.size() == expectedResponse.size();
    }

    /**
     *
     * @return True if this stream has received the same number of expected streams.
     */
    public boolean receivedAtLeastExpectedNumberOfFrames() {
        if (!onlyCheckFrameTypes) {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAtLeastExpectedNumberOfFrames",
                            "receivedExpectedNumberOfFrames: actualResponse.size() = " + actualResponse.size()
                                                                                               + " expectedResponse.size() = " + expectedResponse.size());
        } else {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAtLeastExpectedNumberOfFrames",
                            "receivedExpectedNumberOfFrames: actualResponseTypes.size() = " + actualResponseTypes.size()
                                                                                               + " expectedResponseTypes.size() = " + expectedResponseTypes.size());
        }

        if (onlyCheckFrameTypes) {
            return actualResponseTypes.size() >= expectedResponseTypes.size();
        }
        return actualResponse.size() >= expectedResponse.size();
    }

    /**
     *
     * @return True if a frame had the end of stream flag on.
     */
    public boolean receivedEndOfStreamOrRstStream() {
        return endOfStreamFlagReceived || rstStreamReceived;
    }

    /**
     *
     * @return True if a goaway frame was received.
     */
    public boolean goawayReceived() {
        return goawayReceived;
    }

    /*
     * Check the expected and actual responses for a match
     */
    public List<Exception> checkResult() {
        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "Comparing frames of streamId: " + streamId);

        List<Exception> exceptionsOfStream = new ArrayList<Exception>();

        if (this.onlyCheckFrameTypes) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "Comparing frame types of streamId: " + streamId);
            // we're only checking received frame types on this stream: verify that the expected types were received, then return
            for (FrameTypes type : expectedResponseTypes) {
                if (!actualResponseTypes.contains(type)) {
                    exceptionsOfStream.add(new MissingExpectedFramesException("Expected Frame Type(s) not found: \n" + type.toString()));
                }
            }
            if (0 == exceptionsOfStream.size()) {
                if (LOGGER.isLoggable(Level.INFO))
                    LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "All frame types present for streamId: " + streamId);
            }
            return exceptionsOfStream;
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "List of received frames for streamId " + streamId + ": \n");
            // only list first 10 streams, otherwise to verbose for stress like tests
            Object[] oa = actualResponse.toArray();
            int len = oa.length;
            if (len > 10) {
                len = 10;
            }
            for (int i = 0; i < len; i++) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "Frame: " + (i + 1) + oa[i].toString());
            }
            // log the very last frame
            if (oa.length > 10) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "Last Frame: " + (oa.length) + oa[oa.length - 1].toString());
            }
        }

        if (expectedResponse == null && actualResponse != null) {
            throw new NullPointerException("Expected response for stream " + streamId + " is null, while actual response is not");
        } else if (expectedResponse != null && actualResponse == null) {
            throw new NullPointerException("Actual response for stream " + streamId + " is null, while expected response is not");
        } else if (expectedResponse == null && actualResponse == null) {
            throw new NullPointerException("Both expected response and actual responses for stream " + streamId + " are null");
        } else if (streamId != 0 && streamClosureRequired && !(endOfStreamFlagReceived || rstStreamReceived)) {
            exceptionsOfStream.add(new StreamDidNotReceivedEndOfStreamException("StreamId: " + streamId
                                                                                + " did not receive the end of stream flag on any of the received frames or an RST_STREAM. endOfStreamFlagReceived = "
                                                                                + endOfStreamFlagReceived + " rstStreamReceived = " + rstStreamReceived));
        } else {
            Frame expectedFrame = null;
            Frame actualFrame = null;

            StringBuilder expectedFrameNotFound = new StringBuilder();

            /*
             * Compare the two lists
             * Need to improve this to not only look at frame types
             */
            for (int i = 0; i < this.expectedResponse.size(); i++) {
                expectedFrame = expectedResponse.get(i);

                if (i < 10) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "***expectedFrame[" + i + "] = " + expectedFrame);
                    }
                } else if (i == this.expectedResponse.size() - 1) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.logp(Level.INFO, CLASS_NAME, "checkResult", "***last expectedFrame[" + i + "] = " + expectedFrame);
                    }
                }

                boolean expectedFrameFound = false;
                for (int j = 0; j < this.actualResponse.size(); j++) {
                    actualFrame = actualResponse.get(j);
                    if (expectedFrame.equals(actualFrame)) {
                        expectedFrameFound = true;
                        //remove this element so we avoid problems when frames are the same (like Settings frames)
                        actualResponse.remove(j--);
                        break;
                    }
                }

                if (!expectedFrameFound) {
                    System.out.println("***expectedframe not found!: " + expectedFrame);
                    expectedFrameNotFound.append(expectedFrame).append("\n");
                }
            }

            if (expectedFrameNotFound.length() != 0) //if this string is not empty, we did not find some expected frames
                exceptionsOfStream.add(new MissingExpectedFramesException("Expected frame(s) not found: \n" + expectedFrameNotFound.toString()));

        }
        return exceptionsOfStream;
    }

    public boolean isExpectedFrameType(Frame frame) {
        FrameTypes x = frame.getFrameType();
        // check the expected frame array
        for (int i = 0; i < expectedResponse.size(); i++) {
            if (x == expectedResponse.get(i).getFrameType()) {
                return true;
            }
        }
        // Also check the expected frame type array
        for (int i = 0; i < expectedResponseTypes.size(); i++) {
            if (x == expectedResponseTypes.get(i)) {
                return true;
            }
        }

        return false;
    }

    public boolean isExpectedFrame(Frame frame) {
        return expectedResponse.contains(frame);
    }

    public void setStreamClosureRequired(boolean streamClosureRequired) {
        this.streamClosureRequired = streamClosureRequired;
    }

    /**
     * @param frame
     * @return
     */
    public boolean didFrameArrive(Frame frame) {
        return actualResponse.contains(frame);
    }

}
