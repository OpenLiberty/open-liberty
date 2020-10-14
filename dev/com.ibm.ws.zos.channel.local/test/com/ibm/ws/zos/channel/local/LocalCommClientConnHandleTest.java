/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

/**
 *
 */
public class LocalCommClientConnHandleTest {

    @Test
    public void test() {

        byte[] rawData = new byte[] { 0x00, 0x01, 0x02, 0x03,
                                      0x04, 0x05, 0x06, 0x07,
                                      0x08, 0x09, 0x0a, 0x0b,
                                      0x0c, 0x0d, 0x0e, 0x0f };

        LocalCommClientConnHandle handle = new LocalCommClientConnHandle(rawData);

        assertArrayEquals(rawData, handle.getBytes());
        assertNotSame(rawData, handle.getBytes());
        assertEquals(0x0001020304050607L, handle.getLhdlPtr());
        assertEquals(0x08090a0b, handle.getInstanceCount());

        assertEquals(handle.hashCode(), handle.hashCode());
        assertEquals(handle, new LocalCommClientConnHandle(rawData));
    }

}
