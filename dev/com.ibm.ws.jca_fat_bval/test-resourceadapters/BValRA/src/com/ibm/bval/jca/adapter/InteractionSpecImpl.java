/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package com.ibm.bval.jca.adapter;

import javax.resource.cci.InteractionSpec;

/**
 * Example InteractionSpec implementation with a single property, functionName,
 * which determines the function that the interaction performs. Must be one of: ADD, FIND, REMOVE.
 */
public class InteractionSpecImpl implements InteractionSpec {
    private static final long serialVersionUID = 5603920830755093398L;

    private String functionName;
    private int xmlBValIntValue;

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public int getXmlBValIntValue() {
        return xmlBValIntValue;
    }

    public void setXmlBValIntValue(int i) {
        this.xmlBValIntValue = i;
    }
}
