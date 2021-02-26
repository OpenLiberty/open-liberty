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

package com.ibm.ambiguous.ejb;

/**
 * Remote Home interface for Enterprise Bean: AmbiguousName
 */
public interface AmbiguousNameRemoteHome extends javax.ejb.EJBHome {
    /**
     * Creates a default instance of Session Bean: AmbiguousName
     */
    public AmbiguousRemoteName create() throws javax.ejb.CreateException, java.rmi.RemoteException;
}
