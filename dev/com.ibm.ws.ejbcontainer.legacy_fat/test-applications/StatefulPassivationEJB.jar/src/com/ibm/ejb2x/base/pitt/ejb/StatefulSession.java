/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.pitt.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * Simple test EJB that expects to be deployed as a stateful
 * session bean. <p>
 * 
 * This bean is used for testing the container and deployment tools. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: StatefulSession.java,v 1.17 1999/11/30 20:10:26 chriss Exp $
 */
public interface StatefulSession extends EJBObject {
    public int increment() throws RemoteException;

    /**
     * Call increment on both supplied entity beans, outside the scope of a
     * transaction.
     */
    public void txNotSupportedDelegate(CMEntity b1, CMEntity b2) throws BeanException1, BeanException2, RemoteException;

    public void rollbackOnly() throws RemoteException;

    /**
     * Utility method for RT61259, delegate getNonpersistent call to given entity
     * bean instance.
     */
    public int getNonpersistent(BMEntity bean) throws RemoteException;
}