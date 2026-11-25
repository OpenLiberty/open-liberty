/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.internal;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.ws.zos.channel.local.queuing.LocalChannelProvider;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.channel.local.queuing.internal.NativeRequestHandler;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.VirtualConnection;

import test.common.SharedOutputManager;

/**
 * Abstract class common to needed unit tests.
 */
public abstract class LocalCommTestCommon {

    /**
     * Mock environment that sets imposteriser to allow non-interfaces to
     * be mocked up.
     */
    public Mockery mockery;

    /**
     * Output manager.
     */
    public static SharedOutputManager outputMgr;

    /**
     * The NativeRequestHandler provides a bunch of native methods for interacting with
     * the local comm channel native code. This needs to be mocked for these unit tests
     * (no native code available).
     */
    public NativeRequestHandler mockedNativeRequestHandler;

    /**
     * The LocalChannelProvider is the access point for the native half of the channel.
     * It provides refs to NativeRequestHandler, BlackQueueDemultiplexor, etc.
     */
    public LocalChannelProvider localChannelProvider;

    /**
     * Mocked DiscriminationProcess that allows downstream channels to select supported upstream channels.
     */
    public DiscriminationProcess mockedDiscriminationProcess;

    /**
     * Mocked virtualConnection object.
     */
    public VirtualConnection mockedVirtialConnection;

    /**
     * Mocked upstream channel callback set during discrimination.
     */
    public ConnectionReadyCallback mockedAppCallback;

    /**
     * Mocked Java representation of a native work request.
     */
    public NativeWorkRequest mockedNativeWorkRequest;

    /**
     * Mocked LocalCommConnLink object. Endpoint for inter-channel communication.
     */
    public LocalCommConnLink mockedLocalCommConnLink;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
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
        outputMgr.restoreStreams();
    }

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
     * Mock up native side components (LocalChannelProvider) that are needed for the test(s).
     *
     * 1. Create a mocked NativeRequestHandler.
     * 2. Create a LocalChannelProvider and set the mocked NativeRequestHandler into it.
     * 3. Create and activate the LocalCommChannelFactoryProvider
     * 4. Set the LocalChannelProvider into LocalCommChannelFactoryProvider.
     * 5. Create a mocked ExecutorService and set into LocalCommChannelFactoryProvider.
     *
     * LocalCommChannel gets a ref to the native side (LocalChannelProvider) and the ExecutorService
     * via a static ref within LocalCommChannelFactoryProvider.
     *
     */
    public void mockUpNativeSideStuff() {

        mockedNativeRequestHandler = mockery.mock(NativeRequestHandler.class);

        localChannelProvider = new TestLocalChannelProviderImpl().setNativeRequestHandler(mockedNativeRequestHandler);

        LocalCommChannelFactoryProvider localCommChannelFactoryProvider = new LocalCommChannelFactoryProvider();
        localCommChannelFactoryProvider.localChannelProvider = localChannelProvider;
        localCommChannelFactoryProvider.executorService = new TestExecutorService();
        localCommChannelFactoryProvider.activate();
    }

    /**
     * Mocks local comm side classes/interfaces
     */
    public void mockupLocalCommSideObjects() {
        mockedDiscriminationProcess = mockery.mock(DiscriminationProcess.class);
        mockedVirtialConnection = mockery.mock(VirtualConnection.class);
        mockedAppCallback = mockery.mock(ConnectionReadyCallback.class);
        mockedNativeWorkRequest = mockery.mock(NativeWorkRequest.class);
        mockedLocalCommConnLink = mockery.mock(LocalCommConnLink.class);
    }
}
