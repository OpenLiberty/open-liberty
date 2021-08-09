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

import javax.resource.ResourceException;
import javax.resource.cci.MessageListener;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

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