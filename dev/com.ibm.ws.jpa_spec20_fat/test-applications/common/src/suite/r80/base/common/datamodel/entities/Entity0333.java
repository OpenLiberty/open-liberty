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

package suite.r80.base.common.datamodel.entities;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 * <p>Entity of the Common Datamodel (which uses all the possible JPA 2.0 Annotations as described in the
 * <a href="http://www.j2ee.me/javaee/6/docs/api/javax/persistence/package-summary.html">javax.persistence documentation</a>)
 * All the Entity0xxx named entities are the base of a class hierarchy and define the primary key for the entire hierarchy.
 * Per the JSR 317 final spec (page 28), the primary key of these base classes may be any of the Java primitive types,
 * Java primitive wrapper types, or a composite primary key (via @IdClass annotation)
 *
 *
 * <p>These annotations are exercised:
 * <ul>
 * <li><b>@Id</b> (22 of 65):
 * <p>Specifies the primary key property or field of an entity
 * <p>These are the possible @Id combinations:
 * <ol>
 * <li>@Id on field/property (of type byte)
 * <li>@Id on field/property (of type Byte)
 * <li>@Id on field/property (of type char)
 * <li>@Id on field/property (of type Character)
 * <li>@Id on field/property (of type String)
 * <li>@Id on field/property (of type double)
 * <li>@Id on field/property (of type Double)
 * <li>@Id on field/property (of type float)
 * <li>@Id on field/property (of type Float)
 * <li>@Id on field/property (of type int)
 * <li>@Id on field/property (of type Integer)
 * <li>@Id on field/property (of type long)
 * <li>@Id on field/property (of type Long)
 * <li>@Id on field/property (of type short)
 * <li>@Id on field/property (of type Short)
 * <li>@Id on field/property (of type BigDecimal)
 * <li>@Id on field/property (of type BigInteger)
 * <li>@Id on field/property (of type java.util.Date)
 * <li>@Id on field/property (of type java.sql.Date)
 * <li>@Id on field/property (of type Object)
 * <li>@Id on field/property (of type Object ID)
 * </ol>
 * <p>These are the @Id combinations exercised in this entity:
 * <ul>
 * <li>@Id on field/property (of type short)
 * <li>@Id on field/property (of type BigDecimal)
 * <li>@Id on field/property (of type java.util.Date)
 * </ul>
 *
 * <li><b>@Version</b> (65 of 65):
 * <p>This annotation specifies the version field or property of an entity class that serves as its optimistic lock value
 * <p>These are the possible @Version combinations:
 * <ul>
 * <li>@Version on field/property (of type int)
 * <li>@Version on field/property (of type Integer)
 * <li>@Version on field/property (of type long)
 * <li>@Version on field/property (of type Long)
 * <li>@Version on field/property (of type short)
 * <li>@Version on field/property (of type Short)
 * <li>@Version on field/property (of type Timestamp)
 * </ul>
 * <p>These are the @Version combinations exercised in this entity:
 * <ul>
 * <li>@Version on field/property (of type Short)
 * </ul>
 * </ul>
 *
 *
 * <p><b>Notes:</b>
 * <ol>
 * <li>Uses a compound primary key and an Id class
 * <li>Per the JSR-317 spec (page 26), if java.util.Date is used as a primary key field or property, the temporal
 * type should be specified as DATE
 * </ol>
 */
@NamedQueries(
              value = {
                        @NamedQuery(name = "ENTITY0333_SELECT",
                                    query = "SELECT e FROM Entity0333 e WHERE e.entity0333_id1 = :id1_0333 AND e.entity0333_id2 = :id2_0333 AND e.entity0333_id3 = :id3_0333")
              })
@Entity
@IdClass(IdClass0333.class)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "CDM_Entity0333")
public class Entity0333 {

    @Id
    private short entity0333_id1;

    @Id
    private BigDecimal entity0333_id2;

    @Id
    @Temporal(TemporalType.DATE)
    private java.util.Date entity0333_id3;

    @Version
    private Short entity0333_version;

    private String entity0333_string01;

    private String entity0333_string02;

    private String entity0333_string03;

    public Entity0333() {}

    public Entity0333(short id1,
                      BigDecimal id2,
                      java.util.Date id3,
                      Short version,
                      String string01,
                      String string02,
                      String string03) {
        this.entity0333_id1 = id1;
        this.entity0333_id2 = id2;
        this.entity0333_id3 = id3;
        this.entity0333_version = version;
        this.entity0333_string01 = string01;
        this.entity0333_string02 = string02;
        this.entity0333_string03 = string03;
    }

    @Override
    public String toString() {
        return (" Entity0333: " +
                " entity0333_id1: " + getEntity0333_id1() +
                " entity0333_id2: " + getEntity0333_id2() +
                " entity0333_id3: " + getEntity0333_id3() +
                " entity0333_version: " + getEntity0333_version() +
                " entity0333_string01: " + getEntity0333_string01() +
                " entity0333_string02: " + getEntity0333_string02() +
                " entity0333_string03: " + getEntity0333_string03());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public short getEntity0333_id1() {
        return entity0333_id1;
    }

    public void setEntity0333_id1(short p) {
        this.entity0333_id1 = p;
    }

    public BigDecimal getEntity0333_id2() {
        return entity0333_id2;
    }

    public void setEntity0333_id2(BigDecimal p) {
        this.entity0333_id2 = p;
    }

    public java.util.Date getEntity0333_id3() {
        return entity0333_id3;
    }

    public void setEntity0333_id3(java.util.Date p) {
        this.entity0333_id3 = p;
    }

    public Short getEntity0333_version() {
        return entity0333_version;
    }

    public void setEntity0333_version(Short p) {
        this.entity0333_version = p;
    }

    public String getEntity0333_string01() {
        return entity0333_string01;
    }

    public void setEntity0333_string01(String p) {
        this.entity0333_string01 = p;
    }

    public String getEntity0333_string02() {
        return entity0333_string02;
    }

    public void setEntity0333_string02(String p) {
        this.entity0333_string02 = p;
    }

    public String getEntity0333_string03() {
        return entity0333_string03;
    }

    public void setEntity0333_string03(String p) {
        this.entity0333_string03 = p;
    }
}
