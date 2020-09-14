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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.ejbcontainer.fat.rar.core.EISTimer;
import com.ibm.ws.ejbcontainer.fat.rar.core.FVTAdapterImpl;
import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.core.XATerminatorWrapper;
import com.ibm.ws.ejbcontainer.fat.rar.core.XidImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProviderImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointFactoryWrapper;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointWrapper;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageWaitTimeoutException;
import com.ibm.ws.ejbcontainer.fat.rar.message.TextMessageImpl;

/**
 * <p>This class represents a work dispatcher. A work dispatcher is responsible for
 * creating and submitting work objects.</p>
 *
 * <p>Message providers, such as FVTMessageProvider, can call doMessageDelivery()
 * or scheduleMessageDelivery() of this class to delivery messages to end point
 * applications.</p>
 *
 * <p>When doMessageDelivery() or scheduleMessageDelivery() are called,
 * an instance of FVTSimpleWorkImpl or FVTComplexWorkImpl is created and
 * submitted to the work manager of the application server for execution.</p>
 */
public class FVTWorkDispatcher implements WorkListener {
    private final static String CLASSNAME = FVTWorkDispatcher.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static CountDownLatch svMessageLatch = null;

    /** Adapter instance */
    protected FVTAdapterImpl adapter;

    /** Work manager */
    protected WorkManager workManager;

    /** XATerminator */
    protected XATerminatorWrapper xaTermWrapper;

    /** EIS Timer */
    private EISTimer eisTimer;

    /** Thread for EIS Timer */
    private Thread eisStatusThread;

    /** vector of work instance */
    protected Hashtable works;

    /** Object used for synchronization */
    // We will use a local variable for syncObj instead of instance variable.
    // In this case we can ensure the sync block is unlocked by the appropriate
    // timer or the workEvent.
    // protected Object syncObj = new Object();

    /** indicate whether the message delivery method is timed out or not */
    protected boolean timedout = false;

    /** Name for WorkCompletedException class */
    private final String completedException = "javax.resource.spi.work.WorkCompletedException";

    /** Name for WorkRejectedException class */
    private final String rejectedException = "javax.resource.spi.work.WorkRejectedException";

    // RA (WorkDispatcher) should keep a set of active trans.
    // This set serves as this set of active trans.
    /** Set of active transactions */
    private final Set SetActiveTrans = new HashSet();

    /** Set of indoubt transactions */
    private final Set SetIndoubtTrans = new HashSet();

    // Add a new instance variable to indicate if we need timeout or not.
    // If workEventReceived is set to true, that means we don't need the timeout.
    // If we don't need the timeout, we shouldn't throw the timeout exception.
    private final boolean workEventReceived = false;

    /**
     * <p>Constructor.</p>
     *
     * @param adapter the FVTAdapterImpl instance
     * @param workManager the work manager instance from the application server
     * @param xaTerm the XA terminator from the application server
     */
    public FVTWorkDispatcher(FVTAdapterImpl adapter, WorkManager workManager, XATerminator xaTerm) {
        svLogger.entering(CLASSNAME, "<init>", new Object[] { adapter, workManager, xaTerm });

        this.adapter = adapter;
        this.workManager = workManager;
        this.xaTermWrapper = (XATerminatorWrapper) xaTerm;

        // initialize works hash map
        works = new Hashtable(11);

        // create a thread for the timer object to check the status of the EIS (messageProvider)
        // Currently there is only 1 timer object as there is only 1 messageProvider.
        // Modified the constructor of the EISTimer, add WorkDispatcher
        // this avoid provider need to know the WorkDispatcher instance.
        eisTimer = new EISTimer(this, adapter.getProvider());

        // Assign this timer object to the XATermWrapper so that the wrapper
        // can reset the timer.
        // The 'xaTermWrapper' object will be null if:
        //	a) the .start() method has never been invoked on the RA object
        //	b) the .stop() method HAS been invoked on the RA object
        // In either of these cases, we'll get a NullPointerException here.  The best solution would be to check this
        // object for null-ness and then throw a clean exception stating that, but that would require changing the signature
        // on the constructor, which then force everywhere its called to deal with it. So, I'm going with the path of least
        // resistance, which is to log message indicating that its null so that when the NPE does happen, we can at least check
        // the log and no for sure what went wrong.
        if (xaTermWrapper == null) {
            svLogger.info("The xaTermWrapper object on the RA object is null.  This will cause a NullPointer in the instantiation of the FVTWorkDispatcher object.");
        }
        this.xaTermWrapper.setEISTimer(eisTimer);

        // Assign this workDispatcher object to the XATermWrapper so that the wrapper
        // can compare the list of indoubt trans from app server and from TRA
        this.xaTermWrapper.setWorkDispatcher(this);

        // Need to use doPrivleged call to create a thread
        // eisStatusThread = new Thread(eisTimer);
        eisStatusThread = (Thread) AccessController.doPrivileged(
                                                                 new PrivilegedAction() {
                                                                     @Override
                                                                     public Object run() {
                                                                         svLogger.entering(CLASSNAME, "doPrivileged on create eisTimerThread");
                                                                         return new Thread(eisTimer);
                                                                     }
                                                                 });
        if (eisStatusThread != null) {
            eisStatusThread.start();
        } else {
            svLogger.info("<init>: Returned eisStatusThread is null.");
        }

        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    public static void initMessageLatch(int messageCount) {
        svMessageLatch = new CountDownLatch(messageCount);
    }

    public static void awaitMessageLatch() throws InterruptedException {
        svMessageLatch.await(60, TimeUnit.SECONDS);
    }

    /**
     * Get the message endpoint factory from the endpoint name
     */
    public MessageEndpointFactoryWrapper getMessageFactory(String endpointName) {
        return (MessageEndpointFactoryWrapper) adapter.getMessageFactories().get(endpointName);
    }

    // -----------------------------------------------------------
    //  sendMessage methods.
    // -----------------------------------------------------------

    /**
     * <p>Deliver a complex message represented by an FVTMessage object using doWork,
     * scheduleWork, startWork, or direct call without any work instance involved. If the doWorkType
     * is DO_WORK, the method blocks until the message delivery is completed. If the
     * doWorkType is SCHEDULE_WORK or START_WORK, the method blocks until the work instance has reached
     * the specified state.</p>
     *
     * <p>This method is called by scheduleMessageDelivery or doMessageDelivery methods
     * in the FVTMessageProviderImpl class.</p>
     *
     * <p>This method will return and do nothing if the EISStatus is STATUS_FAIL.
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
     *            won't be able to retreive these information.
     * @param message an FVTMessage object
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum time for waiting the work to
     *            reach the specified state. Once the waitTime is reached, the method will return
     *            even if the work instance hasn't reach the specified state yet.
     * @param xid the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    public void deliverComplexMessage(String deliveryId,
                                      FVTMessage message,
                                      int state,
                                      int waitTime,
                                      int doWorkType,
                                      Xid xid) throws ResourceException {

        svLogger.entering(CLASSNAME, "deliverComplexMessage", new Object[] { this,
                                                                             deliveryId,
                                                                             message,
                                                                             AdapterUtil.getWorkStatusString(state),
                                                                             new Integer(waitTime),
                                                                             AdapterUtil.getWorkTypeString(doWorkType),
                                                                             xid });

        // Check the status of the messageProvider before delivering messages
        // This is not be necessary as if the test case signal EIS failure,
        // it should not send any more messages.

        if (adapter.getProvider().getEISStatus() == FVTMessageProviderImpl.STATUS_FAIL) {
            svLogger.info("deliverComplexMessage: MessageProvider fails. Should not send messages.");
            throw new ResourceException("deliverComplexMessage: MessageProvider fails. Should not send messages.");
        }

        // Initiate a work instance
        FVTComplexWorkImpl work = new FVTComplexWorkImpl(deliveryId, message, this);

        // Initiate an executionContext
        ExecutionContext ec = new ExecutionContext();

        // Need to set xid to the ec no matter it's null or not.
        ec.setXid(xid);

        if (xid != null) {
            svLogger.info("deliverComplexMessage: xid is not null.");

            // ec.setXid(xid);

            // Add the xid to the SetActiveTrans
            // Add the xid to the ActiveTransSet only if the xid is valid (format id != -1)
            if (xid.getFormatId() >= 0) {
                if (!addActiveTransToSet(xid)) {
                    svLogger.info("deliverComplexMessage: Duplicate xid!");
                    // Do not throw the Duplicate xid exception
                    // throw new ResourceException("Duplicate xid!");
                }
            }

            // If the xid is not null, then the messageProvider is sending messages
            // to the RA with imported transaction. Then reset the EISTimer.
            // Since the RA is checking the status of the messageProvider for the
            // sake of rolling back active trans and complete indoubt trans,
            // there is no need to reset the EISTimer if xid is null.
            eisTimer.resetTimeLeft();
        }

        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise, return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            svLogger.info("deliverComplexMessage: no work");

            // Don't use work manager, directly call run() method.
            try {
                work.run();
            } catch (RuntimeException t) { // d174592 - log the exception in trace
                Throwable t1 = t;
                svLogger.info("deliverComplexMessage: " + t);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverComplexMessage: ------chained exception------");
                    svLogger.info("deliverComplexMessage: " + t1.getCause());
                }

                throw t;
            }
        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            svLogger.info("deliverComplexMessage: do work");

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    svLogger.info("deliverComplexWorkMessage: " + work.getName() + " : isWorkCompleted -> true");
                } else {
                    svLogger.info("deliverComplexWorkMessage: " + work.getName() + " : isWorkCompleted -> false");
                    throw new WorkException("ComplexWork - " + work.getName() + " : is not in completed state after doWork.");
                }
            } catch (WorkException t) {
                // d174592 - log the exception in trace
                Throwable t1 = t;
                svLogger.info("deliverComplexMessage: " + t);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverComplexMessage: ------chained exception------");
                    svLogger.info("deliverComplexMessage: " + t1.getCause());
                }

                throw t;
            }
        } else if (doWorkType == FVTMessageProvider.START_WORK) {
            svLogger.info("deliverComplexMessage: start work");

            // Define the local variable for syncObject
            Object syncObj = new Object();

            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.startWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                svLogger.info("deliverComplexMessage: " + we);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverComplexMessage: ------chained exception------");
                    svLogger.info("deliverComplexMessage: " + t1.getCause());
                }
                throw we;
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        svLogger.info("deliverComplexMessage: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivileged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work, syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) AccessController.doPrivileged(
                                                                          new PrivilegedAction() {
                                                                              @Override
                                                                              public Object run() {
                                                                                  svLogger.info("deliverComplexMessage: StartWork: doPrivileged on create Thread t");
                                                                                  return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                              }
                                                                          });
                        t.start();

                        svLogger.info("deliverComplexMessage: wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                svLogger.info("InterruptedException is caught in deliverComplexMessage: " + ie);
                svLogger.exiting(CLASSNAME, "deliverComplexMessage", "InterruptedException");
                throw new ResourceException("InterruptedException is caught in deliverComplexMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                svLogger.exiting(CLASSNAME, "deliverComplexMessage", "MessageWaitTimeoutException");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // This else statements get the exception object from the Work so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        } else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            svLogger.info("deliverComplexMessage: schedule work");

            // Define the local variable for syncObject
            Object syncObj = new Object();

            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                svLogger.info("deliverComplexMessage: " + we);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverComplexMessage: ------chained exception------");
                    svLogger.info("deliverComplexMessage: " + t1.getCause());
                }

                throw we;
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        svLogger.info("deliverComplexMessage: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivileged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work, syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) AccessController.doPrivileged(
                                                                          new PrivilegedAction() {
                                                                              @Override
                                                                              public Object run() {
                                                                                  svLogger.info("deliverComplexMessage: ScheduleWork: doPrivileged on create Thread t");
                                                                                  return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                              }
                                                                          });
                        t.start();

                        svLogger.info("deliverComplexMessage: wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                svLogger.info("InterruptedException is caught in deliverComplexMessage: " + ie);
                svLogger.exiting(CLASSNAME, "deliverComplexMessage", "InterruptedException");
                throw new ResourceException("InterruptedException is caught in deliverComplexMessage", ie);
            }

            if (timedout) {
                // d173989
                timedout = false;
                svLogger.exiting(CLASSNAME, "deliverComplexMessage", "MessageWaitTimeoutException");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // This else statemetns get the exception object from the Work so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        } else {
            svLogger.exiting(CLASSNAME, "deliverComplexMessage", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :" + doWorkType);
        }

        svLogger.exiting(CLASSNAME, "deliverComplexMessage");
    }

    /**
     * <p>Deliver a simple message using doWork, scheduleWork, startWork, or direct call without work
     * instance involved. If the doWorkType is DO_WORK, the method blocks until the message
     * delivery is completed. If the doWorkType is SCHEDULE_WORK or START_WORK, the method blocks until the
     * work instance has reached the specified state.</p>
     *
     * <p>This method is called by scheduleMessageDelivery or doMessageDelivery methods
     * in the FVTMessageProviderImpl class.</p>
     *
     * <p>The work instace can be in the following four state:</p>
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
     *            won't be able to retreive these information.
     * @param endpintName the name of the endpoint application
     * @param message the message going to be sent
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum time for waiting the work to
     *            reach the specified state. Once the waitTime is reached, the method will return
     *            even if the work instance hasn't reached the specified state yet.
     * @param xaResource the XAResource object used for transaction notification.
     *            Set to null if you don't want get transaction notification.
     * @param doWorkType how to submit the work the work manager. It could be either DO_WORK
     *            or SCHEDULE_WORK
     * @param xid the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    public void deliverSimpleMessage(String deliveryId,
                                     String endpointName,
                                     String message,
                                     int state,
                                     int waitTime,
                                     XAResource xaResource,
                                     int doWorkType,
                                     Xid xid) throws ResourceException {

        svLogger.entering(CLASSNAME, "deliverSimpleMessage", new Object[] {
                                                                            this,
                                                                            deliveryId,
                                                                            endpointName,
                                                                            message,
                                                                            AdapterUtil.getWorkStatusString(state),
                                                                            new Integer(waitTime),
                                                                            xaResource,
                                                                            AdapterUtil.getWorkTypeString(doWorkType),
                                                                            xid });

        // Check the status of the messageProvider before delivering messages

        if (adapter.getProvider().getEISStatus() == FVTMessageProviderImpl.STATUS_FAIL) {
            svLogger.info("deliverSimpleMessage: MessageProvider fails. Should not send messages.");
            throw new ResourceException("deliverSimpleMessage: MessageProvider fails. Should not send messages.");
        }

        FVTXAResourceImpl resource = null;

        try {
            resource = (FVTXAResourceImpl) xaResource;
        } catch (ClassCastException cce) {
            svLogger.info("deliverSimpleMessage: " + cce);
            throw cce;
        }

        // From beanName, get the MessageEndpointFactoryWrapper instance.
        MessageEndpointFactoryWrapper factoryWrapper = getMessageFactory(endpointName);

        if (factoryWrapper == null) {
            // Cannot find the bean, throw an exception
            ResourceException re = new ResourceException("Cannot find the endpoint factory with name " + endpointName);
            svLogger.info("resource exception: " + re);
            throw re;
        }

        // Initiate a work instance
        FVTSimpleWorkImpl work = new FVTSimpleWorkImpl(deliveryId, endpointName, factoryWrapper, resource);

        work.setMessage(new TextMessageImpl(message));

        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise, return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        // Initiate a executionContext
        ExecutionContext ec = new ExecutionContext();

        // Need to set xid to the ec no matter it's null or not.
        ec.setXid(xid);

        if (xid != null) {
            //ec.setXid(xid);

            // Add the xid to the ActiveTransSet only if the xid is valid (format id != -1)
            if (xid.getFormatId() >= 0) {
                if (!addActiveTransToSet(xid)) {
                    svLogger.info("deliverSimpleMessage: Duplicate xid!");
                    // Do not throw Duplicate xid exception
                    // throw new ResourceException("Duplicate xid!");
                }
            }

            // If the xid is not null, then the messageProvider is sending messages
            // to the RA with imported transaction. Then reset the EISTimer.
            // Since the RA is checking the status of the messageProvider for the
            // sake of rolling back active trans and complete indoubt trans,
            // there is no need to reset the EISTimer if xid is null.
            eisTimer.resetTimeLeft();
        }

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            svLogger.info("deliverSimpleMessage: no work");

            // Directly call run() method.
            try {
                work.run();
            } catch (RuntimeException t) { // d174592 - log the exception in trace
                Throwable t1 = t;
                svLogger.info("deliverSimpleMessage: " + t);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverSimpleMessage: ------chained exception------");
                    svLogger.info("deliverSimpleMessage: " + t1.getCause());
                }

                throw t;
            }

        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            svLogger.info("deliverSimpleMessage: do work");

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    svLogger.info("deliverSimpleMessage: " + work.getName() + " : isWorkCompleted -> true");
                } else {
                    svLogger.info("deliverSimpleMessage: " + work.getName() + " : isWorkCompleted -> false");
                    throw new WorkException("SimpleWork - " + work.getName() + " : is not in completed state after doWork.");
                }
            } catch (WorkException t) { // d174592 - log the exception in trace
                Throwable t1 = t;
                svLogger.info("deliverSimpleMessage" + t);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverSimpleMessage: ------chained exception------");
                    svLogger.info("deliverSimpleMessage: " + t1.getCause());
                }

                throw t;
            }
        } else if (doWorkType == FVTMessageProvider.START_WORK) {
            svLogger.info("deliverSimpleMessage: start work");

            // Define the local variable for syncObject
            Object syncObj = new Object();

            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.startWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) {
                // d174592 - log the exception in trace
                Throwable t1 = we;
                svLogger.info("deliverSimpleMessage: " + we);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverSimpleMessage: ------chained exception------");
                    svLogger.info("deliverSimpleMessage: " + t1.getCause());
                }

                throw we;
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        svLogger.info("deliverSimpleMessage: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work, syncObj));
                        final int pWaitTime = waitTime;
                        final FVTSimpleWorkImpl pWork = work;
                        Thread t = (Thread) AccessController.doPrivileged(
                                                                          new PrivilegedAction() {
                                                                              @Override
                                                                              public Object run() {
                                                                                  svLogger.info("deliverSimpleMessage: StartWork: doPrivileged on create Thread t");
                                                                                  return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                              }
                                                                          });

                        t.start();

                        svLogger.info("deliverSimpleMessage: wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                svLogger.info("InterruptedException is caught in deliverSimpleMessage: " + ie);
                svLogger.exiting("deliverSimpleMessage", "Exception");
                throw new ResourceException("InterruptedException is caught in deliverSimpleMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                svLogger.exiting(CLASSNAME, "deliverSimpleMessage", "MessageWaitTimeoutException");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // This else statemetns get the exception object from the Work so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        } else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            svLogger.info("deliverSimpleMessage: schedule work");

            // Define the local variable for syncObject
            Object syncObj = new Object();

            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                svLogger.info("deliverSimpleMessage: " + we);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverSimpleMessage: ------chained exception------");
                    svLogger.info("deliverSimpleMessage: " + t1.getCause());
                }

                throw we;
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        svLogger.info("deliverSimpleMessage: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivileged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work, syncObj));
                        final int pWaitTime = waitTime;
                        final FVTSimpleWorkImpl pWork = work;
                        Thread t = (Thread) AccessController.doPrivileged(
                                                                          new PrivilegedAction() {
                                                                              @Override
                                                                              public Object run() {
                                                                                  svLogger.info("deliverSimpleMessage: ScheduleWork: doPrivileged on create Thread t");
                                                                                  return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                              }
                                                                          });

                        t.start();

                        svLogger.info("deliverSimpleMessage: wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                svLogger.info("InterruptedException is caught in deliverSimpleMessage: " + ie);
                svLogger.exiting(CLASSNAME, "deliverSimpleMessage", "InterruptedException");
                throw new ResourceException("InterruptedException is caught in deliverSimpleMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                svLogger.exiting(CLASSNAME, "deliverSimpleMessage", "MessageWaitTimeoutException");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // This else statemetns get the exception object from the Work so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        } else {
            svLogger.exiting(CLASSNAME, "deliverSimpleMessage", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :" + doWorkType);
        }

        svLogger.exiting(CLASSNAME, "deliverSimpleMessage");
    }

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
     *            won't be able to retreive these information.
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
    public void deliverNestedWorkMessage(String deliveryId, FVTMessage message, int state, int waitTime, int doWorkType, int nestedDoWorkType, Xid parentXid,
                                         Xid childXid) throws ResourceException {
        svLogger.entering(CLASSNAME, "deliverNestedWorkMessage",
                          new Object[] { this, deliveryId, message, AdapterUtil.getWorkStatusString(state), new Integer(waitTime), AdapterUtil.getWorkTypeString(doWorkType),
                                         parentXid });

        // Initiate a work instance
        FVTNestedWorkImpl work = new FVTNestedWorkImpl(deliveryId, message, this, nestedDoWorkType, waitTime, state, childXid);

        // Initiate an executionContext
        ExecutionContext ec = new ExecutionContext();

        // Need to set xid to the ec no matter it's null or not.
        ec.setXid(parentXid);

        if (parentXid != null) {
            //ec.setXid(parentXid);

            // Add the parentXid and childXid to the SetActiveTrans only if the work is executed
            // successfully. (according to Cathy).
            // Do not rethrow ResourceException since the test case may supply
            // the same Xid for parent and child work (exception flow)
            // Indicate in trace is enough.  The reason is if deliverNestedWorkMessage
            // may send message with same Xid for parent and child for exceptional flow scenarios.
            // Then it's no appropriate to throw ResourceException
            // Add the xid to the ActiveTransSet only if the xid is valid (format id != -1)
            if (parentXid.getFormatId() >= 0) {
                try {
                    if (!addActiveTransToSet(parentXid)) {
                        svLogger.info("deliverNestedWorkMessage: Duplicate parentXid with global trans id: " + parentXid.getGlobalTransactionId()[0]);
                    }
                } catch (ResourceException re) {
                    svLogger.exiting(CLASSNAME, "deliverNestedWorkMessage", "Unexpected ResourceException. Rethrow it.");
                    throw new WorkException(re);
                }
            }

            // If the xid is not null, then the messageProvider is sending messages
            // to the RA with imported transaction. Then reset the EISTimer.
            // Since the RA is checking the status of the messageProvider for the
            // sake of rolling back active trans and complete indoubt trans,
            // there is no need to reset the EISTimer if xid is null.
            eisTimer.resetTimeLeft();
        }

        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise, return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            svLogger.info("deliverNestedWorkMessage: no work");
        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            svLogger.info("deliverNestedWorkMessage: do work");

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    svLogger.info("deliverNestedWorkMessage: " + work.getName() + " : isWorkCompleted -> true");
                } else {
                    svLogger.info("deliverNestedWorkMessage: " + work.getName() + " : isWorkCompleted -> false");
                    throw new WorkException("Nested Work Parent - " + work.getName() + " : is not in completed state after doWork.");
                }

            } catch (WorkException t) {
                Throwable t1 = t;
                svLogger.info("deliverNestedWorkMessage: " + t);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverNestedWorkMessage: ------chained exception------");
                    svLogger.info("deliverNestedWorkMessage: " + t1.getCause());
                }

                throw t;
            } catch (WorkRuntimeException rte) {
                svLogger.info(" *WorkRuntimeException from parent nested work - " + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - " + work.getName() + " : " + rte.toString());
            }

        }

        else if (doWorkType == FVTMessageProvider.START_WORK) {
            svLogger.info("deliverNestedWorkMessage: start work");

            // Define the local variable for syncObject
            Object syncObj = new Object();

            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.startWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) {
                Throwable t1 = we;
                svLogger.info("deliverNestedWorkMessage: " + we);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverNestedWorkMessage: ------chained exception------");
                    svLogger.info("deliverNestedWorkMessage: " + t1.getCause());
                }

                throw we;
            } catch (WorkRuntimeException rte) {
                svLogger.info(" *WorkRuntimeException from parent nested work - " + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - " + work.getName() + " : " + rte.toString());
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        svLogger.info("deliverNestedWorkMessage: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work, syncObj));
                        final int pWaitTime = waitTime;
                        final FVTNestedWorkImpl pWork = work;
                        Thread t = (Thread) AccessController.doPrivileged(
                                                                          new PrivilegedAction() {
                                                                              @Override
                                                                              public Object run() {
                                                                                  svLogger.info("deliverNestedWorkMessage: StartWork: doPrivileged on create Thread t");
                                                                                  return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                              }
                                                                          });
                        t.start();

                        svLogger.info("deliverNestedWorkMessage: " + work.getName() + " wait for notifications");
                        syncObj.wait();
                    }
                }

            } catch (InterruptedException ie) {
                svLogger.info("InterruptedException is caught in deliverNestedWorkMessage: " + ie);
                svLogger.exiting(CLASSNAME, "deliverNestedWorkMessage", "InterruptedException");
                throw new ResourceException("InterruptedException is caught in deliverNestedWorkMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                svLogger.info("deliverNestedWorkMessage: " + work.getName() + " has not reached the desire state: " + state);
                throw new MessageWaitTimeoutException("Nested Work Parent submission has been timed out. Desire state: " + state + " has not reached.");
            } else {
                // This else statements get the exception object from the Work so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        }

        else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            svLogger.info("deliverNestedWorkMessage: schedule work");

            // Define the local variable for syncObject
            Object syncObj = new Object();

            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) {
                // d174592 - log the exception in trace
                Throwable t1 = we;
                svLogger.info("deliverNestedWorkMessage: " + we);
                while ((t1 = t1.getCause()) != null) {
                    svLogger.info("deliverNestedWorkMessage: ------chained exception------");
                    svLogger.info("deliverNestedWorkMessage: " + t1.getCause());
                }

                throw we;
            } catch (WorkRuntimeException rte) {
                svLogger.info(" *WorkRuntimeException from parent nested work - " + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - " + work.getName() + " : " + rte.toString());
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        svLogger.info("deliverNestedWorkMessage: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work, syncObj));
                        final int pWaitTime = waitTime;
                        final FVTNestedWorkImpl pWork = work;
                        Thread t = (Thread) AccessController.doPrivileged(
                                                                          new PrivilegedAction() {
                                                                              @Override
                                                                              public Object run() {
                                                                                  svLogger.info("deliverNestedWorkMessage: ScheduleWork: doPrivileged on create Thread t");
                                                                                  return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                              }
                                                                          });
                        t.start();

                        svLogger.info("deliverNestedWorkMessage: " + work.getName() + " wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                svLogger.info("InterruptedException is caught in deliverNestedWorkMessage: " + ie);
                svLogger.exiting(CLASSNAME, "deliverNestedWorkMessage", "InterruptedException");
                throw new ResourceException("InterruptedException is caught in deliverNestedWorkMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                svLogger.info("deliverNestedWorkMessage: " + work.getName() + " has not reached the desire state: " + state);
                throw new MessageWaitTimeoutException("Nested Work Parent submission has been timed out. Desire state: " + state + " has not reached.");
            } else {
                // This else statemetns get the exception object from the Work so that
                // sendMessagexxx can throw WorkCompletedException.
                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        }

        else {
            svLogger.exiting(CLASSNAME, "deliverNestedWorkMessage", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :" + doWorkType);
        }

        svLogger.exiting(CLASSNAME, "deliverNestedWorkMessage");
    }

    /**
     * <p>Add work to the works HashMap.</p>
     * <p>It is created for the delivery of nested work. The child of the nested work needs to
     * call this method in order to check the work state of the child nested work.</p>
     *
     * @param workName name of the work
     * @param work work to add
     */
    public void addWork(Object workName, Object work) {
        works.put(workName, work);
    }

    /**
     * <p>This method returns the work instance associated with the message delivery id.</p>
     *
     * <p>After getting the work instance, the test application can call methods
     * isWorkAccepted()/isWorkStarted()/isWorkCompleted/isWorkRejected of the work
     * instance to test whether the work has been accpeted/started/completed/rejected .</p>
     *
     * @param deliveryId the ID related to this message delivery. This message delivery ID
     *            is used to retrieve the work instance.
     *
     * @return an FVT work object.
     */
    public FVTWorkImpl getWork(String deliveryId) {
        svLogger.entering(CLASSNAME, "getWork", new Object[] { this, deliveryId });
        FVTWorkImpl work = (FVTWorkImpl) (works.get(deliveryId));
        svLogger.exiting(CLASSNAME, "getWork", work);
        return work;
    }

    /**
     * <p>This method returns a Hashtable of endpoint wrapper instances associated with the
     * message delivery id.</p>
     *
     * <p>User can get the message endpoint test results from the endpoints for test
     * verification.</p>
     *
     * <p>Users are not allowed to modify the Hashtable returned from this method.</p>
     *
     * @param deliveryId the ID related to this message delivery. This message delivery
     *            is used to retrieve the endpoint wrapper instances.
     *
     * @return a Hashtable of endpoint instances used by this work intance. The hashkey to
     *         get the work instance is endpointName + instanceID.
     */
    public Hashtable getEndpoints(String deliveryId) {
        svLogger.entering(CLASSNAME, "getEndpoints", new Object[] { this, deliveryId });
        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) works.get(deliveryId);

        if (work == null) {
            svLogger.exiting(CLASSNAME, "getEndpoints", "Cannot find work, return null");
            return null;
        } else {
            Hashtable instances = work.getInstances();
            String firstKey = work.getFirstInstanceKey();

            if (instances == null) {
                if (firstKey == null) {
                    svLogger.exiting(CLASSNAME, "getEndpoints", "Cannot find endpoints, return null");
                    return null;
                }

                instances = new Hashtable(1);

                // Add the first instance to the hash table
                instances.put(firstKey, work.getInstance());
            } else {
                // Add the first instance to the hash table
                instances.put(firstKey, work.getInstance());
            }

            svLogger.exiting(CLASSNAME, "getEndpoints", instances);
            return instances;
        }
    }

    // d174256: Added by swai for Work that doesn't send through sendMessagexxx
    // can still get the endpoints to cleanup the test results.

    /**
     * <p>This method returns a Hashtable of endpoint wrapper instances associated with the
     * Work instance.</p>
     *
     * <p>User can get the message endpoint test results from the endpoints for test
     * verification.</p>
     *
     * <p>Users are not allowed to modify the Hashtable returned from this method.</p>
     *
     * @param inputWork this work instance is used to retrieve the endpoint
     *            wrapper instances if the Work is submitted by user instead of by sendMessagexxx.
     *            It should be created through FVTMessageProvider.createWork().
     *
     * @return a Hashtable of endpoint instances used by this work intance. The hashkey to
     *         get the work instance is endpointName + instanceID.
     */
    public Hashtable getEndpoints(FVTGeneralWorkImpl inputWork) {
        svLogger.entering(CLASSNAME, "getEndpoints", new Object[] { this, inputWork });
        if (inputWork == null) {
            svLogger.exiting(CLASSNAME, "getEndpoints", "Cannot find work, return null");
            return null;
        } else {
            Hashtable instances = inputWork.getInstances();
            String firstKey = inputWork.getFirstInstanceKey();

            if (instances == null) {
                if (firstKey == null) {
                    svLogger.exiting(CLASSNAME, "getEndpoints", "Cannot find endpoints, return null");
                    return null;
                }

                instances = new Hashtable(1);
                // Add the first instance to the hash table
                instances.put(firstKey, inputWork.getInstance());
            } else {
                // Add the first instance to the hash table
                instances.put(firstKey, inputWork.getInstance());
            }

            svLogger.exiting(CLASSNAME, "getEndpoints", instances);
            return instances;
        }
    }

    /**
     * <p>This method returns an array of test results asscoicated with a particular
     * endpoint instance (identified by endpoint name and instance ID) in a specific
     * delivery.
     *
     * @param deliveryId the ID related to this message delivery. This message delivery ID
     *            is used to retrieve the endpoint wrapper instances.
     *
     * @return an array of test results associated with a particular endpoint instance.
     */
    public MessageEndpointTestResults[] getTestResults(String deliveryId, String endpointName, int instanceId) {
        svLogger.entering(CLASSNAME, "getTestResults", new Object[] { this, deliveryId, endpointName, new Integer(instanceId) });
        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) works.get(deliveryId);

        if (work == null) {
            svLogger.exiting(CLASSNAME, "getTestResults", "Cannot find the work, return null");
            return null;
        } else {
            // get the work instance object
            MessageEndpointWrapper endpointWrapper = work.getEndpointWrapper(endpointName + instanceId);

            if (endpointWrapper == null) {
                svLogger.exiting(CLASSNAME, "getTestResults", "Cannot find the instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper.getTestResults();
                svLogger.exiting(CLASSNAME, "getTestResults", testResults);
                return testResults;
            }
        }
    }

    /**
     * <p>This method returns the test result asscociated with the message delviery id. </p>
     *
     * <p>This method can only be used when there is only one test result added in this
     * message delivery. If there is more than one test results added in the delivery,
     * there is no guarantee which test result will be returned. Method callers should
     * be aware of this.</p>
     *
     * @param deliveryId the ID related to this message delivery. This message delivery ID
     *            is used to retrieve the endpoint wrapper instances.
     *
     * @return a message endpoint test result object.
     */
    public MessageEndpointTestResults getTestResult(String deliveryId) {
        svLogger.entering(CLASSNAME, "getTestResults", new Object[] { this, deliveryId });
        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) works.get(deliveryId);

        if (work == null) {
            svLogger.exiting(CLASSNAME, "getTestResult", "Canot find the work, return null");
            return null;
        } else {
            // get the work hashtable
            MessageEndpointWrapper endpointWrapper = work.getInstance();

            if (endpointWrapper == null) {
                svLogger.exiting(CLASSNAME, "getTestResult", "Cannot find any instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper.getTestResults();
                svLogger.exiting(CLASSNAME, "getTestResult", testResults[0]);
                return testResults[0];
            }
        }
    }

    // d174256: Added by swai for Work that doesn't send through sendMessagexxx
    // can still get the test results.

    /**
     * <p>This method returns an array of test results asscoicated with a particular
     * endpoint instance (identified by endpoint name and instance ID) in a specific
     * delivery.
     *
     * @param inputWork this work instance is used to retrieve the endpoint
     *            wrapper instances if the Work is submitted by user instead of by sendMessagexxx
     *
     * @return an array of test results associated with a particular endpoint instance.
     */
    public MessageEndpointTestResults[] getTestResults(Work inputWork, String endpointName, int instanceId) {
        svLogger.entering(CLASSNAME, "getTestResults", new Object[] { this, inputWork, endpointName, new Integer(instanceId) });
        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) inputWork;

        if (work == null) {
            svLogger.exiting(CLASSNAME, "getTestResults", "Cannot find the work, return null");
            return null;
        } else {
            // get the work instance object
            MessageEndpointWrapper endpointWrapper = work.getEndpointWrapper(endpointName + instanceId);

            if (endpointWrapper == null) {
                svLogger.exiting(CLASSNAME, "getTestResults", "Cannot find the instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper.getTestResults();
                svLogger.exiting(CLASSNAME, "getTestResults", testResults);
                return testResults;
            }
        }
    }

    // d174256: Added by swai for Work that doesn't send through sendMessagexxx
    // can still get the test results.

    /**
     * <p>This method returns the test result asscociated with the message delviery id. </p>
     *
     * <p>This method can only be used when there is only one test result added in this
     * message delivery. If there is more than one test results added in the delivery,
     * there is no guarantee which test result will be returned. Method callers should
     * be aware of this.</p>
     *
     * @param inputWork this work instance is used to retrieve the endpoint
     *            wrapper instances if the Work is submitted by user instead of by sendMessagexxx
     *
     * @return a message endpoint test result object.
     */
    public MessageEndpointTestResults getTestResult(Work inputWork) {
        svLogger.entering(CLASSNAME, "getTestResults", new Object[] { this, inputWork });
        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) inputWork;

        if (work == null) {
            svLogger.exiting(CLASSNAME, "getTestResult", "Canot find the work, return null");
            return null;
        } else {
            // get the work hashtable
            MessageEndpointWrapper endpointWrapper = work.getInstance();

            if (endpointWrapper == null) {
                svLogger.exiting(CLASSNAME, "getTestResult", "Cannot find any instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper.getTestResults();
                svLogger.exiting(CLASSNAME, "getTestResult", testResults[0]);
                return testResults[0];
            }
        }
    }

    /**
     * <p>This method hints the testing resource adapter to release all the information
     * related to this message delivery ID. TRA will unset the test results of the endpoint
     * instance, recycle the work object, and remove the message delivery ID
     * from the Hashtable. After calling this method, users cannot get any endpoint
     * information from this message delivery ID any more. </p>
     *
     * @param deliveryId the ID related to this message delivery.
     */
    public void releaseDeliveryId(String deliveryId) {
        svLogger.entering(CLASSNAME, "releaseDeliveryId", new Object[] { this, deliveryId });
        // get all the instances including the first one.
        Hashtable instances = getEndpoints(deliveryId);

        // release all the instances
        if (instances != null) {
            Collection endpoints = instances.entrySet();
            for (Iterator iter = endpoints.iterator(); iter.hasNext();) {
                Map.Entry map = (Map.Entry) iter.next();
                MessageEndpointWrapper endpoint = (MessageEndpointWrapper) map.getValue();

                // release and unset the test result
                // d174592 - eat exception from endpoint.release
                try {
                    endpoint.release();
                } catch (Throwable t) {
                    svLogger.info("releaseDeliveryId: " + t);
                    while ((t = t.getCause()) != null) {
                        svLogger.info("releaseDeliveryId: ------chained exception------");
                        svLogger.info("releaseDeliveryId: " + t.getCause());
                    }
                }

                // todo: change this since we support multiple test results in one
                // endpoint instance.
                // endpoint.unsetTestResult();
            }
        }

        // todo - recycle the work objects.
        // remove the work object from the hashmap
        works.remove(deliveryId);
    }

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
    public void releaseWork(FVTGeneralWorkImpl inputWork) {
        svLogger.entering(CLASSNAME, "releaseWork", new Object[] { this, inputWork });
        // get all the instances including the first one.
        Hashtable instances = getEndpoints(inputWork);

        // release all the instances
        if (instances != null) {
            Collection endpoints = instances.entrySet();
            for (Iterator iter = endpoints.iterator(); iter.hasNext();) {
                Map.Entry map = (Map.Entry) iter.next();
                MessageEndpointWrapper endpoint = (MessageEndpointWrapper) map.getValue();

                // release and unset the test result// release and unset the test result
                // d174592 - eat exception from endpoint.release
                try {
                    endpoint.release();
                } catch (Throwable t) {
                    svLogger.info("releaseWork: " + t);
                    while ((t = t.getCause()) != null) {
                        svLogger.info("releaseWork: ------chained exception------");
                        svLogger.info("releaseWork: " + t.getCause());
                    }
                }

                // todo: change this since we support multiple test results in one
                // endpoint instance.
                // endpoint.unsetTestResult();
            }
        }
    }

    // Need to add waitTime and state for executeConcurrentWork so that this will
    // return if all concurrentWork instances reached the desired state.
    /**
     * <p>This method call executes two or more work instances concurrently. This method
     * is mainly used to test associating two or more work instances concurrently with
     * a or more source managed transaction (EIS imported transaction).
     *
     * <p>This method cannot be called concurrently in several threads. That is, you can
     * not initiate two threads, and have both of them call this method. This usage will
     * lead to unexpected hehavior. </p>
     *
     * <p>This method only guarantees executing these works concurrently. It doesn't guarantee
     * delivering messages to endpoints concurrently.</p>
     *
     * @param work an array of Work instance. Currently, we only support executing the work
     *            instances which type or super type is FVTConcurrentWorkImpl.
     * @param xids an array of XID which represents the imported transaction
     * @param doWorkType how to submit the work the work manager. It could be any of the
     *            following: DO_WORK, START_WORK or SCHEDULE_WORK
     * @param state the state which the work instance has reached when the method call returns.
     * @param waitTime This parameter specifies the maximum time for waiting the work to
     *            reach the specified state. Once the waitTime is reached, the method will return
     *            even if the work instance hasn't reach the specified state yet.
     *
     * @exception WorkCompletedException
     */

    public void executeConcurrentWork(Work[] works,
                                      Xid[] xids,
                                      int doWorkType,
                                      int state,
                                      int waitTime) throws ResourceException {
        svLogger.entering(CLASSNAME, "executeConcurrentWork", new Object[] {
                                                                             this,
                                                                             works,
                                                                             xids,
                                                                             new Integer(doWorkType),
                                                                             new Integer(state),
                                                                             new Integer(waitTime) });

        int numOfWorks = works.length;

        // I need works.length sync object for locks. The reason is I have to wait
        // until all works received the correct workEvent (work state) object.
        // Each sync object corresponds to the lock for each work object.
        // Then executeConcurrentWork can be returned to the test case.

        boolean singleXid = (xids.length == 1);

        FVTConcurrentWorkImpl.setConcurrentWorkNumber(numOfWorks);

        // Need a list of Work and EC rather than one as the Work and EC will be used outside
        // this for loop when concurrentWorks are submitted below.
        FVTConcurrentWorkImpl[] work = new FVTConcurrentWorkImpl[numOfWorks];
        ExecutionContext[] ec = new ExecutionContext[numOfWorks];

        for (int i = 0; i < numOfWorks; i++) {
            ec[i] = new ExecutionContext();
            try {
                // Need a list of work rather than one as the work will be used outside
                // this for loop when concurrentWorks are submitted below.
                work[i] = (FVTConcurrentWorkImpl) works[i];
            } catch (ClassCastException cce) {
                svLogger.info("executeConcurrentWork: " + cce);
                throw new WorkRuntimeException("Work instance must be a FVTConcurrentWorkImpl type", cce);
            }

            // Need a list of EC rather than one as the EC will be used outside
            // this for loop when concurrentWorks are submitted below.
            // Initiate a executionContext
            // ExecutionContext ec = new ExecutionContext();

            if (singleXid) {
                ec[i].setXid(xids[0]);
            } else {
                ec[i].setXid(xids[i]);
            }
        }

        try {
            switch (doWorkType) {
                case FVTMessageProvider.DO_WORK: {
                    svLogger.info("executeConcurrentWork: Submit concurrent work with Do work is not allowed.");
                    throw new WorkException("Submit concurrent work with Do work is not allowed.");
                }
                case FVTMessageProvider.START_WORK: {
                    svLogger.info("executeConcurrentWork: Start work");
                    Object syncObj = new Object();

                    // Need a final object for doPrivileged Call
                    final Object pSyncObj = syncObj;

                    work[0].setSyncObj(syncObj);

                    for (int i = 0; i < numOfWorks; i++) {
                        work[i].setNotificationState(state);

                        try {
                            workManager.startWork(work[i], WorkManager.INDEFINITE, ec[i], this);
                        } catch (WorkException we) { // d174592 - log the exception in trace
                            Throwable t1 = we;
                            svLogger.info("executeConcurrentWork: " + we);
                            while ((t1 = t1.getCause()) != null) {
                                svLogger.info("executeConcurrentWork: ------chained exception------");
                                svLogger.info("executeConcurrentWork: " + t1.getCause());
                            }

                            throw we;
                        }
                        // Add the xid to the SetActiveTrans
                        // Do not rethrow ResourceException since the test case may supply
                        // the same Xid for all concurrentWork (exception flow)
                        // Indicate in trace is enough.  The reason is if executeConcurrentWork
                        // will wait until certain WorkException is received, then it's no good
                        // to rethrow ResourceException
                        if (xids[i] != null) {
                            try {
                                if (!addActiveTransToSet(xids[i])) {
                                    svLogger.info("executeConcurrentWork: Duplicate xid with global trans id: " + xids[i].getGlobalTransactionId()[0]);
                                }
                            } catch (ResourceException re) {
                                svLogger.exiting(CLASSNAME, "executeConcurrentWork", "Unexpected ResourceException. Rethrow it.");
                                throw new WorkException(re);
                            }
                        }
                    }

                    try {
                        synchronized (syncObj) {
                            // 174742 - coming here, the work might have already been in the state.
                            // check the state before start the timer
                            if (work[0].isWorkHasReachedState()) {
                                svLogger.info("executeConcurrentWork: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                            } else {
                                // start the timer
                                // Need to use doPrivleged call to create a thread
                                // Thread t = new Thread(new Timer(waitTime, work[0], syncObj));
                                final int pWaitTime = waitTime;
                                final FVTConcurrentWorkImpl pWork = work[0];
                                Thread t = (Thread) AccessController.doPrivileged(
                                                                                  new PrivilegedAction() {
                                                                                      @Override
                                                                                      public Object run() {
                                                                                          svLogger.info("executeConcurrentWork: StartWork: doPrivileged on create Thread t");
                                                                                          return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                                      }
                                                                                  });
                                t.start();
                                svLogger.info("executeConcurrentWork: wait for notifications");
                                syncObj.wait();
                            }
                        }
                    } catch (InterruptedException ie) {
                        svLogger.info("InterruptedException is caught in executeConcurrentWork: " + ie);
                        svLogger.exiting(CLASSNAME, "executeConcurrentWork", "InterruptedException");
                        throw new ResourceException("InterruptedException is caught in executeConcurrentWork", ie);
                    }

                    if (timedout) { // d173989
                        timedout = false;
                        svLogger.info("executeConcurrentWork: ConcurrentWorks have not reached the desire state: " + state);
                        throw new MessageWaitTimeoutException("Concurrent Work submission has been timed out. Desire state: " + state + " has not reached.");
                    } else {
                        // This else statements get the exception object from the Work so that
                        // sendMessagexxx can throw WorkCompletedException.
                        // Will throw the workException from the last submitted work only.
                        if (work[numOfWorks - 1].getWorkException() != null) {
                            throw work[numOfWorks - 1].getWorkException();
                        }
                    }
                    break;
                }
                case FVTMessageProvider.SCHEDULE_WORK: {
                    svLogger.info("executeConcurrentWork: Schedule work");

                    Object syncObj = new Object();

                    // Need a final object for doPrivileged Call
                    final Object pSyncObj = syncObj;

                    work[0].setSyncObj(syncObj);

                    for (int i = 0; i < numOfWorks; i++) {
                        work[i].setNotificationState(state);

                        try {
                            workManager.scheduleWork(work[i], WorkManager.INDEFINITE, ec[i], this);
                        } catch (WorkException we) { // d174592 - log the exception in trace
                            Throwable t1 = we;
                            svLogger.info("executeConcurrentWork: " + we);
                            while ((t1 = t1.getCause()) != null) {
                                svLogger.info("executeConcurrentWork: ------chained exception------");
                                svLogger.info("executeConcurrentWork: " + t1.getCause());
                            }

                            throw we;
                        }
                        // Add the xid to the SetActiveTrans
                        // Do not rethrow ResourceException since the test case may supply
                        // the same Xid for all concurrentWork (exception flow)
                        // Indicate in trace is enough.  The reason is if executeConcurrentWork
                        // will wait until certain WorkException is received, then it's no good
                        // to rethrow ResourceException
                        if (xids[i] != null) {
                            try {
                                if (!addActiveTransToSet(xids[i])) {
                                    svLogger.exiting(CLASSNAME, "executeConcurrentWork", "Duplicate xid with global trans id: " + xids[i].getGlobalTransactionId()[0]);
                                }
                            } catch (ResourceException re) {
                                svLogger.exiting(CLASSNAME, "executeConcurrentWork", "Unexpected ResourceException. Rethrow it.");
                                throw new WorkException(re);
                            }
                        }
                    }

                    try {
                        synchronized (syncObj) {
                            // 174742 - coming here, the work might have already been in the state.
                            // check the state before start the timer
                            if (work[0].isWorkHasReachedState()) {
                                svLogger.info("executeConcurrentWork: " + AdapterUtil.getWorkStatusString(state) + " has been reached");
                            } else {
                                // start the timer
                                // Need to use doPrivleged call to create a thread
                                // Thread t = new Thread(new Timer(waitTime, work[0], syncObj));
                                final int pWaitTime = waitTime;
                                final FVTConcurrentWorkImpl pWork = work[0];
                                Thread t = (Thread) AccessController.doPrivileged(
                                                                                  new PrivilegedAction() {
                                                                                      @Override
                                                                                      public Object run() {
                                                                                          svLogger.info("executeConcurrentWork: ScheduleWork: doPrivileged on create Thread t");
                                                                                          return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                                                      }
                                                                                  });
                                t.start();
                                svLogger.info("executeConcurrentWork: wait for notifications");
                                syncObj.wait();
                            }
                        }
                    } catch (InterruptedException ie) {
                        svLogger.info("InterruptedException is caught in executeConcurrentWork: " + ie);
                        svLogger.exiting(CLASSNAME, "executeConcurrentWork", "InterruptedException");
                        throw new ResourceException("InterruptedException is caught in executeConcurrentWork", ie);
                    }

                    if (timedout) { // d173989
                        timedout = false;
                        svLogger.info("executeConcurrentWork: ConcurrentWorks have not reached the desire state: " + state);
                        throw new MessageWaitTimeoutException("Concurrent Work submission has been timed out. Desire state: " + state + " has not reached.");
                    } else {
                        // This else statements get the exception object from the Work so that
                        // sendMessagexxx can throw WorkCompletedException.
                        // Will throw the workException from the last submitted work only.
                        if (work[numOfWorks - 1].getWorkException() != null) {
                            throw work[numOfWorks - 1].getWorkException();
                        }
                    }

                    break;
                }
                default:
                    throw new WorkException("TRA doesn't support doWork type " + doWorkType);
            }
        } catch (WorkException we) {
            svLogger.exiting(CLASSNAME, "executeConcurrentWork", "Exception " + we.getMessage());
            throw we;
        }
        svLogger.exiting(CLASSNAME, "executeConcurrentWork");
    }

    //--------------------------------------------------------------------------
    // work event notification methods.
    //--------------------------------------------------------------------------

    /**
     * Invoked when a Work instance has been accepted.
     */
    @Override
    public void workAccepted(WorkEvent event) {
        svLogger.info("workAccepted: " + event);
        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_ACCEPTED);
    }

    /**
     * Invoked when a Work instance has been rejected.
     */
    @Override
    public void workRejected(WorkEvent event) {
        svLogger.info("workRejected: " + event);

        // Add the exception object to the Work so that sendMessagexxx can
        // throw WorkRejectedException.
        // Useful for WorkRejected after work is accepted
        // Firstly, check if there is an exception or not.
        // If so, then see what exception is that and then throw it.

        if (event.getException() != null) {
            if ((event.getException().getClass().getName().equals(rejectedException))) {
                ((FVTWorkImpl) event.getWork()).setWorkException(event.getException());
            }
        }

        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_REJECTED);
    }

    /**
     * Invoked when a Work instance has started execution. This only means that a
     * thread has been allocated.
     */
    @Override
    public void workStarted(WorkEvent event) {
        svLogger.info("workStarted: " + event);
        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_STARTED);
    }

    /**
     * Invoked when a Work instance has completed execution.
     */
    @Override
    public void workCompleted(WorkEvent event) {
        svLogger.info("workCompleted: " + event);

        // Add the exception object to the Work so that sendMessagexxx can
        // throw WorkCompletedException.
        // Firstly, check if there is an exception or not.
        // If so, then see what exception is that and then throw it.

        if (event.getException() != null) {
            if ((event.getException().getClass().getName().equals(completedException))) {
                ((FVTWorkImpl) event.getWork()).setWorkException(event.getException());
            }
        }

        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_COMPLETED);

        if (svMessageLatch != null)
            svMessageLatch.countDown();
    }

    public String introspecSelf() {
        String lineSeparator = System.getProperty("line.separator");

        StringBuffer str = new StringBuffer();
        // Add the work instance
        str.append("Work instances:" + lineSeparator);

        Collection instances = works.entrySet();
        if (works != null) {
            for (Iterator iter = instances.iterator(); iter.hasNext();) {
                Map.Entry map = (Map.Entry) iter.next();
                String iKey = (String) map.getKey();
                FVTWorkImpl work = (FVTWorkImpl) map.getValue();
                str.append(iKey + "={" + lineSeparator + work + "}" + lineSeparator);
            }
        } else {
            str.append("No works" + lineSeparator);
        }

        return str.toString();
    }

    /**
     * <p>This method will add the indoubt transaction to indoubt trans set</p>
     * and remove the xid from the active trans set
     *
     * @param xid The xid of the imported transaction
     * @exception ResourceException
     */
    // Synchronized methods which add/remove Xid from Set
    protected synchronized boolean addTransToSet(Xid xid, Set tranSet) throws ResourceException {
        boolean rc_add;
        svLogger.entering(CLASSNAME, "addTransToSet", "Entry");

        try {
            //rc_remove = SetActiveTrans.remove(xid);
            rc_add = tranSet.add(xid);
        } catch (UnsupportedOperationException uoe) {
            svLogger.info("addTransToSet: " + uoe);
            throw uoe;
        } catch (ClassCastException cce) {
            svLogger.info("addTransToSet: " + cce);
            throw cce;
        } catch (NullPointerException npe) {
            svLogger.info("addTransToSet: " + npe);
            throw npe;
        } catch (IllegalArgumentException iae) {
            svLogger.info("addTransToSet: " + iae);
            throw iae;
        }

        return rc_add;
    }

    /**
     * <p>This method will add the active transaction to active trans set</p>
     *
     * @param xid The xid of the imported transaction
     * @exception ResourceException
     */
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean addActiveTransToSet(Xid xid) throws ResourceException {
        byte[] gid = xid.getGlobalTransactionId();
        svLogger.entering(CLASSNAME, "addActiveTransToSet", "Xid with global trans Id " + gid[0]);

        // Due to M3_TransactionInflow_4.testSubmitAfterPreparedCMTRequired, it is possible
        // that the user may send another message with the same xid while he has prepared
        // the transaction already. So it is possible that a xid is added to the active
        // trans set while it is already existed in the indoubt trans set. Since this
        // test scenario throws exception, so do not add the active trans while the
        // same xid already exist in the indoubt trans.
        if (setContainsTrans(SetIndoubtTrans, xid) != null) {
            svLogger.info("addActiveTransToSet: Xid exists in indoubt trans set. Will not added to active trans set.");
            return true;
        }
        return (addTransToSet(xid, SetActiveTrans));
    }

    /**
     * <p>This method will add the indoubt transaction to indoubt trans set</p>
     * and remove the xid from the active trans set
     *
     * @param xid The xid of the imported transaction
     * @exception ResourceException
     */
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean addIndoubtTransToSet(Xid xid) throws ResourceException {
        byte[] gid = xid.getGlobalTransactionId();
        svLogger.info("addIndoubtTransToSet: Xid with global trans Id " + gid[0]);
        return (addTransToSet(xid, SetIndoubtTrans));
    }

    /**
     * <p>This method will remove the transaction to trans set</p>
     *
     * @param xid The xid of the imported transaction
     * @exception ResourceException
     */
    // Synchronized methods which add/remove Xid from Set
    protected synchronized boolean removeTransFromSet(Xid xid, Set tranSet) throws ResourceException {
        boolean rc_remove;
        svLogger.entering(CLASSNAME, "removeTransFromSet", "Entry");

        try {
            svLogger.info("removeTransFromSet: About to remove Xid with gid=" + xid.getGlobalTransactionId()[0] + " from set");
            // 10/29/03: rruvinsk
            // Because we're dealing with two different implementations of Xid, we need
            // to call setContainsTrans to get an instance of the actual object in the set
            // we want to remove
            // 01/21/04: swai
            // Check if the xid is in the set. If not, setcontainsTrans return null
            Xid tempXid = setContainsTrans(tranSet, xid);
            if (tempXid == null) {
                svLogger.info("removeTransFromSet: Xid with gid=" + xid.getGlobalTransactionId()[0] + " does not exist in set.");
                return false;
            }
            rc_remove = tranSet.remove(tempXid);

            if (!rc_remove) {
                svLogger.info("removeTransFromSet: Xid with gid=" + xid.getGlobalTransactionId()[0] + " can not be removed from set.");
                return false;
            }

            svLogger.info("removeTransFromSet: Done removing from set");
        } catch (UnsupportedOperationException uoe) {
            svLogger.info("removeTransFromSet: " + uoe);
            throw uoe;
        } catch (ClassCastException cce) {
            svLogger.info("removeTransFromSet: " + cce);
            throw cce;
        } catch (NullPointerException npe) {
            svLogger.info("removeTransFromSet: " + npe);
            throw npe;
        }

        svLogger.exiting(CLASSNAME, "removeTransFromSet", "Exit");
        return rc_remove;
    }

    /**
     * <p>This method will remove the active transaction to active trans set</p>
     *
     * @param xid The xid of the imported transaction
     * @exception ResourceException
     */
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean removeActiveTransFromSet(Xid xid) throws ResourceException {
        byte[] gid = xid.getGlobalTransactionId();
        svLogger.info("removeActiveTransFromSet: Xid with global trans Id " + gid[0]);
        return (removeTransFromSet(xid, SetActiveTrans));
    }

    /**
     * <p>This method will remove the indoubt transaction to indoubt trans set</p>
     *
     * @param xid The xid of the imported transaction
     * @exception ResourceException
     */
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean removeIndoubtTransFromSet(Xid xid) throws ResourceException {
        byte[] gid = xid.getGlobalTransactionId();
        svLogger.info("removeIndoubtTransFromSet: Xid with global trans Id " + gid[0]);
        return (removeTransFromSet(xid, SetIndoubtTrans));
    }

    /**
     * <p>This method will get the indoubt transaction set</p>
     *
     * @return Set of Xid
     */
    public Set getIndoubtTransSet() {
        return SetIndoubtTrans;
    }

    /**
     * <p>Verify if the list of indoubt trans from app server is the same as the list keep in TRA.
     * If not, exception will be thrown and the indoubt trans list in TRA will be cleanup.</p>
     *
     */
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean verifyIndoubtTrans(Xid[] xidList) throws ResourceException {
        // For each xid in the xidList, see if it is in the Set. If so, remove that one from
        // the Set. If not, save that one to a list of non-exist xid and flag fails.
        // If fail, throw ResourceException
        boolean listMatch = true;
        byte[] gid;
        int size = xidList.length;

        // Check the size of the xidList from the Appserver after the recover call
        svLogger.info("verifyIndoubtTrans: Size of Xid from Appserver after recover call is: " + size);

        // Create a temp set for remove of XID element instead of using the original
        // Indoubt Trans. The removal of the XID in indoubt trans should be done when
        // the indoubt trans is committed.
        HashSet tempSet = new HashSet(SetIndoubtTrans);

        for (int i = 0; i < size; i++) {
            // Can't compare the returned XID directly to the ones in set as
            // the returned XIDs are different implmentations of the XID
            // interface. Use setContainsTrans method to compare the returned
            // XID with the one from the indoubt trans set.
            // If xidInSet is null, means the xidList[i] is not in set.
            Xid xidInSet = setContainsTrans(SetIndoubtTrans, xidList[i]);
            gid = xidList[i].getGlobalTransactionId();

            // Can't use Set.contains() due to the above reason.
            //if( !SetIndoubtTrans.contains(xidList[i]) )
            if (xidInSet == null) {
                listMatch = false;
                svLogger.info("verifyIndoubtTrans: Xid with global trans Id=" + gid[0] + "not in Indoubt Trans list from TRA!");
            }
            // Use the returned xidInSet to remove from temp indoubt trans set.
            //else if(!removeIndoubtTransFromSet(xidList[i]))
            else if (!removeTransFromSet(xidInSet, tempSet)) {
                // Remove this xid from the set. But the xid doesn't exist in the Set
                svLogger.info("verifyIndoubtTrans: Xid with global trans Id=" + gid[0] + "can't be removed in Indoubt Trans list!");
            }
        }
        // If the temp set is not empty, means the 2 lists mismatch
        // so check the tempSet instead of the SetIndoubtTrans
        //if( !SetIndoubtTrans.isEmpty() )
        if (!tempSet.isEmpty()) {
            // Suppose the 2 lists should match. Since there are more indoubt
            // trans in the TRA set than the one from app server, unmatch!
            svLogger.info("verifyIndoubtTrans: There are more Xid in SetIndoubtTrans than that from AppServer!");
            Iterator it = tempSet.iterator();
            while (it.hasNext()) {
                gid = ((XidImpl) (it.next())).getGlobalTransactionId();
                svLogger.info("verifyIndoubtTrans: Xid with global trans Id=" + gid[0] + "not in Indoubt Trans list from AppServer!");
            }
            listMatch = false;
        }
        if (!listMatch) {
            // Since the lists are unmatched, cleanup the list in TRA so that the next test
            // case will have a new SetIndoubtTrans.
            SetIndoubtTrans.clear();
            svLogger.info("verifyIndoubtTrans: There are unmatched Xid. Check trace message above!");
        }
        return listMatch;
    }

    /**
     * <p>This method will rollback all active transaction from the active trans set.</p>
     *
     * <p>If there is anything wrong when rollback or when remove xid from active trans set,
     * it will return false and cleanup the active trans set.</p>
     *
     * <p>This method is called by EISTimer</p>
     *
     */
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean rollbackAllActiveTrans() {
        Iterator it = SetActiveTrans.iterator();
        XidImpl xid;
        XATerminator xaTerm = xaTermWrapper.getNativeXaTerm();
        byte[] gid;
        boolean rollbackOK = true;

        svLogger.entering(CLASSNAME, "rollbackActiveTrans", "Entry");

        while ((it.hasNext() && rollbackOK)) {
            xid = (XidImpl) (it.next());
            gid = xid.getGlobalTransactionId();
            try {
                svLogger.info("rollbackActiveTrans: Rollback Xid with global trans Id=" + gid[0]);
                xaTerm.rollback(xid);
                // remove the element from the active trans set
                // use it.remove() instead of removeActiveTransFromSet
                // This is because if not using it.remove(), then the
                // iterator will be invalid anymore. Calling it.next()
                // will raise ConcurrentModificationException.
                it.remove();
            } catch (XAException xe) {
                svLogger.info("rollbackActiveTrans: Can not rollback xid with global transaction id " + gid[0]);
                rollbackOK = false;
            }
        }
        // If there is anything wrong in rollback all the active trans, clear
        // the active trans set.
        if (!rollbackOK) {
            svLogger.info("rollbackActiveTrans: Rollback not OK. Clearing SetActiveTrans");
            SetActiveTrans.clear();
        }

        svLogger.exiting(CLASSNAME, "rollbackActiveTrans", "Exit");
        return rollbackOK;
    }

    /**
     * Iterates through set 'set' and checks to see if it contains xid 'xid.'
     * Necessary because two different implementations of Xid are used: XidImpl and
     * WebSphere's. This makes it impossible to compare them using .equals(), which the
     * Set.contains() method relies on.
     *
     * @param set The set to look in
     * @param xid The xid to search for
     * @return The xid from the set if found; null otherwise
     */
    private Xid setContainsTrans(Set set, Xid xid) {
        Iterator i = set.iterator();
        while (i.hasNext()) {
            Xid setXid = (Xid) i.next();
            svLogger.info("setContainsTrans: Xid from Set has bq = " + setXid.getBranchQualifier()[0]);
            svLogger.info("setContainsTrans: Xid supplied has bq = " + xid.getBranchQualifier()[0]);

            svLogger.info("setContainsTrans: Xid from Set has formatId = " + setXid.getFormatId());
            svLogger.info("setContainsTrans: Xid supplied has formatId = " + xid.getFormatId());

            svLogger.info("setContainsTrans: Xid from Set has gid = " + setXid.getGlobalTransactionId()[0]);
            svLogger.info("setContainsTrans: Xid supplied has gid = " + xid.getGlobalTransactionId()[0]);

            /*
             * 02/19/04: swai
             * d188279
             * Since the pass-in xid may be from recover() call, the format Id and the
             * BranchQualifier will be different from what we submitted when we send
             * message. Therefore, we can only rely on the check on globalTranId to
             * determine if the xid from recover() call is the same one listed in
             * the indoubt or active trans Set in TRA.
             * As a result, if the globalTranId of the 2 xids are the same, we treat
             * that xid from the recover() call exist in either indoubt or active
             * trans set.
             */
            if (setXid.getGlobalTransactionId()[0] == xid.getGlobalTransactionId()[0]) {
                svLogger.info("setContainsTrans: Xid is in Set.");
                return setXid;
            }
        }

        svLogger.info("setContainsTrans: Xid is NOT in Set.");
        return null;
    }

    /**
     * <p>This method will return the EISTimer associated with this workDispatcher</p>
     *
     * @return EISTimer The EISTimer instance associated with this workDispatcher
     */
    public EISTimer getEISTimer() {
        return eisTimer;
    }

    // d177221 ends: swai: 09/24/03

    class Timer implements Runnable {
        private final Object syncObj;
        private final int waitTimeout;

        // 11/20/03: swai
        // Let timer to find out if work has reached the desired state.
        private final FVTWorkImpl work;

        // 11/20/03: swai
        public Timer(int waitTimeout, FVTWorkImpl work, Object obj) {
            svLogger.entering(CLASSNAME, "WorkDispatcher.Timer.init", new Object[] { work, new Integer(waitTimeout), obj });
            syncObj = obj;
            this.waitTimeout = waitTimeout;
            this.work = work;
            svLogger.exiting(CLASSNAME, "<init>", this);
        }

        @Override
        public void run() {
            try {
                // Sleep for waitTimeout millisecond.
                Thread.sleep(waitTimeout);
                // 11/19/03: swai
                // Only need to set timedout to true if we really need the timer.
                if (!work.isWorkHasReachedState()) {
                    // Has to set timedout to true
                    timedout = true;
                    svLogger.info("timer: timeout");

                    // Notify the waiters
                    synchronized (syncObj) {
                        syncObj.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                svLogger.info("InterruptedException is caught in Timer: " + e);
                throw new RuntimeException("InterruptedException is caught in Timer", e);
            }
        }
    }
}