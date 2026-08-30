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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;

/**
 *
 */
public class SharedMemoryAttachmentManagerTest {

    /**
     * Mock environment.
     */
    public Mockery mockery;

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
    public void testObtainAccessOrAttach() throws Exception {

        final NativeWorkRequest mockConnectWorkRequest = mockery.mock(NativeWorkRequest.class);
        final NativeRequestHandler mockNativeRequestHandler = mockery.mock(NativeRequestHandler.class);

        final LocalCommClientConnHandle clientConnHandle = new LocalCommClientConnHandle(new byte[] { 0, 1, 2, 3 });
        final long sharedMemoryToken = 0x1L;

        // Setup expectations for an attach.
        mockery.checking(new Expectations() {
            {
                exactly(2).of(mockConnectWorkRequest).getSharedMemoryToken();
                will(returnValue(sharedMemoryToken));

                oneOf(mockConnectWorkRequest).getClientConnectionHandle();
                will(returnValue(clientConnHandle));

                oneOf(mockConnectWorkRequest).getRequestSpecificParms();
                will(returnValue(new byte[0]));

                oneOf(mockNativeRequestHandler).connectToClientsSharedMemory(with(any(byte[].class)), with(any(byte[].class)));
            }
        });

        // Only the first call to obtainAccessOrAttach should actually attach.
        // The Expectations are setup to test for this.
        SharedMemoryAttachmentManager smam = new SharedMemoryAttachmentManager(mockNativeRequestHandler);
        smam.obtainAccessOrAttach(mockConnectWorkRequest);
        assertEquals(1L, smam.getStateToken(sharedMemoryToken).get());

        smam.obtainAccessOrAttach(mockConnectWorkRequest);
        assertEquals(2L, smam.getStateToken(sharedMemoryToken).get());
    }

    /**
     *
     */
    @Test
    public void testGetStateToken() throws Exception {

        final NativeRequestHandler mockNativeRequestHandler = mockery.mock(NativeRequestHandler.class);

        // The same sharedMemoryToken value should return the same state token instance
        SharedMemoryAttachmentManager smam = new SharedMemoryAttachmentManager(mockNativeRequestHandler);
        assertSame(smam.getStateToken(1L), smam.getStateToken(1L));
        assertNotSame(smam.getStateToken(1L), smam.getStateToken(2L));

        // All should be initialized to -1.
        assertEquals(-1L, smam.getStateToken(0L).get());
        assertEquals(-1L, smam.getStateToken(1L).get());
        assertEquals(-1L, smam.getStateToken(2L).get());
    }

    /**
     *
     */
    @Test
    public void testReleaseAccessAndDetach() throws Exception {

        final NativeWorkRequest mockConnectWorkRequest = mockery.mock(NativeWorkRequest.class);
        final NativeRequestHandler mockNativeRequestHandler = mockery.mock(NativeRequestHandler.class);

        final LocalCommClientConnHandle clientConnHandle = new LocalCommClientConnHandle(new byte[] { 0, 1, 2, 3 });
        final long sharedMemoryToken = 0x1L;

        // Setup expectations for attach and detach.
        mockery.checking(new Expectations() {
            {
                allowing(mockConnectWorkRequest).getSharedMemoryToken();
                will(returnValue(sharedMemoryToken));

                // 1 for the attach, 1 for the detach.
                exactly(2).of(mockConnectWorkRequest).getClientConnectionHandle();
                will(returnValue(clientConnHandle));

                // 1 for the attach, 1 for the detach.
                exactly(2).of(mockConnectWorkRequest).getRequestSpecificParms();
                will(returnValue(new byte[0]));

                oneOf(mockNativeRequestHandler).connectToClientsSharedMemory(with(any(byte[].class)), with(any(byte[].class)));
                oneOf(mockNativeRequestHandler).disconnectFromClientsSharedMemory(with(any(byte[].class)), with(any(byte[].class)));
            }
        });

        // Only the first call to obtainAccessOrAttach should actually attach.
        // The Expectations are setup to test for this.
        SharedMemoryAttachmentManager smam = new SharedMemoryAttachmentManager(mockNativeRequestHandler);
        smam.obtainAccessOrAttach(mockConnectWorkRequest);
        smam.obtainAccessOrAttach(mockConnectWorkRequest);
        assertEquals(2L, smam.getStateToken(sharedMemoryToken).get());

        smam.obtainAccess(mockConnectWorkRequest);
        smam.obtainAccess(mockConnectWorkRequest);
        assertEquals(4L, smam.getStateToken(sharedMemoryToken).get());

        smam.releaseAccess(mockConnectWorkRequest);
        assertEquals(3L, smam.getStateToken(sharedMemoryToken).get());

        smam.obtainAccess(mockConnectWorkRequest);
        assertEquals(4L, smam.getStateToken(sharedMemoryToken).get());

        smam.releaseAccess(mockConnectWorkRequest);
        assertEquals(3L, smam.getStateToken(sharedMemoryToken).get());

        smam.releaseAccess(mockConnectWorkRequest);
        assertEquals(2L, smam.getStateToken(sharedMemoryToken).get());

        smam.releaseAccess(mockConnectWorkRequest);
        smam.releaseAccess(mockConnectWorkRequest); // this one should call detach.
        assertEquals(-1L, smam.getStateToken(sharedMemoryToken).get());
    }

    /**
     *
     */
    @Test(expected = SharedMemoryAccessException.class)
    public void testObtainAccessTokenDoesntExist() throws Exception {

        final NativeRequestHandler mockNativeRequestHandler = mockery.mock(NativeRequestHandler.class);
        final NativeWorkRequest mockConnectWorkRequest = mockery.mock(NativeWorkRequest.class);

        // Setup expectations for an attach.
        mockery.checking(new Expectations() {
            {
                allowing(mockConnectWorkRequest).getSharedMemoryToken();
            }
        });

        // The token was never attached, so obtainAccess shall blow up.
        SharedMemoryAttachmentManager smam = new SharedMemoryAttachmentManager(mockNativeRequestHandler);
        smam.obtainAccess(mockConnectWorkRequest);
    }

    /**
     *
     */
    @Test(expected = SharedMemoryAccessException.class)
    public void testObtainAccessAlreadyDetached() throws Exception {

        final NativeWorkRequest mockConnectWorkRequest = mockery.mock(NativeWorkRequest.class);
        final NativeRequestHandler mockNativeRequestHandler = mockery.mock(NativeRequestHandler.class);

        final LocalCommClientConnHandle clientConnHandle = new LocalCommClientConnHandle(new byte[] { 0, 1, 2, 3 });
        final long sharedMemoryToken = 0x1L;

        // Setup expectations for attach and detach.
        mockery.checking(new Expectations() {
            {
                allowing(mockConnectWorkRequest).getSharedMemoryToken();
                will(returnValue(sharedMemoryToken));

                // 1 for the attach, 1 for the detach.
                exactly(2).of(mockConnectWorkRequest).getClientConnectionHandle();
                will(returnValue(clientConnHandle));

                // 1 for the attach, 1 for the detach.
                exactly(2).of(mockConnectWorkRequest).getRequestSpecificParms();
                will(returnValue(new byte[0]));

                oneOf(mockNativeRequestHandler).connectToClientsSharedMemory(with(any(byte[].class)), with(any(byte[].class)));
                oneOf(mockNativeRequestHandler).disconnectFromClientsSharedMemory(with(any(byte[].class)), with(any(byte[].class)));
            }
        });

        // Only the first call to obtainAccessOrAttach should actually attach.
        // The Expectations are setup to test for this.
        SharedMemoryAttachmentManager smam = new SharedMemoryAttachmentManager(mockNativeRequestHandler);
        smam.obtainAccessOrAttach(mockConnectWorkRequest);
        smam.obtainAccessOrAttach(mockConnectWorkRequest);

        smam.releaseAccess(mockConnectWorkRequest);
        smam.releaseAccess(mockConnectWorkRequest); // this one should detach.

        smam.obtainAccess(mockConnectWorkRequest); // this should blow up.
    }

}
