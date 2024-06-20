/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.wsspi.logging.LogRecordExt;

/**
 * Test LoggingWtoLogHandler
 */
@RunWith(JMock.class)
public class LoggingWtoLogHandlerTest {

    /**
     * Mock environment for native methods.
     */
    protected static Mockery mockery = null;

    protected ZosLoggingBundleActivator mockZlba = null;

    /**
     * Counter for generating unique mock object names.
     */
    protected static int uniqueMockNameCount = 1;

    protected LoggingWtoLogHandler wtoLH;

    class TestLogRecord extends LogRecord implements LogRecordExt {

        /**  */
        private static final long serialVersionUID = 6542740398918120790L;

        private final String message;

        TestLogRecord(Level level, String msg) {
            super(level, msg);
            message = msg;
        }

        /**
         * @{inheritDoc
         */
        @Override
        public String getFormattedMessage(Locale locale) {
            return message;
        }
    };

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        mockZlba = mockery.mock(ZosLoggingBundleActivator.class);
        wtoLH = new LoggingWtoLogHandler(mockZlba);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        mockZlba = null;
        wtoLH = null;
    }

    /**
     * Test LoggingWtoLogHandler null msg to native routine
     */
    @Test
    public void test_LoggingWtoLogHandlerNull() {

        final byte[] msgBytes = null;
        mockery.checking(new Expectations() {
            {
                oneOf(mockZlba).ntv_WriteToOperatorConsole(with(equal(msgBytes)));
                will(returnValue(-102));
            }
        });

        int rc = wtoLH.writeToOperatorConsole(null);
        assertEquals(-102, rc);

    }

    /**
     * Test LoggingWtoLogHandler publish with null LogRecord
     */
    @Test
    public void test_LoggingWtoLogHandlerPublishNullLogRecord() {

        String msg = "GROUP1";
        final byte[] msgBytes = new byte[] { -57, -39, -42, -28, -41, -15, 0 }; // "GROUP1" in EBCDIC (hex.c7.d9.d6.e4.d7.f1.00)

        mockery.checking(new Expectations() {
            {
                oneOf(mockZlba).ntv_WriteToOperatorConsole(with(equal(msgBytes)));
                will(returnValue(0));
            }
        });

        wtoLH.publish(new TestRoutedMessage(msg, null));

    }

    /**
     * Test LoggingWtoLogHandler with null message and LogRecord
     */
    @Test
    public void test_LoggingWtoLogHandlerPublishNullMessageAndLogRecord() {

        mockery.checking(new Expectations() {
            {
                never(mockZlba).ntv_WriteToOperatorConsole(null);
            }
        });

        wtoLH.publish(new TestRoutedMessage(null, null));

    }

    /**
     * Test LoggingWtoLogHandler publish with message and LogRecord
     */
    @Test
    public void test_LoggingWtoLogHandler() {

        String msg = "GROUP2";
        final byte[] msgBytes = new byte[] { -57, -39, -42, -28, -41, -14, 0 }; // "GROUP2" in EBCDIC (hex.c7.d9.d6.e4.d7.f2.00)
        mockery.checking(new Expectations() {
            {
                oneOf(mockZlba).ntv_WriteToOperatorConsole(with(equal(msgBytes)));
                will(returnValue(0));
            }
        });

        wtoLH.publish(new TestRoutedMessage(msg));

    }

    /**
     * Test LoggingWtoLogHandler publish with message and LogRecord that
     * gets a non zero return code.
     */
    @Test
    public void test_LoggingWtoLogHandlerReturnCode() {

        String msg = "GROUP2";
        final byte[] msgBytes = new byte[] { -57, -39, -42, -28, -41, -14, 0 }; // "GROUP2" in EBCDIC (hex.c7.d9.d6.e4.d7.f2.00)
        mockery.checking(new Expectations() {
            {
                oneOf(mockZlba).ntv_WriteToOperatorConsole(with(equal(msgBytes)));
                will(returnValue(-101));
            }
        });

        wtoLH.publish(new TestRoutedMessage(msg));

    }

    /**
     * Test LoggingWtoLogHandler publish with message and LogRecord german locale
     */
    @Test
    public void test_LoggingWtoLogHandlerGerman() {

        Locale tmpDefault = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);

        final TestLogRecord mockTLR = mockery.mock(TestLogRecord.class, "TestLogRecord" + uniqueMockNameCount++);

        final String returnedMsg = "GROUP2";
        final byte[] msgBytes = new byte[] { -57, -39, -42, -28, -41, -14, 0 }; // "GROUP2" in EBCDIC (hex.c7.d9.d6.e4.d7.f2.00)

        mockery.checking(new Expectations() {
            {
                oneOf(mockTLR).getFormattedMessage(Locale.ENGLISH);
                will(returnValue(returnedMsg));
                oneOf(mockZlba).ntv_WriteToOperatorConsole(with(equal(msgBytes)));
                will(returnValue(0));

            }
        });

        // Need to create the object *after* setting the default locale
        wtoLH = new LoggingWtoLogHandler(mockZlba);

        wtoLH.publish(new TestRoutedMessage(returnedMsg, mockTLR));

        Locale.setDefault(tmpDefault);

    }

}
