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

import static org.hamcrest.Matchers.hasItemInArray;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.LoggingTestUtils;

import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.wsspi.logprovider.LogProviderConfig;
import com.ibm.wsspi.logprovider.TrService;

@RunWith(JMock.class)
public class TrStaticAPITest {
    static {
        LoggingTestUtils.ensureLogManager();
    }
    static final Class<?> myClass = TrStaticAPITest.class;
    static final String myName = TrStaticAPITest.class.getName();

    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

    static final Object[] emptyArray = new Object[0];

    static TraceComponent tc;

    Mockery context = new JUnit4Mockery();

    final LogProviderConfig mockConfig = context.mock(LogProviderConfig.class);
    final TrService mockService = context.mock(TrService.class);

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
        tc = Tr.register(myClass, null);
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
        SharedTr.clearConfig();
    }

    @Test
    public void testStaticTraceMethods() {
        // Each of the following methods have three flavors, which should
        // result in
        // three calls to the delegate:
        // one with a null third (argument) parameter,
        // one with the singular object parameter wrapped in an array,
        // one with the object array passed through
        final String msg = "m";
        final Object o = new Object();
        final Object os[] = new Object[] { o };
        final TrStaticAPITest c = new TrStaticAPITest();
        final Object id = new Object();

        context.checking(new Expectations()
        {
            {
                one(mockService).audit(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).audit(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).audit(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.audit(tc, msg);
        Tr.audit(tc, msg, o);
        Tr.audit(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).debug(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).debug(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).debug(with(same(tc)), with(same(msg)), with(same(os)));
                one(mockService).debug(with(same(tc)), with(same(id)), with(same(msg)), with(same(os)));
            }
        });

        Tr.debug(tc, msg);
        Tr.debug(tc, msg, o);
        Tr.debug(tc, msg, os);
        Tr.debug(id, tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).debug(with(same(tc)), with(same(c)), with(same(msg)), with(emptyArray));
                one(mockService).debug(with(same(tc)), with(same(c)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).debug(with(same(tc)), with(same(c)), with(same(msg)), with(same(os)));
            }
        });

        Tr.debug(c, tc, msg);
        Tr.debug(c, tc, msg, o);
        Tr.debug(c, tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).dump(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).dump(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).dump(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.dump(tc, msg);
        Tr.dump(tc, msg, o);
        Tr.dump(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).entry(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).entry(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).entry(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.entry(tc, msg);
        Tr.entry(tc, msg, o);
        Tr.entry(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).error(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).error(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).error(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.error(tc, msg);
        Tr.error(tc, msg, o);
        Tr.error(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).event(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).event(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).event(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.event(tc, msg);
        Tr.event(tc, msg, o);
        Tr.event(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).exit(with(same(tc)), with(same(msg)));
                one(mockService).exit(with(same(tc)), with(same(msg)), with(same(o)));
                // one (mockService).exit(with(same(tc)), with(same(msg)),
                // with(same(os)));
            }
        });

        Tr.exit(tc, msg);
        Tr.exit(tc, msg, o);
        // Tr.exit(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).fatal(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).fatal(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).fatal(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.fatal(tc, msg);
        Tr.fatal(tc, msg, o);
        Tr.fatal(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).info(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).info(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).info(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.info(tc, msg);
        Tr.info(tc, msg, o);
        Tr.info(tc, msg, os);

        context.checking(new Expectations()
        {
            {
                one(mockService).warning(with(same(tc)), with(same(msg)), with(emptyArray));
                one(mockService).warning(with(same(tc)), with(same(msg)), with(hasItemInArray(same(o))));
                one(mockService).warning(with(same(tc)), with(same(msg)), with(same(os)));
            }
        });

        Tr.warning(tc, msg);
        Tr.warning(tc, msg, o);
        Tr.warning(tc, msg, os);
    }
}
