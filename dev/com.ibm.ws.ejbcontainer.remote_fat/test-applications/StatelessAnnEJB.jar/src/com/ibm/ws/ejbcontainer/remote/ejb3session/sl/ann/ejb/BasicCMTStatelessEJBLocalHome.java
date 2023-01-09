/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * LocalHome interface for basic Container Managed Transaction Stateless Session
 * bean.
 **/
public interface BasicCMTStatelessEJBLocalHome extends EJBLocalHome {
    /**
     * @return BasicCMTStatelessEJBLocal The StatelessBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatelessBean EJB object was not created.
     */
    public BasicCMTStatelessEJBLocal create() throws CreateException;
}