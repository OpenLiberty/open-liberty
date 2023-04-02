/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
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
package com.ibm.websphere.csi;

import com.ibm.ws.ejbcontainer.util.PoolManager;

/**
 * A PrimaryPooledObject is used as the "primary copy" for creating instances where creation
 * through the default (no-arg) constructor will not suffice. Typically this is in cases
 * where one needs to copy instance variables from the primary copy into each new created copy.
 *
 * @see PooledObject
 * @see Pool
 * @see PoolManager
 */

public interface PrimaryPooledObject extends PooledObject {

    /**
     * Called when the Pool requires a new instance of the PooledObject. Note that
     * a PooledObject type is returned (not a PrimaryPooledObject); this is because
     * typically a single PrimaryPooledObject is used to create multiple copies of
     * PooledObject. (If necessary, the implementation of this method may of course
     * return a PrimaryPooledObject, which the caller can then cast back to
     * PrimaryPooledObject as needed.)
     */
    public PooledObject newInstance();

}
