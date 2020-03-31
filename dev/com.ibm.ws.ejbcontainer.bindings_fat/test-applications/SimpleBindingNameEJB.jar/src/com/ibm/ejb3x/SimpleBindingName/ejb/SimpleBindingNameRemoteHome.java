/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb3x.SimpleBindingName.ejb;

/**
 * Home interface for Enterprise Bean: SimpleBindingName
 */
public interface SimpleBindingNameRemoteHome extends javax.ejb.EJBHome {
    /**
     * Creates a default instance of Session Bean: SimpleBindingName
     */
    public SimpleBindingRemoteName create() throws javax.ejb.CreateException, java.rmi.RemoteException;
}
