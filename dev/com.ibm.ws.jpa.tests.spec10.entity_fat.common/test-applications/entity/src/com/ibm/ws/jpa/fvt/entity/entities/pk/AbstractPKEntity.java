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

package com.ibm.ws.jpa.fvt.entity.entities.pk;

import java.sql.Date;

import com.ibm.ws.jpa.fvt.entity.entities.IPKEntity;

public abstract class AbstractPKEntity implements IPKEntity {

    public AbstractPKEntity() {
    }

    @Override
    public boolean getBooleanPK() {
        return false;
    }

    @Override
    public Boolean getBooleanWrapperPK() {
        return new Boolean(false);
    }

    @Override
    public byte getBytePK() {
        return (byte) 0;
    }

    @Override
    public Byte getByteWrapperPK() {
        return new Byte((byte) 0);
    }

    @Override
    public char getCharPK() {
        return ' ';
    }

    @Override
    public Character getCharacterWrapperPK() {
        return new Character(' ');
    }

    @Override
    public int getIntPK() {
        return 0;
    }

    @Override
    public Integer getIntegerWrapperPK() {
        return new Integer(0);
    }

    @Override
    public Date getJavaSqlDatePK() {
        return null;
    }

    @Override
    public java.util.Date getJavaUtilDatePK() {
        return null;
    }

    @Override
    public long getLongPK() {
        return 0;
    }

    @Override
    public Long getLongWrapperPK() {
        return new Long(0);
    }

    @Override
    public short getShortPK() {
        return 0;
    }

    @Override
    public Short getShortWrapperPK() {
        return new Short((short) 0);
    }

    @Override
    public String getStringPK() {
        return new String();
    }

    @Override
    public void setBooleanPK(boolean pkey) {
    }

    @Override
    public void setBooleanWrapperPK(Boolean pkey) {
    }

    @Override
    public void setBytePK(byte pkey) {
    }

    @Override
    public void setByteWrapperPK(Byte pkey) {
    }

    @Override
    public void setCharPK(char pkey) {
    }

    @Override
    public void setCharacterWrapperPK(Character pkey) {
    }

    @Override
    public void setIntPK(int pkey) {
    }

    @Override
    public void setIntegerWrapperPK(Integer pkey) {
    }

    @Override
    public void setJavaSqlDatePK(Date pkey) {
    }

    @Override
    public void setJavaUtilDatePK(java.util.Date pkey) {
    }

    @Override
    public void setLongPK(long pkey) {
    }

    @Override
    public void setLongWrapperPK(Long pkey) {
    }

    @Override
    public void setShortPK(short pkey) {
    }

    @Override
    public void setShortWrapperPK(Short pkey) {
    }

    @Override
    public void setStringPK(String pkey) {
    }
}
