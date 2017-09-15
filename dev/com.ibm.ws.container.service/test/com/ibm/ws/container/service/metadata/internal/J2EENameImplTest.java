/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import com.ibm.websphere.csi.J2EEName;

public class J2EENameImplTest {

    /**
     * Verifies that the constructor creates a {@link J2EEName} with the correct
     * attributes
     */
    @Test
    public void testConstructor() {
        J2EEName j2n = new J2EENameImpl("appName", "moduleName", "componentName");
        assertEquals("appName", j2n.getApplication());
        assertEquals("moduleName", j2n.getModule());
        assertEquals("componentName", j2n.getComponent());
        assertEquals("appName#moduleName#componentName", j2n.toString());

        j2n = new J2EENameImpl("appName", "moduleName", null);
        assertEquals("appName", j2n.getApplication());
        assertEquals("moduleName", j2n.getModule());
        assertNull(j2n.getComponent());
        assertEquals("appName#moduleName", j2n.toString());

        j2n = new J2EENameImpl("appName", null, null);
        assertEquals("appName", j2n.getApplication());
        assertNull(j2n.getModule());
        assertNull(j2n.getComponent());
        assertEquals("appName", j2n.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorModuleWithoutAppError() {
        new J2EENameImpl(null, "moduleName", "componentName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorCompWithoutAppError() {
        new J2EENameImpl(null, null, "componentName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorCompWithoutModuleError() {
        new J2EENameImpl("appName", null, "componentName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorAppNameError() {
        new J2EENameImpl("#", null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorModuleNameError() {
        new J2EENameImpl("app", "#", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorCompNameError() {
        new J2EENameImpl("app", "module", "#");
    }

    /**
     * Verifies that constructing a {@link J2EENameImpl} from bytes recreates
     * the valid object.
     */
    @Test
    public void testBytes() {
        // Testing app#module#component
        J2EEName j2n1 = new J2EENameImpl("appName", "moduleName", "componentName");
        byte[] bytes = j2n1.getBytes();

        J2EEName j2n2 = new J2EENameImpl(bytes);

        assertTrue(j2n1.equals(j2n2));
        assertEquals(j2n1.hashCode(), j2n2.hashCode());

        j2n1 = new J2EENameImpl("app", "module", "component");
        bytes = j2n1.getBytes();

        assertFalse(j2n1.equals(j2n2));
        assertNotSame(j2n1.hashCode(), j2n2.hashCode());

        // Testing app#module
        j2n1 = new J2EENameImpl("appName", "moduleName", null);
        bytes = j2n1.getBytes();

        j2n2 = new J2EENameImpl(bytes);

        assertTrue(j2n1.equals(j2n2));
        assertEquals(j2n1.hashCode(), j2n2.hashCode());

        j2n1 = new J2EENameImpl("app", "module", null);
        bytes = j2n1.getBytes();

        assertFalse(j2n1.equals(j2n2));
        assertNotSame(j2n1.hashCode(), j2n2.hashCode());

        // Testing app
        j2n1 = new J2EENameImpl("appName", null, null);
        bytes = j2n1.getBytes();

        j2n2 = new J2EENameImpl(bytes);

        assertTrue(j2n1.equals(j2n2));
        assertEquals(j2n1.hashCode(), j2n2.hashCode());

        j2n1 = new J2EENameImpl("app", null, null);
        bytes = j2n1.getBytes();

        assertFalse(j2n1.equals(j2n2));
        assertNotSame(j2n1.hashCode(), j2n2.hashCode());
    }

    /**
     * Verifies that serialization/deserialization of {@link J2EENameImpl} produces the same object.
     */
    @Test
    public void testSerialization() throws Exception {
        // Testing app#module#component
        J2EEName j2nBefore = new J2EENameImpl("appName", "moduleName", "componentName");

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bao);
        oos.writeObject(j2nBefore);

        byte[] bytes = bao.toByteArray();
        ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bai);
        J2EEName j2nAfter = (J2EEName) ois.readObject();

        assertArrayEquals(j2nBefore.getBytes(), j2nAfter.getBytes());
        assertEquals(j2nBefore, j2nAfter);

        // Testing app#module
        j2nBefore = new J2EENameImpl("appName", "moduleName", null);

        bao = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bao);
        oos.writeObject(j2nBefore);

        bytes = bao.toByteArray();
        bai = new ByteArrayInputStream(bytes);
        ois = new ObjectInputStream(bai);
        j2nAfter = (J2EEName) ois.readObject();

        assertArrayEquals(j2nBefore.getBytes(), j2nAfter.getBytes());
        assertEquals(j2nBefore, j2nAfter);

        // Testing app#module
        j2nBefore = new J2EENameImpl("appName", null, null);

        bao = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bao);
        oos.writeObject(j2nBefore);

        bytes = bao.toByteArray();
        bai = new ByteArrayInputStream(bytes);
        ois = new ObjectInputStream(bai);
        j2nAfter = (J2EEName) ois.readObject();

        assertArrayEquals(j2nBefore.getBytes(), j2nAfter.getBytes());
        assertEquals(j2nBefore, j2nAfter);
    }

    /**
     * Verifies that the UTF-8 encoding is working as intended.
     * Note: eclipse sets the file encoding to be UTF-8 by default so the test
     * will always pass from eclipse. This test is best run in a command line.
     */
    @Test
    public void testString() throws Exception {
        J2EEName j2nBefore = new J2EENameImpl("app\u0fffName", null, null);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bao);
        oos.writeObject(j2nBefore);

        byte[] bytes = bao.toByteArray();
        ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bai);
        J2EEName j2nAfter = (J2EEName) ois.readObject();

        assertArrayEquals(j2nBefore.getBytes(), j2nAfter.getBytes());
        assertEquals(j2nBefore, j2nAfter);
    }
}
