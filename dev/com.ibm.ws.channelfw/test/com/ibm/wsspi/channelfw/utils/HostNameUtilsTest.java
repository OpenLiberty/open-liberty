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
package com.ibm.wsspi.channelfw.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.channelfw.utils.HostNameUtils;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 * Unit tests for HostNameUtils
 */
public class HostNameUtilsTest {

    private final Mockery mock = new JUnit4Mockery();
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info=enabled:ChannelFramework=all");

    private final ComponentContext componentContext = mock.mock(ComponentContext.class, "componentContext");

    private CHFWBundle bundle;
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void initialize() {
        // Instantiate CHFWBundle and mock OSGi environment
        // Inspiration taken from com.ibm.ws.event.internal.EventEngineImplTest
        bundle = new CHFWBundle();
        final BundleContext bundleContext = mock.mock(BundleContext.class, "bundleContext");

        // BundleContext accessed through ComponentContext
        mock.checking(new Expectations() {
            {
                ignoring(bundleContext);
            }
        });

        // ComponentContext used on activate/deactivate
        mock.checking(new Expectations() {
            {
                allowing(componentContext).getProperties();
                will(returnValue(new Hashtable<String, Object>()));
                allowing(componentContext).getBundleContext();
                will(returnValue(bundleContext));
            }
        });
        try {
            // invoke the protected activate method
            Method activateMethod = bundle.getClass().getDeclaredMethod("activate", ComponentContext.class, Map.class);
            activateMethod.setAccessible(true);
            activateMethod.invoke(bundle, componentContext, new HashMap<String, Object>());

            // invoke the protected setExecutorService method
            Method setExecutorServiceMethod = bundle.getClass().getDeclaredMethod("setExecutorService", ExecutorService.class);
            setExecutorServiceMethod.setAccessible(true);
            setExecutorServiceMethod.invoke(bundle, Executors.newSingleThreadExecutor());
        } catch (Exception e) {
            outputMgr.failWithThrowable("HostNameUtilsTest", e);
        }

    }

    /**
     * Verify for doPrivilegedWithTimeoutWarning():
     * 1. a runnable taking more than HOSTNAME_LOOKUP_TIMEOUT_WARNING_MS will cause a debug message
     * 2. that runnable will still complete successfully despite the timeout
     */
    @Test
    public void testDoPriviledWithTimeoutWarning() {
        try {
            PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>() {
                @Override
                @FFDCIgnore({ SocketException.class, UnknownHostException.class })
                public Boolean run() {
                    try {
                        // sleep for a few ms longer than the timeout
                        Thread.sleep(HostNameUtils.HOSTNAME_LOOKUP_TIMEOUT_WARNING_MS + 100);
                    } catch (InterruptedException e) {
                        outputMgr.failWithThrowable("testDoPriviledWithTimeoutWarning", e);
                    }
                    return true;
                }
            };
            boolean result = HostNameUtils.doPrivilegedWithTimeoutWarning(action);

            Assert.assertTrue("the runnable did not complete correctly", result);
            Assert.assertTrue("timeout string not found",
                              outputMgr.checkForTrace("hostname lookup has taken longer than " 
                              + HostNameUtils.HOSTNAME_LOOKUP_TIMEOUT_WARNING_MS + " ms"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testDoPriviledWithTimeoutWarning", t);
        }
    }

}
