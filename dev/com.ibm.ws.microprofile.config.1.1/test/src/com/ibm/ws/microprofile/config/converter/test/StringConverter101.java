/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config.converter.test;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

@Priority(101)
public class StringConverter101 implements Converter<String> {

    /** {@inheritDoc} */
    @Override
    public String convert(String value) throws IllegalArgumentException {
        return "101=" + value;
    }

}
