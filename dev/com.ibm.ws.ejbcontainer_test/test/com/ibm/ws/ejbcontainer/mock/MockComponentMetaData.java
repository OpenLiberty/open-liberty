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
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 *
 */
public class MockComponentMetaData implements ComponentMetaData {

    private J2EEName ivJ2eeName = null;
    private ModuleMetaData ivMMD = null;

    public MockComponentMetaData(J2EEName j2eeName, ModuleMetaData mmd) {
        ivJ2eeName = j2eeName;
        ivMMD = mmd;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "ejb";
    }

    /** {@inheritDoc} */
    @Override
    public void setMetaData(MetaDataSlot slot, Object metadata) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /** {@inheritDoc} */
    @Override
    public Object getMetaData(MetaDataSlot slot) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void release() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /** {@inheritDoc} */
    @Override
    public ModuleMetaData getModuleMetaData() {
        return ivMMD;
    }

    /** {@inheritDoc} */
    @Override
    public J2EEName getJ2EEName() {
        return ivJ2eeName;
    }

}
