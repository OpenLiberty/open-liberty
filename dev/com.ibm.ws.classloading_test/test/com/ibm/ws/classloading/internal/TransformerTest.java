/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.classloading.internal.ClassLoadingServiceImpl.ClassFileTransformerAdapter;
import com.ibm.ws.classloading.internal.ContainerClassLoader.ByteResourceInformation;
import com.ibm.ws.classloading.internal.ContainerClassLoader.ContainerURL;
import com.ibm.ws.classloading.internal.ContainerClassLoader.UniversalContainer;
import com.ibm.ws.classloading.internal.ContainerClassLoader.UniversalContainerList;
import com.ibm.ws.kernel.boot.classloader.ClassLoaderHook;
import com.ibm.wsspi.classloading.ClassTransformer;

import test.common.SharedOutputManager;

/**
 * Test to make sure that transformers can be correctly added to/removed from an AppClassLoader
 */
@SuppressWarnings("restriction")
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

    private static UniversalContainer testContainer = new UniversalContainer() {

        private final URL url;

        {
            URL urlToSet;
            try {
                urlToSet = new File(System.getProperty("user.dir")).toURI().toURL();
            } catch (MalformedURLException e) {
                urlToSet = null;
            }
            url = urlToSet;
        }

        @Override
        public UniversalResource getResource(String name) {
            return null;
        }

        @Override
        public void updatePackageMap(Map<Integer, UniversalContainerList> map, boolean prepend) {

        }

        @Override
        public Collection<URL> getContainerURLs() {
            return null;
        }

        @Override
        public void definePackage(String packageName, LibertyLoader loader, ContainerURL sealBase) {

        }

        @Override
        public ContainerURL getContainerURL(UniversalResource resource) {
            return new ContainerURL(url);
        }

        @Override
        public URL getSharedClassCacheURL(UniversalResource resource) {
            return url;
        }

    };

    @Test
    public void testTransformerReturnsNull() throws Exception {
        doTestTransformerReturnsNull(false);
    }

    @Test
    public void testSystemTransformerReturnsNull() throws Exception {
        doTestTransformerReturnsNull(true);
    }

    private void doTestTransformerReturnsNull(boolean systemTransformer) throws Exception {
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
        ByteResourceInformation toTransform = new ByteResourceInformation(testContainer, null, null, () -> originalBytes, null);
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
        ByteResourceInformation toTransform = new ByteResourceInformation(testContainer, null, null, () -> originalBytes, null);
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
        ByteResourceInformation toTransform = new ByteResourceInformation(testContainer, null, null, () -> originalBytes, null);
        byte[] transformedBytes = loader.transformClassBytes("greetings", toTransform);

        assertTrue(transformerInvoked.get());
        assertFalse(Arrays.equals(originalBytes, transformedBytes));
        assertEquals("Greetings and salutations!", new String(transformedBytes));
    }

    @Test
    public void testTransformerReturnsTransformedBytesClassCached() throws Exception {
        doTestTransformerReturnsTransformedBytesClassCached(false);
    }

    @Test
    public void testSystemTransformerReturnsTransformedBytesClassCached() throws Exception {
        doTestTransformerReturnsTransformedBytesClassCached(true);
    }

    private void doTestTransformerReturnsTransformedBytesClassCached(boolean systemTransformer) throws Exception {
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

        final AtomicBoolean hookLoadClassInvoked = new AtomicBoolean(false);
        byte[] originalBytes = "Greetings".getBytes();
        ClassLoaderHook hook = new ClassLoaderHook() {

            @Override
            public byte[] loadClass(URL arg0, String arg1) {
                hookLoadClassInvoked.set(true);
                return originalBytes;
            }

            @Override
            public void storeClass(URL arg0, Class<?> arg1) {
                // do nothing
            }

        };
        AtomicInteger supplierCalled = new AtomicInteger(0);
        Supplier<byte[]> supplier = new Supplier<byte[]>() {

            @Override
            public byte[] get() {
                supplierCalled.incrementAndGet();
                return originalBytes;
            }

        };

        ByteResourceInformation toTransform = new ByteResourceInformation(testContainer, null, null, supplier, hook);
        assertTrue(hookLoadClassInvoked.get());
        assertTrue(toTransform.foundInClassCache());
        byte[] transformedBytes = loader.transformClassBytes("greetings", toTransform);

        assertTrue(transformerInvoked.get());
        assertFalse(Arrays.equals(originalBytes, transformedBytes));
        assertEquals("Greetings and salutations!", new String(transformedBytes));
        // If supplier is called twice it means we did not use hook to get the bytes.
        assertEquals(1, supplierCalled.get());
    }
}
