/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

/**
 * This class holds 'leftover' data that the WOLAMessageParser could not process. It also
 * holds extra information about the portion of the WOLA message that was left over. For
 * example, if the first buffer contains the WOLA message header, and we already verified the
 * eye catcher, we'll remember that here.
 *
 * This class is here primarily for performance reasons. The WOLA message parser will reject
 * an incomplete message and cause another read to be performed. After the read completes, the
 * message parser starts over. This may happen two or three (or four) times.
 *
 * This class assumes that the ByteBuffers contained in this class will not be modified outside
 * of this class. Specifically, if you add a byte buffer, the amount of available data
 * should not change.
 */
public class WOLAMessageLeftovers {
    /**
     * The extra byte buffers. Presumably these contain the first part of a WOLA message.
     */
    private final ByteBufferVector byteBuffers;

    /**
     * Is the eye catcher in the WOLA header correct?
     */
    private boolean isEyeCatcherValid = false;

    /**
     * Is the WOLA message header complete?
     */
    private boolean isMessageHeaderComplete = false;

    // TODO: Decide if we want to keep track of the number of bytes remaining in the byte buffers.

    /**
     * Constructor
     */
    WOLAMessageLeftovers(ByteBufferVector byteBuffers, boolean isEyeCatcherValid, boolean isMessageHeaderComplete) {
        this.byteBuffers = byteBuffers;
        this.isEyeCatcherValid = isEyeCatcherValid;
        this.isMessageHeaderComplete = isMessageHeaderComplete;
    }

    /**
     * Get the byte buffers
     */
    ByteBufferVector getByteBuffers() {
        return byteBuffers;
    }

    /**
     * Get the eye catcher valid flag.
     */
    boolean isEyeCatcherValid() {
        return isEyeCatcherValid;
    }

    /**
     * Get the message header complete flag.
     */
    boolean isMessageHeaderComplete() {
        return isMessageHeaderComplete;
    }
}
