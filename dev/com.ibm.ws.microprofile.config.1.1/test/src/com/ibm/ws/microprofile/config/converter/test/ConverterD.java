/*******************************************************************************

    Copyright (c) 2017 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
    Contributors:

    IBM Corporation - initial API and implementation

*******************************************************************************/
package com.ibm.ws.microprofile.config.converter.test;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to convert to ClassD. If the input string is "", a null should be returned.
 */
public class ConverterD implements Converter<ClassD> {

    private final int conversionCount = 0;

    /** {@inheritDoc} */
    @Override
    public ClassD convert(String value) {
        if ("".equals(value)) {
            return null;
        }
        return new ClassD(value);
    }

    public int getConversionCount() {
        return conversionCount;
    }

}
