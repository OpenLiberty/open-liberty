/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util.wsoc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.websocket.Session;

/**
 * WebSocket Java clients are multi-threaded - even when running a single client. WsocTextContext enhances an endpoint with test information such as storing expected messages,
 * message counts, test run timeouts. Endpoints should use the text context to store data they want to verify upon completion of the test.
 * etc.
 */
public class WsocTestContext {
    private static final Logger LOG = Logger.getLogger(WsocTestContext.class.getName());

    //completeLatch - Latch to wait for test completion.. tests will call terminate to trigger completion.
    //connectLatch  - Tests should call connected when onOpen occurs.
    //messageLatch  - List of tests can wait for all the tests to receive a message.
    public static CountDownLatch completeLatch = null;
    public static CountDownLatch connectLatch = null;
    public static CountDownLatch messageLatch = null;

    private List<Object> _messages = null;

    private int _curMessage = 0;

    private int _numMsgsExpected = 0;

    private boolean _terminateAll = false;

    private boolean _limitReached = false;

    private boolean _messageCountOnly = false;

    private WsocTestException _exception = null;

    private Session _session = null;

    private boolean _timedOut = false;

    private String singleMessage = null;

    private boolean closedAlready = false;

    public WsocTestContext() {

    }

    /**
     * 
     * @param numMsgsExpected - Tests will usually expect this many messages to be received.
     */
    public WsocTestContext(int numMsgsExpected) {
        this(numMsgsExpected, false);
    }

    /**
     * 
     * @param numMsgsExpected - Tests will usually expect this many messages to be received.
     * @param messageCountOnly - True and textContext will not store any messages when addMessage is called.
     */
    public WsocTestContext(int numMsgsExected, boolean messageCountOnly) {
        _numMsgsExpected = numMsgsExected;
        _messageCountOnly = messageCountOnly;

        if (!_messageCountOnly) {
            _messages = new ArrayList<Object>(numMsgsExected);
        }
        else {
            _messages = new ArrayList<Object>(1);
        }
    }

    public void addSession(Session sess) {
        _session = sess;
    }

    public Session getSession() {
        return _session;
    }

    /**
     * addMessage. In endpoint code you can add anything you want to verify here, incoming wsoc messages, Exceptions, etc..
     * 
     * @param msg
     */
    public synchronized void addMessage(Object msg) {

        _curMessage++;
        // log the six messages of each test, for better debugging
        if (_curMessage <= 6) {
            LOG.info("Adding message to test results, message #: " + _curMessage + " " + msg);
        }
        if (!_messageCountOnly) {
            _messages.add(msg);

            if ((_curMessage == _numMsgsExpected - 1) && (lastMsg != null)) {
                _curMessage++;
                _messages.add(lastMsg);
                lastMsg = null;
            }
        }

        // If maxMessages <= 0 then we'll never reach the limit thorugh receiving messages n    

        if (_numMsgsExpected > 0) {
            if (_curMessage >= _numMsgsExpected) {
                LOG.info(this.toString() + " --  Message Total: " + this._messages.toString());
                _limitReached = true;

            }
        }
        return;
    }

    public Object lastMsg = null;

    public void addAsLastMessage(Object msg) {
        LOG.info("addAsLastMessage called with msg: " + msg);

        lastMsg = msg;
    }

    /**
     * Normal termination.. usually as result of endpoint receiving expected number of messages.
     */
    public void terminateClient() {
        LOG.info("Wsoc process has been terminated");
        _limitReached = true;
        if (completeLatch != null) {
            completeLatch.countDown();
        }
    }

    /**
     * For multi client tests.. MSN TODO - not sure if this is used and workign currently.
     */
    public void terminateAllClients() {
        LOG.info("Terminate all clients has been set.");
        _terminateAll = true;
        terminateClient();
    }

    /**
     * Terminate the clienit with Exception e. Can be used as expected exception. or abnormal exception depending on the test.
     * 
     * @param msg - additional info you want to pass...
     * @param e - Exception
     */
    public void addExceptionAndTerminate(String msg, Throwable e) {
        LOG.info("adding exception: " + e.toString());
        _exception = new WsocTestException(msg, e);
        terminateClient();
    }

    /**
     * MSN TODO - not sure if this currently works
     * 
     * @param msg
     * @param e
     */
    public void addExceptionAndTerminateAllClients(String msg, Throwable e) {
        _exception = new WsocTestException(msg, e);
        terminateAllClients();
    }

    /**
     * @return Exception added by AddException... null if none added.
     */
    public Throwable getException() {
        return _exception;
    }

    /**
     * Could just call reThrowException to fail any test that you expect to pass...
     * 
     * @throws WsocTestException
     */
    public void reThrowException() throws WsocTestException {
        if (_exception != null) {
            throw _exception;
        }
    }

    /**
     * set this value to signal that test got closed, and not to try to close it again. Normally does not need to be used except for test that have special close requirements.
     * 
     * @param x
     */
    public void setClosedAlready(boolean x) {
        closedAlready = x;
    }

    /**
     * get closedAlready
     * 
     * @param x
     */
    public boolean getClosedAlready() {
        return closedAlready;
    }

    /**
     * Test did not complete in expected amount of time, set this value so test can determine this event happened. Test will likely fail anyway.. but this provides some additional
     * info.
     * 
     * @param timed
     */
    public void setTimedout(boolean timed) {
        LOG.info("Timeout set to true");
        _timedOut = timed;
    }

    /**
     * 
     * @return true if test did not complete in expected amount of time, false if it complete in time.
     */
    public boolean getTimedOut() {
        return _timedOut;
    }

    public boolean shouldTerminateAll() {
        return _terminateAll;
    }

    /**
     * 
     * @return Array of messages added through AddMessage
     */
    public List<Object> getMessage() {
        return _messages;
    }

    /**
     * 
     * @return number of messages added through addMessage.
     */
    public int getMessageCount() {
        return _curMessage;
    }

    /**
     * 
     * @return true if messages added through addMessage = numMsgsExpected.
     */
    public boolean limitReached() {
        return _limitReached;
    }

    /**
     * Called when endpoint is connected, should always call this in onOpen with any test that has a connect Timeout.
     */
    public void connected() {
        if (connectLatch != null) {
            connectLatch.countDown();
        }
    }

    /**
     * Need this exposed for some multi client tests to check is test is complete...
     * 
     * @return
     */
    public CountDownLatch getCompleteLatch() {
        return completeLatch;
    }

    /**
     * If we know we are not storing messages, call this instead of addMessage
     */
    public void increaseMessageCount() {
        _curMessage++;

    }

    public void messageLatchDown() {
        if (messageLatch != null) {
            messageLatch.countDown();
        }
    }

    public void overwriteSingleMessage(String x) {
        singleMessage = x;
    }

    public String getSingleMessage() {
        return singleMessage;
    }

}
