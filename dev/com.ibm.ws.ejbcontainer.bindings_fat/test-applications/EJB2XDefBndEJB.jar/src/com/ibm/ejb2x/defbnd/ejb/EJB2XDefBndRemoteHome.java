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

package com.ibm.ejb2x.defbnd.ejb;

/**
 * Remote Home interface for Enterprise Bean: EJB2XDefBnd
 */
public interface EJB2XDefBndRemoteHome extends javax.ejb.EJBHome {
    /**
     * Creates a default instance of Session Bean: EJB2XDefBnd
     */
    public EJB2XDefBndRemote create() throws javax.ejb.CreateException, java.rmi.RemoteException;
}
