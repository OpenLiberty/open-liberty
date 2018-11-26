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
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.createAppClassloader;
import static com.ibm.ws.classloading.internal.TestUtil.getTestJarURL;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.classloading.internal.ClassLoadingServiceImpl.ClassFileTransformerAdapter;
import com.ibm.wsspi.classloading.ClassTransformer;

import test.common.SharedOutputManager;

/**
 * Test to make sure that transformers can be correctly added to/removed from an AppClassLoader
 */
public class TransformerTest {
    @Rule
    public SharedOutputManager outputManager = SharedOutputManager.getInstance();

    @Test
    public void testTransformerRegistration() throws Exception {
        ClassTransformer ct1 = new ClassTransformer() {
            @Override
            public byte[] transformClass(String name, byte[] bytes, CodeSource source, ClassLoader loader) {
                return bytes;
            }
        };
        ClassFileTransformerAdapter transformer1 = new ClassFileTransformerAdapter(ct1);
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader", getTestJarURL(), true);
        assertFalse("Should not be able to remove a transformer before it was even registered", loader.removeTransformer(transformer1));
        assertTrue("Should be able to add new transformer adapter", loader.addTransformer(transformer1));
        assertTrue("Should be able to remove newly added transformer adapter", loader.removeTransformer(transformer1));
        assertFalse("Should not be able to remove newly added transformer adapter twice", loader.removeTransformer(transformer1));
    }

    @Test
    public void testTransformerReturnsNull() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader", getTestJarURL(), true);
        final AtomicBoolean transformerInvoked = new AtomicBoolean(false);
        loader.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                transformerInvoked.set(true);
                return null;
            }
        });
        byte[] originalBytes = "Hello!".getBytes();
        byte[] transformedBytes = loader.transformClassBytes(originalBytes, "hello");

        assertTrue(transformerInvoked.get());
        assertArrayEquals(originalBytes, transformedBytes);
        assertEquals("Hello!", new String(transformedBytes));
    }

    @Test
    public void testTransformerReturnsSameBytes() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader", getTestJarURL(), true);
        final AtomicBoolean transformerInvoked = new AtomicBoolean(false);
        loader.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                transformerInvoked.set(true);
                return classfileBuffer;
            }
        });
        byte[] originalBytes = "Goodbye!".getBytes();
        byte[] transformedBytes = loader.transformClassBytes(originalBytes, "goodbye");

        assertTrue(transformerInvoked.get());
        assertArrayEquals(originalBytes, transformedBytes);
        assertEquals("Goodbye!", new String(transformedBytes));
    }

    @Test
    public void testTransformerReturnsTransformedBytes() throws Exception {
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader", getTestJarURL(), true);
        final AtomicBoolean transformerInvoked = new AtomicBoolean(false);
        loader.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                transformerInvoked.set(true);
                String original = new String(classfileBuffer);
                String transformed = original + " and salutations!";
                return transformed.getBytes();
            }
        });
        byte[] originalBytes = "Greetings".getBytes();
        byte[] transformedBytes = loader.transformClassBytes(originalBytes, "greetings");

        assertTrue(transformerInvoked.get());
        assertFalse(Arrays.equals(originalBytes, transformedBytes));
        assertEquals("Greetings and salutations!", new String(transformedBytes));
    }
}
