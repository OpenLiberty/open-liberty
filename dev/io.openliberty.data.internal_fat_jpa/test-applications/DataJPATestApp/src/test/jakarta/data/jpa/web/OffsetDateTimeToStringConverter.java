/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import java.time.OffsetDateTime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A converter between OffsetDateTime and String.
 */
@Converter
public class OffsetDateTimeToStringConverter implements AttributeConverter<OffsetDateTime, String> {

    @Override
    public String convertToDatabaseColumn(OffsetDateTime d) {
        return d.toString();
    }

    @Override
    public OffsetDateTime convertToEntityAttribute(String s) {
        return OffsetDateTime.parse(s);
    }

}
