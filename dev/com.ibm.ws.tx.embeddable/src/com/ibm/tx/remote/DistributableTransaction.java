/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.remote;

import java.io.Serializable;

import javax.transaction.SystemException;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.embeddable.impl.WSATRecoveryCoordinator;

/**
 *
 */
public interface DistributableTransaction {

    /**
     * 
     */
    void suspendAssociation();

    /**
     * 
     */
    void resumeAssociation();

    /**
     * @return
     */
    String getGlobalId();

    /**
     * 
     */
    void addAssociation();

    /**
     * 
     */
    void removeAssociation();

    /**
     * @return
     */
    int getStatus();

    /**
     * 
     */
    void setRollbackOnly();

    /**
     * @return
     */
    Xid getXid();

    /**
     * @param xaResFactoryFilter
     * @param xaResInfo
     * @param xid
     * @throws SystemException
     */
    void enlistAsyncResource(String xaResFactoryFilter, Serializable xaResInfo, Xid xid) throws SystemException;

    /**
     * @param rc
     */
    void setWSATRecoveryCoordinator(WSATRecoveryCoordinator rc);

    /**
     * 
     */
    void replayCompletion();

}