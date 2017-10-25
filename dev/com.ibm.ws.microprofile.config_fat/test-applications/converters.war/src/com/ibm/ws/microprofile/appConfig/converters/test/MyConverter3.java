/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

public class MyConverter3 implements Converter<CustomPropertyObject3> {

    /** {@inheritDoc} */
    @Override
    public CustomPropertyObject3 convert(String value) {
        CustomPropertyObject3 result = null;
        try {
            result = CustomPropertyObject3.create(value);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }

}