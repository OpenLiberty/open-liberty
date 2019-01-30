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
package com.ibm.ws.jaxws.metadata.builder;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.jaxws.metadata.JaxWsModuleType;

/**
 * The base impl of JaxWsModuleInfoBuilderExtension, set the enclosing JaxWsModuleInfoBuilder types
 */
public abstract class AbstractJaxWsModuleInfoBuilderExtension implements JaxWsModuleInfoBuilderExtension {

    private final Set<JaxWsModuleType> supportTypes = new HashSet<JaxWsModuleType>();

    public AbstractJaxWsModuleInfoBuilderExtension(JaxWsModuleType... supportTypes) {
        for (JaxWsModuleType supportType : supportTypes) {
            this.supportTypes.add(supportType);
        }
    }

    @Override
    public Set<JaxWsModuleType> getSupportTypes() {
        return this.supportTypes;
    }
}
