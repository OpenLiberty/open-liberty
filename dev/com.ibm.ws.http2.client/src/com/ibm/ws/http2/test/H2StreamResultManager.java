/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http2.test.connection.H2Connection;
import com.ibm.ws.http2.test.exceptions.ExpectedPushPromiseDoesNotIncludeLinkHeaderException;
import com.ibm.ws.http2.test.exceptions.ReceivedFrameAfterEndOfStream;
import com.ibm.ws.http2.test.exceptions.ReceivedHeadersFrameAfterEndOfHeaders;
import com.ibm.ws.http2.test.exceptions.ReceivedUnexpectedGoAwayExcetion;
import com.ibm.ws.http2.test.frames.FramePushPromiseClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;
import com.ibm.ws.http2.test.listeners.FramesListener;

public class H2StreamResultManager {

    /*
     * hashtable to store the H2StreamResult objects
     */
    private final ConcurrentHashMap<Integer, H2StreamResult> streamHashtable;
    private final ConcurrentHashMap<FramePushPromiseClient, H2StreamResult> pushPromiseH2StreamResults;

    private FramesListener framesListener;
    private H2Connection h2Connection;

    private boolean receivedExpectedGoAway = false;

    private static final String CLASS_NAME = H2StreamResultManager.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public H2StreamResultManager() {
        this.streamHashtable = new ConcurrentHashMap<Integer, H2StreamResult>();
        this.pushPromiseH2StreamResults = new ConcurrentHashMap<FramePushPromiseClient, H2StreamResult>();
    }

    public H2StreamResultManager(H2Connection h2Connection) {
        this();
        this.h2Connection = h2Connection;

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "constructor", "Adding connection " + h2Connection + " to result manager: " + this);
    }

    /*
     * Add the actual response frame to the H2StreamResult object.
     */
    public int addResponseFrame(Frame frame) throws CompressionException, IOException, ReceivedFrameAfterEndOfStream, ReceivedHeadersFrameAfterEndOfHeaders, ReceivedUnexpectedGoAwayExcetion {

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addResponseFrame", "H2StreamResultmanager.addResponseFrame: entry (object: " + this + ")");
        frame = processFrame(frame, false, false);
        framesListener.receivedFrame(frame);

        //enter here if we are not expecting the goaway frame
        if (frame.getFrameType() == FrameTypes.GOAWAY) {
            if (!isGoAwayExpected(frame)) {
                //If we get a GoAway with an unexpected error then throw an exception
                if (((FrameGoAway) frame).getErrorCode() > 0)
                    throw new ReceivedUnexpectedGoAwayExcetion("The following GoAway frame was not expected and has an error message: " + frame);

                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "addResponseFrame", "Calling listener's receivedFrameGoAway()");
                framesListener.receivedFrameGoAway();
                return 0;
            } else { //it is an expected GoAway, so start finishing test
                receivedExpectedGoAway = true;
            }
        } else if (frame.getFrameType() == FrameTypes.PUSH_PROMISE) {
            //this will update the H2StreamResult to have the right streamID!
            H2StreamResult pushPromisedStreamResults = pushPromiseH2StreamResults.get(frame);
            if (pushPromisedStreamResults != null) {
                int promisedStreamId = ((FramePushPromiseClient) frame).getPromisedStreamId();
                pushPromisedStreamResults.setStreamId(promisedStreamId);
                //add the updated pushPromisedStreamResults to our list now that we have info about the promised stream id
                streamHashtable.put(promisedStreamId, pushPromisedStreamResults);
            }
        }

        H2StreamResult streamResult = getStreamResult(frame);

        streamResult.addActualRespone(frame);

        if (receivedExpectedGoAway) {
            //calling this here to make sure we add the expected GoAway to the streamResult so we can compare correctly
            framesListener.receivedFrameGoAway();
        } else if (receivedAllFrames()) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.logp(Level.INFO, CLASS_NAME, "addResponseFrame", "Calling listener's receivedLastFrame " + "sendGoAway: " + receivedExpectedGoAway);
            //if we got an expected GoAway frame, we don't need to send a GoAway frame automatically.
            framesListener.receivedLastFrame(!receivedExpectedGoAway);
        }

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addResponseFrame", "H2StreamResultmanager.addResponseFrame: exit");

        return 0;
    }

    /*
     * Add the all expected response frames to the H2StreamResult object.
     * Does not work well if you add a PushPromise frame
     */
    public int addExpectedFrames(ArrayList<Frame> frames) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedFrames", "H2StreamResultManager.addExpectedFrames: entry (object: " + this + ")");

        Frame frame = null;

        for (int i = 0; i < frames.size(); i++) {

            frame = processFrame(frames.get(i), false, true);
            addExpectedFrame(frame);
        }

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedFrames", "H2StreamResultManager.addExpectedFrames: exit");

        return 0;
    }

    /**
     * Add the expected response frame to the H2StreamResult object.
     *
     * @param frame
     * @return H2StreamResult if the added frame is a push promise. Otherwise, null.
     * @throws CompressionException
     * @throws IOException
     * @throws ExpectedPushPromiseDoesNotIncludeLinkHeaderException
     */
    public H2StreamResult addExpectedFrame(Frame frame) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedFrame", "H2StreamResultManager.addExpectedFrame: entry (object: " + this + ")");

        if (frame.getFrameType() == FrameTypes.PUSH_PROMISE)
            return addExpectedPushPromiseFrame((FramePushPromiseClient) frame);
        else if (frame.getFrameType() == FrameTypes.GOAWAY) {
            //If an expected GoAway, it should contain the last stream ID to be processed.
            //We are using this info to allow such last stream ID to not contain an EoS or RST_Stream
            getStreamResult(((FrameGoAway) frame).getLastStreamId()).setStreamClosureRequired(false);
        }

        frame = processFrame(frame, false, true);

        H2StreamResult streamResult = getStreamResult(frame);

        streamResult.addExpectedResponse(frame);

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedFrame", "H2StreamResultManager.addExpectedFrame: exit");

        return null;
    }

    /**
     */
    public H2StreamResult addExpectedFrame(FrameTypes type, int stream) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedFrame(FrameTypes, int)", "H2StreamResultManager.addExpectedFrame: entry (object: " + this + ")");

        H2StreamResult streamResult = getStreamResult(stream);

        streamResult.addExpectedResponse(type);

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedFrame", "H2StreamResultManager.addExpectedFrame: exit");

        return null;
    }

    /**
     * Add the expected response frame to the H2StreamResult object.
     *
     * @param frame
     * @return A H2StreamResult object to add expected frames for this push promise frame
     * @throws CompressionException
     * @throws IOException
     * @throws ExpectedPushPromiseDoesNotIncludeLinkHeaderException
     */
    private H2StreamResult addExpectedPushPromiseFrame(FramePushPromiseClient frame) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedPushPromiseFrame", "H2StreamResultManager.addExpectedPushPromiseFrame: entry (object: " + this + ")");
        boolean linkHeaderFound = false;
        for (HeaderEntry headerEntry : frame.getHeaderEntries()) {
            if (headerEntry.getH2HeaderField().getName().equalsIgnoreCase("link"))
                linkHeaderFound = true;
        }
        if (linkHeaderFound)
            throw new ExpectedPushPromiseDoesNotIncludeLinkHeaderException("Frame: " + frame);

        frame = (FramePushPromiseClient) processFrame(frame, false, true);

        H2StreamResult streamResult = getStreamResult(frame);

        streamResult.addExpectedResponse(frame);

        H2StreamResult pushPromiseStreamResult = new H2StreamResult();

        pushPromiseH2StreamResults.put(frame, pushPromiseStreamResult);

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "addExpectedPushPromiseFrame", "H2StreamResultManager.addExpectedPushPromiseFrame: exit");

        return pushPromiseStreamResult;
    }

    /*
     * getStreamResult first tries to lookup the stream in the hashtable.
     * If found, the H2StreamResult object is returned.
     * If not found, one is created.
     *
     */
    private H2StreamResult getStreamResult(Frame frame) {
        return getStreamResult(frame.getStreamId());
    }

    /*
     * getStreamResult first tries to lookup the stream in the hashtable.
     * If found, the H2StreamResult object is returned.
     * If not found, one is created.
     *
     */
    private H2StreamResult getStreamResult(Integer streamId) {

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "getStreamResult", "H2StreamResultmanager.getStreamResult: entry (object: " + this + ")");

        /*
         * If this is a new stream, create a new H2streamResult and put it
         * in the hashtable.
         */
        H2StreamResult streamResult = streamHashtable.get(streamId);

        if (streamResult == null) {
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.logp(Level.INFO, CLASS_NAME, "getStreamResult", "Create new streamHashtable entry for id: " + streamId);
            streamResult = new H2StreamResult(streamId);
            streamHashtable.put(streamId, streamResult);
        }

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "getStreamResult", "H2StreamResultmanager.getStreamResult: exit");

        return streamResult;

    }

    /*
     * lookupStreamResult returns the H2StreamResult object that matches
     * the streamId.
     * It may return null if no object exists.
     *
     */
    private H2StreamResult lookupStreamResult(Integer streamId) {
        /*
         * If this is a new stream, create a new H2streamResult and put it
         * in the hashtable.
         */
        H2StreamResult streamResult = streamHashtable.get(streamId);
        return streamResult;
    }

    public List<Exception> compareStreamResult(Integer streamId) {

        H2StreamResult streamResult = lookupStreamResult(streamId);

        if (streamResult == null) {
            throw new NullPointerException("The H2StreamResult object with streamId " + streamId + " does not exist.");
        } else {
            return (streamResult.checkResult());
        }

    }

    public boolean receivedAllFrames() {
        if (h2Connection.getWaitingForACK().get()) {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAllFrames", "All frames not received: still waiting for SETTINGS frame with ACK set");
            return false;
        }
        Set<Integer> streamIds = streamHashtable.keySet();
        for (Integer streamId : streamIds) {
            if (lookupStreamResult(streamId).getStreamId() != 0 && !lookupStreamResult(streamId).receivedEndOfStreamOrRstStream()
                && !lookupStreamResult(streamId).isContinuationExpected()) {

                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAllFrames", "StreamID: " + lookupStreamResult(streamId).getStreamId() + " has not finished.");
                return false;
            }
            //this is to process all the frame in stream id 0. These don't set end of stream flag nor RST_STREAM
            else if (lookupStreamResult(streamId).getStreamId() == 0) {
                if (!lookupStreamResult(streamId).receivedAtLeastExpectedNumberOfFrames()) {
                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAllFrames", "StreamID: " + lookupStreamResult(streamId).getStreamId() + " has not finished.");
                    return false;
                } else if (lookupStreamResult(streamId).isgoawayExpected() && !lookupStreamResult(streamId).goawayReceived()) {
                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAllFrames", "StreamID: " + lookupStreamResult(streamId).getStreamId() + " has not received goaway.");
                    return false;
                }
            } else if (Http2Client.lockWaitFor.get()) {
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAllFrames", "Still waiting on frames for Http2Client. Test has not finished");
                return false;
            }
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "receivedAllFrames", "StreamID: " + lookupStreamResult(streamId).getStreamId() + " has finished.");

        }

        return true;
    }

    public List<Exception> compareAllStreamResults() {

        List<Exception> exceptionsOnAllStreams = new ArrayList<Exception>();

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.logp(Level.INFO, CLASS_NAME,
                        "compareAllStreamResults", "-*-*-*-*-*-*-*-*-*-*-*- Compare Results EyeCatcher *-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");

        /*
         * Loop through all the streams and compare the expected to actual results.
         * Store up any IOExceptions for each stream, and issue one new IOException at the end.
         */
        Set<Integer> streamIds = streamHashtable.keySet();
        for (Integer streamId : streamIds) {
            exceptionsOnAllStreams.addAll(compareStreamResult(streamId));
        }

        return exceptionsOnAllStreams;
    }

    /**
     *
     * @param goAwayFrame check if this frame is expected in stream 0
     * @return
     */
    public boolean isGoAwayExpected(Frame goAwayFrame) {

        int streamID = 0;
        H2StreamResult streamResult = lookupStreamResult(streamID);

        if (streamResult == null)
            throw new NullPointerException("The H2StreamResult object with streamId " + streamID + " does not exist.");

        return streamResult.isExpectedFrameType(goAwayFrame);

    }

    /**
     *
     * @param expectedFrame check if this frame has arrived
     * @return
     */
    public boolean didframeArrive(Frame expectedFrame) {
        H2StreamResult streamResult = lookupStreamResult(expectedFrame.getStreamId());
        if (streamResult != null && streamResult.didFrameArrive(expectedFrame))
            return true;
        //}

        return false;
    }

    public Frame processFrame(Frame frame, boolean modifyConnectionIfNeeded, boolean isExpected) throws CompressionException, IOException {
        if (modifyConnectionIfNeeded && frame.getFrameType() == FrameTypes.SETTINGS) {
            //sent a Settings Frame
            framesListener.sentSettingsFrame();
            return frame;
        } else if (frame.getFrameType() == FrameTypes.SETTINGS && frame.flagAckSet()) {
            //received a Settings ACK Frame
            framesListener.receivedSettingsAckFrame();
        }

        int streamId = frame.getStreamId();
        if (this.streamHashtable.get(streamId) == null || getStreamResult(frame.getStreamId()).shouldConvertFrame()) {
            return h2Connection.frameConverter(frame, isExpected);
        } else {
            return frame;
        }
    }

    public void setFramesListener(FramesListener framesListener) {
        this.framesListener = framesListener;
    }
}