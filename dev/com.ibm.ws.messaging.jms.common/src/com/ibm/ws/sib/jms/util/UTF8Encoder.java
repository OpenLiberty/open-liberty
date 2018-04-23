/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.jms.util;

public enum UTF8Encoder {
    ;

    public static int getEncodedLength(String s) {
        return Utf8Codec.getEncodedLength(s);
    }

    public static int encode(byte[] buff, int offset, String s) {
        return Utf8Codec.encode(buff, offset, s);
    }

    public static byte[] encode(String s) {
        return Utf8Codec.encode(s);
    }
}

