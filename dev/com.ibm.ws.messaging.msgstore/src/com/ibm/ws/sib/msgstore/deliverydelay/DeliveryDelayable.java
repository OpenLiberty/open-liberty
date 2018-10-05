package com.ibm.ws.sib.msgstore.deliverydelay;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.msgstore.MessageStoreConstants.MaximumAllowedDeliveryDelayAction;

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

/**
 * Defines the behaviour of items which will be unlocked after deliveryDelayTime.
 * That is, items are defined to have an delivery delay time, after
 * which, they may be unlocked by the DeliveryDelayManager (the DeliveryDelayManager Daemon) post which
 * the item will be available for consumption
 */
public interface DeliveryDelayable
{
    /**
     * Return the time at which the DeliveryDelayable should be unlocked.
     * 
     * @return the deliveryDelay time.
     */
    public long deliveryDelayableGetDeliveryDelayTime();

    /**
     * Return the unique ID of the deliveryDelayable object.
     * 
     * @return the object ID.
     */
    public long deliveryDelayableGetID();

    /**
     * Return true if the DeliveryDelayable is in the message store. This method will be called
     * by the DeliveryDelayManager to decide whether to add the DeliveryDelayable into the DeliveryDelay index. DeliveryDelayable
     * which have already been deleted are then ignored.
     * 
     * @return true if the DeliveryDelayable is in the store.
     */
    public boolean deliveryDelayableIsInStore();

    /**
     * Invoked by the DeliveryDelayManager during DeliveryDelayable processing to instruct the associated object
     * to unlock itself. If successful (the method returns true), then the DeliveryDelayManager will
     * remove the DeliveryDelayable reference from the DeliveryDelay index. If the method returns
     * false, then the DeliveryDelayManager will keep the DeliveryDelayable reference in its index and process
     * it again on the next cycle.
     * 
     * @param tran the transaction under which the item is being
     *            expired
     * @param lockID lockID with which the item has to be unlocked
     * @return true if the object has unlocked itself.
     */
    public boolean deliveryDelayableUnlock(PersistentTransaction tran, long lockID) throws MessageStoreException;

	/**
	 * Handle a potentially incorrect delivery delay.
	 * 
	 * @param action to perform
	 * @return true if the object was unlocked.
	 * @throws MessageStoreException 
	 * @throws SIException 
	 */
	public boolean handleInvalidDeliveryDelayable(MaximumAllowedDeliveryDelayAction action) 
		throws MessageStoreException, SIException;
}
