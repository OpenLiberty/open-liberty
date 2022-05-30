/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta;

import java.io.Serializable;

import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

public interface ExtendedTransactionManager extends TransactionManager
{
	/**
	 * Adds a begin method allowing a timeout to be specified.
	 * 
	 * @param timeout
	 * @throws NotSupportedException
	 * @throws SystemException
	 */
	public void begin(int timeout) throws NotSupportedException, SystemException;

    /**
     * enlist XAResouce object in the current JTA Transaction associated
     * with current thread. This interface is supposed to be used with JTA
     * XAResource providers who want to participate in distributed transactions
     * managed by JTA TM or JTS.  This is a modified form of enlist call that
     * may be used after a previous registerResourceInfo call.
     *
     * @param xaRes The XAResource object representing the resource to enlist.
     * @param recoveryId The identifier returned from a call to registerResourceInfo
     *                   associating the appropriate xaResFactoryClassName/xaResInfo
     *                   necessary for produce a XAResource object.
     *
     * @return <i>true</i> if the resource was enlisted successfully;
     *            otherwise <i>false</i>.
     */
    public boolean enlist(XAResource xaRes, int recoveryId)
    throws RollbackException, IllegalStateException, SystemException;

    //-----------------------------------------------------

    /**
     * Delist the resource specified from the current JTA Transaction
     * associated with the calling thread.
     *
     * XAResources delisted using this method will  receive prepare,
     * commit callbacks etc.
     *
     * @param xaRes The XAResource object representing the resource to delist
     * @param flag One of the values of TMSUCCESS, TMSUSPEND, or TMFAIL.
     *
     * @return <i>true</i> if the resource was delisted successfully;
     *            otherwise <i>false</i>.
     */
    public boolean delist(XAResource xaRes, int flag);

    /**
     * Register XAResouceFactory and XAResourceInfo with the transaction service
     * so that it will be logged and can be used in recovery on a server restart.
     * The token returned is an identifier which should be passed on future enlist
     * calls to associate an enlist with the registered resource information.
     *
     * @param xaResFactoryClassName  The class name of XAResourceFactory.
     * @param xaResInfo  Information necessary for producing an XAResource object
     *                   using XAResourceFactory.
     *
     * @return a resource recoveryId value associate with the factory/info which
     *         can be used on resource enlistment otherwise -1 if an error occurs.
     */
    public int registerResourceInfo(String xaResFactoryClassName, Serializable xaResInfo);

    /**
     * Register XAResouceFactory and XAResourceInfo with the transaction service
     * so that it will be logged and can be used in recovery on a server restart.
     * A priority can be defined which will define the order resources are 
     * prepared and committed relative to other resources enlisted in a transaction.
     * The token returned is an identifier which should be passed on future enlist
     * calls to associate an enlist with the registered resource information.
     *
     * @param xaResFactoryClassName  The class name of XAResourceFactory.
     * @param xaResInfo  Information necessary for producing an XAResource object
     *                   using XAResourceFactory.
     * @param priority   The priority associated with resources for this factory
     *                   and xa resource information. Priorities may be assigned
     *                   values in the range Integer.MAX_VALUE through Integer.MIN_VALUE
     *                   with 0 as the default priority if unassigned.  Priority
     *                   values will determine the order of prepare and commit
     *                   during the completion phase; the higher the priority,
     *                   the earlier the resource will be prepared or committed.
     *                   Resources with the same priority value may be prepared or
     *                   committed in any order.  Priority ordering will be maintained
     *                   over failures or retries - higher priority resources must
     *                   complete before lower priority resources are committed.
     *
     * @return a resource recoveryId value associate with the factory/info which
     *         can be used on resource enlistment otherwise -1 if an error occurs.
     */
    public int registerResourceInfo(String xaResFactoryClassName, Serializable xaResInfo, int priority);
}
