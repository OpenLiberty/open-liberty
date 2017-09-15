/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.converters.test;

import org.eclipse.microprofile.config.spi.Converter;

public class MyConverter implements Converter<CustomPropertyObject1> {

    /** {@inheritDoc} */
    @Override
    public CustomPropertyObject1 convert(String value) {
        CustomPropertyObject1 result = null;
        try {
            result = CustomPropertyObject1.create(value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}