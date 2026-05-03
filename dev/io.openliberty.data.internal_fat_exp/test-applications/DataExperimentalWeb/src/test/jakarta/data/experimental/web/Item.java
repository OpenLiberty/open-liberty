/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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
package test.jakarta.data.experimental.web;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;

import test.jakarta.data.experimental.web.Item.UUIDConverter;

/**
 * Simple unannotated entity with a UUID type indicating the Id.
 * This corresponds to the Product entity in io.openliberty.data.internal_fat
 * and is duplicated here as a place to put some experimental function that
 * hasn't made it into Jakarta Data.
 */
@Convert(attributeName = "pk", converter = UUIDConverter.class)
public class Item {
    public String description;

    public String name;

    public UUID pk; // Do not add Id to this name. It should be detectable based on type alone.

    public float price;

    public long version;

    // TODO remove once EclipseLink issue https://github.com/OpenLiberty/open-liberty/issues/34730 is fixed
    public static class UUIDConverter implements AttributeConverter<UUID, String> {
        @Override
        public String convertToDatabaseColumn(UUID uuid) {
            return uuid == null ? null : uuid.toString();
        }

        @Override
        public UUID convertToEntityAttribute(String s) {
            return s == null ? null : UUID.fromString(s);
        }
    }
}
