/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
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
package com.ibm.websphere.cache;

import com.ibm.websphere.cache.ChangeEvent;

/**
 * Implement this interface to receive the ChangeEvent notifications.
 * @ibm-api 
 */
public interface ChangeListener extends java.util.EventListener
{
    /**
     * This method is invoked when there is a change to a cache	entry.
     * @ibm-api 
     */
    public void cacheEntryChanged(ChangeEvent e);
}

