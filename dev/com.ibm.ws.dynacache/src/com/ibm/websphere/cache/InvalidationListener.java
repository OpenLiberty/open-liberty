/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

import com.ibm.websphere.cache.InvalidationEvent;

/**
 * The listener interface for removing cache entry from the cache.
 * @ibm-api 
 */
public interface InvalidationListener extends java.util.EventListener
{
    /**
     * Invoked when the cache is removed from the cache
     * @ibm-api 
     */
    public void fireEvent(InvalidationEvent e);
}
