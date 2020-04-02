/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionRolledbackException;

import com.ibm.ejs.container.lock.LockManager;
import com.ibm.ejs.container.lock.Locker;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.ejs.csi.UOWControl;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.websphere.cpi.CPIException;
import com.ibm.websphere.csi.BeanInstanceInfo;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.TransactionListener;
import com.ibm.websphere.csi.TxContextChange;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.ContainerSynchronization;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.diagnostics.IncidentStreamWriter;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.traceinfo.ejbcontainer.TETxLifeCycleInfo;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * A <code>ContainerTx</code> manages all container
 * resources associated with a given global transaction. <p>
 *
 * Note, the lifecycle of this object guarantees that there will be
 * exactly one instance in a container for each JTS transaction the
 * container is associated with. This means that the default Object
 * hashCode() and equals() method implementations work correctly for
 * instances of <code>ContainerTx</code>. <p>
 *
 * Note also that a transaction can be active on at most 1 thread, so
 * access to this class' HashMap data structures do not need to be
 * guarded by synchronization blocks.
 *
 */

public class ContainerTx implements Locker, Synchronization, com.ibm.websphere.cpi.PersisterTx, ContainerSynchronization {
    private static final TraceComponent tc = Tr.register(ContainerTx.class, "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.ContainerTx";

    /**
     * Various states for ContainerTx
     */
    private static final int ACTIVE = 0;
    private static final int PREPARING = 1;
    private static final int COMMITTED = 2;
    private static final int ROLLEDBACK = 3;

    private static final String[] stateStrs = { "Active", "Preparing",
                                                "Committed", "Rolledback" };

    /**
     * The maximum number of times to loop before failing if
     * BeanO.beforeCompletion continuously enlists new BeanOs.
     */
    private static final int MAX_ENLISTMENT_ITERATIONS = 30;

    /**
     * Container this <code>ContainerTx</code> is associated with. <p>
     */
    private EJSContainer ivContainer;

    /**
     * List of resources (BeanOs) that have been enlisted with this
     * container transaction. <p>
     */
    private HashMap<BeanId, BeanO> beanOs = null; //111555

    /**
     * origLock designates that we have started the beforeCompletion
     * logic.
     */
    // d115597
    private boolean origLock = false;

    protected TransactionListener txListener; //110762.1

    /**
     * An ordered list of the BeanOs that have been enlisted with this
     * container transaction.
     * Both the HashMap above and this list maintain the same information
     * This duplicate information may seem wasteful, but performance will
     * be better with this arrangement (space/time).
     */
    private ArrayList<BeanO> beanOList = null; //111555

    //115597
    /**
     * The following structures are needed to allow ejbStore to call
     * other beans during beforeCompletion. tempList is a list built up
     * for beans called while the list of beans to run beforeCompletion on is
     * run through. There can be multiple instances of templist created if subsequent
     * calls on beans are made during iterations on subsequent lists.
     * The currentBeanO's hashmap is used to determine whether a BeanO is
     * already enlisted in the current tempList
     * The afterList is used to construct a list of BeanOs to call after completion
     * on. This is necessary since the BeanOList no longer has a list of all the
     * potential BeanOs involved in the transaction, as we allow enlistments after
     * beforeCompletion.
     */
    //115597
    private ArrayList<BeanO> tempList = null;//115597
    private HashMap<BeanId, BeanO> currentBeanOs = null;//115597
    private ArrayList<BeanO> afterList = null;//115597

    /**
     * Table of pointers to lists of PM persistence-managed BeanOs enlisted in
     * this transaction. Each entry is keyed by home and points to an ArrayList,
     * which contains the BeanOs.
     */
    // d140003.22
    protected HashMap<EJSHome, ArrayList<BeanO>> homesForPMManagedBeans;

    /**
     * Table of non-persistent timers queued to be started (or not) in afterCompletion
     * lazily created
     */
    public HashMap<String, TimerNpImpl> timersQueuedToStart; // F473-425.1

    /**
     * Table of non-persistent timers canceled within this (global) transaction
     * lazily created
     */
    public HashMap<String, TimerNpImpl> timersCanceled; // F473-425.1

    /**
     * Reference to the BeanO that should be removed during afterCompletion
     * processing in support of EJB 3.0 Remove Methods (@Remove). <p>
     *
     * When a remove method has completed successfully, the corresponding
     * BeanO will be set in ContainerTx to insure the SessionSynchronization
     * callbacks are not called, and the bean is removed after the
     * transaction completes (afterCompletion). <p>
     *
     * This field must be reset when the bean is removed, otherwise a
     * RemoveException will be thrown. Normally, this will occur if
     * a remove method is called, and the transaction does NOT complete. <p>
     **/
    // 390657
    protected BeanO ivRemoveBeanO;

    /**
     * Use to support lazy creation of beanOs and beanOList, if true
     * enlist has occurred and initial values created. Skip doing in
     * constructor, avoiding until vectors actually required.
     */
    // d111555
    private boolean ivBeanOsEnlistedInTx = false;

    /**
     * Set to true if this ContainerTx was begun for the currently
     * executing method
     */
    protected boolean began = false;

    /**
     * Non-null if {@link #afterCompletion} should call {@link EJSContainer#postInvokePopCallbackContexts} because this
     * transaction is being completed because the EJB method that started this
     * transaction is ending. Null if contexts were successfully popped.
     */
    // RTC107108
    protected EJSDeployedSupport ivPostInvokeContext;

    /**
     * Isolation level of this transaction.
     */
    private int ivIsolationLevel;

    /**
     * The current state of this ContainerTx
     */
    private int state;

    /**
     * Is associated transaction global
     */
    protected boolean globalTransaction;

    /**
     * True iff a bean called setRollbackOnly; used to determine whether
     * or not to commit a container-started transaction
     */
    private boolean markedRollbackOnly = false;

    /**
     * JTS transaction associated with this container transaction.
     */
    SynchronizationRegistryUOWScope ivTxKey; // d704504

    /**
     * Activity session that encompasses this container transaction
     */
    private ContainerAS containerAS; // LIDB441.5

    /*
     * ActivitySessions may be checkpointed or reset during the middle
     * of the session. This flag tells us if we are in the middle of,
     * or at the end of, an activitySession when beforeCompletion and
     * afterCompletion calls come in. By default this flag is true.
     * It is only set false for the special case of a mid-activitySession
     * reset or checkpoint. This allows us to do special processing
     * for mid-AS resets or checkpoints.
     */
    private boolean isActivitySessionCompleting = true; // d126930.3

    /*
     * Pointer to container's UOW control collaborator
     */
    private UOWControl uowCtrl; // d126930.3

    /*
     * Accessor to setup ComponentMetaData on thread
     */
    private ComponentMetaDataAccessorImpl cmdAccessor; //d126930.3

    /*
    *
    */
    private ArrayList<Synchronization> finderSyncList = null;

    /**
     * True if the transaction is currently in a finder method which
     * requires that the Persistence Manager must synchronize the
     * enlisted Entity beans that may effect the outcome of the query.
     * This flag is reset to false once the flush has been performed.
     * The transaction cannot continue successfully, if the flush is
     * not performed in accordance with the EJB specification.
     */
    // d140003.22
    protected boolean ivFlushRequired = false;

    /**
     * This state variable advises the persister that a CMP11 preFind flush is
     * in progress for CMP11 beans in a 2.0 module. See the EJSHome.preFind()
     * for usage.
     **/
    protected boolean ivCMP11FlushActive = false; // d112604.1

    /**
     * Holds a reference to the first exception that occurs during post
     * processing (commit). This allows a post processing (commit) time
     * exception to be nested in the eventual TransactionRolledBack
     * exception thrown from EJSContainer.postInvoke().
     **/
    // PQ90221
    protected Throwable ivPostProcessingException = null;

    /** Number of Transaction scoped cache hits. **/
    private long ivTxCacheHits = 0; // LI3408

    /** Number of finds/searches of the Transaction scoped cache. **/
    private long ivTxCacheSearch = 0; // LI3408

    /**
     * Transition to the PREPARING state
     */
    private final/* synchronized */void becomePreparing() {
        if (state != ACTIVE) {
            throw new IllegalStateException(stateStrs[state]);
        }
        state = PREPARING;
    }

    /**
     * Initialize BeanO vectors if require, lazy initialization
     */
    // d111555
    private final void initialize_beanO_vectors() {
        // ensure allocation not just done in another thread via an enlist call
        if (ivBeanOsEnlistedInTx == false) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Lazy beanOs vector creation");

            // ArrayLists and HashMaps are created with the java default values
            // for performance - avoids 1 method call each.               d532639.2
            beanOs = new HashMap<BeanId, BeanO>(16);
            beanOList = new ArrayList<BeanO>(10);
            afterList = new ArrayList<BeanO>(10);//115597
            homesForPMManagedBeans = new HashMap<EJSHome, ArrayList<BeanO>>(16); // d140003.22
            ivBeanOsEnlistedInTx = true; // never set to false once set to true
        } // if beanOs == null
          // d111555 end
    }

    /**
     * Transition to the COMMITTED state
     */
    private final/* synchronized */void becomeCommitted() {
        if (state != PREPARING) {
            throw new IllegalStateException(stateStrs[state]);
        }
        state = COMMITTED;
    }

    /**
     * Transition to the ROLLEDBACK state
     */
    private final/* synchronized */void becomeRolledback() {
        state = ROLLEDBACK;
    }

    /**
     * Check to make sure that this ContainerTx is in the ACTIVE state
     */
    private final/* synchronized */void ensureActive() //d173022.2
                    throws TransactionRolledbackException {
        switch (state) {

            case ACTIVE:
                break;

            case ROLLEDBACK:
                throw new TransactionRolledbackException();

            default:
                throw new IllegalStateException(stateStrs[state]);

        }
    }

    /**
     * Create a new <code>ContainerTx</code> instance. <p>
     */
    public ContainerTx(EJSContainer container, boolean isTransactionGlobal,
                       SynchronizationRegistryUOWScope txKey, UOWControl UOWCtrl) {
        ivContainer = container;
        ivIsolationLevel = java.sql.Connection.TRANSACTION_NONE; // d107762
        ivTxKey = txKey;
        this.uowCtrl = UOWCtrl; //d126930.3
        state = ACTIVE;
        globalTransaction = isTransactionGlobal;
        isActivitySessionCompleting = true; //d126930.3
        cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor(); //d126930.3
        //      finderSyncList=new ArrayList();                             // d173022.12
        try {
            this.containerAS = container.getCurrentSessionalUOW(false); //139562-5.EJBC d348420
        } catch (com.ibm.websphere.csi.CSIException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".<init>",
                                        "359", this);
            this.containerAS = null;
            Tr.error(tc, "IGNORING_UNEXPECTED_EXCEPTION_CNTR0033E", e);
        }

    } // ContainerTx

    /**
     * This method returns true if associated transaction is global
     */
    public boolean isTransactionGlobal() {
        return globalTransaction;
    }

    /**
     * Returns true if the associated transaction is active AND global.
     **/
    // d303100
    public final boolean isActiveGlobal() {
        return (globalTransaction && (state == ACTIVE));
    }

    /**
     * This method is called before the transaction this
     * <code>ContainerTx</code> is associated with is completed
     * (rolled back or committed). <p>
     */
    @Override
    public void beforeCompletion() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "beforeCompletion",
                         new Object[] { Boolean.valueOf(globalTransaction),
                                        this,
                                        Boolean.valueOf(isActivitySessionCompleting) });

            if (TETxLifeCycleInfo.isTraceEnabled()) {
                String tid = getTxId();
                if (tid != null) {
                    if (globalTransaction) {
                        TETxLifeCycleInfo.traceGlobalTxBeforeCompletion(tid, "Global/User Tx BeforeCompletion");
                    } else {
                        TETxLifeCycleInfo.traceLocalTxBeforeCompletion(tid, "Local Tx BeforeCompletion");
                    }
                }
            } // PQ74774
        }

        //d139562.11
        if (finderSyncList != null) {
            int numberofCallers = finderSyncList.size();
            for (int i = 0; i < numberofCallers; ++i) {
                finderSyncList.get(i).beforeCompletion();
            }
        } // d173022.12

        if (ivBeanOsEnlistedInTx) {
            //d173022.2
            origLock = true;//d115597 set the origLock flag to true because beforeCompletion entered

            EJBThreadData threadData = EJSContainer.getThreadData(); // d630940
            BeanO beanO = null;
            boolean popCallbackBeanO = false;
            ComponentMetaData prevCMD = null;
            boolean resetCMD = false;
            ClassLoader prevClassLoader = null;
            Object origClassLoader = ThreadContextAccessor.UNCHANGED;

            try {
                // Process beans in a loop to allow beforeCompletion callbacks to
                // enlist additional beans.  We limit the number of iterations to
                // detect infinite loops.                                      d115597
                int iterationCount = 0;
                for (ArrayList<BeanO> iterationBeanOs = beanOList; iterationBeanOs != null; iterationBeanOs = tempList, tempList = null) {
                    if (iterationCount++ > MAX_ENLISTMENT_ITERATIONS) {
                        beanO = null;
                        throw new RuntimeException("Exceeded " + MAX_ENLISTMENT_ITERATIONS + " enlistment iterations in beforeCompletion");
                    }

                    //--------------------------------------------------------------
                    // d115597
                    // Imposes an order in which the beforeCompletion callbacks
                    // will be fired on the beans.
                    // beforeCompletion is called on beans in the order they were
                    // enlisted.  This is true for beans enlisted after beforeCompletion
                    // has begun as well.
                    // d115597
                    //--------------------------------------------------------------

                    for (int i = 0, size = iterationBeanOs.size(); i < size; i++) {
                        beanO = iterationBeanOs.get(i);
                        BeanMetaData bmd = beanO.home.beanMetaData;

                        // Process all beans if the activity session is completing.
                        // Only process entity beans if this is a checkpoint.
                        //
                        // d126930.3
                        // By default isActivitySessionCompleting is set to true even if there
                        // is no activitySession present.  This simply means we follow the normal
                        // ContainerTx logic for ending a transaction.
                        if (isActivitySessionCompleting || bmd.isEntityBean()) {
                            // Update the thread callback bean.
                            threadData.pushCallbackBeanO(beanO); // d168509, d630940
                            popCallbackBeanO = true;

                            // Update the thread CMD if it's not the same as the
                            // previous bean.
                            if (bmd != prevCMD) {
                                if (resetCMD) {
                                    cmdAccessor.endContext();
                                }

                                cmdAccessor.beginContext(bmd);
                                prevCMD = bmd;
                                resetCMD = true;

                                // Update the class loader if it's not the same as the
                                // previous bean.                         PK51366, PK83186
                                ClassLoader classLoader = bmd.ivContextClassLoader; // F85059
                                if (classLoader != prevClassLoader) {
                                    prevClassLoader = classLoader;
                                    origClassLoader = EJBThreadData.svThreadContextAccessor.repushContextClassLoaderForUnprivileged(origClassLoader, classLoader);
                                }
                            }

                            // d126930.3
                            // EJB 2.0 beans may have sticky local transactions (i.e.
                            // local tx boundary = ActivitySession).  We need to assure
                            // the correct ComponentMetaData and local tx context is
                            // on thread in this case.  Note that EJB 1.1 beans may not
                            // have local tx boundary = ActivitySession.  This is enforced
                            // by a check in BeanMetaData
                            if (isActivitySessionCompleting &&
                                (globalTransaction ||
                                 bmd._localTran.getBoundary() == LocalTransactionSettings.BOUNDARY_ACTIVITY_SESSION)) {
                                beanO.beforeCompletion();
                            } else {
                                TxContextChange changedContext = uowCtrl.setupLocalTxContext(beanO.getId());
                                try {
                                    if (isActivitySessionCompleting) {
                                        beanO.beforeCompletion();
                                    } else {
                                        //-------------------------------------------------------------
                                        // Special case added for mid-activitySession checkpoint.
                                        // Entity beans enlisted in local transactions must be stored
                                        // to flush any in-memory updates out to the backing store
                                        // (ie. checkpoint them).  For other bean types, we do
                                        // nothing on mid-activitySession checkpoint.  The local
                                        // transaction service will make setCompleting = false calls to
                                        // the containerSynchronization interface at checkpoint.  Also,
                                        // beforeCompletion and afterCompletion will be called.  We
                                        // have special code both here, and in afterCompletion, so that
                                        // the beans' states do not change.  We just store them.
                                        // ------------------------------------------------------------
                                        beanO.store();
                                    }
                                } finally {
                                    uowCtrl.teardownLocalTxContext(changedContext);
                                }
                            }

                            popCallbackBeanO = false;
                            threadData.popCallbackBeanO();
                        }
                    }
                }
            } catch (Throwable t) {

                FFDCFilter.processException(t, CLASS_NAME + ".beforeCompletion",
                                            "562", this);
                if (isTraceOn && tc.isEventEnabled()) // d144064
                    Tr.event(tc, "Exception during beforeCompletion(): rolling back",
                             new Object[] { beanO, t });

                // Destroy the beanO, too late to delist. Destroying it should
                // take care of the state when the transaction is rolled back.
                // Note there may not be a beanO, if the exception is container
                // detecting a loop condition - not a bean exception.     d160910
                if (beanO != null)
                    beanO.destroy();

                ivContainer.uowCtrl.setRollbackOnly();

                // Log the exception
                ExceptionUtil.logException(tc, t, null, beanO);

                // Save the first exception that occurs during commit
                // processing to nest in TransactionRolledBackException.  PQ90221
                if (ivPostProcessingException == null)
                    ivPostProcessingException = t;

                // Inform the transaction manager that the beforeCompletion
                // failed. Else, the tran engine might continue to fire
                // beforeCompletion callbacks on other participants
                throw new RuntimeException("", t); //253963

            } finally {
                EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(origClassLoader);

                if (resetCMD) {
                    cmdAccessor.endContext();
                }

                if (popCallbackBeanO) {
                    threadData.popCallbackBeanO();
                }
            }
        }

        if (isActivitySessionCompleting) {
            //--------------------------------------------------------------
            // The before completion callbacks on the beans may result
            // in more beans getting enlisted in this transaction, so we don't
            // change the state to PREPARING until all beans are prepared.
            //--------------------------------------------------------------
            becomePreparing();
        }

        // Now that all of the beans have been successfully stored, call
        // before completion on the PM transaction listener.           d134692
        if (txListener != null) {
            try {
                if (isTraceOn && tc.isDebugEnabled()) // d144064
                    Tr.debug(tc, "Calling txListener.beforeCompletion()");
                txListener.beforeCompletion();
            } catch (Throwable e) {
                FFDCFilter.processException(e, CLASS_NAME + ".beforeCompletion",
                                            "734", this);

                if (ivPostProcessingException == null) { //253963
                    ivPostProcessingException = e; //253963
                }
                throw new RuntimeException("txListener exception" + e.toString());
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "beforeCompletion"); // d144064

    } // beforeCompletion

    /**
     * This method is called after the transaction associated with this
     * <code>ContainerTx</code> has completed (rolled back
     * or committed). <p>
     *
     * @param status a <code>boolean</code> indicating whether the
     *                   transaction committed (true) or rolled back (false) <p>
     */
    @Override
    public void afterCompletion(int status) {

        boolean committed = true;
        boolean reLoad = false;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (status != Status.STATUS_COMMITTED) {
            committed = false;
        }

        if (isTraceOn && TETxLifeCycleInfo.isTraceEnabled()) {
            String tid = getTxId();
            if (tid != null) {
                if (globalTransaction) {
                    TETxLifeCycleInfo.traceGlobalTxAfterCompletion(tid, "Global/User Tx AfterCompletion");
                } else {
                    TETxLifeCycleInfo.traceLocalTxAfterCompletion(tid, "Local Tx AfterCompletion");
                }
            }
        } // PQ74774

        //d139562.11
        if (finderSyncList != null) {
            int numberofCallers = finderSyncList.size();
            for (int i = 0; i < numberofCallers; ++i) {
                finderSyncList.get(i).afterCompletion(status);
            }
        } // d173022.12

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.entry(tc, "afterCompletion", new Object[] { Boolean.valueOf(globalTransaction), this,
                                                           Boolean.valueOf(committed),
                                                           Boolean.valueOf(isActivitySessionCompleting) }); //123293

        // When an EJB method ends, we need to remove bean-specific contexts from
        // the thread.  Those contexts need to be present for beforeCompletion for
        // JPA @PreUpdate, so postInvoke cannot pop the contexts prior to
        // transaction completion, but the calls to Activator.commit/.rollbackBean
        // below might unpin the currently invoked bean from the current
        // transaction, which would allow the bean to be active concurrently on
        // another thread while other transaction callbacks are running on this
        // thread.  To avoid that, pop the contexts now if this transaction is
        // being completed because an EJB method is ending.              RTC107108
        if (ivPostInvokeContext != null) {
            ivContainer.postInvokePopCallbackContexts(ivPostInvokeContext);
            ivPostInvokeContext = null;
        }

        try {

            if (committed) {

                // d126930.3
                // By default activitSessionCompleting is set to true.  Even if there
                // is no activitySession present.  This simply means we follow the noraml
                // ContainerTx logic for ending a transaction.

                if (isActivitySessionCompleting == true) {

                    becomeCommitted();

                    // Since we're committing, we can now allow timers queued to
                    // start to be started (not in this list if also cancelled).
                    if (timersQueuedToStart != null) {
                        for (TimerNpImpl timer : timersQueuedToStart.values()) {
                            timer.start();
                        }
                    }

                    // Any timers canceled in this global tran were added to
                    // timersCanceled in case they needed to be re-created in
                    // rollback.  Since we're not rolling back, go ahead and
                    // destroy these, then clear this map
                    if (timersCanceled != null) {
                        for (TimerNpImpl timer : timersCanceled.values()) {
                            timer.remove(true); // RTC107334
                        }
                    }

                    // d111555  Only perform if a beanO has been enlisted
                    if (ivBeanOsEnlistedInTx) {
                        //115597 changed to use afterList
                        int afterListSize = afterList.size(); // d122418-5
                        for (int i = 0; i < afterListSize; i++) {
                            BeanO beanO = afterList.get(i); // d122418-5
                            try {
                                beanO.commit(this);
                            } catch (Throwable ex) {
                                FFDCFilter.processException(ex,
                                                            CLASS_NAME + ".afterCompletion",
                                                            "795", this);
                                if (isTraceOn && tc.isEventEnabled()) // d144064
                                    Tr.event(tc, "Exception thrown in commit()",
                                             new Object[] { beanO, ex });
                            } finally {
                                // Perform the Activation Strategy afterCompletion -
                                // commit processing. Ignore any exception that may
                                // occur, as the transaction has already committed
                                // and insure the rest of the beans in the list
                                // get processed to minimize side effects of any
                                // failure here.                                 d145697
                                try {
                                    ivContainer.activator.commitBean(this, beanO);
                                } catch (Throwable ex) {
                                    FFDCFilter.processException(ex,
                                                                CLASS_NAME +
                                                                    ".afterCompletion",
                                                                "811", this);
                                    if (isTraceOn && tc.isEventEnabled())
                                        Tr.event(tc, "Exception thrown in commitBean()",
                                                 new Object[] { beanO, ex });
                                }
                            }
                        } // end for
                    } // if ivBeanOsEnlistedInTx               // d111555

                    // d126930.3
                    // --------------------------------------------------------------
                    // Special case added for mid-activitySession checkpoint.  Entity
                    // beans enlisted in local transactions must be stored during
                    // the checkpoint.  This occured at beforeCompletion time.  Here
                    // we just do nothing, to make sure the bean's state does not
                    // not change.
                    // --------------------------------------------------------------

                } else { //checkpoint/reset processing

                    reLoad = true;

                } // end else (special case for mid-ActivitySession checkpoint)

            } else { // JTS Status is Rollback

                // d126930.3
                // By default activitSessionCompleting is set to true.  Even if there
                // is no activitySession present.  This simply means we follow the normal
                // ContainerTx logic for ending a transaction.

                if (isActivitySessionCompleting == true) {

                    becomeRolledback();

                    // re-start any timers that had been cancelled in this global tran
                    if (timersCanceled != null) {
                        for (TimerNpImpl timer : timersCanceled.values()) {
                            timer.start();
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "rollback re-started queued-as-canceled timer: " + timer);
                            }
                        }
                    }

                    // Do not start timers queued to start.  Rather, mark them
                    // destroyed and clear the queue.
                    if (timersQueuedToStart != null) {
                        for (TimerNpImpl timer : timersQueuedToStart.values()) {
                            timer.remove(true); // RTC107334
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "rollback destroyed queued-to-start timer: " + timer);
                            }
                        }
                    }

                    // d111555 only preform if one or more beans in tx
                    if (ivBeanOsEnlistedInTx) { // d111555
                        int afterListSize = afterList.size(); // d122418-5
                        //d115597 changed to use afterList
                        for (int i = 0; i < afterListSize; i++) {
                            BeanO beanO = afterList.get(i); // d122418-5
                            try {
                                beanO.rollback(this);
                            } catch (Throwable ex) {
                                FFDCFilter.processException(ex,
                                                            CLASS_NAME + ".afterCompletion",
                                                            "863", this);
                                if (isTraceOn && tc.isEventEnabled()) // d144064
                                    Tr.event(tc, "Exception thrown in rollback()",
                                             new Object[] { beanO, ex });
                            } finally {
                                // Perform the Activation Strategy afterCompletion -
                                // commit processing. Ignore any exception that may
                                // occur, as the transaction has already committed
                                // and insure the rest of the beans in the list
                                // get processed to minimize side effects of any
                                // failure here.                                 d145697
                                try {
                                    ivContainer.activator.rollbackBean(this, beanO);
                                } catch (Throwable ex) {
                                    FFDCFilter.processException(ex,
                                                                CLASS_NAME +
                                                                    ".afterCompletion",
                                                                "879", this);
                                    if (isTraceOn && tc.isEventEnabled())
                                        Tr.event(tc, "Exception thrown in rollbackBean()",
                                                 new Object[] { beanO, ex });
                                }
                            }
                        } // while
                    } // if ivBeanOsEnlistedInTx              // d111555

                    // d126930.3
                    // -------------------------------------------------------------
                    // Special case added for mid-activitySession reset.  Entity
                    // beans enlisted in local transactions must be reloaded
                    // from the backing store to erase any in-memory updates that
                    // were done up to this point (ie. reset them).  For other bean
                    // types, we do nothing on mid-activitySession reset. The local
                    // transaction service will make a setCompleting = false call on
                    // the containerSynchronization interface, followed by an
                    // afterCompletion call.  These calls are not preceeded a
                    // a beforeCompletion call.  The beans' state should not be
                    // changed.  We just reload them.
                    // -------------------------------------------------------------

                } else {
                    reLoad = true;
                }

            } // end else (rollback)

        } catch (Throwable t) {

            FFDCFilter.processException(t, CLASS_NAME + ".afterCompletion",
                                        "913", this);
            if (isTraceOn && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Exception during afterCompletion", t);
            throw new RuntimeException(t.toString());

        } finally { //110762.1

            // Persistence Manager wants notified

            try {
                if (txListener != null)
                    try {
                        txListener.afterCompletion(status); // d160445
                        txListener = null;
                    } catch (Throwable e) {
                        FFDCFilter.processException(e, CLASS_NAME + ".afterCompletion",
                                                    "929", this);
                        throw new Exception("txListener exception" + e.toString());
                    }
            } catch (Throwable e) {
                FFDCFilter.processException(e, CLASS_NAME + ".afterCompletion",
                                            "934", this);
            }

            if (!reLoad)
                ivContainer.containerTxCompleted(this); //d153430 added reLoad flag
        } // end finally

        // Activity session checkpoint.

        if (reLoad && afterList != null && !afterList.isEmpty()) { // d630940
            // Reset flag so beans will return to normal ContainerTx logic
            // unless another activitySession checkpoint or reset call is made.

            // Save the BeanOs processed in the following loop in an instance variable
            // so methods called from the customer callbacks can determine which beanO
            // is active on the thread. Access the BeanO in the loop in a local
            // variable for performance.                                       d168509
            EJBThreadData threadData = EJSContainer.getThreadData(); // d630940
            boolean popCallbackBeanO = false;
            ComponentMetaData prevCMD = null;
            boolean resetCMD = false;
            ClassLoader prevClassLoader = null;
            Object origClassLoader = ThreadContextAccessor.UNCHANGED;

            try {
                int afterListSize = afterList.size();
                for (int i = 0; i < afterListSize; i++) {
                    BeanO beanO = afterList.get(i);
                    try {
                        BeanMetaData bmd = beanO.home.beanMetaData;

                        if (bmd.type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY ||
                            bmd.type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY) {
                            attachListener();
                            if (txListener != null) {
                                txListener.afterBegin();
                            } else {
                                throw new Exception("Error: Null Transaction Listener");
                            }

                            // If the Entity bean has exclusive access to the data
                            // (OptA) or the LoadPolicy has defined a reload interval
                            // (ReadOnly), then there is no need to re-load the
                            // bean.  Otherwise, the Entity bean must be reloaded
                            // to acquire the proper DB locks. The forUpdate = false
                            // parameter has no effect because these are EJB 2.0
                            // beans.  Both BMP and CMP entities must use the
                            // access intent information from the application
                            // profile to determine the locking requirements.
                            // Also, we make sure the correct ComponentMetaData
                            // and local transaction context is on the thread before
                            // loading the bean.
                            if (bmd.optionACommitOption ||
                                bmd.ivCacheReloadType != BeanMetaData.CACHE_RELOAD_NONE) {
                                // Bean is not re-loaded, but PM needs to be informed
                                // the bean is enlisted with the tran.               LI3408
                                ((EntityBeanO) beanO).enlistForOptionA(this);
                            } else {
                                threadData.pushCallbackBeanO(beanO); // d168509, d630940
                                popCallbackBeanO = true;

                                // Update the thread CMD if it's not the same as the
                                // previous bean and this is a bean-managed bean.
                                if (bmd != prevCMD &&
                                    bmd.type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY) {
                                    if (resetCMD) {
                                        cmdAccessor.endContext();
                                    }

                                    cmdAccessor.beginContext(bmd);
                                    prevCMD = bmd;
                                    resetCMD = true;

                                    // Update the class loader if it's not the same as the
                                    // previous bean.                         PK51366, PK83186
                                    ClassLoader classLoader = bmd.ivContextClassLoader; // F85059
                                    if (classLoader != prevClassLoader) {
                                        prevClassLoader = classLoader;
                                        origClassLoader = EJBThreadData.svThreadContextAccessor.repushContextClassLoaderForUnprivileged(origClassLoader, classLoader);
                                    }
                                }

                                Object cmdCtxSet = null;
                                TxContextChange changedContext = null;
                                try {
                                    cmdCtxSet = cmdAccessor.beginContext(bmd);
                                    changedContext = uowCtrl.setupLocalTxContext(beanO.getId());
                                    ((EntityBeanO) beanO).load(this, false);
                                } finally {
                                    uowCtrl.teardownLocalTxContext(changedContext);
                                    if (cmdCtxSet != null) {
                                        cmdAccessor.endContext();
                                    }
                                }

                                popCallbackBeanO = false;
                                threadData.popCallbackBeanO();
                            }
                        }

                    } catch (Throwable ex) {
                        FFDCFilter.processException(ex,
                                                    CLASS_NAME + ".afterCompletion",
                                                    "978", this);
                        if (isTraceOn && tc.isEventEnabled()) // d144064
                            Tr.event(tc, "Exception thrown attempting to reload bean " +
                                         "due to activitySession reset",
                                     new Object[] { beanO, ex });

                        //d135218 - added destroy, setRollbackOnly, logException, &
                        //          throw exception below to match beforeCompletion
                        //          logic since ActiviySession reset occurs during
                        //          the middle of a local transaction, not at the
                        //          end, like afterCompletion usually handles.

                        // Destroy the beanO, too late to delist. Destroying it should
                        // take care of the state when the transaction is rolled back
                        beanO.destroy();

                        ivContainer.uowCtrl.setRollbackOnly();

                        // Log the exception
                        ExceptionUtil.logException(tc, ex, null, beanO);

                        // Inform the activitySession service that reset
                        // failed.
                        throw new RuntimeException(ex.toString());

                    }
                } // end for loop
            } finally {
                EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(origClassLoader);

                if (resetCMD) {
                    cmdAccessor.endContext();
                }

                if (popCallbackBeanO) {
                    threadData.popCallbackBeanO();
                }
            }

        } // end else (special case for mid-ActivitySession reset)

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "afterCompletion"); // d144064

    } // afterCompletion

    protected void attachListener() {
        // This method is overridden in WASContainerTx to allow for Persistence
        // Manager logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    }

    // begin F743-425.1
    /**
     * Add the input non-persistent timer to the queue
     * of timers to be started upon commit of the global
     * transaction. If not in a global transaction,
     * start the timer immediately.
     *
     * Called by each of the BeanO.createXxxTimer methods
     * immediately after creating a non-persistent timer.
     */
    public void queueTimerToStart(TimerNpImpl timer) { // F743-13022

        if (globalTransaction) {
            // If this is a calendar-based timer with no expirations, don't queue to start
            if (timer.getIvExpiration() == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.exit(tc, "queueTimerToStart: not queued - created with no expiration");
                return;
            }

            if (timersQueuedToStart == null) { // F743-425.CodRev
                // Lazy creation of HashMap
                timersQueuedToStart = new HashMap<String, TimerNpImpl>(); // F473-425.1
            }

            timersQueuedToStart.put(timer.getTaskId(), timer);

        } else {

            timer.start();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tran ctx is not global, so started timer immediately.");
            }

        }

    }

    // end F743-425.1

    /**
     * Returns the bean instance corresponding to the specified beanid,
     * if the bean is enlisted in this Transaction. <p>
     *
     * This method enables the ContainerTx to behave as a Transaction
     * scoped bean cache for the Activation Strategies, improving performance
     * by avoiding EJB Cache lookups. <p>
     *
     * @param beanId identifier of bean instance to be found
     *
     * @return bean instance, if enlisted in the Transaction;
     *         otherwise null.
     **/
    // d173022.4
    public BeanO find(BeanId beanId) {
        BeanO bean = null;

        if (beanOs != null) {
            bean = beanOs.get(beanId);
        }

        // If debug is enabled, go ahead and calculate some hit rate
        // metrics and print out whether found or not.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            ivTxCacheSearch++;

            if (bean != null) {
                ivTxCacheHits++;
                Tr.debug(tc, "Bean found in Transaction cache (Hit Rate:" +
                             ivTxCacheHits + "/" + ivTxCacheSearch + ")");
            } else {
                Tr.debug(tc, "Bean not in Transaction cache (Hit Rate:" +
                             ivTxCacheHits + "/" + ivTxCacheSearch + ")");
            }
        }

        return bean;
    }

    /**
     * Enlist the given <code>BeanO</code> in this container transaction.
     */
    protected boolean enlist(BeanO beanO) throws TransactionRolledbackException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.entry(tc, "enlist", new Object[] { Boolean.valueOf(globalTransaction), this, beanO }); //123293

        ensureActive();

        if (ivBeanOsEnlistedInTx == false) {
            initialize_beanO_vectors(); // d111555
        }

        boolean wasEnlisted = false;//d132828 avoid multiple returns

        //synchronized (beanOs) {  //d173022.2

        BeanO oldBeanO = beanOs.put(beanO.beanId, beanO);

        //d115597 begins

        if (oldBeanO == null) {

            // d140003.22 -- If CMP 2.0 bean, add bean to CMP 2.0 list for later use
            // Except, don't add ReadOnly/Interval beans to CMP 2.0 list; they
            // never need to be included in pre-find flush.               LI3408
            BeanMetaData bmd = beanO.home.beanMetaData;
            if (bmd.cmpVersion == InternalConstants.CMP_VERSION_2_X &&
                bmd.ivCacheReloadType == BeanMetaData.CACHE_RELOAD_NONE) {
                ArrayList<BeanO> bundle = homesForPMManagedBeans.get(beanO.home);

                if (bundle == null) { // here, value of null indicates no bundle in map
                    bundle = new ArrayList<BeanO>();
                    homesForPMManagedBeans.put(beanO.home, bundle);
                }

                bundle.add(beanO);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Added this BeanO to CMP20 structure");
            }

            afterList.add(beanO);//d115597 if new BeanO add to afterList
            if (origLock == false) {
                beanOList.add(beanO);//d115597 have not started beforeCompletion

            } else {
                setupTempLists();//d132828
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "sec enlist new : " + beanO);

                currentBeanOs.put(beanO.beanId, beanO);//d115597 new bean added once beforeCompletion called
                tempList.add(beanO); //d115597 add to the tempList
            }

            wasEnlisted = true;//d132828 avoid multiple returns

        } //new beanO
        else {
            // d115597 this bean is already in the tran, need to determine if this is a reenlistment
            if (origLock == true) {
                // d115597 this path is that beforeCompletion has started
                setupTempLists(); //d132828

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "enlist in beforeCompletion: putting in list");

                BeanO obO = currentBeanOs.put(beanO.beanId, beanO);

                if (obO == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Secondary enlist old : " + beanO);

                    // d115597 is this in the current list if not add?
                    tempList.add(beanO);
                } // d115597 not in current tran list

                // d115597 should return false
            } // d115597 not in original list of beanos (2nd or greater run through

        } // old beanO
          //d115597 end
          //}//synch end

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.exit(tc, "enlist : " + wasEnlisted); // d144064

        return wasEnlisted;// d132828 avoid multiple returns
    } // enlist

    /*
     * d132828 added this method to set up templists in one spot
     */
    private void setupTempLists() {
        //synchronized(this){   //d173022.2
        //d115597 if necessary create a new tempList
        // and list of currentBeanOs.  Always delay this until actually needed
        //d115597

        if (tempList == null) {
            tempList = new ArrayList<BeanO>();
            currentBeanOs = new HashMap<BeanId, BeanO>();//should release old list
        }
        //}
    }

    @Override
    public void registerSynchronization(Synchronization s) throws CPIException {
        try {
            ivContainer.uowCtrl.enlistWithTransaction(s);
        } catch (CSIException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".registerSynchronization",
                                        "1143", this);
            throw new CPIException(e.toString());
        }
    } // enlist

    /**
     * Remove the given <code>BeanO</code> in this container transaction.
     *
     * @return true if the specified BeanO was successfully removed from the
     *         transaction; returns false if the bean was not enlisted.
     */
    public boolean delist(BeanO beanO) throws TransactionRolledbackException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "delist",
                     new Object[] { Boolean.valueOf(globalTransaction), this, beanO });

        boolean removed = false; // d145697

        ensureActive();

        if (beanOs == null) { // d111555
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "delist : false : no beanOs");
            return removed;
        }

        //synchronized(beanOs)  //d173022.2
        //{
        // Can't delist after beforeCompletion                          d132828
        if (origLock == true) {
            if (isTraceOn && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Attempt to delist after beforeCompletion");
            throw new RuntimeException("Delisted after starting beforeCompletion");
        } else {
            // Only remove the BeanO from the transaction if it was already
            // enlisted. Note that only this specific BeanO is removed, not
            // just any BeanO that may have the same beanId.             d145697
            if (beanOList.contains(beanO)) {
                beanOs.remove(beanO.beanId);
                beanOList.remove(beanOList.indexOf(beanO));
                afterList.remove(afterList.indexOf(beanO));
                removed = true;

                // If CMP 2.0 bean, remove from CMP 2.0 list that is sorted
                // by home.... used for intelligent flush.             d140003.22
                if (beanO.home.beanMetaData.cmpVersion == InternalConstants.CMP_VERSION_2_X) {
                    ArrayList<BeanO> bundle = homesForPMManagedBeans.get(beanO.home);

                    bundle.remove(bundle.lastIndexOf(beanO));
                }
            }
        }
        //}

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.exit(tc, "delist : " + removed);

        return removed; // d145697
    } // delist

    /**
     * Get snapshot of all beans enlisted in current transaction.
     * Note, it is important to release the lock on the beanOs
     * hashtable, in case storing the bean's persistent state
     * causes a recursive call to this method (this is probably
     * bad, but at least we won't hang in the recursive call).
     */
    protected BeanO[] getAllEnlistedBeanOs() {
        if (beanOs == null)
            return null; // d111555

        BeanO result[];
        // d122418-5
        //synchronized(beanOs) {  //d173022.2
        // d140003.22 -- changed for less code, better performance
        result = beanOList.toArray(new BeanO[beanOList.size()]);
        //}

        return result;
    } // getBeanOs

    protected final void preInvoke(EJSDeployedSupport s) throws TransactionRolledbackException {
        ensureActive();

        // Save previous method's tx 'began' indication, to be restored in
        // postInvoke, and set current method's 'began' indication from
        // uowCookie (result of tx collaborator).  Note that if there is no
        // uowCookie, then the tx collaborator was not called (i.e. running
        // in Lightweight mode) and so a tx was obviously not begun.     LI3795-56
        s.previousBegan = began;
        if (s.uowCookie == null) // LI3795-56
            began = false;
        else
            began = s.uowCookie.beganTx();
        s.began = began; // d156688
    } // preInvoke

    /**
     * Set the isolation level for the current transacion and ensure
     * that it has not changed within the transaction.
     */
    protected void setIsolationLevel(int isolationLevel) throws IsolationLevelChangeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "current isolation level = "
                         + MethodAttribUtils.getIsolationLevelString(ivIsolationLevel)
                         + ", attempting to change to = "
                         + MethodAttribUtils.getIsolationLevelString(isolationLevel)); // PK31372
        }

        // Treat TRANSACTION_NONE as "don't care".  The current value may be
        // NONE because it hasn't been set yet, or because all the methods
        // invoked so far did not have an isolation level specified by
        // the customer (in the deployment descriptor).                 d107762
        if (ivIsolationLevel == java.sql.Connection.TRANSACTION_NONE) {
            ivIsolationLevel = isolationLevel;
        } else if (ivIsolationLevel != isolationLevel &&
                   isolationLevel != java.sql.Connection.TRANSACTION_NONE) {
            throw new IsolationLevelChangeException();
        }
    } // setIsolationLevel

    /**
     * Perform postinvoke processing for this global transaction. <p>
     */
    protected final void postInvoke(EJSDeployedSupport s) {
        began = s.previousBegan;
    } // postInvoke

    /**
     * Call flush on each beanO, first failure terminates. <p>
     *
     * @param theBeanOs         the BeanOs to flush
     * @param cmp2xBeansToFlush null to flush all beans, or non-null to only
     *                              flush a bean if it can be removed from the Set
     */
    protected void flush(BeanO[] theBeanOs, HashSet<BeanInstanceInfo> cmp2xBeansToFlush) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.entry(tc, "flush: num=" + (theBeanOs == null ? null : theBeanOs.length) +
                         ", cmp2x=" + (cmp2xBeansToFlush == null ? null : cmp2xBeansToFlush.size()));

        // Save the BeanOs processed in the following loop in an instance variable
        // so methods called from the customer callbacks can determine which beanO
        // is active on the thread.                                        d168509

        if (theBeanOs != null) {
            EJBThreadData threadData = EJSContainer.getThreadData(); // d630940
            boolean popCallbackBeanO = false;
            ComponentMetaData prevCMD = null;
            boolean resetCMD = false;
            ClassLoader prevClassLoader = null;
            Object origClassLoader = ThreadContextAccessor.UNCHANGED;

            try {
                for (int i = 0; i < theBeanOs.length; i++) {
                    BeanO beanO = theBeanOs[i];
                    BeanMetaData bmd = beanO.home.beanMetaData;

                    if (bmd.cmpVersion != InternalConstants.CMP_VERSION_2_X ||
                        cmp2xBeansToFlush == null ||
                        cmp2xBeansToFlush.remove(beanO)) {
                        threadData.pushCallbackBeanO(beanO); // d168509, d630940
                        popCallbackBeanO = true;

                        // Update the thread CMD if it's not the same as the
                        // previous bean and this is a bean-managed bean.
                        if (bmd != prevCMD &&
                            bmd.type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY) {
                            if (resetCMD) {
                                cmdAccessor.endContext();
                            }

                            cmdAccessor.beginContext(bmd);
                            prevCMD = bmd;
                            resetCMD = true;

                            // Update the class loader if it's not the same as the
                            // previous bean.                         PK51366, PK83186
                            ClassLoader classLoader = bmd.ivContextClassLoader; // F85059
                            if (classLoader != prevClassLoader) {
                                prevClassLoader = classLoader;
                                origClassLoader = EJBThreadData.svThreadContextAccessor.repushContextClassLoaderForUnprivileged(origClassLoader, classLoader);
                            }
                        }

                        try {
                            beanO.store();
                        } catch (RemoteException ex) {
                            FFDCFilter.processException(ex, CLASS_NAME + ".flush", "1505", this);
                            throw new CSIException("Problem storing an enlisted bean", ex);
                        }

                        popCallbackBeanO = false;
                        threadData.popCallbackBeanO();
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Excluded from flush: " + beanO);
                    }
                }
            } finally {
                EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(origClassLoader);

                if (resetCMD) {
                    cmdAccessor.endContext();
                }

                if (popCallbackBeanO) {
                    threadData.popCallbackBeanO();
                }
            }
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.exit(tc, "flush");
    } // flush

    /**
     * Flush the persistent state of all entity beans enlisted
     * in this transaction. <p>
     */
    protected void flush() throws CSIException {
        // This method is overridden in WASContainerTx to allow for Persistence
        // Manager logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    } // flush

    /**
     * Return true iff this transaction was created by the container when
     * the bean method call currenly in progress arrived.
     */
    @Override
    public final boolean beganInThisScope() {
        return began;
    }

    /**
     * Return isolation level in effect for this transaction. <p>
     */
    public final int getIsolationLevel() {
        return ivIsolationLevel;
    }

    /**
     * Mark this transaction for rollback, if an immediate rollback is
     * required, contact the uowCtrl and rollback the transaction.
     * This method only marks the transaction for rollback, the actual
     * rollback will be driven in the beforeCompletion callback
     */
    public final void setRollbackOnly() {
        markedRollbackOnly = true;
        ivContainer.uowCtrl.setRollbackOnly(); //LIDB1673.2.1.5
    }

    /**
     * Return true iff this transaction is marked for rollback or has
     * already been rolled back (returns true iff setRollbackOnly was
     * called on this ContainerTx instance)
     */
    public final boolean getRollbackOnly() {
        return markedRollbackOnly;
    }

    /**
     * Return true iff this transaction is marked for rollback or has
     * already been rolled back (queries JTS for the answer)
     */
    protected final boolean getGlobalRollbackOnly() {
        return ivContainer.uowCtrl.getRollbackOnly();
    }

    //------------------------------------
    //
    // Methods from the Locker interface
    //
    //------------------------------------

    /**
     * A ContainerTx is not a real lock implementation.
     */
    @Override
    public boolean isLock() {
        return false;
    } // isLock

    /**
     * Return the lock mode for the given lock. The lock name is the
     * bean id of a bean enlisted with this transaction. <p>
     *
     * To satisfy this request, find the EntityBeanO in this transaction
     * associated with the given bean id and delegate the call to it. <p>
     *
     * Do not try to activate bean here, this transaction may already have
     * been enlisted by a beanO. The beanO should be active in this
     * situation. Problems will arise if the activation tries to reenlist
     * in this transaction since its state is unknown.
     *
     * This call is made when the lock needs to be upgraded from a proxy
     * to a full Lock object. At that point a determination needs to be
     * made on the locking mode of the holder of the proxy lock. No
     * assumptions can be made on the holder's state.
     */
    @Override
    public int getLockMode(Object lockName) {
        BeanId beanId = (BeanId) lockName;

        // When the number of bean instances is limited, a lock will be
        // obtained for the home bean id to block creation of additional
        // instances... this is an EXCLUSIVE lock.                         PK20648
        if (beanId.isHome()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "getLockMode: locking home bean: EXCLUSIVE");
            return LockManager.EXCLUSIVE;
        }

        EntityBeanO beanO = null;

        beanO = (EntityBeanO) ivContainer.activator.getBean(this, beanId);

        if (beanO == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) // d144064
                Tr.event(tc, "getLockMode: failed to get bean");
            return LockManager.EXCLUSIVE;
        }

        return beanO.getLockMode();

    } // getLockMode

    // LIDB441.5
    /**
     * Get the container's activity session
     */
    //139562-5.EJBC - made final and moved code to CTOR.
    final public ContainerAS getContainerAS() {
        return containerAS;
    }

    // d425046 allow containerAS to be set separately
    final protected void setContainerAS(ContainerAS cas) {
        containerAS = cas;
    }

    //110799
    public ContainerTx getCurrentTx() throws CSITransactionRolledbackException {
        return ivContainer.getCurrentTx(false);//d171654
    }

    //110799

    //d126930.3
    @Override
    public void setCompleting(boolean isCompleting) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setCompleting= " + isCompleting);

        isActivitySessionCompleting = isCompleting;
        //d138562.11
        if (finderSyncList != null) {
            int numberofCallers = finderSyncList.size();
            for (int i = 0; i < numberofCallers; ++i) {
                ((ContainerSynchronization) finderSyncList.get(i)).setCompleting(isCompleting);
            }
        } // d173022.12

        if (txListener != null)
            try {
                //txListener.afterCompletion(); d153430
                txListener.setCompleting(isCompleting); //d153430
            } catch (Throwable e) {
                FFDCFilter.processException(e, CLASS_NAME + ".afterCompletion",
                                            "1733", this);
                throw new RuntimeException("txListener exception" + e.toString());
            }
    }

    /*
     * Pre-condition: The Synchronization object being enlisted
     * must implement ContainerSynchronization interface
     *///d139562.11
    public void enlistContainerSync(Synchronization s) throws CPIException {
        if (!(s instanceof ContainerSynchronization)) {
            throw new CPIException("Must implement ContainerSynchronization interface");
        }
        if (finderSyncList == null) {
            finderSyncList = new ArrayList<Synchronization>(); // d173022.12
        } // d173022.12
        finderSyncList.add(s);
    }

    /**
     * d112604.1
     *
     * Returns a boolean representing the fast that a CMP11 BeanO flush in progress. <p>
     *
     */
    public boolean getCMP11FlushActive() {
        return (ivCMP11FlushActive);
    }

    /**
     * Called when the object is no longer in use. This is a way to clear
     * the internal state, so it does not hold too many resources in case
     * a reference is held to it beyond the life fo the transaction. <p>
     *
     * Formerly called 'OnReturnToPool', but pooling this object did not
     * provice a benifit, so it has been renamed not that it clears the
     * state rather than re-initializing it. <p>
     */
    // d154342.10 d215317
    protected void releaseResources() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "releaseResources : State = " + stateStrs[state]);

        // If the state is not complete, then the ContainerTx should not
        // be cleared, and should not be returned to the pool.  This is common
        // for BMT, where a method may begin the tx, but not complete it
        // until a subsequent method call.                                 d157262
        if (state != COMMITTED && state != ROLLEDBACK) {
            return;
        }

        // Simply clear all instance variables that may hold a reference to
        // another object.                                                 d215317

        afterList = null;
        beanOList = null;
        beanOs = null;
        cmdAccessor = null;
        containerAS = null;
        currentBeanOs = null;
        finderSyncList = null;
        homesForPMManagedBeans = null;
        ivContainer = null;
        ivPostProcessingException = null; // PQ90221
        ivTxKey = null;
        tempList = null;
        txListener = null;
        uowCtrl = null;
        timersQueuedToStart = null; // F743-425.CodRev
        timersCanceled = null; // F743-425.CodRev
    }

    /**
     * Returns void and instead writes directly to IncidentStream to avoid large string buffers.<p>
     *
     * Used by the EJBContainerDiagnosticModule instead of toString().
     * The information returned is way too much information for toString(),
     * which is used by trace. <p>
     */
    // d137957 PK98076
    public void ffdcDump(IncidentStream is) {
        introspect(new IncidentStreamWriter(is));
    }

    /**
     * Writes the important state data of this class, in a readable format,
     * to the specified output writer. <p>
     *
     * @param writer output resource for the introspection data
     */
    // F86406
    public void introspect(IntrospectionWriter writer) {
        // -----------------------------------------------------------------------
        // Indicate the start of the dump, and include the toString()
        // of ContainerTx, so this can easily be matched to a trace.
        // -----------------------------------------------------------------------
        writer.begin("Start ContainerTx Dump ---> " + this);

        // -----------------------------------------------------------------------
        // Dump the basic state information about the ContainerTx.
        // -----------------------------------------------------------------------
        writer.println("Tx Key                  = " + ivTxKey);
        writer.println("State                   = " + stateStrs[state]);
        writer.println("Entered beforCompletion = " + origLock);
        writer.println("Marked Rollback Only    = " + markedRollbackOnly);
        writer.println("Method Began            = " + began);
        writer.println("Isolation Level         = " +
                       MethodAttribUtils.getIsolationLevelString(ivIsolationLevel));

        if (!isActivitySessionCompleting) {
            writer.println("isActivitySessionCompleting = " + isActivitySessionCompleting);
        }

        // -----------------------------------------------------------------------
        // Dump the beans enlisted in the transaction.
        // -----------------------------------------------------------------------

        int numEnlisted = (beanOList == null) ? 0 : beanOList.size();

        writer.begin("Enlisted Beans : " + numEnlisted);
        for (int i = 0; i < numEnlisted; ++i) {
            writer.println(beanOList.get(i).toString());
        }
        writer.end();

        // -----------------------------------------------------------------------
        // Dump the AccessIntent cache for the transaction.
        // -----------------------------------------------------------------------

        introspectAccessIntent(writer);

        // -----------------------------------------------------------------------
        // ContainerTx dump is complete.
        // -----------------------------------------------------------------------

        writer.end();
    }

    protected void introspectAccessIntent(IntrospectionWriter writer) {
        // This method is overridden in WASContainerTx to allow for AccessIntent
        // logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    }

    // d165585 Begins
    /**
    */
    protected String getTxId() {
        String rtnStr = null;
        if (ivTxKey != null) {
            if (globalTransaction) {
                int idx;
                rtnStr = ivTxKey.toString();
                rtnStr = (rtnStr != null) ? (((idx = rtnStr.indexOf("#")) != -1) ? rtnStr.substring(idx + 5) : rtnStr) : "NoTx";
            } else {
                rtnStr = Integer.toHexString(System.identityHashCode(ivTxKey));
            }
        }
        return rtnStr;
    }

    // d165585 Ends

    /**
     * Returns the UOW ID in a String format suitable for tracing. <p>
     *
     * This method is provided so that tx id trace points will be
     * consistent throughout EJB Container. <p>
     *
     * @param uowId The Unit Of Work Identififer (i.e. tx id).
     **/
    // LI3795-56
    protected static String uowIdToString(Object uowId) {
        String tidStr = null;
        if (uowId != null) {
            if (uowId instanceof LocalTransactionCoordinator) {
                tidStr = "tid=" + Integer.toHexString(uowId.hashCode()) + "(LTC)";
            } else {
                int idx;
                tidStr = uowId.toString();
                if ((idx = tidStr.lastIndexOf("#")) != -1)
                    tidStr = tidStr.substring(idx + 1);
            }
        }
        return tidStr;
    }

    /**
     * Return boolean true if there is an active BeanManaged Transaction
     * currently associated with the calling thread.
     */
    // d167937 - added entire method
    boolean isBmtActive(EJBMethodInfoImpl methodInfo) {
        return uowCtrl.isBmtActive(methodInfo);
    }

    /**
     * Overridden for trace; to include transaction id.
     **/
    // LI3795-56
    @Override
    public String toString() {
        String toString = "ContainerTx@" + Integer.toHexString(hashCode());
        if (ivTxKey != null) {
            if (globalTransaction)
                toString += "#tid=" + getTxId();
            else
                toString += "#tid=" + getTxId() + "(LTC)";
        } else
            toString += "#NoTx";

        return toString;
    }

    public boolean isFlushRequired() {
        return ivFlushRequired;
    }

} // ContainerTx
