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

import com.ibm.ws.microprofile.config12.converter.type.beans.ChildType;

/**
 * Intentionally does not have the generic type set on the Converter interface
 */
@SuppressWarnings("rawtypes")
public class ChildTypeConverter implements Converter {

    /** {@inheritDoc} */
    @Override
    public ChildType convert(String value) {
        return new ChildType(value, "SubTypeConverter");
    }

}
