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
package com.ibm.ws.ffdc;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.Component_DM;
import test.TestConstants;
import test.common.SharedOutputManager;

public class DiagnosticModuleTest {
    static Mockery context = new Mockery();
    static final IncidentStream mockStream = context.mock(IncidentStream.class);

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

    @After
    public void tearDown() throws Exception {
        outputMgr.assertContextStatisfied(context);
        outputMgr.resetStreams();
    }

    @Test
    public void testValidate() {
        final String m = "testValidate";
        try {
            DiagnosticModule.debugDiagnosticModules = null;
            System.setProperty(DiagnosticModule.DEBUG_DM_PROPERTY, "true");

            boolean result;
            DiagnosticModule dm;

            dm = new Component_DM();
            result = dm.validate();
            assertTrue("Component_DM should be valid", result);

            dm = new Bad_DM_1();
            result = dm.validate();
            assertFalse("Bad_DM_1 should not be valid", result);

            dm = new Bad_DM_2();
            result = dm.validate();
            assertFalse("Bad_DM_2 should not be valid", result);

            DiagnosticModule.debugDiagnosticModules = null;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testGetDirectives() throws DiagnosticModuleRegistrationFailureException {
        final String m = "testGetDirectives";
        try {
            DiagnosticModule dm = new Component_DM();
            dm.init();

            String directives[] = dm.getDirectives();
            assertThat("ffdcDumpDefaultObjectX should be a directive", directives, hasItemInArray("ffdcDumpDefaultObjectX"));
            assertThat("bindItem should not be a directive", directives, not(hasItemInArray("bindItem")));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testDumpComponentData() throws DiagnosticModuleRegistrationFailureException {
        final String m = "testDumpComponentData";
        try {
            DiagnosticModule dm = new Component_DM();
            dm.init();

            final Exception ex = new Exception();
            final Object callerThis = new Object();
            final Object catcherObjects[] = new Object[] { "p1" };
            final String sourceId = m;

            context.checking(new Expectations()
            {
                {
                    exactly(4).of(mockStream).writeLine(with(any(String.class)), with(any(Object.class)));
                    exactly(2).of(mockStream).writeLine(with(any(String.class)), with(any(String.class)));
                    one(mockStream).writeLine(with(any(String.class)), with(true));
                }
            });

            dm.dumpComponentData(new String[] { "ffdcdumpMethod1" }, ex, mockStream, callerThis, catcherObjects, sourceId, null);

            assertTrue("ffdcDumpDefaultObjectX should have been called", ((Component_DM) dm).calledMethods.contains("ffdcDumpDefaultObjectX"));
            assertTrue("ffdcdumpMethod1 should have been called", ((Component_DM) dm).calledMethods.contains("ffdcdumpMethod1"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testDumpComponentDataException() throws DiagnosticModuleRegistrationFailureException {
        final String m = "testDumpComponentDataException";
        try {
            DiagnosticModule dm = new Component_DM();
            dm.init();

            final Exception ex = new Exception();
            final Object callerThis = new Object();
            final Object catcherObjects[] = new Object[] { "p1" };
            final String sourceId = m;

            context.checking(new Expectations()
            {
                {
                    one(mockStream).writeLine(with(any(String.class)), with(any(Throwable.class)));
                }
            });

            dm.getDataForDirectives(new String[] { "ffdcdumpMethod2" }, ex, mockStream, callerThis, catcherObjects, sourceId);

            assertTrue("ffdcdumpMethod2 should have been called", ((Component_DM) dm).calledMethods.contains("ffdcdumpMethod2"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testStopProcessing() throws DiagnosticModuleRegistrationFailureException {
        final String m = "testStopProcessingException";
        try {
            DiagnosticModule dm = new Component_DM();
            dm.init();

            final Exception ex = new Exception();
            final Object callerThis = new Object();
            final Object catcherObjects[] = new Object[] { "p1" };
            final String sourceId = m;

            context.checking(new Expectations()
            {
                {
                    one(mockStream).writeLine(with(any(String.class)), with(same(catcherObjects)));
                    one(mockStream).writeLine(with(any(String.class)), with(any(String.class)));
                }
            });

            dm.getDataForDirectives(new String[] { "ffdcdumpMethod3", "ffdcdumpMethod4" }, ex, mockStream, callerThis, catcherObjects, sourceId);

            assertTrue("ffdcdumpMethod3 should have been called", ((Component_DM) dm).calledMethods.contains("ffdcdumpMethod3"));
            assertFalse("ffdcdumpMethod4 should not have been called", ((Component_DM) dm).calledMethods.contains("ffdcdumpMethod4"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    class Bad_DM_1 extends DiagnosticModule {
        /**
         * Create a method with the wrong number of parameters
         * 
         * @see DiagnosticModule#FFDC_DUMP_PARAMS
         */
        public void ffdcDumpDefaultObjectX(Object badSignature) {}
    }

    class Bad_DM_2 extends DiagnosticModule {
        /**
         * Create a method with the right number of parameters, but the wrong
         * parameter types
         * 
         * @see DiagnosticModule#FFDC_DUMP_PARAMS
         */
        public void ffdcDumpDefaultObjectX(Object badSignature, IncidentStream is, Object callerThis, Object[] o, String sourceId) {}
    }
}
