/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.bval.jca.adapter;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

/**
 * Example activation spec.
 */
public class ActivationSpecImpl implements ActivationSpec {
    private ResourceAdapter adapter;
    private String functionName;
    private int xmlBValIntValue;

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

    public int getXmlBValIntValue() {
        return xmlBValIntValue;
    }

    public void setXmlBValIntValue(int i) {
        this.xmlBValIntValue = i;
    }
}
