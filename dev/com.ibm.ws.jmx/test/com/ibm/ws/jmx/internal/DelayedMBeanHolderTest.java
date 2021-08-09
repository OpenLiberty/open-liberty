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
package com.ibm.ws.jmx.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.jmx_test.mbeans.Counter;
import com.ibm.ws.jmx_test.mbeans.CounterMBean;

/**
 *
 */
public class DelayedMBeanHolderTest {

    @Test
    public void testInitialState() {
        DelayedMBeanHolder mBeanHolder = new DelayedMBeanHolder(new MockServiceReference<Object>());
        assertEquals("Initial registration state is DELAYED.", DelayedMBeanRegistrationState.DELAYED, mBeanHolder.registrationState.get());
        assertEquals("Initial count on PROCESSING state latch is 1.", 1L, mBeanHolder.processingCompleteSignal.getCount());
    }

    @Test
    public void testLocateService() throws Exception {
        ServiceReference<Object> sr = new MockServiceReference<Object>(CounterMBean.class.getName());
        AtomicReference<BundleContext> contextReference = new AtomicReference<BundleContext>();
        DelayedMBeanHolder mBeanHolder = new DelayedMBeanHolder(sr);
        CounterMBean serviceObject = new Counter();
        MockBundleContext context = new MockBundleContext();
        context.addService(sr, serviceObject);
        contextReference.set(context);
        Object mbeanInstance = MBeanUtil.getRegisterableMBean(sr, serviceObject);
        assertSame("Expecting the object that was stored in the ComponentContext.", serviceObject, mbeanInstance);
    }
}
