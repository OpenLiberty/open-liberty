/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Specialized cache for ManagedBeans, which will weakly reference a
 * ManagedBean wrapper, so when all references are dropped PreDestroy
 * may be called on the ManagedBean instance. <p>
 */
public final class WeakManagedBeanCache
{
    private static final TraceComponent tc = Tr.register(WeakManagedBeanCache.class, "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * The singleton instance of a WeakManagedBeanCache.
     */
    private static WeakManagedBeanCache svInstance = null;

    /**
     * Returns the singleton instance of a WeakManagedBeanCache.
     */
    public static synchronized WeakManagedBeanCache instance()
    {
        // This method is synchronized to avoid a race condition creating the
        // singleton instance.  Not expected to be called very often.
        if (svInstance == null) {
            svInstance = new WeakManagedBeanCache();
        }
        return svInstance;
    }

    /**
     * A ReferenceQueue for references to all ManagedBean wrappers that
     * have a PreDestroy lifecycle callback.
     */
    private final ReferenceQueue<EJSWrapperBase> ivRefQueue;

    /**
     * The actual cache, a map of weak references of the wrapper to the
     * bean instance (actually BeanO) they wrap.
     */
    private final Map<WeakReference<EJSWrapperBase>, BeanO> ivCache;

    /**
     * Default constructor for a WeakManagedBeanCache; made private to
     * insure there is only a single instance of this class.
     */
    private WeakManagedBeanCache()
    {
        ivRefQueue = new ReferenceQueue<EJSWrapperBase>();
        ivCache = new HashMap<WeakReference<EJSWrapperBase>, BeanO>();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "WeakManagedBeanCache : " + ivRefQueue + ", " + ivCache);
    }

    /**
     * Add a ManagedBean instance (held in a BeanO) to this weak reference
     * cache, keyed by the wrapper instance associated with it. <p>
     * 
     * @param wrapper object representing a wrapper for the managed bean
     * @param bean ManagedBean context, which holds the ManagedBean instance
     */
    public void add(EJSWrapperBase wrapper, BeanO bean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "add(" + Util.identity(wrapper) + ", " + bean + ")");

        poll();

        WeakReference<EJSWrapperBase> key = new WeakReference<EJSWrapperBase>(wrapper, ivRefQueue);
        synchronized (ivCache)
        {
            ivCache.put(key, bean);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "add : key = " + key);
    }

    /**
     * Removes all entries from the cache for the specified home and
     * destroys them (i.e. PreDestroy is called).
     */
    public void remove(EJSHome home)
    {
        List<BeanO> removedBeans = new ArrayList<BeanO>();

        // Clear out any entries eligible for garbage collection and then
        // loop through the remaining entries, looking for values that belong
        // to the specified home and remove them.  This will hold the cache
        // lock for longer than normal, but it is not expected that
        // applications will be stopped very often.
        synchronized (ivCache)
        {
            poll();

            for (Iterator<Map.Entry<WeakReference<EJSWrapperBase>, BeanO>> it = ivCache.entrySet().iterator(); it.hasNext();)
            {
                BeanO bean = it.next().getValue();

                if (bean.home == home)
                {
                    it.remove(); // d730739
                    removedBeans.add(bean);
                }
            }
        }

        // Now that the cache lock has been dropped, go ahead and destroy
        // all of the beans that were removed.
        for (BeanO bean : removedBeans)
        {
            bean.destroy();
        }
    }

    /**
     * Removes all values from the cache whose key has been garbage collected. <p>
     * 
     * When a value is removed from the cache, it is destroyed (i.e PreDestroy
     * is called).
     */
    private void poll()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // Note: A ReferenceQueue is thread safe and may be accessed concurrently
        //       from multiple threads. However, a BeanO should only be destroyed
        //       on a single thread, which is fine since its key will be returned
        //       from poll() only once.

        BeanO bean = null;
        int removeCount = 0;
        Reference<? extends EJSWrapperBase> ref;
        while ((ref = ivRefQueue.poll()) != null)
        {
            synchronized (ivCache)
            {
                bean = ivCache.remove(ref);
            }
            ++removeCount;

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "poll : removed (" + ref + ", " + bean + ")");

            // It is possible a bean was removed during clear, and yet the
            // wrapper is still on the reference queue, so handle null.
            if (bean != null) {
                bean.destroy();
            }
        }
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "poll : size = " + ivCache.size() + ", removed = " + removeCount);
    }

}
