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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.net.URL;
import java.util.Map;

import com.ibm.ws.runtime.metadata.ModuleMetaData;

public class TestHttpAuthenticationMechanismsTracker extends HttpAuthenticationMechanismsTracker {

    private final Map<URL, ModuleMetaData> moduleMetadataMap;

    public TestHttpAuthenticationMechanismsTracker(Map<URL, ModuleMetaData> moduleMetadataMap) {
        this.moduleMetadataMap = moduleMetadataMap;
    }

    @Override
    protected Map<URL, ModuleMetaData> getModuleMetaDataMap() {
        return moduleMetadataMap;
    }
}