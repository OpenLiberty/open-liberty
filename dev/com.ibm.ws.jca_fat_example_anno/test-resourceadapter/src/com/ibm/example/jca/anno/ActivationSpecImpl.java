/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.example.jca.anno;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.spi.Activation;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

/**
 * Example activation spec.
 */
@Activation(messageListeners = MessageListener.class)
public class ActivationSpecImpl implements ActivationSpec {
    private ResourceAdapter adapter;

    @ConfigProperty(description = "Function name (ADD or REMOVE), upon successful completion of which to invoke the message driven bean.")
    private String functionName;

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        if (!"ADD".equalsIgnoreCase(functionName) && !"REMOVE".equalsIgnoreCase(functionName))
            throw new InvalidPropertyException("functionName: " + functionName);
    }
}