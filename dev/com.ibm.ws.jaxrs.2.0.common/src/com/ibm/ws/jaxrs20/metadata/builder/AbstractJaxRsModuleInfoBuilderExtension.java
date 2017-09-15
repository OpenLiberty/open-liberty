/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.metadata.builder;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.jaxrs20.api.JaxRsModuleInfoBuilderExtension;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleType;

/**
 * The base impl of JaxWsModuleInfoBuilderExtension, set the enclosing JaxWsModuleInfoBuilder types
 */
public abstract class AbstractJaxRsModuleInfoBuilderExtension implements JaxRsModuleInfoBuilderExtension {

    private final Set<JaxRsModuleType> supportTypes = new HashSet<JaxRsModuleType>();

    public AbstractJaxRsModuleInfoBuilderExtension(JaxRsModuleType... supportTypes) {
        for (JaxRsModuleType supportType : supportTypes) {
            this.supportTypes.add(supportType);
        }
    }

    @Override
    public Set<JaxRsModuleType> getSupportTypes() {
        return this.supportTypes;
    }
}
