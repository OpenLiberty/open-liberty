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

package com.ibm.ws.ejbcontainer.fat.rar.message;

import java.util.Timer;

import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ws.ejbcontainer.fat.rar.work.FVTWorkImpl;

/**
 * <p>This interface defines the methods to send messages to the endpoint applications
 * via testing resource adapter. The work manager from the application server will be
 * involved in sending the messages. An implementation of this interface serves as a
 * message provider. </p>
 *
 * <p>The target user of this interface is J2C FVT.</p>
 */
public interface FVTMessageProvider extends FVTBaseMessageProvider {
    static int DO_WORK = 1;
    static int START_WORK = DO_WORK + 1;
    static int SCHEDULE_WORK = DO_WORK + 2;
    static int NO_WORK = DO_WORK + 3;

    /**
     * <p>This method sends a message to a particular endpoint application via work
     * manager and waits until the message is delivered. </p>
     *
     * <p>This method call blocks until the message delivery is completed.</p>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retreive the information.
     * @param beanName the message endpoint name
     * @param message the message going to be delivered to the message endpoint
     * @param xaResource the XAResource object used for getting transaction notifications. Set
     *            to null if you don't want to get transaction notifications.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     *
     * @exception ResourceException
     */
    void sendMessage(String deliveryId, String beanName, String message, XAResource xaResource, Xid xid) throws ResourceException;

    /**
     * <p>This method sends one or more messages to one or more endpoint applications
     * via work manager and waits until all the message are delivered. </p>
     *
     * <p>This call blocks until all the message deliveries are completed.</p>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and work object. If you don't provide deliveryId, you won't be able to retrieve
     *            the information.
     * @param message the FVT message object.
     * @param xid the XID which represents the imported transaction. Set this parameter to
     *            null if no imported transaction exists.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendMessage(String deliveryId, FVTMessage message, Xid xid) throws ResourceException;

    /**
     * <p>This method asynchronously sends a message to a particular endpoint
     * application via work manager. </p>
     *
     * <p>This method call won't block the execution. It returns once the message
     * is accepted by the application server to deliver. </p>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retrieve the information.
     * @param beanName the message endpoint name
     * @param message the message going to be delivered to the message endpoint
     * @param xaResource the XAResource object used for getting transaction notifications. Set
     *            to null if you don't want to get transaction notifications.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     *
     * @exception ResourceException
     */
    void sendMessageNoWait(String deliveryId, String beanName, String message, XAResource xaResource, Xid xid) throws ResourceException;

    /**
     * <p>This method asynchronously sends one or more messages to one or more endpoint
     * applications via work manager.</p>
     *
     * <p>This method call won't block the execution. It returns once the message is
     * accepted by the application server to deliver. </p>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and work object. If you don't provide deliveryId, you won't be able to retrieve
     *            the information.
     * @param message the FVT message object.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendMessageNoWait(String deliveryId, FVTMessage message, Xid xid) throws ResourceException;

    /**
     * <p>This method sends a message to a particular endpoint application via work
     * manager and returns once a certain state is reached.</p>
     *
     * <p> This method call will block the execution until the work, which
     * delivers the message, reaches certain state.</p>
     *
     * <p>There are four possible states:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL></P>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retrieve the information.
     * @param beanName the message endpoint name
     * @param message the message going to be delivered to the message endpoint
     * @param state the state which the work instance has reached when the method call returns.
     * @param xaResource the XAResource object used for getting transaction notifications. Set
     *            to null if you don't want to get transaction notifications.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     *
     * @exception ResourceException
     */
    void sendMessageWait(String deliveryId, String beanName, String message, int state, XAResource xaResource, Xid xid) throws ResourceException;

    /**
     * <p>This method sends one or more messages to one or more endpoint applications
     * via work manager and returns once a certain satus is reached. </p>
     *
     * <p> This method call will block the execution until the work, which
     * delivers the message, reaches certain state.</p>
     *
     * <p>There are four possible states:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL></P>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retrieve the information.
     * @param message the FVT message object.
     * @param state the state which the work instance has reached when the method call returns.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendMessageWait(String deliveryId, FVTMessage message, int state, Xid xid) throws ResourceException;

    /**
     * <p>This method sends a message to a particular endpoint application and returns once a
     * certain state is reached.</p>
     *
     * <p>This method call will block the execution until the work, which
     * delivers the message, reaches certain state.</p>
     *
     * <p>The maximum waiting time is waitTime millisecond. Once the waitTime is reached, the
     * method will return even if the work instance hasn't reach the specified state yet</p>
     *
     * <p>There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL></P>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retrieve these information.
     * @param beanName the message endpoint name
     * @param message the message going to be delivered to the message endpoint
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum waiting time. Once the waitTime
     *            is reached, the method will return even if the work instance hasn't reach the specified
     *            state yet</p>
     * @param xaResource the XAResource object used for getting transaction notifications. Set
     *            to null if you don't want to get transaction notifications.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     *
     * @exception ResourceException
     */
    void sendMessageWait(String deliveryId, String beanName, String message, int state, int waitTime, XAResource xaResource, Xid xid) throws ResourceException;

    /**
     * <p>This method sends one or more messages to one or more endpoint applications and
     * returns once a certain status is reached.
     *
     * <p> This method call will block the execution until the work, which
     * delivers the message, reaches certain state.</p>
     *
     * <p>The maximum waiting time is waitTime millisecond. Once the waitTime is reached, the
     * method will return even if the work instance hasn't reach the specified state yet</p>
     *
     * <p>There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL></P>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retrieve these information.
     * @param message the FVT message object.
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum waiting time. Once the waitTime
     *            is reached, the method will return even if the work instance hasn't reach the specified
     *            state yet</p>
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendMessageWait(String deliveryId, FVTMessage message, int state, int waitTime, Xid xid) throws ResourceException;

    /**
     * <p>This method sends one or more messages to one or more endpoint applications and
     * returns once a certain status is reached. You can specify how to submit the work
     * to the work manager.</P>
     *
     * <p> This method call will block the execution until the work, which
     * delivers the message, reaches certain state.</p>
     *
     * <p>The maximum waiting time is waitTime millisecond. Once the waitTime is reached, the
     * method will return even if the work instance hasn't reach the specified state yet</p>
     *
     * <p>There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL></P>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retrieve these information.
     * @param message the FVT message object.
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum waiting time. Once the waitTime
     *            is reached, the method will return even if the work instance hasn't reach the specified
     *            state yet</p>
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     * @param doWorkType how to submit the work the work manager. It could be any of the
     *            following: DO_WORK, START_WORK or SCHEDULE_WORK
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendMessageWait(String deliveryId, FVTMessage message, int state, int waitTime, Xid xid, int doWorkType) throws ResourceException;

    /**
     * <p>This method sends one or more messages to one or more endpoint applications and
     * returns once a certain status is reached. You can specify how to submit the work
     * to the work manager.</P>
     *
     * <p> This method call will block the execution until the work, which
     * delivers the message, reaches certain state.</p>
     *
     * <p>The maximum waiting time is waitTime millisecond. Once the waitTime is reached, the
     * method will return even if the work instance hasn't reach the specified state yet</p>
     *
     * <p>There are four possible state:<br>
     * <UL>
     * <LI>WORK_ACCEPTED</LI>
     * <LI>WORK_REJECTED</LI>
     * <LI>WORK_STARTED</LI>
     * <LI>Work_COMPLETED</LI>
     * </UL></P>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this message delivery. If you don't provide deliveryId,
     *            you won't be able to retrieve these information.
     * @param message the FVT message object.
     * @param state the state which the work instance has reached when the method call returns.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     * @param doWorkType how to submit the work the work manager. It could be any of the
     *            following: DO_WORK, START_WORK or SCHEDULE_WORK
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendMessageWait(String deliveryId, FVTMessage message, int state, Xid xid, int doWorkType) throws ResourceException;

    /**
     * <p>This method call executes two or more works concurrently. This method is mainly
     * used to test associating two or more works concurrently within a source managed
     * transaction (EIS imported transaction). The caller will receive a WorkCompletedException
     * set to the error code WorkException.TX_CONCURRENT_WORK_DISALLOWED</p>
     *
     * <p>This method cannot be called concurrently in several threads. That is, you can
     * not initiate two threads, and have both of them call this method. This usage will lead to
     * unexpected behavior. </p>
     *
     * <p>This method only guarantees executing these works concurrently. It doesn't guarantee
     * delivering messages to endpoints concurrently.</p>
     *
     * @param works an array of Work instance. Currently, we only support executing the
     *            work instances which type or super type is FVTConcurrentWorkImpl.
     * @param xid the XID which represents the imported transaction. If there is no
     *            import transaction, set xid to null.
     * @param doWorkType how to submit the work the work manager. It could be any of the
     *            following: DO_WORK, START_WORK or SCHEDULE_WORK
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum time for waiting the work to
     *            reach the specified state. Once the waitTime is reached, the method will return
     *            even if the work instance hasn't reach the specified state yet.
     *
     * @exception WorkException Any WorkException. A WorkCompletedException set to the error code
     *                WorkException.TX_CONCURRENT_WORK_DISALLOWED is expected.
     * @exception ResourceException
     */
    void executeConcurrentWork(Work[] works, Xid xid, int doWorkType, int state) throws WorkException, ResourceException;

    /**
     * <p>This method call executes two or more works concurrently. This method is mainly
     * used to test associating two or more works concurrently within different source managed
     * transaction (EIS imported transaction). If these Xids are different, the call should
     * be executed successfully.</p>
     *
     * <p>This method cannot be called concurrently in several threads. That is, you can
     * not initiate two threads, and have both of them call this method. This usage will lead to
     * unexpected behavior. </p>
     *
     * <p>This method only guarantees executing these works concurrently. It doesn't guarantee
     * delivering messages to endpoints concurrently.</p>
     *
     * @param works an array of Work instance. Currently, we only support executing the
     *            work instances which type or super type is FVTConcurrentWorkImpl.
     * @param xid an array of XIDs which represent different imported transaction.
     * @param doWorkType how to submit the work the work manager. It could be any of the
     *            following: DO_WORK, START_WORK or SCHEDULE_WORK
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum time for waiting the work to
     *            reach the specified state. Once the waitTime is reached, the method will return
     *            even if the work instance hasn't reach the specified state yet.
     *
     * @exception WorkException Any WorkException. A WorkCompletedException set to the error code
     *                WorkException.TX_CONCURRENT_WORK_DISALLOWED is expected.
     * @exception ResourceException
     */
    void executeConcurrentWork(Work[] works, Xid[] xids, int doWorkType, int state) throws WorkException, ResourceException;

    /**
     * <p>This method returns the work instance associated with the message delivery id.</p>
     *
     * <p>After getting the work instance, the test application can call methods
     * isWorkAccepted()/isWorkStarted()/isWorkCompleted/isWorkRejected of the work
     * instance to test whether the work has been accepted/started/completed/rejected .</p>
     *
     * @param deliveryId the ID related to this message delivery. This message delivery ID
     *            is used to retrieve the work instance.
     *
     * @return an FVT work object.
     */
    public FVTWorkImpl getWork(String deliveryId);

    /**
     * <p>Get the XATerminator from the application server to manage the imported
     * transaction.</p>
     *
     * @return the XATerminator object
     */
    XATerminator getXATerminator();

    /**
     * <p>Provides a handle to a WorkManager instance. The WorkManager instance could
     * be used by the test case to do its work by submitting Work instances for
     * execution. </p>
     *
     * @return a WorkManager instance
     */
    WorkManager getWorkManager();

    /**
     * <p>Creates a new java.util.Timer instance. The Timer instance could be used
     * to perform periodic Work executions or other tasks. </p>
     *
     * @return a new Timer instance.
     *
     * @exception UnavailableException indicates that a Timer instance is not
     *                available. The request may be retried later.
     */
    Timer createTimer() throws UnavailableException;

    /**
     * <p>Create a work instance based on the passed-in FVTMessage object. </p>
     *
     * <p>This method is mainly used for work manager testing. FVT test case can
     * call this method to get a work instance and submit this work instance
     * to the work manager. </p>
     *
     * @param message an FVTMessage object
     *
     * @return a work instance
     */
    Work createWork(FVTMessage message);

    // d174256: Added by swai for Work that doesn't send through sendMessagexxx
    // can still get the test results.

    /**
     * <p>This method returns an array of test results associated with a particular
     * endpoint instance (identified by endpoint name and instance ID) in a specific
     * delivery.
     *
     * @param inputWork this work instance is used to retrieve the endpoint
     *            wrapper instances if the Work is submitted by user instead of by sendMessagexxx
     *
     * @return an array of test results associated with a particular endpoint instance.
     */
    MessageEndpointTestResults[] getTestResults(Work inputWork, String endpointName, int instanceId);

    // d174256: Added by swai for Work that doesn't send through sendMessagexxx
    // can still get the test results.

    /**
     * <p>This method returns the test result associated with the message delivery id. </p>
     *
     * <p>This method is only useful when there is only one test result added in this
     * message delivery. If there is more than one test results added in the delivery,
     * there is no guarantee which test result will be returned. Method callers should
     * be aware of this.</p>
     *
     * @param inputWork this work instance is used to retrieve the endpoint
     *            wrapper instances if the Work is submitted by user instead of by sendMessagexxx
     *
     * @return a message endpoint test result object.
     */
    MessageEndpointTestResults getTestResult(Work inputWork);

    // d174256: Added by swai for Work that doesn't send through sendMessagexxx
    // can still get the endpoints to cleanup the test results.

    /**
     * <p>This method hints the testing resource adapter to release all the information
     * related to this Work instance. TRA will unset the test results of the endpoint
     * instance. After calling this method, users cannot get any endpoint
     * information from this Work instance any more. </p>
     *
     * @param inputWork this work instance is used to retrieve the endpoint
     *            wrapper instances if the Work is submitted by user instead of by sendMessagexxx.
     *            It should be created through FVTMessageProvider.createWork().
     */
    // d174592
    void releaseWork(Work inputWork);

    /**
     * <p>Deliver a nested work message represented by an FVTMessage object using doWork,
     * scheduleWork, or startWork. If the doWorkType is DO_WORK, the method blocks until
     * the message delivery is completed. If the doWorkType is SCHEDULE_WORK or START_WORK,
     * the method blocks until the work instance has reached the specified state.</p>
     *
     * <p>This method is called by sendMessageWaitNestedWork method in the FVTMessageProviderImpl class.</p>
     *
     * <p>The work instance can be in the following four states:</p>
     * <ul>
     * <li>WORK_ACCEPTED</li>
     * <li>WORK_REJECTED</li>
     * <li>WORK_STARTED</li>
     * <li>WORK_COMPLETED</li>
     * </ul>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this delivery. If you don't provide deliveryId, you
     *            won't be able to retrieve these information.
     * @param message an FVTMessage object
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum time for waiting the work to
     *            reach the specified state. Once the waitTime is reached, the method will return
     *            even if the work instance hasn't reach the specified state yet.
     * @param doWorkType the work type of the parent nested work.
     *            It can be doWork, scheduleWork, or startWork
     * @param nestedDoWorkType the work type of the child nested work.
     *            It can be doWork, scheduleWork, or startWork
     * @param xid the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    public void sendMessageWaitNestedWork(String deliveryId, FVTMessage message, int state, Xid xid, int doWorkType, int nestedDoWorkType) throws ResourceException;

    /**
     * <p>Deliver a nested work message represented by an FVTMessage object using doWork,
     * scheduleWork, or startWork. If the doWorkType is DO_WORK, the method blocks until
     * the message delivery is completed. If the doWorkType is SCHEDULE_WORK or START_WORK,
     * the method blocks until the work instance has reached the specified state.</p>
     *
     * <p>This method is called by sendMessageWaitNestedWork method in the FVTMessageProviderImpl class.</p>
     *
     * <p>The work instace can be in the following four states:</p>
     * <ul>
     * <li>WORK_ACCEPTED</li>
     * <li>WORK_REJECTED</li>
     * <li>WORK_STARTED</li>
     * <li>WORK_COMPLETED</li>
     * </ul>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances
     *            and the work object used for this delivery. If you don't provide deliveryId, you
     *            won't be able to retrieve these information.
     * @param message an FVTMessage object
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum time for waiting the work to
     *            reach the specified state. Once the waitTime is reached, the method will return
     *            even if the work instance hasn't reach the specified state yet.
     * @param doWorkType the work type of the parent nested work.
     *            It can be doWork, scheduleWork, or startWork
     * @param nestedDoWorkType the work type of the child nested work.
     *            It can be doWork, scheduleWork, or startWork
     * @param xid the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    public void sendMessageWaitNestedWork(String deliveryId, FVTMessage message, int state, Xid parentXid, Xid childXid, int doWorkType,
                                          int nestedDoWorkType) throws ResourceException;

    // @alvinso.1
    String getProperty_a();

    String getProperty_m();

    void setProperty_a(String string);

    void setProperty_m(String string);

    public String getActivationName(String name);
}