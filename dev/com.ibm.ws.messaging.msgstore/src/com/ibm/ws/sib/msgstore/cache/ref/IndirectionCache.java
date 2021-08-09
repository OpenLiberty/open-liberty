package com.ibm.ws.sib.msgstore.cache.ref;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.CacheStatistics;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;

/**
 * This abstract class defines the interface presented to an {@link Indirection}
 * by an IndirectionCache. (We use an abstract class as this could be marginally 
 * faster than an interface, although an interface would be slightly better design)
 * 
 * <p>A reference to an IndirectionCache is obtained by registering an indirection
 * with the Cache Manager. This reference should be stored for future use.
 * 
 * <p>Whenever the Indirection decides that the memory used by its item has become
 * discardable, it requests that the cache manage its notional memory.
 * This request may elicit a single callback to 
 * {@link Indirection#releaseCachedMemory()}.
 * 
 * <p>If the indirection decides that its memory is no longer discardable it may
 * request that the cache does not manage its memory. This will prevent any
 * pending callback to {@link Indirection#releaseCachedMemory()}.
 * 
 * <p>If the Indirection decides that it no longer requires any management, it can
 * unregister from the cache.
 */
public abstract class IndirectionCache implements CacheStatistics 
{
    /**
     * Notify the cache that the indirection does not wish to be considered
     * discardable. This indicates that the indirection does not wish to
     * receive {@link Indirection#releaseCachedMemory()} callbacks.
     * 
     * @param ind
     */
    public abstract void unmanage(Indirection ind) throws SevereMessageStoreException;

    /**
     * Notify the cache that the indirection wishes to be considered
     * discardable.This indicates that the indirection does wishes to
     * receive a {@link Indirection#releaseCachedMemory()} callback.
     *  
     * @param ind
     */
    public abstract void manage(Indirection ind, AbstractItem item) throws SevereMessageStoreException;
}
