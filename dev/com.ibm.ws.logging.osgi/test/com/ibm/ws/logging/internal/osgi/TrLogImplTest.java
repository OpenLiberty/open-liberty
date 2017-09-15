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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogReaderService;

import test.TestConstants;
import test.common.SharedOutputManager;

/**
 *
 */
@RunWith(JMock.class)
public class TrLogImplTest {
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
    final ServiceRegistration<LogReaderService> mockReg = context.mock(ServiceRegistration.class, "sr1");
    final ServiceRegistration<LogReaderService> mockReg2 = context.mock(ServiceRegistration.class, "sr2");

    final TrLogImpl logImpl = new TrLogImpl();

    @Test
    public void testRegisterTwice() {
        final String m = "testRegisterTwice";
        try {
            assertTrue("List of registered services is initially empty", TrLogImpl.ReaderHolder.readers.isEmpty());

            LogReaderService service1 = logImpl.getService(mockBundle, mockReg);
            LogReaderService service2 = logImpl.getService(mockBundle, mockReg);

            assertEquals("One service should be registered", 1, TrLogImpl.ReaderHolder.readers.size());
            assertSame("Second registration should return the same instance", service1, service2);

            service2 = logImpl.getService(mockBundle, mockReg2);
            assertEquals("Two services should be registered", 2, TrLogImpl.ReaderHolder.readers.size());
            assertNotSame("Second registration should return different instance", service1, service2);

            logImpl.ungetService(mockBundle, mockReg, service1);
            assertEquals("List of registered services should have 1 element after first unget", 1, TrLogImpl.ReaderHolder.readers.size());

            logImpl.ungetService(mockBundle, mockReg, service2);
            assertEquals("List of registered services should still have 1 element after remove duplicate registration", 1, TrLogImpl.ReaderHolder.readers.size());

            logImpl.ungetService(mockBundle, mockReg2, service2);
            assertTrue("List of registered services should be empty after unget of second reference", TrLogImpl.ReaderHolder.readers.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
