package com.ibm.ws.sib.msgstore.expiry;
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

import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;


/** 
 * Defines the behaviour of items which may expire from the
 * message store. That is, they are defined to have an expiry time, after
 * which, they may be removed by the Expirer (the Expiry Daemon).
 */
public interface Expirable
{
    /**
     * Return the time at which the expirable should expire.
     * @return the expiry time.
     */
    public long expirableGetExpiryTime(); 

    /**
     * Return the unique ID of the expirable object.
     * @return the object ID.
     */
    public long expirableGetID();

    /**
     * Return true if the Expirable is in the message store. This method will be called
     * by the expirer to decide whether to add the expirable into the expiry index. Expirables
     * which have already been deleted are then ignored.
     * @return true if the Expirable is in the store.
     */ 
    public boolean expirableIsInStore();

    /**
     * Invoked by the Expirer during expiry processing to instruct the associated object
     * to delete itself. If successful (the method returns true), then the Expirer will 
     * remove the expirable reference from the expiry index. If the method returns
     * false, then the Expirer will keep the expirable reference in its index and process
     * it again on the next cycle. 
     * @param tran the transaction under which the item is being
     *               expired
     * @return true if the object has deleted itself.
     */
    public boolean expirableExpire(PersistentTransaction tran) throws SevereMessageStoreException;      // 179365.3
}
