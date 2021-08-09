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
package com.ibm.ws.http2.test;

import java.util.ArrayList;

import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;

public class H2Stream {

    private final int requestID, streamID;
    private ArrayList<Frame> request, response;

    public H2Stream(int requestID) {

        this.requestID = requestID;
        streamID = H2ClientStreamHelper.nextStreamID();

    }

    public H2Stream(FrameHeaders headersFrame) {

        this.requestID = -1;
        this.streamID = headersFrame.getStreamId();
        response = new ArrayList<Frame>();
        response.add(headersFrame);

    }

    public ArrayList<Frame> getRequestFrames() {
        return this.request;
    }

    public ArrayList<Frame> getResponseFrames() {
        return this.response;
    }

    public void addRequestFrame(Frame frame) {
        request.add(frame);
    }

    public void addResponseFrame(Frame frame) {
        response.add(frame);
    }

    /**
     * @return
     */
    public ArrayList<Frame> getExpectedResponse() {
        return null;
    }

}
