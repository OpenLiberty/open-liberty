/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.commonentities.jpa10.simple;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "CMN10_SimpleVEnt")
public class SimpleVersionedEntity10 implements ISimpleVersionedEntity10 {
    @Id
    private int id;

    private String strData;
    private char charData;

    private int intData;
    private long longData;

    private float floatData;
    private double doubleData;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] byteArrData;

    @Version
    private int version;

    public SimpleVersionedEntity10() {

    }

    public SimpleVersionedEntity10(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStrData() {
        return strData;
    }

    public void setStrData(String strData) {
        this.strData = strData;
    }

    public char getCharData() {
        return charData;
    }

    public void setCharData(char charData) {
        this.charData = charData;
    }

    public int getIntData() {
        return intData;
    }

    public void setIntData(int intData) {
        this.intData = intData;
    }

    public long getLongData() {
        return longData;
    }

    public void setLongData(long longData) {
        this.longData = longData;
    }

    public float getFloatData() {
        return floatData;
    }

    public void setFloatData(float floatData) {
        this.floatData = floatData;
    }

    public double getDoubleData() {
        return doubleData;
    }

    public void setDoubleData(double doubleData) {
        this.doubleData = doubleData;
    }

    public byte[] getByteArrData() {
        return byteArrData;
    }

    public void setByteArrData(byte[] byteArrData) {
        this.byteArrData = byteArrData;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "SimpleVersionedEntity10 [id=" + id + ", strData=" + strData
               + ", charData=" + charData + ", intData=" + intData
               + ", longData=" + longData + ", floatData=" + floatData
               + ", doubleData=" + doubleData + ", version=" + version + "]";
    }
}