/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tx.rrs;

import java.io.Serializable;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * com.ibm.tx.jta.XAResourceFactory's extension.
 */
public interface RRSXAResourceFactory extends XAResourceFactory {

    /**
     * Obtains a one phase capable native XA resource.
     * 
     * @param uowCoord The unit of work coordinator to be associated with the resource.
     * @param pmiName The back-end resource factory/data source name.
     * 
     * @return A native one phase XA resource.
     * 
     * @throws XAResourceNotAvailableException
     */
    public XAResource getOnePhaseXAResource(UOWCoordinator uowCoord) throws XAResourceNotAvailableException;

    /**
     * Obtains a two phase capable native XA resource.
     * 
     * @param pmiName The back-end resource factory/data source name.
     * @param xid The transaction ID for which this request is being made.
     * 
     * @return A native two phase XA resource.
     * 
     * @throws XAResourceNotAvailableException
     */
    public XAResource getTwoPhaseXAResource(Xid xid) throws XAResourceNotAvailableException;

    /**
     * Obtains a native XA resource information object reference. It contains
     * resource specific data to be persisted by the transaction manager.
     * 
     * @param pmiName The back-end resource factory/data source name information.
     * @param xid The transaction ID for which this request is being made.
     * 
     * @return A native XA resource information object.
     */
    public Serializable getXAResourceInfo(Xid xid);

    /**
     * Enlistment request notification.
     * 
     * @param uowc The unit of work coordinator under which the enlistment is taking place.
     * @param resource The resource to be enlisted.
     */
    public void enlist(UOWCoordinator uowc, XAResource resource);

    /**
     * Delistment request notification.
     * 
     * @param uowc The unit of work coordinator under which the enlistment is taking place.
     * @param resource The resource to be delisted.
     */
    public void delist(UOWCoordinator uowc, XAResource resource);
}