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
import io.openliberty.checkpoint.spi.SnapshotHook;
import io.openliberty.checkpoint.spi.SnapshotHookFactory;
import io.openliberty.checkpoint.spi.SnapshotResult;
import io.openliberty.checkpoint.spi.SnapshotResult.SnapshotResultType;

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
        public SnapshotResult dump(File directory) {
            this.directory = directory;
            if (throwIOException) {
                return new SnapshotResult(SnapshotResultType.SNAPSHOT_FAILED, "Snapshot Failed", new IOException());
            }
            return new SnapshotResult(SnapshotResultType.SUCCESS, "Success", null);
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
            volatile SnapshotResult abortPrepareCause = null;
            volatile boolean restoreCalled = false;
            volatile SnapshotResult abortRestoreCause = null;

            @Override
            public void prepare() {
                prepareCalled = true;
                if (prepareException != null) {
                    throw prepareException;
                }
            }

            @Override
            public void abortPrepare(SnapshotResult result) {
                abortPrepareCause = result;
            }

            @Override
            public void restore() {
                restoreCalled = true;
                if (restoreException != null) {
                    throw restoreException;
                }
            }

            @Override
            public void abortRestore(SnapshotResult result) {
                abortRestoreCause = result;
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
    public void testNullFactories() {
        TestCRIU criu = new TestCRIU();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(), criu);
        checkDirectory(Phase.FEATURES, checkpoint, criu);
        checkFailDump(Phase.FEATURES, checkpoint, criu);
    }

    @Test
    public void testNullHook() {
        TestCRIU criu = new TestCRIU();
        TestSnapshotHookFactory factory = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(factory), criu);
        checkPhase(factory, checkpoint, criu);
    }

    @Test
    public void testPrepareRestore() {
        TestCRIU criu = new TestCRIU();
        TestSnapshotHookFactory f1 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f2 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f3 = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu);

        checkDirectory(Phase.APPLICATIONS, checkpoint, criu);
        List<TestSnapshotHook> hooks = getHooks(f1, f2, f3);
        for (TestSnapshotHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertNull("Unexpected Prepare abort", hook.abortPrepareCause);
            assertEquals("Restore not called.", true, hook.restoreCalled);
            assertNull("Unexpected Restore abort.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testPrepareException() {
        TestCRIU criu = new TestCRIU();
        RuntimeException prepareException = new RuntimeException("prepare exception test.");
        TestSnapshotHookFactory f1 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f2 = new TestSnapshotHookFactory(prepareException, null);
        TestSnapshotHookFactory f3 = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu);

        SnapshotResult result = checkpoint.snapshot(Phase.APPLICATIONS, new File("test"));
        assertEquals("Wrong type.", SnapshotResultType.PREPARE_ABORT, result.getType());
        assertEquals("Wrong cause.", prepareException, result.getCause());

        List<TestSnapshotHook> hooks = getHooks(f1, f2, f3);
        for (TestSnapshotHook hook : hooks) {
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
            assertNull("Unexpected Restore abort.", hook.abortRestoreCause);
        }
        assertEquals("Prepare not called.", true, f1.hook.prepareCalled);
        assertEquals("Wrong cause.", prepareException, f1.hook.abortPrepareCause.getCause());

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
            assertTrue("Unexpected Prepare Exception.", hook.abortPrepareCause.getCause() instanceof IOException);
            assertEquals("Unexpected Restore called.", false, hook.restoreCalled);
            assertNull("Unexpected Restore abort.", hook.abortRestoreCause);
        }
    }

    @Test
    public void testRestoreException() {
        TestCRIU criu = new TestCRIU();
        RuntimeException restoreException = new RuntimeException("restore exception test.");
        TestSnapshotHookFactory f1 = new TestSnapshotHookFactory();
        TestSnapshotHookFactory f2 = new TestSnapshotHookFactory(null, restoreException);
        TestSnapshotHookFactory f3 = new TestSnapshotHookFactory();
        CheckpointImpl checkpoint = new CheckpointImpl(createComponentContext(f1, f2, f3), criu);

        File test = new File("test");
        SnapshotResult result = checkpoint.snapshot(Phase.APPLICATIONS, test);
        assertEquals("Wrong type.", SnapshotResultType.RESTORE_ABORT, result.getType());
        assertEquals("Wrong cause.", restoreException, result.getCause());

        List<TestSnapshotHook> hooks = getHooks(f1, f2, f3);
        for (TestSnapshotHook hook : hooks) {
            assertEquals("Prepare not called.", true, hook.prepareCalled);
            assertNull("Unexpected Prepare abort.", hook.abortPrepareCause);
        }
        assertEquals("Restore not called.", true, f1.hook.restoreCalled);
        assertEquals("Wrong cause.", restoreException, f1.hook.abortRestoreCause.getCause());

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

    private void checkPhase(TestSnapshotHookFactory factory, CheckpointImpl checkpoint, TestCRIU criu) {
        checkDirectory(Phase.APPLICATIONS, checkpoint, criu);
        assertEquals("Wrong phase.", Phase.APPLICATIONS, factory.phase);
//        checkFailDump(Phase.SERVER, checkpoint, criu);
//        assertEquals("Wrong phase.", Phase.SERVER, factory.phase);

    }

    private void checkDirectory(Phase phase, CheckpointImpl checkpoint, TestCRIU criu) {
        File test1 = new File("test1");
        SnapshotResult result = checkpoint.snapshot(phase, test1);
        assertEquals("Wrong file.", test1, criu.directory);
    }

    private void checkFailDump(Phase phase, CheckpointImpl checkpoint, TestCRIU criu) {
        File test2 = new File("test2");
        criu.throwIOException = true;

        SnapshotResult result = checkpoint.snapshot(phase, test2);
        assertEquals("Wrong type.", SnapshotResultType.SNAPSHOT_FAILED, result.getType());
        assertTrue("Wrong cause.", result.getCause() instanceof IOException);

        assertEquals("Wrong file.", test2, criu.directory);
    }
}
