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
package com.ibm.ws.microprofile.config12.converter.implicit.converters;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config12.converter.implicit.beans.StringCtorType;

@Priority(2) //just high enough the beat the implict string constructor
public class StringCtorTypeConverter implements Converter<StringCtorType> {

    /** {@inheritDoc} */
    @Override
    public StringCtorType convert(String value) {
        return new StringCtorType(value, "StringCtorTypeConverter");
    }

}
