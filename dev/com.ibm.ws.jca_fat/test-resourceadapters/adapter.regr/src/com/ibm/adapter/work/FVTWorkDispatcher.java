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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.TransactionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.adapter.AdapterUtil;
import com.ibm.adapter.EISTimer;
import com.ibm.adapter.FVTAdapterImpl;
import com.ibm.adapter.FVTSecurityContext;
import com.ibm.adapter.XATerminatorWrapper;
import com.ibm.adapter.endpoint.MessageEndpointFactoryWrapper;
import com.ibm.adapter.endpoint.MessageEndpointWrapper;
import com.ibm.adapter.message.FVTMessage;
import com.ibm.adapter.message.FVTMessageProvider;
import com.ibm.adapter.message.FVTMessageProviderImpl;
import com.ibm.adapter.message.MessageWaitTimeoutException;
import com.ibm.adapter.message.TextMessageImpl;
import com.ibm.adapter.message.WorkInformation;
import com.ibm.adapter.tra.FVTConcurrentWorkImpl;
import com.ibm.adapter.tra.FVTWorkImpl;
import com.ibm.adapter.tra.FVTXAResourceImpl;
import com.ibm.adapter.tra.XidImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.csi.MessageEndpointTestResults;

/**
 * <p>
 * This class represents a work dispatcher. A work dispatcher is repsonsible for
 * creating and submitting work objects.
 * </p>
 *
 * <p>
 * Message providers, such as FVTMessageProvider, can call doMessageDelivery()
 * or scheduleMessageDelivery() of this class to delivery messages to end point
 * applications.
 * </p>
 *
 * <p>
 * When doMessageDelivery() or scheduleMessageDelivery() are called, an instance
 * of FVTSimpleWorkImpl or FVTComplexWorkImpl is created and submitted to the
 * work manager of the application server for execution.
 * </p>
 */
public class FVTWorkDispatcher implements WorkListener {

    private static final TraceComponent tc = Tr
                    .register(FVTWorkDispatcher.class);

    /** Adapter instance */
    protected FVTAdapterImpl adapter;

    /** Work manager */
    protected WorkManager workManager;

    /** XATerminator */
    protected XATerminatorWrapper xaTermWrapper;

    // d177221 begin: 
    /** EIS Timer */
    private EISTimer eisTimer;

    /** Thread for EIS Timer */
    private Thread eisStatusThread;

    /** vector of work instance */
    protected Hashtable works;

    /** Object used for synchronization */
    // 11/20/03: 
    // We will use a local variable for syncObj instead of instance variable.
    // In this case we can ensure the sync block is unlocked by the appropriate
    // timer or the workEvent.
    // protected Object syncObj = new Object();

    /** indicate whether the message delivery method is timed out or not */
    protected boolean timedout = false;

    /** Name for WorkCompletedException class */
    private final String completedException = WorkCompletedException.class.getCanonicalName();

    /** Name for WorkRejectedException class */
    private final String rejectedException = WorkRejectedException.class.getCanonicalName();

    // d177221: 09/23/03
    // RA (WorkDispatcher) should keep a set of active trans.
    // This set serves as this set of active trans.
    /** Set of active transactions */
    private final Set SetActiveTrans = new HashSet();

    /** Set of indoubt transactions */
    private final Set SetIndoubtTrans = new HashSet();

    // 11/19/03: 
    // Add a new instance variable to indicate if we need timeout or not.
    // If workEventReceived is set to true, that means we don't need the
    // timeout.
    // If we don't need the timeout, we shouldn't throw the timeout exception.
    private final boolean workEventReceived = false;

    /**
     * <p>
     * Constructor.
     * </p>
     *
     * @param adapter
     *            the FVTAdapterImpl instance
     * @param workManager
     *            the work manager instance from the application server
     * @param xaTerm
     *            the XA terminator from the application server
     */
    public FVTWorkDispatcher(FVTAdapterImpl adapter, WorkManager workManager,
                             XATerminator xaTerm) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>",
                     new Object[] { adapter, workManager, xaTerm });

        this.adapter = adapter;
        this.workManager = workManager;
        this.xaTermWrapper = (XATerminatorWrapper) xaTerm;

        // initialize works hash map
        works = new Hashtable(11);

        // d177221 begin: 09/21/03
        // create a thread for the timer object to check the status of the EIS
        // (messageProvider)
        // Currently there is only 1 timer object as there is only 1
        // messageProvider.
        // 10/22/03: 
        // Modified the constructor of the EISTimer, add WorkDispatcher
        // this avoid provider need to know the WorkDispatcher instance.
        eisTimer = new EISTimer(this, adapter.getProvider());

        // d177221: 09/22/03
        // Assign this timer object to the XATermWrapper so that the wrapper
        // can reset the timer.
        // The 'xaTermWrapper' object will be null if:
        // a) the .start() method has never been invoked on the RA object
        // b) the .stop() method HAS been invoked on the RA object
        // In either of these cases, we'll get a NullPointerException here. The
        // best solution would be to check this
        // object for null-ness and then throw a clean exception stating that,
        // but that would require changing the signature
        // on the constructor, which then force everywhere its called to deal
        // with it. So, I'm going with the path of least
        // resistance, which is to log message indicating that its null so that
        // when the NPE does happen, we can at least check
        // the log and no for sure what went wrong.
        if (xaTermWrapper == null) {
            System.out
                            .println("The xaTermWrapper object on the RA object is null.  This will cause a NullPointer in the instantiation "
                                     + "of the FVTWorkDispatcher object.");
        } // end if
        this.xaTermWrapper.setEISTimer(eisTimer);

        // d177221: 09/25/03
        // Assign this workDispatcher object to the XATermWrapper so that the
        // wrapper
        // can compare the list of indoubt trans from app server and from TRA
        this.xaTermWrapper.setWorkDispatcher(this);

        // 02/17/04: 
        // Need to use doPrivleged call to create a thread
        // eisStatusThread = new Thread(eisTimer);
        eisStatusThread = (Thread) java.security.AccessController
                        .doPrivileged(new java.security.PrivilegedAction() {
                            @Override
                            public Object run() {
                                if (tc.isEntryEnabled())
                                    Tr.entry(tc,
                                             "doPrivileged on create eisTimerThread");
                                return new Thread(eisTimer);
                            }
                        });
        if (eisStatusThread != null) {
            eisStatusThread.start();
        } else {
            if (tc.isEntryEnabled())
                Tr.debug(tc, "<init>", "Returned eisStatusThread is null.");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);

    }

    /**
     * Get the message endpoint factory from the endpoint name
     */
    public MessageEndpointFactoryWrapper getMessageFactory(String endpointName) {
        return (MessageEndpointFactoryWrapper) adapter.getMessageFactories()
                        .get(endpointName);
    }

    // -----------------------------------------------------------
    // sendMessage methods.
    // -----------------------------------------------------------

    /**
     * <p>
     * Deliver a complex message represented by an FVTMessage object using
     * doWork, scheduleWork, startWork, or direct call without any work instance
     * involved. If the doWorkType is DO_WORK, the method blocks until the
     * message delivery is completed. If the doWorkType is SCHEDULE_WORK or
     * START_WORK, the method blocks until the work instance has reached the
     * specified state.
     * </p>
     *
     * <p>
     * This method is called by scheduleMessageDelivery or doMessageDelivery
     * methods in the FVTMessageProviderImpl class.
     * </p>
     *
     * <p>
     * This method will return and do nothing if the EISStatus is STATUS_FAIL.
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
     * @param xid
     *            the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    public void deliverComplexMessage(String deliveryId, FVTMessage message,
                                      int state, int waitTime, int doWorkType, Xid xid) throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "deliverComplexMessage",
                     new Object[] { this, deliveryId, message,
                                    AdapterUtil.getWorkStatusString(state),
                                    new Integer(waitTime),
                                    AdapterUtil.getWorkTypeString(doWorkType), xid });

        // d177221 begin: 
        // Check the status of the messageProvider before delivering messages
        // This is not be necessary as if the test case signal EIS failure,
        // it should not send any more messages.

        if (adapter.getProvider().getEISStatus() == FVTMessageProviderImpl.STATUS_FAIL) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexMessage",
                         "MessageProvider fails. Should not send messages.");
            }
            throw new ResourceException("deliverComplexMessage: MessageProvider fails. Should not send messages.");
        }

        // Initiate a work instance
        FVTComplexWorkImpl work = new FVTComplexWorkImpl(deliveryId, message, this);

        // Initiate an executionContext
        ExecutionContext ec = new ExecutionContext();

        // 12/03/03: 
        // Need to set xid to the ec no matter it's null or not.
        ec.setXid(xid);

        if (xid != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "deliverComplexMessage", "xid is not null.");

            // 12/03/03: 
            // ec.setXid(xid);

            // d177221: 09/30/03
            // Add the xid to the SetActiveTrans
            // 12/03/03: 
            // Add the xid to the ActiveTransSet only if the xid is valid
            // (format id != -1)
            if (xid.getFormatId() >= 0) {
                if (!addActiveTransToSet(xid)) {
                    if (tc.isEntryEnabled())
                        Tr.debug(tc, "deliverComplexMessage", "Duplicate xid!");
                    // 12/17/03: 
                    // Do not throw the Duplicate xid exception
                    // throw new ResourceException("Duplicate xid!");
                }
            }

            // d177221: 09/18/03
            // If the xid is not null, then the messageProvider is sending
            // messages
            // to the RA with imported transaction. Then reset the EISTimer.
            // Since the RA is checking the status of the messageProvider for
            // the
            // sake of rolling back active trans and complete indoubt trans,
            // there is no need to reset the EISTimer if xid is null.
            eisTimer.resetTimeLeft();
        }

        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise,
        // return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexMessage", "no work");
            }

            // Don't use work manager, directly call run() method.
            try {
                work.run();
            } catch (RuntimeException t) { // d174592 - log the exception in
                                           // trace
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexMessage", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexMessage", t1.getCause());
                    }
                }

                throw t;
            }
        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexMessage", "do work");
            }

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverComplexWorkMessage",
                                 work.getName() + " : isWorkCompleted -> true");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverComplexWorkMessage",
                                 work.getName() + " : isWorkCompleted -> false");
                    throw new WorkException("ComplexWork - " + work.getName()
                                            + " : is not in completed state after doWork.");
                }
            } catch (WorkException t) { // d174592 - log the exception in trace
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexMessage", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexMessage", t1.getCause());
                    }
                }

                throw t;
            }
        } else if (doWorkType == FVTMessageProvider.START_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexMessage", "start work");
            }

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.startWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexMessage", t1.getCause());
                    }
                }
                throw we;
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                  + " has been reached");
                    } else {
                        // start the timer
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(tc,
                                                             "deliverComplexMessage",
                                                             "StartWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexMessage",
                                     "wait for notifications");
                        syncObj.wait();
                    }
                }

            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverComplexMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverComplexMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexMessage", "Exception");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // d174256: 
                // This else statements get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }

        } else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexMessage", "schedule work");
            }

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager
                                .scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexMessage", t1.getCause());
                    }
                }

                throw we;
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                  + " has been reached");
                    } else {
                        // start the timer
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(tc,
                                                             "deliverComplexMessage",
                                                             "ScheduleWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexMessage",
                                     "wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverComplexMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverComplexMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexMessage", "Exception");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // d174256: 
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deliverComplexMessage", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :"
                                           + doWorkType);
        }

        /*
         * 12/03/03: Need to add xid to active trans set before work is
         * submitted. The reason is even if the work is failed, the transaction
         * is still in active state.
         *
         * // d177221: 09/30/03 // Add the xid to the SetActiveTrans if
         * (xid != null) { if(!addActiveTransToSet(xid)) { if
         * (tc.isEntryEnabled()) Tr.exit(tc, "deliverComplexMessage",
         * "Duplicate xid!"); throw new ResourceException("Duplicate xid!"); } }
         */

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deliverComplexMessage");

    }

    /**
     * <p>
     * Deliver a complex message represented by an FVTMessage object using
     * doWork, scheduleWork, startWork, or direct call without any work instance
     * involved. If the doWorkType is DO_WORK, the method blocks until the
     * message delivery is completed. If the doWorkType is SCHEDULE_WORK or
     * START_WORK, the method blocks until the work instance has reached the
     * specified state.
     * </p>
     *
     * <p>
     * This method is called by the sendJCA16Message method in the
     * FVTMessageProviderImpl class.
     * </p>
     *
     * <p>
     * This method will return and do nothing if the EISStatus is STATUS_FAIL.
     *
     * <p>
     * This method will allow configuration of the securityContext required for
     * inbound security propagation as per JCA 1.6.
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
     * @param xid
     *            the XID which represents the imported transaction
     * @param wi
     *            the WorkInformation that is used to configure the security
     *            context under which the work should execute.
     *
     * @exception ResourceException
     */
    public void deliverComplexJCA16Message(String deliveryId,
                                           FVTMessage message, int state, int waitTime, int doWorkType,
                                           Xid xid, WorkInformation workInfo) throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "deliverComplexJCA16Message",
                     new Object[] { this, deliveryId, message,
                                    AdapterUtil.getWorkStatusString(state),
                                    new Integer(waitTime),
                                    AdapterUtil.getWorkTypeString(doWorkType), xid,
                                    workInfo });

        // Check the status of the messageProvider before delivering messages
        // This is not be necessary as if the test case signal EIS failure,
        // it should not send any more messages.

        if (adapter.getProvider().getEISStatus() == FVTMessageProviderImpl.STATUS_FAIL) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexJCA16Message",
                         "MessageProvider fails. Should not send messages.");
            }
            throw new ResourceException("deliverComplexJCA16Message: MessageProvider fails. Should not send messages.");
        }

        // Initiate a work instance
        ExecutionContext ec = null;
        FVTComplexJCA16WorkImpl work = new FVTComplexJCA16WorkImpl(deliveryId, message, this, null);

        if (workInfo.isImplementsWorkContextProvider()) {

            if (workInfo.isHasSecurityContext()) {
                FVTSecurityContext sCtx = new FVTSecurityContext(workInfo);
                work.getWorkContexts().add(sCtx);
            }

            if (workInfo.isHasTransactionContext()) {
                TransactionContext tCtx = new TransactionContext();
                work.getWorkContexts().add(tCtx);
                // Need to set xid to the tCtx no matter it's null or not.
                tCtx.setXid(xid);
                if (xid != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 "xid is not null.");

                    // Add the xid to the ActiveTransSet only if the xid is
                    // valid (format id != -1)
                    if (xid.getFormatId() >= 0) {
                        if (!addActiveTransToSet(xid)) {
                            if (tc.isEntryEnabled())
                                Tr.debug(tc, "deliverComplexJCA16Message",
                                         "Duplicate xid!");
                        }
                    }

                    // If the xid is not null, then the messageProvider is
                    // sending messages
                    // to the RA with imported transaction. Then reset the
                    // EISTimer.
                    // Since the RA is checking the status of the
                    // messageProvider for the
                    // sake of rolling back active trans and complete indoubt
                    // trans,
                    // there is no need to reset the EISTimer if xid is null.
                    eisTimer.resetTimeLeft();
                }

            }
        } else {
            // Initiate an executionContext
            ec = new ExecutionContext();

            // Need to set xid to the ec no matter it's null or not.
            ec.setXid(xid);

            if (xid != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "deliverComplexMessage", "xid is not null.");

                // ec.setXid(xid);
                // Add the xid to the SetActiveTrans
                // Add the xid to the ActiveTransSet only if the xid is valid
                // (format id != -1)
                if (xid.getFormatId() >= 0) {
                    if (!addActiveTransToSet(xid)) {
                        if (tc.isEntryEnabled())
                            Tr.debug(tc, "deliverComplexMessage",
                                     "Duplicate xid!");
                        // Do not throw the Duplicate xid exception
                        // throw new ResourceException("Duplicate xid!");
                    }
                }

                // If the xid is not null, then the messageProvider is sending
                // messages
                // to the RA with imported transaction. Then reset the EISTimer.
                // Since the RA is checking the status of the messageProvider
                // for the
                // sake of rolling back active trans and complete indoubt trans,
                // there is no need to reset the EISTimer if xid is null.
                eisTimer.resetTimeLeft();
            }
        }
        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise,
        // return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexJCA16Message", "no work");
            }

            // Don't use work manager, directly call run() method.
            try {
                work.run();
            } catch (RuntimeException t) {
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexJCA16Message", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 t1.getCause());
                    }
                }

                throw t;
            }
        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexJCA16Message", "do work");
            }

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 work.getName() + " : isWorkCompleted -> true");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 work.getName() + " : isWorkCompleted -> false");
                    throw new WorkException("ComplexWork - " + work.getName()
                                            + " : is not in completed state after doWork.");
                }
            } catch (WorkException t) {
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexJCA16Message", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 t1.getCause());
                    }
                }

                throw t;
            }
        } else if (doWorkType == FVTMessageProvider.START_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexJCA16Message", "start work");
            }

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
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexJCA16Message", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 t1.getCause());
                    }
                }
                throw we;
            }

            try {
                synchronized (syncObj) {
                    // coming here, the work might have already been in the
                    // state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexJCA16Message",
                                     AdapterUtil.getWorkStatusString(state)
                                                                       + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(
                                                             tc,
                                                             "deliverComplexJCA16Message",
                                                             "StartWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        });
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexJCA16Message",
                                     "wait for notifications");
                        syncObj.wait();
                    }
                }

            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverComplexJCA16Message",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexJCA16Message", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverComplexJCA16Message", ie);
            }

            if (timedout) {
                timedout = false;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexJCA16Message", "Exception");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // This else statements get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }

        } else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverComplexJCA16Message", "schedule work");
            }

            // Define the local variable for syncObject
            Object syncObj = new Object();

            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager
                                .scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) {
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverComplexJCA16Message", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverComplexJCA16Message",
                                 t1.getCause());
                    }
                }

                throw we;
            }

            try {
                synchronized (syncObj) {
                    // coming here, the work might have already been in the
                    // state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexJCA16Message",
                                     AdapterUtil.getWorkStatusString(state)
                                                                       + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(
                                                             tc,
                                                             "deliverComplexJCA16Message",
                                                             "ScheduleWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        });
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverComplexJCA16Message",
                                     "wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverComplexJCA16Message",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexJCA16Message", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverComplexJCA16Message", ie);
            }

            if (timedout) {
                timedout = false;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverComplexJCA16Message", "Exception");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deliverComplexJCA16Message", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :"
                                           + doWorkType);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deliverComplexJCA16Message");

    }

    /**
     * <p>
     * Deliver a simple message using doWork, scheduleWork, startWork, or direct
     * call without work instance involved. If the doWorkType is DO_WORK, the
     * method blocks until the message delivery is completed. If the doWorkType
     * is SCHEDULE_WORK or START_WORK, the method blocks until the work instance
     * has reached the specified state.
     * </p>
     *
     * <p>
     * This method is called by scheduleMessageDelivery or doMessageDelivery
     * methods in the FVTMessageProviderImpl class.
     * </p>
     *
     * <p>
     * The work instace can be in the following four state:
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
     * @param endpintName
     *            the name of the endpoint application
     * @param message
     *            the message going to be sent
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     * @param waitTime
     *            This parameter specifies the maximum time for waiting the work
     *            to reach the specified state. Once the waitTime is reached,
     *            the method will return even if the work instance hasn't
     *            reached the specified state yet.
     * @param xaResource
     *            the XAResource object used for transaction notification. Set
     *            to null if you don't want get transaction notification.
     * @param doWorkType
     *            how to submit the work the work manager. It could be either
     *            DO_WORK or SCHEDULE_WORK
     * @param xid
     *            the XID which represents the imported transaction
     *
     * @exception ResourceException
     */
    public void deliverSimpleMessage(String deliveryId, String endpointName,
                                     String message, int state, int waitTime, XAResource xaResource,
                                     int doWorkType, Xid xid) throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "deliverSimpleMessage",
                     new Object[] { this, deliveryId, endpointName, message,
                                    AdapterUtil.getWorkStatusString(state),
                                    new Integer(waitTime), xaResource,
                                    AdapterUtil.getWorkTypeString(doWorkType), xid });

        // d177221 begin: 
        // Check the status of the messageProvider before delivering messages

        if (adapter.getProvider().getEISStatus() == FVTMessageProviderImpl.STATUS_FAIL) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverSimpleMessage",
                         "MessageProvider fails. Should not send messages.");
            }
            throw new ResourceException("deliverSimpleMessage: MessageProvider fails. Should not send messages.");
        }

        FVTXAResourceImpl resource = null;

        try {
            resource = (FVTXAResourceImpl) xaResource;
        } catch (ClassCastException cce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "deliverSimpleMessage", cce);
            throw cce;
        }

        // From beanName, get the MessageEndpointFactoryWrapper instance.
        MessageEndpointFactoryWrapper factoryWrapper = getMessageFactory(endpointName);

        if (factoryWrapper == null) {
            // Cannot find the bean, throw an exception
            ResourceException re = new ResourceException("Cannot find the endpoint factory with name "
                                                         + endpointName);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "resource exception", re);
            throw re;
        }

        // Initiate a work instance
        FVTSimpleWorkImpl work = new FVTSimpleWorkImpl(deliveryId, endpointName, factoryWrapper, resource);

        work.setMessage(new TextMessageImpl(message));

        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise,
        // return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        // Initiate a executionContext
        ExecutionContext ec = new ExecutionContext();

        // 12/03/03: 
        // Need to set xid to the ec no matter it's null or not.
        ec.setXid(xid);

        if (xid != null) {
            // 12/03/03: 
            // ec.setXid(xid);

            // 12/03/03: 
            // Add the xid to the ActiveTransSet only if the xid is valid
            // (format id != -1)
            if (xid.getFormatId() >= 0) {
                if (!addActiveTransToSet(xid)) {
                    if (tc.isEntryEnabled())
                        Tr.debug(tc, "deliverSimpleMessage", "Duplicate xid!");
                    // 12/17/03: 
                    // Do not throw Duplicate xid exception
                    // throw new ResourceException("Duplicate xid!");
                }
            }

            // d177221: 
            // If the xid is not null, then the messageProvider is sending
            // messages
            // to the RA with imported transaction. Then reset the EISTimer.
            // Since the RA is checking the status of the messageProvider for
            // the
            // sake of rolling back active trans and complete indoubt trans,
            // there is no need to reset the EISTimer if xid is null.
            eisTimer.resetTimeLeft();
        }

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "deliverSimpleMessage", "no work");

            // Directly call run() method.
            try {
                work.run();
            } catch (RuntimeException t) { // d174592 - log the exception in
                                           // trace
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverSimpleMessage", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverSimpleMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverSimpleMessage", t1.getCause());
                    }
                }

                throw t;
            }

        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "deliverSimpleMessage", "do work");

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverSimpleMessage", work.getName()
                                                             + " : isWorkCompleted -> true");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverSimpleMessage", work.getName()
                                                             + " : isWorkCompleted -> false");
                    throw new WorkException("SimpleWork - " + work.getName()
                                            + " : is not in completed state after doWork.");
                }
            } catch (WorkException t) { // d174592 - log the exception in trace
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverSimpleMessage", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverSimpleMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverSimpleMessage", t1.getCause());
                    }
                }

                throw t;
            }

        } else if (doWorkType == FVTMessageProvider.START_WORK) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "deliverSimpleMessage", "start work");

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.startWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverSimpleMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverSimpleMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverSimpleMessage", t1.getCause());
                    }
                }

                throw we;
            }

            try {
                synchronized (syncObj) {

                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverSimpleMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                 + " has been reached");
                    } else {
                        // start the timer
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTSimpleWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(tc,
                                                             "deliverSimpleMessage",
                                                             "StartWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended

                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverSimpleMessage",
                                     "wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverSimpleMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverSimpleMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverSimpleMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverSimpleMessage", "Exception");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // d174256: 
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        }

        else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "deliverSimpleMessage", "schedule work");

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager
                                .scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverSimpleMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverSimpleMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverSimpleMessage", t1.getCause());
                    }
                }

                throw we;
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverSimpleMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                 + " has been reached");
                    } else {
                        // start the timer
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTSimpleWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(tc,
                                                             "deliverSimpleMessage",
                                                             "ScheduleWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended

                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverSimpleMessage",
                                     "wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverSimpleMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverSimpleMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverSimpleMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverSimpleMessage", "Exception");
                throw new MessageWaitTimeoutException("sendMessageWait has been timed out.");
            } else {
                // d174256: 
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deliverSimpleMessage", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :"
                                           + doWorkType);
        }

        /*
         * 12/03/03: Need to add xid to active trans set before work is
         * submitted. The reason is even if the work is failed, the transaction
         * is still in active state.
         *
         * // d177221: 09/30/03 // Add the xid to the SetActiveTrans if
         * (xid != null) { if(!addActiveTransToSet(xid)) { if
         * (tc.isEntryEnabled()) Tr.exit(tc, "deliverComplexMessage",
         * "Duplicate xid!"); throw new ResourceException("Duplicate xid!"); } }
         */

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deliverSimpleMessage");
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
    public void deliverNestedWorkMessage(String deliveryId, FVTMessage message,
                                         int state, int waitTime, int doWorkType, int nestedDoWorkType,
                                         Xid parentXid, Xid childXid) throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "deliverNestedWorkMessage",
                     new Object[] { this, deliveryId, message,
                                    AdapterUtil.getWorkStatusString(state),
                                    new Integer(waitTime),
                                    AdapterUtil.getWorkTypeString(doWorkType),
                                    parentXid });

        // Initiate a work instance
        FVTNestedWorkImpl work = new FVTNestedWorkImpl(deliveryId, message, this, nestedDoWorkType, waitTime, state, childXid);

        // Initiate an executionContext
        ExecutionContext ec = new ExecutionContext();

        // 12/03/03: 
        // Need to set xid to the ec no matter it's null or not.
        ec.setXid(parentXid);

        if (parentXid != null) {
            // 12/03/03: 
            // ec.setXid(parentXid);

            // 11/27/03: 
            // Add the parentXid and childXid to the SetActiveTrans only if the
            // work is executed
            // successfully. (according to Cathy).
            // Do not rethrow ResourceException since the test case may supply
            // the same Xid for parent and child work (exception flow)
            // Indicate in trace is enough. The reason is if
            // deliverNestedWorkMessage
            // may send message with same Xid for parent and child for
            // exceptional flow scenarios.
            // Then it's no appropriate to throw ResourceException
            // 12/03/03: 
            // Add the xid to the ActiveTransSet only if the xid is valid
            // (format id != -1)
            if (parentXid.getFormatId() >= 0) {
                try {
                    if (!addActiveTransToSet(parentXid)) {
                        if (tc.isEntryEnabled())
                            Tr.debug(
                                     tc,
                                     "deliverNestedWorkMessage",
                                     "Duplicate parentXid with global trans id: "
                                                                 + parentXid
                                                                                 .getGlobalTransactionId()[0]);
                    }
                } catch (ResourceException re) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "deliverNestedWorkMessage",
                                "Unexpected ResourceException. Rethrow it.");
                    throw new WorkException(re);
                }
            }

            // d177221: 09/18/03
            // If the xid is not null, then the messageProvider is sending
            // messages
            // to the RA with imported transaction. Then reset the EISTimer.
            // Since the RA is checking the status of the messageProvider for
            // the
            // sake of rolling back active trans and complete indoubt trans,
            // there is no need to reset the EISTimer if xid is null.
            eisTimer.resetTimeLeft();
        }

        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise,
        // return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedWorkMessage", "no work");
            }
        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedWorkMessage", "do work");
            }

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverNestedWorkMessage", work.getName()
                                                                 + " : isWorkCompleted -> true");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverNestedWorkMessage", work.getName()
                                                                 + " : isWorkCompleted -> false");
                    throw new WorkException("Nested Work Parent - "
                                            + work.getName()
                                            + " : is not in completed state after doWork.");
                }

            } catch (WorkException t) {
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverNestedWorkMessage", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverNestedWorkMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverNestedWorkMessage", t1.getCause());
                    }
                }

                throw t;
            } catch (WorkRuntimeException rte) {
                System.out
                                .println(" *WorkRuntimeException from parent nested work - "
                                         + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - "
                                        + work.getName() + " : " + rte.toString());
            }

        }

        else if (doWorkType == FVTMessageProvider.START_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedWorkMessage", "start work");
            }

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager.startWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) {
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverNestedWorkMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverNestedWorkMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverNestedWorkMessage", t1.getCause());
                    }
                }

                throw we;
            } catch (WorkRuntimeException rte) {
                System.out
                                .println(" *WorkRuntimeException from parent nested work - "
                                         + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - "
                                        + work.getName() + " : " + rte.toString());
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedWorkMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                     + " has been reached");
                    } else {
                        // start the timer
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTNestedWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(tc,
                                                             "deliverNestedWorkMessage",
                                                             "StartWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedWorkMessage",
                                     work.getName() + " wait for notifications");
                        syncObj.wait();
                    }
                }

            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverNestedWorkMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverNestedWorkMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverNestedWorkMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "deliverNestedWorkMessage", work.getName()
                                                             + " has not reached the desire state: " + state);
                throw new MessageWaitTimeoutException("Nested Work Parent submission has been timed out. Desire state: "
                                                      + state + " has not reached.");
            } else {
                // d174256: 
                // This else statements get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }

            /*
             * 11/26/03: No need to check if the work is completed or not.
             * Coz we need to check if the NestedWork reached certain state or
             * not.
             *
             * FVTWorkImpl w = this.getWork(work.getName());
             *
             * if (w.isWorkCompleted()) { if (tc.isDebugEnabled()) Tr.debug(tc,
             * "deliverNestedWorkMessage", work.getName() +
             * " : isWorkCompleted -> true"); } else { if (tc.isDebugEnabled())
             * Tr.debug(tc, "deliverNestedWorkMessage", work.getName() +
             * " : isWorkCompleted -> false"); throw new
             * WorkException("Nested Work Parent - " + work.getName() +
             * " : is not in completed state after doWork."); }
             */

        }

        else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedWorkMessage", "schedule work");
            }

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager
                                .scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) { // d174592 - log the exception in trace
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverNestedWorkMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverNestedWorkMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverNestedWorkMessage", t1.getCause());
                    }
                }

                throw we;
            } catch (WorkRuntimeException rte) {
                System.out
                                .println(" *WorkRuntimeException from parent nested work - "
                                         + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - "
                                        + work.getName() + " : " + rte.toString());
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedWorkMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                     + " has been reached");
                    } else {
                        // start the timer
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTNestedWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(tc,
                                                             "deliverNestedWorkMessage",
                                                             "ScheduleWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedWorkMessage",
                                     work.getName() + " wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverNestedWorkMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverNestedWorkMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverNestedWorkMessage", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "deliverNestedWorkMessage", work.getName()
                                                             + " has not reached the desire state: " + state);
                throw new MessageWaitTimeoutException("Nested Work Parent submission has been timed out. Desire state: "
                                                      + state + " has not reached.");
            } else {
                // d174256: 
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }

            /*
             * 11/26/03: No need to check if the work is completed or not.
             * Coz we need to check if the NestedWork reached certain state or
             * not. FVTWorkImpl w = this.getWork(work.getName());
             *
             * if (w.isWorkCompleted()) { if (tc.isDebugEnabled()) Tr.debug(tc,
             * "deliverNestedWorkMessage", work.getName() +
             * " : isWorkCompleted -> true"); } else { if (tc.isDebugEnabled())
             * Tr.debug(tc, "deliverNestedWorkMessage", work.getName() +
             * " : isWorkCompleted -> false"); throw new
             * WorkException("Nested Work Parent - " + work.getName() +
             * " : is not in completed state after doWork."); }
             */
        }

        else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deliverNestedWorkMessage", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :"
                                           + doWorkType);
        }

        /*
         * 12/03/03: Need to add xid to active trans set before work is
         * submitted. The reason is even if the work is failed, the transaction
         * is still in active state.
         *
         * // 11/27/03: // Add the parentXid and childXid to the
         * SetActiveTrans only if the work is executed // successfully.
         * (according to Cathy). // Do not rethrow ResourceException since the
         * test case may supply // the same Xid for parent and child work
         * (exception flow) // Indicate in trace is enough. The reason is if
         * deliverNestedWorkMessage // may send message with same Xid for parent
         * and child for exceptional flow scenarios. // Then it's no appropriate
         * to throw ResourceException if (parentXid != null) { try {
         * if(!addActiveTransToSet(parentXid)) { if (tc.isEntryEnabled())
         * Tr.exit(tc, "deliverNestedWorkMessage",
         * "Duplicate parentXid with global trans id: " +
         * parentXid.getGlobalTransactionId()[0]); } } catch (ResourceException
         * re) { if (tc.isEntryEnabled()) Tr.exit(tc,
         * "deliverNestedWorkMessage",
         * "Unexpected ResourceException. Rethrow it."); throw new
         * WorkException(re); } }
         *
         *
         *
         * if (childXid != null) { try { if(!addActiveTransToSet(childXid)) { if
         * (tc.isEntryEnabled()) Tr.exit(tc, "deliverNestedWorkMessage",
         * "Duplicate childXid with global trans id: " +
         * childXid.getGlobalTransactionId()[0]); } } catch (ResourceException
         * re) { if (tc.isEntryEnabled()) Tr.exit(tc,
         * "deliverNestedWorkMessage",
         * "Unexpected ResourceException. Rethrow it."); throw new
         * WorkException(re); } }
         */

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deliverNestedWorkMessage");

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
     * This method is called by sendJCA16MessageWaitNestedWork method in the
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
     *            deliveryId the ID related to this message delivery. You can
     *            use this message delivery ID to retrieve delivery-related
     *            information, such as endpoint instances
     * @param message
     *            an FVTMessage object
     * @param workCtxs
     *            The workContexts that are provided by the parent work
     * @param childWorkCtxs
     *            The workContexts that are provided by the child work
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
     * @param parentXid
     *            the XID which represents the imported transaction for the
     *            parent work
     * @param childXid
     *            the XID which represents the imported transaction for the
     *            child work
     *
     * @throws ResourceException
     */
    public void deliverNestedJCA16WorkMessage(String deliveryId,
                                              FVTMessage message, WorkInformation workInfo, int state,
                                              int waitTime, int doWorkType, int nestedDoWorkType, Xid parentXid,
                                              Xid childXid) throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "deliverNestedJCA16WorkMessage",
                     new Object[] { this, deliveryId, message, workInfo,
                                    AdapterUtil.getWorkStatusString(state),
                                    new Integer(waitTime),
                                    AdapterUtil.getWorkTypeString(doWorkType),
                                    parentXid, childXid });

        if (workInfo == null) {
            // no work information so call the method for jca15
            deliverNestedWorkMessage(deliveryId, message, state, waitTime,
                                     doWorkType, nestedDoWorkType, parentXid, childXid);
            return;
        }
        // Initiate a work instance
        WorkInformation nestedWorkInfo = workInfo.getNestedWorkInformation();

        if (nestedWorkInfo == null) {
            nestedWorkInfo = new WorkInformation();
            nestedWorkInfo.setHasSecurityContext(false);
            nestedWorkInfo.setHasTransactionContext(false);
            nestedWorkInfo.setImplementsWorkContextProvider(false);
        }
        int workSpecLevel = nestedWorkInfo.isImplementsWorkContextProvider() ? 1 : 0;
        List<WorkContext> childWorkContexts = new ArrayList<WorkContext>();
        FVTComplexWorkImpl work = new FVTNestedJCA16WorkImpl(deliveryId, message, this, new ArrayList<WorkContext>(), childWorkContexts, nestedDoWorkType, waitTime, state, workSpecLevel, childXid);
        ExecutionContext ec = null;
        if (workInfo.isImplementsWorkContextProvider()) {
            if (workInfo.isHasSecurityContext()) {
                FVTSecurityContext sCtx = new FVTSecurityContext(workInfo);
                ((FVTNestedJCA16WorkImpl) work).getWorkContexts().add(sCtx);
            }
            if (workInfo.isHasTransactionContext()) {
                TransactionContext tCtx = new TransactionContext();
                ((FVTNestedJCA16WorkImpl) work).getWorkContexts().add(tCtx);
                tCtx.setXid(parentXid);
                if (parentXid != null) {
                    if (parentXid.getFormatId() >= 0) {
                        try {
                            if (!addActiveTransToSet(parentXid)) {
                                if (tc.isEntryEnabled())
                                    Tr.debug(
                                             tc,
                                             "deliverNestedJCA16WorkMessage",
                                             "Duplicate parentXid with global trans id: "
                                                                              + parentXid
                                                                                              .getGlobalTransactionId()[0]);
                            }
                        } catch (ResourceException re) {
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "deliverNestedJCA16WorkMessage",
                                        "Unexpected ResourceException. Rethrow it.");
                            throw new WorkException(re);
                        }
                    }
                    eisTimer.resetTimeLeft();
                }
            }
        } else {
            // use the old JCA 15 nested Work Flow. This does not support a
            // child that is JCA 1.6.
            // Will need to modify this if we need to support JCA 1.6 children
            // for JCA 1.5 parents.
            work = new FVTNestedWorkImpl(deliveryId, message, this, nestedDoWorkType, waitTime, state, childXid);

            // Initiate an executionContext
            ec = new ExecutionContext();
            ec.setXid(parentXid);
            if (parentXid != null) {
                if (parentXid.getFormatId() >= 0) {
                    try {
                        if (!addActiveTransToSet(parentXid)) {
                            if (tc.isEntryEnabled())
                                Tr.debug(
                                         tc,
                                         "deliverNestedWorkMessage",
                                         "Duplicate parentXid with global trans id: "
                                                                     + parentXid
                                                                                     .getGlobalTransactionId()[0]);
                        }
                    } catch (ResourceException re) {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "deliverNestedWorkMessage",
                                    "Unexpected ResourceException. Rethrow it.");
                        throw new WorkException(re);
                    }
                }
                eisTimer.resetTimeLeft();
            }
        }
        // Add work to the works HashMap.
        // work.getName() returns deliveryId if it is not null; otherwise,
        // return the
        // hashcode of the work instance.
        works.put(work.getName(), work);

        if (nestedWorkInfo.isImplementsWorkContextProvider()) {
            if (nestedWorkInfo.isHasSecurityContext()) {
                FVTSecurityContext sCtx = new FVTSecurityContext(nestedWorkInfo);
                childWorkContexts.add(sCtx);
            }
            if (nestedWorkInfo.isHasTransactionContext()) {
                TransactionContext tCtx = new TransactionContext();
                childWorkContexts.add(tCtx);
                tCtx.setXid(childXid);
                if (childXid != null) {
                    if (childXid.getFormatId() >= 0) {
                        try {
                            if (!addActiveTransToSet(childXid)) {
                                if (tc.isEntryEnabled())
                                    Tr.debug(
                                             tc,
                                             "deliverNestedJCA16WorkMessage",
                                             "Duplicate childXid with global trans id: "
                                                                              + childXid
                                                                                              .getGlobalTransactionId()[0]);
                            }
                        } catch (ResourceException re) {
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "deliverNestedJCA16WorkMessage",
                                        "Unexpected ResourceException. Rethrow it.");
                            throw new WorkException(re);
                        }
                    }
                    eisTimer.resetTimeLeft();
                }
            }
        }
        if (doWorkType == FVTMessageProvider.NO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedJCA16WorkMessage", "no work");
            }
        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedJCA16WorkMessage", "do work");
            }

            try {
                workManager.doWork(work, WorkManager.INDEFINITE, ec, this);

                FVTWorkImpl w = this.getWork(work.getName());

                if (w.isWorkCompleted()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 work.getName() + " : isWorkCompleted -> true");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 work.getName() + " : isWorkCompleted -> false");
                    throw new WorkException("Nested Work Parent - "
                                            + work.getName()
                                            + " : is not in completed state after doWork.");
                }

            } catch (WorkException t) {
                Throwable t1 = t;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverNestedJCA16WorkMessage", t);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 t1.getCause());
                    }
                }

                throw t;
            } catch (WorkRuntimeException rte) {
                System.out
                                .println(" *WorkRuntimeException from parent nested work - "
                                         + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - "
                                        + work.getName() + " : " + rte.toString());
            }

        }

        else if (doWorkType == FVTMessageProvider.START_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedJCA16WorkMessage", "start work");
            }

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
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverNestedJCA16WorkMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 t1.getCause());
                    }
                }

                throw we;
            } catch (WorkRuntimeException rte) {
                System.out
                                .println(" *WorkRuntimeException from parent nested work - "
                                         + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - "
                                        + work.getName() + " : " + rte.toString());
            }

            try {
                synchronized (syncObj) {
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                          + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(
                                                             tc,
                                                             "deliverNestedJCA16WorkMessage",
                                                             "StartWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        });
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                     work.getName() + " wait for notifications");
                        syncObj.wait();
                    }
                }

            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverNestedJCA16WorkMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverNestedJCA16WorkMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverNestedJCA16WorkMessage", ie);
            }

            if (timedout) {
                timedout = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                             work.getName()
                                                                  + " has not reached the desire state: "
                                                                  + state);
                throw new MessageWaitTimeoutException("Nested Work Parent submission has been timed out. Desire state: "
                                                      + state + " has not reached.");
            } else {

                // This else statements get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }
        }

        else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "deliverNestedJCA16WorkMessage", "schedule work");
            }
            // Define the local variable for syncObject
            Object syncObj = new Object();
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            work.setNotificationState(state);
            work.setSyncObj(syncObj);

            try {
                workManager
                                .scheduleWork(work, WorkManager.INDEFINITE, ec, this);
            } catch (WorkException we) {
                Throwable t1 = we;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deliverNestedJCA16WorkMessage", we);
                    while ((t1 = t1.getCause()) != null) {
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 "------chained exception------");
                        Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                 t1.getCause());
                    }
                }

                throw we;
            } catch (WorkRuntimeException rte) {
                System.out
                                .println(" *WorkRuntimeException from parent nested work - "
                                         + work.getName() + " : " + rte.toString());
                throw new WorkException(" *WorkRuntimeException from parent nested work - "
                                        + work.getName() + " : " + rte.toString());
            }

            try {
                synchronized (syncObj) {
                    // coming here, the work might have already been in the
                    // state.
                    // check the state before start the timer
                    if (work.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                     AdapterUtil.getWorkStatusString(state)
                                                                          + " has been reached");
                    } else {
                        // start the timer
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, work,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = work;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(
                                                             tc,
                                                             "deliverNestedJCA16WorkMessage",
                                                             "ScheduleWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        });
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                                     work.getName() + " wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "InterruptedException is caught in deliverNestedJCA16WorkMessage",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverNestedJCA16WorkMessage", "Exception");
                throw new ResourceException("InterupttedException is caught in deliverNestedJCA16WorkMessage", ie);
            }

            if (timedout) {
                timedout = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "deliverNestedJCA16WorkMessage",
                             work.getName()
                                                                  + " has not reached the desire state: "
                                                                  + state);
                throw new MessageWaitTimeoutException("Nested Work Parent submission has been timed out. Desire state: "
                                                      + state + " has not reached.");
            } else {
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (work.getWorkException() != null) {
                    throw work.getWorkException();
                }
            }

        }

        else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "deliverNestedJCA16WorkMessage", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :"
                                           + doWorkType);
        }

        /*
         * Need to add xid to active trans set before work is submitted. The
         * reason is even if the work is failed, the transaction is still in
         * active state.
         *
         * // Add the parentXid and childXid to the SetActiveTrans only if the
         * work is executed // successfully. (according to Cathy). // Do not
         * rethrow ResourceException since the test case may supply // the same
         * Xid for parent and child work (exception flow) // Indicate in trace
         * is enough. The reason is if deliverNestedJCA16WorkMessage // may send
         * message with same Xid for parent and child for exceptional flow
         * scenarios. // Then it's no appropriate to throw ResourceException
         */

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deliverNestedJCA16WorkMessage");

    }

    /**
     * <p>
     * Add work to the works HashMap.
     * </p>
     * <p>
     * It is created for the delivery of nested work. The child of the nested
     * work needs to call this method in order to check the work state of the
     * child nested work.
     * </p>
     *
     * @param workName
     *            name of the work
     * @param work
     *            work to add
     */
    public void addWork(Object workName, Object work) {
        works.put(workName, work);
    }

    /**
     * <p>
     * This method returns the work instance associated with the message
     * delivery id.
     * </p>
     *
     * <p>
     * After getting the work instance, the test application can call methods
     * isWorkAccepted()/isWorkStarted()/isWorkCompleted/isWorkRejected of the
     * work instance to test whether the work has been
     * accpeted/started/completed/rejected .
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. This message delivery
     *            ID is used to retrieve the work instance.
     *
     * @return an FVT work object.
     */
    public FVTWorkImpl getWork(String deliveryId) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getWork", new Object[] { this, deliveryId });

        FVTWorkImpl work = (FVTWorkImpl) (works.get(deliveryId));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getWork", work);

        return work;
    }

    /**
     * <p>
     * This method returns a Hashtable of endpoint wrapper instances associated
     * with the message delivery id.
     * </p>
     *
     * <p>
     * User can get the message endpoint test results from the endpoints for
     * test verification.
     * </p>
     *
     * <p>
     * Users are not allowed to modify the Hashtable returned from this method.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery. This message delivery
     *            is used to retrieve the endpoint wrapper instances.
     *
     * @return a Hashtable of endpoint instances used by this work intance. The
     *         hashkey to get the work instance is endpointName + instanceID.
     */
    public Hashtable getEndpoints(String deliveryId) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getEndpoints", new Object[] { this, deliveryId });

        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) works.get(deliveryId);

        if (work == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getEndpoints", "Cannot find work, return null");
            return null;
        } else {
            Hashtable instances = work.getInstances();

            String firstKey = work.getFirstInstanceKey();

            if (instances == null) {
                if (firstKey == null) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getEndpoints",
                                "Cannot find endpoints, return null");
                    return null;
                }

                instances = new Hashtable(1);

                // Add the first instance to the hash table
                instances.put(firstKey, work.getInstance());
            } else {
                // Add the first instance to the hash table
                instances.put(firstKey, work.getInstance());
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getEndpoints", instances);
            return instances;
        }
    }

    // d174256: Added for Work that doesn't send through sendMessagexxx
    // can still get the endpoints to cleanup the test results.

    /**
     * <p>
     * This method returns a Hashtable of endpoint wrapper instances associated
     * with the Work instance.
     * </p>
     *
     * <p>
     * User can get the message endpoint test results from the endpoints for
     * test verification.
     * </p>
     *
     * <p>
     * Users are not allowed to modify the Hashtable returned from this method.
     * </p>
     *
     * @param inputWork
     *            this work instance is used to retrieve the endpoint wrapper
     *            instances if the Work is submitted by user instead of by
     *            sendMessagexxx. It should be created through
     *            FVTMessageProvider.createWork().
     *
     * @return a Hashtable of endpoint instances used by this work intance. The
     *         hashkey to get the work instance is endpointName + instanceID.
     */
    public Hashtable getEndpoints(FVTGeneralWorkImpl inputWork) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getEndpoints", new Object[] { this, inputWork });

        if (inputWork == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getEndpoints", "Cannot find work, return null");
            return null;
        } else {
            Hashtable instances = inputWork.getInstances();

            String firstKey = inputWork.getFirstInstanceKey();

            if (instances == null) {
                if (firstKey == null) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getEndpoints",
                                "Cannot find endpoints, return null");
                    return null;
                }

                instances = new Hashtable(1);

                // Add the first instance to the hash table
                instances.put(firstKey, inputWork.getInstance());
            } else {
                // Add the first instance to the hash table
                instances.put(firstKey, inputWork.getInstance());
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getEndpoints", instances);
            return instances;
        }

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
    public MessageEndpointTestResults[] getTestResults(String deliveryId,
                                                       String endpointName, int instanceId) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTestResults", new Object[] { this, deliveryId,
                                                          endpointName, new Integer(instanceId) });

        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) works.get(deliveryId);

        if (work == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getTestResults",
                        "Cannot find the work, return null");
            return null;
        } else {

            // get the work instance object
            MessageEndpointWrapper endpointWrapper = work
                            .getEndpointWrapper(endpointName + instanceId);

            if (endpointWrapper == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResults",
                            "Cannot find the instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper
                                .getTestResults();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResults", testResults);
                return testResults;
            }

        }
    }

    /**
     * <p>
     * This method returns the test result asscociated with the message delviery
     * id.
     * </p>
     *
     * <p>
     * This method can only be used when there is only one test result added in
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
    public MessageEndpointTestResults getTestResult(String deliveryId) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTestResults", new Object[] { this, deliveryId });

        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) works.get(deliveryId);

        if (work == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getTestResult", "Canot find the work, return null");
            return null;
        } else {
            // get the work hashtable
            MessageEndpointWrapper endpointWrapper = work.getInstance();

            if (endpointWrapper == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResult",
                            "Cannot find any instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper
                                .getTestResults();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResult", testResults[0]);
                return testResults[0];
            }

        }
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
    public MessageEndpointTestResults[] getTestResults(Work inputWork,
                                                       String endpointName, int instanceId) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTestResults", new Object[] { this, inputWork,
                                                          endpointName, new Integer(instanceId) });

        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) inputWork;

        if (work == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getTestResults",
                        "Cannot find the work, return null");
            return null;
        } else {

            // get the work instance object
            MessageEndpointWrapper endpointWrapper = work
                            .getEndpointWrapper(endpointName + instanceId);

            if (endpointWrapper == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResults",
                            "Cannot find the instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper
                                .getTestResults();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResults", testResults);
                return testResults;
            }

        }
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
     * This method can only be used when there is only one test result added in
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
    public MessageEndpointTestResults getTestResult(Work inputWork) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTestResults", new Object[] { this, inputWork });

        // get the work instance.
        FVTGeneralWorkImpl work = (FVTGeneralWorkImpl) inputWork;

        if (work == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getTestResult", "Canot find the work, return null");
            return null;
        } else {
            // get the work hashtable
            MessageEndpointWrapper endpointWrapper = work.getInstance();

            if (endpointWrapper == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResult",
                            "Cannot find any instance, return null");
                return null;
            } else {
                MessageEndpointTestResults[] testResults = endpointWrapper
                                .getTestResults();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getTestResult", testResults[0]);
                return testResults[0];
            }

        }
    }

    /**
     * <p>
     * This method hints the testing resource adapter to release all the
     * information related to this message delivery ID. TRA will unset the test
     * results of the endpoint instance, recycle the work object, and remove the
     * message delivery ID from the Hashtable. After calling this method, users
     * cannot get any endpoint information from this message delivery ID any
     * more.
     * </p>
     *
     * @param deliveryId
     *            the ID related to this message delivery.
     */
    public void releaseDeliveryId(String deliveryId) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "releaseDeliveryId", new Object[] { this, deliveryId });

        // get all the instances including the first one.
        Hashtable instances = getEndpoints(deliveryId);

        // release all the instances
        if (instances != null) {
            Collection endpoints = instances.entrySet();
            for (Iterator iter = endpoints.iterator(); iter.hasNext();) {
                java.util.Map.Entry map = (java.util.Map.Entry) iter.next();
                MessageEndpointWrapper endpoint = (MessageEndpointWrapper) map
                                .getValue();

                // release and unset the test result
                // d174592 - eat exception from endpoint.release
                try {
                    endpoint.release();
                } catch (Throwable t) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "releaseDeliveryId", t);
                        while ((t = t.getCause()) != null) {
                            Tr.debug(tc, "releaseDeliveryId",
                                     "------chained exception------");
                            Tr.debug(tc, "releaseDeliveryId", t.getCause());
                        }
                    }
                }
            }
        }

        // remove the work object from the hashmap
        works.remove(deliveryId);
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
    public void releaseWork(FVTGeneralWorkImpl inputWork) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "releaseWork", new Object[] { this, inputWork });

        // get all the instances including the first one.
        Hashtable instances = getEndpoints(inputWork);

        // release all the instances
        if (instances != null) {
            Collection endpoints = instances.entrySet();
            for (Iterator iter = endpoints.iterator(); iter.hasNext();) {
                java.util.Map.Entry map = (java.util.Map.Entry) iter.next();
                MessageEndpointWrapper endpoint = (MessageEndpointWrapper) map
                                .getValue();

                // release and unset the test result// release and unset the
                // test result
                // d174592 - eat exception from endpoint.release
                try {
                    endpoint.release();
                } catch (Throwable t) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "releaseWork", t);
                        while ((t = t.getCause()) != null) {
                            Tr.debug(tc, "releaseWork",
                                     "------chained exception------");
                            Tr.debug(tc, "releaseWork", t.getCause());
                        }
                    }
                }
            }
        }
    }

    // 11/20/03: 
    // Need to add waitTime and state for executeConcurrentWork so that this
    // will
    // return if all concurrentWork instances reached the desired state.
    /**
     * <p>
     * This method call executes two or more work instances concurrently. This
     * method is mainly used to test associating two or more work instances
     * concurrently with a or more source managed transaction (EIS imported
     * transaction).
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
     * @param work
     *            an array of Work instance. Currently, we only support
     *            executing the work instances which type or super type is
     *            FVTConcurrentWorkImpl.
     * @param xids
     *            an array of XID which represents the imported transaction
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
     * @exception WorkCompletedException
     */

    public void executeConcurrentWork(Work[] works, Xid[] xids, int doWorkType,
                                      int state, int waitTime) throws ResourceException {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "executeConcurrentWork", new Object[] { this, works,
                                                                 xids, new Integer(doWorkType), new Integer(state),
                                                                 new Integer(waitTime) });

        int numOfWorks = works.length;

        // 11/17/03: 
        // I need works.length sync object for locks. The reason is I have to
        // wait
        // until all works received the correct workEvent (work state) object.
        // Each sync object corresponds to the lock for each work object.
        // Then executeConcurrentWork can be returned to the test case.

        boolean singleXid = (xids.length == 1);

        FVTConcurrentWorkImpl.setConcurrentWorkNumber(numOfWorks);

        // 11/20/03: Begin
        // Need a list of Work and EC rather than one as the Work and EC will be
        // used outside
        // this for loop when concurrentWorks are submitted below.
        FVTConcurrentWorkImpl[] work = new FVTConcurrentWorkImpl[numOfWorks];
        ExecutionContext[] ec = new ExecutionContext[numOfWorks];

        for (int i = 0; i < numOfWorks; i++) {
            // FVTConcurrentWorkImpl work = null;

            ec[i] = new ExecutionContext();

            try {
                // 11/20/03: Begin
                // Need a list of work rather than one as the work will be used
                // outside
                // this for loop when concurrentWorks are submitted below.
                work[i] = (FVTConcurrentWorkImpl) works[i];
            } catch (ClassCastException cce) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "executeConcurrentWork", cce);
                throw new WorkRuntimeException("Work instance must be a FVTConcurrentWorkImpl type", cce);
            }

            // 11/20/03: Begin
            // Need a list of EC rather than one as the EC will be used outside
            // this for loop when concurrentWorks are submitted below.
            // Initiate a executionContext
            // ExecutionContext ec = new ExecutionContext();

            if (singleXid) {
                ec[i].setXid(xids[0]);
            } else {
                ec[i].setXid(xids[i]);
            }
            // 11/20/03: End
        }

        try {
            switch (doWorkType) {
                case FVTMessageProvider.DO_WORK: {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "executeConcurrentWork",
                                 "Submit concurrent work with Do work is not allowed.");
                    }
                    throw new WorkException("Submit concurrent work with Do work is not allowed.");
                    /*
                     * 11/20/03: ConcurrentWorks can't be submitted with
                     * doWork.
                     *
                     * try { workManager.doWork(work, WorkManager.INDEFINITE, ec,
                     * this); } catch (WorkException we) { // d174592 - log the
                     * exception in trace Throwable t1 = we; if
                     * (tc.isDebugEnabled()) { Tr.debug(tc, "executeConcurrentWork",
                     * we); while ((t1 = t1.getCause()) != null) { Tr.debug(tc,
                     * "executeConcurrentWork", "------chained exception------");
                     * Tr.debug(tc, "executeConcurrentWork", t1.getCause()); } }
                     * throw we; }
                     */
                }
                case FVTMessageProvider.START_WORK: {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "executeConcurrentWork", "Start work");
                    }
                    Object syncObj = new Object();

                    // 02/17/04: 
                    // Need a final object for doPrivileged Call
                    final Object pSyncObj = syncObj;

                    work[0].setSyncObj(syncObj);

                    for (int i = 0; i < numOfWorks; i++) {
                        work[i].setNotificationState(state);

                        try {
                            workManager.startWork(work[i], WorkManager.INDEFINITE,
                                                  ec[i], this);
                        } catch (WorkException we) { // d174592 - log the exception
                                                     // in trace
                            Throwable t1 = we;
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "executeConcurrentWork", we);
                                while ((t1 = t1.getCause()) != null) {
                                    Tr.debug(tc, "executeConcurrentWork",
                                             "------chained exception------");
                                    Tr.debug(tc, "executeConcurrentWork",
                                             t1.getCause());
                                }
                            }

                            throw we;
                        }
                        // d177221: 11/16/03
                        // Add the xid to the SetActiveTrans
                        // Do not rethrow ResourceException since the test case may
                        // supply
                        // the same Xid for all concurrentWork (exception flow)
                        // Indicate in trace is enough. The reason is if
                        // executeConcurrentWork
                        // will wait until certain WorkException is received, then
                        // it's no good
                        // to rethrow ResourceException
                        if (xids[i] != null) {
                            try {
                                if (!addActiveTransToSet(xids[i])) {
                                    if (tc.isEntryEnabled())
                                        Tr.debug(
                                                 tc,
                                                 "executeConcurrentWork",
                                                 "Duplicate xid with global trans id: "
                                                                          + xids[i]
                                                                                          .getGlobalTransactionId()[0]);
                                }
                            } catch (ResourceException re) {
                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "executeConcurrentWork",
                                            "Unexpected ResourceException. Rethrow it.");
                                throw new WorkException(re);
                            }
                        }
                    }

                    try {
                        synchronized (syncObj) {
                            // 174742 - coming here, the work might have already
                            // been in the state.
                            // check the state before start the timer
                            if (work[0].isWorkHasReachedState()) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "executeConcurrentWork",
                                             AdapterUtil.getWorkStatusString(state)
                                                                          + " has been reached");
                            } else {
                                // start the timer
                                // 02/17/04: begin
                                // Need to use doPrivleged call to create a thread
                                // Thread t = new Thread(new Timer(waitTime,
                                // work[0], syncObj));
                                final int pWaitTime = waitTime;
                                final FVTConcurrentWorkImpl pWork = work[0];
                                Thread t = (Thread) java.security.AccessController
                                                .doPrivileged(new java.security.PrivilegedAction() {
                                                    @Override
                                                    public Object run() {
                                                        if (tc.isEntryEnabled())
                                                            Tr.debug(
                                                                     tc,
                                                                     "executeConcurrentWork",
                                                                     "StartWork: doPrivileged on create Thread t");
                                                        return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                    }
                                                }); // 02/17/04: ended
                                t.start();

                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "executeConcurrentWork",
                                             "wait for notifications");
                                syncObj.wait();
                            }
                        }
                    } catch (InterruptedException ie) {
                        if (tc.isDebugEnabled())
                            Tr.debug(
                                     tc,
                                     "InterruptedException is caught in executeConcurrentWork",
                                     ie);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "executeConcurrentWork", "Exception");
                        throw new ResourceException("InterupttedException is caught in executeConcurrentWork", ie);
                    }

                    if (timedout) { // d173989
                        timedout = false;
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "executeConcurrentWork",
                                     "ConcurrentWorks have not reached the desire state: "
                                                                  + state);
                        throw new MessageWaitTimeoutException("Concurrent Work submission has been timed out. Desire state: "
                                                              + state + " has not reached.");
                    } else {
                        // d174256: 
                        // This else statements get the exception object from the
                        // Work so that
                        // sendMessagexxx can throw WorkCompletedException.
                        // 11/20/03: 
                        // Will throw the workException from the last submitted work
                        // only.
                        if (work[numOfWorks - 1].getWorkException() != null) {
                            throw work[numOfWorks - 1].getWorkException();
                        }
                    }
                    break;
                }
                case FVTMessageProvider.SCHEDULE_WORK: {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "executeConcurrentWork", "Schedule work");
                    }

                    Object syncObj = new Object();

                    // 02/17/04: 
                    // Need a final object for doPrivileged Call
                    final Object pSyncObj = syncObj;

                    work[0].setSyncObj(syncObj);

                    for (int i = 0; i < numOfWorks; i++) {
                        work[i].setNotificationState(state);

                        try {
                            workManager.scheduleWork(work[i],
                                                     WorkManager.INDEFINITE, ec[i], this);
                        } catch (WorkException we) { // d174592 - log the exception
                                                     // in trace
                            Throwable t1 = we;
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "executeConcurrentWork", we);
                                while ((t1 = t1.getCause()) != null) {
                                    Tr.debug(tc, "executeConcurrentWork",
                                             "------chained exception------");
                                    Tr.debug(tc, "executeConcurrentWork",
                                             t1.getCause());
                                }
                            }

                            throw we;
                        }
                        // d177221: 11/16/03
                        // Add the xid to the SetActiveTrans
                        // Do not rethrow ResourceException since the test case may
                        // supply
                        // the same Xid for all concurrentWork (exception flow)
                        // Indicate in trace is enough. The reason is if
                        // executeConcurrentWork
                        // will wait until certain WorkException is received, then
                        // it's no good
                        // to rethrow ResourceException
                        if (xids[i] != null) {
                            try {
                                if (!addActiveTransToSet(xids[i])) {
                                    if (tc.isEntryEnabled())
                                        Tr.exit(tc,
                                                "executeConcurrentWork",
                                                "Duplicate xid with global trans id: "
                                                                         + xids[i]
                                                                                         .getGlobalTransactionId()[0]);
                                }
                            } catch (ResourceException re) {
                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "executeConcurrentWork",
                                            "Unexpected ResourceException. Rethrow it.");
                                throw new WorkException(re);
                            }
                        }
                    }

                    try {
                        synchronized (syncObj) {
                            // 174742 - coming here, the work might have already
                            // been in the state.
                            // check the state before start the timer
                            if (work[0].isWorkHasReachedState()) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "executeConcurrentWork",
                                             AdapterUtil.getWorkStatusString(state)
                                                                          + " has been reached");
                            } else {
                                // start the timer
                                // 02/17/04: begin
                                // Need to use doPrivleged call to create a thread
                                // Thread t = new Thread(new Timer(waitTime,
                                // work[0], syncObj));
                                final int pWaitTime = waitTime;
                                final FVTConcurrentWorkImpl pWork = work[0];
                                Thread t = (Thread) java.security.AccessController
                                                .doPrivileged(new java.security.PrivilegedAction() {
                                                    @Override
                                                    public Object run() {
                                                        if (tc.isEntryEnabled())
                                                            Tr.debug(
                                                                     tc,
                                                                     "executeConcurrentWork",
                                                                     "ScheduleWork: doPrivileged on create Thread t");
                                                        return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                                    }
                                                }); // 02/17/04: ended
                                t.start();

                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "executeConcurrentWork",
                                             "wait for notifications");
                                syncObj.wait();
                            }
                        }
                    } catch (InterruptedException ie) {
                        if (tc.isDebugEnabled())
                            Tr.debug(
                                     tc,
                                     "InterruptedException is caught in executeConcurrentWork",
                                     ie);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "executeConcurrentWork", "Exception");
                        throw new ResourceException("InterupttedException is caught in executeConcurrentWork", ie);
                    }

                    if (timedout) { // d173989
                        timedout = false;
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "executeConcurrentWork",
                                     "ConcurrentWorks have not reached the desire state: "
                                                                  + state);
                        throw new MessageWaitTimeoutException("Concurrent Work submission has been timed out. Desire state: "
                                                              + state + " has not reached.");
                    } else {
                        // d174256: 
                        // This else statements get the exception object from the
                        // Work so that
                        // sendMessagexxx can throw WorkCompletedException.
                        // 11/20/03: 
                        // Will throw the workException from the last submitted work
                        // only.
                        if (work[numOfWorks - 1].getWorkException() != null) {
                            throw work[numOfWorks - 1].getWorkException();
                        }
                    }

                    break;
                }
                default:
                    throw new WorkException("TRA doesn't support doWork type "
                                            + doWorkType);
            } // end switch
        } catch (WorkException we) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "executeConcurrentWork",
                        "Exception " + we.getMessage());
            throw we;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "executeConcurrentWork");

    }

    // --------------------------------------------------------------------------
    // work event notification methods.
    // --------------------------------------------------------------------------

    /**
     * Invoked when a Work instance has been accepted.
     */
    @Override
    public void workAccepted(WorkEvent event) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "workAccepted", event);

        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_ACCEPTED);
    }

    /**
     * Invoked when a Work instance has been rejected.
     */
    @Override
    public void workRejected(WorkEvent event) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "workRejected", event);

        // d174256: 
        // Add the exception object to the Work so that sendMessagexxx can
        // throw WorkRejectedException.
        // Useful for WorkRejected after work is accepted
        // Firstly, check if there is an exception or not.
        // If so, then see what exception is that and then throw it.

        if (event.getException() != null) {
            if ((event.getException()
                            .getClass()
                            .getName()
                            .equals(rejectedException))) {
                ((FVTWorkImpl) event.getWork()).setWorkException(event
                                .getException());
            }
        }

        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_REJECTED);
    }

    /**
     * Invoked when a Work instance has started execution. This only means that
     * a thread has been allocated.
     */
    @Override
    public void workStarted(WorkEvent event) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "workStarted", event);

        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_STARTED);
    }

    /**
     * Invoked when a Work instance has completed execution.
     */
    @Override
    public void workCompleted(WorkEvent event) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "workCompleted", event);

        // d174256: 
        // Add the exception object to the Work so that sendMessagexxx can
        // throw WorkCompletedException.
        // Firstly, check if there is an exception or not.
        // If so, then see what exception is that and then throw it.

        if (event.getException() != null) {
            if ((event.getException()
                            .getClass()
                            .getName()
                            .equals(completedException))) {
                ((FVTWorkImpl) event.getWork()).setWorkException(event
                                .getException());
            }
        }

        ((FVTWorkImpl) event.getWork()).setState(WorkEvent.WORK_COMPLETED);
    }

    public String introspecSelf() {
        String lineSeparator = System.getProperty("line.separator");

        StringBuffer str = new StringBuffer();
        // Add the work instance
        str.append("Work instances:" + lineSeparator);

        Collection instances = works.entrySet();
        if (works != null) {
            for (Iterator iter = instances.iterator(); iter.hasNext();) {
                java.util.Map.Entry map = (java.util.Map.Entry) iter.next();
                String iKey = (String) map.getKey();
                FVTWorkImpl work = (FVTWorkImpl) map.getValue();
                str.append(iKey + "={" + lineSeparator + work + "}"
                           + lineSeparator);
            }
        } else {
            str.append("No works" + lineSeparator);
        }

        return str.toString();

    }

    // d177221 begin: 09/24/03

    /**
     * <p>
     * This method will add the indoubt transaction to indoubt trans set
     * </p>
     * and remove the xid from the active trans set
     *
     * @param xid
     *            The xid of the imported transaction
     * @exception ResourceException
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    protected synchronized boolean addTransToSet(Xid xid, Set tranSet) throws ResourceException {

        boolean rc_add;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "addTransToSet", "Entry");

        try {
            // rc_remove = SetActiveTrans.remove(xid);
            rc_add = tranSet.add(xid);
        } catch (UnsupportedOperationException uoe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addTransToSet", uoe);
            throw uoe;
        } catch (ClassCastException cce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addTransToSet", cce);
            throw cce;
        } catch (NullPointerException npe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addTransToSet", npe);
            throw npe;
        } catch (IllegalArgumentException iae) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addTransToSet", iae);
            throw iae;
        }
        /*
         * if (!rc_remove) { ResourceException re_remove = new
         * ResourceException("Xid does not exist in Active Trans Set! ERROR!");
         * if( tc.isDebugEnabled()) Tr.debug(tc, "addIndoubtTransToSet",
         * re_remove); throw re_remove; } if (!rc_add) { ResourceException
         * re_add = new ResourceException(
         * "Xid already exists in Indoubt Trans Set. Non unique Xid is used!");
         * if( tc.isDebugEnabled()) Tr.debug(tc, "addIndoubtTransToSet",
         * re_add); throw re_add; }
         */
        return rc_add;
    }

    /**
     * <p>
     * This method will add the active transaction to active trans set
     * </p>
     *
     * @param xid
     *            The xid of the imported transaction
     * @exception ResourceException
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean addActiveTransToSet(Xid xid) throws ResourceException {

        byte[] gid = xid.getGlobalTransactionId();

        if (tc.isDebugEnabled())
            Tr.entry(tc, "addActiveTransToSet", "Xid with global trans Id "
                                                + gid[0]);

        // 12/04/03: 
        // Due to M3_TransactionInflow_4.testSubmitAfterPreparedCMTRequired, it
        // is possible
        // that the user may send another message with the same xid while he has
        // prepared
        // the transaction already. So it is possible that a xid is added to the
        // active
        // trans set while it is already existed in the indoubt trans set. Since
        // this
        // test scenario throws exception, so do not add the active trans while
        // the
        // same xid already exist in the indoubt trans.
        if (setContainsTrans(SetIndoubtTrans, xid) != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addActiveTransToSet",
                         "Xid exists in indoubt trans set. Will not added to active trans set.");
            return true;
        }
        return (addTransToSet(xid, SetActiveTrans));
    }

    /**
     * <p>
     * This method will add the indoubt transaction to indoubt trans set
     * </p>
     * and remove the xid from the active trans set
     *
     * @param xid
     *            The xid of the imported transaction
     * @exception ResourceException
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean addIndoubtTransToSet(Xid xid) throws ResourceException {

        byte[] gid = xid.getGlobalTransactionId();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "addIndoubtTransToSet", "Xid with global trans Id "
                                                 + gid[0]);
        return (addTransToSet(xid, SetIndoubtTrans));
    }

    /**
     * <p>
     * This method will remove the transaction to trans set
     * </p>
     *
     * @param xid
     *            The xid of the imported transaction
     * @exception ResourceException
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    protected synchronized boolean removeTransFromSet(Xid xid, Set tranSet) throws ResourceException {
        boolean rc_remove;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "removeTransFromSet", "Entry");

        try {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "removeTransFromSet",
                         "About to remove Xid with gid="
                                               + xid.getGlobalTransactionId()[0] + " from set");
            // 10/29/03: rruvinsk
            // Because we're dealing with two different implementations of Xid,
            // we need
            // to call setContainsTrans to get an instance of the actual object
            // in the set
            // we want to remove
            // 01/21/04: 
            // Check if the xid is in the set. If not, setcontainsTrans return
            // null
            Xid tempXid = setContainsTrans(tranSet, xid);
            if (tempXid == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "removeTransFromSet",
                             "Xid with gid=" + xid.getGlobalTransactionId()[0]
                                                       + " does not exist in set.");
                return false;
            }
            rc_remove = tranSet.remove(tempXid);

            if (!rc_remove) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "removeTransFromSet",
                             "Xid with gid=" + xid.getGlobalTransactionId()[0]
                                                       + " can not be removed from set.");
                return false;
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "removeTransFromSet", "Done removing from set");
        } catch (UnsupportedOperationException uoe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "removeTransFromSet", uoe);
            throw uoe;
        } catch (ClassCastException cce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "removeTransFromSet", cce);
            throw cce;
        } catch (NullPointerException npe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "removeTransFromSet", npe);
            throw npe;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "removeTransFromSet", "Exit");

        return rc_remove;
    }

    /**
     * <p>
     * This method will remove the active transaction to active trans set
     * </p>
     *
     * @param xid
     *            The xid of the imported transaction
     * @exception ResourceException
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean removeActiveTransFromSet(Xid xid) throws ResourceException {

        byte[] gid = xid.getGlobalTransactionId();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "removeActiveTransFromSet",
                     "Xid with global trans Id " + gid[0]);
        return (removeTransFromSet(xid, SetActiveTrans));
    }

    /**
     * <p>
     * This method will remove the indoubt transaction to indoubt trans set
     * </p>
     *
     * @param xid
     *            The xid of the imported transaction
     * @exception ResourceException
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean removeIndoubtTransFromSet(Xid xid) throws ResourceException {

        byte[] gid = xid.getGlobalTransactionId();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "removeIndoubtTransFromSet",
                     "Xid with global trans Id " + gid[0]);
        return (removeTransFromSet(xid, SetIndoubtTrans));
    }

    /**
     * <p>
     * This method will get the indoubt transaction set
     * </p>
     *
     * @return Set of Xid
     */
    public Set getIndoubtTransSet() {
        return SetIndoubtTrans;
    }

    /**
     * <p>
     * Verify if the list of indoubt trans from app server is the same as the
     * list keep in TRA. If not, exception will be thrown and the indoubt trans
     * list in TRA will be cleanup.
     * </p>
     *
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean verifyIndoubtTrans(Xid[] xidList) throws ResourceException {
        // For each xid in the xidList, see if it is in the Set. If so, remove
        // that one from
        // the Set. If not, save that one to a list of non-exist xid and flag
        // fails.
        // If fail, throw ResourceException
        boolean listMatch = true;
        byte[] gid;
        int size = xidList.length;

        // 01/21/04: 
        // Check the size of the xidList from the Appserver after the recover
        // call
        if (tc.isDebugEnabled())
            Tr.debug(tc, "verifyIndoubtTrans",
                     "Size of Xid from Appserver after recover call is: " + size);

        // 10/27/03: 
        // Create a temp set for remove of XID element instead of using the
        // original
        // Indoubt Trans. The removal of the XID in indoubt trans should be done
        // when
        // the indoubt trans is committed.
        HashSet tempSet = new HashSet(SetIndoubtTrans);

        for (int i = 0; i < size; i++) {
            // 10/26/03: 
            // Can't compare the returned XID directly to the ones in set as
            // the returned XIDs are different implmentations of the XID
            // interface. Use setContainsTrans method to compare the returned
            // XID with the one from the indoubt trans set.
            // If xidInSet is null, means the xidList[i] is not in set.
            Xid xidInSet = setContainsTrans(SetIndoubtTrans, xidList[i]);

            gid = xidList[i].getGlobalTransactionId();

            // 10/26/03: 
            // Can't use Set.contains() due to the above reason.
            // if( !SetIndoubtTrans.contains(xidList[i]) )
            if (xidInSet == null) {
                listMatch = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "verifyIndoubtTrans",
                             "Xid with global trans Id=" + gid[0]
                                                       + "not in Indoubt Trans list from TRA!");
            }
            // 10/27/03: 
            // Use the returned xidInSet to remove from temp indoubt trans set.
            // else if(!removeIndoubtTransFromSet(xidList[i]))
            else if (!removeTransFromSet(xidInSet, tempSet)) {
                // Remove this xid from the set. But the xid doesn't exist in
                // the Set
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "verifyIndoubtTrans",
                             "Xid with global trans Id=" + gid[0]
                                                       + "can't be removed in Indoubt Trans list!");
            }
        }
        // 10/27/03: 
        // If the temp set is not empty, means the 2 lists mismatch
        // so check the tempSet instead of the SetIndoubtTrans
        // if( !SetIndoubtTrans.isEmpty() )
        if (!tempSet.isEmpty()) {
            // Suppose the 2 lists should match. Since there are more indoubt
            // trans in the TRA set than the one from app server, unmatch!
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "verifyIndoubtTrans",
                         "There are more Xid in SetIndoubtTrans than that from AppServer!");
                Iterator it = tempSet.iterator();
                while (it.hasNext()) {
                    gid = ((XidImpl) (it.next())).getGlobalTransactionId();
                    Tr.debug(
                             tc,
                             "verifyIndoubtTrans",
                             "Xid with global trans Id="
                                                   + gid[0]
                                                   + "not in Indoubt Trans list from AppServer!");
                }
            }
            listMatch = false;
        }
        if (!listMatch) {
            // Since the lists are unmatched, cleanup the list in TRA so that
            // the next test
            // case will have a new SetIndoubtTrans.
            SetIndoubtTrans.clear();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "verifyIndoubtTrans",
                         "There are unmatched Xid. Check trace message above!");
        }
        return listMatch;
    }

    /**
     * <p>
     * This method will rollback all active transaction from the active trans
     * set.
     * </p>
     *
     * <p>
     * If there is anything wrong when rollback or when remove xid from active
     * trans set, it will return false and cleanup the active trans set.
     * </p>
     *
     * <p>
     * This method is called by EISTimer
     * </p>
     *
     */
    // 10/26/03: 
    // Synchronized methods which add/remove Xid from Set
    public synchronized boolean rollbackAllActiveTrans() {
        Iterator it = SetActiveTrans.iterator();
        XidImpl xid;
        XATerminator xaTerm = xaTermWrapper.getNativeXaTerm();
        byte[] gid;
        boolean rollbackOK = true;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "rollbackActiveTrans", "Entry");

        while ((it.hasNext() && rollbackOK)) {
            xid = (XidImpl) (it.next());
            gid = xid.getGlobalTransactionId();
            try {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "rollbackActiveTrans",
                             "Rollback Xid with global trans Id=" + gid[0]);
                xaTerm.rollback(xid);
                // remove the element from the active trans set
                // 10/26/03: 
                // use it.remove() instead of removeActiveTransFromSet
                // This is because if not using it.remove(), then the
                // iterator will be invalid anymore. Calling it.next()
                // will raise ConcurrentModificationException.
                // if(!removeActiveTransFromSet(xid))
                it.remove();
                /*
                 * { // Remove this xid from the set. But the xid doesn't exist
                 * in the Set if (tc.isDebugEnabled()) Tr.debug(tc,
                 * "rollbackActiveTrans", "Xid with global trans Id "+ gid[0] +
                 * "can't be removed in Active Trans list!"); }
                 */
            } catch (XAException xe) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "rollbackActiveTrans",
                             "Can not rollback xid with global transaction id "
                                                        + gid[0]);
                rollbackOK = false;
            }
            /*
             * 10/26/03: use it.remove() instead of
             * removeActiveTransFromSet.
             *
             * catch (ResourceException re) { if (tc.isDebugEnabled())
             * Tr.debug(tc, "rollbackActiveTrans",
             * "Can not remove rollback xid from active trans set. The global transaction id is "
             * + gid[0]); rollbackOK = false; }
             */
        }
        // If there is anything wrong in rollback all the active trans, clear
        // the active trans set.
        if (!rollbackOK) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "rollbackActiveTrans",
                         "Rollback not OK. Clearing SetActiveTrans");
            SetActiveTrans.clear();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "rollbackActiveTrans", "Exit");

        return rollbackOK;
    }

    /**
     * Iterates through set 'set' and checks to see if it contains xid 'xid.'
     * Necessary because two different implementations of Xid are used: XidImpl
     * and WebSphere's. This makes it impossible to compare them using
     * .equals(), which the Set.contains() method relies on.
     *
     * @param set
     *            The set to look in
     * @param xid
     *            The xid to search for
     * @return The xid from the set if found; null otherwise
     */
    private Xid setContainsTrans(Set set, Xid xid) {
        Iterator i = set.iterator();
        while (i.hasNext()) {
            Xid setXid = (Xid) i.next();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "setContainsTrans", "Xid from Set has bq = "
                                                 + setXid.getBranchQualifier()[0]);
                Tr.debug(tc, "setContainsTrans",
                         "Xid supplied has bq = " + xid.getBranchQualifier()[0]);

                Tr.debug(tc, "setContainsTrans", "Xid from Set has formatId = "
                                                 + setXid.getFormatId());
                Tr.debug(tc, "setContainsTrans", "Xid supplied has formatId = "
                                                 + xid.getFormatId());

                Tr.debug(tc, "setContainsTrans", "Xid from Set has gid = "
                                                 + setXid.getGlobalTransactionId()[0]);
                Tr.debug(tc, "setContainsTrans", "Xid supplied has gid = "
                                                 + xid.getGlobalTransactionId()[0]);
            }

            /*
             * 02/19/04: d188279 Since the pass-in xid may be from
             * recover() call, the format Id and the BranchQualifier will be
             * different from what we submitted when we send message. Therefore,
             * we can only rely on the check on globalTranId to determine if the
             * xid from recover() call is the same one listed in the indoubt or
             * active trans Set in TRA. As a result, if the globalTranId of the
             * 2 xids are the same, we treat that xid from the recover() call
             * exist in either indoubt or active trans set.
             *
             * if( setXid.getBranchQualifier()[0] == xid.getBranchQualifier()[0]
             * && setXid.getFormatId() == xid.getFormatId() &&
             * setXid.getGlobalTransactionId()[0] ==
             * xid.getGlobalTransactionId()[0])
             */
            if (setXid.getGlobalTransactionId()[0] == xid
                            .getGlobalTransactionId()[0]) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "setContainsTrans", "Xid is in Set.");

                return setXid;
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setContainsTrans", "Xid is NOT in Set.");
        return null;
    }

    /**
     * <p>
     * This method will return the EISTimer associated with this workDispatcher
     * </p>
     *
     * @return EISTimer The EISTimer instance associated with this
     *         workDispatcher
     */
    public EISTimer getEISTimer() {
        return eisTimer;
    }

    // d177221 ends: 09/24/03

    class Timer implements Runnable {
        private final Object syncObj;
        private final int waitTimeout;

        // 11/20/03: 
        // Let timer to find out if work has reached the desired state.
        private final FVTWorkImpl work;

        // 11/20/03: 
        public Timer(int waitTimeout, FVTWorkImpl work, Object obj) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "WorkDispatcher.Timer.init", new Object[] { work,
                                                                         new Integer(waitTimeout), obj });

            syncObj = obj;
            this.waitTimeout = waitTimeout;
            this.work = work;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "<init>", this);
        }

        @Override
        public void run() {
            try {

                // Sleep for waitTimeout millisecond.
                Thread.sleep(waitTimeout);

                // 11/19/03: 
                // Only need to set timedout to true if we really need the
                // timer.
                if (!work.isWorkHasReachedState()) {
                    // Has to set timedout to true
                    timedout = true;

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "timer", "timeout");

                    // Notify the waiters
                    synchronized (syncObj) {
                        syncObj.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "InterruptedException is caught in Timer", e);
                throw new RuntimeException("InterruptedException is caught in Timer", e);
            }
        }
    }

    /*
     * class ConcurrentWorkTimer implements Runnable { private Object syncObj;
     * private int waitTimeout;
     *
     * // 11/20/03: // Let timer to find out if work has reached the
     * desired state. private FVTConcurrentWorkImpl work;
     *
     * // 11/20/03: public ConcurrentWorkTimer(int waitTimeout,
     * FVTConcurrentWorkImpl work, Object obj) { if (tc.isEntryEnabled())
     * Tr.entry(tc, "<init>", new Object[] { work, new Integer(waitTimeout), obj
     * });
     *
     * syncObj = obj; this.waitTimeout = waitTimeout; this.work = work;
     *
     * if (tc.isEntryEnabled()) Tr.exit(tc, "<init>", this); } public void run()
     * { try {
     *
     * // Sleep for waitTimeout millisecond. Thread.sleep(waitTimeout);
     *
     *
     * // 11/20/03: // Need to check the type of instance of work. If it's
     * // concurrentWork, call isAllWorkHaveReachedState instead. if(
     * !FVTConcurrentWorkImpl.isAllWorksHaveReachedState() ) { // Has to set
     * timedout to true timedout = true;
     *
     * if (tc.isDebugEnabled()) Tr.debug(tc, "timer", "timeout");
     *
     * // Notify the waiters synchronized (syncObj) { syncObj.notifyAll(); } } }
     * catch (InterruptedException e) { if( tc.isDebugEnabled()) Tr.debug(tc,
     * "InterruptedException is caught in ConcurrentWorkTimer", e); throw new
     * RuntimeException("InterruptedException is caught in ConcurrentWorkTimer",
     * e); } } }
     */

}
