/*******************************************************************************
 * Copyright (c) 2002, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * @ibm-private-in-use
 */
public interface EJBModuleMetaData
                extends ModuleMetaData
{
    /**
     * Get the Module Version of this EJB Module
     */
    public int getEJBModuleVersion();

    /**
     * Set the base application and module names for EJBs defined in this
     * module if it uses a forward-compatible version strategy.
     */
    // F54184 F54184.2
    public void setVersionedModuleBaseName(String appBaseName, String modBaseName);
}
