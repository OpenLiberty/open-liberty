/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.info.AnnotationValue;

/**
 * <p>Implementation for annotation values.</p>
 * 
 * <p>Several cases are handled:</p>
 * 
 * <ul><li>Primitive cases</li>
 * <ul><li>Primitive values (boolean, char, byte, short, int, float, long, and double)</li>
 * <li>Primitive class values (stored as String)</li>
 * <li>Other (unspecified) cases of Object as a primitive value</li>
 * </ul>
 * <li>Array case</li>
 * <li>Annotation case</li>
 * </ul>
 */
public class AnnotationValueImpl implements AnnotationValue {

    public static final String CLASS_NAME = AnnotationValueImpl.class.getName();

    //

    /**
     * <p>Create an annotation value for an array of values.</p>
     */
    public AnnotationValueImpl() {
        this.enumClassName = null;
        this.value = new LinkedList<AnnotationValueImpl>();
    }

    /**
     * <p>Create an annotation value with primitive or annotation typed
     * value. Arrays and enumeration values are handled with different
     * constructors.</p>
     * 
     * @param value The value to store in the new annotation value.
     */
    public AnnotationValueImpl(Object value) {
        super();

        this.enumClassName = null;
        this.value = value;
    }

    /**
     * <p>Create an annotation for an enumerated value. Store the
     * enumeration class name and the enumeration literal value.</p>
     * 
     * @param enumClassName The name of the class of the enumerated value.
     * @param enumLiteralValue The name of the enumerated value.
     */
    public AnnotationValueImpl(String enumClassName, String enumLiteralValue) {
        super();

        this.enumClassName = enumClassName;
        this.value = enumLiteralValue;
    }

    // Special entry point for storing the elements of an array
    // typed annotation value.
    //
    // Used by InfoVisitor_Annotation.storeAnnotationValue, for the
    // case of visitor calls to process array element values.

    @SuppressWarnings("unchecked")
    protected void addArrayValue(AnnotationValueImpl annotationValue) {
        ((List<AnnotationValueImpl>) getArrayValue()).add(annotationValue);

        this.stringValue = null;
    }

    // Extra data (enumeration class name) for enumeration value storage.

    protected String enumClassName;

    public String getEnumType() {
        return this.enumClassName;
    }

    @Override
    public String getEnumClassName() {
        return this.enumClassName;
    }

    // Base data, used in all cases.  Note that many primitive values
    // (e.g., boolean), are stored using object types, (e.g., Boolean).

    protected Object value;

    @Override
    @Trivial
    public Object getObjectValue() {
        return this.value;
    }

    // Special case typed access.  For an array typed child value and
    // for an array typed child value.

    @Override
    public AnnotationInfoImpl getAnnotationValue() {
        return ((AnnotationInfoImpl) this.value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends AnnotationValue> getArrayValue() {
        return ((List<? extends AnnotationValue>) this.value);
    }

    // Primitive typed access ...

    @Override
    public Boolean getBoolean() {
        return ((Boolean) this.value);
    }

    @Override
    public boolean getBooleanValue() {
        return ((Boolean) this.value).booleanValue();
    }

    @Override
    public Byte getByte() {
        return ((Byte) this.value);
    }

    @Override
    public byte getByteValue() {
        return ((Byte) this.value).byteValue();
    }

    @Override
    public Character getCharacter() {
        return ((Character) this.value);
    }

    @Override
    public char getCharValue() {
        return ((Character) this.value).charValue();
    }

    @Override
    public String getClassNameValue() {
        return ((String) this.value);
    }

    @Override
    public Double getDouble() {
        return ((Double) this.value);
    }

    @Override
    public double getDoubleValue() {
        return ((Number) this.value).doubleValue();
    }

    @Override
    public String getEnumValue() {
        return ((String) this.value);
    }

    @Override
    public Float getFloat() {
        return ((Float) this.value);
    }

    @Override
    public float getFloatValue() {
        return ((Number) this.value).floatValue();
    }

    @Override
    public Integer getInteger() {
        return ((Integer) this.value);
    }

    @Override
    public int getIntValue() {
        return ((Number) this.value).intValue();
    }

    @Override
    public Long getLong() {
        return ((Long) this.value);
    }

    @Override
    public long getLongValue() {
        return ((Number) this.value).longValue();
    }

    @Override
    public String getStringValue() {
        return this.value.toString();
    }

    // API to obtain a string representation of any child value.
    //
    // The value will be the same as the string value, per
    // 'getStringValue', for a child value which is actually
    // a string.  Otherwise, the string value is the string
    // representation of the child value.
    //
    // The result value is cached.
    //
    // A string value is available for all child types, including
    // array type and annotation type child values.

    protected String stringValue;

    @Override
    @Trivial
    public String toString() {
        if (stringValue == null) {
            stringValue = String.valueOf(getObjectValue());
        }

        return stringValue;
    }
}
