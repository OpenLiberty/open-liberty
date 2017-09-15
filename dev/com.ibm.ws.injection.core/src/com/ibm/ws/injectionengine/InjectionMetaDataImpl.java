/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionMetaData;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class InjectionMetaDataImpl
                implements InjectionMetaData
{
    private final AbstractInjectionEngine ivInjectionEngine;
    private final ComponentNameSpaceConfiguration ivCompNSConfig;
    private final ReferenceContext ivReferenceContext;

    public InjectionMetaDataImpl(AbstractInjectionEngine injectionEngine,
                                 ComponentNameSpaceConfiguration compNSConfig,
                                 ReferenceContext refContext)
    {
        ivInjectionEngine = injectionEngine;
        ivCompNSConfig = compNSConfig;
        ivReferenceContext = refContext;
    }

    @Override
    public String toString()
    {
        return super.toString() + '[' + getComponentNameSpaceConfiguration().getOwningFlow() + ':' + getJ2EEName() + ']';
    }

    public ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration()
    {
        return ivCompNSConfig;
    }

    public ModuleMetaData getModuleMetaData()
    {
        return ivCompNSConfig.getModuleMetaData();
    }

    public J2EEName getJ2EEName()
    {
        return ivCompNSConfig.getJ2EEName();
    }

    public ReferenceContext getReferenceContext()
    {
        return ivReferenceContext;
    }

    public void bindJavaComp(String name, Object bindingObject)
                    throws InjectionException
    {
        ivInjectionEngine.bindJavaNameSpaceObject(ivCompNSConfig, InjectionScope.COMP, name, null, bindingObject);
    }
}
