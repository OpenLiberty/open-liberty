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
//     Oracle - initial API and implementation from Oracle TopLink
//     06/03/2013-2.5.1 Guy Pelletier
//       - 402380: 3 jpa21/advanced tests failed on server with
//         "java.lang.NoClassDefFoundError: org/eclipse/persistence/testing/models/jpa21/advanced/enums/Gender"
package org.eclipse.persistence.mappings.converters;

import java.security.AccessController;
import java.security.PrivilegedActionException;

import org.eclipse.persistence.mappings.*;
import org.eclipse.persistence.mappings.foundation.AbstractDirectMapping;
import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.descriptors.ClassNameConversionRequired;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.sessions.AbstractSession;

/**
 * <b>Purpose</b>: Type conversion converters are used to explicitly map a database type to a
 * Java type.
 *
 * @author James Sutherland
 * @since OracleAS TopLink 10<i>g</i> (10.0.3)
 */
public class TypeConversionConverter implements Converter, ClassNameConversionRequired {
    protected DatabaseMapping mapping;

    /** Field type */
    protected Class dataClass;
    protected String dataClassName;

    /** Object type */
    protected Class objectClass;
    protected String objectClassName;

    /**
     * PUBLIC:
     * Default constructor.
     */
    public TypeConversionConverter() {
    }

    /**
     * PUBLIC:
     * Default constructor.
     */
    public TypeConversionConverter(DatabaseMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this converter to actual class-based
     * settings. This method is used when converting a project that has been built
     * with class names to a project with classes.
     * This method is implemented by subclasses as necessary.
     * @param classLoader
     */
    public void convertClassNamesToClasses(ClassLoader classLoader){
        Class dataClass = null;
        Class objectClass = null;
        try{
            if (dataClassName != null){
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    try {
                        dataClass = (Class)AccessController.doPrivileged(new PrivilegedClassForName(dataClassName, true, classLoader));
                    } catch (PrivilegedActionException exception) {
                        throw ValidationException.classNotFoundWhileConvertingClassNames(dataClassName, exception.getException());
                    }
                } else {
                    dataClass = PrivilegedAccessHelper.getClassForName(dataClassName, true, classLoader);
                }
                setDataClass(dataClass);
            }
        } catch (ClassNotFoundException exc){
            throw ValidationException.classNotFoundWhileConvertingClassNames(dataClassName, exc);
        }
        try {
            if (objectClassName != null){
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    try {
                        objectClass = (Class)AccessController.doPrivileged(new PrivilegedClassForName(objectClassName, true, classLoader));
                    } catch (PrivilegedActionException exception) {
                        throw ValidationException.classNotFoundWhileConvertingClassNames(objectClassName, exception.getException());
                    }
                } else {
                    objectClass = PrivilegedAccessHelper.getClassForName(objectClassName, true, classLoader);
                }
                setObjectClass(objectClass);
            }
        } catch (ClassNotFoundException exc){
            throw ValidationException.classNotFoundWhileConvertingClassNames(objectClassName, exc);
        }
    }

    /**
     * INTERNAL:
     * The field value must first be converted to the field type, then the attribute type.
     */
    public Object convertDataValueToObjectValue(Object fieldValue, Session session) {
        Object attributeValue = fieldValue;
        if (attributeValue != null) {
            try {
                attributeValue = ((AbstractSession)session).getDatasourcePlatform().convertObject(attributeValue, getDataClass());
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(mapping, mapping.getDescriptor(), e);
            }

            try {
                attributeValue = ((AbstractSession)session).getDatasourcePlatform().convertObject(attributeValue, getObjectClass());
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(mapping, mapping.getDescriptor(), e);
            }
        }

        return attributeValue;
    }

    /**
     * PUBLIC:
     * Returns the class type of the object value.
     */
    public Class getObjectClass() {
        return objectClass;
    }

    /**
     * INTERNAL:
     * Return the name of the object type for the MW usage.
     */
    public String getObjectClassName() {
        if ((objectClassName == null) && (objectClass != null)) {
            objectClassName = objectClass.getName();
        }
        return objectClassName;
    }

    /**
     * PUBLIC:
     * Returns the class type of the data value.
     */
    public Class getDataClass() {
        return dataClass;
    }

    /**
     * INTERNAL:
     * Return the name of the data type for the MW usage.
     */
    public String getDataClassName() {
        if ((dataClassName == null) && (dataClass != null)) {
            dataClassName = dataClass.getName();
        }
        return dataClassName;
    }

    /**
     * PUBLIC:
     * Set the class type of the data value.
     */
    public void setDataClass(Class dataClass) {
        this.dataClass = dataClass;
    }

    /**
     * INTERNAL:
     * Set the name of the data type for the MW usage.
     */
    public void setDataClassName(String dataClassName) {
        this.dataClassName = dataClassName;
    }

    /**
     * PUBLIC:
     * Set the class type of the object value.
     */
    public void setObjectClass(Class objectClass) {
        this.objectClass = objectClass;
    }

    /**
     * INTERNAL:
     * Set the name of the object type for the MW usage.
     */
    public void setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
    }

    /**
     *  INTERNAL:
     *  Convert to the field class.
     */
    public Object convertObjectValueToDataValue(Object attributeValue, Session session) {
        try {
            if (session.isConnected()) {
                //Should handle conversions where DB connection is needed like String -> java.sql.Clob
                return session.getDatasourcePlatform().convertObject(attributeValue, getDataClass(), (AbstractSession)session);
            } else {
                return session.getDatasourcePlatform().convertObject(attributeValue, getDataClass());
            }
        } catch (ConversionException e) {
            throw ConversionException.couldNotBeConverted(mapping, mapping.getDescriptor(), e);
        }
    }

    /**
     * INTERNAL:
     * Set the mapping.
     */
    public void initialize(DatabaseMapping mapping, Session session) {
        this.mapping = mapping;
        // CR#... Mapping must also have the field classification.
        if (getMapping().isDirectToFieldMapping()) {
            AbstractDirectMapping directMapping = (AbstractDirectMapping)getMapping();

            // Allow user to specify field type to override computed value. (i.e. blob, nchar)
            if (directMapping.getFieldClassification() == null) {
                directMapping.setFieldClassification(getDataClass());
            }

            // Set the object class from the attribute, if null.
            if (getObjectClass() == null) {
                setObjectClass(directMapping.getAttributeClassification());
            }
        } else if (getMapping().isDirectCollectionMapping()) {
            ((DirectCollectionMapping) getMapping()).setAttributeClassification(getObjectClass());
        }
    }

    /**
     * INTERNAL:
     * Return the mapping.
     */
    protected DatabaseMapping getMapping() {
        return mapping;
    }

    /**
     * INTERNAL:
     * If the converter converts the value to a non-atomic value, i.e.
     * a value that can have its' parts changed without being replaced,
     * then it must return false, serialization can be non-atomic.
     */
    public boolean isMutable() {
        return false;
    }
}
