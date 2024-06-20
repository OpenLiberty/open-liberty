/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests to make sure the Enum doesn't get broken.
 */
public class NativeWorkRequestTypeTest {

    @Test
    public void verifyEnumValues() {

        assertEquals(NativeWorkRequestType.REQUESTTYPE_CONNECT, NativeWorkRequestType.forNativeValue(1));
        assertEquals(NativeWorkRequestType.REQUESTTYPE_CONNECTRESPONSE, NativeWorkRequestType.forNativeValue(2));
        assertEquals(NativeWorkRequestType.REQUESTTYPE_DISCONNECT, NativeWorkRequestType.forNativeValue(3));
        assertEquals(NativeWorkRequestType.REQUESTTYPE_SEND, NativeWorkRequestType.forNativeValue(4));
        assertEquals(NativeWorkRequestType.REQUESTTYPE_READREADY, NativeWorkRequestType.forNativeValue(5));
    }

    @Test
    public void verifyForNativeValue() {
        for (NativeWorkRequestType type : NativeWorkRequestType.class.getEnumConstants()) {
            assertEquals(type, NativeWorkRequestType.forNativeValue(type.getNativeValue()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidZeroValue() {
        NativeWorkRequestType.forNativeValue(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidValueTooHigh() {
        NativeWorkRequestType.forNativeValue(7);
    }
}
