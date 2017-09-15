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
package com.ibm.ws.ejbcontainer.mock;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 *
 */
public class MockModuleMetaData extends MetaDataImpl implements ModuleMetaData {

    private J2EEName ivJ2eeName = null;
    private ApplicationMetaData ivAMD = null;

    public MockModuleMetaData(J2EEName j2eeName, ApplicationMetaData amd) {
        super(1);
        ivJ2eeName = j2eeName;
        ivAMD = amd;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "mod";
    }

    /** {@inheritDoc} */
    @Override
    public J2EEName getJ2EEName() {
        return ivJ2eeName;
    }

    /** {@inheritDoc} */
    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return ivAMD;
    }

    /** {@inheritDoc} */
    @Override
    public ComponentMetaData[] getComponentMetaDatas() {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
