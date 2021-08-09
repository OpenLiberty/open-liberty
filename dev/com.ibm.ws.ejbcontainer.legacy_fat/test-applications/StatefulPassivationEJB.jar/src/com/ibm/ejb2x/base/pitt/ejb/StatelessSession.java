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
 * Simple test EJB that expects to be deployed as a stateless
 * session bean. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: StatelessSession.java,v 1.9 1999/11/30 20:11:46 chriss Exp $
 */
public interface StatelessSession extends EJBObject {
    /**
     * Utility method for regression test 70091.
     */
    public EJBObject getEJBObject() throws RemoteException;
}