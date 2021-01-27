/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.converter.converters;

import org.eclipse.microprofile.config.spi.Converter;

import io.openliberty.microprofile.config.internal_fat.apps.converter.CustomType;

@SuppressWarnings("serial")
public class MyBadConverter implements Converter<CustomType> {

    /** {@inheritDoc} */
    @Override
    public CustomType convert(String value) {
        throw new IllegalArgumentException("Converter throwing intentional exception");
    }

}