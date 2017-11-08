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
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
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
import com.ibm.ws.kernel.boot.ClientRunnerException;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.launch.internal.FrameworkManager;
import com.ibm.ws.kernel.launch.service.ClientRunner;
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

        installDirBefore = Utils.getInstallDir();
        Utils.setInstallDir(new File("test", "test data"));
    }

    @After
    public void after() throws Exception {
        Utils.setInstallDir(installDirBefore);

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

    private void launchFramework(FrameworkManager fm, boolean isClient) {
        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr, isClient);
        config.setInitProps(new HashMap<String, String>());
        fm.launchFramework(config, null);
    }

    @Test
    public void testClientRunner() throws Throwable {
        mockery.checking(new Expectations() {
            {
                @SuppressWarnings("unchecked")
                ServiceReference<ClientRunner> ref = mockery.mock(ServiceReference.class);
                allowing(systemBundleContext).getServiceReference(ClientRunner.class);
                will(returnValue(ref));

                ClientRunner clientRunner = mockery.mock(ClientRunner.class);
                allowing(systemBundleContext).getService(ref);
                will(returnValue(clientRunner));

                one(clientRunner).run();
            }
        });

        setupFrameworkReadyServices();
        TestFrameworkManager fm = new TestFrameworkManager();
        launchFramework(fm, true);
    }

    @Test
    public void testClientRunnerException() throws Throwable {
        mockery.checking(new Expectations() {
            {
                @SuppressWarnings("unchecked")
                ServiceReference<ClientRunner> ref = mockery.mock(ServiceReference.class);
                allowing(systemBundleContext).getServiceReference(ClientRunner.class);
                will(returnValue(ref));

                ClientRunner clientRunner = mockery.mock(ClientRunner.class);
                allowing(systemBundleContext).getService(ref);
                will(returnValue(clientRunner));

                one(clientRunner).run();
                will(throwException(new TestException()));
            }
        });

        setupFrameworkReadyServices();
        TestFrameworkManager fm = new TestFrameworkManager();
        try {
            launchFramework(fm, true);
            Assert.fail("expected TestException");
        } catch (ClientRunnerException cre) {
            Assert.assertEquals(35, cre.getReturnCode().getValue());
            Assert.assertEquals(TestException.class, cre.getCause().getClass());
        }
    }

    @Test
    public void testClientRunnerMissing() throws Throwable {
        // Return null for clientRunner and find error CWWKE0916E
        mockery.checking(new Expectations() {
            {
                allowing(systemBundleContext).getServiceReference(ClientRunner.class);
                will(returnValue(null));
            }
        });

        setupFrameworkReadyServices();
        TestFrameworkManager fm = new TestFrameworkManager();
        launchFramework(fm, true);
        outputMgr.expectError("CWWKE0916E");
    }

    @Test
    public void testClientStartFrameworkException() throws Throwable {
        TestFrameworkManager fm = new TestFrameworkManager() {
            @Override
            protected Framework startFramework(BootstrapConfig config) {
                throw new TestException();
            }
        };

        try {
            launchFramework(fm, true);
            Assert.fail("expected TestException");
        } catch (TestException e) {
        }
    }

    @Test
    public void testClientWaitForReadyException() throws Throwable {
        TestFrameworkManager fm = new TestFrameworkManager() {
            @Override
            public boolean waitForReady() throws InterruptedException {
                throw new TestException();
            }
        };

        try {
            launchFramework(fm, true);
            Assert.fail("expected TestException");
        } catch (ClientRunnerException e) {
        }
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
