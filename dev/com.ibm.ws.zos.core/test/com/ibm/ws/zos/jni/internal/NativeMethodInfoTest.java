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
package com.ibm.ws.zos.jni.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NativeMethodInfoTest {

    @Test
    public void testNativeMethodInfoNull() {
        NativeMethodInfo nativeMethodInfo = new NativeMethodInfo(null, null, null, 0L);
        assertNull(nativeMethodInfo.getClazz());
        assertNull(nativeMethodInfo.getNativeDescriptorName());
        assertNull(nativeMethodInfo.getExtraInfo());
        assertEquals(0L, nativeMethodInfo.getDllHandle());
    }

    @Test
    public void testNativeMethodInfoSame() {
        Class<?> clazz = java.io.Serializable.class;
        String nativeDescriptorName = clazz.toString();
        Object[] extraInfo = new Object[] { "Foo", "Bar", Long.valueOf(1) };
        long dllHandle = 987654321L;

        NativeMethodInfo nativeMethodInfo = new NativeMethodInfo(clazz, nativeDescriptorName, extraInfo, dllHandle);
        assertSame(clazz, nativeMethodInfo.getClazz());
        assertSame(nativeDescriptorName, nativeMethodInfo.getNativeDescriptorName());
        assertSame(extraInfo, nativeMethodInfo.getExtraInfo());
        assertEquals(dllHandle, nativeMethodInfo.getDllHandle());
    }

    @Test
    public void testToString() {
        NativeMethodInfo nativeMethodInfo = new NativeMethodInfo(NativeMethodInfoTest.class, "nativeName", new Object[0], 12345678L);
        String string = nativeMethodInfo.toString();

        assertTrue(string, string.contains(".NativeMethodInfo@"));
        assertTrue(string, string.contains("clazz=" + NativeMethodInfoTest.class.toString()));
        assertTrue(string, string.contains("nativeDescriptorName=nativeName"));
        assertTrue(string, string.contains("extraInfo=[]"));
        assertTrue(string, string.contains("dllHandle=12345678"));
    }

}
