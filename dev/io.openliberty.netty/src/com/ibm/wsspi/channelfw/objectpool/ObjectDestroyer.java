/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * ObjectDestroyer interface for cleaning up objects that can be pooled.
 * 
 */
public interface ObjectDestroyer {

    /**
     * Destroy the input object.
     * 
     * @param obj
     */
    void destroy(Object obj);

}
