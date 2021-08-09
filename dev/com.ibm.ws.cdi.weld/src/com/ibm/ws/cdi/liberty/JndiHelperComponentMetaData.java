/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.liberty;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;

/**
 * A Component MetaData used for jndi validation
 */
public class JndiHelperComponentMetaData extends MetaDataImpl implements ComponentMetaData, IdentifiableComponentMetaData {

    private final ModuleMetaData module;

    public JndiHelperComponentMetaData(ModuleMetaData module) {
        super(0);
        this.module = module;
    }

    public JndiHelperComponentMetaData(ApplicationMetaData app) {
        this(new JndiHelperModuleMetaData(app));
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
        return CDIDeferredMetaDataFactoryImpl.getPersistentIdentifier(module);
    }
}
