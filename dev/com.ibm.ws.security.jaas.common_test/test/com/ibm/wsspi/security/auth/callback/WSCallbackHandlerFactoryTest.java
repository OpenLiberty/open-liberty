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
package com.ibm.wsspi.security.auth.callback;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.jaas.common.internal.callback.WSCallbackHandlerFactoryImpl;

/**
 *
 */
public class WSCallbackHandlerFactoryTest {

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual setup before each test.
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        resetFactoryToNull();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        resetFactoryToNull();
    }

    private void resetFactoryToNull() throws NoSuchFieldException, IllegalAccessException {
        Field factory = WSCallbackHandlerFactory.class.getDeclaredField("factory");
        factory.setAccessible(true);
        factory.set(null, null);
        factory.setAccessible(false);
    }

    @Test
    public void testGetInstanceWithFactoryClassName() throws Exception {
        final String methodName = "testGetInstanceWithFactoryClassName";
        try {
            String factoryClassName = "com.ibm.wsspi.security.auth.callback.TestCallbackHandlerFactory";
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance(factoryClassName);
            assertTrue("The WSCallbackHandlerFactory object must be an instance of " + factoryClassName + ".",
                       factory instanceof TestCallbackHandlerFactory);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetInstance() throws Exception {
        final String methodName = "testGetInstance";
        try {
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            assertTrue("The WSCallbackHandlerFactory object must be an instance of " + WSCallbackHandlerFactoryImpl.class.getName() + ".",
                       factory instanceof WSCallbackHandlerFactoryImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
