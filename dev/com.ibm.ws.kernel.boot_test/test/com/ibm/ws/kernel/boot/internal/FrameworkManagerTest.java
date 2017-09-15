/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.SharedBootstrapConfig;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.launch.internal.FrameworkManager;
import com.ibm.ws.kernel.launch.service.FrameworkReady;

import junit.framework.Assert;
import test.common.SharedOutputManager;

public class FrameworkManagerTest {
    @Rule
    public final TimeoutRule rule = new TimeoutRule();

    @Rule
    public final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final Mockery mockery = new Mockery();
    Framework framework = mockery.mock(Framework.class);
    BundleContext systemBundleContext = mockery.mock(BundleContext.class);

    boolean frameworkStarted;
    boolean frameworkStopped;
    private Thread launchFrameworkThread;
    private Throwable launchFrameworkThrowable;
    private File installDirBefore;

    @Before
    public void before() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(framework).getBundleContext();
                will(returnValue(systemBundleContext));
                allowing(framework).getState();
                will(returnValue(Bundle.ACTIVE));

            }
        });

        Field f = Utils.class.getDeclaredField("installDir");
        f.setAccessible(true);
        installDirBefore = (File) f.get(null);
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        f.set(null, new File(testClassesDir, "test data"));
    }

    @After
    public void after() throws Exception {
        Field f = Utils.class.getDeclaredField("installDir");
        f.setAccessible(true);
        f.set(null, installDirBefore);

        // Previous these tests were using @RunWith(JMock) to check expectations. I switched to asserting
        // expectations here because the TimeoutRule will not be called when using @RunWith(JMock)
        mockery.assertIsSatisfied();
    }

    private void setupFrameworkReadyServices() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(systemBundleContext).getServiceReferences(FrameworkReady.class, null);
                will(returnValue(Collections.emptyList()));
            }
        });
    }

    private void launchFramework(FrameworkManager fm) {
        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInitProps(new HashMap<String, String>());
        fm.launchFramework(config, null);
    }

    private void startLaunchFrameworkThread(final FrameworkManager fm) {
        launchFrameworkThread = new Thread() {
            @Override
            public void run() {
                try {
                    launchFramework(fm);
                } catch (Throwable t) {
                    launchFrameworkThrowable = t;
                }
            }
        };
        launchFrameworkThread.start();
    }

    private void joinLaunchFrameworkThread() throws InterruptedException {
        launchFrameworkThread.join();
        if (launchFrameworkThrowable != null) {
            throw new RuntimeException(launchFrameworkThrowable);
        }
    }

    @Test
    public void testLaunchAndShutdown() throws Throwable {
        setupFrameworkReadyServices();
        TestFrameworkManager fm = new TestFrameworkManager();

        startLaunchFrameworkThread(fm);
        fm.waitForReady();
        Assert.assertTrue(frameworkStarted);

        fm.shutdownFramework();
        fm.waitForShutdown();
        Assert.assertTrue(frameworkStopped);

        joinLaunchFrameworkThread();
    }

    @Test
    public void testFrameworkReadyService() throws Exception {
        mockery.checking(new Expectations() {
            {
                ServiceReference<?> reference = mockery.mock(ServiceReference.class);
                allowing(systemBundleContext).getServiceReferences(with(FrameworkReady.class), with((String) null));
                will(returnValue(Collections.singletonList(reference)));

                FrameworkReady frameworkReady = mockery.mock(FrameworkReady.class);
                allowing(systemBundleContext).getService(reference);
                will(returnValue(frameworkReady));

                one(frameworkReady).waitForFrameworkReady();
            }
        });

        TestFrameworkManager fm = new TestFrameworkManager();

        startLaunchFrameworkThread(fm);
        Assert.assertTrue(fm.waitForReady());

        fm.shutdownFramework();
        fm.waitForShutdown();

        joinLaunchFrameworkThread();
    }

    @Test
    public void testStartFrameworkException() throws Throwable {
        TestFrameworkManager fm = new TestFrameworkManager() {
            @Override
            protected Framework startFramework(BootstrapConfig config) {
                throw new TestException();
            }
        };

        try {
            launchFramework(fm);
            Assert.fail("expected TestException");
        } catch (TestException e) {
        }

        Assert.assertFalse(fm.waitForReady());
        Assert.assertFalse(frameworkStarted);

        fm.waitForShutdown();
        Assert.assertFalse(frameworkStopped);
    }

    @Test
    public void testInnerLaunchFrameworkException() throws Throwable {
        setupFrameworkReadyServices();

        TestFrameworkManager fm = new TestFrameworkManager() {
            @Override
            protected void innerLaunchFramework(boolean isClient) {
                throw new TestException();
            }
        };

        try {
            launchFramework(fm);
            Assert.fail("expected TestException");
        } catch (TestException e) {
        }

        Assert.assertFalse(fm.waitForReady());
        Assert.assertTrue(frameworkStarted);

        fm.waitForShutdown();
        Assert.assertTrue(frameworkStopped);
    }

    // Timing out here rather than in the ant script so that we can generate a core using TimeoutRule when
    // this hangs. Five minutes is probably excessive, but it's still less than the ant timeout.
    // This is here to debug Java Defect 126649 -- once we get a core from that, we can remove this.
    @Test(timeout = 300000)
    public void testShutdownHook() throws Throwable {
        setupFrameworkReadyServices();
        TestFrameworkManager fm = new TestFrameworkManager();

        startLaunchFrameworkThread(fm);
        Assert.assertTrue(fm.waitForReady());
        Assert.assertTrue(frameworkStarted);

        fm.runShutdownHook();

        fm.waitForShutdown();
        Assert.assertTrue(frameworkStopped);

        joinLaunchFrameworkThread();
    }

    @SuppressWarnings("serial")
    private static class TestException extends RuntimeException {}

    private class TestFrameworkManager extends FrameworkManager {
        private final CountDownLatch frameworkStoppedLatch = new CountDownLatch(1);

        void runShutdownHook() {
            shutdownHook.run();
        }

        @Override
        protected Framework startFramework(BootstrapConfig config) throws BundleException {
            frameworkStarted = true;
            return FrameworkManagerTest.this.framework;
        }

        @Override
        protected void stopFramework() {
            frameworkStopped = true;
            frameworkStoppedLatch.countDown();
        }

        @Override
        protected void waitForFrameworkStop() {
            try {
                frameworkStoppedLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void innerLaunchFramework(boolean isClient) {}

        @Override
        protected void startServerCommandListener() {}

        @Override
        public boolean waitForReady() throws InterruptedException {
            boolean result = super.waitForReady();

            if (result) {
                Assert.assertNotNull(framework);
            }

            return result;
        }
    }
}
