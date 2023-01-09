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

public interface IPKEntity {
    /*
     * Support for IPKEntities with a boolean/Boolean primary key
     */
    public boolean getBooleanPK();

    public Boolean getBooleanWrapperPK();

    public void setBooleanPK(boolean pkey);

    public void setBooleanWrapperPK(Boolean pkey);

    /*
     * Support for IPKEntities with a byte/Byte primary key
     */
    public byte getBytePK();

    public Byte getByteWrapperPK();

    public void setBytePK(byte pkey);

    public void setByteWrapperPK(Byte pkey);

    /*
     * Support for IPKEntities with a char/Char primary key
     */
    public char getCharPK();

    public Character getCharacterWrapperPK();

    public void setCharPK(char pkey);

    public void setCharacterWrapperPK(Character pkey);

    /*
     * Support for IPKEntities with an int/Integer primary key
     */
    public int getIntPK();

    public Integer getIntegerWrapperPK();

    public void setIntPK(int pkey);

    public void setIntegerWrapperPK(Integer pkey);

    /*
     * Support for IPKEntities with a short/Short primary key
     */
    public short getShortPK();

    public Short getShortWrapperPK();

    public void setShortPK(short pkey);

    public void setShortWrapperPK(Short pkey);

    /*
     * Support for IPKEntities with a long/Long primary key
     */
    public long getLongPK();

    public Long getLongWrapperPK();

    public void setLongPK(long pkey);

    public void setLongWrapperPK(Long pkey);

    /*
     * Support for IPKEntities with a String primary key
     */
    public String getStringPK();

    public void setStringPK(String pkey);

    /*
     * Support for IPKEntities with a java.util.Date primary key
     */
    public java.util.Date getJavaUtilDatePK();

    public void setJavaUtilDatePK(java.util.Date pkey);

    /*
     * Support for IPKEntities with a java.sql.Date primary key
     */
    public java.sql.Date getJavaSqlDatePK();

    public void setJavaSqlDatePK(java.sql.Date pkey);

    /*
     * Accessor methods for entity data payload.
     */
    public int getIntVal();

    public void setIntVal(int intVal);

}
