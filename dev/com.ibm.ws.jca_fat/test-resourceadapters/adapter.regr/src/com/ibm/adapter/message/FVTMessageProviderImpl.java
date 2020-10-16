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

package com.ibm.adapter.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Timer;

import javax.jms.MessageListener;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.sql.DataSource;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.adapter.EISTimer;
import com.ibm.adapter.FVTAdapterHelper;
import com.ibm.adapter.FVTAdapterImpl;
import com.ibm.adapter.endpoint.MessageEndpointFactoryWrapper;
import com.ibm.adapter.endpoint.MessageEndpointWrapper;
import com.ibm.adapter.tra.FVTWorkImpl;
import com.ibm.adapter.work.FVTComplexWorkImpl;
import com.ibm.adapter.work.FVTGeneralWorkImpl;
import com.ibm.adapter.work.FVTWorkDispatcher;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.csi.MessageEndpointTestResults;

/**
 * <p>
 * This class implements interface
 * <code>fvt.adapter.message.FVTMessageProvider</code> and
 * <code>fvt.adapter.message.FVTBaseMessageProvider</code>.
 * </p>
 *
 * <p>
 * An object of this class simulates the message provider which uses the testing
 * resource adapter to deliver messages to endpoint applications. This
 * MessageProvider has some methods to send different kinds of messages to the
 * endpoint applications via the testing resource adapter.
 * </p>
 *
 * <p>
 * An object of this class is an administered object. When the testing resource
 * adapter is started, the configured administered object will be bound into
 * JNDI name space. Applications (such as test applications) can lookup the
 * configured administered object (configured in applications res-env-ref) to
 * send messages to the endpoint applications.
 * <p>
 *
 * <p>
 * This class also stores XA transaction resources into a text file so it can
 * provide the XAResource object during crash recovery.
 * </p>
 */
public class FVTMessageProviderImpl implements FVTMessageProvider, FVTBaseMessageProvider, Serializable {

    private static final TraceComponent tc = Tr
                    .register(FVTMessageProviderImpl.class);

    /** work dispatcher */
    FVTWorkDispatcher workDispatcher;

    /** adapter instance */
    FVTAdapterImpl adapter;

    /** transaction log file path name */
    // 12/16/03: 
    // public static String tranLog = System.getProperty("WAS_HOME") +
    // "\\temp\tranlog";
    private static String wasInstallRoot = null;
    private static String pathSep = null;
    protected static String tranLog = null;

    /** default maximum waiting time */
    private final static int DEFAULT_WAIT_TIME = 60000;

    // d177221 begin: 
    /** EIS status: running */
    public final static int STATUS_OK = 1;

    /** EIS status: fail */
    public final static int STATUS_FAIL = 0;

    private int EISStatus;

    // 12/01/03: 
    /** Synchronization object */
    private final Integer syncEISTimerObj = new Integer(10);
    // d177221 end: 

    // @alvinso.1
    String property_a;

    String property_m;

    /**
     * <p>
     * Default constructor
     * </p>
     *
     * <p>
     * This constructor will be called when this administered object is looked
     * up.
     * </p>
     */
    public FVTMessageProviderImpl() throws ResourceAdapterInternalException {

        if (tc.isDebugEnabled())
            Tr.debug(tc, "<init>");

        // d177221: Initialize the EIS Status to STATUS_OK
        EISStatus = STATUS_OK;

        // 11/17/03: 
        // Init workDispatcher and adapter to null
        workDispatcher = null;
        adapter = null;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "tranLog", tranLog);
    }

    /**
     * <p>
     * This method sends a message to a particular endpoint application directly
     * without using work manager.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as the endpoint instance used for this message deilivery.
     *            If you don't provide deliveryId, you won't be able to retreive
     *            these information.
     * @param beanName
     *            the message endpoint name
     * @param message
     *            the message going to be delivered to the message endpoint
     * @param xaResource
     *            the XAResource object used for getting transaction
     *            notifications. Set to null if you don't want to get
     *            transaction notifications.
     *
     * @exception ResourceException
     */
    @Override
    public void sendDirectMessage(String deliveryId, String beanName,
                                  String message, XAResource xaResource) throws ResourceException {
        workDispatcher.deliverSimpleMessage(deliveryId, beanName, message, 0,
                                            0, xaResource, FVTMessageProvider.NO_WORK, null);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications directly without using work manager.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances used for this message deilivery. If
     *            you don't provide deliveryId, you won't be able to retreive
     *            these information.
     * @param message
     *            the FVT message object
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendDirectMessage(String deliveryId, FVTMessage message) throws ResourceException {
        workDispatcher.deliverComplexMessage(deliveryId, message, 0, 0,
                                             FVTMessageProvider.NO_WORK, null);
    }

    /**
     * <p>
     * This method sends a message to a particular endpoint application and wait
     * until the message is delivered.
     * </p>
     *
     * <p>
     * This method call blocks until the message delivery is completed.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param beanName
     *            the message endpoint name
     * @param message
     *            the message going to be delivered to the message endpoint
     * @param xaResource
     *            the XAResource object used for getting transaction
     *            notifications. Set to null if you don't want to get
     *            transaction notifications.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     */
    @Override
    public void sendMessage(String deliveryId, String beanName, String message,
                            XAResource xaResource, Xid xid) throws ResourceException {
        workDispatcher.deliverSimpleMessage(deliveryId, beanName, message, 0,
                                            0, xaResource, FVTMessageProvider.DO_WORK, xid);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications and wait until all the message are delivered.
     * </p>
     *
     * <p>
     * This call blocks until all the message deliveries are completed.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and work object. If you don't
     *            provide deliveryId, you won't be able to retreive these
     *            information.
     * @param message
     *            the FVT message object.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendMessage(String deliveryId, FVTMessage message, Xid xid) throws ResourceException {
        workDispatcher.deliverComplexMessage(deliveryId, message, 0, 0,
                                             FVTMessageProvider.DO_WORK, xid);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications and wait until all the message are delivered. What
     * differentiates this from the sendMessage is that it supports Works that
     * implement {@link javax.resource.spi.work.WorkContextProvider} </p>
     *
     * <p>
     * This call blocks until all the message deliveries are completed.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and work object. If you don't
     *            provide deliveryId, you won't be able to retreive these
     *            information.
     * @param message
     *            the FVT message object.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     * @param wi
     *            the WorkInformation used to configure the SecurityContext.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendJCA16Message(String deliveryId, FVTMessage message,
                                 Xid xid, WorkInformation wi) throws ResourceException {
        workDispatcher.deliverComplexJCA16Message(deliveryId, message, 0, 0,
                                                  FVTMessageProvider.DO_WORK, xid, wi);
    }

    /**
     * <p>
     * This method asynchronously sends a message to a particular endpoint
     * application.
     * </p>
     *
     * <p>
     * This method call won't block the execution. It returns once the message
     * is accepted by the application server to deliver.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param beanName
     *            the message endpoint name
     * @param message
     *            the message going to be delivered to the message endpoint
     * @param xaResource
     *            the XAResource object used for getting transaction
     *            notifications. Set to null if you don't want to get
     *            transaction notifications.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     */
    @Override
    public void sendMessageNoWait(String deliveryId, String beanName,
                                  String message, XAResource xaResource, Xid xid) throws ResourceException {
        workDispatcher.deliverSimpleMessage(deliveryId, beanName, message, 0,
                                            0, xaResource, FVTMessageProvider.SCHEDULE_WORK, xid);
    }

    /**
     * <p>
     * This method asynchronously sends one or more messages to one or more
     * endpoint applications.
     * </p>
     *
     * <p>
     * This method call won't block the execution. It returns once the message
     * is accepted by the application server to deliver.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and work object. If you don't
     *            provide deliveryId, you won't be able to retreive these
     *            information.
     * @param message
     *            the FVT message object.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendMessageNoWait(String deliveryId, FVTMessage message, Xid xid) throws ResourceException {
        workDispatcher.deliverComplexMessage(deliveryId, message, 0, 0,
                                             FVTMessageProvider.SCHEDULE_WORK, xid);
    }

    /**
     * <p>
     * This method sends a message to a particular endpoint application and
     * returns once a certain state is reached.
     * </p>
     *
     * <p>
     * This method call will block the execution until the work, which delivers
     * the message, reaches certain state.
     * </p>
     *
     * <p>
     * There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL>
     * </P>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param beanName
     *            the message endpoint name
     * @param message
     *            the message going to be delivered to the message endpoint
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param xaResource
     *            the XAResource object used for getting transaction
     *            notifications. Set to null if you don't want to get
     *            transaction notifications.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     */
    @Override
    public void sendMessageWait(String deliveryId, String beanName,
                                String message, int state, XAResource xaResource, Xid xid) throws ResourceException {
        workDispatcher.deliverSimpleMessage(deliveryId, beanName, message,
                                            state, DEFAULT_WAIT_TIME, xaResource,
                                            FVTMessageProvider.SCHEDULE_WORK, xid);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications and returns once a certain satus is reached.
     *
     * <p>
     * This method call will block the execution until the work, which delivers
     * the message, reaches certain state.
     * </p>
     *
     * <p>
     * There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL>
     * </P>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param message
     *            the FVT message object.
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendMessageWait(String deliveryId, FVTMessage message,
                                int state, Xid xid) throws ResourceException {
        workDispatcher.deliverComplexMessage(deliveryId, message, state,
                                             DEFAULT_WAIT_TIME, FVTMessageProvider.SCHEDULE_WORK, xid);
    }

    /**
     * <p>
     * This method sends a message to a particular endpoint application and
     * returns once a certain state is reached.
     * </p>
     *
     * <p>
     * This method call will block the execution until the work, which delivers
     * the message, reaches certain state.
     * </p>
     *
     * <p>
     * The maximum waiting time is waitTime millisecond. Once the waitTime is
     * reached, the method will return even if the work instance hasn't reach
     * the specified state yet
     * </p>
     *
     * <p>
     * There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL>
     * </P>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param beanName
     *            the message endpoint name
     * @param message
     *            the message going to be delivered to the message endpoint
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum waiting time. Once the
     *            waitTime is reached, the method will return even if the work
     *            instance hasn't reach the specified state yet</p>
     * @param xaResource
     *            the XAResource object used for getting transaction
     *            notifications. Set to null if you don't want to get
     *            transaction notifications.
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     */
    @Override
    public void sendMessageWait(String deliveryId, String beanName,
                                String message, int state, int waitTime, XAResource xaResource,
                                Xid xid) throws ResourceException {
        workDispatcher.deliverSimpleMessage(deliveryId, beanName, message,
                                            state, waitTime, xaResource, FVTMessageProvider.SCHEDULE_WORK,
                                            xid);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications and returns once a certain satus is reached.
     *
     * <p>
     * This method call will block the execution until the work, which delivers
     * the message, reaches certain state.
     * </p>
     *
     * <p>
     * The maximum waiting time is waitTime millisecond. Once the waitTime is
     * reached, the method will return even if the work instance hasn't reach
     * the specified state yet
     * </p>
     *
     * <p>
     * There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL>
     * </P>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param message
     *            the FVT message object.
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum waiting time. Once the
     *            waitTime is reached, the method will return even if the work
     *            instance hasn't reach the specified state yet</p>
     * @param xid
     *            the XID which represents the imported transaction. Set this
     *            parameter to null if no imported transaction exists.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendMessageWait(String deliveryId, FVTMessage message,
                                int state, int waitTime, Xid xid) throws ResourceException {
        workDispatcher.deliverComplexMessage(deliveryId, message, state,
                                             waitTime, FVTMessageProvider.SCHEDULE_WORK, xid);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications and returns once a certain satus is reached. You can sepcify
     * how to submit the work to the work manager.
     * </P>
     *
     * <p>
     * This method call will block the execution until the work, which delivers
     * the message, reaches certain state.
     * </p>
     *
     * <p>
     * The maximum waiting time is waitTime millisecond. Once the waitTime is
     * reached, the method will return even if the work instance hasn't reach
     * the specified state yet
     * </p>
     *
     * <p>
     * There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL>
     * </P>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param message
     *            the FVT message object.
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum waiting time. Once the
     *            waitTime is reached, the method will return even if the work
     *            instance hasn't reach the specified state yet</p>
     * @param xid
     *            the XID which represents the imported transaction. If there is
     *            no import transaction, set xid to null.
     * @param doWorkType
     *            how to submit the work the work manager. It could be any of
     *            the following: DO_WORK, START_WORK or SCHEDULE_WORK
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendMessageWait(String deliveryId, FVTMessage message,
                                int state, int waitTime, Xid xid, int doWorkType) throws ResourceException {
        workDispatcher.deliverComplexMessage(deliveryId, message, state,
                                             waitTime, doWorkType, xid);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications and returns once a certain status is reached. You can
     * specify how to submit the work to the work manager. It also provides a
     * configuration object of type WorkInformation that can be passed by the
     * caller for configuring the SecurityContext that should be established for
     * running the Work
     * </P>
     *
     * <p>
     * This method call will block the execution until the work, which delivers
     * the message, reaches certain state.
     * </p>
     *
     * <p>
     * The maximum waiting time is waitTime millisecond. Once the waitTime is
     * reached, the method will return even if the work instance hasn't reach
     * the specified state yet
     * </p>
     *
     * <p>
     * There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL>
     * </P>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param message
     *            the FVT message object.
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param xid
     *            the XID which represents the imported transaction. If there is
     *            no import transaction, set xid to null.
     * @param doWorkType
     *            how to submit the work the work manager. It could be any of
     *            the following: DO_WORK, START_WORK or SCHEDULE_WORK
     * @param wi
     *            the WorkInformation object that is used to configure the
     *            securityContext.
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendJCA16MessageWait(String deliveryId, FVTMessage message,
                                     int state, int waitTime, Xid xid, int doWorkType, WorkInformation wi) throws ResourceException {
        workDispatcher.deliverComplexJCA16Message(deliveryId, message, state,
                                                  waitTime, doWorkType, xid, wi);
    }

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications and returns once a certain satus is reached. You can sepcify
     * how to submit the work to the work manager.
     * </P>
     *
     * <p>
     * This method call will block the execution until the work, which delivers
     * the message, reaches certain state.
     * </p>
     *
     * <p>
     * The maximum waiting time is waitTime millisecond. Once the waitTime is
     * reached, the method will return even if the work instance hasn't reach
     * the specified state yet
     * </p>
     *
     * <p>
     * There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL>
     * </P>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            message delivery. If you don't provide deliveryId, you won't
     *            be able to retreive these information.
     * @param message
     *            the FVT message object.
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param xid
     *            the XID which represents the imported transaction. If there is
     *            no import transaction, set xid to null.
     * @param doWorkType
     *            how to submit the work the work manager. It could be any of
     *            the following: DO_WORK, START_WORK or SCHEDULE_WORK
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    @Override
    public void sendMessageWait(String deliveryId, FVTMessage message,
                                int state, Xid xid, int doWorkType) throws ResourceException {
        workDispatcher.deliverComplexMessage(deliveryId, message, state,
                                             DEFAULT_WAIT_TIME, doWorkType, xid);

    }

    /**
     * <p>
     * This method call executes two or more works concurrently. This method is
     * mainly used to test associating two or more works concurrently within a
     * source managed transaction (EIS imported transaction). The caller will
     * receive a WorkCompletedException set to the error code
     * WorkException.TX_CONCURRENT_WORK_DISALLOWED
     * </p>
     *
     * <p>
     * This method cannot be called concurrently in several threads. That is,
     * you can not initiate two threads, and have both of them call this method.
     * This usage will lead to unexpected hehavior.
     * </p>
     *
     * <p>
     * This method only guarantees executing these works concurrently. It
     * doesn't guarantee delivering messages to endpoints concurrently.
     * </p>
     *
     * @param works
     *            an array of Work instance. Currently, we only support
     *            executing the work instances which type or super type is
     *            FVTConcurrentWorkImpl.
     * @param xid
     *            the XID which represents the imported transaction. If there is
     *            no import transaction, set xid to null.
     * @param doWorkType
     *            how to submit the work the work manager. It could be any of
     *            the following: DO_WORK, START_WORK or SCHEDULE_WORK
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum time for waiting the work
     *            to reach the specified state. Once the waitTime is reached,
     *            the method will return even if the work instance hasn't reach
     *            the specified state yet.
     *
     * @exception WorkException
     *                Any WorkException. A WorkCompletedException set to the
     *                error code WorkException.TX_CONCURRENT_WORK_DISALLOWED is
     *                expected.
     * @exception ResourceException
     */
    @Override
    public void executeConcurrentWork(Work[] works, Xid xid, int doWorkType,
                                      int state) throws WorkException, ResourceException {
        workDispatcher.executeConcurrentWork(works, new Xid[] { xid },
                                             doWorkType, state, DEFAULT_WAIT_TIME);
    }

    /*
     * public void executeConcurrentWork(Work[] works, Xid xid, int doWorkType)
     * throws WorkException { workDispatcher.executeConcurrentWork(works, new
     * Xid[]{xid}, doWorkType); }
     */

    /**
     * <p>
     * This method call executes two or more works concurrently. This method is
     * mainly used to test associating two or more works concurrently within
     * different source managed transaction (EIS imported transaction). If these
     * Xids are different, the call should be executed successfully.
     * </p>
     *
     * <p>
     * This method cannot be called concurrently in several threads. That is,
     * you can not initiate two threads, and have both of them call this method.
     * This usage will lead to unexpected hehavior.
     * </p>
     *
     * <p>
     * This method only guarantees executing these works concurrently. It
     * doesn't guarantee delivering messages to endpoints concurrently.
     * </p>
     *
     * @param works
     *            an array of Work instance. Currently, we only support
     *            executing the work instances which type or super type is
     *            FVTConcurrentWorkImpl.
     * @param xid
     *            an arary of XIDs which represent different imported
     *            transaction.
     * @param doWorkType
     *            how to submit the work the work manager. It could be any of
     *            the following: DO_WORK, START_WORK or SCHEDULE_WORK
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum time for waiting the work
     *            to reach the specified state. Once the waitTime is reached,
     *            the method will return even if the work instance hasn't reach
     *            the specified state yet.
     *
     * @exception WorkException
     *                Any WorkException.
     * @exception ResourceException
     */
    @Override
    public void executeConcurrentWork(Work[] works, Xid[] xids, int doWorkType,
                                      int state) throws WorkException, ResourceException {
        if (works.length != xids.length)
            throw new WorkException("The size of array works and xids are not equal");
        workDispatcher.executeConcurrentWork(works, xids, doWorkType, state,
                                             DEFAULT_WAIT_TIME);
    }

    /*
     * public void executeConcurrentWork(Work[] works, Xid[] xids, int
     * doWorkType) throws WorkException { if (works.length != xids.length) throw
     * new WorkException("The size of array works and xids are not equal");
     *
     * workDispatcher.executeConcurrentWork(works, xids, doWorkType); }
     *
     *
     * /** <p>This method returns the work instance associated with the message
     * delivery id.</p>
     *
     * <p>After getting the work instance, the test application can call methods
     * isWorkAccepted()/isWorkStarted()/isWorkCompleted/isWorkRejected of the
     * work instance to test whether the work has been
     * accpeted/started/completed/rejected .</p>
     *
     * @param deliveryId the ID related to this message delivery. This message
     * delivery ID is used to retrieve the work instance.
     *
     * @return an FVT work object.
     */
    @Override
    public FVTWorkImpl getWork(String deliveryId) {
        return workDispatcher.getWork(deliveryId);
    }

    /**
     * <p>
     * This method returns a Hashtable of endpoint wrapper instances associated
     * with the message delivery id.
     * </p>
     *
     * <p>
     * Users can get the message endpoint test result from the endpoints for
     * test verifications.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. This message delivery
     *            ID is used to retrieve the endpoint wrapper instances.
     *
     * @return a Hashtable of endpoint instances used by this work intance. The
     *         hashkey to get the work instance is endpointName + instanceID.
     */
    public Hashtable getEndpoints(String deliveryId) {
        return workDispatcher.getEndpoints(deliveryId);

    }

    /**
     * <p>
     * This method returns an array of test results asscoicated with a
     * particular endpoint instance (identified by endpoint name and instance
     * ID) in a specific delivery.
     *
     * @param deliveryId
     *            the ID related to this message delivery. This message delivery
     *            ID is used to retrieve the endpoint wrapper instances.
     *
     * @return an array of test results associated with a particular endpoint
     *         instance.
     */
    @Override
    public MessageEndpointTestResults[] getTestResults(String deliveryId,
                                                       String endpointName, int instanceId) {
        return workDispatcher.getTestResults(deliveryId, endpointName,
                                             instanceId);
    }

    /**
     * <p>
     * This method returns the test result asscociated with the message delviery
     * id.
     * </p>
     *
     * <p>
     * This method is only useful when there is only one test result added in
     * this message delivery. If there is more than one test results added in
     * the delivery, there is no guarantee which test result will be returned.
     * Method callers should be aware of this.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. This message delivery
     *            ID is used to retrieve the endpoint wrapper instances.
     *
     * @return a message endpoint test result object.
     */
    @Override
    public MessageEndpointTestResults getTestResult(String deliveryId) {
        return workDispatcher.getTestResult(deliveryId);
    }

    // d174256: Added for Work that doesn't send through sendMessagexxx
    // can still get the test results.

    /**
     * <p>
     * This method returns an array of test results asscoicated with a
     * particular endpoint instance (identified by endpoint name and instance
     * ID) in a specific delivery.
     *
     * @param inputWork
     *            this work instance is used to retrieve the endpoint wrapper
     *            instances if the Work is submitted by user instead of by
     *            sendMessagexxx
     *
     * @return an array of test results associated with a particular endpoint
     *         instance.
     */
    @Override
    public MessageEndpointTestResults[] getTestResults(Work inputWork,
                                                       String endpointName, int instanceId) {
        return workDispatcher.getTestResults(inputWork, endpointName,
                                             instanceId);
    }

    // d174256: Added for Work that doesn't send through sendMessagexxx
    // can still get the test results.

    /**
     * <p>
     * This method returns the test result asscociated with the message delviery
     * id.
     * </p>
     *
     * <p>
     * This method is only useful when there is only one test result added in
     * this message delivery. If there is more than one test results added in
     * the delivery, there is no guarantee which test result will be returned.
     * Method callers should be aware of this.
     * </p>
     *
     * @param inputWork
     *            this work instance is used to retrieve the endpoint wrapper
     *            instances if the Work is submitted by user instead of by
     *            sendMessagexxx
     *
     * @return a message endpoint test result object.
     */
    @Override
    public MessageEndpointTestResults getTestResult(Work inputWork) {
        return workDispatcher.getTestResult(inputWork);
    }

    /**
     * <p>
     * This method hints the testing resource adapter to release all the
     * information related with this message delivery. TRA will remove the
     * message delivery ID from the hashmap. After calling this method, users
     * cannot get any endpoint information from this message delivery ID any
     * more.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery.
     */
    @Override
    public void releaseDeliveryId(String deliveryId) {
        workDispatcher.releaseDeliveryId(deliveryId);
    }

    // d174256: Added for Work that doesn't send through sendMessagexxx
    // can still get the endpoints to cleanup the test results.

    /**
     * <p>
     * This method hints the testing resource adapter to release all the
     * information related to this Work instance. TRA will unset the test
     * results of the endpoint instance. After calling this method, users cannot
     * get any endpoint information from this Work instance any more.
     * </p>
     *
     * @param inputWork
     *            this work instance is used to retrieve the endpoint wrapper
     *            instances if the Work is submitted by user instead of by
     *            sendMessagexxx. It should be created through
     *            FVTMessageProvider.createWork().
     */
    // d174592
    @Override
    public void releaseWork(Work inputWork) {
        workDispatcher.releaseWork((FVTGeneralWorkImpl) inputWork);
    }

    /**
     * <p>
     * This method is called by the application server during crash recovery.
     * This method takes in an array of ActivationSpec JavaBeans and returns an
     * array of XAResource objects each of which represents a unique resource
     * manager. The resource adapter may return null if it does not implement
     * the XAResource interface. Otherwise, it must return an array of
     * XAResource objects, each of which represents a unique resource manager
     * that was used by the endpoint applications. The application server uses
     * the XAResource objects to query each resource manager for a list of
     * in-doubt transactions. It then completes each pending transaction by
     * sending the commit decision to the participating resource managers.
     * </p>
     *
     * <p>
     * This method will not check the ActivationSpec array to return the
     * corresponding XAResource array. Instead, it just returns an array with
     * one XAResource which includes all the Xids in the transaction log. This
     * is not a right way to implement this method, but considering most of the
     * methods of XAResource implementation class are no-ops, it should be
     * enough for FVT purpose. However, this is definitely a place for
     * improvement.
     * </p>
     *
     * @param specs
     *            an array of ActivationSpec JavaBeans each of which corresponds
     *            to an deployed endpoint application that was active prior to
     *            the system crash.
     *
     * @return an array of XAResource objects each of which represents a unique
     *         resource manager.
     *
     * @exception ResourceException
     *                generic exception if operation fails due to an error
     *                condition.
     *
     *
     */
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getXAResources", this);

        // @alvinso.2
        /*
         * java.util.Properties props = new java.util.Properties();
         *
         * try { java.io.InputStream is =
         * Thread.currentThread().getContextClassLoader
         * ().getResourceAsStream(tranLog);
         *
         * if (is != null) props.load(is); } catch (Throwable t) { if(
         * tc.isDebugEnabled()) Tr.debug(tc, t.getMessage()); throw new
         * ResourceException("cannot get xa resource: " + t); }
         *
         * FVTXAResourceImpl xaResource = new FVTXAResourceImpl(props.keys());
         *
         * if (tc.isEntryEnabled()) Tr.entry(tc, "getXAResources", xaResource);
         *
         * return new FVTXAResourceImpl[] { xaResource };
         */

        return null;
    }

    /**
     * <p>
     * Get the XATerminator from the application server to manage the imported
     * transaction.
     * </p>
     *
     * @return the XATerminator object
     */
    @Override
    public XATerminator getXATerminator() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getXATerminator", this);

        XATerminator xaterm = adapter.getXaTerm();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getXATerminator", xaterm);

        return xaterm;
    }

    /**
     * <p>
     * Provides a handle to a WorkManager instance. The WorkManager instance
     * could be used by the test case to do its work by submitting Work
     * instances for execution.
     * </p>
     *
     * @return a WorkManager instance
     */
    @Override
    public WorkManager getWorkManager() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getWorkManager", this);

        WorkManager wm = adapter.getWorkManager();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getWorkManager", wm);

        return wm;
    }

    /**
     * <p>
     * Provides a handle to the BootstrapContext.
     * </p>
     *
     * @return BootstrapContext
     */
    @Override
    public BootstrapContext getBootstrapContext() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getBootstrapContext", this);

        BootstrapContext bc = adapter.getBootstrapContext();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getBootstrapContext", bc);

        return bc;
    }

    /**
     * <p>
     * Creates a new java.util.Timer instance. The Timer instance could be used
     * to perform periodic Work executions or other tasks.
     * </p>
     *
     * @return a new Timer instance
     *
     * @exception UnavailableException
     *                indicates that a Timer instance is not available. The
     *                request may be retried later.
     */
    @Override
    public Timer createTimer() throws javax.resource.spi.UnavailableException {
        return getBootstrapContext().createTimer();
    }

    /**
     * <p>
     * Create a work instance based on the passed-in FVTMessage object.
     * </p>
     *
     * <p>
     * This method is mainly used for work manager testing. FVT test case can
     * call this method to get a work instance and submit this work instance to
     * the work manager.
     * </p>
     *
     * @param message
     *            an FVTMessage object
     *
     * @return a work instance
     */
    @Override
    public Work createWork(FVTMessage message) {
        return new FVTComplexWorkImpl(null, message, workDispatcher);
    }

    @Override
    public void setResourceAdapter(FVTAdapterImpl ra) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setResourceAdapter", new Object[] { this, ra });
        adapter = ra;
        ra.setProvider(this);

        // Associate one workDisaptcher with one Message provider instance
        if (workDispatcher == null) {
            workDispatcher = new FVTWorkDispatcher(adapter, adapter.getWorkManager(), adapter.getXaTerm());
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setResourceAdapter", new Object[] { adapter,
                                                             workDispatcher });
    }

    /**
     * <p>
     * Set the association between the resource adapter and the message
     * provider.
     * </p>
     *
     * <p>
     * The FVTMessageProviderImpl is an administered object, which has no
     * association with the resource adapter instance. We have to use an
     * external contract to associate the FVTMessageProviderImpl instance with
     * the testing resource adpater instance.
     * </p>
     *
     * <p>
     * This method asssociates this instance with a testing resource adapter
     * instance. It calls a helper class to get the ConnectionFactory object (In
     * the testing resource adapter case, the connection factory has type as
     * javax.sql.DataSource), and from this ConnectionFactory object, the
     * resource adpater instance is retrieved. After that, this resource adapter
     * instance is set to this message provider instance.
     * </p>
     *
     * <p>
     * It is required that applications call this setResourceAdapter method
     * after looking up the administered object to establish the association
     * between the administered object and the resource adapter instance.
     * <p>
     *
     * @param cfJndiName
     *            the connection factory JNDI name.
     */
    @Override
    public void setResourceAdapter(String cfJNDIName) throws ResourceAdapterInternalException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setResourceAdapter",
                     new Object[] { this, cfJNDIName });

        DataSource ds = null;

        // Get the datasource instance
        try {
            ds = FVTAdapterHelper.getConnectionFactory(cfJNDIName);
        } catch (ClassNotFoundException cnfe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, cnfe.getMessage());
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setResourceAdapter", "exception");
            throw new ResourceAdapterInternalException(cnfe);
        } catch (NameNotFoundException nnfe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, nnfe.getMessage());
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setResourceAdapter", "exception");
            throw new ResourceAdapterInternalException(nnfe);
        } catch (NamingException ne) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, ne.getMessage());
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setResourceAdapter", "exception");
            throw new ResourceAdapterInternalException(ne);
        }

        // Get the test resource adapter instance from the connection factory
        // instance.
        try {
            adapter = (FVTAdapterImpl) FVTAdapterHelper.getResourceAdapter(ds);
        } catch (ClassCastException cce) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setResourceAdapter", "exception");
            throw new ResourceAdapterInternalException(cce);
        }

        // 10/20/03: 
        // Add setProvider to here associate the provider to the adapter
        // instance.
        adapter.setProvider(this);

        // Associate one workDisaptcher with one Message provider instance.
        // 11/17/03: 
        // Reuse the workDispatcher
        if (workDispatcher == null) {
            workDispatcher = new FVTWorkDispatcher(adapter, adapter.getWorkManager(), adapter.getXaTerm());
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setResourceAdapter", new Object[] { adapter,
                                                             workDispatcher });

    }

    /**
     * Returns the transaction log file path name.
     *
     * @return the transaction log file path name
     */
    public static String getTranLog() {

        // 12/16/03: 
        if (tranLog == null) {
            wasInstallRoot = System.getProperty("was.install.root");
            pathSep = System.getProperty("file.separator");
            setTranLog(wasInstallRoot + pathSep + "tranlog");
        }
        return tranLog;
    }

    /**
     * Sets the transaction log file path name.
     *
     * @param tranLog
     *            the transaction log file path name
     */
    public static void setTranLog(String tranLog) {
        FVTMessageProviderImpl.tranLog = tranLog;
    }

    /**
     * Set the resource adapter instance. This method is used for unit test
     * purpose.
     */
    public void setAdapter(FVTAdapterImpl adapter) {
        this.adapter = adapter;
        workDispatcher = new FVTWorkDispatcher(adapter, adapter.getWorkManager(), adapter.getXaTerm());
    }

    /**
     * Sets the workDispatcher.
     *
     * @param workDispatcher
     *            The workDispatcher to set
     */
    public void setWorkDispatcher(FVTWorkDispatcher workDispatcher) {
        this.workDispatcher = workDispatcher;
    }

    /**
     * <p>
     * Getters of WorkDispatcher object instance.
     * </p>
     */
    @Override
    public FVTWorkDispatcher getWorkDispatcher() {
        if (tc.isEntryEnabled())
            Tr.debug(tc, "getWorkDispatcher", workDispatcher);
        return workDispatcher;
    }

    // d177221 begin: 
    /**
     * <p>
     * Indicate to the TRA that the MessageProvider (underlying EIS) is failing.
     *
     * <p>
     * This method set the instance variable, EISStatus to STATUS_FAIL. TRA
     * checks this value to find out if the EIS is failing or not. This simulate
     * the EIS is failed.
     *
     * @param boolean if true means signal EIS to fail state.
     */
    @Override
    public void signalEISFailure() throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.debug(tc, "signalEISFailure");

        EISTimer timer = getWorkDispatcher().getEISTimer();

        // 12/01/03: 
        // Need to let the EISTimer know about the sync object
        timer.setSyncObject(syncEISTimerObj);

        // We required the user to set the EIS Status back to OK by himself
        // because the user
        // may still want to do something else after active transactions are
        // rollback.
        // This provide a flexibility for writing test cases.
        EISStatus = STATUS_FAIL;

        // Wait until all active trans is rollback before return
        try {
            synchronized (syncEISTimerObj) {
                if (tc.isEntryEnabled())
                    Tr.debug(tc, "signalEISFailure",
                             "Wait for all active trans rollback.");

                // Since it is still in normal state, rollback is not started
                // yet.
                // Return only after all transactions are rollbacked by the
                // EISTimer.
                syncEISTimerObj.wait();
            }
        } catch (InterruptedException e) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "***** InterruptedException is caught in signalEISFailure *****",
                         e);
            throw new ResourceException(e.getMessage());
        }

        // Now, the rollback is either completed or failed
        if (timer.isRollbackFailed()) {
            if (tc.isEntryEnabled())
                Tr.debug(tc, "signalEISFailure",
                         "Rollback Failed. Need to reset Timer to normal state.");

            // 12/01/03: 
            // Since there is error in rollback active trans, in order to reset
            // the Timer to normal
            // state for the next test case, need to call signalEISRecover
            // before throwing the exp.
            signalEISRecover();

            throw new ResourceException("Rollback active transactions failed.");
        }
    }

    /**
     * <p>
     * Indicate to the TRA that the MessageProvider (underlying EIS) is
     * recovered.
     *
     * <p>
     * This method set the instance variable, EISStatus to STATUS_OK. TRA checks
     * this value to find out if the EIS is recovered or not. This simulate the
     * EIS is recovered.
     *
     */
    @Override
    public void signalEISRecover() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.debug(tc, "signalEISRecover");
        EISStatus = STATUS_OK;

        // 12/01/03: 
        // Need to wait until the EISTimer reset back to normal state.
        try {
            synchronized (syncEISTimerObj) {
                if (tc.isEntryEnabled())
                    Tr.debug(tc, "signalEISRecover",
                             "Wait for EISTimer back to normal state.");
                syncEISTimerObj.wait();
            }
        } catch (InterruptedException ie) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "InterruptedException is caught in deliverComplexMessage",
                         ie);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deliverComplexMessage", "Exception");
            throw new ResourceException("InterupttedException is caught in signalEISRecover", ie);
        }
    }

    /**
     * <p>
     * Get the status of the EIS (messageProvider). Running or Failure
     *
     */
    public int getEISStatus() {
        return EISStatus;
    }

    // d177221 end: 

    /**
     * <p>
     * Deliver a nested work message represented by an FVTMessage object using
     * doWork, scheduleWork, or startWork. If the doWorkType is DO_WORK, the
     * method blocks until the message delivery is completed. If the doWorkType
     * is SCHEDULE_WORK or START_WORK, the method blocks until the work instance
     * has reached the specified state.
     * </p>
     *
     * <p>
     * This method is called by sendMessageWaitNestedWork method in the
     * FVTMessageProviderImpl class.
     * </p>
     *
     * <p>
     * The work instace can be in the following four states:
     * </p>
     * <ul>
     * <li>WORK_ACCEPTED</li>
     * <li>WORK_REJECTED</li>
     * <li>WORK_STARTED</li>
     * <li>WORK_COMPLETED</li>
     * </ul>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            delivery. If you don't provide deliveryId, you won't be able
     *            to retreive these information.
     * @param message
     *            an FVTMessage object
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum time for waiting the work
     *            to reach the specified state. Once the waitTime is reached,
     *            the method will return even if the work instance hasn't reach
     *            the specified state yet.
     * @param doWorkType
     *            the work type of the parent nested work. It can be doWork,
     *            scheduleWork, or startWork
     * @param nestedDoWorkType
     *            the work type of the child nested work. It can be doWork,
     *            scheduleWork, or startWork
     * @param xid
     *            the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    @Override
    public void sendMessageWaitNestedWork(String deliveryId,
                                          FVTMessage message, int state, Xid xid, int doWorkType,
                                          int nestedDoWorkType) throws ResourceException {
        workDispatcher.deliverNestedWorkMessage(deliveryId, message, state,
                                                DEFAULT_WAIT_TIME, doWorkType, nestedDoWorkType, xid, null);

    }

    /**
     * <p>
     * Deliver a nested work message represented by an FVTMessage object using
     * doWork, scheduleWork, or startWork. If the doWorkType is DO_WORK, the
     * method blocks until the message delivery is completed. If the doWorkType
     * is SCHEDULE_WORK or START_WORK, the method blocks until the work instance
     * has reached the specified state.
     * </p>
     *
     * <p>
     * This method is called by sendMessageWaitNestedWork method in the
     * FVTMessageProviderImpl class.
     * </p>
     *
     * <p>
     * The work instace can be in the following four states:
     * </p>
     * <ul>
     * <li>WORK_ACCEPTED</li>
     * <li>WORK_REJECTED</li>
     * <li>WORK_STARTED</li>
     * <li>WORK_COMPLETED</li>
     * </ul>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            delivery. If you don't provide deliveryId, you won't be able
     *            to retreive these information.
     * @param message
     *            an FVTMessage object
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum time for waiting the work
     *            to reach the specified state. Once the waitTime is reached,
     *            the method will return even if the work instance hasn't reach
     *            the specified state yet.
     * @param doWorkType
     *            the work type of the parent nested work. It can be doWork,
     *            scheduleWork, or startWork
     * @param nestedDoWorkType
     *            the work type of the child nested work. It can be doWork,
     *            scheduleWork, or startWork
     * @param xid
     *            the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    @Override
    public void sendMessageWaitNestedWork(String deliveryId,
                                          FVTMessage message, int state, Xid parentXid, Xid childXid,
                                          int doWorkType, int nestedDoWorkType) throws ResourceException {
        workDispatcher.deliverNestedWorkMessage(deliveryId, message, state,
                                                DEFAULT_WAIT_TIME, doWorkType, nestedDoWorkType, parentXid,
                                                childXid);

    }

    /**
     * <p>
     * Deliver a nested JCA 1.6 work message represented by an FVTMessage object
     * using doWork, scheduleWork, or startWork. If the doWorkType is DO_WORK,
     * the method blocks until the message delivery is completed. If the
     * doWorkType is SCHEDULE_WORK or START_WORK, the method blocks until the
     * work instance has reached the specified state.
     * </p>
     *
     * <p>
     * The work instace can be in the following four states:
     * </p>
     * <ul>
     * <li>WORK_ACCEPTED</li>
     * <li>WORK_REJECTED</li>
     * <li>WORK_STARTED</li>
     * <li>WORK_COMPLETED</li>
     * </ul>
     *
     * @param deliveryId
     *            the ID related to this message delivery. You can use this
     *            message delivery ID to retrieve delivery-related information,
     *            such as endpoint instances and the work object used for this
     *            delivery. If you don't provide deliveryId, you won't be able
     *            to retreive these information.
     * @param message
     *            an FVTMessage object
     * @parem wi the WorkInformation object that is used to configure the work
     *        instance
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param parentXid
     *            the XID which represents the imported transaction for the
     *            parent.
     * @param childXid
     *            the XID which represents the imported transaction for the
     *            child.
     * @param doWorkType
     *            the work type of the parent nested work. It can be doWork,
     *            scheduleWork, or startWork
     * @param nestedDoWorkType
     *            the work type of the child nested work. It can be doWork,
     *            scheduleWork, or startWork
     *
     * @throws ResourceException
     */
    @Override
    public void sendJCA16MessageWaitNestedWork(String deliveryId,
                                               FVTMessage message, WorkInformation wi, int state, Xid parentXid,
                                               Xid childXid, int doWorkType, int nestedDoWorkType) throws ResourceException {
        workDispatcher.deliverNestedJCA16WorkMessage(deliveryId, message, wi,
                                                     state, DEFAULT_WAIT_TIME, doWorkType, nestedDoWorkType,
                                                     parentXid, childXid);
    }

    // @alvinso.1
    /**
     * @return
     */
    @Override
    public String getProperty_a() {
        return property_a;
    }

    /**
     * @return
     */
    @Override
    public String getProperty_m() {
        return property_m;
    }

    /**
     * @param string
     */
    @Override
    public void setProperty_a(String string) {
        this.property_a = string;
    }

    /**
     * @param string
     */
    @Override
    public void setProperty_m(String string) {
        this.property_m = string;
    }

    // MHD: Begin failover code
    public void sendMessageWithoutWorkProcessing(String endpointName,
                                                 XAResource xaResource, String rowID) throws Exception {
        // get the MessageEndpointFactoryWrapper from the RA
        System.out.println("Inside .sendMessageWithoutWorkProcessing()...");
        HashMap factories = adapter.getMessageFactories();
        MessageEndpointFactoryWrapper factoryWrapper = (MessageEndpointFactoryWrapper) factories
                        .get(endpointName);

        // create a MessageEndpoint wrapper...which holds the real
        // MessageEndpoint
        System.out.println("Getting MessageEndpoint wrapper from factory...");
        MessageEndpointWrapper messageEndpointWrapper = (MessageEndpointWrapper) factoryWrapper
                        .createEndpoint(xaResource);

        // Get the real MessageEndpoint from the Server...since the RA's
        // deployment descriptor called out the MessageListener interface, we
        // assume that the Server
        // has implemented their MessageEndpoint object to implement the
        // MessageListener interface (if they haven't done this, then we are
        // broken)....we need the
        // .onMessage() method, which we can only get at if we cast it into a
        // MessageListener...so, since the Server's MessageEndpoint IS a
        // MessageListener, we
        // cast it into a MessageListener and thereby get access to the
        // .onMessage() method
        System.out
                        .println("Getting actual MessageEndpoint and casting into MessageListener...");
        MessageListener listener = (MessageListener) messageEndpointWrapper
                        .getEndpoint();

        // create the TextMessage using the hashCode from the RA instance we are
        // using
        String hashCode = Integer.toString(adapter.hashCode());
        String fullMessage = rowID + ":" + hashCode;
        System.out.println("Creating New TextMessage of **" + fullMessage
                           + "**");
        TextMessageImpl textMessage = new TextMessageImpl(fullMessage);

        // actually send in the message
        System.out.println("Actually sending message...");
        listener.onMessage(textMessage);

        // At this point, the message should go to the EJB, and we should have
        // successful inbound communication
        System.out.println("Done sending message...");
    }// end

    @Override
    public ResourceAdapter getAdapter() {
        return adapter;
    }
}
