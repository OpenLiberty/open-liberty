/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.frames;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;

/**
 * Frame class representing a frame that has frame type not defined in the http/2 spec
 */
public class FrameUnknown extends Frame {

    /**
     * Write frame constructor
     */
    public FrameUnknown(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.UNKNOWN;
    }

    /**
     * Write frame constructor
     */
    public FrameUnknown(int streamId, byte[] payload, boolean reserveBit) {
        super(streamId, 8, (byte) 0x00, reserveBit, FrameDirection.WRITE);
        frameType = FrameTypes.UNKNOWN;
        setInitialized();
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        setFlags();
    }

    @Override
    public byte[] buildFrameForWrite() {

        byte[] frame = super.buildFrameForWrite();
        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_UNKNOWN);
        return frame;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException, FrameSizeException {
        // no op
    }

    @Override
    protected void setFlags() {
        // no op
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FrameUnknown)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Object is not a FrameUnknown");
            }
            return false;
        }
        FrameUnknown frameUnknownToCompare = (FrameUnknown) object;

        if (!super.equals(frameUnknownToCompare))
            return false;

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();
        frameToString.append(super.toString());
        return frameToString.toString();

    }
}
