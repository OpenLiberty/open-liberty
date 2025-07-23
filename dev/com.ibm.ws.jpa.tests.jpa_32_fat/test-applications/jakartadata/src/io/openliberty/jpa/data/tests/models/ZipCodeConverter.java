/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ZipCodeConverter implements AttributeConverter<ZipCode, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ZipCode zipCode) {
        return zipCode != null ? zipCode.getValue() : null;
    }

    @Override
    public ZipCode convertToEntityAttribute(Integer dbData) {
        return dbData != null ? ZipCode.of(dbData) : null;
    }
}
