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

import java.io.IOException;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/**
 * This class defines the behaviour of an indirection.  These are used to manage
 * the memory burden of items. (The name is historical - there used to be an
 * instance of a class that indirectly held the item on behalf of a link.)   
 * 
 * An indirection is now an entity that implements this interface. The interface
 * allows its implementor to register with an indirection cache, and to be called 
 * back when a memory constraint is being broken.
 *
 * The Indirection is expected to hold two references on behalf of the cache. 
 * These references are synchronized by the cache and should not be synchronized 
 * by the indirection. They allow the cache to maintain a linked list of of
 * Indirections for callbacks.
 */
public interface Indirection 
{
    /**
     * This is the callback from the item cache to indicate that the 
     * discardable item should be freed. It's only used to remove STORE_NEVER items
     * from the MS when the cache of these items is full.
     */
    public void releaseIfDiscardable() throws SevereMessageStoreException;


    /**
     * @return the receivers ID. Used for identifying the receiver in trace
     * output
     */
    public long getID();

    /**
     * @return the approx in memory size of the item. Used by the cache to calculate
     * memory burden.
     */
    public int getInMemoryItemSize();

    /**
     * @return link to next item in this cache.
     * Used by the cache for maintaining a linked list of 
     * cached indirections.
     * The implementor should merely return the value provided by the 
     * cache in {@link #itemCacheSetNextLink()}. 
     * The implementor should not synchronize access to this variable.
     */
    public Indirection itemCacheGetNextLink();

    /**
     * @return link to previous item in this cache.
     * Used by the cache for maintaining a linked list of 
     * cached indirections.
     * The implementor should merely return the value provided by the 
     * cache in {@link #itemCacheSetPreviousLink()}. 
     * The implementor should not synchronize access to this variable.
     */
    public Indirection itemCacheGetPrevioustLink();

    /**
     * @param linkNext the next item in the caches list of 
     * indirections.
     * Used by the cache for maintaining a linked list of 
     * cached indirections.
     * The implementor should merely store the value provided and
     * return it to the cache in {@link #itemCacheGetNextLink()}. 
     * The implementor should not synchronize access to this variable.
     */
    public void itemCacheSetNextLink(Indirection linkNext);

    /**
     * @param linkPrevious the previous item in the caches list of 
     * indirections.
     * Used by the cache for maintaining a linked list of 
     * cached indirections.
     * The implementor should merely store the value provided and
     * return it to the cache in {@link #itemCacheGetPreviousLink()}. 
     * The implementor should not synchronize access to this variable.
     */
    public void itemCacheSetPreviousLink(Indirection linkPrevious);

    /**
     * @param item a reference to the indirection's item
     * The implementor should merely store the value provided. 
     * The implementor should not synchronize access to this variable.
     */
    public void itemCacheSetManagedReference(AbstractItem item);

    /**
     * Print a short XML description of the receiver on the given writer.
     * @param writer
     * @throws IOException
     */
    public void xmlShortWriteOn(FormattedWriter writer) throws IOException;
}
