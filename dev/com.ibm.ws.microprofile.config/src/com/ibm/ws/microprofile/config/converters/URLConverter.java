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

import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class URLConverter extends BuiltInConverter {

    @Trivial
    public URLConverter() {
        super(URL.class);
    }

    /** {@inheritDoc} */
    @Override
    public URL convert(String value) {
        URL converted = null;
        if (value != null) {
            try {
                converted = new URL(value);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return converted;
    }
}
