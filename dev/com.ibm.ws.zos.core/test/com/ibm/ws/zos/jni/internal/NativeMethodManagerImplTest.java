/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;

import com.ibm.ws.zos.jni.NativeMethodManager;
@RunWith(JMock.class)
public class NativeMethodManagerImplTest {

    final Mockery context = new JUnit4Mockery();

    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testActivateDeactivate() {
        // Instantiate our test instance
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext);
        final AngelUtilsImpl testAngelServices = AngelUtilsImpl.INSTANCE;
        context.checking(new Expectations() {
            {
                oneOf(bundleContext).addBundleListener(with(same(testManager)));
                oneOf(bundleContext).registerService(with(NativeMethodManager.class),
                                                     with(same(testManager)),
                                                     with(aNonNull(Dictionary.class)));
                oneOf(bundleContext).registerService(with(com.ibm.ws.zos.jni.AngelUtils.class),
                                                     with(same(testAngelServices)),
                                                     with(aNull(Dictionary.class)));
                oneOf(bundleContext).removeBundleListener(with(same(testManager)));
            }
        });

        // Ensure we register a bundle listener and hold the bundle context
        testManager.start(bundleContext);
        assertSame(bundleContext, testManager.bundleContext);
        assertNotNull(testManager.nativeInfo);
        assertTrue(testManager.nativeInfo.isEmpty());

        // Ensure we unregister the bundle listener
        testManager.stop(bundleContext);
        assertTrue(testManager.nativeInfo.isEmpty());
        assertTrue(testManager.bundleData.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeactivateWithClasses() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final AngelUtilsImpl testAngelServices = AngelUtilsImpl.INSTANCE;
        context.checking(new Expectations() {
            {
                allowing(bundleContext).addBundleListener(with(any(BundleListener.class)));
                oneOf(bundleContext).registerService(with(NativeMethodManager.class),
                                                     with(aNonNull(NativeMethodManager.class)),
                                                     with(aNonNull(Dictionary.class)));
                oneOf(bundleContext).registerService(with(com.ibm.ws.zos.jni.AngelUtils.class),
                                                     with(same(testAngelServices)),
                                                     with(aNull(Dictionary.class)));
                allowing(bundleContext).removeBundleListener(with(any(BundleListener.class)));
            }
        });

        final Version version = new Version("1.2.3.build7890");
        final Bundle mockBundle = context.mock(Bundle.class);
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundle).getVersion();
                will(returnValue(version));
            }
        });

        // Instantiate our test instance
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext) {
            int registerCount = 0;
            int deregisterCount = 0;

            @Override
            protected Bundle getBundle(Class<?> clazz) {
                return mockBundle;
            }

            @Override
            protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
                return ++registerCount;
            }

            @Override
            protected long invokeHelperDeregisterNatives(long dllHandle, Class<?> clazz, String structureName, Object[] extraInfo) {
                deregisterCount++;
                assertEquals(1, registerCount);
                return 0;
            }

            @Override
            public void stop(BundleContext bundleContext) {
                super.stop(bundleContext);
                assertEquals(1, deregisterCount);
            }
        };

        // Activate the component
        testManager.start(bundleContext);
        assertSame(bundleContext, testManager.bundleContext);
        assertTrue(testManager.nativeInfo.isEmpty());

        // Register natives
        testManager.registerNatives(java.io.Serializable.class);
        assertFalse(testManager.bundleData.isEmpty());
        assertFalse(testManager.nativeInfo.isEmpty());

        // Deactivate and verify
        testManager.stop(bundleContext);
        assertTrue(testManager.nativeInfo.isEmpty());
        assertTrue(testManager.bundleData.isEmpty());
    }

    @Test
    public void testGetStructureCandidates() {
        BundleContext bundleContext = context.mock(BundleContext.class);
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext);

        Version version = new Version(1, 2, 3, "ignored");
        List<String> candidates = testManager.getStructureCandidates(NativeMethodManagerImplTest.class, version);
        assertEquals(5, candidates.size());
        assertEquals("zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest__1_2_3", candidates.get(0));
        assertEquals("zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest__1_2", candidates.get(1));
        assertEquals("zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest__1", candidates.get(2));
        assertEquals("zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest", candidates.get(3));
        assertEquals("zJNI_NativeMethodManagerImplTest", candidates.get(4));

        List<String> nonBundleCandidates = testManager.getStructureCandidates(NativeMethodManagerImplTest.class, null);
        assertEquals(2, nonBundleCandidates.size());
        assertEquals("zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest", nonBundleCandidates.get(0));
        assertEquals("zJNI_NativeMethodManagerImplTest", nonBundleCandidates.get(1));
    }

    @Test
    public void testRegisterNativesDescriptorNotFound() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final Version version = new Version("1.2.3.build7890");
        final Bundle mockBundle = context.mock(Bundle.class);
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundle).getVersion();
                will(returnValue(version));
            }
        });

        final List<String> structures = new ArrayList<String>();
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext) {
            @Override
            synchronized public void registerNatives(Class<?> clazz, Object[] extraInfo) {
                // Verify when extra info isn't provided that we see a null
                assertNull(extraInfo);
                assertSame(NativeMethodManagerImplTest.class, clazz);
                super.registerNatives(clazz, extraInfo);
            }

            @Override
            protected Bundle getBundle(Class<?> clazz) {
                return mockBundle;
            }

            // Pretend native descriptors are not found
            @Override
            protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
                assertNull(extraInfo);
                assertSame(NativeMethodManagerImplTest.class, clazz);
                structures.add(structureName);
                return 0;
            }
        };

        // Verify that an unsatisfied link error is thrown when the native
        // descriptors can't be resolved
        try {
            testManager.registerNatives(NativeMethodManagerImplTest.class);
            fail("Unsatisfied link error not thrown");
        } catch (UnsatisfiedLinkError e) {
            assertNotNull(e);
        }

        // Verify that all of the structure name candidates were tried
        assertEquals(structures, testManager.getStructureCandidates(NativeMethodManagerImplTest.class, version));
    }

    @Test
    public void testResisterNativesDescriptorFound() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final Version version = new Version("1.2.3.build7890");
        final Object[] expectedExtraInfo = { "Some", "Extra", "Info", Long.valueOf(9999) };
        final String descriptorName = "zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest__1_2";
        final long dllHandle = 0x0DEAD0BEEF0CAFEL;

        final Bundle mockBundle = context.mock(Bundle.class);
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundle).getVersion();
                will(returnValue(version));
            }
        });

        final List<String> structures = new ArrayList<String>();
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext) {
            @Override
            synchronized public void registerNatives(Class<?> clazz, Object[] extraInfo) {
                // Verify when extra info isn't provided that we see a null
                assertSame(expectedExtraInfo, extraInfo);
                assertSame(NativeMethodManagerImplTest.class, clazz);
                super.registerNatives(clazz, extraInfo);
            }

            @Override
            protected Bundle getBundle(Class<?> clazz) {
                assertSame(NativeMethodManagerImplTest.class, clazz);
                return mockBundle;
            }

            // Pretend first native descriptor wasn't found
            @Override
            protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
                assertSame(expectedExtraInfo, extraInfo);
                assertSame(NativeMethodManagerImplTest.class, clazz);
                structures.add(structureName);
                if (descriptorName.equals(structureName)) {
                    return dllHandle;
                }
                return 0;
            }
        };

        // Drive registerNatives with extra info
        testManager.registerNatives(NativeMethodManagerImplTest.class, expectedExtraInfo);

        // Verify we stopped searching on success and that we looked for the right names
        assertEquals(2, structures.size());
        assertTrue(structures.contains("zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest__1_2_3"));
        assertTrue(structures.contains("zJNI_com_ibm_ws_zos_jni_internal_NativeMethodManagerImplTest__1_2"));

        assertEquals(1, testManager.bundleData.size());
        assertTrue(testManager.bundleData.containsKey(mockBundle));
        assertEquals(1, testManager.bundleData.get(mockBundle).size());
        assertTrue(testManager.bundleData.get(mockBundle).contains(NativeMethodManagerImplTest.class));

        NativeMethodInfo methodInfo = testManager.nativeInfo.get(NativeMethodManagerImplTest.class);
        assertNotNull(methodInfo);
        assertSame(methodInfo.clazz, NativeMethodManagerImplTest.class);
        assertEquals(methodInfo.dllHandle, dllHandle);
        assertSame(methodInfo.extraInfo, expectedExtraInfo);
        assertEquals(methodInfo.nativeDescriptorName, descriptorName);
    }

    @Test
    public void testRegisterNativesDeregisterBundle() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final Version version1 = new Version("1.2.3");
        final Bundle mockBundle1 = context.mock(Bundle.class, "mockBundle1");
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundle1).getVersion();
                will(returnValue(version1));
            }
        });
        final Map<Class<?>, Long> bundle1Classes = new HashMap<Class<?>, Long>();
        bundle1Classes.put(java.io.Serializable.class, Long.valueOf(0x00010001));
        bundle1Classes.put(java.lang.Boolean.class, Long.valueOf(0x00010002));
        bundle1Classes.put(java.util.concurrent.Executors.class, Long.valueOf(0x00010003));

        final Version version2 = new Version("2.0.2");
        final Bundle mockBundle2 = context.mock(Bundle.class, "mockBundle2");
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundle2).getVersion();
                will(returnValue(version2));
            }
        });
        final Map<Class<?>, Long> bundle2Classes = new HashMap<Class<?>, Long>();
        bundle2Classes.put(java.util.concurrent.locks.Lock.class, Long.valueOf(0x00020001));
        bundle2Classes.put(java.lang.System.class, Long.valueOf(0x00020002));
        bundle2Classes.put(java.lang.ClassLoader.class, Long.valueOf(0x00020003));

        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext) {
            @Override
            protected Bundle getBundle(Class<?> clazz) {
                if (bundle1Classes.containsKey(clazz)) {
                    return mockBundle1;
                } else if (bundle2Classes.containsKey(clazz)) {
                    return mockBundle2;
                } else {
                    fail("Unexpected class; No associated bundle.");
                    return null;
                }
            }

            @Override
            protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
                if (bundle1Classes.containsKey(clazz)) {
                    return bundle1Classes.get(clazz);
                } else if (bundle2Classes.containsKey(clazz)) {
                    return bundle2Classes.get(clazz);
                } else {
                    fail("Unexpected class; No associated DLL handle");
                    return -1;
                }

            }

            @Override
            protected long invokeHelperDeregisterNatives(long dllHandle, Class<?> clazz, String structureName, Object[] extraInfo) {
                // Remove classes as we go along so we can verify we were called
                if (bundle1Classes.containsKey(clazz)) {
                    assertEquals(bundle1Classes.remove(clazz).longValue(), dllHandle);
                } else if (bundle2Classes.containsKey(clazz)) {
                    assertEquals(bundle2Classes.remove(clazz).longValue(), dllHandle);
                } else {
                    fail("Unexpected class; no information found");
                }
                return 0;
            }
        };

        // Make sure we start with empty, non-null tables
        assertNotNull(testManager.bundleData);
        assertTrue(testManager.bundleData.isEmpty());
        assertNotNull(testManager.nativeInfo);
        assertTrue(testManager.nativeInfo.isEmpty());

        // Call register natives for bundle 1 classes
        for (Class<?> clazz : bundle1Classes.keySet()) {
            testManager.registerNatives(clazz);
            assertTrue(testManager.nativeInfo.containsKey(clazz));
            assertEquals(bundle1Classes.get(clazz).longValue(), testManager.nativeInfo.get(clazz).dllHandle);
        }
        assertEquals(1, testManager.bundleData.size());
        assertTrue(testManager.bundleData.containsKey(mockBundle1));
        assertTrue(testManager.bundleData.get(mockBundle1).containsAll(bundle1Classes.keySet()));
        assertEquals(bundle1Classes.size(), testManager.nativeInfo.size());

        // Call register natives for bundle 2 classes
        for (Class<?> clazz : bundle2Classes.keySet()) {
            testManager.registerNatives(clazz);
            assertTrue(testManager.nativeInfo.containsKey(clazz));
            assertEquals(bundle2Classes.get(clazz).longValue(), testManager.nativeInfo.get(clazz).dllHandle);
        }
        assertEquals(2, testManager.bundleData.size());
        assertTrue(testManager.bundleData.containsKey(mockBundle2));
        assertTrue(testManager.bundleData.get(mockBundle2).containsAll(bundle2Classes.keySet()));
        assertEquals(bundle1Classes.size() + bundle2Classes.size(), testManager.nativeInfo.size());

        // Deregister bundle 1
        assertTrue(testManager.bundleData.containsKey(mockBundle1));
        testManager.cleanupRegistrations(mockBundle1);
        assertFalse(testManager.bundleData.containsKey(mockBundle1));
        assertTrue(bundle1Classes.isEmpty());
        assertEquals(bundle2Classes.size(), testManager.nativeInfo.size());
        assertTrue(testManager.nativeInfo.keySet().containsAll(bundle2Classes.keySet()));

        // Deregister bundle 2
        assertTrue(testManager.bundleData.containsKey(mockBundle2));
        testManager.cleanupRegistrations(mockBundle2);
        assertTrue(bundle2Classes.isEmpty());

        // Ensure the data is cleaned up
        assertTrue(testManager.bundleData.isEmpty());
        assertTrue(testManager.nativeInfo.isEmpty());
    }

    @Test
    public void testBundleChanged() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final Version version = new Version("1.2.3.build7890");
        final Object[] expectedExtraInfo = { "Some", "Extra", "Info", Long.valueOf(9999) };
        final Bundle mockBundle = context.mock(Bundle.class);
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundle).getVersion();
                will(returnValue(version));
            }
        });

        final BundleEvent installedEvent = new BundleEvent(BundleEvent.INSTALLED, mockBundle);
        final BundleEvent stoppingEvent = new BundleEvent(BundleEvent.STOPPING, mockBundle);
        final BundleEvent stoppedEvent = new BundleEvent(BundleEvent.STOPPED, mockBundle);

        final AtomicBoolean deregisterCalled = new AtomicBoolean(false);
        final AtomicBoolean helperDeregisterCalled = new AtomicBoolean(false);
        final long dllHandle = 0x0123456789ABCDL;
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext) {
            @Override
            protected Bundle getBundle(Class<?> clazz) {
                assertSame(NativeMethodManagerImplTest.class, clazz);
                return mockBundle;
            }

            @Override
            synchronized void cleanupRegistrations(Bundle bundle) {
                assertSame(mockBundle, bundle);
                assertFalse(deregisterCalled.get());
                deregisterCalled.set(true);
                super.cleanupRegistrations(bundle);
            }

            @Override
            protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
                assertSame(NativeMethodManagerImplTest.class, clazz);
                assertSame(expectedExtraInfo, extraInfo);
                assertFalse(deregisterCalled.get());
                assertFalse(helperDeregisterCalled.get());
                return dllHandle;
            }

            @Override
            protected long invokeHelperDeregisterNatives(long dllHandle, Class<?> clazz, String structureName, Object[] extraInfo) {
                assertSame(NativeMethodManagerImplTest.class, clazz);
                assertSame(expectedExtraInfo, extraInfo);
                assertTrue(deregisterCalled.get());
                assertFalse(helperDeregisterCalled.get());
                helperDeregisterCalled.set(true);
                return 0;
            }
        };

        // Make sure we start with empty, non-null tables
        assertNotNull(testManager.bundleData);
        assertTrue(testManager.bundleData.isEmpty());
        assertNotNull(testManager.nativeInfo);
        assertTrue(testManager.nativeInfo.isEmpty());

        // Call register natives
        testManager.registerNatives(NativeMethodManagerImplTest.class, expectedExtraInfo);
        assertFalse(testManager.bundleData.isEmpty());
        assertFalse(testManager.nativeInfo.isEmpty());

        // Drive unrelated bundle events
        testManager.bundleChanged(installedEvent);
        testManager.bundleChanged(stoppingEvent);
        assertFalse(deregisterCalled.get());

        // Drive the bundle stop event
        testManager.bundleChanged(stoppedEvent);
        assertTrue(deregisterCalled.get());

        // Ensure the data is cleaned up
        assertTrue(testManager.bundleData.isEmpty());
        assertTrue(testManager.nativeInfo.isEmpty());
    }

    @Test
    public void testRegisterNativesNoBundle() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext) {
            @Override
            protected Bundle getBundle(Class<?> clazz) {
                assertSame(java.io.Serializable.class, clazz);
                return null;
            }

            @Override
            protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
                assertSame(java.io.Serializable.class, clazz);
                assertEquals("zJNI_java_io_Serializable", structureName);
                return 0x12345678L;
            }
        };

        // Drive the registration
        testManager.registerNatives(java.io.Serializable.class, null);
        assertTrue(testManager.bundleData.isEmpty());
        assertEquals(1, testManager.nativeInfo.size());
        assertTrue(testManager.nativeInfo.containsKey(java.io.Serializable.class));
    }

    @Test
    public void testRegisterDuplicateRegistration() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        final NativeMethodManagerImpl testManager = new NativeMethodManagerImpl(bundleContext) {
            int registerCount = 0;

            @Override
            protected Bundle getBundle(Class<?> clazz) {
                return null;
            }

            @Override
            protected long invokeHelperRegisterNatives(Class<?> clazz, String structureName, Object[] extraInfo) {
                return ++registerCount;
            }
        };

        // Drive the registration
        testManager.registerNatives(java.io.Serializable.class, null);
        testManager.registerNatives(java.io.Serializable.class, null);
        assertEquals(1, testManager.nativeInfo.size());
        assertTrue(testManager.nativeInfo.containsKey(java.io.Serializable.class));
        assertEquals(1, testManager.nativeInfo.get(java.io.Serializable.class).dllHandle);
    }
}
