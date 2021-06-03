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

import io.openliberty.checkpoint.internal.CheckpointImplTest.TestSnapshotHookFactory.TestSnapshotHook;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.Checkpoint.Phase;
import io.openliberty.checkpoint.spi.SnapshotFailed;
import io.openliberty.checkpoint.spi.SnapshotFailed.Type;
import io.openliberty.checkpoint.spi.SnapshotHook;
import io.openliberty.checkpoint.spi.SnapshotHookFactory;

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
        public void dump(File directory) throws IOException {
            this.directory = directory;
            if (throwIOException) {
                throw new IOException("Test exception thrown from TestCRIU");
            }
        }

    }

    static class TestSnapshotHookFactory implements SnapshotHookFactory {
        final RuntimeException prepareException;
        final RuntimeException restoreException;

        public TestSnapshotHookFactory() {
            prepareException = null;
            restoreException = null;
        }

        public TestSnapshotHookFactory(RuntimeException prepareException, RuntimeException restoreException) {
            this.prepareException = prepareException;
            this.restoreException = restoreException;
        }

        volatile boolean nullHook = false;
        volatile TestSnapshotHook hook = null;
        volatile Phase phase;

        class TestSnapshotHook implements SnapshotHook {
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
        public SnapshotHook create(Phase phase) {
            this.phase = phase;
            if (nullHook) {
                return null;
            }
            return hook = new TestSnapshotHook();
        }
    }

    private ComponentContext createComponentContext(Object... factories) {
        if (factories.length == 0) {
            factories = null;
        }
        return (ComponentContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ComponentContext.class }, new Factories(factories));
    }

    @Test
    public void testNullFactories() throws SnapshotFailed {
        TestCRIU criu = new TestCRIU();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu);
        checkDirectory(Phase.FEATURES, checkpoint, criu);
        checkFailDump(Phase.FEATURES, checkpoint, criu);
    }

    @Test
    public void testNullHook() throws SnapshotFailed {
        TestCRIU criu = new TestCRIU();
        TestSnapshotHookFactory factory = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(factory), criu);
        checkPhase(factory, checkpoint, criu);
    }

    @Test
    public void testPrepareRestore() throws SnapshotFailed {
        TestCRIU criu = new TestCRIU();
        TestSnapshotHookFactory f1 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f2 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f3 = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu);

        checkDirectory(Phase.APPLICATIONS, checkpoint, criu);
        List<TestSnapshotHook> hooks = getHooks(f1, f2, f3);
        for (TestSnapshotHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertNull("Unexpected Prepare Exception.", hook.abortPrepareCause);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertNull("Unexpected Restore Exception.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testPrepareException() throws SnapshotFailed {
        TestCRIU criu = new TestCRIU();
        RuntimeException prepareException = new RuntimeException("prepare exception test.");
        TestSnapshotHookFactory f1 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f2 = new TestSnapshotHookFactory(prepareException, null);
        TestSnapshotHookFactory f3 = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu);

        try {
            checkpoint.snapshot(Phase.APPLICATIONS, new File("test"));
            fail("Expected SnapshotFailed exception.");
        } catch (SnapshotFailed e) {
            assertEquals("Wrong type.", Type.PREPARE_ABORT, e.getType());
            assertEquals("Wrong cause.", prepareException, e.getCause());
        }
        List<TestSnapshotHook> hooks = getHooks(f1, f2, f3);
        for (TestSnapshotHook hook : hooks) {
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
        TestSnapshotHookFactory f1 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f2 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f3 = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu);

        checkFailDump(Phase.FEATURES, checkpoint, criu);

        List<TestSnapshotHook> hooks = getHooks(f1, f2, f3);
        for (TestSnapshotHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertTrue("Unexpected Prepare Exception.", hook.abortPrepareCause instanceof IOException);
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
            assertNull("Unexpected Restore Exception.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testRestoreException() throws SnapshotFailed {
        TestCRIU criu = new TestCRIU();
        RuntimeException restoreException = new RuntimeException("restore exception test.");
        TestSnapshotHookFactory f1 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f2 = new TestSnapshotHookFactory(null, restoreException);
        TestSnapshotHookFactory f3 = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu);

        File test = new File("test");
        try {
            checkpoint.snapshot(Phase.APPLICATIONS, test);
            fail("Expected SnapshotFailed exception.");
        } catch (SnapshotFailed e) {
            assertEquals("Wrong type.", Type.RESTORE_ABORT, e.getType());
            assertEquals("Wrong cause.", restoreException, e.getCause());
        }
        List<TestSnapshotHook> hooks = getHooks(f1, f2, f3);
        for (TestSnapshotHook hook : hooks) {
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

    private List<TestSnapshotHook> getHooks(TestSnapshotHookFactory... factories) {
        List<TestSnapshotHook> hooks = new ArrayList<>();
        for (TestSnapshotHookFactory factory : factories) {
            hooks.add(factory.hook);
        }
        return hooks;
    }

    private void checkPhase(TestSnapshotHookFactory factory, CheckpointImpl checkpoint, TestCRIU criu) throws SnapshotFailed {
        checkDirectory(Phase.APPLICATIONS, checkpoint, criu);
        assertEquals("Wrong phase.", Phase.APPLICATIONS, factory.phase);
//        checkFailDump(Phase.SERVER, checkpoint, criu);
//        assertEquals("Wrong phase.", Phase.SERVER, factory.phase);

    }

    private void checkDirectory(Phase phase, CheckpointImpl checkpoint, TestCRIU criu) throws SnapshotFailed {
        File test1 = new File("test1");
        checkpoint.snapshot(phase, test1);
        assertEquals("Wrong file.", test1, criu.directory);
    }

    private void checkFailDump(Phase phase, CheckpointImpl checkpoint, TestCRIU criu) {
        File test2 = new File("test2");
        criu.throwIOException = true;
        try {
            checkpoint.snapshot(phase, test2);
            fail("Expected SnapshotFailed exception.");
        } catch (SnapshotFailed e) {
            assertEquals("Wrong type.", Type.SNAPSHOT_FAILED, e.getType());
            assertTrue("Wrong cause.", e.getCause() instanceof IOException);
        }
        assertEquals("Wrong file.", test2, criu.directory);
    }
}
