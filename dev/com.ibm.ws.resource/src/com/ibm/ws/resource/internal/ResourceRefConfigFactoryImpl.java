/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource.internal;

import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;

public class ResourceRefConfigFactoryImpl implements ResourceConfigFactory, ResourceRefConfigFactory {

    @Override
    public ResourceConfig createResourceConfig(String type) {
        return createResourceRefConfig(type);
    }

    @Override
    public ResourceRefConfig createResourceRefConfig(String type) {
        return new ResourceRefConfigImpl(null, type);
    }

    @Override
    public ResourceRefConfigList createResourceRefConfigList() {
        return new ResourceRefConfigListImpl();
    }

}
