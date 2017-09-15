/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * A PooledObjectMaster is used as the "master copy" for creating instances where creation
 * through the default (no-arg) constructor will not suffice. Typically this is in cases
 * where one needs to copy instance variables from the master copy into each new created copy.
 * 
 * @see PooledObject
 * @see Pool
 * @see PoolManager
 */

public interface PooledObjectMaster extends PooledObject {

    /**
     * Called when the Pool requires a new instance of the PooledObject. Note that
     * a PooledObject type is returned (not a PooledObjectMaster); this is because
     * typically a single PooledObjectMaster is used to create multiple copies of
     * PooledObject. (If necessary, the implementation of this method may of course
     * return a PooledObjectMaster, which the caller can then cast back to
     * PooledObjectMaster as needed.)
     */
    public PooledObject newInstance();

}
