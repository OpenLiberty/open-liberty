/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.util.Map;

import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ws.ejbcontainer.EJBReferenceFactory;
import com.ibm.ws.ejbcontainer.osgi.EJBSystemModule;

public class EJBSystemModuleImpl implements EJBSystemModule {
    private final EJBRuntimeImpl runtimeImpl;
    private final EJBModuleMetaDataImpl moduleMetaData;
    private final Map<String, EJBReferenceFactory> referenceFactories;

    public EJBSystemModuleImpl(EJBRuntimeImpl runtimeImpl,
                               EJBModuleMetaDataImpl mmd,
                               Map<String, EJBReferenceFactory> referenceFactories) {
        this.runtimeImpl = runtimeImpl;
        this.moduleMetaData = mmd;
        this.referenceFactories = referenceFactories;
    }

    @Override
    public void stop() {
        runtimeImpl.stopSystemModule(moduleMetaData);
    }

    @Override
    public EJBReferenceFactory getReferenceFactory(String ejbName) {
        EJBReferenceFactory factory = referenceFactories.get(ejbName);
        if (factory == null) {
            throw new IllegalArgumentException(ejbName);
        }
        return factory;
    }
}
