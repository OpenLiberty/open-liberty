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

package com.ibm.ws.microprofile.config13.converters;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.converters.AtomicIntegerConverter;
import com.ibm.ws.microprofile.config.converters.AtomicLongConverter;
import com.ibm.ws.microprofile.config.converters.AutomaticConverter;
import com.ibm.ws.microprofile.config.converters.BitSetConverter;
import com.ibm.ws.microprofile.config.converters.BooleanConverter;
import com.ibm.ws.microprofile.config.converters.CurrencyConverter;
import com.ibm.ws.microprofile.config.converters.OptionalConverter;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config12.converters.ClassConverter;
import com.ibm.ws.microprofile.config12.converters.ListConverter;
import com.ibm.ws.microprofile.config12.converters.SetConverter;

/**
 * The helper class returns all the built-in converters.
 *
 */
public class Config13DefaultConverters {

    private static final PriorityConverterMap defaultConverters = new PriorityConverterMap();

    static {
        defaultConverters.addConverter(new OptionalConverter());

        defaultConverters.addConverter(new BooleanConverter());
        defaultConverters.addConverter(new AutomaticConverter(Integer.class));
        defaultConverters.addConverter(new AutomaticConverter(Long.class));
        defaultConverters.addConverter(new AutomaticConverter(Short.class));
        defaultConverters.addConverter(new AutomaticConverter(Byte.class));
        defaultConverters.addConverter(new AutomaticConverter(Double.class));
        defaultConverters.addConverter(new AutomaticConverter(Float.class));

        defaultConverters.addConverter(new AtomicIntegerConverter());
        defaultConverters.addConverter(new AtomicLongConverter());

        defaultConverters.addConverter(new CurrencyConverter());
        defaultConverters.addConverter(new BitSetConverter());

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
    @Trivial
    public static PriorityConverterMap getDefaultConverters() {
        return defaultConverters;
    }
}
