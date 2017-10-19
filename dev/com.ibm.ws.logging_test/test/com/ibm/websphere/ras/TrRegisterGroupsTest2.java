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
package com.ibm.websphere.ras;

import static org.junit.Assert.assertEquals;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.wsspi.logprovider.LogProviderConfig;
import com.ibm.wsspi.logprovider.TrService;

import test.LoggingTestUtils;
import test.common.SharedOutputManager;

/**
 * Test TraceComponent registration methods using annotations to specify
 * multiple groups only
 */
@RunWith(JMock.class)
@TraceOptions(traceGroups = { "multigroup1", "multigroup2" })
public class TrRegisterGroupsTest2 {
    static {
        LoggingTestUtils.ensureLogManager();
    }
    static final String myName = TrRegisterGroupsTest2.class.getName();

    static SharedOutputManager outputMgr;

    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

    Mockery context = new JUnit4Mockery();

    final LogProviderConfig mockConfig = context.mock(LogProviderConfig.class);
    final TrService mockService = context.mock(TrService.class);

    TraceComponent tc;

    @Before
    public void setUp() throws Exception {
        SharedTr.clearConfig();
        // Create one TraceComponent shared by tests below
        // (See TrRegisterTest for exercise of Tr.register)
        context.checking(new Expectations() {
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
        tc = Tr.register(TrRegisterGroupsTest2.class, null);
    }

    @After
    public void tearDown() throws Exception {
        SharedTr.clearComponents();
    }

    @Test
    public void testRegisterClass() {
        Class<?> myClass = this.getClass();
        TraceOptions options = myClass.getAnnotation(TraceOptions.class);
        System.out.println("options are: " + options);

        final String m = "testRegisterClass";
        try {
            context.checking(new Expectations() {
                {
                    one(mockService).register(with(any(TraceComponent.class)));
                }
            });

            TraceComponent tc = Tr.register(myClass);
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[multigroup1, multigroup2],,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}