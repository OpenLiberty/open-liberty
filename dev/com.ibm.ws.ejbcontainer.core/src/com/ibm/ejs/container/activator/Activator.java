/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.DuplicateKeyException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerAS;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBThreadData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.HomeInternal;
import com.ibm.ejs.container.StatefulBeanReaper;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.container.util.locking.LockTable;
import com.ibm.ejs.util.cache.Cache;
import com.ibm.ejs.util.cache.Element;
import com.ibm.websphere.csi.CacheElement;
import com.ibm.websphere.csi.DiscardStrategy;
import com.ibm.websphere.csi.EJBCache;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ejbcontainer.diagnostics.TrDumpWriter;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.util.cache.DiscardWithLockStrategy;

/**
 * <code>Activator</code> provides the EJB Lifecycle functions to the {@link EJSContainer EJB Container} and manages the EJB Cache(s). <p>
 * 
 * <code>Activator</code> provides a single set of lifecycle interfaces
 * for the {@link EJSContainer EJB Container} to use as it drives all
 * bean types through their lifecycles. <code>Activator</code> redirects
 * the lifecycle method calls to the appropriate {@link ActivationStrategy} as defined by the beans {@link EJSHome Home}. <p>
 * 
 * See the {@link com.ibm.ejs.container.activator} package overview
 * for additional information about the activation strategies. <p>
 * 
 * <code>Activator</code> also encapsulates all of the customization of {@link com.ibm.ejs.util.cache.Cache} required to achieve the correct
 * behavior for the EJB Cache. Currently, <code>Activator</code> configures
 * its cache with the {@link com.ibm.ejs.util.cache.BackgroundLruEvictionStrategy}.
 * <code>Activator</code> itself implements {@link DiscardStrategy},
 * and delegates to the appropriate {@link EJSHome Home} instance to
 * passivate the bean. <p>
 * 
 * A single <code>Activator</code> is used by <code>EJSContainer</code>
 * to manage all configured EJB Caches. However, that activator will
 * only directly manage the default EJB Cache. It will create
 * subordinate activators to manage the user configured caches. <p>
 * 
 * Since each subordinate activator contains its own instance of the
 * activation strategies, when the base activator redirects the lifecycle
 * methods based on the activation strategy in the EJSHome, the
 * redirect will go to the correct EJB Cache. <p>
 * 
 * See the {@link com.ibm.ejs.container} package overview
 * for additional information about the EJB Cache. <p>
 * 
 * @see com.ibm.ejs.util.cache.Cache
 * @see com.ibm.ejs.util.cache.EvictionStrategy
 * @see com.ibm.websphere.csi.DiscardStrategy
 * @see com.ibm.websphere.csi.FaultStrategy
 * @see com.ibm.ejs.util.cache.BackgroundLruEvictionStrategy
 * @see UncachedActivationStrategy
 * @see StatefulActivateOnceActivationStrategy
 * @see StatefulActivateTranActivationStrategy
 **/

public final class Activator implements DiscardWithLockStrategy
{
    //
    // Constants
    //

    // Ids for ActivationStrategy factory interface

    public static final int UNCACHED_ACTIVATION_STRATEGY = 0;
    public static final int STATEFUL_ACTIVATE_ONCE_ACTIVATION_STRATEGY = 1;
    public static final int STATEFUL_ACTIVATE_TRAN_ACTIVATION_STRATEGY = 2;
    public static final int STATEFUL_ACTIVATE_SESSION_ACTIVATION_STRATEGY = 3; // LIDB441.5
    public static final int OPTA_ENTITY_ACTIVATION_STRATEGY = 4;
    public static final int OPTB_ENTITY_ACTIVATION_STRATEGY = 5;
    public static final int OPTC_ENTITY_ACTIVATION_STRATEGY = 6;
    public static final int ENTITY_SESSIONAL_TRAN_ACTIVATION_STRATEGY = 7; // LIDB441.5
    public static final int READONLY_ENTITY_ACTIVATION_STRATEGY = 8; // LI3408
    private static final int NUM_STRATEGIES = 9;

    /**
     * This table translates the strategy into a printable string.
     */
    // LI3408
    protected static final String svStrategyStrs[] =
    {
     "Uncached", // 0
     "Stateful Once", // 1
     "Stateful Transaction", // 2
     "Stateful Session", // 3
     "Entity Option A", // 4
     "Entity Option B", // 5
     "Entity Option C", // 6
     "Entity Option C Sessional", // 7
     "Entity Read Only" // 8
    };

    //
    // Construction
    //

    /**
     * Construct the default Activator, which manages all EJB Caches
     * for the EJB Container. It directly manages the default EJB Cache
     * and indirectly manages the user configured EJB Caches through
     * subordinate Activators. <p>
     * 
     * @param container EJB Container using this Activator
     * @param cache Default EJB Cache
     * @param passivationPolicy passivation policy to be used
     * @param passivator passivator to be used to passivate stateful beans
     * @param failoverCache is the failover cache object for this server or null
     *            if SFSB failover is disabled.
     **/
    public Activator(EJSContainer container, EJBCache cache,
                     PassivationPolicy passivationPolicy,
                     StatefulPassivator passivator,
                     SfFailoverCache failoverCache) //LIDB2018-1
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init>",
                     new Object[] { container, cache, passivationPolicy });

        ScheduledExecutorService deferrableExecutor = container.getEJBRuntime().getDeferrableScheduledExecutorService();

        // LIDB2775-23.4 Begins
        // Create the StatefulBeanReaper prior to initializing, as the
        // ActivationStrategies need a reference to it.
        if (EJSPlatformHelper.isZOS())
        {
            long sweepInterval = 0;
            Long sweepIntervallong = Long.getLong
                            ("com.ibm.websphere.bean.delete.sleep.time");
            if (sweepIntervallong != null)
            {
                sweepInterval = sweepIntervallong.longValue() * 1000;
            }
            statefulBeanReaper = new StatefulBeanReaper(this, cache.getNumBuckets(),
                            sweepInterval,
                            failoverCache, //LIDB2018-1
                            deferrableExecutor); // F73234
        } else
        // LIDB2775-23.4 Ends
        {
            statefulBeanReaper = new StatefulBeanReaper(this, cache.getNumBuckets(),
                            failoverCache, //LIDB2018-1
                            deferrableExecutor); // F73234
        }

        // Now perform all of the initialization that is common between
        // the constructors, including creating the ActivationStrategies
        // and lock table.                                                 d129562
        initialize(container, cache, passivationPolicy, passivator, failoverCache); //LIDB2018-1

        // Start the stateful bean timeout thread
        if (!passivationPolicy.equals(PassivationPolicy.ON_DEMAND))
            statefulBeanReaper.start();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    /**
     * Common code for all constructors. <p>
     * 
     * @param container the EJSContainer that will use this Activator
     * @param cache the EJB Cache this Activator will manage
     * @param passivationPolicy passivation policy to be used
     * @param passivator passivator to be used to passivate stateful beans
     * @param statefulFailoverCache is the failover cache object for this server or null
     *            if SFSB failover is disabled.
     **/
    // d129562
    private void initialize(EJSContainer container, EJBCache cache,
                            PassivationPolicy passivationPolicy,
                            StatefulPassivator passivator,
                            SfFailoverCache failoverCache) //LIDB2018-1
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        this.container = container;
        this.beanCache = cache;

        this.passivator = passivator;

        // Create table for activation locks

        activationLocks = new LockTable(beanCache.getNumBuckets());

        // Create activation strategies

        EJBRuntime runtime = container.getEJBRuntime();

        strategies = new ActivationStrategy[NUM_STRATEGIES];
        for (int type = 0; type < strategies.length; type++)
        {
            strategies[type] = runtime.createActivationStrategy(this, type, passivationPolicy);
        }

        beanCache.setDiscardStrategy(this); // PK04804

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "initialize");
    }

    //
    // Operations
    //

    /**
     * Activate a bean in the context of a transaction. If an instance of the
     * bean is already active in the transaction, that instance is returned.
     * Otherwise, one of several strategies is used to activate an instance
     * depending on what the bean supports. <p>
     * 
     * If this method completes normally, it must be balanced with a call to
     * one of the following methods: removeBean, commitBean, rollbackBean.
     * If this method throws an exception, any partial work has already been
     * undone, and there should be no balancing method call. <p>
     * 
     * If this method returns successfully, {@link EJBThreadData#pushCallbackBeanO} will have been called, and
     * the caller is responsible for calling {@link EJBThreadData#popCallbackBeanO}. <p>
     * 
     * @param threadData the EJB thread data for the currently executing thread
     * @param tx the transaction context in which the bean instance should
     *            be activated.
     * @param beanId the <code>BeanId</code> identifying the bean to
     *            activate.
     * 
     * @return a fully-activated bean instance.
     **/
    public BeanO preInvokeActivateBean(EJBThreadData threadData, ContainerTx tx, BeanId beanId) // d641259
    throws RemoteException
    {
        return beanId.getActivationStrategy().atActivate(threadData, tx, beanId);
    }

    /**
     * Activate a bean in the context of a transaction. If an instance of the
     * bean is already active in the transaction, that instance is returned.
     * Otherwise, one of several strategies is used to activate an instance
     * depending on what the bean supports. <p>
     * 
     * If this method completes normally, it must be balanced with a call to
     * one of the following methods: removeBean, commitBean, rollbackBean.
     * If this method throws an exception, any partial work has already been
     * undone, and there should be no balancing method call. <p>
     * 
     * This method should never be used to obtain bean instances for method
     * invocations. See {@link #preInvokeActivateBean}. <p>
     * 
     * @param threadData the EJB thread data for the currently executing thread
     * @param tx the transaction context in which the bean instance should
     *            be activated.
     * @param beanId the <code>BeanId</code> identifying the bean to
     *            activate.
     * 
     * @return a fully-activated bean instance.
     **/
    public BeanO activateBean(EJBThreadData threadData, ContainerTx tx, BeanId beanId)
                    throws RemoteException
    {
        BeanO beanO = null;

        try
        {
            beanO = beanId.getActivationStrategy().atActivate(threadData, tx, beanId); // d630940
        } finally
        {
            if (beanO != null)
            {
                threadData.popCallbackBeanO();
            }
        }

        return beanO;
    }

    /**
     * Perform actions required following method invocation; this is
     * the complement to activateBean, and should be called by the container
     * to balance activateBean. <p>
     **/
    public void postInvoke(ContainerTx tx, BeanO bean)
    {
        bean.getActivationStrategy().atPostInvoke(tx, bean);
    }

    /**
     * Add a new bean, most likely as a result of a create...() call on
     * a Home interface. It is illegal to attempt to add a bean with a
     * <code>BeanId</code> identical to that of another bean which is
     * currently visible on the specified transaction. <p>
     * 
     * The new bean is enlisted in the specified transaction. <p>
     * 
     * If a bean with the same identity is already enlisted in the current
     * transaction, or there is a visible master instance, a
     * <code>DuplicateKeyException</code> results. <p>
     * 
     * @param tx The transaction on which to add the bean
     * @param bean The new bean
     **/
    public BeanO addBean(ContainerTx tx, BeanO bean)
                    throws DuplicateKeyException,
                    RemoteException
    {
        return bean.getHome().getActivationStrategy().atCreate(tx, bean);
    }

    /**
     * Obtain an exclusive lock on a bean instance. This method is invoked
     * as a result of a create() call. Used primarily for OptA caching
     **/
    // d110984
    public boolean lockBean(ContainerTx tx, BeanId beanId)
                    throws RemoteException
    {
        return beanId.getActivationStrategy().atLock(tx, beanId);
    }

    /**
     * Remove a bean from the cache. This method should be used to handle
     * situations where a bean goes bad.
     * <p>
     * 
     * @param tx The transaction from which to remove the bean
     * @param bean The bean to be removed
     * 
     */
    public void removeBean(ContainerTx tx, BeanO bean)
    {
        bean.getActivationStrategy().atRemove(tx, bean);
    }

    /**
     * Return bean from cache for given transaction, bean id. Return null
     * if no entry in cache.
     */
    public BeanO getBean(ContainerTx tx, BeanId id)
    {
        return id.getActivationStrategy().atGet(tx, id);
    } // getBean

    /**
     * Perform commit-time processing for the specified transaction and bean.
     * This method should be called for each bean which was participating in
     * a transaction which was successfully committed.
     * 
     * The transaction-local instance of the bean is removed from the cache,
     * and any necessary reconciliation between that instance and the master
     * instance is done (e.g. if the bean was removed during the transaction,
     * the master instance is also removed from the cache).
     * <p>
     * 
     * @param tx The transaction which just committed
     * @param bean The BeanId of the bean
     */
    public void commitBean(ContainerTx tx, BeanO bean)
    {
        bean.getActivationStrategy().atCommit(tx, bean);
    }

    /**
     * Perform end of unit of work processing for the specified
     * activity session.
     * 
     * This method should be called for each bean which was
     * participating in an activity session.
     * <p>
     * 
     * @param as The activity session which just completed
     * @param bean The bean for end of unit of work processing
     */
    // LIDB441.5 - added
    public void unitOfWorkEnd(ContainerAS as, BeanO bean)
    {
        bean.getActivationStrategy().atUnitOfWorkEnd(as, bean);
    }

    /**
     * Perform rollback-time processing for the specified transaction and
     * bean. This method should be called for each bean which was
     * participating in a transaction which was rolled back.
     * 
     * Removes the transaction-local instance from the cache.
     * 
     * @param tx The transaction which was just rolled back
     * @param bean The bean for rollback processing
     */
    public void rollbackBean(ContainerTx tx, BeanO bean)
    {
        bean.getActivationStrategy().atRollback(tx, bean);
    }

    /**
     * Perform actions required when a bean is enlisted in a transaction
     * at some point in time other than at activation. Used only for
     * beans using TX_BEAN_MANAGED.
     */
    public final void enlistBean(ContainerTx tx, BeanO bean)
    {
        bean.getActivationStrategy().atEnlist(tx, bean);
    }

    public void setCacheSweepInterval(long sweepInterval)
    {
        beanCache.setSweepInterval(sweepInterval);
    }

    public void setCachePreferredMaxSize(int size)
    {
        beanCache.setCachePreferredMaxSize(size);
    }

    /**
     * Factory interface for ActivationStrategy objects; returns the
     * instance of the strategy associated with the id, corresponding
     * to the EJB Cache managed by this Activator. <p>
     * 
     * This method is intended for use when constructing the HomeOfHomes,
     * which does not have an EJSHome and should use the strategies
     * associated with the default EJB Cache. And also should be
     * used by the Container Extension Factories as they are involved
     * in the construction of subordinate Activators, in which case
     * this method will be invoked on the appropriate Activator
     * (not necessarily the default Activator). <p>
     * 
     * @param id ActivationStrategy Identifier
     */
    public final ActivationStrategy getActivationStrategy(int id)
    {
        return strategies[id];
    }

    /**
     * Factory interface for ActivationStrategy objects; returns the
     * instance of the strategy associated with the id, corresponding
     * to the EJB Cache that will manage the beans associated with
     * the specified Home. <p>
     * 
     * There will be an instance of each strategy associated with each
     * EJB Cache, and there may be multiple EJB Caches. Each EJB Cache may
     * manage beans for 1 or more Homes. <p>
     * 
     * This method is intended for use during construction of EJSHomes. <p>
     * 
     * @param home EJSHome for which an ActivationStrategy is needed.
     * @param id ActivationStrategy Identifier
     **/
    // d129562
    public final ActivationStrategy getActivationStrategy(EJSHome home, int id)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getActivationStrategy: " + beanCache.getName() +
                         " used for " + home.getJ2EEName());

        return strategies[id];
    }

    /**
     * Returns a string 'name' for the strategy associated with the
     * specified id. <p>
     * 
     * A string indicating "Unknown" will be returned if the id is not in
     * the valid range of strategies. <p>
     * 
     * This method is intended for use for trace/dump. <p>
     * 
     * @param id ActivationStrategy Identifier
     */
    // LI3408
    public static final String getActivationStrategyString(int id)
    {
        if (id < 0 || id >= svStrategyStrs.length)
            return ("Unknown (" + id + ")");

        return svStrategyStrs[id];
    }

    //
    // DiscardStrategy interface
    //

    /**
     * Called by the cache after it evicts an object from the cache.
     * Passivates the bean.
     * <p>
     * 
     * @param cache The cache from which the object was evicted
     * @param key The key for the object which was evicted
     * @param object The object which was evicted
     */
    @Override
    public void discardObject(EJBCache cache, Object key, Object object)
                    throws RemoteException
    {
        BeanO bean = (BeanO) object;
        bean.getActivationStrategy().atDiscard(bean);
    }

    /**
     * Returns the LockTable to be used to synchronize Cache eviction
     * processing. <p>
     * 
     * The returned LockTable will be used to obtain a lock object when the
     * Cache has selected an object to be evicted, but prior to obtaining any
     * internal Cache locks (bucket lock). The returned lock object will be
     * used to synchronize both the removal of the object from the Cache, as
     * well as the call to discardObject. <p>
     * 
     * @return LockTable to use for synchronization of the eviction processing.
     **/
    // PK04804
    @Override
    public LockTable getEvictionLockTable()
    {
        return activationLocks;
    }

    /**
     * Called by the session bean reaper when an object needs to be timed
     * out. The bean is removed.
     */
    public void timeoutBean(BeanId beanId)
                    throws RemoteException
    {
        beanId.getActivationStrategy().atTimeout(beanId);
    }

    /**
     * Enable cleanup of passivated beans, kind of a hack, required
     * because all the bean's homes have already been cleaned up at this
     * point
     */
    public void terminate()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "terminate");

        statefulBeanReaper.cancel(); //d601399

        // Force a reaper sweep
        statefulBeanReaper.finalSweep(passivator);

        beanCache.terminate();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "terminate");
    }

    /**
     * Uninstall a bean identified by the J2EEName. This is a partial solution
     * to the general problem of how we can quiesce beans.
     * Uninstalling a bean requires us to enumerate the whole bean cache
     * We attempt to find beans which have the same J2EEName as the bean we
     * are uninstalling. We then fire the uninstall event on the activation
     * strategy. It is upto the activation strategy to decide whether to
     * remove the bean or not.
     * 
     * Also, timeout all Stateful Session beans. When a Stateful Session
     * bean is stopped, not only does it need to remove it from the Active
     * EJB Cache, but it also needs to have any passivated instances removed
     * as well, deleting the file associated with them. This needs to be
     * done so that the customer may change the implementation of the bean
     * and restart it. An old file (with serialized bean data) may not
     * restore to the new implementation. d103404.1
     */
    public void uninstallBean(J2EEName homeName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "uninstallBean " + homeName);

        BeanO cacheMember;
        J2EEName cacheHomeName;
        Iterator<?> statefulBeans; // d103404.1
        int numEnumerated = 0, numRemoved = 0, numTimedout = 0; // d103404.2

        // Enumerate through the bean cache, removing all instances
        // associated with the specified home.
        Enumeration<?> enumerate = beanCache.enumerateElements();

        while (enumerate.hasMoreElements())
        {
            cacheMember = (BeanO) ((CacheElement)
                            enumerate.nextElement()).getObject();
            BeanId cacheMemberBeanId = cacheMember.getId();
            cacheHomeName = cacheMemberBeanId.getJ2EEName();
            numEnumerated++;

            // If the cache has homeObj as it's home, remove it.
            // If the bean has been removed since it was found (above),
            // then the call to atUninstall() will just no-op.
            // Note that the enumeration can handle elements being removed
            // from the cache while enumerating.                          d103404.2
            if (cacheHomeName.equals(homeName))
            {
                HomeInternal home = cacheMember.getHome();
                // On z/OS, the application might be stopping in a single SR
                // only.  We can't tell, so assume the SFSB needs to remain
                // available for the application running in another SR, and
                // passivate the SFSB rather than uninstalling it. LIDB2775-23.4
                BeanMetaData bmd = cacheMemberBeanId.getBeanMetaData();
                if (EJSPlatformHelper.isZOS() && bmd.isPassivationCapable()) {
                    try {
                        home.getActivationStrategy().atPassivate(cacheMemberBeanId);
                    } catch (Exception ex) {
                        Tr.warning(tc, "UNABLE_TO_PASSIVATE_EJB_CNTR0005W",
                                   new Object[] { cacheMemberBeanId, this, ex });
                    }
                } else {
                    home.getActivationStrategy().atUninstall(cacheMemberBeanId,
                                                             cacheMember);
                }
                numRemoved++;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { // d103404.1 d103404.2
            Tr.debug(tc, beanCache.getName() + ": Uninstalled " + numRemoved +
                         " bean instances (total = " + numEnumerated + ")");
        }

        // Also, uninstall all passivated Stateful Session beans for the
        // specified home, regardless of whether they have timed out or not.
        // All of the beans for this home in the StatefulBeanReaper should
        // be in the passivated state (i.e. state written to a file), as the
        // above loop should have cleared all active ones from the EJB Cache.
        // Calling atUninstall on them will delete the file and remove them
        // from the reaper.                                              d103404.1
        statefulBeans = statefulBeanReaper.getPassivatedStatefulBeanIds(homeName);
        while (statefulBeans.hasNext())
        {
            // Call atUninstall just like for those in the cache... atUninstall
            // will handle them not being in the cache and remove the file.
            // Note that atTimeout cannot be used, as the beans (though passivated)
            // may not be timed out, and atTimeout would skip them.         d129562
            BeanId beanId = (BeanId) statefulBeans.next();
            // LIDB2775-23.4 Begins
            if (EJSPlatformHelper.isZOS())
            {
                statefulBeanReaper.remove(beanId);
            } else
            // LIDB2775-23.4 Ends
            {
                beanId.getActivationStrategy().atUninstall(beanId, null);
            }
            numTimedout++;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { // d103404.2
            Tr.debug(tc, "Passivated Beans: Uninstalled " + numTimedout +
                         " bean instances");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "uninstallBean");
    }

    /**
     * Return number of active beans. This includes the active beans in all
     * caches managed by this Activator.
     */
    public int size()
    {
        int size = beanCache.getSize();

        return size;
    } // size

    /**
     * Dump the internal state of the activator (all the beans in the cache).
     */
    public void dump()
    {
        if (!tc.isDumpEnabled()) {
            return;
        }
        introspect(new TrDumpWriter(tc));
    } // dump

    /**
     * Writes the important state data of this class, in a readable format,
     * to the specified output writer. <p>
     * 
     * @param writer output resource for the introspection data
     */
    // F86406
    public void introspect(IntrospectionWriter writer)
    {
        writer.begin("Activator Dump (" + beanCache.getName() + "): " + beanCache.getSize() + " beans in cache");

        Enumeration<Element> vEnum = ((Cache) beanCache).enumerateElements();
        while (vEnum.hasMoreElements())
        {
            Element element = vEnum.nextElement();
            Object key = element.getKey();
            if (key instanceof TransactionKey)
            {
                Object bean = element.getObject();
                String state = element.stateToString();
                ContainerTx tx = ((TransactionKey) key).tx;
                writer.println(bean + state + " : " + tx);
            }
            else
            {
                writer.println(element.toString());
            }
        }

        writer.end();
    }

    //
    // Data
    //

    /**
     * Cache of BeanO objects, indexed by <code>TransactionKey</code>
     */
    EJBCache beanCache;

    /**
     * Table of locks, indexed by <code>BeanId</code>, used to synchronize
     * bean activation.
     */
    LockTable activationLocks;

    /**
     * The container instance in which we live
     */
    protected EJSContainer container;

    /**
     * Singleton instances of each ActivationStrategy
     */
    private ActivationStrategy[] strategies;

    /**
     * Reference to the passivator instance in use in this container
     */
    StatefulPassivator passivator;

    // Tracing
    private static final TraceComponent tc = Tr.register(Activator.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * Garbage collector for timed out stateful session beans
     */
    StatefulBeanReaper statefulBeanReaper;
}
