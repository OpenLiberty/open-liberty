/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.endpoint;

import java.lang.reflect.Method;

import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.csi.MessageEndpointTestResults;

/**
 * <p>
 * This class wraps an instance of MessageEndpoint. It also stores the test
 * results associated with the MessageEndpoint instance in different time. Users
 * can call getTestResult() to get the current test result or call get
 * getTestResult(i) to get a particular test result.
 * </p>
 */
public class MessageEndpointWrapper implements MessageEndpoint {

    private static final TraceComponent tc = Tr
                    .register(MessageEndpointWrapper.class);

    /** constant states of the endpoint */
    public final static int INUSE = 1, FREE = 2;

    /** The associated MessageEndpoint factory wrapper */
    private final MessageEndpointFactoryWrapper factory;

    /** The wrapped endpoint */
    private final MessageEndpoint endpoint;

    /** XAResource used to create the endpoint */
    private final XAResource resource;

    /** An array list of MessageEndpointTestResults */
    private MessageEndpointTestResults[] testResults;

    private static int TEST_RESULT_INITIAL_SIZE = 5;
    private static int TEST_RESULT_INCREMENT_SIZE = 5;
    private int testResultNumber = 0;

    /**
     * <p>
     * The state of the endpoint. An endpoint can be in a INUSE state, or FREE
     * state.
     * <p>
     */
    private int state = INUSE;

    /**
     * Constructor
     *
     * @param endpoint
     *            the wrapped MessageEndpoint instance
     * @param factory
     *            the message endpoint factory wrapper
     * @param resource
     *            the XAResource associated with this endpoint instance for
     *            transaction delivery.
     */
    public MessageEndpointWrapper(MessageEndpoint endpoint,
                                  MessageEndpointFactoryWrapper factory, XAResource resource) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { factory, resource });

        this.endpoint = endpoint;
        this.factory = factory;
        this.resource = resource;

        // initialize the array list
        testResults = new MessageEndpointTestResults[TEST_RESULT_INITIAL_SIZE];

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * <p>
     * This is called by a resource adapter before a message is delivered.
     * </p>
     *
     * @param method
     *            description of a target method. This information about the
     *            intended target method allows an application server to decide
     *            whether to start a transaction during this method call,
     *            depending on the transaction preferences of the target method.
     *            The processing (by the application server) of the actual
     *            message delivery method call on the endpoint must be
     *            independent of the class loader associated with this
     *            descriptive method object.
     *
     * @exception NoSuchMethodException
     *                indicates that the specified method does not exist on the
     *                target endpoint.
     * @exception ResourceException
     *                generic exception.
     */
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "beforeDelivery", new Object[] { this, method }); // d174149

        endpoint.beforeDelivery(method);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "beforeDelivery"); // d174149
    }

    /**
     * <p>
     * This is called by a resource adapter after a message is delivered.
     * </p>
     *
     * @exception ResourceException
     *                generic exception.
     * @exception ApplicationServerInternalException
     *                indicates an error condition in the application server.
     * @exception IllegalStateException
     *                indicates that the endpoint is in an illegal state for the
     *                method invocation. For example, this occurs when
     *                beforeDelivery and afterDelivery method calls are not
     *                paired.
     * @exception UnavailableException
     *                indicates that the endpoint is not available.
     */
    @Override
    public void afterDelivery() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "afterDelivery", this); // d174149

        endpoint.afterDelivery();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "afterDelivery"); // d174149
    }

    /**
     * <p>
     * This method may be called by the resource adapter to indicate that it no
     * longer needs a proxy endpoint instance. This hint may be used by the
     * application server for endpoint pooling decisions.
     * </p>
     */
    @Override
    public void release() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "release", this); // d174149

        endpoint.release();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "release"); // d174149
    }

    /**
     * Returns the endpoint.
     *
     * @return MessageEndpoint
     */
    public MessageEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Returns the factory wrapper object.
     *
     * @return MessageEndpointFactoryWrapper The associated message endpoint
     *         factory wrapper
     */
    public MessageEndpointFactoryWrapper getFactory() {
        return factory;
    }

    /**
     * Returns the resource.
     *
     * @return XAResource the associated XAResource for transaction notificatio
     */
    public XAResource getResource() {
        return resource;
    }

    /**
     * Returns the state.
     *
     * @return int the state
     */
    public int getState() {
        return state;
    }

    /**
     * Sets the state.
     *
     * @param state
     *            The state to set
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * introspecSelf method
     */
    public String introspecSelf() {
        return "resource:" + resource + "  state: "
               + (state == INUSE ? "INUSE" : "FREE");
    }

    /**
     * Returns the latest test result.
     *
     * @return The latest MessageEndpointTestResults object in the array list
     */
    public synchronized MessageEndpointTestResults getTestResult() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTestResult", this); // d174149

        if (testResultNumber == 0) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getTestResult",
                        "testResultNumber is 0, return null"); // d174149
            return null;
        } else {
            MessageEndpointTestResults testResult = testResults[testResultNumber - 1];

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getTestResult", testResult); // d174149
            return testResult;
        }
    }

    /**
     * Returns an array of the test results
     *
     * @return an array of the test results
     */
    public synchronized MessageEndpointTestResults[] getTestResults() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTestResults", this);

        if (testResultNumber == 0) {

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getTestResults", null);
            return null;
        }

        MessageEndpointTestResults[] ret = new MessageEndpointTestResults[testResultNumber];

        System.arraycopy(testResults, 0, ret, 0, testResultNumber);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTestResults", ret);
        return ret;

    }

    /**
     * <p>
     * Add a test result. This method will construct a new test result, set it
     * to the endpoint invocation handler, and then add it to the array.
     * </p>
     */
    public void addTestResult() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addTestResult", this);

        MessageEndpointTestResultsImpl testResult = new MessageEndpointTestResultsImpl();

        if (testResultNumber >= testResults.length) {
            // Not enough room for test results, increase the size.
            MessageEndpointTestResults[] temp = testResults;
            testResults = new MessageEndpointTestResults[testResultNumber
                                                         + TEST_RESULT_INCREMENT_SIZE];

            // Copy the array
            System.arraycopy(temp, 0, testResults, 0, temp.length);

            if (tc.isDebugEnabled())
                Tr.debug(tc,
                         "Get more test results than expected, increased array size to "
                             + testResults.length);
        }

        if (!factory.getAdapter().testMode) {
            // set the test result to the endpoint proxy.
            // ((MessageEndpointInvocationHandler) (Proxy
            // .getInvocationHandler(endpoint)))
            // .setTestResults(testResult);
        }

        testResults[testResultNumber++] = (testResult);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addTestResult");

    }

    /**
     * Unset the test result.
     */
    public void unsetTestResult() {
        // unset the test result to the endpoint proxy.
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetTestResult", this); // d174149

        // ((MessageEndpointInvocationHandler) (Proxy
        // .getInvocationHandler(endpoint))).unsetTestResults();

    }

}
