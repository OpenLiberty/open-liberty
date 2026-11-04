/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;

/**
 *
 */
public class FFDCCallbackTest {

    /**
     * Mock environment.
     */
    private Mockery mockery = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        // Needs to be ClassImposteriser in order to mock NativeRequestHandler and NativeWorkRequest.
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     *
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    /**
     *
     */
    @Test
    public void testBuildException() {

        final NativeWorkRequest mockNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        final int tp = 0x33;

        final ByteBuffer rawData = ByteBuffer.allocate(NativeWorkRequest.REQUESTTYPE_FFDC_RAWDATA_SIZE);
        rawData.duplicate().put(CodepageUtils.getEbcdicBytes("blah.blah.blah"));

        mockery.checking(new Expectations() {
            {
                allowing(mockNativeWorkRequest).getTP();
                will(returnValue(tp));

                allowing(mockNativeWorkRequest).getFFDCRawData();
                will(returnValue(rawData.array()));
            }
        });

        Exception e = new FFDCCallback().buildException(mockNativeWorkRequest);
        String expectedMsg = "Native error at TP(x33): blah.blah.blah (Note: the stack trace for this exception is irrelevant)";
        assertEquals(expectedMsg, e.getMessage());
    }
}
