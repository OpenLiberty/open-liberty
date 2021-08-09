/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata;

import com.ibm.ws.runtime.metadata.ModuleMetaData;

public interface ModuleMetaDataListener {
    /**
     * Notification that the metadata for a module has been created.
     * 
     * @param event the event, with {@link MetaDataEvent#getMetaData} returning {@link DeployedMod}
     */
    void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException;

    /**
     * Notification that the metadata for a module has been destroyed. This event might be fired without a corresponding {@link #moduleMetaDataCreated} event if an error occurred
     * while creating the metadata.
     * 
     * @param event the event
     */
    void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event);
}
