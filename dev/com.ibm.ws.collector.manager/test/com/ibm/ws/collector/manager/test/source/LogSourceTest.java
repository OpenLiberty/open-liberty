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
package com.ibm.ws.collector.manager.test.source;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.logging.RoutedMessage;
//import com.ibm.ws.logging.internal.osgi.TestRoutedMessage;
import com.ibm.ws.logging.source.LogSource;

/**
 *
 */
public class LogSourceTest {

    private LogSource ls;

    @Before
    public void setup() {
        ls = new LogSource();
    }

    @After
    public void tearDown() {}

    @Test
    public void testParseMessageId() {
        String id;
        TestRoutedMessage msg;
        String result;

        //STRUCTURE: [PREFIX][SUFFIX][SEVERITY][:]
        //FORMAT: [1 letter][3-4 letters or numbers][4 numbers][1 letter][colon]

        //[4L][4#][1L][:] should be accepted
        id = "ABCD1234I";
        msg = new TestRoutedMessage("ABCD1234I Test message one");
        result = ls.parse(msg).getMessageId();
        assertNull("testParseMessageId(): result1 is NOT null", result);

        //[4L][4#][1L] should be NULL
        id = "ABCD1234W";
        msg = new TestRoutedMessage("ABCD1234W: Test message two");
        result = ls.parse(msg).getMessageId();
        assertNotNull("testParseMessageId(): result2 is null", result);
        assertTrue("testParseMessageId(): " + id + " != " + result, result.equals(id));

        //[5L][4#][1L] should be NULL
        id = "ABCDE1234E";
        msg = new TestRoutedMessage("ABCDE1234E Test message three");
        result = ls.parse(msg).getMessageId();
        assertNull("testParseMessageId(): result3 is NOT null", result);

        //[5L][4#][1L][:] should be accepted
        id = "ABCDE1234E";
        msg = new TestRoutedMessage("ABCDE1234E: Test message four");
        result = ls.parse(msg).getMessageId();
        assertNotNull("testParseMessageId(): result4 is null", result);
        assertTrue("testParseMessageId(): " + id + " != " + result, result.equals(id));

        //[10#][:] should be NULL
        id = "1234567890";
        msg = new TestRoutedMessage("1234567890: Test message five");
        result = ls.parse(msg).getMessageId();
        assertNull("testParseMessageId(): result5 is NOT null", result);

        //[1L][3#L][4#][1L][:] should be accepted
        id = "A9CD1234W";
        msg = new TestRoutedMessage("A9CD1234W: Test message six");
        result = ls.parse(msg).getMessageId();
        assertNotNull("testParseMessageId(): result6 is null", result);
        assertTrue("testParseMessageId(): " + id + " != " + result, result.equals(id));

        //[1L][4#L][4#][1L][:] should be accepted
        id = "A9CDE1234W";
        msg = new TestRoutedMessage("A9CDE1234W: Test message seven");
        result = ls.parse(msg).getMessageId();
        assertNotNull("testParseMessageId(): result7 is null", result);
        assertTrue("testParseMessageId(): " + id + " != " + result, result.equals(id));

        //[5L][4#][1L] should be NULL
        id = "ABCDE1234E";
        msg = new TestRoutedMessage("ABCDE1234E Test message eight");
        result = ls.parse(msg).getMessageId();
        assertNull("testParseMessageId(): result8 is NOT null", result);
    }
}

/**
 * Helper class, for test purposes only.
 */
class TestRoutedMessage implements RoutedMessage {
    private final String formattedMsg;
    private final LogRecord logRecord;

    public TestRoutedMessage(String formattedMsg) {
        this.formattedMsg = formattedMsg;
        this.logRecord = new LogRecord(Level.INFO, formattedMsg);
    }

    @Override
    public String getFormattedMsg() {
        return formattedMsg;
    }

    @Override
    public String getFormattedVerboseMsg() {
        return getFormattedMsg();
    }

    @Override
    public String getMessageLogFormat() {
        return getFormattedMsg();
    }

    @Override
    public LogRecord getLogRecord() {
        return logRecord;
    }
}
