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
package com.ibm.ws.microprofile.config12.converter.type.converters;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config12.converter.type.beans.MyStringObject;

public class MyStringObjectConverter implements Converter<MyStringObject> {

    /** {@inheritDoc} */
    @Override
    public MyStringObject convert(String value) {

        // From the MP Config spec, A Converter must "@return the converted value, or {@code null} if the value is empty"
        if (value == null || value.isEmpty()) {
            return null;
        }

        MyStringObject obj = new MyStringObject();
        obj.setValue(value);
        return obj;
    }

}
