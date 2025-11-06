/*
 * Copyright (c) 1998, 2023 Oracle and/or its affiliates. All rights reserved.
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
//     05/16/2008-1.0M8 Guy Pelletier
//       - 218084: Implement metadata merging functionality between mapping files
//     03/27/2009-2.0 Guy Pelletier
//       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
//     04/27/2010-2.1 Guy Pelletier
//       - 309856: MappedSuperclasses from XML are not being initialized properly
//     03/24/2011-2.3 Guy Pelletier
//       - 337323: Multi-tenant with shared schema support (part 1)
package org.eclipse.persistence.internal.jpa.metadata.converters;

import java.io.Serializable;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.SerializedObjectConverter;
import org.eclipse.persistence.mappings.converters.TypeConversionConverter;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappingAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataClass;

/**
 * INTERNAL:
 * Abstract converter class that parents both the JPA and Eclipselink
 * converters.
 *
 * Key notes:
 * - any metadata mapped from XML to this class must be compared in the
 *   equals method.
 * - when loading from annotations, the constructor accepts the metadata
 *   accessor this metadata was loaded from. Used it to look up any
 *   'companion' annotation needed for processing.
 * - methods should be preserved in alphabetical order.
 *
 * @author Guy Pelletier
 * @since EclipseLink 1.2
 */
public class LobMetadata extends MetadataConverter {
    /**
     * INTERNAL:
     * Used for XML loading.
     */
    public LobMetadata() {
        super("<lob>");
    }

    /**
     * INTERNAL:
     * Used for annotation loading.
     */
    public LobMetadata(MetadataAnnotation lob, MetadataAccessor accessor) {
        super(lob, accessor);

        // Nothing to read off a lob.
    }

    /**
     * INTERNAL:
     */
    @Override
    public boolean equals(Object objectToCompare) {
        return super.equals(objectToCompare) && objectToCompare instanceof LobMetadata;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * INTERNAL:
     * Returns true if the given class is a valid blob type.
     */
    public static boolean isValidBlobType(MetadataClass cls) {
        return cls.equals(byte[].class) ||
               cls.equals(Byte[].class) ||
               cls.equals(java.sql.Blob.class);
    }

    /**
     * INTERNAL:
     * Returns true if the given class is a valid clob type.
     */
    public static boolean isValidClobType(MetadataClass cls) {
        return cls.equals(char[].class) ||
               cls.equals(String.class) ||
               cls.equals(Character[].class) ||
               cls.equals(java.sql.Clob.class);
    }

    /**
     * INTERNAL:
     * Returns true if the given class is a valid lob type.
     */
    public static boolean isValidLobType(MetadataClass cls) {
        return isValidClobType(cls) || isValidBlobType(cls);
    }

    /**
     * INTERNAL:
     * Every converter needs to be able to process themselves.
     */
    public void process(DatabaseMapping mapping, MappingAccessor accessor, MetadataClass referenceClass, boolean isForMapKey) {
        // Set the field classification type on the mapping based on the
        // referenceClass type.
        if (isValidClobType(referenceClass)) {
            setFieldClassification(mapping, java.sql.Clob.class, isForMapKey);
            TypeConversionConverter typeConversionConverter = new TypeConversionConverter(mapping);
            typeConversionConverter.setDataClass(java.sql.Clob.class);
            setConverter(mapping, typeConversionConverter, isForMapKey);
        } else if (isValidBlobType(referenceClass)) {
            setFieldClassification(mapping, java.sql.Blob.class, isForMapKey);
            TypeConversionConverter typeConversionConverter = new TypeConversionConverter(mapping);
            typeConversionConverter.setDataClass(java.sql.Blob.class);
            setConverter(mapping, typeConversionConverter, isForMapKey);
        } else if (referenceClass.extendsInterface(Serializable.class)) {
            setFieldClassification(mapping, java.sql.Blob.class, isForMapKey);
            setConverter(mapping, new SerializedObjectConverter(mapping), isForMapKey);
        } else {
            // The referenceClass is neither a valid BLOB or CLOB attribute.
            throw ValidationException.invalidTypeForLOBAttribute(mapping.getAttributeName(), referenceClass, accessor.getJavaClass());
        }
    }
}
