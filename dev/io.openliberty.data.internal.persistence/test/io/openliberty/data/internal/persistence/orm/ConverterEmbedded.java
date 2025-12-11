/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

import io.openliberty.data.internal.persistence.orm.TestConverters.MethodConverter;
import jakarta.persistence.Convert;

/**
 * Embedded class with a converter
 */
public class ConverterEmbedded {

    private String lastName;

    @Convert(converter = MethodConverter.class)
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
