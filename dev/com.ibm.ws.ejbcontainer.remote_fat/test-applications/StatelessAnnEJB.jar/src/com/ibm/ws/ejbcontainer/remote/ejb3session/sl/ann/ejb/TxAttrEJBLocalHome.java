/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * EJBLocalHome interface for Stateless Session bean for testing Tx Attributes,
 **/
public interface TxAttrEJBLocalHome extends EJBLocalHome {
    /**
     * Default create method with no parameters.
     * <p>
     *
     * @return TxAttrEJBLocal The StatelessBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatelessBean EJB object was not created.
     */
    public TxAttrEJBLocal create() throws CreateException;
}