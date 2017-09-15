/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy.extended;

import java.util.List;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 *
 */
public interface ExtendedModuleInfo extends ModuleInfo {
    /**
     * @return
     */
    ModuleMetaData getMetaData();

    /**
     * @param moduleType
     * @param nestedMetaData
     */
    void putNestedMetaData(String moduleType, ModuleMetaData nestedMetaData);

    /**
     * @param moduleType
     * @return
     */
    ModuleMetaData getNestedMetaData(String moduleType);

    /**
     * @return
     */
    List<ModuleMetaData> getNestedMetaData();
}
