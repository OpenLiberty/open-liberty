/*******************************************************************************
 * Copyright (c) 1998, 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *  A CSIServant is what is registered with the CSI object adapter.
 */

package com.ibm.websphere.csi;

public interface CSIServant
                extends java.rmi.Remote
{
    /**
     * Can this servant instance be wlm'ed?
     */

    public boolean wlmable()
                    throws java.rmi.RemoteException;
}
