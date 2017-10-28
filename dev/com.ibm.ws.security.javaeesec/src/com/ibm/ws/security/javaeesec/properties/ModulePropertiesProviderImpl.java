/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
/**
 *
 */
public class ModulePropertiesProviderImpl implements ModulePropertiesProvider {
    private static final TraceComponent tc = Tr.register(ModulePropertiesProviderImpl.class);
    private Map<String, ModuleProperties> moduleMap;
    private ModulePropertiesUtils mpu = ModulePropertiesUtils.getInstance();

    public ModulePropertiesProviderImpl(Map<String, ModuleProperties> moduleMap) {
        this.moduleMap = moduleMap;
    }

    @Override
    public ModuleProperties getModuleProperties() {
        return moduleMap.get(mpu.getJ2EEModuleName());
    }

    @Override
    public List<Class> getAuthMechClassList() {
        List<Class> list = null;
        ModuleProperties mp = getModuleProperties();
        if (mp != null) {
            list = new ArrayList<Class>(mp.getAuthMechMap().keySet());
        }
        return list;
    }

    @Override
    public Properties getAuthMechProperties(Class authMech) {
        Properties props = null;
        ModuleProperties mp = getModuleProperties();
        if (mp != null) {
            props = mp.getAuthMechMap().get(authMech);
        }
        return props;
    }

    // this is for unittest
    protected void setModulePropertiesUtils(ModulePropertiesUtils mpu) {
        this.mpu = mpu;
    }
}
