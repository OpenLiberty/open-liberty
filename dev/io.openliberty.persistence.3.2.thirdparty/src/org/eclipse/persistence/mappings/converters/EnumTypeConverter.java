/*
 * Copyright (c) 1998, 2025 Oracle and/or its affiliates. All rights reserved.
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
//     Oracle - initial API and implementation from Oracle TopLink
//      //     30/05/2012-2.4 Guy Pelletier
//       - 354678: Temp classloader is still being used during metadata processing
package org.eclipse.persistence.mappings.converters;

import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedGetDeclaredField;
import org.eclipse.persistence.internal.security.PrivilegedGetValueFromField;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.sessions.Session;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * <b>Purpose</b>: Object type converter is used to match a fixed number of
 * database data values to a Java enum object value. It can be used when the
 * values on the database and in the Java differ. To create an object type
 * converter, simply specify the set of conversion value pairs. A default value
 * and one-way conversion are also supported for legacy data situations.
 *
 * @author Guy Pelletier
 * @since Toplink 10.1.4RI
 */
public class EnumTypeConverter extends ObjectTypeConverter {
    private Class m_enumClass;
    private String m_enumClassName;
    private String m_enumFieldName;
    private boolean m_useOrdinalValues;

    /**
     * PUBLIC:
     * Creating an enum converter this way will create the conversion values
     * for you using ordinal or name values.
     */
    public EnumTypeConverter(DatabaseMapping mapping, Class<?> enumClass, boolean useOrdinalValues) {
        super(mapping);
        m_enumClass = enumClass;
        m_enumClassName = enumClass.getName();
        m_useOrdinalValues = useOrdinalValues;
        initializeConversions(m_enumClass);
    }

    /**
     * PUBLIC:
     * Creating an enum converter this way expects that you will provide
     * the conversion values separately.
     */
    public EnumTypeConverter(DatabaseMapping mapping, String enumClassName) {
        super(mapping);
        m_enumClassName = enumClassName;
    }

    //Call after convertClassNamesToClasses or at the end to ensure, that all required classes are available
    private void initializeConversions(Class enumClass) {
        // Initialize conversion if not already set by Converter
        if (getFieldToAttributeValues().isEmpty()) {
            EnumSet theEnums = EnumSet.allOf(enumClass);
            Iterator<Enum> i = theEnums.iterator();
            Field m_enumField = null;
            if (m_enumFieldName != null) {
                try {
                    m_enumField = AccessController.doPrivileged(new PrivilegedGetDeclaredField(m_enumClass, m_enumFieldName, true));
                } catch (PrivilegedActionException exception) {
                    throw ValidationException.invalidFieldForClass(m_enumFieldName, m_enumClassName);
                }
            }
            while (i.hasNext()) {
                Enum theEnum = i.next();
                if (m_enumField != null) {
                    Object theEnumValue = null;
                    try {
                        theEnumValue = AccessController.doPrivileged(new PrivilegedGetValueFromField(m_enumField, theEnum));
                    } catch (PrivilegedActionException exception) {
                        throw ConversionException.unableGetValueFromEnum(theEnums.getClass().getName(), m_enumField.getName(), exception);
                    }
                    addConversionValue(theEnumValue, theEnum.name());
                } else {
                    if (m_useOrdinalValues) {
                        addConversionValue(theEnum.ordinal(), theEnum.name());
                    } else {
                        addConversionValue(theEnum.name(), theEnum.name());
                    }
                }
            }
        }
    }

    public Class<?> getEnumClass() {
        return m_enumClass;
    }

    public String getEnumClassName() {
        return m_enumClassName;
    }

    public void setEnumFieldName(String m_enumFieldName) {
        this.m_enumFieldName = m_enumFieldName;
    }

    public void setUseOrdinalValues(boolean m_useOrdinalValues) {
        this.m_useOrdinalValues = m_useOrdinalValues;
    }

    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this converter to actual
     * class-based settings. This method is used when converting a project
     * that has been built with class names to a project with classes.
     */
    @Override
    public void convertClassNamesToClasses(ClassLoader classLoader) {
        super.convertClassNamesToClasses(classLoader);

        // convert if enumClass is null or if different classLoader
        if (m_enumClass == null || !m_enumClass.getClassLoader().equals(classLoader)) {
            try {
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    try {
                        m_enumClass = AccessController.doPrivileged(
                            new PrivilegedClassForName<>(m_enumClassName, true, classLoader));
                    } catch (PrivilegedActionException exception) {
                        throw ValidationException.classNotFoundWhileConvertingClassNames(
                            m_enumClassName, exception.getException());
                    }
                } else {
                    m_enumClass = PrivilegedAccessHelper.getClassForName(m_enumClassName, true,
                        classLoader);
                }
            } catch (ClassNotFoundException exception){
                throw ValidationException.classNotFoundWhileConvertingClassNames(m_enumClassName,
                    exception);
            }
        }
        initializeConversions(m_enumClass);
    }

    /**
     * INTERNAL:
     * Returns the corresponding attribute value for the specified field value.
     * Wraps the super method to return an Enum type from the string conversion.
     */
    @Override
    public Object convertDataValueToObjectValue(Object fieldValue, Session session) {
        Object obj = super.convertDataValueToObjectValue(fieldValue, session);

        if (fieldValue == null || obj == null) {
            return obj;
        } else {
            return Enum.valueOf(m_enumClass, (String) obj);
        }
    }

    /**
     * INTERNAL:
     * Convert Enum object to the data value. Internal enums are stored as
     * strings (names) so this method wraps the super method in that if
     * breaks down the enum to a string name before converting it.
     */
    @Override
    public Object convertObjectValueToDataValue(Object attributeValue, Session session) {
        if (attributeValue == null) {
            return super.convertObjectValueToDataValue(null, session);
        } else {
            return super.convertObjectValueToDataValue(((Enum)attributeValue).name(), session);
        }
    }
}
