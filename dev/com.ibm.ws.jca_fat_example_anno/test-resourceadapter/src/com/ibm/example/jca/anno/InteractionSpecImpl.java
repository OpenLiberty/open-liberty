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
package com.ibm.example.jca.anno;

import javax.resource.cci.InteractionSpec;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;

/**
 * Example InteractionSpec implementation with a single property, functionName,
 * which determines the function that the interaction performs. Must be one of: ADD, FIND, REMOVE.
 */
@AdministeredObject
public class InteractionSpecImpl implements InteractionSpec {
    private static final long serialVersionUID = -4153264175499435511L;

    @ConfigProperty(description = "Function name. Supported values are: ADD, FIND, REMOVE")
    private String functionName;

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
}
