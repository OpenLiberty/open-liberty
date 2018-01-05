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
package com.ibm.ws.jaxrs20.utils;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 *
 */
public class CustomizerUtils {

    public static String createCustomizerKey(JaxRsFactoryBeanCustomizer customizer) {
        J2EEName j2eeName = null;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            ModuleMetaData mmd = cmd.getModuleMetaData();
            j2eeName = mmd.getJ2EEName();
        }
        return Integer.toString(customizer.hashCode()) + j2eeName;// + ":" + Thread.currentThread().getId();
    }
}
