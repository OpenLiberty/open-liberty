/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
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
package com.ibm.ws.http.channel.h2internal.frames;

import com.ibm.ws.http.channel.h2internal.FrameTypes;

/**
 *
 */
public class FramePPHeaders extends FrameHeaders {
    /**
     * Read frame constructor for pushed headers
     */
    public FramePPHeaders(int streamId, byte[] headerBlockFragment) {
        super(streamId, 0, (byte) 0x00, false, FrameDirection.READ);

        if (headerBlockFragment != null) {
            payloadLength += headerBlockFragment.length;
        }
        this.headerBlockFragment = headerBlockFragment;

        this.END_STREAM_FLAG = false;
        this.END_HEADERS_FLAG = true;

        frameType = FrameTypes.PUSHPROMISEHEADERS;
        setInitialized();

    }

}
