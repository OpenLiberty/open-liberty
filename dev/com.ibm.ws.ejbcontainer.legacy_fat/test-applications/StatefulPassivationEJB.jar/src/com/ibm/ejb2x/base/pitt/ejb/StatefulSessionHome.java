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
 * The home interface for a stateful session bean for testing the
 * deployment tool and session container. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: StatefulSessionHome.java,v 1.5 1999/01/08 16:25:19 chriss Exp $
 */
public interface StatefulSessionHome extends EJBHome {
    public StatefulSession create() throws CreateException, RemoteException;
}