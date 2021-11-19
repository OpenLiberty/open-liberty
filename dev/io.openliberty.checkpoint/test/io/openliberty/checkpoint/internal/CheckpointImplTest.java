/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import io.openliberty.checkpoint.internal.CheckpointImplTest.TestCheckpointHookFactory.TestCheckpointHook;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointHookFactory;
import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;
import test.common.SharedLocationManager;

/**
 *
 */
public class CheckpointImplTest {

    private static final String testbuildDir = System.getProperty("test.buildDir", "generated");
    private CompletionListener<Boolean> completionListener;

    static class Factories implements InvocationHandler {
        final Object[] factories;

        public Factories(Object[] factories) {
            super();
            this.factories = factories;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("locateServices".equals(method.getName())) {
                return factories;
            }
            if ("getBundleContext".equals(method.getName())) {
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { BundleContext.class }, (p, m, a) -> {
                    if ("getProperty".equals(m.getName())) {
                        return null;
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    static class TestCRIU implements ExecuteCRIU {
        volatile File imageDir = null;
        volatile String logFilename = null;
        volatile File workDir = null;
        volatile boolean throwIOException = false;

        @Override
        public void dump(File imageDir, String logFileName, File workDir, File envProps) throws CheckpointFailedException {
            this.imageDir = imageDir;
            this.logFilename = logFileName;
            this.workDir = workDir;
            if (throwIOException) {
                throw new CheckpointFailedException(Type.SYSTEM_CHECKPOINT_FAILED, "Test failure", new IOException("failed"), 22);
            }
        }

        @Override
        public void checkpointSupported() {
        }
    }

    static class TestCheckpointHookFactory implements CheckpointHookFactory {
        final RuntimeException prepareException;
        final RuntimeException restoreException;

        public TestCheckpointHookFactory() {
            prepareException = null;
            restoreException = null;
        }

        public TestCheckpointHookFactory(RuntimeException prepareException, RuntimeException restoreException) {
            this.prepareException = prepareException;
            this.restoreException = restoreException;
        }

        volatile boolean nullHook = false;
        volatile TestCheckpointHook hook = null;
        volatile Phase phase;

        class TestCheckpointHook implements CheckpointHook {
            volatile boolean prepareCalled = false;
            volatile Exception abortPrepareCause = null;
            volatile boolean restoreCalled = false;
            volatile Exception abortRestoreCause = null;

            @Override
            public void prepare() {
                prepareCalled = true;
                if (prepareException != null) {
                    throw prepareException;
                }
            }

            @Override
            public void abortPrepare(Exception cause) {
                abortPrepareCause = cause;
            }

            @Override
            public void restore() {
                restoreCalled = true;
                if (restoreException != null) {
                    throw restoreException;
                }
            }

            @Override
            public void abortRestore(Exception cause) {
                abortRestoreCause = cause;
            }
        }

        @Override
        public CheckpointHook create(Phase phase) {
            this.phase = phase;
            if (nullHook) {
                return null;
            }
            return hook = new TestCheckpointHook();
        }
    }

    private ComponentContext createComponentContext(CheckpointHookFactory... factories) {
        if (factories.length == 0) {
            factories = null;
        }
        return (ComponentContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ComponentContext.class }, new Factories(factories));
    }

    private RuntimeUpdateNotification createRuntimeUpdateNotification() {
        return (RuntimeUpdateNotification) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { RuntimeUpdateNotification.class }, new InvocationHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                if ("getName".equals(method.getName())) {
                    return RuntimeUpdateNotification.APPLICATIONS_STARTING;
                }
                if ("onCompletion".equals(method.getName())) {
                    completionListener = (CompletionListener<Boolean>) args[0];
                    return true;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        });

    }

    @SuppressWarnings("unchecked")
    private Future<Boolean> createTestFuture() {
        return (Future<Boolean>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Future.class }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("isDone".equals(method.getName())) {
                    return true;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        });
    }

    @Test
    public void testNullFactories() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test1");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, Phase.FEATURES);
        checkDirectory(checkpoint, criu, locAdmin);
        checkpoint.resetCheckpointCalled();
        checkFailDump(checkpoint, criu, locAdmin);
    }

    @Test
    public void testNullHook() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHookFactory factory = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test2");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(factory), criu, locAdmin, Phase.APPLICATIONS);
        checkPhase(factory, checkpoint, criu, locAdmin);
    }

    @Test
    public void testPrepareRestore() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test3");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, locAdmin, Phase.APPLICATIONS);

        checkDirectory(checkpoint, criu, locAdmin);
        List<TestCheckpointHook> hooks = getHooks(f1, f2, f3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertNull("Unexpected Prepare Exception.", hook.abortPrepareCause);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertNull("Unexpected Restore Exception.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testPrepareException() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        RuntimeException prepareException = new RuntimeException("prepare exception test.");
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory(prepareException, null);
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test4");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, locAdmin, Phase.APPLICATIONS);

        try {
            checkpoint.checkpoint();
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.PREPARE_ABORT, e.getType());
            assertEquals("Wrong cause.", prepareException, e.getCause());
        }
        List<TestCheckpointHook> hooks = getHooks(f1, f2, f3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
            assertNull("Unexpected Restore Exception.", hook.abortRestoreCause);
        }
        assertEquals("Prepare not called.", true, f1.hook.prepareCalled);
        assertEquals("Wrong cause.", prepareException, f1.hook.abortPrepareCause);

        assertEquals("Prepare not called.", true, f2.hook.prepareCalled);
        assertNull("Wrong cause.", f2.hook.abortPrepareCause);

        assertEquals("Unexpected Prepare called.", false, f3.hook.prepareCalled);
        assertNull("Wrong cause.", f3.hook.abortPrepareCause);

        assertNull("Unexpected call to criu", criu.imageDir);
    }

    @Test
    public void testAbortPrepareFromFailedDump() {
        TestCRIU criu = new TestCRIU();
        criu.throwIOException = true;
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test5");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, locAdmin, Phase.FEATURES);

        checkFailDump(checkpoint, criu, locAdmin);

        List<TestCheckpointHook> hooks = getHooks(f1, f2, f3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertTrue("Unexpected Prepare Exception: " + hook.abortPrepareCause.getCause(), hook.abortPrepareCause.getCause() instanceof IOException);
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
            assertNull("Unexpected Restore Exception.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testRestoreException() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        RuntimeException restoreException = new RuntimeException("restore exception test.");
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory(null, restoreException);
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test6");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, locAdmin, Phase.APPLICATIONS);

        try {
            checkpoint.checkpoint();
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.RESTORE_ABORT, e.getType());
            assertEquals("Wrong cause.", restoreException, e.getCause());
        }
        List<TestCheckpointHook> hooks = getHooks(f1, f2, f3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertNull("Unexpected Prepare Exception.", hook.abortPrepareCause);
        }
        assertEquals("Restore not called.", true, f3.hook.restoreCalled);
        assertEquals("Wrong cause.", restoreException, f3.hook.abortRestoreCause);

        assertEquals("Restore not called.", true, f2.hook.restoreCalled);
        assertNull("Wrong cause.", f2.hook.abortRestoreCause);

        assertEquals("Unexpected Restore called.", false, f1.hook.restoreCalled);
        assertNull("Wrong cause.", f1.hook.abortRestoreCause);

        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    @Test
    public void testCheckpointApplications() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHookFactory factory = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test7");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(factory), criu, locAdmin, Phase.APPLICATIONS);
        checkpoint.check();
        assertEquals("Wrong phase.", Phase.APPLICATIONS, factory.phase);
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    @Test
    public void testCheckpointFeatures() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHookFactory factory = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test8");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(factory), criu, locAdmin, Phase.FEATURES);
        RuntimeUpdateNotification trn = createRuntimeUpdateNotification();
        checkpoint.notificationCreated(null, trn);
        CompletionListener<Boolean> cl = getCompletionListener();
        assertNotNull("Expected to have called onCompletion", cl);
        cl.successfulCompletion(createTestFuture(), Boolean.TRUE);
        assertEquals("Wrong phase.", Phase.FEATURES, factory.phase);
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    @Test
    public void testMultipleCheckpoints() {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHookFactory factory = new TestCheckpointHookFactory();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test9");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(factory), criu, locAdmin, Phase.FEATURES);
        checkpoint.check();
        assertTrue("Expected to have called checkpoint", checkpoint.checkpointCalledAlready());
        checkpoint.check();
        assertTrue("Expected to have called checkpoint", checkpoint.checkpointCalledAlready());
        checkpoint.resetCheckpointCalled();
        assertTrue("Expected to have reset checkpoint", !checkpoint.checkpointCalledAlready());
        checkpoint.check();
        assertTrue("Expected to have called checkpoint", checkpoint.checkpointCalledAlready());
    }

    private CompletionListener<Boolean> getCompletionListener() {
        return completionListener;
    }

    private List<TestCheckpointHook> getHooks(TestCheckpointHookFactory... factories) {
        List<TestCheckpointHook> hooks = new ArrayList<>();
        for (TestCheckpointHookFactory factory : factories) {
            hooks.add(factory.hook);
        }
        return hooks;
    }

    private void checkPhase(TestCheckpointHookFactory factory, CheckpointImpl checkpoint, TestCRIU criu, WsLocationAdmin locAdmin) throws CheckpointFailedException {
        checkDirectory(checkpoint, criu, locAdmin);
        assertEquals("Wrong phase.", Phase.APPLICATIONS, factory.phase);
    }

    private void checkDirectory(CheckpointImpl checkpoint, TestCRIU criu, WsLocationAdmin locAdmin) throws CheckpointFailedException {
        checkpoint.checkpoint();
        assertTrue("Wrong file.", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    private void checkFailDump(CheckpointImpl checkpoint, TestCRIU criu, WsLocationAdmin locAdmin) {
        criu.throwIOException = true;
        try {
            checkpoint.checkpoint();
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.SYSTEM_CHECKPOINT_FAILED, e.getType());
            assertTrue("Wrong cause.", e.getCause() instanceof IOException);
        }
        assertTrue("Wrong file.", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }
}
