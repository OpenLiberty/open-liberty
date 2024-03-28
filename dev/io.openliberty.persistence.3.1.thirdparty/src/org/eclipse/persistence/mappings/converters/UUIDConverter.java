/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation
package org.eclipse.persistence.mappings.converters;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.foundation.AbstractDirectMapping;
import org.eclipse.persistence.sessions.Session;

import java.util.UUID;

/**
 * Default UUID field value to JDBC data type converter.
 */
public class UUIDConverter implements Converter {

    /**
     * Creates an instance of default UUID field value to JDBC data type converter.
     */
    public UUIDConverter() {
    }

    /**
     * Converts UUID field value to String.
     *
     * @param uuidValue source UUID field value
     * @param session current database session
     * @return target String to be stored as JDBC VARCHAR
     */
    @Override
    public Object convertObjectValueToDataValue(Object uuidValue, Session session) {
        if (uuidValue == null) { //Issue-24925
            return null;
        }
        if (uuidValue instanceof UUID) {
            return uuidValue.toString();
        }
        throw new IllegalArgumentException("Source object is not an instance of java.util.UUID");
    }

    /**
     * Converts String from JDBC VARCHAR parameter to UUID field value.
     *
     * @param jdbcValue source String from JDBC VARCHAR
     * @param session current database session
     * @return target UUID field value
     */
    @Override
    public Object convertDataValueToObjectValue(Object jdbcValue, Session session) {
        return UUID.fromString(jdbcValue.toString());
    }

    /**
     * UUID values and String are immutable.
     *
     * @return value of {@code false}
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * Initialize mapping for JDBC data type.
     *
     * @param mapping field database mapping
     * @param session current database session
     */
    @Override
    public void initialize(DatabaseMapping mapping, Session session) {
        if (mapping.isDirectToFieldMapping()) {
            if (((AbstractDirectMapping)mapping).getFieldClassification() == null) {
                final AbstractDirectMapping directMapping = AbstractDirectMapping.class.cast(mapping);
                final Class<?> attributeClassification = mapping.getAttributeClassification();
                if (attributeClassification.isInstance(UUID.class)) {
                    directMapping.setFieldClassification(UUID.class);
                }
            }
        }
    }
}