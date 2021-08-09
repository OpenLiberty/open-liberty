/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.adapter;

import javax.resource.spi.ConnectionManager;
import com.ibm.ws.resource.ResourceRefInfo;

/**
 * WebSphere Application Server extensions to the ConnectionManager interface.
 */
public interface WSConnectionManager extends ConnectionManager {
    /**
     * Returns the purge policy for this connection manager.
     * 
     * @return the purge policy for this connection manager.
     */
    PurgePolicy getPurgePolicy();

    /**
     * Returns resource reference attributes.
     * 
     * @return resource reference attributes.
     */
    ResourceRefInfo getResourceRefInfo();
}