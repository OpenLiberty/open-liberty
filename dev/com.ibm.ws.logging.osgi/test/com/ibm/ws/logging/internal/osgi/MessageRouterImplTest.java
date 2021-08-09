/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import test.common.SharedOutputManager;

import com.ibm.wsspi.logging.LogHandler;

/**
 * Test the MessageRouterImpl.
 */
@RunWith(JMock.class)
public class MessageRouterImplTest {

    @Rule
    public TestRule outputRule = SharedOutputManager.getInstance();

    /**
     * Mockery environment for LogHandlers.
     */
    protected static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    protected static int uniqueMockNameCount = 1;

    /**
     * LogHandler mock object.
     */
    protected LogHandler mockLogHandler = null;

    /**
     * Another LogHandler mock object.
     */
    protected LogHandler mockLogHandler1 = null;

    /**
     * Create a new mockery environment and MessageRouterImpl. Should be called
     * by every test method to ensure a fresh isolated test environment.
     */
    protected MessageRouterImpl getMessageRouterImpl() {
        mockery = new JUnit4Mockery();

        mockLogHandler = mockery.mock(LogHandler.class, "LogHandler" + uniqueMockNameCount++);
        mockLogHandler1 = mockery.mock(LogHandler.class, "LogHandler" + uniqueMockNameCount++);

        return new MessageRouterImpl();
    }

    /**
     * Helper method for setting expectations on the LogHandler.
     */
    protected void setupLogHandlerExpectations(final LogHandler mockLH, final String msg, final LogRecord logRecord) {
        mockery.checking(new Expectations() {
            {
                oneOf(mockLH).publish(with(equal(msg)), with(equal(logRecord)));
            }
        });
    }

    @Test
    public void testNullLogHandlerId() {
        // No NullPointerException if the id passed is null (non-existent service property)
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler(null, null);
        msgRouter.unsetLogHandler(null, null);
    }

    @Test
    public void testNullLogHandlerRef() {
        // No NullPointerException if the ref passed is null (result of bad getService)
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("id", null);
        msgRouter.unsetLogHandler("id", null);
    }

    /**
     * Test setting the LogHandler on MessageRouterImpl after MessageRouterImpl.modified
     * has been called (i.e. after the properties have been parsed).
     */
    @Test
    public void setLogHandlerAfterModified() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Set LogHandler *after* modified().
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Create msg.
        String msg = "MYMSG1234I: blah blah blah";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));
    }

    /**
     * Test setting the LogHandler on MessageRouterImpl before MessageRouterImpl.modified
     * has been called (i.e. before the properties have been parsed).
     */
    @Test
    public void setLogHandlerBeforeModified() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();

        // Set LogHandler *before* modified.
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        String msg = "MYMSG1234I: blah blah blah";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));
    }

    /**
     * Dynamically override previous settings with new settings, removing a LogHandler
     * from the list (via '-'). Verify that the LogHandler is called prior to the
     * override, but not after.
     */
    @Test
    public void overrideAndRemove() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        String msg = "MYMSG1234I: blah blah blah";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));

        // Override and remove MYLOGHANDLER from the list.
        Properties props1 = new Properties();
        props1.setProperty("MYMSG1234I", "-MYLOGHANDLER");
        msgRouter.modified(props1);

        // mockLogHandler should not be called. No expectations to set up.

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));
    }

    /**
     * Remove the default ("-DEFAULT") and verify that MessageRouterImpl.route()
     * returns false.
     */
    @Test
    public void removeDefault() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "-DEFAULT,+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        String msg = "MYMSG1234I: blah blah blah";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);

        // Since I specified -DEFAULT, route() should return false.
        assertFalse(msgRouter.route(msg, logRecord));
    }

    /**
     * Unset a LogHandler. Verify that the LogHandler is called prior to unsetting it,
     * but not after.
     */
    @Test
    public void unsetLogHandler() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        String msg = "MYMSG1234I: blah blah blah";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));

        // Remove the LogHandler.
        msgRouter.unsetLogHandler("MYLOGHANDLER", mockLogHandler);

        // mockLogHandler now should not be called. No expectations to set up.

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));
    }

    /**
     * Verify that bad property data (empty values, +/- alone) doesn't cause problems.
     */
    @Test
    public void badPropertyData() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "-DEFAULT,,+,-,+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        String msg = "MYMSG1234I: blah blah blah";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);

        // Since I specified -DEFAULT, route() should return false.
        assertFalse(msgRouter.route(msg, logRecord));
    }

    /**
     * Add multiple LogHandlers and verify they get control when appropriate.
     */
    @Test
    public void multipleLogHandlers() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);
        msgRouter.setLogHandler("MYLOGHANDLER1", mockLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER,+MYLOGHANDLER1,-DEFAULT");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER");
        props.setProperty("MYMSG1236I", "+MYLOGHANDLER1");
        msgRouter.modified(props);

        // Create msg.
        String msg1234 = "MYMSG1234I: blah blah blah";
        LogRecord logRecord1234 = new LogRecord(Level.INFO, msg1234);
        String msg1235 = "MYMSG1235I: blah blah blah";
        LogRecord logRecord1235 = new LogRecord(Level.INFO, msg1235);
        String msg1236 = "MYMSG1236I: blah blah blah";
        LogRecord logRecord1236 = new LogRecord(Level.INFO, msg1236);

        // mockLogHandler should get called for msg1234 and msg1235.
        // mockLogHandler1 should get called for msg1234 and msg1236.
        setupLogHandlerExpectations(mockLogHandler, msg1234, logRecord1234);
        setupLogHandlerExpectations(mockLogHandler, msg1235, logRecord1235);
        setupLogHandlerExpectations(mockLogHandler1, msg1234, logRecord1234);
        setupLogHandlerExpectations(mockLogHandler1, msg1236, logRecord1236);

        // route(msg1234) should return false; the others true.
        assertFalse(msgRouter.route(msg1234, logRecord1234));
        assertTrue(msgRouter.route(msg1235, logRecord1235));
        assertTrue(msgRouter.route(msg1236, logRecord1236));
    }

    /**
     * Override previous settings with new settings and add a new LogHandler.
     * Verify both the existing LogHandler and the new LogHandler are both
     * called after the add.
     */
    @Test
    public void overrideAndAdd() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);
        msgRouter.setLogHandler("MYLOGHANDLER1", mockLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        String msg = "MYMSG1234I: blah blah blah";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));

        // Override and add MYLOGHANDLER1 to the list.
        Properties props1 = new Properties();
        props1.setProperty("MYMSG1234I", "+MYLOGHANDLER1");
        msgRouter.modified(props1);

        // Both mockLogHandler and mockLogHandler1 should get called.
        setupLogHandlerExpectations(mockLogHandler, msg, logRecord);
        setupLogHandlerExpectations(mockLogHandler1, msg, logRecord);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));
    }

    /**
     * Test an empty "" LogHandler key.
     * Nothing bad will happen (tho the LogHandler will never be routed to).
     */
    @Test
    public void setEmptyLogHandlerKey() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("", mockLogHandler1);
    }

    /**
     * Test an invalid message (shorter than msgId length).
     */
    @Test
    public void invalidMessage() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        String msg = "MYMSG";
        LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler should not get called.  No expectations to set up.

        // Since we're not routing anywhere, route() should return true.
        assertTrue(msgRouter.route(msg, logRecord));
    }

    /**
     * Test a null message. Msgs should never be null. The MessageRouter will
     * ignore null messages.
     */
    @Test
    public void nullMessage() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.route(null, null);
    }

    /**
     * Test when the LogHandler raises an unchecked exception, e.g NPE.
     * MessageRouterImpl propagates the NPE.
     * 
     * LogHandlers should *NEVER* raise exceptions (tho perhaps the MessageRouterImpl
     * should be fortified against the possibility nonetheless).
     */
    @Test(expected = NullPointerException.class)
    public void logHandlerThrowsException() {
        MessageRouterImpl msgRouter = getMessageRouterImpl();
        msgRouter.setLogHandler("MYLOGHANDLER", mockLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        final String msg = "MYMSG1234I: blah blah blah";
        final LogRecord logRecord = new LogRecord(Level.INFO, msg);

        // mockLogHandler will get called and raise an exception
        mockery.checking(new Expectations() {
            {
                oneOf(mockLogHandler).publish(with(equal(msg)), with(equal(logRecord)));
                will(throwException(new NullPointerException("null ptr from LogHandler")));
            }
        });

        msgRouter.route(msg, logRecord);
    }
}
