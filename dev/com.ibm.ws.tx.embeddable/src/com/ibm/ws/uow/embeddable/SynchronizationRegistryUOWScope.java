/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.uow.embeddable;

import javax.transaction.Synchronization;

import com.ibm.ws.uow.UOWScope;

/**
 * This interface declares the methods required to support the implementation
 * of TransactionSynchronizationRegistry and UOWSynchronizationRegistry.
 * 
 * @see com.ibm.websphere.uow.UOWSynchronizationRegistry
 * @see javax.transaction.TransactionSynchronizationRegistry
 */
public interface SynchronizationRegistryUOWScope extends UOWScope
{
	// This set of three modifiers are used to ensure that local id's returned from
	// getLocalId are unique across all three UOW types. We do not change the most
	// significant bit to avoid causing the numbers to become negative. In
	// binary the three modifiers are 0110, 0100, and 0010 respectively. This limits
	// us to 61 bits until there is a risk of clashes in local id between the three
	// UOW types. This equates to 2305843009213693951 UOWs of each type; sufficient
	// to create one uow per millisecond for in excess of 73 million years until a
	// clash may occur.
//	public static final long GLOBAL_TRANSACTION_LOCAL_ID_MODIFIER = 0x6000000000000000L;
	public static final long LOCAL_TRANSACTION_LOCAL_ID_MODIFIER = 0x4000000000000000L;
	public static final long ACTIVITYSESSION_LOCAL_ID_MODIFIER = 0x2000000000000000L;
	
	public void putResource(Object key, Object resource);
    public Object getResource(Object key);
    
    public long getLocalId();
    
    public boolean getRollbackOnly();
    public void setRollbackOnly();
    
    public int getUOWStatus();
    public int getUOWType();
    
    public void registerInterposedSynchronization(Synchronization sync);
    public String getUOWName();
}
