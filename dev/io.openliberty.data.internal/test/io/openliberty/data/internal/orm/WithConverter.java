/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.orm;

import java.time.LocalDateTime;
import java.util.Set;

import io.openliberty.data.internal.orm.TestConverters.ClassConverter;
import io.openliberty.data.internal.orm.TestConverters.FieldConverter;
import io.openliberty.data.internal.orm.TestConverters.MethodConverter;
import jakarta.persistence.Convert;

/**
 * A class with a converter at every target
 */
@Convert(converter = ClassConverter.class)
public class WithConverter {

    public long id;

    public Set<String> aliases;

    @Convert(converter = FieldConverter.class)
    public String firstName;

    private String lastName;

    @Convert //Should be ignored
    public LocalDateTime version;

    @Convert(converter = MethodConverter.class)
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}
