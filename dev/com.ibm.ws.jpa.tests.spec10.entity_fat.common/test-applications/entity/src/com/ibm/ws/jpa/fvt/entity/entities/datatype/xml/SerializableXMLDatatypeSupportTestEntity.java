/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entity.entities.datatype.xml;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.ibm.ws.jpa.fvt.entity.entities.IDatatypeSupportTestEntity;
import com.ibm.ws.jpa.fvt.entity.support.Constants;
import com.ibm.ws.jpa.fvt.entity.support.SerializableClass;

@SuppressWarnings("serial")
public class SerializableXMLDatatypeSupportTestEntity implements IDatatypeSupportTestEntity, java.io.Serializable {
    private int id;

    // Primitive Java Types
    private byte byteAttrDefault;
    private int intAttrDefault;
    private short shortAttrDefault;
    private long longAttrDefault;
    private boolean booleanAttrDefault;
    private char charAttrDefault;
    private float floatAttrDefault;
    private double doubleAttrDefault;

    // Wrapper Classes of Primitive Java Types
    private Byte byteWrapperAttrDefault;
    private Integer integerWrapperAttrDefault;
    private Short shortWrapperAttrDefault;
    private Long longWrapperAttrDefault;
    private Boolean booleanWrapperAttrDefault;
    private Character characterWrapperAttrDefault;
    private Float floatWrapperAttrDefault;
    private Double doubleWrapperAttrDefault;

    // Large Numeric Types
    private BigInteger bigIntegerAttrDefault;
    private BigDecimal bigDecimalAttrDefault;

    // Byte and Character Array Types
    private byte[] byteArrayAttrDefault;
    private Byte[] byteWrapperArrayAttrDefault;
    private char[] charArrayAttrDefault;
    private Character[] charWrapperArrayAttrDefault;

    // Strings
    private String stringAttrDefault;

    // Java Temporal Types
    private java.util.Date utilDateAttrDefault;
    private java.util.Calendar utilCalendarAttrDefault;

    // JDBC Temporal Types
    private java.sql.Date sqlDateAttrDefault;
    private java.sql.Time sqlTimeAttrDefault;
    private java.sql.Timestamp sqlTimestampAttrDefault;

    // Enumerated Types
    private Constants.TestEnumeration enumeration;

    // Serializable Objects
    private SerializableClass serializableClass;

    private String transientString;

    public SerializableXMLDatatypeSupportTestEntity() {
        transientString = "";
    }

    @Override
    public BigDecimal getBigDecimalAttrDefault() {
        return bigDecimalAttrDefault;
    }

    @Override
    public void setBigDecimalAttrDefault(BigDecimal bigDecimalAttrDefault) {
        this.bigDecimalAttrDefault = bigDecimalAttrDefault;
    }

    @Override
    public BigInteger getBigIntegerAttrDefault() {
        return bigIntegerAttrDefault;
    }

    @Override
    public void setBigIntegerAttrDefault(BigInteger bigIntegerAttrDefault) {
        this.bigIntegerAttrDefault = bigIntegerAttrDefault;
    }

    @Override
    public boolean isBooleanAttrDefault() {
        return booleanAttrDefault;
    }

    @Override
    public void setBooleanAttrDefault(boolean booleanAttrDefault) {
        this.booleanAttrDefault = booleanAttrDefault;
    }

    @Override
    public Boolean getBooleanWrapperAttrDefault() {
        return booleanWrapperAttrDefault;
    }

    @Override
    public void setBooleanWrapperAttrDefault(Boolean booleanWrapperAttrDefault) {
        this.booleanWrapperAttrDefault = booleanWrapperAttrDefault;
    }

    @Override
    public byte[] getByteArrayAttrDefault() {
        return byteArrayAttrDefault;
    }

    @Override
    public void setByteArrayAttrDefault(byte[] byteArrayAttrDefault) {
        this.byteArrayAttrDefault = byteArrayAttrDefault;
    }

    @Override
    public byte getByteAttrDefault() {
        return byteAttrDefault;
    }

    @Override
    public void setByteAttrDefault(byte byteAttrDefault) {
        this.byteAttrDefault = byteAttrDefault;
    }

    @Override
    public Byte[] getByteWrapperArrayAttrDefault() {
        return byteWrapperArrayAttrDefault;
    }

    @Override
    public void setByteWrapperArrayAttrDefault(Byte[] byteWrapperArrayAttrDefault) {
        this.byteWrapperArrayAttrDefault = byteWrapperArrayAttrDefault;
    }

    @Override
    public Byte getByteWrapperAttrDefault() {
        return byteWrapperAttrDefault;
    }

    @Override
    public void setByteWrapperAttrDefault(Byte byteWrapperAttrDefault) {
        this.byteWrapperAttrDefault = byteWrapperAttrDefault;
    }

    @Override
    public Character getCharacterWrapperAttrDefault() {
        return characterWrapperAttrDefault;
    }

    @Override
    public void setCharacterWrapperAttrDefault(Character characterWrapperAttrDefault) {
        this.characterWrapperAttrDefault = characterWrapperAttrDefault;
    }

    @Override
    public char[] getCharArrayAttrDefault() {
        return charArrayAttrDefault;
    }

    @Override
    public void setCharArrayAttrDefault(char[] charArrayAttrDefault) {
        this.charArrayAttrDefault = charArrayAttrDefault;
    }

    @Override
    public char getCharAttrDefault() {
        return charAttrDefault;
    }

    @Override
    public void setCharAttrDefault(char charAttrDefault) {
        this.charAttrDefault = charAttrDefault;
    }

    @Override
    public Character[] getCharWrapperArrayAttrDefault() {
        return charWrapperArrayAttrDefault;
    }

    @Override
    public void setCharWrapperArrayAttrDefault(Character[] charWrapperArrayAttrDefault) {
        this.charWrapperArrayAttrDefault = charWrapperArrayAttrDefault;
    }

    @Override
    public double getDoubleAttrDefault() {
        return doubleAttrDefault;
    }

    @Override
    public void setDoubleAttrDefault(double doubleAttrDefault) {
        this.doubleAttrDefault = doubleAttrDefault;
    }

    @Override
    public Double getDoubleWrapperAttrDefault() {
        return doubleWrapperAttrDefault;
    }

    @Override
    public void setDoubleWrapperAttrDefault(Double doubleWrapperAttrDefault) {
        this.doubleWrapperAttrDefault = doubleWrapperAttrDefault;
    }

    @Override
    public Constants.TestEnumeration getEnumeration() {
        return enumeration;
    }

    @Override
    public void setEnumeration(Constants.TestEnumeration enumeration) {
        this.enumeration = enumeration;
    }

    @Override
    public float getFloatAttrDefault() {
        return floatAttrDefault;
    }

    @Override
    public void setFloatAttrDefault(float floatAttrDefault) {
        this.floatAttrDefault = floatAttrDefault;
    }

    @Override
    public Float getFloatWrapperAttrDefault() {
        return floatWrapperAttrDefault;
    }

    @Override
    public void setFloatWrapperAttrDefault(Float floatWrapperAttrDefault) {
        this.floatWrapperAttrDefault = floatWrapperAttrDefault;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getIntAttrDefault() {
        return intAttrDefault;
    }

    @Override
    public void setIntAttrDefault(int intAttrDefault) {
        this.intAttrDefault = intAttrDefault;
    }

    @Override
    public Integer getIntegerWrapperAttrDefault() {
        return integerWrapperAttrDefault;
    }

    @Override
    public void setIntegerWrapperAttrDefault(Integer integerWrapperAttrDefault) {
        this.integerWrapperAttrDefault = integerWrapperAttrDefault;
    }

    @Override
    public long getLongAttrDefault() {
        return longAttrDefault;
    }

    @Override
    public void setLongAttrDefault(long longAttrDefault) {
        this.longAttrDefault = longAttrDefault;
    }

    @Override
    public Long getLongWrapperAttrDefault() {
        return longWrapperAttrDefault;
    }

    @Override
    public void setLongWrapperAttrDefault(Long longWrapperAttrDefault) {
        this.longWrapperAttrDefault = longWrapperAttrDefault;
    }

    @Override
    public SerializableClass getSerializableClass() {
        return serializableClass;
    }

    @Override
    public void setSerializableClass(SerializableClass serializableClass) {
        this.serializableClass = serializableClass;
    }

    @Override
    public short getShortAttrDefault() {
        return shortAttrDefault;
    }

    @Override
    public void setShortAttrDefault(short shortAttrDefault) {
        this.shortAttrDefault = shortAttrDefault;
    }

    @Override
    public Short getShortWrapperAttrDefault() {
        return shortWrapperAttrDefault;
    }

    @Override
    public void setShortWrapperAttrDefault(Short shortWrapperAttrDefault) {
        this.shortWrapperAttrDefault = shortWrapperAttrDefault;
    }

    @Override
    public java.sql.Date getSqlDateAttrDefault() {
        return sqlDateAttrDefault;
    }

    @Override
    public void setSqlDateAttrDefault(java.sql.Date sqlDateAttrDefault) {
        this.sqlDateAttrDefault = sqlDateAttrDefault;
    }

    @Override
    public java.sql.Time getSqlTimeAttrDefault() {
        return sqlTimeAttrDefault;
    }

    @Override
    public void setSqlTimeAttrDefault(java.sql.Time sqlTimeAttrDefault) {
        this.sqlTimeAttrDefault = sqlTimeAttrDefault;
    }

    @Override
    public java.sql.Timestamp getSqlTimestampAttrDefault() {
        return sqlTimestampAttrDefault;
    }

    @Override
    public void setSqlTimestampAttrDefault(java.sql.Timestamp sqlTimestampAttrDefault) {
        this.sqlTimestampAttrDefault = sqlTimestampAttrDefault;
    }

    @Override
    public String getStringAttrDefault() {
        return stringAttrDefault;
    }

    @Override
    public void setStringAttrDefault(String stringAttrDefault) {
        this.stringAttrDefault = stringAttrDefault;
    }

    @Override
    public java.util.Calendar getUtilCalendarAttrDefault() {
        return utilCalendarAttrDefault;
    }

    @Override
    public void setUtilCalendarAttrDefault(java.util.Calendar utilCalendarAttrDefault) {
        this.utilCalendarAttrDefault = utilCalendarAttrDefault;
    }

    @Override
    public java.util.Date getUtilDateAttrDefault() {
        return utilDateAttrDefault;
    }

    @Override
    public void setUtilDateAttrDefault(java.util.Date utilDateAttrDefault) {
        this.utilDateAttrDefault = utilDateAttrDefault;
    }

    @Override
    public String getTransientString() {
        return transientString;
    }

    @Override
    public void setTransientString(String transientString) {
        this.transientString = transientString;
    }

    @Override
    public String toString() {
        return "SerializableXMLDatatypeSupportTestEntity [id=" + id + "]";
    }
}
