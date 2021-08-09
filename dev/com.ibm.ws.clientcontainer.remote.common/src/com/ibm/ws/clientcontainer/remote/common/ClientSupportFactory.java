/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.remote.common;

import java.rmi.RemoteException;

/**
 * Provides access to the ClientSupport remote interface of a singleton class bound in CosNaming
 * to allow remote clients (the client container in particular) to access objects bound in the
 * server's namespace.
 */
public interface ClientSupportFactory {

    /**
     * Returns the remote reference (Stub) to the ClientSupport instance on the
     * default server process.
     *
     * @throws RemoteException if the remote ClientSupport reference cannot be obtained
     */
    ClientSupport getRemoteClientSupport() throws RemoteException;

}
