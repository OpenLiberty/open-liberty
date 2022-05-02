/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.condition.Condition;

import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
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
    private CompletionListener<Boolean> completionListener;

    static class Hooks implements InvocationHandler {
        final List<CheckpointHook> singleThreadedHooks;
        final List<CheckpointHook> multiThreadedHooks;
        final AtomicReference<ClassFileTransformer> transformer = new AtomicReference<>();
        final AtomicReference<Condition> runningCondition = new AtomicReference<>();

        public Hooks(List<CheckpointHook> singleThreadedHooks, List<CheckpointHook> multiThreadedHooks) {
            super();
            this.singleThreadedHooks = singleThreadedHooks;
            this.multiThreadedHooks = multiThreadedHooks;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("locateServices".equals(method.getName())) {
                if (CheckpointImpl.HOOKS_REF_NAME_SINGLE_THREAD.equals(args[0])) {
                    return singleThreadedHooks.toArray();
                }
                if (CheckpointImpl.HOOKS_REF_NAME_MULTI_THREAD.equals(args[0])) {
                    return multiThreadedHooks.toArray();
                }
            }
            if ("getBundleContext".equals(method.getName())) {
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { BundleContext.class }, (p, m, a) -> {
                    if ("getProperty".equals(m.getName())) {
                        return null;
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
                    throw new UnsupportedOperationException(m.getName());
                });
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    static class TestCRIU implements ExecuteCRIU {
        final AtomicBoolean singleThreaded;
        volatile File imageDir = null;
        volatile String logFilename = null;
        volatile File workDir = null;
        volatile boolean throwIOException = false;

        public TestCRIU() {
            this(new AtomicBoolean());
        }

        TestCRIU(AtomicBoolean singleThreaded) {
            this.singleThreaded = singleThreaded;
        }

        @Override
        public void dump(Runnable prepare, Runnable restore, File imageDir, String logFileName, File workDir, File envProps,
                         boolean unprivileged) throws CheckpointFailedException {
            singleThreaded.set(true);
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
                singleThreaded.set(false);
            }
        }

        @Override
        public void checkpointSupported() {
        }
    }

    static class TestCheckpointHook implements CheckpointHook {
        final RuntimeException prepareException;
        final RuntimeException restoreException;
        final AtomicBoolean singleThreaded;

        public TestCheckpointHook(AtomicBoolean singleThreaded) {
            this(null, null, singleThreaded);
        }

        public TestCheckpointHook(RuntimeException prepareException, RuntimeException restoreException, AtomicBoolean singleThreaded) {
            this.prepareException = prepareException;
            this.restoreException = restoreException;
            this.singleThreaded = singleThreaded;
        }

        volatile boolean prepareCalled = false;
        volatile boolean prepareCalledSingleThreaded = false;
        volatile boolean restoreCalled = false;
        volatile boolean restoreCalledSingleThreaded = false;

        @Override
        public void prepare() {
            prepareCalled = true;
            prepareCalledSingleThreaded = singleThreaded.get();
            if (prepareException != null) {
                throw prepareException;
            }
        }

        @Override
        public void restore() {
            restoreCalled = true;
            restoreCalledSingleThreaded = singleThreaded.get();
            if (restoreException != null) {
                throw restoreException;
            }
        }
    }

    private ComponentContext createComponentContext() {
        return createComponentContext(new Hooks(emptyList(), emptyList()));
    }

    private ComponentContext createComponentContext(Hooks hooks) {
        return (ComponentContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ComponentContext.class }, hooks);
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
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.FEATURES);
        checkDirectory(checkpoint, criu, locAdmin);
        checkpoint.resetCheckpointCalled();
        checkFailDump(checkpoint, criu, locAdmin);
    }

    @Test
    public void testNullHook() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, locAdmin, CheckpointPhase.APPLICATIONS);
        checkDirectory(checkpoint, criu, locAdmin);
    }

    @Test
    public void testPrepareRestore() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook h1 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h2 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new Hooks(asList(h1, h2, h3), emptyList())), criu, locAdmin, CheckpointPhase.APPLICATIONS);

        checkDirectory(checkpoint, criu, locAdmin);
        List<TestCheckpointHook> hooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Restore not called.", true, hook.restoreCalled);
        }
    }

    @Test
    public void testPrepareRestoreWithMultiThreadedHooks() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook h1 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h2 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h4 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h5 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h6 = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new Hooks(asList(h1, h2, h3), asList(h4, h5, h6))), criu, locAdmin, CheckpointPhase.APPLICATIONS);

        checkDirectory(checkpoint, criu, locAdmin);
        List<TestCheckpointHook> singleThreadedHooks = getHooks(h1, h2, h3);
        for (TestCheckpointHook hook : singleThreadedHooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Prepare called while not single threaded.", true, hook.prepareCalledSingleThreaded);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertEquals("Restore called while not single threaded.", true, hook.restoreCalledSingleThreaded);
        }
        List<TestCheckpointHook> multiThreadedhooks = getHooks(h4, h5, h6);
        for (TestCheckpointHook hook : multiThreadedhooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertEquals("Prepare called while single threaded.", false, hook.prepareCalledSingleThreaded);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertEquals("Restore called while single threaded.", false, hook.restoreCalledSingleThreaded);
        }
    }

    @Test
    public void testPrepareException() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        RuntimeException prepareException = new RuntimeException("prepare exception test.");
        TestCheckpointHook h1 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h2 = new TestCheckpointHook(prepareException, null, criu.singleThreaded);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new Hooks(asList(h1, h2, h3), emptyList())), criu, locAdmin, CheckpointPhase.APPLICATIONS);

        try {
            checkpoint.checkpoint();
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

        assertNull("Unexpected call to criu", criu.imageDir);
    }

    @Test
    public void testAbortPrepareFromFailedDump() {
        TestCRIU criu = new TestCRIU();
        criu.throwIOException = true;
        TestCheckpointHook h1 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h2 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new Hooks(asList(h1, h2, h3), emptyList())), criu, locAdmin, CheckpointPhase.FEATURES);

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
        TestCheckpointHook h1 = new TestCheckpointHook(criu.singleThreaded);
        TestCheckpointHook h2 = new TestCheckpointHook(null, restoreException, criu.singleThreaded);
        TestCheckpointHook h3 = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new Hooks(asList(h1, h2, h3), emptyList())), criu, locAdmin, CheckpointPhase.APPLICATIONS);

        try {
            checkpoint.checkpoint();
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailedException e) {
            assertEquals("Wrong type.", Type.LIBERTY_RESTORE_FAILED, e.getType());
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
        TestCheckpointHook hook = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        Hooks hooks = new Hooks(asList(hook), emptyList());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(hooks), criu, locAdmin, CheckpointPhase.APPLICATIONS);
        assertNull("Should not have running condition yet.", hooks.runningCondition.get());
        checkpoint.check();
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
        assertNotNull("Should have running condition now", hooks.runningCondition.get());
    }

    @Test
    public void testCheckpointFeatures() throws CheckpointFailedException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook hook = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        Hooks hooks = new Hooks(asList(hook), emptyList());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(hooks), criu, locAdmin, CheckpointPhase.FEATURES);
        assertNull("Should not have running condition yet.", hooks.runningCondition.get());
        RuntimeUpdateNotification trn = createRuntimeUpdateNotification();
        checkpoint.notificationCreated(null, trn);
        CompletionListener<Boolean> cl = getCompletionListener();
        assertNotNull("Expected to have called onCompletion", cl);
        cl.successfulCompletion(createTestFuture(), Boolean.TRUE);
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
        assertNotNull("Should have running condition now", hooks.runningCondition.get());
    }

    public void testCheckpointDeployment() throws CheckpointFailedException, IllegalClassFormatException {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook hook = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        Hooks hooks = new Hooks(asList(hook), emptyList());
        new CheckpointImpl(createComponentContext(hooks), criu, locAdmin, CheckpointPhase.DEPLOYMENT);
        assertNull("Should not have running condition yet.", hooks.runningCondition.get());
        ClassFileTransformer transformer = hooks.transformer.get();
        assertNotNull("Null transformer", transformer);
        transformer.transform(getClass().getClassLoader(), "test", getClass(), null, new byte[] {});
        assertTrue("Expected to have called criu", criu.imageDir.getAbsolutePath().contains(locAdmin.getServerName()));
        assertNotNull("Should have running condition now", hooks.runningCondition.get());
    }

    @Test
    public void testMultipleCheckpoints() {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHook hook = new TestCheckpointHook(criu.singleThreaded);
        WsLocationAdmin locAdmin = (WsLocationAdmin) SharedLocationManager.createLocations(testbuildDir, testName.getMethodName());
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(new Hooks(asList(hook), emptyList())), criu, locAdmin, CheckpointPhase.FEATURES);
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
