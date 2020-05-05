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

package com.ibm.ws.query.entities.xml;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import com.ibm.ws.query.entities.interfaces.ISerializableClass;
import com.ibm.ws.query.entities.interfaces.ITypeTestBean;
import com.ibm.ws.query.entities.interfaces.SerializableClass;

public class TypeTestBean implements ITypeTestBean, java.io.Serializable, Cloneable {
    private static final long serialVersionUID = 635324451613535173L;
    public int id;
    public int i4;
    public Integer o4;
    public byte i1;
    public Byte o1;
    public short i2;
    public Short o2;
    public long i8;
    public Long o8;
    public String name;
    public char ic;
    public Character oc;
    public char[] ichars;
    public Character[] ochars;
    public byte[] ibytes;
    public Byte[] obytes;
    public byte[] bigbytes;
    public double idouble;
    public Double odouble;
    public float ifloat;
    public Float ofloat;
    public java.math.BigDecimal deficitUSA;
    public java.math.BigInteger ageofUniverse;
    public boolean iboolean;
    public Boolean oboolean;
    public java.sql.Date sdate;
    public java.sql.Time stime;
    public Timestamp stimestamp;
    public Date udate;
    public Calendar ucalendar;
    public EmployeeStatus status;
    public SalaryRate payScale;
    public SerializableClass busPass;

    public TypeTestBean() {
    }

    public TypeTestBean(int id) {
        this.i4 = id;
    }

    @Override
    public java.math.BigInteger getAgeofUniverse() {
        return ageofUniverse;
    }

    @Override
    public java.math.BigDecimal getDeficitUSA() {
        return deficitUSA;
    }

    @Override
    public byte getI1() {
        return i1;
    }

    @Override
    public short getI2() {
        return i2;
    }

    @Override
    public int getI4() {
        return i4;
    }

    @Override
    public long getI8() {
        return i8;
    }

    @Override
    public boolean isIboolean() {
        return iboolean;
    }

    @Override
    public boolean getIboolean() {
        return iboolean;
    }

    @Override
    public char getIc() {
        return ic;
    }

    @Override
    public double getIdouble() {
        return idouble;
    }

    @Override
    public float getIfloat() {
        return ifloat;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Byte getO1() {
        return o1;
    }

    @Override
    public Short getO2() {
        return o2;
    }

    @Override
    public Integer getO4() {
        return o4;
    }

    @Override
    public Long getO8() {
        return o8;
    }

    @Override
    public Boolean getOboolean() {
        return oboolean;
    }

    @Override
    public Character getOc() {
        return oc;
    }

    @Override
    public Double getOdouble() {
        return odouble;
    }

    @Override
    public Float getOfloat() {
        return ofloat;
    }

    @Override
    public java.sql.Date getSdate() {
        return sdate;
    }

    @Override
    public java.sql.Time getStime() {
        return stime;
    }

    @Override
    public Timestamp getStimestamp() {
        return stimestamp;
    }

    @Override
    public Calendar getUcalendar() {
        return ucalendar;
    }

    @Override
    public Date getUdate() {
        return udate;
    }

    @Override
    public String toString() {
        if (name != null)
            return "TypeTestBean(" + Integer.toString(i4) + "," + name + ")";
        else
            return "TypeTestBean(" + Integer.toString(i4) + ")";
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TypeTestBean && ((TypeTestBean) o).i4 == i4)
            return true;
        return false;
    }

    @Override
    public Object clone() {
        TypeTestBean rc = new TypeTestBean();
        rc.ageofUniverse = ageofUniverse;
        rc.deficitUSA = deficitUSA;
        rc.i1 = i1;
        rc.i2 = i2;
        rc.i4 = i4;
        rc.i8 = i8;
        rc.iboolean = iboolean;
        rc.ic = ic;
        rc.idouble = idouble;
        rc.ifloat = ifloat;
        rc.name = name;
        rc.ichars = ichars;
        rc.ochars = ochars;
        rc.ibytes = ibytes;
        rc.obytes = obytes;
        rc.o1 = o1;
        rc.o2 = o2;
        rc.o4 = o4;
        rc.o8 = o8;
        rc.oboolean = oboolean;
        rc.oc = oc;
        rc.odouble = odouble;
        rc.ofloat = ofloat;
        rc.sdate = (java.sql.Date) sdate.clone();
        rc.stime = (java.sql.Time) stime.clone();
        rc.stimestamp = (java.sql.Timestamp) stimestamp.clone();
        rc.ucalendar = (java.util.Calendar) ucalendar.clone();
        rc.udate = (java.util.Date) udate.clone();
        return rc;
    }

    @Override
    public byte[] getIbytes() {
        return ibytes;
    }

    @Override
    public char[] getIchars() {
        return ichars;
    }

    @Override
    public Byte[] getObytes() {
        return obytes;
    }

    @Override
    public Character[] getOchars() {
        return ochars;
    }

    @Override
    public EmployeeStatus getStatus() {
        return status;
    }

    @Override
    public SalaryRate getPayScale() {
        return payScale;
    }

//	public BusPass getBusPass() {
//		return busPass;
//	}
//
//
//	public void setBusPass(BusPass busPass) {
//		this.busPass = busPass;
//	}

    @Override
    public SerializableClass getBusPass() {
        return busPass;
    }

    public void setBusPass(ISerializableClass busPass) {
        this.busPass = (SerializableClass) busPass;

    }

//	public static void setDebug(boolean debug) {
//		ITypeTestBean.debug = debug;
//	}
//	public static void setDOUBLE_DB_MAX_VALUE(double double_db_max_value) {
//		DOUBLE_DB_MAX_VALUE = double_db_max_value;
//	}
//	public static void setDOUBLE_DB_MIN_VALUE(double double_db_min_value) {
//		DOUBLE_DB_MIN_VALUE = double_db_min_value;
//	}
//	public static void setFLOAT_DB_MAX_VALUE(float float_db_max_value) {
//		FLOAT_DB_MAX_VALUE = float_db_max_value;
//	}
//	public static void setFLOAT_DB_MIN_VALUE(float float_db_min_value) {
//		FLOAT_DB_MIN_VALUE = float_db_min_value;
//	}
//	public static void setNoErrors(int noErrors) {
//		ITypeTestBean.noErrors = noErrors;
//	}
//	public static void setNoQuery(int noQuery) {
//		ITypeTestBean.noQuery = noQuery;
//	}
    @Override
    public void setAgeofUniverse(java.math.BigInteger ageofUniverse) {
        this.ageofUniverse = ageofUniverse;
    }

    @Override
    public void setBigbytes(byte[] bigbytes) {
        this.bigbytes = bigbytes;
    }

    @Override
    public void setBusPass(SerializableClass busPass) {
        this.busPass = busPass;
    }

    @Override
    public void setDeficitUSA(java.math.BigDecimal deficitUSA) {
        this.deficitUSA = deficitUSA;
    }

    @Override
    public void setI1(byte i1) {
        this.i1 = i1;
    }

    @Override
    public void setI2(short i2) {
        this.i2 = i2;
    }

    @Override
    public void setI4(int i4) {
        this.i4 = i4;
    }

    @Override
    public void setI8(long i8) {
        this.i8 = i8;
    }

    @Override
    public void setIboolean(boolean iboolean) {
        this.iboolean = iboolean;
    }

    @Override
    public void setIbytes(byte[] ibytes) {
        this.ibytes = ibytes;
    }

    @Override
    public void setIc(char ic) {
        this.ic = ic;
    }

    @Override
    public void setIchars(char[] ichars) {
        this.ichars = ichars;
    }

    @Override
    public void setIdouble(double idouble) {
        this.idouble = idouble;
    }

    @Override
    public void setIfloat(float ifloat) {
        this.ifloat = ifloat;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setO1(Byte o1) {
        this.o1 = o1;
    }

    @Override
    public void setO2(Short o2) {
        this.o2 = o2;
    }

    @Override
    public void setO4(Integer o4) {
        this.o4 = o4;
    }

    @Override
    public void setO8(Long o8) {
        this.o8 = o8;
    }

    @Override
    public void setOboolean(Boolean oboolean) {
        this.oboolean = oboolean;
    }

    @Override
    public void setObytes(Byte[] obytes) {
        this.obytes = obytes;
    }

    @Override
    public void setOc(Character oc) {
        this.oc = oc;
    }

    @Override
    public void setOchars(Character[] ochars) {
        this.ochars = ochars;
    }

    @Override
    public void setOdouble(Double odouble) {
        this.odouble = odouble;
    }

    @Override
    public void setOfloat(Float ofloat) {
        this.ofloat = ofloat;
    }

    @Override
    public void setPayScale(SalaryRate payScale) {
        this.payScale = payScale;
    }

    @Override
    public void setSdate(java.sql.Date sdate) {
        this.sdate = sdate;
    }

    @Override
    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    @Override
    public void setStime(java.sql.Time stime) {
        this.stime = stime;
    }

    @Override
    public void setStimestamp(Timestamp stimestamp) {
        this.stimestamp = stimestamp;
    }

    @Override
    public void setUcalendar(Calendar ucalendar) {
        this.ucalendar = ucalendar;
    }

    @Override
    public void setUdate(Date udate) {
        this.udate = udate;
    }

    @Override
    public void setId(int key) {
        this.id = key;
    }

}
