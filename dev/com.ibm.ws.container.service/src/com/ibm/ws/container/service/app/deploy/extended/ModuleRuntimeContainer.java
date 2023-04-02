/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.container.service.app.deploy.extended;

import java.util.concurrent.Future;

import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 *
 */
public interface ModuleRuntimeContainer {

    /**
     * @param moduleInfo
     * @return non-null metadata otherwise must throw a MetaDataException
     */
    ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException;

    /**
     * @param moduleInfo
     */
    Future<Boolean> startModule(ExtendedModuleInfo moduleInfo) throws StateChangeException;

    /**
     * @param moduleInfo
     */
    void stopModule(ExtendedModuleInfo moduleInfo);
}
