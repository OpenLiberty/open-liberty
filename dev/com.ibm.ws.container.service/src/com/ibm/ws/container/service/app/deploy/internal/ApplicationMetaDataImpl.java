/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;

/**
 *
 */
class ApplicationMetaDataImpl extends MetaDataImpl implements ApplicationMetaData {

    private final J2EEName j2eeName;

    public ApplicationMetaDataImpl(J2EEName j2eeName) {
        super(0);
        this.j2eeName = j2eeName;
    }

    @Override
    public String getName() {
        return this.j2eeName.getApplication();
    }

    @Override
    public J2EEName getJ2EEName() {
        return this.j2eeName;
    }

    @Override
    public boolean createComponentMBeans() {
        return false;
    }

}
