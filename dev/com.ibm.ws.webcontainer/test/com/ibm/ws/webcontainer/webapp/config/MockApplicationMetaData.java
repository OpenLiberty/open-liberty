/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp.config;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;

/**
 *
 */
public class MockApplicationMetaData extends MetaDataImpl implements ApplicationMetaData {

    private J2EEName ivJ2eeName = null;

    public MockApplicationMetaData(J2EEName j2eeName) {
        super(1);
        ivJ2eeName = j2eeName;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "app";
    }

    /** {@inheritDoc} */
    @Override
    public J2EEName getJ2EEName() {
        return ivJ2eeName;
    }

    @Override
    public boolean createComponentMBeans() {
        throw new UnsupportedOperationException("Not Implemented");
    }
}