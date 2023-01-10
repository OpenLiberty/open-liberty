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

package com.ibm.ws.jpa.fvt.entity.entities;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.ibm.ws.jpa.fvt.entity.support.Constants;
import com.ibm.ws.jpa.fvt.entity.support.SerializableClass;

public interface IDatatypeSupportTestEntity {
    public int getId();

    public void setId(int id);

    public BigDecimal getBigDecimalAttrDefault();

    public void setBigDecimalAttrDefault(BigDecimal bigDecimalAttrDefault);

    public BigInteger getBigIntegerAttrDefault();

    public void setBigIntegerAttrDefault(BigInteger bigIntegerAttrDefault);

    public boolean isBooleanAttrDefault();

    public void setBooleanAttrDefault(boolean booleanAttrDefault);

    public Boolean getBooleanWrapperAttrDefault();

    public void setBooleanWrapperAttrDefault(Boolean booleanWrapperAttrDefault);

    public byte[] getByteArrayAttrDefault();

    public void setByteArrayAttrDefault(byte[] byteArrayAttrDefault);

    public byte getByteAttrDefault();

    public void setByteAttrDefault(byte byteAttrDefault);

    public Byte[] getByteWrapperArrayAttrDefault();

    public void setByteWrapperArrayAttrDefault(Byte[] byteWrapperArrayAttrDefault);

    public Byte getByteWrapperAttrDefault();

    public void setByteWrapperAttrDefault(Byte byteWrapperAttrDefault);

    public Character getCharacterWrapperAttrDefault();

    public void setCharacterWrapperAttrDefault(Character characterWrapperAttrDefault);

    public char[] getCharArrayAttrDefault();

    public void setCharArrayAttrDefault(char[] charArrayAttrDefault);

    public char getCharAttrDefault();

    public void setCharAttrDefault(char charAttrDefault);

    public Character[] getCharWrapperArrayAttrDefault();

    public void setCharWrapperArrayAttrDefault(Character[] charWrapperArrayAttrDefault);

    public double getDoubleAttrDefault();

    public void setDoubleAttrDefault(double doubleAttrDefault);

    public Double getDoubleWrapperAttrDefault();

    public void setDoubleWrapperAttrDefault(Double doubleWrapperAttrDefault);

    public Constants.TestEnumeration getEnumeration();

    public void setEnumeration(Constants.TestEnumeration enumeration);

    public float getFloatAttrDefault();

    public void setFloatAttrDefault(float floatAttrDefault);

    public Float getFloatWrapperAttrDefault();

    public void setFloatWrapperAttrDefault(Float floatWrapperAttrDefault);

    public int getIntAttrDefault();

    public void setIntAttrDefault(int intAttrDefault);

    public Integer getIntegerWrapperAttrDefault();

    public void setIntegerWrapperAttrDefault(Integer integerWrapperAttrDefault);

    public long getLongAttrDefault();

    public void setLongAttrDefault(long longAttrDefault);

    public Long getLongWrapperAttrDefault();

    public void setLongWrapperAttrDefault(Long longWrapperAttrDefault);

    public SerializableClass getSerializableClass();

    public void setSerializableClass(SerializableClass serializableClass);

    public short getShortAttrDefault();

    public void setShortAttrDefault(short shortAttrDefault);

    public Short getShortWrapperAttrDefault();

    public void setShortWrapperAttrDefault(Short shortWrapperAttrDefault);

    public java.sql.Date getSqlDateAttrDefault();

    public void setSqlDateAttrDefault(java.sql.Date sqlDateAttrDefault);

    public java.sql.Time getSqlTimeAttrDefault();

    public void setSqlTimeAttrDefault(java.sql.Time sqlTimeAttrDefault);

    public java.sql.Timestamp getSqlTimestampAttrDefault();

    public void setSqlTimestampAttrDefault(java.sql.Timestamp sqlTimestampAttrDefault);

    public String getStringAttrDefault();

    public void setStringAttrDefault(String stringAttrDefault);

    public java.util.Calendar getUtilCalendarAttrDefault();

    public void setUtilCalendarAttrDefault(java.util.Calendar utilCalendarAttrDefault);

    public java.util.Date getUtilDateAttrDefault();

    public void setUtilDateAttrDefault(java.util.Date utilDateAttrDefault);

    public String getTransientString();

    public void setTransientString(String transientString);
}
