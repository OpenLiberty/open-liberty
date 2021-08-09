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
package com.ibm.ws.microprofile.config12.converter.implicit.beans;

/**
 *
 */
public class MissingStringCtorType {

    private final String value;
    private final String converter;

    public MissingStringCtorType(String value, String converter) {
        this.value = value;
        this.converter = converter;
    }

    public String getValue() {
        return value;
    }

    public String getConverter() {
        return converter;
    }

}
