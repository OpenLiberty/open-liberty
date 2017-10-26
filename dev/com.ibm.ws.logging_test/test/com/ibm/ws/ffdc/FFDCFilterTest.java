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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wsspi.logprovider.FFDCFilterService;

import test.TestConstants;
import test.common.SharedOutputManager;

public class FFDCFilterTest {
    static Mockery context = new Mockery();
    static final FFDCFilterService mockService = context.mock(FFDCFilterService.class);

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();

        // Set internal delegate to the mockService
        SharedFFDCConfigurator.setDelegate(mockService);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // reset internal delegate
        FFDCConfigurator.delegate = null;

        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.assertContextStatisfied(context);
        outputMgr.resetStreams();
    }

    @Test
    public void testProcessExceptionThrowableStringString() {
        final String m = "testProcessExceptionThrowableStringString";
        try {
            final Throwable th = new Throwable();
            final String sourceId = "sourceId";
            final String probeId = "probeId";

            context.checking(new Expectations()
            {
                {
                    one(mockService).processException(with(same(th)), with(same(sourceId)), with(same(probeId)));
                }
            });

            FFDCFilter.processException(th, sourceId, probeId);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testProcessExceptionThrowableStringStringObject() {
        final String m = "testProcessExceptionThrowableStringStringObject";
        try {
            final Throwable th = new Throwable();
            final String sourceId = "sourceId";
            final String probeId = "probeId";
            final Object callerThis = new Object();

            context.checking(new Expectations()
            {
                {
                    one(mockService).processException(with(same(th)), with(same(sourceId)), with(same(probeId)), with(same(callerThis)));
                }
            });

            FFDCFilter.processException(th, sourceId, probeId, callerThis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testProcessExceptionThrowableStringStringObjectArray() {
        final String m = "testProcessExceptionThrowableStringStringObjectArray";
        try {
            final Throwable th = new Throwable();
            final String sourceId = "sourceId";
            final String probeId = "probeId";
            final Object[] objectArray = new Object[] {};

            context.checking(new Expectations()
            {
                {
                    one(mockService).processException(with(same(th)), with(same(sourceId)), with(same(probeId)), with(same(objectArray)));
                }
            });

            FFDCFilter.processException(th, sourceId, probeId, objectArray);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testProcessExceptionThrowableStringStringObjectObjectArray() {
        final String m = "testProcessExceptionThrowableStringStringObjectObjectArray";
        try {
            final Throwable th = new Throwable();
            final String sourceId = "sourceId";
            final String probeId = "probeId";
            final Object callerThis = new Object();
            final Object[] objectArray = new Object[] {};

            context.checking(new Expectations()
            {
                {
                    one(mockService).processException(with(same(th)), with(same(sourceId)), with(same(probeId)), with(same(callerThis)), with(same(objectArray)));
                }
            });

            FFDCFilter.processException(th, sourceId, probeId, callerThis, objectArray);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
