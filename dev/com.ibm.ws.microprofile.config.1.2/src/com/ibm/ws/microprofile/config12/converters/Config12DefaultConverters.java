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

package com.ibm.ws.microprofile.config12.converters;

import com.ibm.ws.microprofile.config.converters.DefaultConverters;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;

/**
 * The helper class returns all the built-in converters.
 *
 */
public class Config12DefaultConverters {

    //start off with the same default converters as for 1.1
    private final static PriorityConverterMap defaultConverters = new PriorityConverterMap(DefaultConverters.getDefaultConverters());

    static {
        //add the class converter
        defaultConverters.addConverter(new ClassConverter());
        //add the list and set converters
        defaultConverters.addConverter(new ListConverter());
        defaultConverters.addConverter(new SetConverter());

        //set the map as unmodifiable
        defaultConverters.setUnmodifiable();
    }

    /**
     * @return defaultConverters
     */
    public static PriorityConverterMap getDefaultConverters() {
        return defaultConverters;
    }
}
