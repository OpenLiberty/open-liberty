/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.converter.type.beans;

/**
 *
 */
public class ParentType implements Converted {

    private final String value;
    private final String converter;

    public ParentType(String value, String converter) {
        this.value = value;
        this.converter = converter;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getConverter() {
        return converter;
    }

}
