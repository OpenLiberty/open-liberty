/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization.internal;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.serialization.TestUtil;

public class DeserializationObjectInputStreamImplTest {
    @Test
    public void testLoadClass() throws Exception {
        for (int i = 0; i < 2; i++) {
            final boolean[] loadClass = new boolean[1];
            ClassLoader cl = new ClassLoader() {
                @Override
                public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    loadClass[0] = true;
                    throw new ClassNotFoundException(name);
                }
            };

            final boolean[] contextLoadClassShouldThrow = new boolean[] { i == 0 };
            final boolean[] contextLoadClass = new boolean[1];
            DeserializationContextImpl context = new DeserializationContextImpl(new SerializationServiceImpl()) {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    contextLoadClass[0] = true;
                    if (contextLoadClassShouldThrow[0]) {
                        throw new ClassNotFoundException(name);
                    }
                    return null;
                }
            };

            byte[] bytes = TestUtil.serialize(new TestSerializable());

            ObjectInputStream ois = new DeserializationObjectInputStreamImpl(new ByteArrayInputStream(bytes), cl, context);
            try {
                ois.readObject();
                Assert.fail(i + ": ClassLoader.loadClass should have failed");
            } catch (ClassNotFoundException e) {
            }

            Assert.assertTrue(i + ": context.loadClass should be called", contextLoadClass[0]);
            if (contextLoadClassShouldThrow[0]) {
                Assert.assertFalse(i + ": classLoader.loadClass should not be called", loadClass[0]);
            } else {
                Assert.assertTrue(i + ": classLoader.loadClass should be called", loadClass[0]);
            }
        }
    }

    @SuppressWarnings("serial")
    private static class TestSerializable implements Serializable {}

    @Test
    public void testResolveObject() throws Exception {
        final Object[] resolve = new Object[1];
        DeserializationContextImpl context = new DeserializationContextImpl(new SerializationServiceImpl()) {
            @Override
            public boolean isResolveObjectNeeded() {
                return resolve[0] != null;
            }

            @Override
            public Object resolveObject(Object object) {
                if (!isResolveObjectNeeded()) {
                    throw new UnsupportedOperationException();
                }
                return resolve[0];
            }
        };
        ClassLoader cl = getClass().getClassLoader();

        byte[] bytes = TestUtil.serialize(0);
        resolve[0] = null;
        ObjectInputStream ois = new DeserializationObjectInputStreamImpl(new ByteArrayInputStream(bytes), cl, context);
        Assert.assertEquals(0, ois.readObject());

        resolve[0] = 1;
        ois = new DeserializationObjectInputStreamImpl(new ByteArrayInputStream(bytes), cl, context);
        Assert.assertEquals(1, ois.readObject());
    }
}
