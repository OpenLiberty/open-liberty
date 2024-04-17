/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.condition.Condition;

import com.ibm.wsspi.kernel.feature.LibertyFeature;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import test.common.SharedLocationManager;

public class CheckpointImplTest {
    private static final String testbuildDir = System.getProperty("test.buildDir", "generated");
    @Rule
    public TestName testName = new TestName();

    static class ComponentContextHandler implements InvocationHandler {
        final AtomicReference<ClassFileTransformer> transformer = new AtomicReference<>();
        final AtomicReference<Condition> runningCondition = new AtomicReference<>();
        final Set<String> enabledFeatures;
        final Map<String, String> contextProperties = new HashMap<>();;

        public ComponentContextHandler() {
            this(Collections.emptySet());
        }

        public ComponentContextHandler(Set<String> enabledFeatures) {
            super();
            this.enabledFeatures = enabledFeatures;
        }

        void putContextProperty(String key, String value) {
            this.contextProperties.put(key, value);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getBundleContext".equals(method.getName())) {
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { BundleContext.class }, (p, m, a) -> {
                    if ("getProperty".equals(m.getName())) {
                        return contextProperties.get(a[0]);
                    }
                    if ("registerService".equals(m.getName())) {
                        if (Condition.class.equals(a[0]) || ClassFileTransformer.class.equals(a[0])) {
                            if (ClassFileTransformer.class.equals(a[0])) {
                                transformer.set((ClassFileTransformer) a[1]);
                            }
                            if (Condition.class.equals(a[0])) {
                                runningCondition.set((Condition) a[1]);
                            }
                            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ServiceRegistration.class }, (p1, m1, a1) -> {
                                if ("unregister".equals(m1.getName())) {
                                    return null;
                                }
                                throw new UnsupportedOperationException(m1.getName());
                            });
                        }
                    }
                    if ("getServiceReferences".equals(m.getName())) {
                        if (enabledFeatures.isEmpty()) {
                            return null;
                        }
                        ServiceReference<?>[] featureServices = new ServiceReference<?>[enabledFeatures.size()];
                        int i = 0;
                        for (String featureName : enabledFeatures) {
                            featureServices[i] = (ServiceReference<?>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ServiceReference.class }, (p1, m1, a1) -> {
                                if ("getProperty".equals(m1.getName())) {
                                    return featureName;
                                }
                                throw new UnsupportedOperationException(m1.getName());
                            });
                            i = i + 1;
                        }
                        return featureServices;
                    }
                    if ("getService".equals(m.getName())) {
                        @SuppressWarnings("unchecked")
                        ServiceReference<LibertyFeature> ref = (ServiceReference<LibertyFeature>) a[0];
                        String featureName = (String) ref.getProperty("ibm.featureName");
                        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { LibertyFeature.class }, (p1, m1, a1) -> {
                            if ("getHeader".equals(m1.getName())) {
                                return featureName.startsWith("notSupported") ? null : "true";
                            }
                            throw new UnsupportedOperationException(m1.getName());
                        });
                    }
                    if ("ungetService".equals(m.getName())) {
                        return true;
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    static class TestCRIU implements ExecuteCRIU {
        final AtomicBoolean runtimeSingleThreadMode;
        volatile File imageDir = null;
        volatile String logFilename = null;
        volatile File workDir = null;
        volatile boolean throwIOException = false;

        public TestCRIU() {
            this(new AtomicBoolean());
        }

        TestCRIU(AtomicBoolean runtimeSingleThreaded) {
            this.runtimeSingleThreadMode = runtimeSingleThreaded;
        }

        @Override
        public void dump(Runnable prepare, Runnable restore, File imageDir, String logFileName, File workDir, File envProps,
                         boolean unprivileged) throws CheckpointFailedException {
            runtimeSingleThreadMode.set(true);
            try {
                prepare.run();
                this.imageDir = imageDir;
                this.logFilename = logFileName;
                this.workDir = workDir;
                if (throwIOException) {
                    throw new CheckpointFailedException(Type.SYSTEM_CHECKPOINT_FAILED, "Test failure", new IOException("failed"));
                }
                restore.run();
            } finally {
                runtimeSingleThreadMode.set(false);
            }
        }

        @Override
        public void checkpointSupported() {
        }
    }

    @After
    public void clearTestCheckHookClass() {
        TestCheckpointHook.nextId.set(0);
        TestCheckpointHook.prepareOrder.clear();
        TestCheckpointHook.restoreOrder.clear();
    }

    static class TestCheckpointHook implements CheckpointHook, ServiceReference<CheckpointHook> {
        static final AtomicInteger nextId = new AtomicInteger();
        static final List<TestCheckpointHook> prepareOrder = Collections.synchronizedList(new ArrayList<>());
        static final List<TestCheckpointHook> restoreOrder = Collections.synchronizedList(new ArrayList<>());
        final RuntimeException prepareException;
        final RuntimeException restoreException;
        final AtomicBoolean runtimeSingleThreadMode;
        final long id;
        final int rank;
        final boolean hookMultiThreadMode;
        final boolean cracHooks;

        public TestCheckpointHook(AtomicBoolean runtimeSingleThreadMode, boolean hookMultiThreaded, boolean cracHook) {
            this(null, null, runtimeSingleThreadMode, 0, hookMultiThreaded, cracHook);
        }

        public TestCheckpointHook(RuntimeException prepareException, RuntimeException restoreException,
                                  AtomicBoolean runtimeSingleThreadMode, //
                                  int rank,
                                  boolean hookMultiThreadMode, //
                                  boolean cracHooks) {
            this.prepareException = prepareException;
            this.restoreException = restoreException;
            this.runtimeSingleThreadMode = runtimeSingleThreadMode;
            this.id = nextId.getAndIncrement();
            this.rank = rank;
            this.hookMultiThreadMode = hookMultiThreadMode;
            this.cracHooks = cracHooks;
        }

        volatile boolean prepareCalled = false;
        volatile boolean prepareCalledSingleThreaded = false;
        volatile boolean checkpointFailed = false;
        volatile boolean restoreCalled = false;
        volatile boolean restoreCalledSingleThreaded = false;

        @Override
        public void prepare() {
            prepareOrder.add(this);
            prepareCalled = true;
            prepareCalledSingleThreaded = runtimeSingleThreadMode.get();
            if (prepareException != null) {
                throw prepareException;
            }
        }

        @Override
        public void checkpointFailed() {
            checkpointFailed = true;
        }

        @Override
        public void restore() {
            restoreOrder.add(this);
            restoreCalled = true;
            restoreCalledSingleThreaded = runtimeSingleThreadMode.get();
            if (restoreException != null) {
                throw restoreException;
            }
        }

        @Override
        public <A> A adapt(Class<A> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Object o) {
            TestCheckpointHook oHook = (TestCheckpointHook) o;

            if (this.rank != oHook.rank) {
                if (this.rank < oHook.rank) {
                    return -1;
                }
                return 1;
            }
            if (this.id == oHook.id) {
                return 0;
            }
            if (this.id < oHook.id) {
                return 1;
            }
            return -1;
        }

        @Override
        public Bundle getBundle() {
            throw new UnsupportedOperationException();

        }

        @Override
        public Dictionary<String, Object> getProperties() {
            throw new UnsupportedOperationException();

        }

        @Override
        public Object getProperty(String key) {
            if (key.equals(Constants.SERVICE_ID)) {
                return id;
            }
            if (key.equals(Constants.SERVICE_RANKING)) {
                return rank;
            }
            if (key.equals(CheckpointHook.MULTI_THREADED_HOOK)) {
                return hookMultiThreadMode;
            }
            if (key.equals("io.openliberty.crac.hooks")) {
                return cracHooks;
            }
            return null;
        }

        @Override
        public String[] getPropertyKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle[] getUsingBundles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAssignableTo(Bundle arg0, String arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "TestCheckPointHook: id=" + id + " rank=" + rank + " multiThreadMode=" + hookMultiThreadMode + " cracHooks=" + cracHooks;
        }
    }

    private ComponentContext createComponentContext() {
        return createComponentContext(new ComponentContextHandler());
    }

    private ComponentContext createComponentContext(ComponentContextHandler componentContextHandler) {
        return (ComponentContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ComponentContext.class }, componentContextHandler);
    }

    private void setHooks(CheckpointImpl checkpoint, TestCheckpointHook... hooks) {
        for (TestCheckpointHook hook : hooks) {
            checkpoint.addHook(hook, hook);
        }
    }

    @Test
    public void testNullFactories() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        checkDirectory(checkpoint, criu, locAdmin);
        checkpoint.resetCheckpointCalled();
        checkFailDump(checkpoint, criu, locAdmin);
    }

    @Test
    public void testNullHook() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        checkDirectory(checkpoint, criu, locAdmin);
    }

    @Test
    public void testPrepareRestore() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new ComponentContextHandler()), criu, locAdmin, CheckpointPhase.AFTER_APP_START);

        TestCheckpointHook h1 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h2 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        setHooks(checkpoint, h1, h2, h3);

        checkDirectory(checkpoint, criu, locAdmin);
        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Checkpoint failed called", false, hook.checkpointFailed);
            assertEquals("Restore not called.", true, hook.restoreCalled);
        }
    }

    @Test
    public void testPrepareRestoreWithMultiThreadedHooks() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new ComponentContextHandler()), criu, locAdmin, CheckpointPhase.AFTER_APP_START);

        TestCheckpointHook h1 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h2 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h4 = new TestCheckpointHook(criu.runtimeSingleThreadMode, true, false);
        TestCheckpointHook h5 = new TestCheckpointHook(criu.runtimeSingleThreadMode, true, false);
        TestCheckpointHook h6 = new TestCheckpointHook(criu.runtimeSingleThreadMode, true, false);
        setHooks(checkpoint, h1, h2, h3, h4, h5, h6);

        checkDirectory(checkpoint, criu, locAdmin);
        List<TestCheckpointHook> singleThreadedHooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : singleThreadedHooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Checkpoint failed called", false, hook.checkpointFailed);
            assertEquals("Prepare called while not single threaded.", true, hook.prepareCalledSingleThreaded);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertEquals("Restore called while not single threaded.", true, hook.restoreCalledSingleThreaded);
        }
        List<TestCheckpointHook> multiThreadedhooks = getHooks(h4, h5, h6);
        for (TestCheckpointHook hook : multiThreadedhooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Checkpoint failed called", false, hook.checkpointFailed);
            assertEquals("Prepare called while single threaded.", false, hook.prepareCalledSingleThreaded);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertEquals("Restore called while single threaded.", false, hook.restoreCalledSingleThreaded);
        }
    }

    @Test
    public void testPrepareRestoreHookOrder() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new ComponentContextHandler()), criu, locAdmin, CheckpointPhase.AFTER_APP_START);

        TestCheckpointHook h1 = new TestCheckpointHook(null, null, criu.runtimeSingleThreadMode, -100, false, false);
        TestCheckpointHook h2 = new TestCheckpointHook(null, null, criu.runtimeSingleThreadMode, 0, false, false);
        TestCheckpointHook h3 = new TestCheckpointHook(null, null, criu.runtimeSingleThreadMode, Integer.MAX_VALUE, false, false);
        TestCheckpointHook h4 = new TestCheckpointHook(null, null, criu.runtimeSingleThreadMode, -100, true, false);
        TestCheckpointHook h5 = new TestCheckpointHook(null, null, criu.runtimeSingleThreadMode, 0, true, false);
        TestCheckpointHook h6 = new TestCheckpointHook(null, null, criu.runtimeSingleThreadMode, Integer.MAX_VALUE, true, false);
        TestCheckpointHook cracHooks = new TestCheckpointHook(null, null, criu.runtimeSingleThreadMode, 0, true, true);
        setHooks(checkpoint, h1, h2, h3, cracHooks, h4, h5, h6);

        checkDirectory(checkpoint, criu, locAdmin);
        List<TestCheckpointHook> singleThreadedHooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : singleThreadedHooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Checkpoint failed called", false, hook.checkpointFailed);
            assertEquals("Prepare called while not single threaded.", true, hook.prepareCalledSingleThreaded);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertEquals("Restore called while not single threaded.", true, hook.restoreCalledSingleThreaded);
        }
        List<TestCheckpointHook> multiThreadedhooks = getHooks(h4, h5, h6, cracHooks);
        for (TestCheckpointHook hook : multiThreadedhooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Checkpoint failed called", false, hook.checkpointFailed);
            assertEquals("Prepare called while single threaded.", false, hook.prepareCalledSingleThreaded);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertEquals("Restore called while single threaded.", false, hook.restoreCalledSingleThreaded);
        }

        List<TestCheckpointHook> expectedPrepareOrder = Arrays.asList(cracHooks, h6, h5, h4, h3, h2, h1);
        assertEquals("Wrong prepare order", expectedPrepareOrder, TestCheckpointHook.prepareOrder);

        List<TestCheckpointHook> expectedRestoreOrder = Arrays.asList(h1, h2, h3, h4, h5, h6, cracHooks);
        assertEquals("Wrong restore order", expectedRestoreOrder, TestCheckpointHook.restoreOrder);
    }

    @Test
    public void testPrepareException() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.AFTER_APP_START);

        RuntimeException prepareException = new RuntimeException("prepare exception test.");
        TestCheckpointHook h1 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h2 = new TestCheckpointHook(prepareException, null, criu.runtimeSingleThreadMode, 0, false, false);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        setHooks(checkpoint, h1, h2, h3);

        try {
            checkpoint.checkpoint(CheckpointPhase.AFTER_APP_START);
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.LIBERTY_PREPARE_FAILED, e.getType());
            assertEquals("Wrong cause.", prepareException, e.getCause());
        }
        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
        }
        assertEquals("Prepare not called.", true, h1.prepareCalled);
        assertEquals("Prepare not called.", true, h2.prepareCalled);
        assertEquals("Unexpected Prepare called.", false, h3.prepareCalled);
        // we expect all hook prepareFailed to be called
        assertEquals("Checkpoint Failed not called.", true, h1.checkpointFailed);
        assertEquals("Checkpoint Failed not called.", true, h2.checkpointFailed);
        assertEquals("Checkpoint Failed not called.", true, h3.checkpointFailed);

        assertNull("Unexpected call to criu", criu.imageDir);
    }

    @Test
    public void testAbortPrepareFromFailedDump() {
        TestCRIU criu = new TestCRIU();
        criu.throwIOException = true;
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new ComponentContextHandler()), criu, locAdmin, CheckpointPhase.AFTER_APP_START);

        TestCheckpointHook h1 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h2 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        setHooks(checkpoint, h1, h2, h3);

        checkFailDump(checkpoint, criu, locAdmin);

        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Checkpoint Failed not called.", true, hook.checkpointFailed);
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
        }
    }

    @Test
    public void testRestoreException() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();

        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new ComponentContextHandler()), criu, locAdmin, CheckpointPhase.AFTER_APP_START);

        RuntimeException restoreException = new RuntimeException("restore exception test.");
        TestCheckpointHook h1 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        TestCheckpointHook h2 = new TestCheckpointHook(null, restoreException, criu.runtimeSingleThreadMode, 0, false, false);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.runtimeSingleThreadMode, false, false);
        setHooks(checkpoint, h1, h2, h3);

        try {
            checkpoint.checkpoint(CheckpointPhase.AFTER_APP_START);
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.LIBERTY_RESTORE_FAILED, e.getType());
            assertEquals("Wrong cause.", restoreException, e.getCause());
        }
        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Checkpoint failed called.", false, hook.checkpointFailed);
        }
        assertEquals("Restore not called.", true, h3.restoreCalled);
        assertEquals("Restore not called.", true, h2.restoreCalled);
        assertEquals("Unexpected Restore called.", false, h1.restoreCalled);

        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    @Test
    public void testCheckpointAfterAppStart() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        ComponentContextHandler contextHandler = new ComponentContextHandler();

        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(contextHandler), criu, locAdmin, CheckpointPhase.AFTER_APP_START);

        assertNull("Should not have running condition yet.", contextHandler.runningCondition.get());
        checkpoint.check();
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
        assertNotNull("Should have running condition now", contextHandler.runningCondition.get());
    }

    public void testCheckpointBeforeAppStart() throws CheckpointFailedException, IllegalClassFormatException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        ComponentContextHandler contextHandler = new ComponentContextHandler();
        new CheckpointImpl(createComponentContext(contextHandler), criu, locAdmin, CheckpointPhase.BEFORE_APP_START);

        assertNull("Should not have running condition yet.", contextHandler.runningCondition.get());
        ClassFileTransformer transformer = contextHandler.transformer.get();
        assertNotNull("Null transformer", transformer);
        transformer.transform(getClass().getClassLoader(), "test", getClass(), null, new byte[] {});
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
        assertNotNull("Should have running condition now", contextHandler.runningCondition.get());
    }

    @Test
    public void testMultipleCheckpoints() {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        checkpoint.check();

        assertTrue("Expected to have called checkpoint", checkpoint.checkpointCalledAlready());
        checkpoint.check();
        assertTrue("Expected to have called checkpoint", checkpoint.checkpointCalledAlready());
        checkpoint.resetCheckpointCalled();
        assertTrue("Expected to have reset checkpoint", !checkpoint.checkpointCalledAlready());
        checkpoint.check();
        assertTrue("Expected to have called checkpoint", checkpoint.checkpointCalledAlready());
    }

    @Test
    public void testUnsupportedFeature() {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        ComponentContextHandler contextHandler = new ComponentContextHandler(new HashSet<>(Arrays.asList("notSupported1", "notSupported2")));
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(contextHandler), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        try {
            checkpoint.checkpoint(CheckpointPhase.AFTER_APP_START);
        } catch (CheckpointFailedException e) {
            assertEquals("Unexpected type", Type.LIBERTY_PREPARE_FAILED, e.getType());
            String msg = e.getMessage();
            assertTrue(msg + " notSupported1", msg.contains("notSupported1"));
            assertTrue(msg + " notSupported2", msg.contains("notSupported2"));
        }
    }

    @Test
    public void testSupportedFeature() {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        ComponentContextHandler contextHandler = new ComponentContextHandler(new HashSet<>(Arrays.asList("supported1", "supported2")));
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(contextHandler), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        checkpoint.checkpoint(CheckpointPhase.AFTER_APP_START);
    }

    @Test
    public void testPauseRestore() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        ComponentContextHandler contextHandler = new ComponentContextHandler();
        contextHandler.putContextProperty(CheckpointImpl.CHECKPOINT_PAUSE_RESTORE, "5000");
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());

        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(contextHandler), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        long startTime = System.nanoTime();
        checkDirectory(checkpoint, criu, locAdmin);
        long totalTime = System.nanoTime() - startTime;
        // we just assume if it took more than 4 seconds then it paused.  We give 1 second buffer here
        assertTrue("checkpoint did not take long enough: " + totalTime, TimeUnit.NANOSECONDS.toSeconds(totalTime) > 4);

        // test non-number
        contextHandler.putContextProperty(CheckpointImpl.CHECKPOINT_PAUSE_RESTORE, "badvalue");
        // create new checkpoint object to force read of property again
        checkpoint = new CheckpointImpl(createComponentContext(contextHandler), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        startTime = System.nanoTime();
        checkDirectory(checkpoint, criu, locAdmin);
        totalTime = System.nanoTime() - startTime;
        assertTrue("checkpoint took too long: " + totalTime, TimeUnit.NANOSECONDS.toSeconds(totalTime) < 2);

        // test negative number
        contextHandler.putContextProperty(CheckpointImpl.CHECKPOINT_PAUSE_RESTORE, "-5000");
        // create new checkpoint object to force read of property again
        checkpoint = new CheckpointImpl(createComponentContext(contextHandler), criu, locAdmin, CheckpointPhase.AFTER_APP_START);
        startTime = System.nanoTime();
        checkDirectory(checkpoint, criu, locAdmin);
        totalTime = System.nanoTime() - startTime;
        assertTrue("checkpoint took too long: " + totalTime, TimeUnit.NANOSECONDS.toSeconds(totalTime) < 2);
    }

    private List<TestCheckpointHook> getHooks(TestCheckpointHook... hooks) {
        List<TestCheckpointHook> hookList = new ArrayList<>();
        for (TestCheckpointHook hook : hooks) {
            hookList.add(hook);
        }
        return hookList;
    }

    private void checkDirectory(CheckpointImpl checkpoint, TestCRIU criu, WsLocationAdmin locAdmin) throws CheckpointFailedException {
        checkpoint.checkpoint(CheckpointPhase.AFTER_APP_START);
        assertTrue("Wrong file.", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    private void checkFailDump(CheckpointImpl checkpoint, TestCRIU criu, WsLocationAdmin locAdmin) {
        criu.throwIOException = true;
        try {
            checkpoint.checkpoint(CheckpointPhase.AFTER_APP_START);
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.SYSTEM_CHECKPOINT_FAILED, e.getType());
            assertTrue("Wrong cause.", e.getCause() instanceof IOException);
        }
        assertTrue("Wrong file.", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }
}
