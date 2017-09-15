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
package com.ibm.ws.ejbcontainer.failover;

import com.ibm.ejs.container.BeanId;

public interface SfFailoverCache
{
    /**
     * Get the SfFailoverClient instance for a specified failover instance ID.
     * 
     * @param id is the unique failover instance ID.
     * 
     * @return SfFailoverClient previously created for the failover instance ID
     *         or NULL if one was not previously created.
     */
    SfFailoverClient getCachedSfFailoverClient(String id);

    /**
     * Returns true if either the SFSB does not exist in the failover cache OR the
     * SFSB has timed out. Note, if not in the failover cache local to this server
     * process, will try to fault in the SFSB data from a remote failover
     * server by using the getEntry interface of failover. This normally does not
     * happen if recommendations for "hot" failover is followed bu customer.
     * But if recommendations are not followed, we simply fault the
     * data into the local failover cache from the remote failover cache.
     * 
     * @param beanId is the BeanId for the SFSB.
     * 
     * @return see description of method.
     */
    boolean beanDoesNotExistOrHasTimedOut(BeanId beanId);

    /**
     * Check whether a specified SFSB exists in the failover cache.
     * 
     * @param beanId is the BeanId for the SFSB.
     * 
     * @return true only if SFSB found in local failover cache.
     */
    boolean beanExists(BeanId beanId);

    /**
     * Return true iff the SFSB exists in the failover cache AND it has
     * timed out. Note, if not in the failover cache local to this server
     * process, will try to fault in the SFSB data from a remote failover
     * server by using the getEntry interface of failover. This normally does not
     * happen if recommendations for "hot" failover is followed bu customer.
     * But if recommendations are not followed, we simply fault the
     * data into the local failover cache from the remote failover cache.
     * 
     * @param beanId is the BeanId for the SFSB.
     * 
     * @return true if the SFSB still exists in failover cache and it has timed out.
     *         Otherwise, false is returned.
     */
    boolean beanExistsAndTimedOut(BeanId beanId);

    /**
     * Get and remove the SFSB data from the failover cache for a
     * specified SFSB.
     * 
     * @param beanId for the SFSB.
     * 
     * @return the byte array that contains the serialized SFSB data or a
     *         null reference if data is not in failover cache.
     */
    byte[] getAndRemoveData(BeanId beanId, SfFailoverClient Client);

    /**
     * Return whether SFSB is in an active sticky UOW
     * (either BMT or Activity Session).
     * 
     * @param beanId for the SFSB.
     * 
     * @return boolean true if SFSB is in an active sticky BMT.
     */
    boolean inStickyUOW(BeanId beanId);

    /**
     * Remove cache entry for a specified SFSB.
     * 
     * @param key is the SfFailoverKey object that contains the serialized
     *            bytes of the SFSB BeanId.
     */
    void removeCacheEntry(BeanId beanId);

    /**
     * Sweep through the failover cache and cleanup beans
     * which have timed out.
     */
    void sweep();
}
