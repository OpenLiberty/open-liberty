/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw.objectpool;

/**
 * ObjectFactory interface for creating objects that can be pooled.
 * 
 */
public interface ObjectFactory {

    /**
     * Create an entry for the pool
     * 
     * @return Object
     */
    Object create();

}
