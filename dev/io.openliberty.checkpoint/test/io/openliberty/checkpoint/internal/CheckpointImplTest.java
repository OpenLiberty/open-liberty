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

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import test.common.SharedLocationManager;

/**
 *
 */
public class CheckpointImplTest {

    private static final String testbuildDir = System.getProperty("test.buildDir", "generated");
    private CompletionListener<Boolean> completionListener;

    static class Hooks implements InvocationHandler {
        final Object[] hooks;

        public Hooks(Object[] hooks) {
            super();
            this.hooks = hooks;;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("locateServices".equals(method.getName())) {
                return hooks;
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

    static class TestCheckpointHook implements CheckpointHook {
        final RuntimeException prepareException;
        final RuntimeException restoreException;

        public TestCheckpointHook() {
            prepareException = null;
            restoreException = null;
        }

        public TestCheckpointHook(RuntimeException prepareException, RuntimeException restoreException) {
            this.prepareException = prepareException;
            this.restoreException = restoreException;
        }

        volatile boolean prepareCalled = false;
        volatile boolean restoreCalled = false;

        @Override
        public void prepare() {
            prepareCalled = true;
            if (prepareException != null) {
                throw prepareException;
            }
        }

        @Override
        public void restore() {
            restoreCalled = true;
            if (restoreException != null) {
                throw restoreException;
            }
        }
    }

    private ComponentContext createComponentContext(CheckpointHook... hooks) {
        if (hooks.length == 0) {
            hooks = null;
        }
        return (ComponentContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ComponentContext.class }, new Hooks(hooks));
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
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.FEATURES);
        checkDirectory(checkpoint, criu, locAdmin);
        checkpoint.resetCheckpointCalled();
        checkFailDump(checkpoint, criu, locAdmin);
    }

    @Test
    public void testNullHook() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test2");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.APPLICATIONS);
        checkDirectory(checkpoint, criu, locAdmin);
    }

    @Test
    public void testPrepareRestore() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook h1 = new TestCheckpointHook();
        TestCheckpointHook h2 = new TestCheckpointHook();
        TestCheckpointHook h3 = new TestCheckpointHook();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test3");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(h1, h2, h3), criu, locAdmin, CheckpointPhase.APPLICATIONS);

        checkDirectory(checkpoint, criu, locAdmin);
        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Restore not called.", true, hook.restoreCalled);
        }
    }

    @Test
    public void testPrepareException() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        RuntimeException prepareException = new RuntimeException("prepare exception test.");
        TestCheckpointHook h1 = new TestCheckpointHook();
        TestCheckpointHook h2 = new TestCheckpointHook(prepareException, null);
        TestCheckpointHook h3 = new TestCheckpointHook();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test4");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(h1, h2, h3), criu, locAdmin, CheckpointPhase.APPLICATIONS);

        try {
            checkpoint.checkpoint();
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.PREPARE_ABORT, e.getType());
            assertEquals("Wrong cause.", prepareException, e.getCause());
        }
        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
        }
        assertEquals("Prepare not called.", true, h1.prepareCalled);

        assertEquals("Prepare not called.", true, h2.prepareCalled);

        assertEquals("Unexpected Prepare called.", false, h3.prepareCalled);

        assertNull("Unexpected call to criu", criu.imageDir);
    }

    @Test
    public void testAbortPrepareFromFailedDump() {
        TestCRIU criu = new TestCRIU();
        criu.throwIOException = true;
        TestCheckpointHook h1 = new TestCheckpointHook();
        TestCheckpointHook h2 = new TestCheckpointHook();
        TestCheckpointHook h3 = new TestCheckpointHook();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test5");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(h1, h2, h3), criu, locAdmin, CheckpointPhase.FEATURES);

        checkFailDump(checkpoint, criu, locAdmin);

        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
        }
    }

    @Test
    public void testRestoreException() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        RuntimeException restoreException = new RuntimeException("restore exception test.");
        TestCheckpointHook h1 = new TestCheckpointHook();
        TestCheckpointHook h2 = new TestCheckpointHook(null, restoreException);
        TestCheckpointHook h3 = new TestCheckpointHook();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test6");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(h1, h2, h3), criu, locAdmin, CheckpointPhase.APPLICATIONS);

        try {
            checkpoint.checkpoint();
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.RESTORE_ABORT, e.getType());
            assertEquals("Wrong cause.", restoreException, e.getCause());
        }
        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
        }
        assertEquals("Restore not called.", true, h3.restoreCalled);

        assertEquals("Restore not called.", true, h2.restoreCalled);

        assertEquals("Unexpected Restore called.", false, h1.restoreCalled);

        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    @Test
    public void testCheckpointApplications() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook hook = new TestCheckpointHook();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test7");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(hook), criu, locAdmin, CheckpointPhase.APPLICATIONS);
        checkpoint.check();
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    @Test
    public void testCheckpointFeatures() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook hook = new TestCheckpointHook();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test8");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(hook), criu, locAdmin, CheckpointPhase.FEATURES);
        RuntimeUpdateNotification trn = createRuntimeUpdateNotification();
        checkpoint.notificationCreated(null, trn);
        CompletionListener<Boolean> cl = getCompletionListener();
        assertNotNull("Expected to have called onCompletion", cl);
        cl.successfulCompletion(createTestFuture(), Boolean.TRUE);
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
    }

    @Test
    public void testMultipleCheckpoints() {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook hook = new TestCheckpointHook();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, "test9");
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(hook), criu, locAdmin, CheckpointPhase.FEATURES);
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

    private List<TestCheckpointHook> getHooks(TestCheckpointHook... hooks) {
        List<TestCheckpointHook> hookList = new ArrayList<>();
        for (TestCheckpointHook hook : hooks) {
            hookList.add(hook);
        }
        return hookList;
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
