/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.classloading.internal.ClassLoadingServiceImpl.ClassFileTransformerAdapter;
import com.ibm.ws.classloading.internal.ContainerClassLoader.ByteResourceInformation;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.classloading.ClassTransformer;

import test.common.SharedOutputManager;

/**
 * Test to make sure that transformers can be correctly added to/removed from an AppClassLoader
 */
@SuppressWarnings("restriction")
public class TransformerTest {

    @Rule
    public SharedOutputManager outputManager = SharedOutputManager.getInstance();

    @After
    public void removeBetaFlag() {
        System.getProperties().remove(ProductInfo.BETA_EDITION_JVM_PROPERTY);
    }

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
        doTestTransformerReturnsNull(false);
    }

    @Test
    public void testSystemTransformerReturnsNull() throws Exception {
        doTestTransformerReturnsNull(true);
    }

    private void doTestTransformerReturnsNull(boolean systemTransformer) throws Exception {
        if (systemTransformer) {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, Boolean.TRUE.toString());
        }

        final AtomicBoolean transformerInvoked = new AtomicBoolean(false);
        ClassFileTransformer transformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                transformerInvoked.set(true);
                return null;
            }
        };
        AppClassLoader loader = createAppClassloaderTransformer(transformer, systemTransformer);

        byte[] originalBytes = "Hello!".getBytes();
        ByteResourceInformation toTransform = new ByteResourceInformation(originalBytes, null, null, null, false, () -> originalBytes);
        byte[] transformedBytes = loader.transformClassBytes("hello", toTransform);

        assertTrue(transformerInvoked.get());
        assertArrayEquals(originalBytes, transformedBytes);
        assertEquals("Hello!", new String(transformedBytes));
    }

    AppClassLoader createAppClassloaderTransformer(ClassFileTransformer transformer, boolean systemTransformer) throws Exception {
        List<ClassFileTransformer> systemTransformers = systemTransformer ? Arrays.asList(transformer) : Collections.emptyList();
        AppClassLoader loader = createAppClassloader(this.getClass().getName() + ".jar-loader", getTestJarURL(), true, GetLibraryAction.NO_LIBS, systemTransformers);
        if (!systemTransformer) {
            loader.addTransformer(transformer);
        }
        return loader;
    }

    @Test
    public void testTransformerReturnsSameBytes() throws Exception {
        doTestTransformerReturnsSameBytes(false);
    }

    @Test
    public void testSystemTransformerReturnsSameBytes() throws Exception {
        doTestTransformerReturnsSameBytes(false);
    }

    private void doTestTransformerReturnsSameBytes(boolean systemTransformer) throws Exception {
        if (systemTransformer) {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, Boolean.TRUE.toString());
        }

        final AtomicBoolean transformerInvoked = new AtomicBoolean(false);
        ClassFileTransformer transformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                transformerInvoked.set(true);
                return classfileBuffer;
            }
        };

        AppClassLoader loader = createAppClassloaderTransformer(transformer, systemTransformer);

        byte[] originalBytes = "Goodbye!".getBytes();
        ByteResourceInformation toTransform = new ByteResourceInformation(originalBytes, null, null, null, false, () -> originalBytes);
        byte[] transformedBytes = loader.transformClassBytes("goodbye", toTransform);

        assertTrue(transformerInvoked.get());
        assertArrayEquals(originalBytes, transformedBytes);
        assertEquals("Goodbye!", new String(transformedBytes));
    }

    @Test
    public void testTransformerReturnsTransformedBytes() throws Exception {
        doTestTransformerReturnsTransformedBytes(false);
    }

    @Test
    public void testSystemTransformerReturnsTransformedBytes() throws Exception {
        doTestTransformerReturnsTransformedBytes(true);
    }

    private void doTestTransformerReturnsTransformedBytes(boolean systemTransformer) throws Exception {
        if (systemTransformer) {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, Boolean.TRUE.toString());
        }

        final AtomicBoolean transformerInvoked = new AtomicBoolean(false);
        ClassFileTransformer transformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                transformerInvoked.set(true);
                String original = new String(classfileBuffer);
                String transformed = original + " and salutations!";
                return transformed.getBytes();
            }
        };
        AppClassLoader loader = createAppClassloaderTransformer(transformer, systemTransformer);

        byte[] originalBytes = "Greetings".getBytes();
        ByteResourceInformation toTransform = new ByteResourceInformation(originalBytes, null, null, null, false, () -> originalBytes);
        byte[] transformedBytes = loader.transformClassBytes("greetings", toTransform);

        assertTrue(transformerInvoked.get());
        assertFalse(Arrays.equals(originalBytes, transformedBytes));
        assertEquals("Greetings and salutations!", new String(transformedBytes));
    }

    @Test
    public void testTransformerReturnsTransformedBytesClassCached() throws Exception {
        doTestTransformerReturnsTransformedBytesClassCached(false, false);
    }

    @Test
    public void testSystemTransformerReturnsTransformedBytesClassCached() throws Exception {
        doTestTransformerReturnsTransformedBytesClassCached(true, true);
    }

    @Test
    public void testSystemTransformerReturnsTransformedBytesClassCachedNotBeta() throws Exception {
        doTestTransformerReturnsTransformedBytesClassCached(true, false);
    }

    private void doTestTransformerReturnsTransformedBytesClassCached(boolean systemTransformer, boolean isBeta) throws Exception {
        if (isBeta) {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, Boolean.TRUE.toString());
        }

        final AtomicBoolean transformerInvoked = new AtomicBoolean(false);
        ClassFileTransformer transformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                transformerInvoked.set(true);
                String original = new String(classfileBuffer);
                String transformed = original + " and salutations!";
                return transformed.getBytes();
            }
        };
        AppClassLoader loader = createAppClassloaderTransformer(transformer, systemTransformer);

        byte[] originalBytes = "Greetings".getBytes();
        final boolean fromCached = true;
        ByteResourceInformation toTransform = new ByteResourceInformation(originalBytes, null, null, null, fromCached, () -> originalBytes);
        byte[] transformedBytes = loader.transformClassBytes("greetings", toTransform);

        if (systemTransformer && isBeta) {
            // System transformers invoke in beta-editions, only
            assertTrue(transformerInvoked.get());
            assertFalse(Arrays.equals(originalBytes, transformedBytes));
            assertEquals("Greetings and salutations!", new String(transformedBytes));
        } else if (systemTransformer) {
            assertFalse(transformerInvoked.get());
            assertTrue(Arrays.equals(originalBytes, transformedBytes));
            assertEquals("Greetings", new String(transformedBytes));
        } else {
            // Transformers invoke
            assertTrue(transformerInvoked.get());
            assertFalse(Arrays.equals(originalBytes, transformedBytes));
            assertEquals("Greetings and salutations!", new String(transformedBytes));
        }
    }
}
