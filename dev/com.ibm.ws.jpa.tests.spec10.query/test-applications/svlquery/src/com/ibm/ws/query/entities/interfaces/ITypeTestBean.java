/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.interfaces;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public interface ITypeTestBean {
    public enum EmployeeStatus {
        FULL_TIME, PART_TIME, CONTRACT
    };

    public enum SalaryRate {
        JUNIOR, SENIOR, MANAGER, EXECUTIVE
    };

    static boolean debug = true;
    static double DOUBLE_DB_MAX_VALUE = 1;
    static double DOUBLE_DB_MIN_VALUE = 1;
    static float FLOAT_DB_MAX_VALUE = 1;
    static float FLOAT_DB_MIN_VALUE = 1;
    static int noErrors = 0;
    static int noQuery = 0;

    public java.math.BigInteger getAgeofUniverse();

    public java.math.BigDecimal getDeficitUSA();

    public byte getI1();

    public short getI2();

    public int getI4();

    public long getI8();

    public boolean isIboolean();

    public boolean getIboolean();

    public char getIc();

    public double getIdouble();

    public float getIfloat();

    public String getName();

    public Byte getO1();

    public Short getO2();

    public Integer getO4();

    public Long getO8();

    public Boolean getOboolean();

    public Character getOc();

    public Double getOdouble();

    public Float getOfloat();

    public java.sql.Date getSdate();

    public java.sql.Time getStime();

    public Timestamp getStimestamp();

    public Calendar getUcalendar();

    public Date getUdate();

    @Override
    public String toString();

    public int getId();

    @Override
    public boolean equals(Object o);

    public Object clone();

    public byte[] getIbytes();

    public char[] getIchars();

    public Byte[] getObytes();

    public Character[] getOchars();

    public EmployeeStatus getStatus();

    public SalaryRate getPayScale();

    public ISerializableClass getBusPass();

    public void setAgeofUniverse(java.math.BigInteger ageofUniverse);

    public void setBigbytes(byte[] bigbytes);

    public void setBusPass(SerializableClass busPass);

    public void setDeficitUSA(java.math.BigDecimal deficitUSA);

    public void setI1(byte i1);

    public void setI2(short i2);

    public void setI4(int i4);

    public void setI8(long i8);

    public void setIboolean(boolean iboolean);

    public void setIbytes(byte[] ibytes);

    public void setIc(char ic);

    public void setIchars(char[] ichars);

    public void setIdouble(double idouble);

    public void setIfloat(float ifloat);

    public void setId(int id);

    public void setName(String name);

    public void setO1(Byte o1);

    public void setO2(Short o2);

    public void setO4(Integer o4);

    public void setO8(Long o8);

    public void setOboolean(Boolean oboolean);

    public void setObytes(Byte[] obytes);

    public void setOc(Character oc);

    public void setOchars(Character[] ochars);

    public void setOdouble(Double odouble);

    public void setOfloat(Float ofloat);

    public void setPayScale(SalaryRate payScale);

    public void setSdate(java.sql.Date sdate);

    public void setStatus(EmployeeStatus status);

    public void setStime(java.sql.Time stime);

    public void setStimestamp(Timestamp stimestamp);

    public void setUcalendar(Calendar ucalendar);

    public void setUdate(Date udate);
}
