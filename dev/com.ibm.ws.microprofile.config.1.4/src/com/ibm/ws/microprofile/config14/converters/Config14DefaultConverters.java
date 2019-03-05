/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.config14.converters;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config13.converters.Config13DefaultConverters;

/**
 * The helper class returns all the built-in converters.
 *
 */
public class Config14DefaultConverters {

    private static final PriorityConverterMap defaultConverters = new PriorityConverterMap(Config13DefaultConverters.getDefaultConverters());

    static {
        defaultConverters.addConverter(new CharacterConverter());

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
