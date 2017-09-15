/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

import test.LoggingTestUtils;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.wsspi.logprovider.LogProviderConfig;
import com.ibm.wsspi.logprovider.TrService;

@RunWith(JMock.class)
public class LoggingConfigurationTest {
    static Mockery context = new JUnit4Mockery();

    final Bundle mockBundle = context.mock(Bundle.class);
    final TrService mockService = context.mock(TrService.class);
    final LogProviderConfig mockConfig = context.mock(LogProviderConfig.class);
    final BundleContext mockBundleContext = context.mock(BundleContext.class);

    static TraceComponent tc;

    static final Object[] emptyArray = new Object[0];

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Clear Tr's internal delegate
        SharedTr.clearComponents();
        LoggingTestUtils.setTraceSpec("*=all=disabled");
    }

    @Before
    public void setUp() throws Exception {
        SharedTr.clearConfig();
        // Create one TraceComponent shared by tests below
        // (See TrRegisterTest for exercise of Tr.register)
        context.checking(new Expectations()
        {
            {
                allowing(mockConfig).getTrDelegate();
                will(returnValue(mockService));

                allowing(mockService).init(mockConfig);
                allowing(mockService).stop();

                allowing(mockConfig).getTraceString();
                will(returnValue("*=all=enabled"));

                one(mockService).register(with(any(TraceComponent.class)));
                atLeast(1).of(mockService).info(with(TraceSpecification.getTc()), with("MSG_TRACE_STATE_CHANGED"), with(any(String.class)));
            }
        });
        TrConfigurator.init(mockConfig);
        tc = Tr.register(LoggingConfigurationTest.class, null);
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
        SharedTr.clearConfig();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdated() throws ConfigurationException {

        @SuppressWarnings("rawtypes")
        final Hashtable newProps = new Hashtable();
        newProps.put("suppressSensitiveTrace", false);
        /*
         * Ignore call to the bundle context from registering OSGiLogAdapter
         */
        context.checking(new Expectations() {
            {
                allowing(mockService).register(with(any(TraceComponent.class)));
                allowing(mockBundleContext).registerService(with(any(Class.class)), with(any(Object.class)), with(any(Dictionary.class)));

                allowing(mockConfig).update(newProps);
                allowing(mockConfig).getTraceString();
                will(returnValue(""));

                allowing(mockService).update(mockConfig);

                // event traced from update call
                one(mockService).event(with(any(TraceComponent.class)), with(any(String.class)), with(any(Object.class)));
            }
        });

        LoggingConfigurationService trService = new LoggingConfigurationService(mockBundleContext, false);
        trService.updated(newProps);
    }
}
