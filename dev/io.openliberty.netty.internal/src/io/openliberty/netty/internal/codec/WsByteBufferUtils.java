/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.codec;

import java.nio.charset.Charset;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class WsByteBufferUtils {

    /**
     * Convert a WsByteBuffer to a string with the provided Charset
     * @param wsbb the WsByteBuffer from which to create a string
     * @param set the Charset to use for the string
     * @return a String representation of the given WsByteBuffer
     */
    private static String convertWsByteBufferToString(WsByteBuffer wsbb, Charset set) {
        if (wsbb.remaining() > 0) {
            if (wsbb.hasArray()) {
                return new String(wsbb.array(), 0, wsbb.array().length, set);
            } else {
                byte[] wsbbArray = new byte[wsbb.remaining()];
                wsbb.mark();
                int position = 0;
                while (wsbb.hasRemaining()) {
                    wsbbArray[position++] = wsbb.get();
                }
                wsbb.reset();
                return new String(wsbbArray, 0, wsbbArray.length, Charset.forName("utf-8"));
            }
        }
        return null;
    }

}
