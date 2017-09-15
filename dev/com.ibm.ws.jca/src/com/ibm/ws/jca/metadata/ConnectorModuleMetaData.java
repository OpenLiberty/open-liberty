/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.metadata;

import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Metadata interface for rar modules.
 */
public interface ConnectorModuleMetaData extends ModuleMetaData {
    /**
     * Returns the unique identifier for the RAR module
     * 
     * @return the unique identifier for the RAR module
     */
    public String getIdentifier();

    /**
     * Returns the JCA specification version with which the resource adapter claims compliance.
     * 
     * @return the JCA specification version with which the resource adapter claims compliance.
     */
    public String getSpecVersion();

    /**
     * Indicates whether or not the RAR module is embedded in an application.
     * 
     * @return true if the RAR module is embedded in an application. Otherwise false.
     */
    public boolean isEmbedded();
}
