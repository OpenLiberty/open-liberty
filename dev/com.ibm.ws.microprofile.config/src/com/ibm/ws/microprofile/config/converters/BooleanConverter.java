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
package com.ibm.ws.microprofile.config.converters;

import java.lang.reflect.Type;

/**
 *
 */
public class BooleanConverter extends BuiltInConverter {

    public BooleanConverter() {
        super(Boolean.class);
    }

    /**
     * allows primitive boolean converter to be an sub-class
     * 
     * @param type
     */
    public BooleanConverter(Type type) {
        super(type);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean convert(String v) {
        if (v == null) {
            return null;
        }
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("y") || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("1")) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }
}
