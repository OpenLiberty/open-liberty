/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.commonentities.jpa10.simple;

public class XMLSimpleVersionedEntity10 implements ISimpleVersionedEntity10 {
    private int id;

    private String strData;
    private char charData;

    private int intData;
    private long longData;

    private float floatData;
    private double doubleData;

    private byte[] byteArrData;

    private int version;

    public XMLSimpleVersionedEntity10() {

    }

    public XMLSimpleVersionedEntity10(int id) {
        this.id = id;
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
    public String getStrData() {
        return strData;
    }

    @Override
    public void setStrData(String strData) {
        this.strData = strData;
    }

    @Override
    public char getCharData() {
        return charData;
    }

    @Override
    public void setCharData(char charData) {
        this.charData = charData;
    }

    @Override
    public int getIntData() {
        return intData;
    }

    @Override
    public void setIntData(int intData) {
        this.intData = intData;
    }

    @Override
    public long getLongData() {
        return longData;
    }

    @Override
    public void setLongData(long longData) {
        this.longData = longData;
    }

    @Override
    public float getFloatData() {
        return floatData;
    }

    @Override
    public void setFloatData(float floatData) {
        this.floatData = floatData;
    }

    @Override
    public double getDoubleData() {
        return doubleData;
    }

    @Override
    public void setDoubleData(double doubleData) {
        this.doubleData = doubleData;
    }

    @Override
    public byte[] getByteArrData() {
        return byteArrData;
    }

    @Override
    public void setByteArrData(byte[] byteArrData) {
        this.byteArrData = byteArrData;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "XMLSimpleVersionedEntity10 [id=" + id + ", strData=" + strData
               + ", charData=" + charData + ", intData=" + intData
               + ", longData=" + longData + ", floatData=" + floatData
               + ", doubleData=" + doubleData + ", version=" + version + "]";
    }
}
