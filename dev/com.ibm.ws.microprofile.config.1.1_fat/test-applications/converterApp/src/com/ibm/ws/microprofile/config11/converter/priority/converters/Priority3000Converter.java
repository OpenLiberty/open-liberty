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
package com.ibm.ws.microprofile.config11.converter.priority.converters;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config11.converter.priority.beans.MyObject;

@Priority(3000)
public class Priority3000Converter implements Converter<MyObject> {

    /** {@inheritDoc} */
    @Override
    public MyObject convert(String value) {
        return new MyObject(value, "Priority3000Converter");
    }

}
