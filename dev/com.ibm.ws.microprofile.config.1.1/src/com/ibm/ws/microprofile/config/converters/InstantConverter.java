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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class InstantConverter extends BuiltInConverter {

    @Trivial
    public InstantConverter() {
        super(Instant.class);
    }

    /** {@inheritDoc} */
    @Override
    public Instant convert(String value) {
        Instant converted = null;
        if (value != null) {
            try {
                converted = Instant.from(OffsetDateTime.parse(value));
            } catch (DateTimeException dte) {
                throw new IllegalArgumentException(dte);
            }
        }
        return converted;
    }
}
