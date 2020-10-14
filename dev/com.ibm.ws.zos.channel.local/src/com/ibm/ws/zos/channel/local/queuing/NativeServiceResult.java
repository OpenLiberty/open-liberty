/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Encapsulates the return codes and reason codes for a local comm channel
 * native method call.
 * 
 */
public class NativeServiceResult {
    
    private static final int RAW_BYTES_LENGTH = 20;
    
    /**
     * The raw byte data containing the rc/rsn codes.  This byte array is passed
     * as an output parm to the native method.  The native method populates the byte array
     * with the rc/rsn codes.
     */
    private byte[] rawBytes = new byte[RAW_BYTES_LENGTH];
    
    /**
     * Indicates whether we've yet parsed the rc/rsn codes from the rawBytes buffer.
     */
    private boolean haveParsedErrorCodes = false;
    
    /**
     * The native method's return code.
     */
    private int wasReturnCode = 0;
    
    /**
     * Native reason code
     */
    private int wasReasonCode = 0;
    
    /**
     * Native extended reason code
     */
    private int wasExtendedReasonCode = 0;
    
    /**
     * Save the return/reason codes.
     */
    private void parseErrorCodes() {
        if (!haveParsedErrorCodes) {
            IntBuffer ibuff = ByteBuffer.wrap(getBytes()).asIntBuffer();

            wasReturnCode         = ibuff.get();
            wasReasonCode         = ibuff.get();
            wasExtendedReasonCode = ibuff.get();
            
            haveParsedErrorCodes = true;
        }
    }
    
    /**
     * @return The native method's return code.
     */
    public int getReturnCode() {
        parseErrorCodes();
        return wasReturnCode;
    }
    
    /**
     * @return The raw byte buffer. The buffer is passed as an output parm on the native method call.
     */
    public byte[] getBytes() {
        return rawBytes;
    }

    /**
     * Simple diagnostic aid.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        // Note: Can't drive parseErrorCodes() here because it could force an early parse which
        // if set from native code, would avoid a re-parse to get updated values.
        IntBuffer ibuff = ByteBuffer.wrap(getBytes()).asIntBuffer();

        wasReturnCode         = ibuff.get();
        wasReasonCode         = ibuff.get();
        wasExtendedReasonCode = ibuff.get();

        sb.append("Local Comm returnCode=").append(this.wasReturnCode);
        sb.append(", Local Comm reasonCode=").append(this.wasReasonCode);
        sb.append(", Local Comm extended reasonCode=").append(this.wasExtendedReasonCode);

        return sb.toString();
    }
}
