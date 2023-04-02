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
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public class ApplicationModuleMetaData implements ModuleMetaData {

    private final ApplicationMetaData application;

    public ApplicationModuleMetaData(ApplicationMetaData application) {
        this.application = application;
    }

    @Override
    public void setMetaData(MetaDataSlot slot, Object metadata) {
    }

    @Override
    public void release() {
    }

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
