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
package com.ibm.ws.logging.internal.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

import test.TestConstants;
import test.common.SharedOutputManager;

/**
 *
 */
@RunWith(JMock.class)
public class TrLogServiceFactoryTest {
    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    final Mockery context = new JUnit4Mockery();
    final Bundle mockBundle = context.mock(Bundle.class, "bundle1");
    final Bundle mockSystemBundle = context.mock(Bundle.class, "systemBundle");
    final ServiceRegistration<LogService> mockReg = context.mock(ServiceRegistration.class);

    final String symName = "com.ibm.sym.name";
    final String version = "1.0.0";
    final Properties p = new Properties();

    final TrLogImpl logImpl = new TrLogImpl();
    final TrLogServiceFactory factory = new TrLogServiceFactory(logImpl, mockSystemBundle);

    protected void setDefaultExpectations() {
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundle).getVersion();
                will(returnValue(new Version(version)));

                atLeast(1).of(mockBundle).getSymbolicName();
                will(returnValue(symName));

                allowing(mockBundle).getBundleId();
                will(returnValue(2L));

                atLeast(1).of(mockBundle).getHeaders("");
                will(returnValue(p));
            }
        });
    }

    @Test
    public void testGetUnget() {
        final String m = "testGetUnget";

        try {
            assertTrue("List of registered services is initially empty", factory.registeredServices.isEmpty());

            setDefaultExpectations();
            LogService service = factory.getService(mockBundle, mockReg);

            assertNotNull("LogService should be allocated and returned", service);
            assertEquals("One service should be registered", 1, factory.registeredServices.size());

            factory.ungetService(mockBundle, mockReg, service);
            assertEquals("One service should still be registered after unget", 1, factory.registeredServices.size());

            factory.getListener().bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, mockBundle));
            assertTrue("List of registered services is empty after bundle is uninstalled", factory.registeredServices.isEmpty());

            context.checking(new Expectations() {
                {
                    atLeast(1).of(mockSystemBundle).getBundleId();
                    will(returnValue(0L));

                    atLeast(1).of(mockSystemBundle).getVersion();
                    will(returnValue(new Version(version)));

                    atLeast(1).of(mockSystemBundle).getSymbolicName();
                    will(returnValue(symName));

                    atLeast(1).of(mockSystemBundle).getHeaders("");
                    will(returnValue(p));
                }
            });

            service = factory.getService(null, mockReg);
            assertNotNull("LogService should be allocated and returned for system bundle", service);
            assertEquals("One service should be registered after get (systemBundle)", 1, factory.registeredServices.size());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testBundleLifecycle() {
        final String m = "testBundleLifecycle";
        try {
            assertTrue("List of registered services is initially empty", factory.registeredServices.isEmpty());

            setDefaultExpectations();
            factory.getListener().bundleChanged(new BundleEvent(BundleEvent.INSTALLED, mockBundle));

            assertEquals("One service should be registered", 1, factory.registeredServices.size());

            factory.getListener().bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, mockBundle));
            assertTrue("List of registered services is empty after bundle is uninstalled", factory.registeredServices.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRegisterTwice() {
        final String m = "testRegisterTwice";
        try {
            assertTrue("List of registered services is initially empty", factory.registeredServices.isEmpty());

            setDefaultExpectations();
            factory.getListener().bundleChanged(new BundleEvent(BundleEvent.INSTALLED, mockBundle));

            assertEquals("One service should be registered", 1, factory.registeredServices.size());

            // Get the service registered by the INSTALLED event
            LogService service1 = factory.registeredServices.get(mockBundle);

            // Now get the service that would be created when a log service is registered by a bundle
            LogService service2 = factory.getService(mockBundle, mockReg);

            assertEquals("One service should be registered", 1, factory.registeredServices.size());
            assertSame("Second registration should return the same instance", service1, service2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
