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
package com.ibm.ws.microprofile.config.converter.test;

import org.eclipse.microprofile.config.spi.Converter;

public class ConverterA<T extends ClassA> implements Converter<T> {

    private int conversionCount = 0;

    /** {@inheritDoc} */
    @Override
    public T convert(String value) {
        conversionCount++;
        return (T) ClassB.newClassB(value);
    }

    public int getConversionCount() {
        return conversionCount;
    }

}
