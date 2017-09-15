/*******************************************************************************
 * Copyright (c) 2001, 2015 IBM Corporation and others.
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
import java.util.Enumeration;

import com.ibm.ejs.container.util.ByteArray;
import com.ibm.ejs.util.cache.Cache;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CacheElement;
import com.ibm.websphere.csi.DiscardException;
import com.ibm.websphere.csi.DiscardStrategy;
import com.ibm.websphere.csi.EJBCache;
import com.ibm.websphere.csi.FaultException;
import com.ibm.websphere.csi.FaultStrategy;
import com.ibm.websphere.csi.IllegalOperationException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ejbcontainer.diagnostics.TrDumpWriter;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * An <code>WrapperManager</code> is responsible for handling requests
 * from the object adapter to map a given IOR key to an implementation. <p>
 *
 * The <code>WrapperManager</code> maintains a cache that maps IOR
 * keys to EJSWrapper instances. To satisfy lookup requests from the ORB,
 * it first consults this cache. If no mapping is found it deserializes
 * the IOR key (a serialized BeanId instance) and then asks the wrapper
 * factory to construct a new wrapper instance corresponding to this
 * BeanId. This wrapper is added to the cache and is returned in response
 * to the ORB's request. <p>
 */

public class WrapperManager
                implements DiscardStrategy,
                FaultStrategy
{
    private static final TraceComponent tc =
                    Tr.register(WrapperManager.class,
                                "EJBContainer",
                                "com.ibm.ejs.container.container"); //d121510

    private static final String CLASS_NAME = "com.ibm.ejs.container.WrapperManager";

    /**
     * The wrapper cache maps an IOR key to a wrapper instance.
     */
    protected EJBCache wrapperCache;

    /**
     * The container this WrapperManager is installed in
     */
    private EJSContainer container;

    /**
     * Cache of BeanIds that have the serialized byte array form cached.
     * This is used to improve performance by reducing the serialization
     * of beanIds.
     **/
    // d140003.12
    protected BeanIdCache beanIdCache;

    /**
     * Create new wrapper manager instance. <p>
     *
     * @param container the <code>EJSContainer</code> instance for the
     *            container this <code>WrapperManager</code> is installed in <p>
     */
    public WrapperManager(EJSContainer container)
    {
        this.container = container;
    }

    public void initialize(Cache wrapperCache)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        this.wrapperCache = wrapperCache;
        wrapperCache.setDiscardStrategy(this);
        wrapperCache.setFaultStrategy(this);

        // Create a cache of serialized BeanIds to improve performance.  d140003.12
        int beanIdCacheSize = getBeanIdCacheSize(wrapperCache.getNumBuckets());
        beanIdCache = new BeanIdCache(beanIdCacheSize);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initialize");
    }

    /**
     * Destroy this wrapper manager instance.
     */
    public void destroy()
    {
        wrapperCache.terminate();
        wrapperCache = null; //d583637
        container = null; //d583637
    } // destroy

    /**
     * Tell the wrapper manager to update the wrapper cache sweep interval.
     *
     * @param interval
     */
    public void setWrapperCacheSweepInterval(long interval)
    {
        wrapperCache.setSweepInterval(interval);
    }

    /**
     * Call to update the BeanIdCache cache size.
     */
    public void setWrapperCacheSize(int cacheSize)
    {
        wrapperCache.setCachePreferredMaxSize(cacheSize);
        int updatedCacheSize = getBeanIdCacheSize(cacheSize);
        beanIdCache.setSize(updatedCacheSize);
    }

    /**
     * Calculate the size to be used for the BeanIdCache.
     *
     * @return cacheSize
     */
    private int getBeanIdCacheSize(int cacheSize)
    {
        int beanIdCacheSize = cacheSize;
        if (beanIdCacheSize < (Integer.MAX_VALUE / 2))
            beanIdCacheSize = beanIdCacheSize * 2;
        else
            beanIdCacheSize = Integer.MAX_VALUE;

        return beanIdCacheSize;
    }

    /**
     * Returns true if the wrapper is pinned in the WrapperManager cache
     * for the duration of the method invocation. This prevents the wrapper
     * from getting unregistered while the method invocation is in progress,
     * and updates the LRU data for the wrapper in the cache when it is
     * unpinned during postInvoke. <p>
     *
     * Returns false if the wrapper is not in the cache, or if another
     * thread or transaction already has the wrapper pinned. <p>
     *
     * If the wrapper is not in the cache, this is due to the Eviction
     * Strategy just evicting it as the method call came in. It is
     * unfortunate, but not a problem. The wrapper will be faulted back
     * into the cache the next time it is used.
     *
     * If another thread or transaction already has the wrapper pinned,
     * then this thread will just leave it up to the other thread to
     * update the LRU data, indicating the wrapper has been used. This
     * avoids the synchronization/contention that may occur pinning
     * the wrapper in the cache, and significantly improves performance
     * (especially for Stateless beans, where the wrapper is a
     * singleton).
     *
     * @param wrapper Wrapper to pin in the Wrapper Cache.
     *
     * @return true if the pin is obtained and postInvoke needs to be called;
     *         otherwise false.
     */
    public boolean preInvoke(EJSWrapperBase wrapper) // d140003.9
    throws CSIException,
                    RemoteException
    {
        // If the wrapper has already been pinned by some other thread or
        // transaction, then don't bother pinning again. This can
        // significantly reduce the contention caused by the synchronized
        // block in Cache.pin (especially for Stateless beans that have
        // a singleton wrapper).                                         d140003.9
        if (wrapper.ivCommon.pinned > 0) // d195605
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "preInvoke : pinned = false (already pinned)",
                         wrapper);
            return false;
        }

        // Attempt to pin the wrapper in the Wrapper Cache.  Only one thread
        // that is using the wrapper needs to pin it, as the pin/unpin is
        // largely done just to update the LRU data.                     d140003.9
        // This can now be done directly on the wrapper, avoiding the lookup
        // in the wrapper cache. The pinOnce method is synchronized on the
        // bucket to avoid multiple pins.                                  d195605
        boolean pinned = wrapper.ivCommon.pinOnce();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "preInvoke : pinned = " + pinned, wrapper);

        return pinned;
    }

    /**
     * Unpin the wrapperKey. This wrapper is now free to be evicted.
     * NOTE : The unpin itself will set the access bit for the element
     * so this wrapper will be available for some time after this
     * operation. <p>
     *
     * Note: postInvoke should only be called if preInvoke returned
     * true. <p>
     *
     * @param wrapper Wrapper to unpin in the Wrapper Cache.
     */
    public void postInvoke(EJSWrapperBase wrapper)
                    throws CSIException,
                    RemoteException
    {
        // This can now be done directly on the wrapper, avoiding the lookup
        // in the wrapper cache. The unpin method is synchronized on the
        // bucket to insure the state of the pin count and LRU data.  And,
        // this method on the EJSWrapperCommon does not return any exceptions
        // if the Wrapper is not currently in the cache or is not pinned.  d195605
        wrapper.ivCommon.unpin();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "postInvoke : unpinned", wrapper);
    }

    /**
     * Create a wrapper for a newly created stateful session bean.
     */
    // F61004.6
    public EJSWrapperCommon createWrapper(StatefulBeanO beanO)
                    throws CSIException,
                    RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createWrapper: " + beanO.getId());

        BeanId id = beanO.getId();
        EJSHome home = beanO.home;
        EJSWrapperCommon wc;

        ByteArray wrapperKey = id.getByteArray();
        wrapperKey.setBeanId(id);

        try
        {
            wc = home.internalCreateWrapper(id);
        } catch (Exception ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".createWrapper", "294", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Malformed object key", ex);
            throw new CSIException("Wrapper creation failure: " + ex, ex);
        }

        // Stateful beans are one-to-one with their wrappers, so cache the
        // wrapper<->bean link.                                        F61004.6
        wc.ivCachedBeanO = beanO;

        // findAndFault used by getWrapper also does not pin.
        wrapperCache.insertUnpinned(wrapperKey, wc);

        // The BeanId has the serialized byte array cached, so put it in the
        // BeanId Cache.
        beanIdCache.add(id);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createWrapper");
        return wc;
    }

    /**
     * Get wrapper instance for a newly created bean.
     */
    // d729903
    public EJSWrapperCommon getWrapperForCreate(BeanO beanO)
                    throws CSIException,
                    RemoteException
    {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getWrapperForCreate");

        BeanId id = beanO.getId();
        EJSHome home = beanO.home;
        EJSWrapperCommon wc;

        if (home.statefulSessionHome)
        {
            wc = ((StatefulBeanO) beanO).ivWrapperCommon;
            if (wc == null)
            {
                // The cached wrapper was evicted from the cache and disconnected
                // before we could return to the application (e.g., the bean's
                // PostConstruct hung).  Recreate the wrapper without a cached
                // bean, which will still work but have worse performance.
                wc = getWrapper(id);
            }
        }
        else
        {
            wc = getWrapper(id);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getWrapperForCreate");
        return wc;
    }

    /**
     * Get wrapper instance for given <code>BeanId</code>. <p>
     */
    public EJSWrapperCommon getWrapper(BeanId id) // f111627
    throws CSIException,
                    RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getWrapper");

        EJSWrapperCommon result = null; // f111627

        // If this BeanId does not already have its serialized byte array
        // cached, and it is not a singleton beanId (for homes, stateless,
        // and messagedriven), then look in the cache of beanIds that have
        // serialized byte arrays cached.                               d140003.12
        if (id.byteArray == null && !id._isHome && id.pkey != null)
            id = beanIdCache.find(id);

        ByteArray wrapperKey = id.getByteArray();
        wrapperKey.setBeanId(id);

        // Use the findAndFault logic to load a wrapper into the cache
        // if required.
        try {
            result = (EJSWrapperCommon) wrapperCache.findAndFault(wrapperKey); // f111627
        } catch (FaultException ex) {

            FFDCFilter.processException(ex, CLASS_NAME + ".getWrapper", "259", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Unable to fault in wrapper", ex);
            throw new CSIException(ex.toString());
        }

        // At this point, all BeanIds are known to have the serialized
        // byte array cached, so put Stateful and Entity BeanIds in the
        // BeanId Cache.  BeanIds for homes, stateless, and messagedriven
        // are singletons and already cached on EJSHome.                 d154342.1
        if (!id._isHome && id.pkey != null)
            beanIdCache.add(result.getBeanId());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getWrapper");
        return result;
    } // getWrapper

    /**
     * Remove given wrapper from this wrapper manager. <p>
     *
     * @param beanId is the BeanId of the <code>EJSWrapperBase</code> object
     *            to unregister.
     * @param dropRef if true, drop a reference on the object before removing
     *            from the cache.
     */
    //d181569 - changed signature of method.
    public boolean unregister(BeanId beanId, boolean dropRef) // f111627
    throws CSIException
    {
        boolean removed = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unregister",
                     new Object[] { beanId, new Boolean(dropRef) }); // d181569

        ByteArray wrapperKey = beanId.getByteArray(); // d181569

        try
        {
            EJSWrapperCommon wrapperCommon = (EJSWrapperCommon) // f111627
            wrapperCache.removeAndDiscard(wrapperKey, dropRef);
            if (wrapperCommon != null) { // f111627
                removed = true;
            }
        } catch (IllegalOperationException ex)
        {
            // Object is pinned and cannot be removed from the cache
            // Swallow the exception for now, and let the Eviction thread
            // take care of the object later
            FFDCFilter.processException(ex, CLASS_NAME + ".unregister", "351", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) { // d144064
                Tr.event(tc,
                         "unregister ignoring IllegalOperationException for object "
                                         + beanId); // d181569
                Tr.event(tc, "Exception: " + ex);
            }
        } catch (DiscardException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".unregister", "358", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) // d144064
                Tr.event(tc, "Unable to discard element");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "unregister");

        return removed;
    } // unregister

    /**
     * unregisterHome removes from cache homeObj and all Objects that
     * have homeObj as it's home. It also unregisters these objects from
     * from the orb object adapter.
     */
    public void unregisterHome(J2EEName homeName, EJSHome homeObj)
                    throws CSIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unregisterHome");

        J2EEName cacheHomeName;
        int numEnumerated = 0, numRemoved = 0; // d103404.2

        Enumeration<?> enumerate = wrapperCache.enumerateElements();
        while (enumerate.hasMoreElements())
        {
            // need to get the beanid from either the remote or local wrapper,
            //  whichever is available, the beanid must be the same for both wrappers
            EJSWrapperCommon wCommon = (EJSWrapperCommon) // f111627
            ((CacheElement) enumerate.nextElement()).getObject(); // f111627

            BeanId cacheMemberBeanId = wCommon.getBeanId(); // d181569
            cacheHomeName = cacheMemberBeanId.getJ2EEName();
            numEnumerated++;

            // If the cache has homeObj as it's home or is itself the home,
            // remove it. If the wrapper has been removed since it was found
            // (above), then the call to unregister() will just return false.
            // Note that the enumeration can handle elements being removed
            // from the cache while enumerating.                          d103404.2
            if (cacheHomeName.equals(homeName) ||
                cacheMemberBeanId.equals(homeObj.getId()))
            {
                unregister(cacheMemberBeanId, true); // d181217 d181569
                numRemoved++;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { // d103404.1 d103404.2
            Tr.debug(tc, "Unregistered " + numRemoved +
                         " wrappers (total = " + numEnumerated + ")");
        }

        // Now remove any cached BeanIds for this home.                    d152323
        beanIdCache.removeAll(homeObj);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "unregisterHome");
    }

    /**
     * Unregister a wrapper instance when it is castout of the wrapper
     * cache.
     */
    @Override
    public void discardObject(EJBCache wrapperCache, Object key, Object ele)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "discardObject", new Object[] { key, ele });

        EJSWrapperCommon wrapperCommon = (EJSWrapperCommon) ele;
        wrapperCommon.disconnect(); // @MD20022C, F58064

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "discardObject");
    } // castout

    /**
     * Return wrapper instance corresponding to the given object key. <p>
     *
     * This method is called whenever the ORB is attempting to invoke a
     * method on an object identitied by an IOR. The byte array received
     * by this method must be a serialized instance of a
     * <code>BeanId</code>. If an entry corresponding to this key is found
     * in the wrapper cache the wrapper is returned, else a new wrapper
     * instance is created and returned. <p>
     *
     * @param key a byte array containing the
     *            serialized <code>BeanId</code> of a wrapper <p>
     *
     * @return a wrapper instance with the given <code>BeanId</code>
     */
    public EJSRemoteWrapper keyToObject(byte[] key)
                    throws RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "keyToObject");

        EJSWrapperCommon wc = null;
        EJSRemoteWrapper wrapper = null;

        // -----------------------------------------------------------------------
        // The key into the wrapper cache is the serialized beanId, and the
        // serialized beanId either is the wrapper key, or at least part of
        // the wrapper key.  For the EJB Component interface (i.e. EJB 2.1),
        // where there is only one remote interface, the wrapper key is the
        // beanId. However, for EJB 3.0 Business interfaces, where there may
        // be multiple remote interfaces, the wrapper key also contains the
        // identity of the specific interface.
        // -----------------------------------------------------------------------

        try
        {
            // If this is a Business Wrapper Key, extract the serialized beanId
            // from it, and use that to find the Common/Factory in the Wrapper
            // Cache, then use the rest of the Wrapper Key information to get
            // the specific remote interface wrapper.                       d419704
            if (key[0] == (byte) 0xAD)
            {
                WrapperId wrapperId = new WrapperId(key);
                ByteArray wrapperKey = wrapperId.getBeanIdArray();
                wc = (EJSWrapperCommon) wrapperCache.findAndFault(wrapperKey);
                if (wc != null)
                {
                    wrapper = wc.getRemoteBusinessWrapper(wrapperId);

                    // At this point, all BeanIds are known to have the serialized
                    // byte array cached, so put Stateful and Entity BeanIds in the
                    // BeanId Cache.  BeanIds for homes, stateless, and messagedriven
                    // are singletons and already cached on EJSHome.                 d154342.1
                    BeanId beanId = wrapper.beanId; // d464515
                    if (!beanId._isHome && beanId.pkey != null) // d464515
                    {
                        beanIdCache.add(beanId); // d464515
                    }
                }
            }

            // If this is a Component Wrapper Key, then it already is the
            // serialized beanId, so just use it to find the Common/Factory in
            // the Wrapper Cache, then get the one remote wrapper.          d419704
            else
            {
                ByteArray wrapperKey = new ByteArray(key);
                wc = (EJSWrapperCommon) wrapperCache.findAndFault(wrapperKey); // f111627
                if (wc != null)
                {
                    wrapper = wc.getRemoteObjectWrapper(); // d112375 d440604

                    // At this point, all BeanIds are known to have the serialized
                    // byte array cached, so put Stateful and Entity BeanIds in the
                    // BeanId Cache.  BeanIds for homes, stateless, and messagedriven
                    // are singletons and already cached on EJSHome.                 d154342.1
                    BeanId beanId = wrapper.beanId; // d464515
                    if (!beanId._isHome && beanId.pkey != null) // d464515
                    {
                        beanIdCache.add(beanId); // d464515
                    }
                }

            }
        } catch (FaultException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".keyToObject", "501", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Unable to fault in wrapper", ex);
            throw new RemoteException(ex.getMessage(), ex); // d356676.1
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "keyToObject", wrapper);
        return wrapper;

    } // keyToObject

    /**
     * Invoked by the Cache FaultStrategy when an object was not found
     * and needs to be inserted. We create a new wrapper instance to be
     * inserted into the cache. The cache package holds the lock on a
     * bucket when this method is invoked.
     */
    @Override
    public Object faultOnKey(EJBCache cache, Object key)
                    throws FaultException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "faultOnKey", key);

        ByteArray wrapperKey = (ByteArray) key;
        EJSWrapperCommon result = null; // f111627

        // Check if the beanId is already set on this key in which case
        // we can avoid the deserialize
        BeanId beanId = wrapperKey.getBeanId();

        try
        {
            if (beanId == null) {
                beanId = BeanId.getBeanId(wrapperKey, container); // d140003.12
            }
            result = container.createWrapper(beanId);
        } catch (InvalidBeanIdException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".faultOnKey", "533", this);

            // If the nested exception is an EJBNotFoundException, then report
            // that the app is not started/installed, rather than just the
            // generic 'Malformed object key'.                            d356676.1
            Throwable cause = ex.getCause();
            if (cause instanceof EJBNotFoundException)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Application Not Started", ex);
                throw new FaultException((EJBNotFoundException) cause,
                                "Application not started or not installed");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Malformed object key", ex);
            throw new FaultException(ex, "Malformed object key");
        } catch (Exception ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".faultOnKey", "548", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Malformed object key", ex);
            throw new FaultException(ex, "Malformed object key");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "faultOnKey");

        return result;
    }

    /**
     * Dump the internal state of the wrapper manager.
     */
    public void dump()
    {
        introspect(new TrDumpWriter(tc));
    } // dump

    /**
     * Writes the significant state data of this class, in a readable format,
     * to the specified output writer. <p>
     *
     * @param writer output resource for the introspection data
     */
    // F86406
    public void introspect(IntrospectionWriter writer)
    {
        writer.begin("WrapperManager");
        beanIdCache.introspect(writer);
        ((Cache) wrapperCache).introspect(writer);
        writer.end();
    }

}
