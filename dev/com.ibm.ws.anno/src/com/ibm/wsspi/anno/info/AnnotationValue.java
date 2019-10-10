/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.anno.info;

import java.util.List;

/**
 * <p>Representation of an annotation value.</p>
 */
public interface AnnotationValue {
    /**
     * <p>Answer the raw, untyped, value of this annotation value.</p>
     * 
     * <p>The raw value of the annotation is set when the annotation value
     * is constructed, and includes at least a raw value, and may optionally
     * include an the name of the enumeration type of the value.</p>
     * 
     * <p>When the type of the value is known, a casting getter may be used
     * to retrieve the typed value. See, for example, {@link #getLongValue()},
     * amount several typing getters. If the casting getter does not match
     * the set type, a class cast exception will occur.</p>
     * 
     * @return The raw, untyped, value of this annotation value.
     */
    public Object getObjectValue();

    //

    /**
     * <p>Answer the value of this annotation value as an annotation.</p>
     * 
     * @return The value of this annotation value, as an annotation itself.
     */
    public AnnotationInfo getAnnotationValue();

    // TFB: Changed type from List<AnnotationValue> to List<? extends AnnotationValue>.
    /**
     * <p>Answer the value of this annotation value as an array of annotation values.</p>
     * 
     * @return The elements of the value of this annotation value, ordered, as a list.
     */
    public List<? extends AnnotationValue> getArrayValue();

    /**
     * <p>Answer the value of this annotation value as a boolean object.</p>
     * 
     * @return The value of this annotation value as a boolean object.
     */
    public Boolean getBoolean();

    /**
     * <p>Answer the value of this annotation value as a simple boolean value.</p>
     * 
     * @return The value of this annotation value as a simple boolean value.
     */
    public boolean getBooleanValue();

    /**
     * <p>Answer the value of this annotation value as a byte object.</p>
     * 
     * @return The value of this annotation value as a byte object.
     */
    public Byte getByte();

    /**
     * <p>Answer the value of this annotation value as a simple byte value.</p>
     * 
     * @return The value of this annotation value as a simple byte value.
     */
    public byte getByteValue();

    /**
     * <p>Answer the value of this annotation value as a character object.</p>
     * 
     * @return The value of this annotation value as a character object.
     */
    public Character getCharacter();

    /**
     * <p>Answer the value of this annotation value as a simple byte value.</p>
     * 
     * @return The value of this annotation value as a simple byte value.
     */
    public char getCharValue();

    /**
     * <p>Answer the class name for an annotation value which is a class reference.</p>
     * 
     * @return The class name for a value which is a class reference.
     */
    public String getClassNameValue();

    /**
     * <p>Answer the value of this annotation value as a double object.</p>
     * 
     * @return The value of this annotation value as a double object.
     */
    public Double getDouble();

    /**
     * <p>Answer the value of this annotation value as a simple double (double precision floating point) value.</p>
     * 
     * @return The value of this annotation value as a simple double (double precision floating point) value.
     */
    public double getDoubleValue();

    /**
     * <p>Answer the name of the class of the stored enumerated value.</p>
     * 
     * @return The name of the class of the stored enumerated value.
     * 
     * @see #getEnumValue()
     * @see #getEnumType()
     */
    public String getEnumClassName();

    /**
     * <p>Answer the enumerated value as the value name.</p>
     * 
     * @return The enumerated value as a value name.
     * 
     * @see #getEnumType()
     */
    public String getEnumValue();

    /**
     * <p>Answer the value of this annotation value as a float object.</p>
     * 
     * @return The value of this annotation value as a float object.
     */
    public Float getFloat();

    /**
     * <p>Answer the value of this annotation value as a simple float (floating point) value.</p>
     * 
     * @return The value of this annotation value as a simple float (floating point) value.
     */
    public float getFloatValue();

    /**
     * <p>Answer the value of this annotation value as an integer object.</p>
     * 
     * @return The value of this annotation value as an integer object.
     */
    public Integer getInteger();

    /**
     * <p>Answer the value of this annotation value as a simple int (integer) value.</p>
     * 
     * @return The value of this annotation value as a simple int (integer) value.
     */
    public int getIntValue();

    /**
     * <p>Answer the value of this annotation value as a long object.</p>
     * 
     * @return The value of this annotation value as a long object.
     */
    public Long getLong();

    /**
     * <p>Answer the value of this annotation value as a simple long value.</p>
     * 
     * @return The value of this annotation value as a simple long value.
     */
    public long getLongValue();

    /**
     * <p>Answer the value of this annotation value as a simple string value.</p>
     * 
     * @return The value of this annotation value as a simple string value.
     */
    public String getStringValue();
}