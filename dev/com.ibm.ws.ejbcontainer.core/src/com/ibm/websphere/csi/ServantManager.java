/*******************************************************************************
 * Copyright (c) 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *  The <code>ServantManager</code> is responsible for satisfying key
 *  to object requests from the communications layer (ORB). <p>
 */

package com.ibm.websphere.csi;

public interface ServantManager {

    /**
     * Return <code>Object</code> instance that corresponds to the give
     * object key.
     * 
     * @exception RemoteException thrown if <code>ServantManager</code>
     *                is unable to may given key to an object instance. <p>
     */

    public Object keyToObject(byte[] key)
                    throws java.rmi.RemoteException;
}
