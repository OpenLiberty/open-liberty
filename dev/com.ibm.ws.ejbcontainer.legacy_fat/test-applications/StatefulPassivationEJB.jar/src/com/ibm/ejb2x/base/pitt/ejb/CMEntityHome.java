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

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * This interface provides home interface to simple entity bean to use
 * for testing the container, deployment tools. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: CMEntityHome.java,v 1.6 1999/01/08 16:25:14 chriss Exp $
 */
public interface CMEntityHome extends EJBHome {
    /**
     * Create a new entity instance with the specified primary key.
     */
    public CMEntity create(CMKey primaryKey) throws CreateException, RemoteException;
}