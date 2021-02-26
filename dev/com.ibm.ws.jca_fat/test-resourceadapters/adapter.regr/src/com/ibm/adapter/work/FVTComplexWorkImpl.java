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

package com.ibm.adapter.work;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.ejb.EJBException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.UnavailableException;

import com.ibm.adapter.AdapterUtil;
import com.ibm.adapter.endpoint.MessageEndpointFactoryWrapper;
import com.ibm.adapter.endpoint.MessageEndpointWrapper;
import com.ibm.adapter.message.FVTMessage;
import com.ibm.adapter.message.TextMessageImpl;
import com.ibm.adapter.tra.FVTXAResourceImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.csi.MessageEndpointTestResults;

/**
 * <p>This class extends FVTWorkImpl class. An object of this class represents a complex work
 * to be submitted to the work manager for execution. This complex work can send one or
 * more messages to one or more endpoint instances.</p>
 */
public class FVTComplexWorkImpl extends FVTGeneralWorkImpl {
    private static final TraceComponent tc = Tr.register(FVTComplexWorkImpl.class);

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

        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { workName, message, workDispatcher });

        this.fvtMessage = message;
        this.workDispatcher = workDispatcher;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * Send the message(s) to endpoint application(s);
     */
    @Override
    public void run() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "run", this);

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

        if (tc.isEntryEnabled())
            Tr.exit(tc, "run");

    }

    /**
     * <p>Before delivery call. </p>
     *
     * @param message the string which contains the before delivery information
     */
    protected void beforeDeliveryCall(String message) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "beforeDeliveryCall", new Object[] { this, message });

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
                if (tc.isEntryEnabled())
                    Tr.debug(tc, "If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                Tr.exit(tc, "beforeDeliveryCall", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            // Find the FVT XA resource.
            FVTXAResourceImpl xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Cannot find XA resource from the message object.");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException re) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "UnavailableException is thrown in beforeDeliveryCall",
                             AdapterUtil.stackTraceToString(re));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "beforeDeliveryCall", "Exception");

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
            if (tc.isDebugEnabled())
                Tr.debug(tc, "NoSuchMethodException is thrown in beforeDeliveryCall", AdapterUtil.stackTraceToString(nsme));
            if (tc.isEntryEnabled())
                Tr.exit(tc, "beforeDeliveryCall", "Exception");

            throw new WorkRuntimeException("NoSuchMethodException is thrown in beforeDeliveryCall", nsme);

        } catch (java.lang.IllegalStateException ise) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "java.lang.IllegalStateException is thrown in beforeDeliveryCall",
                         AdapterUtil.stackTraceToString(ise));

            if (testResult != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "beforeDeliveryCall", "set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "beforeDeliveryCall", "Exception");
                throw new WorkRuntimeException("java.lang.IllegalStateException is thrown in beforeDeliveryCall", ise);
            }
        } catch (javax.resource.spi.IllegalStateException sise) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "IllegalStateException is thrown in beforeDeliveryCall", AdapterUtil.stackTraceToString(sise));

            if (testResult != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "beforeDeliveryCall", "set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "beforeDeliveryCall", "Exception");
                throw new WorkRuntimeException("IllegalStateException is thrown in beforeDeliveryCall", sise);
            }
        } catch (ResourceException re) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "ResourceException is thrown in beforeDeliveryCall", AdapterUtil.stackTraceToString(re));
            if (tc.isEntryEnabled())
                Tr.exit(tc, "beforeDeliveryCall", "Exception");

            throw new WorkRuntimeException("ResourceException is thrown in beforeDeliveryCall", re);

        }

        // 173990 - set optionBMessageDeliveryUsedto true
        if (testResult != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "beforeDeliverycall", "Set option B Message Delivery to the test result");
            testResult.setOptionBMessageDelivery();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "beforeDeliveryCall");
    }

    // d174149 - add exception handling
    /**
     * After delivery call
     *
     * @param message the string which contains the after delivery information
     */
    protected void afterDeliveryCall(String message) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "afterDeliveryCall", new Object[] { this, message });

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
                if (tc.isEntryEnabled())
                    Tr.debug(tc, "If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                Tr.exit(tc, "afterDeliveryCall", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            FVTXAResourceImpl xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Cannot find XA resource from the message object. ");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException re) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "UnavailableException is thrown in afterDeliveryCall", AdapterUtil.stackTraceToString(re));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "afterDeliveryCall", "Exception");

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
        } catch (java.lang.IllegalStateException ise) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "java.lang.IllegalStateException is thrown in afterDeliveryCall",
                         AdapterUtil.stackTraceToString(ise));

            if (testResult != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "afterDeliveryCall", "set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "afterDeliveryCall", "Exception");
                throw new WorkRuntimeException("java.lang.IllegalStateException is thrown in afterDeliveryCall", ise);
            }
        } catch (javax.resource.spi.IllegalStateException sise) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "IllegalStateException is thrown in afterDeliveryCall", AdapterUtil.stackTraceToString(sise));

            if (testResult != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "afterDeliveryCall", "set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "afterDeliveryCall", "Exception");
                throw new WorkRuntimeException("IllegalStateException is thrown in afterDeliveryCall", sise);
            }
        } catch (ResourceException re) {
            // TransactionRolledBackException may be expected when
            // testing for a failed message delivery attempt. Logging
            // with TestResults allows the client test to determine
            // the rollback occurred as expected, and allows the
            // test to continue and attempt delivery retry.     d248457.2
            Throwable chainedThrowable = re.getCause();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "chained Throwable is of type " + chainedThrowable.getClass());
                Tr.debug(tc, "testResult is " + testResult);
            }
            if (chainedThrowable instanceof TransactionRolledbackLocalException &&
                testResult != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "afterDeliveryCall", "set raCaughtTransactionRolledBackException to true");
                testResult.setRaCaughtTransactionRolledbackLocalException();
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "ResourceException is thrown in run()", AdapterUtil.stackTraceToString(re));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "afterDeliveryCall", "Exception");
                throw new WorkRuntimeException("ResourceException is thrown in run()", re);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "afterDeliveryCall");
    }

    // d174149 - add exception handling
    /**
     * Release call
     *
     * @param message the string which contains the release information
     */
    protected void releaseCall(String message) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "releaseCall", new Object[] { this, message });

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
                if (tc.isEntryEnabled())
                    Tr.debug(tc, "If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                Tr.exit(tc, "releaseCall", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            FVTXAResourceImpl xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Cannot find XA resource from the message object.");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException ue) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "UnavailableException is thrown in releaseCall", AdapterUtil.stackTraceToString(ue));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "releaseCall", "Exception");
                throw new WorkRuntimeException("UnavailableException is thrown in releaseCall", ue);

            }

            addEndpointWrapper(key, endpoint);
        } else { // 174149
            testResult = endpoint.getTestResult();
        }

        try {
            // release call
            endpoint.release();
        } catch (java.lang.IllegalStateException ise) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "java.lang.IllegalStateException is thrown in releaseCall",
                         AdapterUtil.stackTraceToString(ise));

            if (testResult != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "releaseCall", "set raCaughtIllegalStateException to true");
                testResult.setRaCaughtIllegalStateException();
            } else {

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "releaseCall", "Exception");
                throw new WorkRuntimeException("java.lang.IllegalStateException is thrown in releaseCall", ise);
            }

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "releaseCall");
    }

    // d174149 - add exception handling
    /**
     * Add a test result to the endpoint instace.
     *
     * @param message the string which contains the result set information
     */
    protected void addTestResult(String message) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addTestResult", new Object[] { this, message });

        // mesage call
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
                if (tc.isEntryEnabled())
                    Tr.debug(tc, "If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
                Tr.exit(tc, "addTestResult", "Exception -- Cannot find the factory");

                throw new WorkRuntimeException("Cannot find the factory");
            }

            FVTXAResourceImpl xaResource = null;

            // get the xa resource
            xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Cannot find XA resource from the message object.");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException ue) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "UnavailableException is thrown in addTestResult", AdapterUtil.stackTraceToString(ue));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "addTestResult", "Exception");
                throw new WorkRuntimeException("UnavailableException is thrown in addTestResult", ue);

            }

            addEndpointWrapper(key, endpoint);
        }

        endpoint.addTestResult();
        if (tc.isEntryEnabled())
            Tr.exit(tc, "addTestResult");

    }

    // d174149 - Add exception handling
    /**
     * call the method which receives the message.
     *
     * @param message the string which contains the message call information
     */
    protected void messageCall(String message) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "messageCall", new Object[] { this, message });

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
            if (tc.isEntryEnabled())
                Tr.debug(tc, "If factory is null, ensure EJB module is 2.1. Check ejb-jar.xml");
            Tr.exit(tc, "messageCall", "Exception -- Cannot find the factory");

            throw new WorkRuntimeException("Cannot find the factory");
        }

        if ((endpoint = getEndpointWrapper(key)) == null) {

            FVTXAResourceImpl xaResource = null;

            // get the xa resource
            xaResource = (FVTXAResourceImpl) resources.get(key);

            if (xaResource == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Cannot find XA resource from the message object.");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "find an XA resource from the message object: " + xaResource);
            }

            try {
                // create an message endpoint
                endpoint = (MessageEndpointWrapper) factory.createEndpoint(xaResource);
            } catch (UnavailableException ue) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "UnavailableException is thrown in messageCall", AdapterUtil.stackTraceToString(ue));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "messageCall", "Exception");
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
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "messageCall", "Set option A Message Delivery to the test result");
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
            } catch (java.lang.IllegalStateException ise) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "java.lang.IllegalStateException is thrown in messageCall",
                             AdapterUtil.stackTraceToString(ise));

                if (testResult != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "messageCall", "set raCaughtIllegalStateException to true");
                    testResult.setRaCaughtIllegalStateException();
                } else {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "messageCall", "Exception");
                    throw new WorkRuntimeException("java.lang.IllegalStateException is thrown in messageCall", ise);
                }
            }

        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "messageCall", "targetMethod is not null :" + targetMethod.getName());

            if (testResult != null) {

                // 173990 - If optionBMessageDeliveryUsed is still false, which means
                // no beforeDelivery call is called before, set optionAMessageDeliveryUsed
                // to true
                if (!testResult.optionBMessageDeliveryUsed()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "messageCall", "Set option A Message Delivery to the test result");
                    testResult.setOptionAMessageDelivery();
                }

                try {
                    // set isDeliveryTransacted information.
                    testResult.setIsDeliveryTransacted(factory.isDeliveryTransacted(targetMethod));
                } catch (NoSuchMethodException nsme) {
                    // throw a WorkRuntimeException
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "NoSuchMethodException", AdapterUtil.stackTraceToString(nsme));
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "messageCall", "Exception");
                    throw new WorkRuntimeException("NoSuchMethodException is caught in messageCall", nsme);
                }
            }

            // use targetMethod

            // First get the parameter types
            Class paramTypes[] = targetMethod.getParameterTypes();

            if (tc.isDebugEnabled()) {
                if (paramTypes.length == 0)
                    Tr.debug(tc, "messageCall", "targetMethod doesn't have parameters");
                else
                    Tr.debug(tc, "messageCall", "targetMethod has parameter " + paramTypes[0].getName());
            }

            Constructor constructor = null;
            try {
                if (paramTypes[0].equals(javax.jms.Message.class)) {
                    // The parameter type is javax.jms.Message.
                    // We use the constructor of TestMessageImpl
                    constructor = TextMessageImpl.class.getConstructor(new Class[] {
                                                                                     String.class });
                } else {
                    // This parameter type should have a constructor with a parameter String
                    constructor = paramTypes[0].getConstructor(new Class[] { String.class });
                }
            } catch (NoSuchMethodException nsme) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "NoSuchMethodException is thrown in MessageCall", AdapterUtil.stackTraceToString(nsme));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "messageCall", "Exception");
                throw new WorkRuntimeException("NoSuchMethodException is thrown in MessageCall", nsme);

            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "messageCall", "constructor is " + constructor);

            // construct the message
            Object messageObj = null;
            try {
                messageObj = constructor.newInstance(new Object[] { msg });
            } catch (IllegalAccessException iae) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "IllegalAccessException is thrown in MessageCall", AdapterUtil.stackTraceToString(iae));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "messageCall", "Exception");
                throw new WorkRuntimeException("IllegalAccessException is thrown in MessageCall", iae);

            } catch (InvocationTargetException ite) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "InvocationTargetException is thrown in MessageCall", AdapterUtil.stackTraceToString(ite));

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "messageCall", "Exception");
                throw new WorkRuntimeException("InvocationTargetException is thrown in MessageCall", ite);
            } catch (InstantiationException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "InstantiationException is thrown in MessageCall", AdapterUtil.stackTraceToString(ie));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "messageCall", "Exception");
                throw new WorkRuntimeException("InstantiationException is thrown in MessageCall", ie);

            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "messageCall", "messageObj is " + messageObj);

            try {

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "messageCall", "endpoint is " + endpoint.getEndpoint());
                    Tr.debug(tc, "messageCall", "endpoint class is " + endpoint.getEndpoint().getClass());
                }

                // send the message
                targetMethod.invoke(endpoint.getEndpoint(), new Object[] { messageObj });
            } catch (IllegalAccessException iae) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "IllegalAccessException is thrown in MessageCall", AdapterUtil.stackTraceToString(iae));
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "messageCall", "Exception");
                throw new WorkRuntimeException("IllegalAccessException is thrown in MessageCall", iae);

            } catch (InvocationTargetException ite) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "InvocationTargetException is thrown in MessageCall", AdapterUtil.stackTraceToString(ite));

                Throwable chainedThrowable = ite.getTargetException();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "chained Throwable is of type " + chainedThrowable.getClass());
                    Tr.debug(tc, "testResult is " + testResult);
                }

                // If the chained Throwable is IllegealStateException, set it to the test result,
                // and eat this exception.
                if ((chainedThrowable instanceof IllegalStateException
                     || chainedThrowable instanceof java.lang.IllegalStateException)
                    && testResult != null) {

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "messageCall", "set raCaughtIllegalStateException to true");
                    testResult.setRaCaughtIllegalStateException();
                }
                // EJBException may be expected when testing
                // for a failed message delivery attempt.
                // Logging with TestResults allows the client
                // test to determine the EJBException occurred
                // as expected, and allows the test to continue
                // and invoke afterDelivery.                           d248457.2
                else if (chainedThrowable instanceof EJBException &&
                         testResult != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "messageCall", "set raCaughtEJBException to true");
                    testResult.setRaCaughtEJBException();
                } else {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "messageCall", "Exception");
                    throw new WorkRuntimeException("InvocationTargetException is thrown in MessageCall", ite);
                }
            } catch (java.lang.IllegalStateException ise) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "java.lang.IllegalStateException is thrown in messageCall",
                             AdapterUtil.stackTraceToString(ise));

                if (testResult != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "messageCall", "set raCaughtIllegalStateException to true");
                    testResult.setRaCaughtIllegalStateException();
                } else {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "messageCall", "Exception");
                    throw new WorkRuntimeException("java.lang.IllegalStateException is thrown in MessageCall", ise);
                }
            }

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "MessageCall");
    }

    /**
     * parse the message into different strings.
     */
    protected String[] parseMessage(String msg) {
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
     * @return this intance
     */
    public FVTComplexWorkImpl recycle(String workName, FVTMessage message, FVTWorkDispatcher workDispatcher) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "recycle", new Object[] { this, workName, message, workDispatcher });

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

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recycle", this);

        return this;
    }

}
