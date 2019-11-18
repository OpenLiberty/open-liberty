/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal.listener.impl;

import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * A MessageEndpointHandler extension in order to support registration of RRS
 * enabled resources. This class is implemented the same way as
 * com.ibm.ws.ejbcontainer.mdb.internal.ExtendedMessageEndpointHandler class.
 */
public class ExtendedMessageEndpointHandler extends MessageEndpointHandler {

    public ExtendedMessageEndpointHandler(BaseMessageEndpointFactory factory, int recoveryId, boolean rrsTransactional) {
        super(factory, recoveryId, rrsTransactional);
    }

    /**
     * This method is used for extending the transaction enlistment logic for
     * the lightweight server. It can be overridden to return a native RRS
     * XAResource
     * 
     * @param coord
     *            - The UOWCoordinator.
     * @throws XAResourceNotAvailableException
     */
    @Override
    protected XAResource getNativeRRSXAR(UOWCoordinator coord) throws XAResourceNotAvailableException {
        try {
            return getBaseMessageEndpointFactory().getBatchExecutor().getRRSXAResource(null, coord.getXid());
        } catch (XAResourceNotAvailableException xnex) {
            throw xnex;
        } catch (Exception ex) {
            throw new XAResourceNotAvailableException(ex);
        }
    }
}
