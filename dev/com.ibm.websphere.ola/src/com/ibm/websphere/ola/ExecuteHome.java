/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ola;

/**
 * Home interface for Enterprise Bean: Execute
 *
 * @ibm-api
 */
public interface ExecuteHome extends javax.ejb.EJBHome {

	/**
	 * Creates a default instance of Session Bean: Execute
	 */
	public com.ibm.websphere.ola.Execute create()
		throws javax.ejb.CreateException,
		java.rmi.RemoteException;
}
