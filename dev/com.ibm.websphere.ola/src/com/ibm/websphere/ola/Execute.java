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
 * Remote interface for Enterprise Bean: Execute
 *
 * @ibm-api
 */
public interface Execute extends javax.ejb.EJBObject {

	/**
   * Method which gets control when driven by OLA.
   *
	 * @param data
	 * @return
	 * @throws java.rmi.RemoteException
	 */
	public byte[] execute(byte[] data) throws java.rmi.RemoteException;
}
