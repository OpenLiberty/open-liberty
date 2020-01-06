/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.exception.ejb;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * Local Home interface for Enterprise Bean: SLRemoteExBean
 */
public interface SLRemoteExLocalHome extends EJBLocalHome {

    SLRemoteExLocal create() throws CreateException, RemoteException;
}
