package com.ibm.ws.sib.msgstore;
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

/**
 * This class provides an interface to query the current counts for a stream. 
 */
public interface Statistics {
    /**
     * @return number of messages currently in the process of being added.
     */
    public long getAddingItemCount();

    /**
     * @return number of messages currently available.
     */
    public long getAvailableItemCount();

    /**
     * @return number of messages currently in the process of being expired.
     */
    public long getExpiringItemCount();

    /**
     * @return number of messages currently locked.
     */
    public long getLockedItemCount();

    /**
     * @return number of messages currently in the process of being removed.
     */
    public long getRemovingItemCount();


    /**
     * @return long the size of the stream in total number of items.
     * This is the value that is used to determine watermark breaches.
     * Only items directly contained within the stream will contribute to the count,
     * not contained reference streams, items streams, or items contained within
     * contained streams.
     * The size will be the maximal calculated for all counted items - ie those in
     * all states which imply containment within the stream.
     * 
     */
    public long getTotalItemCount();

    /**
     * @return number of messages currently not available.
     */
    public long getUnavailableItemCount();

    /**
     * @return number of messages currently in the process of being updated.
     */
    public long getUpdatingItemCount();


}
