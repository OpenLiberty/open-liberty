/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.model;

/**
 * The model for an interface method.
 */
public class ModelMethod {
    /**
     * The name of this method, or null if this is an ignored XMI node.
     */
    public final String name;

    /**
     * The field that backs this method, or null if none.
     */
    public final ModelField field;

    /**
     * The getX or isX method that corresponds to this isSetX method, or null if
     * this is not an isSetX method.
     */
    public ModelMethod isSetAccessorMethod;

    /**
     * The intermediate field that should be used to access {@link #field}.
     */
    public ModelField intermediateField;

    /**
     * A type-specific string indicating the default method return value, or
     * null if a type-specific default value should be used.
     */
    private final String defaultValue;

    /**
     * True if the method should return a constant "XMI".
     */
    public boolean xmiVersion;

    /**
     * If the value for this method should be obtained indirectly by
     * referencing another document, then this represents the
     * CrossComponentReferenceType field for the XMI reference element. If this
     * field is set, then {@link #xmiRefReferenceTypeName} must also be set.
     */
    public ModelField xmiRefField;

    /**
     * If the value for this method should be obtained indirectly by
     * referencing another document, then this field contains the fully
     * qualified type of the object in the referenced document. If this
     * field is set, then {@link #xmiRefValueGetter} must also be set.
     */
    public String xmiRefReferentTypeName;

    /**
     * The name of the getter method on {@link #xmiRefReferentTypeName} that
     * produces values for {@link #method}, or null if this is an ignored node.
     */
    public String xmiRefValueGetter;

    public ModelMethod(String name, ModelField field, String defaultValue) {
        this.name = name;
        this.field = field;
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name + ", " + getType() + ']';
    }

    public boolean isList() {
        if (isSetAccessorMethod != null) {
            return false;
        }
        return field.listAddMethodName != null;
    }

    public ModelType getType() {
        if (isSetAccessorMethod != null) {
            return ModelBasicType.Boolean;
        }
        return field.type;
    }

    public String getJavaTypeName() {
        if (isSetAccessorMethod != null) {
            return getType().getJavaTypeName();
        }
        if (field.listAddMethodName != null) {
            return "java.util.List<" + field.type.getJavaTypeName() + '>';
        }
        return field.type.getJavaTypeName();
    }

    public String getDefaultValue() {
        return getType().getDefaultValue(defaultValue);
    }
}
