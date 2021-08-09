/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.converters;

import java.util.Currency;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class CurrencyConverter extends BuiltInConverter {

    @Trivial
    public CurrencyConverter() {
        super(Currency.class);
    }

    /** {@inheritDoc} */
    @Override
    public Currency convert(String value) {
        Currency converted = null;
        if (value != null) {
            converted = Currency.getInstance(value);
        }
        return converted;
    }
}
