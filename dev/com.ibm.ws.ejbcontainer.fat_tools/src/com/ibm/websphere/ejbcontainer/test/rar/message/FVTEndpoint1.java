// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  07/09/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.message;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;

/**
 * <p>This is an example of endpoint application. This endpoint is used for unit
 * test purpose.</p>
 */
public class FVTEndpoint1 implements MessageListener, MessageEndpoint {
    private final static String CLASSNAME = FVTEndpoint1.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * @see javax.jms.MessageListener#onMessage(Message)
     */
    @Override
    public void onMessage(Message message) {
        try {
            svLogger.info("--onMessage: Hey, I am endpoint 1. I got the message :" + ((TextMessage) message).getText());
        } catch (Exception e) {
        }
    }

    public void messageMethod1(String message) {
        svLogger.info("--messageMethod1: Hey, I am endpoint 1. I got the message :" + message);
    }

    public void messageMethod2(String message) {
        svLogger.info("--messageMethod2: Hey, I am endpoint 1. I got the message :" + message);
    }

    public void integerMethod(Integer i) {
        svLogger.info("--integerMethod: Hey I got an integer. Do you?");
    }

    /**
     * <p>This is called by a resource adapter before a message is delivered. </p>
     * 
     * @param method description of a target method. This information about the
     *            intended target method allows an application server to decide whether to
     *            start a transaction during this method call, depending on the transaction
     *            preferences of the target method. The processing (by the application server)
     *            of the actual message delivery method call on the endpoint must be independent
     *            of the class loader associated with this descriptive method object.
     * 
     * @exception NoSuchMethodException indicates that the specified method does not
     *                exist on the target endpoint.
     * @exception ResourceException generic exception.
     * @exception ApplicationServerInternalException indicates an error condition in
     *                the application server.
     * @exception IllegalStateException indicates that the endpoint is in an illegal
     *                state for the method invocation. For example, this occurs when beforeDelivery
     *                and afterDelivery method calls are not paired.
     * @exception UnavailableException indicates that the endpoint is not available.
     */
    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        svLogger.info("--endpoint 1: beforeDelivery");
    }

    /**
     * <p>This is called by a resource adapter after a message is delivered. </p>
     * 
     * @exception ResourceException generic exception.
     * @exception ApplicationServerInternalException indicates an error condition in
     *                the application server.
     * @exception IllegalStateException indicates that the endpoint is in an illegal
     *                state for the method invocation. For example, this occurs when beforeDelivery
     *                and afterDelivery method calls are not paired.
     * @exception UnavailableException indicates that the endpoint is not available.
     */
    @Override
    public void afterDelivery() throws ResourceException {
        svLogger.info("--endpoint 1: afterDelivery");
    }

    /**
     * <p>This method may be called by the resource adapter to indicate that it no
     * longer needs a proxy endpoint instance. This hint may be used by the
     * application server for endpoint pooling decisions. </p>
     */
    @Override
    public void release() {
        svLogger.info("--endpoint 1: release");
    }
}