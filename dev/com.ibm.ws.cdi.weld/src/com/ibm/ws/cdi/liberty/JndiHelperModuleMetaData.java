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
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * A module MetaData used for JNDI validation
 */
public class JndiHelperModuleMetaData implements ModuleMetaData {

    private final ApplicationMetaData application;

    public JndiHelperModuleMetaData(ApplicationMetaData application) {
        this.application = application;

    }

    @Override
    public void setMetaData(MetaDataSlot slot, Object metadata) {}

    @Override
    public void release() {}

    @Override
    public String getName() {
        return application.getName();
    }

    @Override
    public Object getMetaData(MetaDataSlot slot) {
        return null;
    }

    @Override
    public J2EEName getJ2EEName() {
        return application.getJ2EEName();
    }

    /** {@inheritDoc} */
    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return application;
    }

    /** {@inheritDoc} */
    @Override
    public ComponentMetaData[] getComponentMetaDatas() {
        return null;
    }

}
