/*******************************************************************************
 * Copyright (c) 1998, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *  The <code>OrbUtils</code> provides the EJB container with access
 *  to some ORB specific utilities.
 */

package com.ibm.websphere.csi;

import java.rmi.RemoteException; //d135584

public interface OrbUtils {

    /**
     * Connect the given object with the communications layer (ORB).
     * 
     * @param stub the object to register; will be a stub created by
     *            the orb
     * 
     * @exception CSIException thrown if the connect fails
     * 
     */

    public void connectToOrb(Object stub)
                    throws CSIException;

    /**
     * Map an RMI exception thrown in the container to a transport
     * (ORB) specific exception. For IIOP, this will be mapped to
     * a subclass of org.omg.CORBA.SystemException.
     * 
     * @param e the Exception to be mapped
     * @param message an informational String which may be included
     * @param minorCode a minor code describing the specific problem
     * 
     * @exception CSIException thrown if the mapping fails
     */

    public Exception mapException(RemoteException e, int minorCode) //d135584
    throws CSIException;

    public Exception mapException(RemoteException e) //d135584
    throws CSIException;

}
