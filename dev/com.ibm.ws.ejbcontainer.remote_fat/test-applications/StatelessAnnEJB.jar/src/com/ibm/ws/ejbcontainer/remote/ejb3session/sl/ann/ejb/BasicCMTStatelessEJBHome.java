/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * (Remote)Home interface for basic Container Managed Transaction Stateless
 * Session bean.
 **/
public interface BasicCMTStatelessEJBHome extends EJBHome {
    /**
     * @return BasicCMTStatelessEJB The StatelessBean EJB (remote)object.
     * @exception javax.ejb.CreateException
     *                StatelessBean EJB (remote) object was not created.
     */
    public BasicCMTStatelessEJB create() throws CreateException, RemoteException;
}