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

package com.ibm.ws.jpa.fvt.criteriaquery.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
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
 * <li>@Id on field/propertyimport javax.persistence.Entity;
 * import javax.persistence.Id;
 * import javax.persistence.Inheritance;
 * import javax.persistence.InheritanceType;
 * import javax.persistence.NamedQueries;
 * import javax.persistence.NamedQuery;
 * import javax.persistence.Table;
 * import javax.persistence.Version;
 * ble)
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
 * <li>@Id on field/property (of type Float)
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
 * <li>@Version on field/property (of type Integer)
 * </ul>
 * </ul>
 *
 *
 * <p><b>Notes:</b>
 * <ol>
 * <li>Uses a simple primary key
 * </ol>
 */
@NamedQueries(
              value = {
                        @NamedQuery(name = "ENTITY0009_SELECT",
                                    query = "SELECT e FROM Entity0009 e WHERE e.entity0009_id = :id_0009"),
                        @NamedQuery(
                                    name = "ENTITY0009_UPDATE",
                                    query = "UPDATE Entity0009 e SET e.entity0009_string01 = :string01_0009, e.entity0009_string02 = :string02_0009, e.entity0009_string03 = :string03_0009 WHERE e.entity0009_id = :id_0009")
              })
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "CDM_Entity0009")
public class Entity0009 {

    @Id
    private Float entity0009_id;

    @Version
    private Integer entity0009_version;

    private String entity0009_string01;

    private String entity0009_string02;

    private String entity0009_string03;

    public Entity0009() {
    }

    public Entity0009(Float id,
                      String string01,
                      String string02,
                      String string03) {
        this.entity0009_id = id;
        this.entity0009_string01 = string01;
        this.entity0009_string02 = string02;
        this.entity0009_string03 = string03;
    }

    public Entity0009(Float id,
                      Integer version,
                      String string01,
                      String string02,
                      String string03) {
        this.entity0009_id = id;
        this.entity0009_version = version;
        this.entity0009_string01 = string01;
        this.entity0009_string02 = string02;
        this.entity0009_string03 = string03;
    }

    @Override
    public String toString() {
        return (" Entity0009: " +
                " entity0009_id: " + getEntity0009_id() +
                " entity0009_version: " + getEntity0009_version() +
                " entity0009_string01: " + getEntity0009_string01() +
                " entity0009_string02: " + getEntity0009_string02() +
                " entity0009_string03: " + getEntity0009_string03());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public Float getEntity0009_id() {
        return entity0009_id;
    }

    public void setEntity0009_id(Float p) {
        this.entity0009_id = p;
    }

    public Integer getEntity0009_version() {
        return entity0009_version;
    }

    public void setEntity0009_version(Integer p) {
        this.entity0009_version = p;
    }

    public String getEntity0009_string01() {
        return entity0009_string01;
    }

    public void setEntity0009_string01(String p) {
        this.entity0009_string01 = p;
    }

    public String getEntity0009_string02() {
        return entity0009_string02;
    }

    public void setEntity0009_string02(String p) {
        this.entity0009_string02 = p;
    }

    public String getEntity0009_string03() {
        return entity0009_string03;
    }

    public void setEntity0009_string03(String p) {
        this.entity0009_string03 = p;
    }
}
