/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.Map;

import javax.transaction.Status;
import javax.transaction.TransactionRolledbackException;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;
import com.ibm.ws.uow.embeddable.SystemException;
import com.ibm.ws.uow.embeddable.UOWToken;

/**
 * Class CallbackContextHelper is used as a helper by the BeanO classes
 * when the bean is in a EJB 3 module. The BeanO typically use an instance
 * of this class to suspend/resume the current UOW (either ActivitySession,
 * local transaction, or global transaction) and to begin a local TX that
 * is used by WebSphere as a "unspecified TX context". Typically the BeanO
 * needs this helper instance so that all of the EJB 3 lifecycle callback
 * event methods (PostConstruct, PostActivate, PrePassivate, PreDestroy)
 * are executed in a unspecified TX context as required by EJB 3 core
 * specification. The method annotated as an Init method of a EJB 3 POJO
 * stateful session bean also must execute in a unspecified TX context.
 * Note, a session bean or MDB that is code to the EJB 2.1 or earlier APIs
 * that are in a EJB 3 module also require the container callback methods to
 * execute in a unspecified context. The ejbCreate, ejbActivate, ejbPassivate,
 * ejbRemove are all considered lifecycle callback event methods or an Init
 * method in the case of ejbCreate of a SFSB.
 */
class CallbackContextHelper {
    private static final String CLASS_NAME = CallbackContextHelper.class.getName();
    private static final TraceComponent tc = Tr.register(CallbackContextHelper.class, "EJBContainer", "com.ibm.ejs.container.container");

    enum Contexts {
        /** Push the callback bean. */
        CallbackBean,
        /** Push all contexts. */
        All,
    }

    enum Tx {
        /** Begin an LTC. */
        LTC,
        /** Begin an LTC if the module is EJB 3+, or else do nothing. */
        CompatLTC,
        /** Begin a global transaction. */
        Global,
    }

    /**
     * The BeanO for which this context helper was created.
     */
    private final BeanO ivBeanO;

    /**
     * UOWToken of suspended TX or null if no TX is suspended. //d528073
     */
    private UOWToken ivUowToken;

    /**
     * LocalTransactionCurrent used to begin a local TX for the
     * unspecified TX context.
     */
    protected LocalTransactionCurrent ivLocalTransactionCurrent;

    /**
     * TransactionManager used to begin a global TX for the context.
     */
    protected EmbeddableWebSphereTransactionManager ivTransactionManager;

    /**
     * The EJB thread data associated with the thread currently managing this
     * context helper.
     */
    private EJBThreadData ivThreadData; // d630940

    /**
     * Non-null if contexts need to be popped.
     */
    private Contexts ivPopContexts;

    /**
     * Saved {@link EJBThreadData#ivLifecycleMethodContextIndex}.
     */
    private int ivSavedLifecycleMethodContextIndex; // d644886

    /**
     * Saved {@link EJBThreadData#ivLifecycleMethodBeginTx}.
     */
    private Tx ivSavedLifecycleMethodBeginTx;

    /**
     * Saved {@link EJBThreadData#ivLifecycleContextData}.
     */
    private Map<String, Object> ivSavedLifecycleContextData; // d664886

    /**
     * Create a new object with the specified EJSContainer. The UOWManager
     * associated with the container will be used for suspending/resuming TX
     * currently associated with calling thread.
     */
    CallbackContextHelper(BeanO beanO) {
        ivBeanO = beanO;
    }

    /**
     * Suspend the UOW (either ActivitySession, local transaction, or
     * global transaction) currently associated with a thread and begin a new
     * transaction, and push contexts as required.
     *
     * @param beginTx      the transaction context to begin
     * @param pushContexts the contexts to begin
     * @throws CSIException if an exception occurs while beginning the LTC
     */
    public void begin(Tx beginTx, Contexts pushContexts) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "begin: tx=" + beginTx + ", contexts=" + pushContexts);

        ivThreadData = EJSContainer.getThreadData(); // d630940
        if (pushContexts == Contexts.All) {
            ivThreadData.pushContexts(ivBeanO);
        } else {
            ivThreadData.pushCallbackBeanO(ivBeanO);
        }
        ivPopContexts = pushContexts;

        ivSavedLifecycleContextData = ivThreadData.ivLifecycleContextData; // d644886
        ivThreadData.ivLifecycleContextData = null; // d644886

        ivSavedLifecycleMethodContextIndex = ivThreadData.ivLifecycleMethodContextIndex; // d644886
        ivThreadData.ivLifecycleMethodContextIndex = ivThreadData.getNumMethodContexts(); // d644886, RTC102449

        if (beginTx == Tx.CompatLTC) {
            beginTx = ivBeanO.home.beanMetaData.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0 ? Tx.LTC : null;
        }
        ivSavedLifecycleMethodBeginTx = ivThreadData.ivLifecycleMethodBeginTx;
        ivThreadData.ivLifecycleMethodBeginTx = beginTx;

        if (beginTx != null) {
            // This duplicates logic from TranStrategy.

            if (ivUowToken != null) {
                throw new CSIException("Cannot begin until prior TX is resumed");
            }

            // Get UOWCurrent from TransactionManagerFactory.
            UOWCurrent uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent(); //d632706

            // Only suspend if there is a UOWCurrent.
            if (uowCurrent != null) {
                // Suspend the current UOW.
                try {
                    ivUowToken = ivBeanO.container.ivUOWManager.suspend(); // d578360
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Suspended TX/LTC cntxt: " + ivUowToken);
                    }
                } catch (SystemException e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".begin", "140", this);
                    throw new CSIException("Cannot begin due to failure in suspend.", e);
                }
            }

            boolean ltc = beginTx == Tx.LTC;
            if (ltc) {
                // Begin a new local TX for the unspecified TX context.
                ivLocalTransactionCurrent = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
                ivLocalTransactionCurrent.begin();

                if (isTraceOn && tc.isEventEnabled()) {
                    LocalTransactionCoordinator lCoord = ivLocalTransactionCurrent.getLocalTranCoord();
                    if (lCoord != null) {
                        Tr.event(tc, "Began LTC cntxt: tid=" +
                                     Integer.toHexString(lCoord.hashCode()) + "(LTC)");
                    } else {
                        Tr.event(tc, "Began LTC cntxt: " + "null Coordinator!");
                    }
                }
            } else {
                try {
                    ivTransactionManager = EmbeddableTransactionManagerFactory.getTransactionManager();
                    ivTransactionManager.begin();

                    if (isTraceOn && tc.isEventEnabled())
                        Tr.event(tc, "Began TX cntxt: " + ivTransactionManager.getTransaction()); //LIDB1673.2.1.5
                } catch (Exception ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".begin", "214", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Begin global tx failed", ex);
                    throw new CSIException("Begin global tx failed", ex);
                }
            }

            // d630940 - Make the container aware of the new transaction.
            ivBeanO.container.getCurrentTx((SynchronizationRegistryUOWScope) getUOWCoordinator(), ltc);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "begin");
    }

    /**
     * Complete the UOW that was started by the begin method of this class
     * and and resume any UOW that was suspended by the begin method.
     *
     * @param commit must be true if and only if you want the unspecified TX
     *                   started by begin method to be committed. Otherwise, the
     *                   unspecified TX is rolled back.
     *
     * @throws CSIException is thrown if any exception occurs when trying to
     *                          complete the unspecified TX that was started by suspend method.
     */
    void complete(boolean commit) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "complete called with commit argument set to: " + commit);
        }

        if (ivPopContexts != null) {
            if (ivPopContexts == Contexts.All) {
                ivThreadData.popContexts();
            } else {
                ivThreadData.popCallbackBeanO(); // d630940
            }

            ivThreadData.ivLifecycleContextData = ivSavedLifecycleContextData; // d644886
            ivThreadData.ivLifecycleMethodContextIndex = ivSavedLifecycleMethodContextIndex; // d644886
            ivThreadData.ivLifecycleMethodBeginTx = ivSavedLifecycleMethodBeginTx;
            ivThreadData = null;
        }

        try {
            // This duplicates logic from TranStrategy.

            // Complete the UOW started by the begin method.
            if (ivTransactionManager != null) {
                if (commit) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Committing TX cntxt: " + ivTransactionManager.getTransaction());

                    // Was the transaction marked for rollback?
                    int status = ivTransactionManager.getStatus();
                    if (status == Status.STATUS_MARKED_ROLLBACK ||
                        status == Status.STATUS_ROLLING_BACK ||
                        status == Status.STATUS_ROLLEDBACK) {
                        // If the transaction was marked for rollback because it
                        // timed out, then rollback and rethrow the timeout.
                        try {
                            ivTransactionManager.completeTxTimeout();
                        } catch (TransactionRolledbackException e) {
                            ivTransactionManager.rollback();
                            throw e;
                        }

                        // Otherwise, rollback silently.
                        ivTransactionManager.rollback();
                    } else {
                        ivTransactionManager.commit();
                    }
                } else {
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Rolling back TX cntxt due to bean exception: " +
                                     ivTransactionManager.getTransaction());
                    }
                    ivTransactionManager.rollback();
                }
            } else if (ivLocalTransactionCurrent != null) {
                int endMode = (commit) ? LocalTransactionCurrent.EndModeCommit : LocalTransactionCurrent.EndModeRollBack;

                if (isTraceOn && tc.isEventEnabled()) {
                    LocalTransactionCoordinator lCoord = ivLocalTransactionCurrent.getLocalTranCoord();
                    if (lCoord != null)
                        Tr.event(tc, "Completing LTC cntxt: tid=" +
                                     Integer.toHexString(lCoord.hashCode()) + "(LTC)");
                    else
                        Tr.event(tc, "Completing LTC cntxt: " + "null Coordinator!");
                }

                // Use end rather than complete to properly apply resolver action
                ivLocalTransactionCurrent.end(endMode);
            }
        } catch (Throwable t) {
            //FFDCFilter.processException( t, CLASS_NAME + ".complete", "168", this );
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "unspecified UOW completion failure: " + t, t);
            }
            throw new CSIException("unspecified UOW completion failure: " + t, t);
        } finally {
            // Ensure suspended UOW is resumed.
            if (ivUowToken != null) {
                if (isTraceOn) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "complete is resuming UOW with UOWToken = " + ivUowToken);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Resuming TX/LTC cntxt: " + ivUowToken);
                }
                UOWToken token = ivUowToken;
                ivUowToken = null;

                try {
                    ivBeanO.container.ivUOWManager.resume(token); // d578360
                } catch (SystemException e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".complete", "216", this);

                    throw new CSIException("Cannot complete due to failure in resume.", e);
                }

            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "complete");
        }
    }

    /**
     * Get the UnitOfWorkCoordinator from the TransactionManagerFactory.
     *
     * @return UOWCoordinator.
     */
    //456222
    private UOWCoordinator getUOWCoordinator() {
        UOWCoordinator uowCoord = null;
        UOWCurrent uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();
        if (uowCurrent != null) {
            uowCoord = uowCurrent.getUOWCoord();
        }
        return uowCoord;

    }

    public void resetContextData() {
        ivThreadData.ivLifecycleContextData = null;
    }

}
