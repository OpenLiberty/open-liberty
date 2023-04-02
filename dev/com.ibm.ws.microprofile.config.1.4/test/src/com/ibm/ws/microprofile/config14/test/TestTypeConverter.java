/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test;

import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 */
public class TestTypeConverter implements Converter<TestType> {

    private static int counter = 0;

    /** {@inheritDoc} */
    @Override
    public TestType convert(String value) {
        counter++;
        return new TestType(value + " - " + counter);
    }

}
