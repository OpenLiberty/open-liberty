/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.encoding;

import java.io.ByteArrayOutputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.common.TraceConstants;

public class EncodingUtils {

    public static final TraceComponent tc = Tr.register(EncodingUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public EncodingUtils() {
    }

    @Trivial
    public String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int val = bytes[i];
            if (val < 0) {
                val += 256;
            }
            if (val < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(val));
        }
        return sb.toString();
    }

    @Trivial
    public byte[] hexStringToBytes(String string) throws NumberFormatException {
        if (string == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = 0;
        while (i < string.length() - 1) {
            baos.write(Integer.parseInt(string.substring(i, i + 2), 16));
            i = i + 2;
        }
        return baos.toByteArray();
    }

}
