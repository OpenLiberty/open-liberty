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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 *
 */
public class FrameHeadersClient extends com.ibm.ws.http.channel.h2internal.frames.FrameHeaders implements Cloneable {

    private List<HeaderEntry> headerEntries = new ArrayList<HeaderEntry>();
    private List<H2HeaderField> headerFields = new ArrayList<H2HeaderField>();

    /**
     * @param streamId
     * @param headerBlockFragment
     * @param streamDependency
     * @param paddingLength
     * @param weight
     * @param endStream
     * @param endHeaders
     * @param padded
     * @param priority
     * @param exclusive
     * @param reserveBit
     */
    public FrameHeadersClient(int streamId, byte[] headerBlockFragment, int streamDependency, int paddingLength, int weight, boolean endStream, boolean endHeaders, boolean padded,
                              boolean priority, boolean exclusive, boolean reserveBit) {
        super(streamId, headerBlockFragment, streamDependency, paddingLength, weight, endStream, endHeaders, padded, priority, exclusive, reserveBit);
    }

    public List<HeaderEntry> getHeaderEntries() {
        return headerEntries;
    }

    /**
     * Use this to send headers from the client to the server.
     */
    public void setHeaderEntries(List<HeaderEntry> headerEntries) {
        this.headerEntries = headerEntries;

        //we create a new one every time as we don't want to add the new header fields with the previous ones
        //this method should only be called once, but just in case.
        headerFields = new ArrayList<H2HeaderField>();

        for (HeaderEntry headerEntry : headerEntries)
            headerFields.add(headerEntry.getH2HeaderField());

    }

    /**
     * Use this for comparison in the test framework.
     */
    public void setHeaderFields(List<H2HeaderField> headerFields) {
        this.headerFields = headerFields;
    }

    public List<H2HeaderField> getHeaderFields() {
        return headerFields;
    }

    public void setStreamID(int streamID) {
        this.streamId = streamID;
    }

    @Override
    public boolean equals(Object receivedFrame) {
        if (receivedFrame instanceof FrameHeadersClient) {

            FrameHeadersClient frameToCompare = (FrameHeadersClient) receivedFrame;

            if (this.flagAckSet() != frameToCompare.flagAckSet()) {
                System.out.println("this.flagAckSet() = " + this.flagAckSet() + " frameToCompare.flagAckSet() = " + frameToCompare.flagAckSet());
                return false;
            }
            if (this.flagPrioritySet() != frameToCompare.flagPrioritySet()) {
                System.out.println("this.flagPrioritySet() = " + this.flagPrioritySet() + " frameToCompare.flagPrioritySet() = " + frameToCompare.flagPrioritySet());
                return false;
            }
            if (this.flagEndHeadersSet() != frameToCompare.flagEndHeadersSet()) {
                System.out.println("this.flagEndHeadersSet() = " + this.flagEndHeadersSet() + " frameToCompare.flagEndHeadersSet() = " + frameToCompare.flagEndHeadersSet());
                return false;
            }
            if (this.flagPaddedSet() != frameToCompare.flagPaddedSet()) {
                System.out.println("this.flagPaddedSet() = " + this.flagPaddedSet() + " frameToCompare.flagPaddedSet() = " + frameToCompare.flagPaddedSet());
                return false;
            }
            if (this.getFrameType() != frameToCompare.getFrameType()) {
                System.out.println("getFrameType is false");
                System.out.println("this.getFrameType() = " + this.getFrameType());
                System.out.println("frameToCompare.getFrameType() = " + frameToCompare.getFrameType());
                return false;
            }
            if (this.getFrameReserveBit() != frameToCompare.getFrameReserveBit()) {
                System.out.println("getFrameReserveBit is false");
                return false;
            }
            if (this.getPaddingLength() != frameToCompare.getPaddingLength()) {
                System.out.println("this.getPaddingLength() = " + this.getPaddingLength() + " frameToCompare.getPaddingLength() = " + frameToCompare.getPaddingLength());
                return false;
            }
            if (this.isExclusive() != frameToCompare.isExclusive()) {
                System.out.println("this.isExclusive() = " + this.isExclusive() + " frameToCompare.isExclusive() = " + frameToCompare.isExclusive());
                return false;
            }
            if (this.getStreamDependency() != frameToCompare.getStreamDependency()) {
                System.out.println("this.getStreamDependency() = " + this.getStreamDependency() + " frameToCompare.getStreamDependency() = "
                                   + frameToCompare.getStreamDependency());
                return false;
            }

            if (this.getWeight() != frameToCompare.getWeight()) {
                System.out.println("this.getWeight() = " + this.getWeight() + " frameToCompare.getWeight() = " + frameToCompare.getWeight());
                return false;
            }

            if (this.getStreamId() != frameToCompare.getStreamId()) {
                System.out.println("getStreamId is false");
                return false;
            }

            //we need to check if the expected frames are in receivedFrame
            //we won't compare for extra frame in receivedFrame as some headers might not be expected
            List<H2HeaderField> receivedHeaderFields = frameToCompare.getHeaderFields();

            Supplier<Stream<H2HeaderField>> streamSupplier = () -> receivedHeaderFields.stream();
            for (H2HeaderField headerField : getHeaderFields())
                //Using lambda to simplify check of header values (as we support regex)
                if (!streamSupplier.get().anyMatch(p -> (p.getName().equals(headerField.getName()) && p.getValue().matches(headerField.getValue())))) {
                    System.out.println("headerField mismatch. headerField: " + headerField);
                    return false;
                }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder(super.toString());

        toString.append('\n');
        toString.append("Header fields: ").append('\n');
        for (H2HeaderField headerField : getHeaderFields())
            toString.append(" ").append(headerField).append('\n');

        return toString.toString();
    }

    @Override
    public FrameHeadersClient clone() throws CloneNotSupportedException {
        return (FrameHeadersClient) super.clone();
    }

}
