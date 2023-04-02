/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config.converters;

import java.net.URI;
import java.net.URISyntaxException;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class URIConverter extends BuiltInConverter {

    @Trivial
    public URIConverter() {
        super(URI.class);
    }

    /** {@inheritDoc} */
    @Override
    public URI convert(String value) {
        URI converted = null;
        if (value != null) {
            try {
                converted = new URI(value);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return converted;
    }
}
