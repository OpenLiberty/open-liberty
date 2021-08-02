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

import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import io.openliberty.checkpoint.internal.CheckpointFailed.Type;
import io.openliberty.checkpoint.internal.CheckpointImplTest.TestCheckpointHookFactory.TestCheckpointHook;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointHookFactory;
import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;

/**
 *
 */
public class CheckpointImplTest {
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
            throw new UnsupportedOperationException(method.getName());
        }
    }

    static class TestCRIU implements ExecuteCRIU {
        volatile File directory = null;
        volatile boolean throwIOException = false;

        @Override
        public int dump(File directory) throws IOException {
            this.directory = directory;
            if (throwIOException) {
                throw new IOException("Test exception thrown from TestCRIU");
            }
            return 0;
        }

        @Override
        public boolean isCheckpointSupported() {
            return true;
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

    private ComponentContext createComponentContext(Object... factories) {
        if (factories.length == 0) {
            factories = null;
        }
        return (ComponentContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ComponentContext.class }, new Factories(factories));
    }

    @Test
    public void testNullFactories() throws CheckpointFailed {
        TestCRIU criu = new TestCRIU();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu, false, false);
        checkDirectory(Phase.FEATURES, checkpoint, criu);
        checkFailDump(Phase.FEATURES, checkpoint, criu);
    }

    @Test
    public void testNullHook() throws CheckpointFailed {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHookFactory factory = new TestCheckpointHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(factory), criu, false, false);
        checkPhase(factory, checkpoint, criu);
    }

    @Test
    public void testPrepareRestore() throws CheckpointFailed {
        TestCRIU criu = new TestCRIU();
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, false, false);

        checkDirectory(Phase.APPLICATIONS, checkpoint, criu);
        List<TestCheckpointHook> hooks = getHooks(f1, f2, f3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertNull("Unexpected Prepare Exception.", hook.abortPrepareCause);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertNull("Unexpected Restore Exception.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testPrepareException() throws CheckpointFailed {
        TestCRIU criu = new TestCRIU();
        RuntimeException prepareException = new RuntimeException("prepare exception test.");
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory(prepareException, null);
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, false, false);

        try {
            checkpoint.checkpoint(Phase.APPLICATIONS, new File("test"));
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailed e) {
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

        assertNull("Unexpected call to criu", criu.directory);
    }

    @Test
    public void testAbortPrepareFromFailedDump() {
        TestCRIU criu = new TestCRIU();
        criu.throwIOException = true;
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, false, false);

        checkFailDump(Phase.FEATURES, checkpoint, criu);

        List<TestCheckpointHook> hooks = getHooks(f1, f2, f3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertTrue("Unexpected Prepare Exception.", hook.abortPrepareCause instanceof IOException);
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
            assertNull("Unexpected Restore Exception.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testRestoreException() throws CheckpointFailed {
        TestCRIU criu = new TestCRIU();
        RuntimeException restoreException = new RuntimeException("restore exception test.");
        TestCheckpointHookFactory f1 = new TestCheckpointHookFactory();
        TestCheckpointHookFactory f2 = new TestCheckpointHookFactory(null, restoreException);
        TestCheckpointHookFactory f3 = new TestCheckpointHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu, false, false);

        File test = new File("test");
        try {
            checkpoint.checkpoint(Phase.APPLICATIONS, test);
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailed e) {
            assertEquals("Wrong type.", Type.RESTORE_ABORT, e.getType());
            assertEquals("Wrong cause.", restoreException, e.getCause());
        }
        List<TestCheckpointHook> hooks = getHooks(f1, f2, f3);
        for (TestCheckpointHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertNull("Unexpected Prepare Exception.", hook.abortPrepareCause);
        }
        assertEquals("Restore not called.", true, f1.hook.restoreCalled);
        assertEquals("Wrong cause.", restoreException, f1.hook.abortRestoreCause);

        assertEquals("Restore not called.", true, f2.hook.restoreCalled);
        assertNull("Wrong cause.", f2.hook.abortRestoreCause);

        assertEquals("Unexpected Restore called.", false, f3.hook.restoreCalled);
        assertNull("Wrong cause.", f3.hook.abortRestoreCause);

        assertEquals("Expected to have called criu", test, criu.directory);
    }

    private List<TestCheckpointHook> getHooks(TestCheckpointHookFactory... factories) {
        List<TestCheckpointHook> hooks = new ArrayList<>();
        for (TestCheckpointHookFactory factory : factories) {
            hooks.add(factory.hook);
        }
        return hooks;
    }

    private void checkPhase(TestCheckpointHookFactory factory, CheckpointImpl checkpoint, TestCRIU criu) throws CheckpointFailed {
        checkDirectory(Phase.APPLICATIONS, checkpoint, criu);
        assertEquals("Wrong phase.", Phase.APPLICATIONS, factory.phase);
//        checkFailDump(Phase.SERVER, checkpoint, criu);
//        assertEquals("Wrong phase.", Phase.SERVER, factory.phase);

    }

    private void checkDirectory(Phase phase, CheckpointImpl checkpoint, TestCRIU criu) throws CheckpointFailed {
        File test1 = new File("test1");
        checkpoint.checkpoint(phase, test1);
        assertEquals("Wrong file.", test1, criu.directory);
    }

    private void checkFailDump(Phase phase, CheckpointImpl checkpoint, TestCRIU criu) {
        File test2 = new File("test2");
        criu.throwIOException = true;
        try {
            checkpoint.checkpoint(phase, test2);
            fail("Expected CheckpointFailed exception.");
        } catch (CheckpointFailed e) {
            assertEquals("Wrong type.", Type.SNAPSHOT_FAILED, e.getType());
            assertTrue("Wrong cause.", e.getCause() instanceof IOException);
        }
        assertEquals("Wrong file.", test2, criu.directory);
    }
}
