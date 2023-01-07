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

package com.ibm.ws.jpa.fvt.entity.entities.attributeconfig.xml;

import com.ibm.ws.jpa.fvt.entity.entities.IAttributeConfigFieldEntity;
import com.ibm.ws.jpa.fvt.entity.support.SerializableClass;

//@Entity
//@Table(catalog="jpacat", schema="jpaschema", name="AttrCfgFieldEnt",
//	   uniqueConstraints=@UniqueConstraint(columnNames={"UNIQUECONSTRAINTSTRING"}))
public class XMLAttrConfigFieldEntity implements IAttributeConfigFieldEntity {
    // @Id
    private int id;

    // @Basic attribute test items

    // @Basic(fetch=FetchType.EAGER)
    private String stringValEager;

    // @Basic(fetch=FetchType.LAZY)
    private String stringValLazy;

    // @Basic(optional=false)
    private String stringValOptional;

    // @Column attribute test items

    // @Column(name="intValColName")
    private int intValColumnName;

    // @Column(nullable=false)
    // private String stringValColumnNullable;
    private SerializableClass notNullable;

    // @Column(unique=true)
    private String uniqueString;

    private String uniqueConstraintString;

    // @Column(table="AltColumnTable", name="intValCol")
    private int intValColumnTable;

    // @Column(length=12)
    private String stringValColumnLength;

    // @Column(precision=2)
    private float floatValColumnPrecision;

    // @Column(scale=2)
    private float floatValColumnScale;

    public XMLAttrConfigFieldEntity() {

    }

    @Override
    public float getFloatValColumnPrecision() {
        return floatValColumnPrecision;
    }

    @Override
    public void setFloatValColumnPrecision(float floatValColumnPrecision) {
        this.floatValColumnPrecision = floatValColumnPrecision;
    }

    @Override
    public float getFloatValColumnScale() {
        return floatValColumnScale;
    }

    @Override
    public void setFloatValColumnScale(float floatValColumnScale) {
        this.floatValColumnScale = floatValColumnScale;
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
    public int getIntValColumnName() {
        return intValColumnName;
    }

    @Override
    public void setIntValColumnName(int intValColumnName) {
        this.intValColumnName = intValColumnName;
    }

    @Override
    public int getIntValColumnTable() {
        return intValColumnTable;
    }

    @Override
    public void setIntValColumnTable(int intValColumnTable) {
        this.intValColumnTable = intValColumnTable;
    }

    @Override
    public String getStringValColumnLength() {
        return stringValColumnLength;
    }

    @Override
    public void setStringValColumnLength(String stringValColumnLength) {
        this.stringValColumnLength = stringValColumnLength;
    }

    @Override
    public SerializableClass getNotNullable() {
        return notNullable;
    }

    @Override
    public void setNotNullable(SerializableClass notNullable) {
        this.notNullable = notNullable;
    }

    @Override
    public String getStringValEager() {
        return stringValEager;
    }

    @Override
    public void setStringValEager(String stringValEager) {
        this.stringValEager = stringValEager;
    }

    @Override
    public String getStringValLazy() {
        return stringValLazy;
    }

    @Override
    public void setStringValLazy(String stringValLazy) {
        this.stringValLazy = stringValLazy;
    }

    @Override
    public String getStringValOptional() {
        return stringValOptional;
    }

    @Override
    public void setStringValOptional(String stringValOptional) {
        this.stringValOptional = stringValOptional;
    }

    @Override
    public String getUniqueString() {
        return uniqueString;
    }

    @Override
    public void setUniqueString(String uniqueString) {
        this.uniqueString = uniqueString;
    }

    @Override
    public String getUniqueConstraintString() {
        return uniqueConstraintString;
    }

    @Override
    public void setUniqueConstraintString(String uniqueConstraintString) {
        this.uniqueConstraintString = uniqueConstraintString;
    }

    @Override
    public String toString() {
        return "XMLAttrConfigFieldEntity [id=" + id + "]";
    }

}
