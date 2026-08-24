/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.osgi.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.junit.Test;

import com.ibm.wsspi.classloading.ClassTransformer;

/**
 * Unit tests for persistence-unit-aware class transformer routing.
 */
public class PersistenceClassTransformerCoordinatorTest {
    @Test
    public void unmatchedClassIsNotTransformed() {
        PersistenceClassTransformerCoordinator coordinator = new PersistenceClassTransformerCoordinator();
        RecordingTransformer transformer = new RecordingTransformer(new byte[] { 2 });
        byte[] original = new byte[] { 1 };
        coordinator.add(new Object(), singletonList("example.Entity"), transformer);

        assertSame(original, coordinator.transformClass("example.Other", original, null, getClass().getClassLoader()));
        assertEquals(0, transformer.invocationCount);
    }

    @Test
    public void internalClassNameAndRedefinedClassArePassedToOwner() {
        PersistenceClassTransformerCoordinator coordinator = new PersistenceClassTransformerCoordinator();
        RecordingTransformer transformer = new RecordingTransformer(new byte[] { 2 });
        Class<?> redefinedClass = PersistenceClassTransformerCoordinatorTest.class;
        ProtectionDomain protectionDomain = getClass().getProtectionDomain();
        coordinator.add(new Object(), asList("example.Entity", "example.Other"), transformer);

        assertSame(transformer.result,
                   coordinator.transformClass("example/Entity", redefinedClass, new byte[] { 1 }, protectionDomain, getClass().getClassLoader()));
        assertEquals(1, transformer.invocationCount);
        assertSame(redefinedClass, transformer.classBeingRedefined);
        assertSame(protectionDomain, transformer.protectionDomain);
    }

    @Test
    public void firstOwnerWinsUntilRemoved() {
        PersistenceClassTransformerCoordinator coordinator = new PersistenceClassTransformerCoordinator();
        Object firstOwner = new Object();
        RecordingTransformer first = new RecordingTransformer(new byte[] { 2 });
        RecordingTransformer second = new RecordingTransformer(new byte[] { 3 });
        coordinator.add(firstOwner, singletonList("example.Entity"), first);
        coordinator.add(new Object(), singletonList("example.Entity"), second);

        assertSame(first.result,
                   coordinator.transformClass("example.Entity", new byte[] { 1 }, null, getClass().getClassLoader()));
        assertEquals(1, first.invocationCount);
        assertEquals(0, second.invocationCount);

        coordinator.remove(firstOwner);
        assertSame(second.result,
                   coordinator.transformClass("example.Entity", new byte[] { 1 }, null, getClass().getClassLoader()));
        assertEquals(1, second.invocationCount);
    }

    private static final class RecordingTransformer implements ClassTransformer {
        private final byte[] result;
        private int invocationCount;
        private Class<?> classBeingRedefined;
        private ProtectionDomain protectionDomain;

        private RecordingTransformer(byte[] result) {
            this.result = result;
        }

        @Override
        public byte[] transformClass(String name, byte[] bytes, CodeSource source, ClassLoader loader) {
            return transformClass(name, null, bytes, new ProtectionDomain(source, null, loader, null), loader);
        }

        @Override
        public byte[] transformClass(String name,
                                     Class<?> classBeingRedefined,
                                     byte[] bytes,
                                     ProtectionDomain protectionDomain,
                                     ClassLoader loader) {
            invocationCount++;
            this.classBeingRedefined = classBeingRedefined;
            this.protectionDomain = protectionDomain;
            return result;
        }
    }
}
