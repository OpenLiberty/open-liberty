/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
public class BadConverter implements Converter<TypeWithBadConverter> {

    /** {@inheritDoc} */
    @Override
    public TypeWithBadConverter convert(String value) {
        throw new IllegalArgumentException("Converter throwing intentional exception"); // This should be thrown, though it is caught and swallowed by SmallRye Config
    }

}
