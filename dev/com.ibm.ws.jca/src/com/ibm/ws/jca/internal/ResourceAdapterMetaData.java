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
package com.ibm.ws.jca.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.jca.metadata.ConnectorModuleMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Component Metadata for resource adapters
 */
public class ResourceAdapterMetaData extends MetaDataImpl implements ComponentMetaData, IdentifiableComponentMetaData {

    private final ModuleMetaData rarMD;
    private final String id;
    private final String name;
    private final J2EEName j2eeName;
    private final boolean isEmbedded;

    /**
     * Creates the ResourceAdapterMetaData.
     *
     * @param cmmd The connector module metadaga
     * @param id The id of the resource adapter
     * @param name The module name of the resource adapter
     * @param jName The J2EEName of the application representing the resource adapter
     * @param isEmbedded Whether the adapter is embedded in an application
     */
    public ResourceAdapterMetaData(ConnectorModuleMetaData cmmd, String id, String name, J2EEName jName, boolean isEmbedded) {
        super(0);
        this.rarMD = cmmd;
        this.id = id;
        this.name = name;
        this.j2eeName = jName;
        this.isEmbedded = isEmbedded;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.metadata.MetaData#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * return the id of the resource adapter
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    @Override
    public void setMetaData(MetaDataSlot slot, Object metadata) {}

    @Override
    public Object getMetaData(MetaDataSlot slot) {
        return null;
    }

    @Override
    public void release() {}

    @Override
    public ModuleMetaData getModuleMetaData() {
        return rarMD;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.metadata.ComponentMetaData#getJ2EEName()
     */
    @Override
    public J2EEName getJ2EEName() {
        return j2eeName;
    }

    /**
     * @return the isEmbedded
     */
    public boolean isEmbedded() {
        return isEmbedded;
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData#getPersistentIdentifier()
     */
    @Override
    public String getPersistentIdentifier() {
        return "CONNECTOR#" + j2eeName.toString();
    }
}
