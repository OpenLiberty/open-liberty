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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;

/**
 * Test the MessageRouterImpl.
 */
@RunWith(JMock.class)
public class WsMessageRouterImplTest extends MessageRouterImplTest {

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
     * WsLogHandler mock object.
     */
    protected WsLogHandler mockWsLogHandler = null;

    /**
     * Another WsLogHandler mock object.
     */
    protected WsLogHandler mockWsLogHandler1 = null;

    /**
     * Yet another WsLogHandler mock object.
     */
    protected WsLogHandler mockWsLogHandler2 = null;

    /**
     * Create a new mockery environment and MessageRouterImpl. Should be called
     * by every test method to ensure a fresh isolated test environment.
     */
    @Override
    protected MessageRouterImpl getMessageRouterImpl() {
        super.getMessageRouterImpl(); // setup mock handlers in the superclass.

        mockery = new JUnit4Mockery();

        mockWsLogHandler = mockery.mock(WsLogHandler.class, "WsLogHandler" + uniqueMockNameCount++);
        mockWsLogHandler1 = mockery.mock(WsLogHandler.class, "WsLogHandler" + uniqueMockNameCount++);
        mockWsLogHandler2 = mockery.mock(WsLogHandler.class, "WsLogHandler" + uniqueMockNameCount++);

        return new WsMessageRouterImpl();
    }

    /**
     * 
     */
    protected WsMessageRouterImpl getWsMessageRouterImpl() {
        return (WsMessageRouterImpl) getMessageRouterImpl();
    }

    /**
     * Helper method for setting expectations on the WsLogHandler.
     */
    protected void setupWsLogHandlerExpectations(final WsLogHandler mockWsLogHandler, final RoutedMessage routedMessage) {
        mockery.checking(new Expectations() {
            {
                oneOf(mockWsLogHandler).publish(with(equal(routedMessage)));
            }
        });
    }

    @Test
    public void testNullWsLogHandlerId() {
        // No NullPointerException if the id passed is null (non-existent service property)
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler(null, null);
        msgRouter.unsetWsLogHandler(null, null);
    }

    @Test
    public void testNullWsLogHandlerRef() {
        // No NullPointerException if the ref passed is null (result of bad getService)
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("id", null);
        msgRouter.unsetWsLogHandler("id", null);
    }

    /**
     * Test setting the WsLogHandler on MessageRouterImpl after MessageRouterImpl.modified
     * has been called (i.e. after the properties have been parsed).
     */
    @Test
    public void setWsLogHandlerAfterModified() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Set WsLogHandler *after* modified().
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));
    }

    /**
     * Test setting the LogHandler on MessageRouterImpl before MessageRouterImpl.modified
     * has been called (i.e. before the properties have been parsed).
     */
    @Test
    public void setWsLogHandlerBeforeModified() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Set LogHandler *before* modified.
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));
    }

    /**
     * Dynamically override previous settings with new settings, removing a WsLogHandler
     * from the list (via '-'). Verify that the WsLogHandler is called prior to the
     * override, but not after.
     */
    @Test
    public void overrideAndRemoveWsLogHandler() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));

        // Override and remove MYLOGHANDLER from the list.
        Properties props1 = new Properties();
        props1.setProperty("MYMSG1234I", "-MYLOGHANDLER");
        msgRouter.modified(props1);

        // mockLogHandler should not be called. No expectations to set up.

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));
    }

    /**
     * Remove the default ("-DEFAULT") and verify that MessageRouterImpl.route()
     * returns false.
     */
    @Test
    public void removeDefaultWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "-DEFAULT,+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I specified -DEFAULT, route() should return false.
        assertFalse(msgRouter.route(msg));
    }

    /**
     * Unset a LogHandler. Verify that the LogHandler is called prior to unsetting it,
     * but not after.
     */
    @Test
    public void unsetWsLogHandler() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));

        // Remove the LogHandler.
        msgRouter.unsetWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // mockLogHandler now should not be called. No expectations to set up.

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));
    }

    /**
     * Verify that bad property data (empty values, +/- alone) doesn't cause problems.
     */
    @Test
    public void badPropertyDataWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "-DEFAULT,,+,-,+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I specified -DEFAULT, route() should return false.
        assertFalse(msgRouter.route(msg));
    }

    /**
     * Add multiple LogHandlers and verify they get control when appropriate.
     */
    @Test
    public void multipleWsLogHandlers() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);
        msgRouter.setWsLogHandler("MYLOGHANDLER2", mockWsLogHandler2);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER,+MYLOGHANDLER1,-DEFAULT");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER");
        props.setProperty("MYMSG1236I", "+MYLOGHANDLER1");
        props.setProperty("*", "+MYLOGHANDLER2");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");
        RoutedMessage msgShort = new TestRoutedMessage("12345");

        // mockLogHandler should get called for msg1234 and msg1235.
        // mockLogHandler1 should get called for msg1234 and msg1236.
        // mockLogHandler2 should get called for msg1234, msg1235, msg1236 and msgShort
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msgShort);

        // route(msg1234) should return false; the others true.
        assertFalse(msgRouter.route(msg1234));
        assertTrue(msgRouter.route(msg1235));
        assertTrue(msgRouter.route(msg1236));
        assertTrue(msgRouter.route(msgShort));
    }

    /**
     * Override previous settings with new settings and add a new LogHandler.
     * Verify both the existing LogHandler and the new LogHandler are both
     * called after the add.
     */
    @Test
    public void overrideAndAddWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));

        // Override and add MYLOGHANDLER1 to the list.
        Properties props1 = new Properties();
        props1.setProperty("MYMSG1234I", "+MYLOGHANDLER1");
        msgRouter.modified(props1);

        // Both mockLogHandler and mockLogHandler1 should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg));
    }

    /**
     * Test an empty "" WsLogHandler key.
     * Nothing bad will happen (tho the WsLogHandler will never be routed to).
     */
    @Test
    public void setEmptyWsLogHandlerKey() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("", mockWsLogHandler1);
    }

    /**
     * Test an invalid message (shorter than msgId length).
     */
    @Test
    public void invalidMessageWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG");

        // mockLogHandler should not get called.  No expectations to set up.

        // Since we're not routing anywhere, route() should return true.
        assertTrue(msgRouter.route(msg));
    }

    /**
     * Test a null message. RoutedMessage should never be null. WsMessageRouterImpl
     * ignores null msgs.
     */
    @Test
    public void nullMessageWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.route(null);
    }

    /**
     * Test when the WsLogHandler raises an unchecked exception, e.g NPE.
     * WsMessageRouterImpl propagates the NPE.
     * 
     * WsLogHandlers should *NEVER* raise exceptions (tho perhaps WsMessageRouterImpl
     * should be fortified against the possibility nonetheless).
     */
    @Test(expected = NullPointerException.class)
    public void wsLogHandlerThrowsException() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        final RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler will get called and raise an exception
        mockery.checking(new Expectations() {
            {
                oneOf(mockWsLogHandler).publish(with(equal(msg)));
                will(throwException(new NullPointerException("null ptr from WsLogHandler")));
            }
        });

        msgRouter.route(msg);
    }

    /**
     * Test a WsLogHandler registered for ALL msgs.
     */
    @Test
    public void test_routeAllMsgs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "MYLOGHANDLER");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER1");
        msgRouter.modified(props);

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");

        // mockLogHandler should get called for all msgs
        // mockLogHandler1 should get called only for msg1235.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);

        // All should return true.
        assertTrue(msgRouter.route(msg1234));
        assertTrue(msgRouter.route(msg1235));
        assertTrue(msgRouter.route(msg1236));
    }

    /**
     * Add multiple LogHandlers and verify they get control when appropriate.
     */
    @Test
    public void test_routeAllMultiple() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "+MYLOGHANDLER,+MYLOGHANDLER1,-DEFAULT"); // Note: can't turn off default (-DEFAULT is ignored)
        msgRouter.modified(props);

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");

        // Both mockLogHandlers should get called for all msgs.
        // mockLogHandler1 should get called for msg1234 and msg1236.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1236);

        // Should return true (-DEFAULT is ignored for '*' LogHandlers)
        assertTrue(msgRouter.route(msg1234));
        assertTrue(msgRouter.route(msg1235));
        assertTrue(msgRouter.route(msg1236));
    }

    /**
     * Test delivery of "earlierMessages".
     */
    @Test
    public void test_earlierMessages() {

        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");

        Queue<RoutedMessage> earlierMessages = new ConcurrentLinkedQueue<RoutedMessage>();
        earlierMessages.add(msg1234);
        earlierMessages.add(msg1235);

        // Pass earlierMessages to msgRouter
        msgRouter.setEarlierMessages(earlierMessages);

        // Route another message.  Will be added to earlierMessages queue.
        // No LogHandlers yet.
        assertTrue(msgRouter.route(msg1236));

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "MYLOGHANDLER");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER1");
        msgRouter.modified(props);

        // mockLogHandler should get called for all msgs
        // mockLogHandler1 should get called only for msg1235.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);

        // Earlier messages are routed to the handlers when they're set.
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);
    }

    /**
     * Test delivery of "earlierMessages".
     */
    @Test
    public void test_earlierMessages2() {

        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");
        RoutedMessage msgShort = new TestRoutedMessage("12345");

        Queue<RoutedMessage> earlierMessages = new ConcurrentLinkedQueue<RoutedMessage>();

        // Pass earlierMessages to msgRouter
        msgRouter.setEarlierMessages(earlierMessages);

        // Route messages.  Will be added to earlierMessages queue.
        // No LogHandlers yet.
        assertTrue(msgRouter.route(msg1234));
        assertTrue(msgRouter.route(msg1235));
        assertTrue(msgRouter.route(msg1236));
        assertTrue(msgRouter.route(msgShort));

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "MYLOGHANDLER");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER1");
        msgRouter.modified(props);

        // mockLogHandler should get called for all msgs
        // mockLogHandler1 should get called only for msg1235.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler, msgShort);

        // Earlier messages are routed to the handlers when they're set.
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);
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
