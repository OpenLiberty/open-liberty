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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

/**
 * A BuiltInConverter has a fixed priority of 1
 */
public abstract class BuiltInConverter extends PriorityConverter {

    /**
     * Construct a new PriorityConverter using explicit type and priority values
     *
     * @param type The type to convert to
     * @param converter The actual converter
     */
    @Trivial
    public BuiltInConverter(Type type) {
        super(type, ConfigConstants.BUILTIN_CONVERTER_PRIORITY);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Built In Converter for type " + getType();
    }

}
