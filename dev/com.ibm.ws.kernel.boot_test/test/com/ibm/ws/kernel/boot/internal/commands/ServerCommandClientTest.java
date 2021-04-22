/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.launch.internal.FrameworkManager;
import com.ibm.ws.kernel.launch.internal.ServerCommandListener;

import junit.framework.Assert;
import test.common.SharedOutputManager;
import test.shared.Constants;

/**
 *
 */
@SuppressWarnings("restriction")
public class ServerCommandClientTest {

    private static final String COULD_NOT_DELETE_ERROR_MESSAGE = MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandAuthFailure"), "stop", ".*");
    private static final String COMM_FAILURE_ERROR_MESSAGE = MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandCommFailure"), "stop");

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private volatile TestServerCommandListener scl;

    @Rule
    public TestRule outputRule = outputMgr;

    private enum TestType {
        AUTH_ERROR, UUID_FAILURE, NOT_NUMBER, INVALID_RC, COMM_FAILURE, EMPTY_RESPONSE
    }

    private class TestServerCommandListener extends ServerCommandListener {

        private final CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();
        private TestType test;
        private boolean challengeWritten = false;

        /**
         * @param bootProps
         * @param uuid
         * @param frameworkManager
         */
        public TestServerCommandListener(BootstrapConfig bootProps, String uuid, FrameworkManager frameworkManager, Thread listenerThread) {
            super(bootProps, uuid, frameworkManager, listenerThread);
        }

        @Override
        protected void write(SocketChannel sc, String s) throws IOException {
            if (TestType.AUTH_ERROR == test) {

                FileOutputStream fos = new FileOutputStream(challengeFile);
                fos.getChannel().lock();
                sc.write(encoder.encode(CharBuffer.wrap("challengeFile")));
            } else if (TestType.NOT_NUMBER == test) {
                if (challengeWritten) {
                    challengeWritten = false;
                    sc.write(encoder.encode(CharBuffer.wrap(s + "#notanumber")));

                } else {
                    challengeWritten = true;
                    sc.write(encoder.encode(CharBuffer.wrap(s)));
                }
            } else if (TestType.INVALID_RC == test) {
                if (challengeWritten) {
                    challengeWritten = false;
                    sc.write(encoder.encode(CharBuffer.wrap(s + "#" + ReturnCode.INVALID.getValue())));
                } else {
                    challengeWritten = true;
                    sc.write(encoder.encode(CharBuffer.wrap(s)));
                }
            } else if (TestType.UUID_FAILURE == test) {
                if (challengeWritten) {
                    challengeWritten = false;
                    sc.write(encoder.encode(CharBuffer.wrap("invalidUUID")));
                } else {
                    challengeWritten = true;
                    sc.write(encoder.encode(CharBuffer.wrap(s)));
                }
            } else if (TestType.EMPTY_RESPONSE == test) {
                if (challengeWritten) {
                    challengeWritten = false;
                    sc.close();
                } else {
                    challengeWritten = true;
                    sc.write(encoder.encode(CharBuffer.wrap(s)));
                }
            } else if (TestType.COMM_FAILURE == test) {
                sc.write(encoder.encode(CharBuffer.wrap(s)));
                sc.shutdownOutput();

            } else {
                sc.write(encoder.encode(CharBuffer.wrap(s)));
            }
        }

        void setTestType(TestType test) {
            this.test = test;
        }

    }

    protected static Mockery mockery;
    private File challengeFile;

    @Before
    public void before() throws IOException {
        challengeFile = new File("challenge");
        challengeFile.createNewFile();

        Constants.TEST_TMP_ROOT_FILE.mkdirs();

        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

    }

    @After
    public void after() {
        challengeFile.delete();

        if (scl != null) {
            scl.close();
            scl = null;
        }
    }

    @Test
    public void testCommandAuthorizationFailure() {

        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);
        final FrameworkManager mockFW = mockery.mock(FrameworkManager.class);

        mockery.checking(new Expectations() {
            {
                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_FILE);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_FILE)));

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_AUTH_DIR);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_AUTH_DIR)));

                allowing(mockBootConfig).getWorkareaFile(null);
                will(returnValue(new File(Constants.TEST_TMP_ROOT)));

                allowing(mockBootConfig).get("com.ibm.ws.kernel.default.command.port.disabled");
                will(returnValue(null));

                allowing(mockBootConfig).get("command.port");
                will(returnValue(null));

                allowing(mockBootConfig).getProcessName();
                will(returnValue("testServer"));

                allowing(mockBootConfig).getInstallRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getConfigFile(null);
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR);
                will(returnValue(Constants.TEST_TMP_ROOT));

                allowing(mockBootConfig).getProcessType();
                will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });

        createAndListenServerCommandListener(mockBootConfig, mockFW, TestType.AUTH_ERROR);

        ServerCommandClient scc = new ServerCommandClient(mockBootConfig);
        ReturnCode rc = scc.stopServer(false);
        assertEquals(ReturnCode.ERROR_SERVER_STOP, rc);
        assertTrue(outputMgr.checkForStandardOut(COULD_NOT_DELETE_ERROR_MESSAGE));
    }

    @Test
    public void testCommandResponseInvalidInteger() {
        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);
        final FrameworkManager mockFW = mockery.mock(FrameworkManager.class);

        mockery.checking(new Expectations() {
            {
                ignoring(mockFW);

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_FILE);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_FILE)));

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_AUTH_DIR);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_AUTH_DIR)));

                allowing(mockBootConfig).getWorkareaFile(null);
                will(returnValue(new File(Constants.TEST_TMP_ROOT)));

                allowing(mockBootConfig).get("com.ibm.ws.kernel.default.command.port.disabled");
                will(returnValue(null));

                allowing(mockBootConfig).get("command.port");
                will(returnValue(null));

                allowing(mockBootConfig).getProcessName();
                will(returnValue("testServer"));

                allowing(mockBootConfig).getInstallRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getConfigFile(null);
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR);
                will(returnValue(Constants.TEST_TMP_ROOT));

                allowing(mockBootConfig).getProcessType();
                will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });

        createAndListenServerCommandListener(mockBootConfig, mockFW, TestType.NOT_NUMBER);

        ServerCommandClient scc = new ServerCommandClient(mockBootConfig);
        ReturnCode rc = scc.stopServer(false);
        assertEquals(ReturnCode.INVALID, rc);
        assertTrue(outputMgr.checkForStandardOut(COMM_FAILURE_ERROR_MESSAGE));
    }

    @Test
    public void testCommandResponseInvalidResponse() {
        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);
        final FrameworkManager mockFW = mockery.mock(FrameworkManager.class);

        mockery.checking(new Expectations() {
            {
                ignoring(mockFW);

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_FILE);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_FILE)));

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_AUTH_DIR);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_AUTH_DIR)));

                allowing(mockBootConfig).getWorkareaFile(null);
                will(returnValue(new File(Constants.TEST_TMP_ROOT)));

                allowing(mockBootConfig).get("com.ibm.ws.kernel.default.command.port.disabled");
                will(returnValue(null));

                allowing(mockBootConfig).get("command.port");
                will(returnValue(null));

                allowing(mockBootConfig).getProcessName();
                will(returnValue("testServer"));

                allowing(mockBootConfig).getInstallRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getConfigFile(null);
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR);
                will(returnValue(Constants.TEST_TMP_ROOT));

                allowing(mockBootConfig).getProcessType();
                will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });

        createAndListenServerCommandListener(mockBootConfig, mockFW, TestType.INVALID_RC);

        ServerCommandClient scc = new ServerCommandClient(mockBootConfig);
        ReturnCode rc = scc.stopServer(false);
        assertEquals(ReturnCode.INVALID, rc);
    }

    @Test
    public void testCommandResponseInvalidUUID() {
        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);
        final FrameworkManager mockFW = mockery.mock(FrameworkManager.class);

        mockery.checking(new Expectations() {
            {
                ignoring(mockFW);

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_FILE);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_FILE)));

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_AUTH_DIR);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_AUTH_DIR)));

                allowing(mockBootConfig).getWorkareaFile(null);
                will(returnValue(new File(Constants.TEST_TMP_ROOT)));

                allowing(mockBootConfig).get("com.ibm.ws.kernel.default.command.port.disabled");
                will(returnValue(null));

                allowing(mockBootConfig).get("command.port");
                will(returnValue(null));

                allowing(mockBootConfig).getProcessName();
                will(returnValue("testServer"));

                allowing(mockBootConfig).getInstallRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getConfigFile(null);
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR);
                will(returnValue(Constants.TEST_TMP_ROOT));

                allowing(mockBootConfig).getProcessType();
                will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });

        createAndListenServerCommandListener(mockBootConfig, mockFW, TestType.UUID_FAILURE);

        ServerCommandClient scc = new ServerCommandClient(mockBootConfig);
        ReturnCode rc = scc.stopServer(false);
        assertEquals(ReturnCode.ERROR_SERVER_STOP, rc);
        assertTrue(outputMgr.checkForStandardOut(COMM_FAILURE_ERROR_MESSAGE));
    }

    @Test
    public void testCommandResponseEmptyResponse() {
        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);
        final FrameworkManager mockFW = mockery.mock(FrameworkManager.class);

        mockery.checking(new Expectations() {
            {
                ignoring(mockFW);

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_FILE);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_FILE)));

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_AUTH_DIR);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_AUTH_DIR)));

                allowing(mockBootConfig).getWorkareaFile(null);
                will(returnValue(new File(Constants.TEST_TMP_ROOT)));

                allowing(mockBootConfig).get("com.ibm.ws.kernel.default.command.port.disabled");
                will(returnValue(null));

                allowing(mockBootConfig).get("command.port");
                will(returnValue(null));

                allowing(mockBootConfig).getProcessName();
                will(returnValue("testServer"));

                allowing(mockBootConfig).getInstallRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getConfigFile(null);
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR);
                will(returnValue(Constants.TEST_TMP_ROOT));

                allowing(mockBootConfig).getProcessType();
                will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });

        createAndListenServerCommandListener(mockBootConfig, mockFW, TestType.EMPTY_RESPONSE);

        ServerCommandClient scc = new ServerCommandClient(mockBootConfig);
        ReturnCode rc = scc.stopServer(false);
        assertEquals(ReturnCode.ERROR_SERVER_STOP, rc);
        assertTrue(outputMgr.checkForStandardOut(COMM_FAILURE_ERROR_MESSAGE));
    }

    @Test
    public void testCommandResponseCommFailure() {
        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);
        final FrameworkManager mockFW = mockery.mock(FrameworkManager.class);

        mockery.checking(new Expectations() {
            {
                ignoring(mockFW);

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_FILE);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_FILE)));

                allowing(mockBootConfig).getWorkareaFile(BootstrapConstants.S_COMMAND_AUTH_DIR);
                will(returnValue(new File(Constants.TEST_TMP_ROOT, BootstrapConstants.S_COMMAND_AUTH_DIR)));

                allowing(mockBootConfig).getWorkareaFile(null);
                will(returnValue(new File(Constants.TEST_TMP_ROOT)));

                allowing(mockBootConfig).get("com.ibm.ws.kernel.default.command.port.disabled");
                will(returnValue(null));

                allowing(mockBootConfig).get("command.port");
                will(returnValue(null));

                allowing(mockBootConfig).getProcessName();
                will(returnValue("testServer"));

                allowing(mockBootConfig).getInstallRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getConfigFile(null);
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR);
                will(returnValue(Constants.TEST_TMP_ROOT));

                allowing(mockBootConfig).getProcessType();
                will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });

        createAndListenServerCommandListener(mockBootConfig, mockFW, TestType.COMM_FAILURE);

        ServerCommandClient scc = new ServerCommandClient(mockBootConfig);
        ReturnCode rc = scc.stopServer(false);
        assertEquals(ReturnCode.ERROR_SERVER_STOP, rc);
        assertTrue(outputMgr.checkForStandardOut(COMM_FAILURE_ERROR_MESSAGE));
    }

    private void createAndListenServerCommandListener(final BootstrapConfig mockBootConfig, final FrameworkManager mockFW, final TestType testType) {
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread("test") {
            @Override
            public void run() {
                scl = new TestServerCommandListener(mockBootConfig, "testuuid", mockFW, this);
                scl.setTestType(testType);
                latch.countDown();
                scl.startListening();
            }
        }.start();

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (scl == null) {
            Assert.fail("Server command listener was not created");
        }
    }
}
