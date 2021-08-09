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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;

public class ResourceRefConfigListImpl implements ResourceRefConfigList {

    private final List<ResourceRefConfig> ivList = new ArrayList<ResourceRefConfig>();
    private final Map<String, ResourceRefConfig> ivMap = new HashMap<String, ResourceRefConfig>();

    @Override
    public String toString() {
        return super.toString() + ivList;
    }

    @Override
    public int size() {
        return ivList.size();
    }

    @Override
    public ResourceRefConfig getResourceRefConfig(int i) {
        return ivList.get(i);
    }

    @Override
    public ResourceRefConfig findByName(String name) {
        return ivMap.get(name);
    }

    @Override
    public ResourceRefConfig findOrAddByName(String name) {
        ResourceRefConfig resRef = ivMap.get(name);
        if (resRef == null) {
            resRef = new ResourceRefConfigImpl(name, null);
            ivList.add(resRef);
            ivMap.put(name, resRef);
        }
        return resRef;
    }

}
