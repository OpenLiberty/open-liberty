/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.osgi.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public class ApplicationComponentMetaData extends MetaDataImpl implements ComponentMetaData, IdentifiableComponentMetaData {

    private final ModuleMetaData module;

    public ApplicationComponentMetaData(ModuleMetaData module) {
        super(0);
        this.module = module;
    }

    public ApplicationComponentMetaData(ApplicationMetaData app) {
        this(new ApplicationModuleMetaData(app));
    }

    @Override
    public ModuleMetaData getModuleMetaData() {
        return module;
    }

    @Override
    public J2EEName getJ2EEName() {
        return module.getJ2EEName();
    }

    @Override
    public String getName() {
        return module.getName();
    }

    @Override
    public String getPersistentIdentifier() {
        return "JPA#" + module.getName();
    }
}
