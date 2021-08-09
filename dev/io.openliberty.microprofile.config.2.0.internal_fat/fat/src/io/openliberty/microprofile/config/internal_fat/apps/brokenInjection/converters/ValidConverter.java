/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters;

import org.eclipse.microprofile.config.spi.Converter;

@SuppressWarnings("serial")
public class ValidConverter implements Converter<TypeWithValidConverter> {

    public static final String CHECK_STRING = "In ValidConverter";

    /** {@inheritDoc} */
    @Override
    public TypeWithValidConverter convert(String value) {
        System.out.println(CHECK_STRING);
        return new TypeWithValidConverter();
    }

}
