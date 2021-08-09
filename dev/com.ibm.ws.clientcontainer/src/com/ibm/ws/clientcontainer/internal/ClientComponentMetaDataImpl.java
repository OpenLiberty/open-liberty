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
package com.ibm.ws.clientcontainer.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.clientcontainer.metadata.ClientModuleMetaData;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 *
 */
public class ClientComponentMetaDataImpl extends MetaDataImpl implements ComponentMetaData, IdentifiableComponentMetaData {

    private final ClientModuleMetaData cmmd;

    ClientComponentMetaDataImpl(ClientModuleMetaData cmmd) {
        super(0);
        this.cmmd = cmmd;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ModuleMetaData getModuleMetaData() {
        return cmmd;
    }

    /** {@inheritDoc} */
    @Override
    public J2EEName getJ2EEName() {
        return cmmd.getJ2EEName();
    }

    /** {@inheritDoc} */
    @Override
    public String getPersistentIdentifier() {
        return "CLIENT#" + cmmd.getJ2EEName().toString();
    }

}
