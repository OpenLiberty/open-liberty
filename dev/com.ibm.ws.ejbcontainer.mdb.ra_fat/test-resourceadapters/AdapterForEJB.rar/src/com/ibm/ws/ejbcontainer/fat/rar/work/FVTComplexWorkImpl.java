/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.work;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointFactoryWrapper;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointWrapper;
import com.ibm.ws.ejbcontainer.fat.rar.message.TextMessageImpl;

/**
 * <p>This class extends FVTWorkImpl class. An object of this class represents a complex work
 * to be submitted to the work manager for execution. This complex work can send one or
 * more messages to one or more endpoint instances.</p>
 */
public class FVTComplexWorkImpl extends FVTGeneralWorkImpl {
    private final static String CLASSNAME = FVTComplexWorkImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** The FVTMessage object */
    FVTMessage fvtMessage;

    /** the FVT work dispatcher */
    FVTWorkDispatcher workDispatcher;

    /** target method used to send message */
    Method targetMethod;

    /** Messages from the FVTMessage object */
    Vector messages;

    /** XAResources from the FVTMessage object */
    HashMap resources;

    /**
     * Constructor for FVTComplexWorkImpl.
     *
     * @param workName the name of the work
     * @param message the FVTMessage object
     * @param workDispatcher the work dispatcher
     */
    public FVTComplexWorkImpl(String workName, FVTMessage message, FVTWorkDispatcher workDispatcher) {
        super(workName);

        svLogger.entering(CLASSNAME, "<init>", new Object[] { workName, message, workDispatcher });

        this.fvtMessage = message;
        this.workDispatcher = workDispatcher;

        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * Send the message(s) to endpoint application(s);
     */
    @Override
    public void run() {
        svLogger.entering(CLASSNAME, "run", this);

        // get the message vector
        messages = fvtMessage.getMessages();
        // get the resources Hashtable
        resources = fvtMessage.getResources();

        // loop through the message vector
        for (int i = 0; i < messages.size(); i++) {
            String message = (String) messages.elementAt(i);

            if (message.startsWith(FVTMessage.BEFORE_DELIVERY_STRING)) {
                // Get the target method.
                targetMethod = (Method) messages.elementAt(++i);

                beforeDeliveryCall(message);
            } else if (message.startsWith(FVTMessage.AFTER_DELIVERY_STRING)) {
                afterDeliveryCall(message);
            } else if (message.startsWith(FVTMessage.RELEASE_STRING)) {
                releaseCall(message);
            } else if (message.startsWith(FVTMessage.TEST_RESULT_STRING)) {
                addTestResult(message);
            } else {
                // Get the target method.
                targetMethod = (Method) messages.elementAt(++i);
                messageCall(message);
            }
        }

        svLogger.exiting(CLASSNAME, "run");
    }

    // todo: add an option B delivery method in FVTMessage class

    // d174149 - add exception handling
    /**
     * <p>Before delivery call. </p>
     *
     * @param message the string which contains the before delivery information
     */
    protected void beforeDeliveryCall(String message) {
        svLogger.entering(CLASSNAME, "beforeDeliveryCall", new Object[] { this, message });

        // beforeDelivery call
        // message format: beforeDelivery:endpointName:instanceID.
        String str[] = parseMessage(message);
        String endpointName = str[1];
        String instanceId = str[2];

        MessageEndpointWrapper endpoint = null;
        MessageEndpointTestResults testResult = null;

        String key = endpointName + instanceId;

        if ((endpoint = getEndpointWrapper(key)) == null) {

            // Cannot find the instance in the hashtable. Create a new one

            // get the message endpoint factory
            MessageEndpointFactoryWrapper factory = workDispatcher.getMessageFactory(endpointName);

            // d180125 - check whether the factory is null or not.
            if (factory == null) {
                svLogger.info("If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                svLogger.exiting(CLASSNAME, "beforeDeliveryCall", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            // Find the FVT XA resource.
            FVTXAResourceImpl xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                svLogger.info("Cannot find XA resource from the message object.");
            } else {
                svLogger.info("find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException re) {
                svLogger.info("UnavailableException is thrown in beforeDeliveryCall: " + AdapterUtil.stackTraceToString(re));
                svLogger.exiting(CLASSNAME, "beforeDeliveryCall", "UnavailableException");

                throw new WorkRuntimeException("UnavailableException is thrown in beforeDeliveryCall", re);
            }

            // Put the instance in the hash table
            addEndpointWrapper(key, endpoint);
        } else { // 173990
            testResult = endpoint.getTestResult();
        }

        try {
            // beforeDelivery  call
            endpoint.beforeDelivery(targetMethod);
        } catch (NoSuchMethodException nsme) {
            svLogger.info("NoSuchMethodException is thrown in beforeDeliveryCall: " + AdapterUtil.stackTraceToString(nsme));
            svLogger.exiting(CLASSNAME, "beforeDeliveryCall", "NoSuchMethodException");

            throw new WorkRuntimeException("NoSuchMethodException is thrown in beforeDeliveryCall", nsme);
        } catch (IllegalStateException ise) {
            svLogger.info("IllegalStateException is thrown in beforeDeliveryCall: " + AdapterUtil.stackTraceToString(ise));

            if (testResult != null) {
                svLogger.info("beforeDeliveryCall: set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                svLogger.exiting(CLASSNAME, "beforeDeliveryCall", "IllegalStateException");
                throw new WorkRuntimeException("IllegalStateException is thrown in beforeDeliveryCall", ise);
            }
        } catch (javax.resource.spi.IllegalStateException sise) {
            svLogger.info("javax.resource.spi.IllegalStateException is thrown in beforeDeliveryCall: " + AdapterUtil.stackTraceToString(sise));

            if (testResult != null) {
                svLogger.info("beforeDeliveryCall: set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                svLogger.exiting(CLASSNAME, "beforeDeliveryCall", "javax.resource.spi.IllegalStateException");
                throw new WorkRuntimeException("javax.resource.spi.IllegalStateException is thrown in beforeDeliveryCall", sise);
            }
        } catch (ResourceException re) {
            svLogger.info("ResourceException is thrown in beforeDeliveryCall: " + AdapterUtil.stackTraceToString(re));
            svLogger.exiting(CLASSNAME, "beforeDeliveryCall", "ResourceException");
            throw new WorkRuntimeException("ResourceException is thrown in beforeDeliveryCall", re);
        }

        // 173990 - set optionBMessageDeliveryUsedto true
        if (testResult != null) {
            svLogger.info("beforeDeliverycall: Set option B Message Delivery to the test result");
            testResult.setOptionBMessageDelivery();
        }

        svLogger.exiting(CLASSNAME, "beforeDeliveryCall");
    }

    // d174149 - add exception handling
    /**
     * After delivery call
     *
     * @param message the string which contains the after delivery information
     */
    protected void afterDeliveryCall(String message) {
        svLogger.entering(CLASSNAME, "afterDeliveryCall", new Object[] { this, message });

        // afterDelivery call
        // message format: afterDelivery:endpointName:instanceID.

        String str[] = parseMessage(message);

        String endpointName = str[1];
        String instanceId = str[2];

        MessageEndpointWrapper endpoint = null;
        MessageEndpointTestResults testResult = null;

        String key = endpointName + instanceId;

        if ((endpoint = getEndpointWrapper(key)) == null) {

            // get the message endpoint factory
            MessageEndpointFactoryWrapper factory = workDispatcher.getMessageFactory(endpointName);

            // d180125 - check whether the factory is null or not.
            if (factory == null) {
                svLogger.info("If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                svLogger.exiting(CLASSNAME, "afterDeliveryCall", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            FVTXAResourceImpl xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                svLogger.info("Cannot find XA resource from the message object.");
            } else {
                svLogger.info("find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException re) {
                svLogger.info("UnavailableException is thrown in afterDeliveryCall: " + AdapterUtil.stackTraceToString(re));
                svLogger.exiting(CLASSNAME, "afterDeliveryCall", "UnavailableException");

                throw new WorkRuntimeException("UnavailableException is thrown in afterDeliveryCall", re);
            }

            addEndpointWrapper(key, endpoint);
        } else {
            testResult = endpoint.getTestResult();
        }

        // null out the targetMethod since the delovery is going to be ended.
        targetMethod = null;

        try {

            // afterDelivery  call
            endpoint.afterDelivery();
        } catch (IllegalStateException ise) {
            svLogger.info("IllegalStateException is thrown in afterDeliveryCall: " + AdapterUtil.stackTraceToString(ise));

            if (testResult != null) {
                svLogger.info("afterDeliveryCall: set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                svLogger.exiting(CLASSNAME, "afterDeliveryCall", "IllegalStateException");
                throw new WorkRuntimeException("IllegalStateException is thrown in afterDeliveryCall", ise);
            }
        } catch (javax.resource.spi.IllegalStateException sise) {
            svLogger.info("javax.resource.spi.IllegalStateException is thrown in afterDeliveryCall: " + AdapterUtil.stackTraceToString(sise));

            if (testResult != null) {
                svLogger.info("afterDeliveryCall: set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                svLogger.exiting(CLASSNAME, "afterDeliveryCall", "javax.resource.spi.IllegalStateException");
                throw new WorkRuntimeException("javax.resource.spi.IllegalStateException is thrown in afterDeliveryCall", sise);
            }
        } catch (ResourceException re) {
            // TransactionRolledBackException may be expected when
            // testing for a failed message delivery attempt. Logging
            // with TestResults allows the client test to determine
            // the rollback occurred as expected, and allows the
            // test to continue and attempt delivery retry.     d248457.2
            Throwable chainedThrowable = re.getCause();
            svLogger.info("chained Throwable is of type: " + chainedThrowable.getClass());
            svLogger.info("testResult is: " + testResult);

            if (chainedThrowable instanceof TransactionRolledbackLocalException && testResult != null) {
                svLogger.info("afterDeliveryCall: set raCaughtTransactionRolledBackException to true");
                testResult.setRaCaughtTransactionRolledbackLocalException();
            } else {
                svLogger.info("ResourceException is thrown in run(): " + AdapterUtil.stackTraceToString(re));
                svLogger.exiting(CLASSNAME, "afterDeliveryCall", "ResourceException");
                throw new WorkRuntimeException("ResourceException is thrown in run()", re);
            }
        }

        svLogger.exiting(CLASSNAME, "afterDeliveryCall");
    }

    // d174149 - add exception handling
    /**
     * Release call
     *
     * @param message the string which contains the release information
     */
    protected void releaseCall(String message) {
        svLogger.entering(CLASSNAME, "releaseCall", new Object[] { this, message });

        // release call
        // message format: release:endpointName:instanceID.

        String str[] = parseMessage(message);

        String endpointName = str[1];
        String instanceId = str[2];
        MessageEndpointWrapper endpoint = null;
        MessageEndpointTestResults testResult = null; // 174149

        String key = endpointName + instanceId;

        if ((endpoint = getEndpointWrapper(key)) == null) {

            // get the message endpoint factory
            MessageEndpointFactoryWrapper factory = workDispatcher.getMessageFactory(endpointName);

            // d180125 - check whether the factory is null or not.
            if (factory == null) {
                svLogger.info("If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                svLogger.exiting(CLASSNAME, "releaseCall", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            FVTXAResourceImpl xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                svLogger.info("Cannot find XA resource from the message object.");
            } else {
                svLogger.info("find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException ue) {
                svLogger.info("UnavailableException is thrown in releaseCall: " + AdapterUtil.stackTraceToString(ue));
                svLogger.exiting(CLASSNAME, "releaseCall", "UnavailableException");
                throw new WorkRuntimeException("UnavailableException is thrown in releaseCall", ue);
            }

            addEndpointWrapper(key, endpoint);
        } else { // 174149
            testResult = endpoint.getTestResult();
        }

        try {
            // release call
            endpoint.release();
        } catch (IllegalStateException ise) {
            svLogger.info("IllegalStateException is thrown in releaseCall: " + AdapterUtil.stackTraceToString(ise));

            if (testResult != null) {
                svLogger.info("releaseCall: set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                svLogger.exiting(CLASSNAME, "releaseCall", "IllegalStateException");
                throw new WorkRuntimeException("IllegalStateException is thrown in releaseCall", ise);
            }
        }

        svLogger.exiting(CLASSNAME, "releaseCall");
    }

    // d174149 - add exception handling
    /**
     * Add a test result to the endpoint instace.
     *
     * @param message the string which contains the result set information
     */
    protected void addTestResult(String message) {
        svLogger.entering(CLASSNAME, "addTestResult", new Object[] { this, message });

        // message call
        // message format: TestResult:endpointName:instanceID.

        String str[] = parseMessage(message);

        String endpointName = str[1];
        String instanceId = str[2];

        MessageEndpointWrapper endpoint = null;

        String key = endpointName + instanceId;

        if ((endpoint = getEndpointWrapper(key)) == null) {

            // get the message endpoint factory
            MessageEndpointFactoryWrapper factory = workDispatcher.getMessageFactory(endpointName);

            // d180125 - check whether the factory is null or not.
            if (factory == null) {
                svLogger.info("If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                svLogger.exiting(CLASSNAME, "addTestResult", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            FVTXAResourceImpl xaResource = null;

            // get the xa resource
            xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                svLogger.info("Cannot find XA resource from the message object.");
            } else {
                svLogger.info("find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException ue) {
                svLogger.info("UnavailableException is thrown in addTestResult: " + AdapterUtil.stackTraceToString(ue));
                svLogger.exiting(CLASSNAME, "addTestResult", "UnavailableException");
                throw new WorkRuntimeException("UnavailableException is thrown in addTestResult", ue);
            }

            addEndpointWrapper(key, endpoint);
        }

        endpoint.addTestResult();
        svLogger.exiting(CLASSNAME, "addTestResult");
    }

    // d174149 - Add exception handling
    /**
     * call the method which receives the message.
     *
     * @param message the string which contains the message call information
     */
    protected void messageCall(String message) {
        // todo: catch Throwable in invoke.
        // todo: check the exception

        svLogger.entering(CLASSNAME, "messageCall", new Object[] { this, message });

        // mesage call
        // message format: endpointName:message:instanceID.

        String str[] = parseMessage(message);

        String endpointName = str[0];
        String msg = str[1];
        String instanceId = str[2];

        String key = endpointName + instanceId;

        MessageEndpointWrapper endpoint = null;

        MessageEndpointTestResults testResult = null;

        // get the message endpoint factory
        MessageEndpointFactoryWrapper factory = workDispatcher.getMessageFactory(endpointName);

        // d180125 - check whether the factory is null or not.
        if (factory == null) {
            svLogger.info("If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
            svLogger.exiting(CLASSNAME, "messageCall", "Exception -- Cannot find the factory");

            throw new WorkRuntimeException("Cannot find the factory");
        }

        if ((endpoint = getEndpointWrapper(key)) == null) {
            FVTXAResourceImpl xaResource = null;

            // get the xa resource
            xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                svLogger.info("Cannot find XA resource from the message object.");
            } else {
                svLogger.info("find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException ue) {
                svLogger.info("UnavailableException is thrown in messageCall: " + AdapterUtil.stackTraceToString(ue));
                svLogger.exiting(CLASSNAME, "messageCall", "UnavailableException");
                throw new WorkRuntimeException("UnavailableException is thrown in messageCall", ue);
            }

            addEndpointWrapper(key, endpoint);
        } else {
            // get the test result
            testResult = endpoint.getTestResult();
        }

        if (targetMethod == null) {
            // use onMessage() method
            if (testResult != null) {

                // 173990 - If optionBMessageDeliveryUsed is still false, which means
                // no beforeDelivery call is called before, set optionAMessageDeliveryUsed
                // to true
                if (!testResult.optionBMessageDeliveryUsed()) {
                    svLogger.info("messageCall: Set option A Message Delivery to the test result");
                    testResult.setOptionAMessageDelivery();
                }

                try {
                    Method m = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });

                    // set isDeliveryTransacted information.
                    testResult.setIsDeliveryTransacted(factory.isDeliveryTransacted(m));
                } catch (NoSuchMethodException nsme) {
                    // nothing to do
                }
            }

            try {
                ((MessageListener) endpoint.getEndpoint()).onMessage(new TextMessageImpl(msg));
            } catch (IllegalStateException ise) {
                svLogger.info("IllegalStateException is thrown in messageCall: " + AdapterUtil.stackTraceToString(ise));

                if (testResult != null) {
                    svLogger.info("messageCall: set raCaughtIllegalStateException to true");
                    testResult.setRaCaughtIllegalStateException();
                } else {
                    svLogger.exiting(CLASSNAME, "messageCall", "IllegalStateException");
                    throw new WorkRuntimeException("IllegalStateException is thrown in messageCall", ise);
                }
            }
        } else {
            svLogger.info("messageCall: targetMethod is not null :" + targetMethod.getName());

            if (testResult != null) {
                // 173990 - If optionBMessageDeliveryUsed is still false, which means
                // no beforeDelivery call is called before, set optionAMessageDeliveryUsed
                // to true
                if (!testResult.optionBMessageDeliveryUsed()) {
                    svLogger.info("messageCall: Set option A Message Delivery to the test result");
                    testResult.setOptionAMessageDelivery();
                }

                try {
                    // set isDeliveryTransacted information.
                    testResult.setIsDeliveryTransacted(factory.isDeliveryTransacted(targetMethod));
                } catch (NoSuchMethodException nsme) {
                    // throw a WorkRuntimeException
                    svLogger.info("NoSuchMethodException: " + AdapterUtil.stackTraceToString(nsme));
                    svLogger.exiting(CLASSNAME, "messageCall", "NoSuchMethodException");
                    throw new WorkRuntimeException("NoSuchMethodException is caught in messageCall", nsme);
                }
            }

            // use targetMethod

            // First get the parameter types
            Class paramTypes[] = targetMethod.getParameterTypes();

            if (paramTypes.length == 0)
                svLogger.info("messageCall: targetMethod doesn't have parameters");
            else
                svLogger.info("messageCall: targetMethod has parameter " + paramTypes[0].getName());

            Constructor constructor = null;
            try {
                if (paramTypes[0].equals(Message.class)) {
                    // The parameter type is javax.jms.Message.
                    // We use the constructor of TestMessageImpl
                    constructor = TextMessageImpl.class.getConstructor(new Class[] { String.class });
                } else {
                    // This parameter type should have a constructor with a parameter String
                    constructor = paramTypes[0].getConstructor(new Class[] { String.class });
                }
            } catch (NoSuchMethodException nsme) {
                svLogger.info("NoSuchMethodException is thrown in MessageCall: " + AdapterUtil.stackTraceToString(nsme));
                svLogger.exiting(CLASSNAME, "messageCall", "NoSuchMethodException");
                throw new WorkRuntimeException("NoSuchMethodException is thrown in MessageCall", nsme);
            }

            svLogger.info("messageCall: constructor is " + constructor);

            // construct the message
            Object messageObj = null;
            try {
                messageObj = constructor.newInstance(new Object[] { msg });
            } catch (IllegalAccessException iae) {
                svLogger.info("IllegalAccessException is thrown in MessageCall: " + AdapterUtil.stackTraceToString(iae));
                svLogger.exiting(CLASSNAME, "messageCall", "IllegalAccessException");
                throw new WorkRuntimeException("IllegalAccessException is thrown in MessageCall", iae);
            } catch (InvocationTargetException ite) {
                svLogger.info("InvocationTargetException is thrown in MessageCall: " + AdapterUtil.stackTraceToString(ite));
                svLogger.exiting(CLASSNAME, "messageCall", "InvocationTargetException");
                throw new WorkRuntimeException("InvocationTargetException is thrown in MessageCall", ite);
            } catch (InstantiationException ie) {
                svLogger.info("InstantiationException is thrown in MessageCall: " + AdapterUtil.stackTraceToString(ie));
                svLogger.exiting(CLASSNAME, "messageCall", "InstantiationException");
                throw new WorkRuntimeException("InstantiationException is thrown in MessageCall", ie);
            }
            svLogger.info("messageCall: messageObj is " + messageObj);

            try {
                svLogger.info("messageCall: endpoint is " + endpoint.getEndpoint());
                svLogger.info("messageCall: endpoint class is " + endpoint.getEndpoint().getClass());

                // send the message
                targetMethod.invoke(endpoint.getEndpoint(), new Object[] { messageObj });
            } catch (IllegalAccessException iae) {
                svLogger.info("IllegalAccessException is thrown in MessageCall: " + AdapterUtil.stackTraceToString(iae));
                svLogger.exiting(CLASSNAME, "messageCall", "IllegalAccessException");
                throw new WorkRuntimeException("IllegalAccessException is thrown in MessageCall", iae);
            } catch (InvocationTargetException ite) {
                svLogger.info("InvocationTargetException is thrown in MessageCall: " + AdapterUtil.stackTraceToString(ite));
                Throwable chainedThrowable = ite.getTargetException();

                svLogger.info("chained Throwable is of type " + chainedThrowable.getClass());
                svLogger.info("testResult is " + testResult);

                // If the chained Throwable is IllegalStateException, set it to the test result,
                // and eat this exception.
                if ((chainedThrowable instanceof javax.resource.spi.IllegalStateException || chainedThrowable instanceof IllegalStateException) && testResult != null) {
                    svLogger.info("messageCall: set raCaughtIllegalStateException to true");
                    testResult.setRaCaughtIllegalStateException();
                }
                // EJBException may be expected when testing
                // for a failed message delivery attempt.
                // Logging with TestResults allows the client
                // test to determine the EJBException occurred
                // as expected, and allows the test to continue
                // and invoke afterDelivery.                           d248457.2
                else if (chainedThrowable instanceof EJBException && testResult != null) {
                    svLogger.info("messageCall: set raCaughtEJBException to true");
                    testResult.setRaCaughtEJBException();
                } else {
                    svLogger.exiting(CLASSNAME, "messageCall", "InvocationTargetException");
                    throw new WorkRuntimeException("InvocationTargetException is thrown in MessageCall", ite);
                }
            } catch (IllegalStateException ise) {
                svLogger.info("IllegalStateException is thrown in messageCall: " + AdapterUtil.stackTraceToString(ise));

                if (testResult != null) {
                    svLogger.info("messageCall: set raCaughtIllegalStateException to true");
                    testResult.setRaCaughtIllegalStateException();
                } else {
                    svLogger.exiting(CLASSNAME, "messageCall", "IllegalStateException");
                    throw new WorkRuntimeException("IllegalStateException is thrown in MessageCall", ise);
                }
            }
        }

        svLogger.exiting(CLASSNAME, "MessageCall");
    }

    /**
     * parse the message into different strings.
     */
    private String[] parseMessage(String msg) {
        String str[] = new String[4];
        int i = 0;

        StringTokenizer tokenizer = new StringTokenizer(msg, ":");

        while (tokenizer.hasMoreTokens()) {
            str[i++] = tokenizer.nextToken();
        }

        return str;
    }

    /**
     * Sets the workDispatcher.
     *
     * @param workDispatcher The workDispatcher to set
     */
    public void setWorkDispatcher(FVTWorkDispatcher workDispatcher) {
        this.workDispatcher = workDispatcher;
    }

    /**
     * recycle the object for reuse
     *
     * @param workName the name of the work, also called the delivery ID
     * @param message the FVTMessage object
     * @param workDispatcher the work dispatcher
     *
     * @return this instance
     */
    public FVTComplexWorkImpl recycle(String workName, FVTMessage message, FVTWorkDispatcher workDispatcher) {

        svLogger.entering(CLASSNAME, "recycle", new Object[] { this, workName, message, workDispatcher });

        // If passed-in workName is null, use the hash code of the work as the workName.
        if (workName == null || workName.equals("") || workName.trim().equals(""))
            name = "" + this.hashCode();

        // Set the state to INITIAL state.
        state = INITIAL;

        this.fvtMessage = message;
        this.workDispatcher = workDispatcher;
        instances = null;

        firstInstanceKey = null;
        instance = null;

        svLogger.exiting(CLASSNAME, "recycle", this);

        return this;
    }
}