/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.transaction.Synchronization;

import com.ibm.websphere.cpi.CPIException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.LocalTransaction.ContainerSynchronization;

/**
 * A ContainerAS manages all container resources associated with a
 * given ActivitySession.
 * 
 * Note, the lifecycle of this object guarantees that there will be
 * exactly one instance in a container for each ActivitySession the
 * container is associated with. This means that the default Object
 * hashCode() and equals() method implementations work correctly for
 * instances of ContainerAS.
 * 
 * Also, like transactions, an activity session may only be active on a single
 * thread, so synchronization of methods is not needed for this class.
 */

public class ContainerAS
                implements Synchronization,
                ContainerSynchronization //d126930.3
{
    private static final String CLASS_NAME = ContainerAS.class.getName();

    private static final TraceComponent tc = Tr.register(ContainerAS.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * Various states for ContainerAS
     * 
     * State Table:
     * 
     * Current State Event Action New State
     * Active beforeCompletion notify current containerTx Preparing
     * Active afterCompletion IllegalStateException
     * Preparing beforeCompletion IllegalStateException
     * Preparing afterCompletion passivate beans
     */
    private static final int ACTIVE = 0;
    private static final int PREPARING = 1;

    private static final String[] stateStrs = { "Active", "Preparing" };

    /**
     * Container this <code>ContainerAS</code> is associated with. <p>
     */
    private EJSContainer ivContainer;

    /**
     * List of resources (BeanOs) that have been enlisted with this
     * container activity session. <p>
     */
    private HashMap<BeanId, BeanO> ivBeanOs;

    /**
     * An ordered list of the BeanOs that have been enlisted with this
     * container activity session. <p>
     * Both the HashMap above and this list maintain the same information
     * This duplicate information may seem wasteful, but performance will
     * be better with this arrangement (space/time).
     */
    private ArrayList<BeanO> ivBeanOList;

    /**
     * The current state of this ContainerAS
     */
    private int ivState;

    /**
     * ActivitySession associated with this container activity session.
     */
    private Object ivASKey;

    /** Number of ActivitySession scoped cache hits. **/
    private long ivASCacheHits = 0; // LI3408

    /** Number of finds/searches of the ActivitySession scoped cache. **/
    private long ivASCacheSearch = 0; // LI3408

    /**
     * Transition to the PREPARING state
     */
    private final void becomePreparing()
    {
        if (ivState != ACTIVE) {
            throw new IllegalStateException(stateStrs[ivState]);
        }
        ivState = PREPARING;
    }

    /**
     * Check to make sure that this ContainerAS is in the ACTIVE state
     */
    private final void ensureActive()
    {
        switch (ivState)
        {
            case ACTIVE:
                break;

            default:
                throw new IllegalStateException(stateStrs[ivState]);
        }
    }

    /**
     * Create a new <code>ContainerAS</code> instance. <p>
     */
    public ContainerAS(EJSContainer container, Object asKey)
    {
        this.ivContainer = container;
        this.ivASKey = asKey;
        ivBeanOs = new HashMap<BeanId, BeanO>();
        ivBeanOList = new ArrayList<BeanO>();
        ivState = ACTIVE;
    }

    /**
     * This method is called before the activity session this
     * <code>ContainerAS</code> is associated with is completed
     * (reset or completed). <p>
     */
    public void beforeCompletion()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "beforeCompletion");
        becomePreparing(); // Change the state to PREPARRING
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "beforeCompletion");
    }

    /**
     * This method is called after the ActivitySession associated with
     * this <code>ContainerAS</code> has completed.
     */
    public void afterCompletion(int status)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.entry(tc, "afterCompletion (" + status + ") : " + this);

        try
        {
            // inform the activator that the session has ended
            // so the beans can be passivated if necessary
            BeanO[] beanOs = getBeanOs();
            EJBKey[] ejbKeys = getEJBKeys(beanOs);
            if (beanOs != null) {
                for (int i = 0; i < beanOs.length; ++i) {
                    BeanO beanO = beanOs[i];
                    try {
                        ivContainer.activator.unitOfWorkEnd(this, beanO);
                    } catch (Throwable ex) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".afterCompletion",
                                                    "214", this);
                        if (isTraceOn && tc.isEventEnabled()) // d144064
                            Tr.event(tc,
                                     "Exception thrown in afterCompletion()",
                                     new Object[] { beanO, ex });
                    }
                }
            }
            // inform the unit of controller the session has ended
            // so references that are no longer needed can be released
            ivContainer.uowCtrl.sessionEnded(ejbKeys);

            //d425046 Ensure ContainerAS not available now
            ContainerTx tx = ivContainer.getCurrentTx(false);
            if (tx != null)
            {
                tx.setContainerAS(null); // prevent activation strategy finding
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "removed ContainerAS during afterCompletion");
            }

        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".afterCompletion", "219", this);
            if (isTraceOn && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Exception during afterCompletion", t);
            throw new RuntimeException(t.toString());
        } finally {
            ivContainer.containerASCompleted(ivASKey);
        }

        if (isTraceOn && tc.isEntryEnabled()) // d144064
            Tr.exit(tc, "afterCompletion");
    } // afterCompletion

    /**
     * Returns the bean instance corresponding to the specified beanId,
     * if the bean is enlisted in this ActivitySession. <p>
     * 
     * This method enables the ContainerAS to behave as a ActivitySession
     * scoped bean cache for the Activation Strategies, improving performance
     * by avoiding EJB Cache lookups. <p>
     * 
     * @param beanId identifier of bean instance to be found
     * 
     * @return bean instance, if enlisted in the ActivitySession;
     *         otherwise null.
     **/
    // LI3408
    public BeanO find(BeanId beanId)
    {
        BeanO bean = ivBeanOs.get(beanId);

        // If debug is enabled, go ahead and calculate some hit rate
        // metrics and print out whether found or not.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            ivASCacheSearch++;

            if (bean != null)
            {
                ivASCacheHits++;
                Tr.debug(tc, "Bean found in ActivitySession cache (Hit Rate:" +
                             ivASCacheHits + "/" + ivASCacheSearch + ")");
            }
            else
            {
                Tr.debug(tc, "Bean not in ActivitySession cache (Hit Rate:" +
                             ivASCacheHits + "/" + ivASCacheSearch + ")");
            }
        }

        return bean;
    }

    /**
     * Returns true if the specified BeanId is enlisted in the activity session. <p>
     * 
     * @param beanId identifier of bean instance to be found
     */
    // d655854
    public boolean isEnlisted(BeanId beanId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isEnlisted : " + (ivBeanOs.get(beanId) != null));

        return ivBeanOs.get(beanId) != null;
    }

    /**
     * Enlist the given <code>BeanO</code> in this activity session.
     */
    public boolean enlist(BeanO beanO)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "enlist : " + beanO);
        ensureActive();
        BeanO oldBeanO = ivBeanOs.put(beanO.beanId, beanO);
        if (oldBeanO == null) {
            ivBeanOList.add(beanO);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "enlist : true");
            return true;
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "enlist : false");
        return false;
    } // enlist

    /**
     * Delist the given <code>BeanO</code> from this activity session.
     */
    public void delist(BeanO beanO)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "delist : " + beanO);
        ensureActive();
        ivBeanOs.remove(beanO.beanId); // d130022
        ivBeanOList.remove(beanO); // d130022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "delist : " + beanO);
    } // delist

    /**
     * Register the synchronization object with this activity session
     */
    public void registerSynchronization(Synchronization s) throws CPIException
    {
        try {
            ivContainer.uowCtrl.enlistWithSession(s);
            // enlistSession(s)
        } catch (CSIException e) {
            throw new CPIException(e.toString());
        }
    } // enlist

    /**
     * Get snapshot of all EJBKeys associated with the beans involved in current
     * activity session
     */
    private EJBKey[] getEJBKeys(BeanO[] beans)
    {
        EJBKey result[] = null;
        if (beans != null) {
            result = new EJBKey[beans.length];
            for (int i = 0; i < beans.length; ++i) {
                result[i] = beans[i].getId();
            }
        }
        return result;
    }

    /**
     * Get snapshot of all beans involved in current activity session
     */
    private BeanO[] getBeanOs()
    {
        BeanO result[];
        result = new BeanO[ivBeanOs.size()];
        Iterator<BeanO> iter = ivBeanOs.values().iterator();
        int i = 0;
        while (iter.hasNext()) {
            result[i++] = iter.next();
        }
        return result;
    }

    /**
     * Return boolean true if there is an active BeanManaged ActivitySession
     * currently associated with the calling thread.
     */
    // LIDB2018-1 added entire method
    boolean isBmasActive(EJBMethodInfoImpl methodInfo)
    {
        return ivContainer.uowCtrl.isBmasActive(methodInfo);
    }

    public static ContainerAS getContainerAS(ContainerTx tx)
    {
        ContainerAS as = null;
        try {
            if (tx != null) {
                as = tx.getContainerAS();
                return as;
            }
            as = ContainerAS.getCurrentContainerAS();
        } catch (CSIException ce) {
            FFDCFilter.processException(ce, CLASS_NAME + ".getContainerAS", "402");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Exception thrown getting ContainerAS", ce);
        }
        return as;
    }

    public static ContainerAS getCurrentContainerAS()
                    throws CSIException, CSITransactionRolledbackException
    {
        EJSContainer container = EJSContainer.getDefaultContainer();
        ContainerAS cas = null;
        cas = container.getCurrentSessionalUOW(false); // d348420
        return cas;
    }

    // d126930.3

    public void setCompleting(boolean isCompleting)
    {
        // This method is a no-op.  The ContainerSynchronization interface provides
        // a marker for the ActivtySession service to make sure that afterCompletion
        // is called on the EJBContainer's ContainerAS (ie. a sync object) only after
        // all other sync objects enlisted in the activitySession have been driven.
        // This is done to guarantee that passivation of EJBs is the very last thing
        // to occur when an ActivitySession ends.
    }

    /**
     * Dump the internal state of this <code>ContainerAS</code>.
     */
    public void dump()
    {
        Tr.dump(tc, "-- ContainerAS Dump --", new Object[] { this, ivBeanOs });
    }

}
