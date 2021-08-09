/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public class TestModuleMetaData extends TestMetaDataImpl implements ModuleMetaData {
    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return null;
    }

    @Override
    public ComponentMetaData[] getComponentMetaDatas() {
        return null;
    }

    @Override
    public J2EEName getJ2EEName() {
        return null;
    }
}
