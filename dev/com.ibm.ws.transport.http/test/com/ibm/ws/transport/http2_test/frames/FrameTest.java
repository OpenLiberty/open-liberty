/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.transport.http2_test.frames;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameContinuation;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FramePing;
import com.ibm.ws.http.channel.h2internal.frames.FramePriority;
import com.ibm.ws.http.channel.h2internal.frames.FramePushPromise;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;

import test.common.SharedOutputManager;

/**
 *
 */
public class FrameTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @After
    public void tearDown() {
        //outputMgr.copySystemStreams();
    }

    int byteSize = 1;
    int frameHeaderSize = Frame.SIZE_FRAME_BEFORE_PAYLOAD;

    private void verifyBaseFrame(Frame frame, int streamId, int payloadLength, byte[] frameBytes) {
        Assert.assertTrue("streamID not set correctly: " + frame.getStreamId() + " != " + streamId, frame.getStreamId() == streamId);
        Assert.assertTrue("Payload length is not correct", frame.getPayloadLength() == payloadLength);
        Assert.assertTrue("buildFrameForWrite() size is not correct",
                          frameBytes.length == frameHeaderSize + frame.getPayloadLength());
        Assert.assertTrue("frame was not initialized", frame.getInitialized());
        Exception exception = null;
        try {
            frame.validate(new H2ConnectionSettings());
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertTrue("frame did not validate(): " + exception, exception == null);

    }

    /**
     * Check expected frame flag values versus their actual values
     */
    private void verifyFlags(Frame frame, boolean endStream, boolean padded, boolean isAck, boolean priority, boolean endHeaders) {
        Assert.assertTrue("endStream flag was not set correctly", frame.flagEndStreamSet() == endStream);
        Assert.assertTrue("padded flag was not set correctly", frame.flagPaddingSet() == padded);
        Assert.assertTrue("ACK flag was not set correctly", frame.flagAckSet() == isAck);
        Assert.assertTrue("priority flag was not set correctly", frame.flagPrioritySet() == priority);
        Assert.assertTrue("endHeaders flag was not set correctly", frame.flagEndHeadersSet() == endHeaders);
    }

    @Test
    public void testFrameDummyRemoveLater() {
        Assert.assertTrue(true);
    }

    @Test
    public void testSettings() {
        int streamId = 0;
        boolean reserveBit = false;
        int headerTableSize = 20;
        int enablePush = 0;
        int maxConcurrentStreams = 2;
        int initialWindowSize = 23;
        int maxFrameSize = 16385;
        int maxHeaderListSize = 50;
        int settingPayloadSize = 6;
        int expectedPayloadSize = (settingPayloadSize * 6) * byteSize;

        FrameSettings frame = new FrameSettings(streamId, headerTableSize, enablePush, maxConcurrentStreams, initialWindowSize, maxFrameSize, maxHeaderListSize, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();
        H2ConnectionSettings settings = new H2ConnectionSettings();
        settings.updateSettings(frame);

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, false, false, false, false, false);

        // Verify that all of the settings were set up properly
        Assert.assertTrue("headerTableSize setting was not set correctly", settings.headerTableSize == headerTableSize);
        Assert.assertTrue("enablePushsetting was not set correctly", settings.enablePush == enablePush);
        Assert.assertTrue("maxConcurrentStreams setting was not set correctly", settings.maxConcurrentStreams == maxConcurrentStreams);
        Assert.assertTrue("initialWindowSize setting was not set correctly", settings.initialWindowSize == initialWindowSize);
        Assert.assertTrue("maxFrameSize setting was not set correctly", settings.maxFrameSize == maxFrameSize);
        Assert.assertTrue("maxHeaderListSize setting was not set correctly", settings.maxHeaderListSize == maxHeaderListSize);

        // verify empty ACK settings frame
        FrameSettings ackFrame = new FrameSettings(streamId, -1, -1, -1, -1, -1, -1, false);
        expectedPayloadSize = 0;
        frameBytes = ackFrame.buildFrameForWrite();
        ackFrame.setAckFlag();
        verifyBaseFrame(ackFrame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(ackFrame, false, false, true, false, false);
    }

    @Test
    public void testHeaders() {
        String testHeader = "test data";
        int streamId = 13;
        byte[] headerBlockFragment = testHeader.getBytes();
        int paddingLength = 1;
        boolean endStream = true;
        boolean padded = true;
        boolean reserveBit = false;
        int streamDependency = -1;
        int weight = 1;
        boolean priority = false;
        boolean exclusive = false;
        boolean endHeaders = true;
        int paddingFieldLength = 1;
        int expectedPayloadSize = (paddingFieldLength + paddingLength + headerBlockFragment.length) * byteSize;

        FrameHeaders frame = new FrameHeaders(streamId, headerBlockFragment, streamDependency, paddingLength, weight, endStream, endHeaders, padded, priority, exclusive, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();
        byte[] headerBytes = frame.getHeaderBlockFragment();

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, endStream, padded, false, priority, endHeaders);

        Assert.assertTrue("Header fragment bytes are not set correctly", headerBlockFragment.equals(headerBytes));
        Assert.assertTrue("Padding flag is not set", frame.flagPaddingSet());
        Assert.assertTrue("Padding length is not set correctly", frame.getPaddingLength() == 1);
    }

    @Test
    public void testData() {
        String testString = "test data";
        int streamId = 11;
        byte[] data = testString.getBytes();
        int paddingLength = 1;
        boolean endStream = true;
        boolean padded = true;
        boolean reserveBit = false;
        int paddingFieldLength = 1;
        int expectedPayloadSize = (paddingFieldLength + paddingLength + data.length) * byteSize;

        FrameData frame = new FrameData(streamId, data, paddingLength, endStream, padded, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();
        byte[] dataBytes = frame.getData();

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, endStream, padded, false, false, false);

        Assert.assertTrue("Data payload bytes are not set correctly", data.equals(dataBytes));
        Assert.assertTrue("Padding flag is not set", frame.flagPaddingSet());
        Assert.assertTrue("Padding length is not set correctly", frame.getPaddingLength() == 1);
        Assert.assertTrue("Payload length is not correct", frame.getPayloadLength() == (paddingFieldLength + paddingLength + data.length));
    }

    @Test
    public void testGoAway() {
        String debugString = "debug data";
        int streamId = 0;
        byte[] debugData = debugString.getBytes();
        int errorCode = 9001;
        int lastStreamId = 1;
        boolean reserveBit = false;
        int expectedPayloadSize = (8 + debugData.length) * byteSize;

        FrameGoAway frame = new FrameGoAway(streamId, debugData, errorCode, lastStreamId, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();
        byte[] debugBytes = frame.getDebugData();

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, false, false, false, false, false);

        Assert.assertTrue("Debug data bytes were not set", debugBytes.equals(debugData));
        Assert.assertTrue("Last stream ID was not set correctly", frame.getLastStreamId() == lastStreamId);
        Assert.assertTrue("Error code was not set correctly", frame.getErrorCode() == (9001));

        // try to create a GOAWAY frame with a non-zero ID: it should fail to validate
        int invalidStreamId = 1;
        frame = new FrameGoAway(invalidStreamId, debugData, errorCode, lastStreamId, reserveBit);
        boolean exception = false;
        try {
            frame.validate(new H2ConnectionSettings());
        } catch (Http2Exception e) {
            exception = true;
        }
        Assert.assertTrue("The frame's stream ID is invalid, however validation passed", exception);
    }

    @Test
    public void testRstStream() {
        int streamId = 19;
        boolean reserveBit = false;
        int errorCode = 9001;
        int expectedPayloadSize = 4 * byteSize;

        FrameRstStream frame = new FrameRstStream(streamId, errorCode, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, false, false, false, false, false);

        Assert.assertTrue("Error code not set correctly", frame.getErrorCode() == errorCode);

    }

    @Test
    public void testPushPromise() {
        String testHeader = "test data";
        int streamId = 15;
        byte[] headerBlockFragment = testHeader.getBytes();
        int paddingLength = 3;
        int promisedStreamId = 17;
        boolean padded = true;
        boolean reserveBit = false;
        boolean priority = false;
        boolean endHeaders = true;
        int paddingFieldLength = 1;
        int expectedPayloadSize = (paddingFieldLength + 4 + paddingLength + headerBlockFragment.length) * byteSize;

        FramePushPromise frame = new FramePushPromise(streamId, headerBlockFragment, promisedStreamId, paddingLength, endHeaders, padded, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();
        byte[] headerBytes = frame.getHeaderBlockFragment();

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, false, padded, false, priority, endHeaders);
        Assert.assertTrue("Header fragment bytes are not set correctly", headerBlockFragment.equals(headerBytes));
        Assert.assertTrue("Padding flag is not set", frame.flagPaddingSet());
        Assert.assertTrue("Padding length is not set correctly", frame.getPaddingLength() == paddingLength);
        Assert.assertTrue("Promised stream not set correctly:" + frame.getPromisedStreamId(), frame.getPromisedStreamId() == promisedStreamId);

    }

    @Test
    public void testPriority() {
        int streamId = 13;
        boolean reserveBit = false;
        int streamDependency = 11;
        int weight = 9000;
        boolean exclusive = false;
        int expectedPayloadSize = 5 * byteSize;

        FramePriority frame = new FramePriority(streamId, streamDependency, weight, exclusive, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, false, false, false, false, false);

        Assert.assertTrue("Stream dependency not set correctly", frame.getStreamDependency() == streamDependency);
        Assert.assertTrue("Weight not set correctly", frame.getWeight() == weight);

    }

    @Test
    public void testPing() {
        int streamId = 0;
        boolean reserveBit = false;
        byte[] payload = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        FramePing frame = new FramePing(streamId, payload, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();

        verifyBaseFrame(frame, streamId, payload.length, frameBytes);
        verifyFlags(frame, false, false, false, false, false);
        Assert.assertTrue("Ping payload response was not correct", payload.equals(frame.getPayload()));

    }

    @Test
    public void testContinuaton() {
        String testHeader = "test data";
        int streamId = 45;
        byte[] headerBlockFragment = testHeader.getBytes();
        boolean endStream = true;
        boolean reserveBit = false;
        boolean endHeaders = true;
        int expectedPayloadSize = (headerBlockFragment.length) * byteSize;

        FrameContinuation frame = new FrameContinuation(streamId, headerBlockFragment, endHeaders, endStream, reserveBit);
        byte[] frameBytes = frame.buildFrameForWrite();
        byte[] headerBytes = frame.getHeaderBlockFragment();

        verifyBaseFrame(frame, streamId, expectedPayloadSize, frameBytes);
        verifyFlags(frame, endStream, false, false, false, endHeaders);

        Assert.assertTrue("Header fragment bytes are not set correctly", headerBlockFragment.equals(headerBytes));
    }
}
