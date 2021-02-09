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

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.model;

import java.util.Calendar;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 * <p>Entity of the Common Datamodel (which uses all the possible JPA 2.0 Annotations as described in the
 * <a href="http://www.j2ee.me/javaee/6/docs/api/javax/persistence/package-summary.html">javax.persistence documentation</a>)
 *
 *
 * <p>These annotations are exercised:
 * <ul>
 * <li><b>@Access</b> (01 of 65):
 * <p>The Access annotation is used to specify an access type to be applied to an entity class, mapped superclass, or embeddable class, or to a specific attribute of such a class.
 * <p>Possible @Access combinations:
 * <ul>
 * <li>ElementType.TYPE = Entity
 * <li>ElementType.TYPE = Mapped Superclass
 * <li>ElementType.TYPE = Embeddable
 * <li>AccessType.FIELD on class
 * <li>AccessType.PROPERTY on class
 * <li>AccessType.FIELD on field
 * <li>AccessType.PROPERTY on property
 * </ul>
 * <p>@Access combinations exercised in this entity:
 * <ul>
 * <li>ElementType.TYPE = Entity
 * <li>AccessType.FIELD on class
 * </ul>
 *
 *
 * <li><b>@Basic</b> (06 or 65):
 * <p>The Basic annotation is the simplest type of mapping to a database column. It is optional (usually for documentation puposes) and does not need to be used to make a field or
 * property persistent.
 * <p>Possible @Basic combinations:
 * <ul>
 * <li>FetchType.EAGER on field (to persist)
 * <li>FetchType.LAZY on field (to persist)
 * <li>FetchType.EAGER on property (to persist)
 * <li>FetchType.LAZY on property (to persist)
 * </ul>
 * <p>@Basic combinations exercised in this entity:
 * <ul>
 * <li>FetchType.EAGER on field
 * </ul>
 *
 *
 * <li><b>@Column</b> (07 of 65):
 * <p>Is used to specify a mapped column for a persistent property or field.
 * <p>Possible @Column combinations:
 * <ul>
 * <li>@Column on field
 * <li>@Column on property
 * <li>columnDefinition
 * <li>insertable
 * <li>length
 * <li>name
 * <li>nullable
 * <li>precision
 * <li>scale
 * <li>table
 * <li>updatable
 * </ul>
 * <p>@Column combinations exercised in this entity:
 * <ul>
 * <li>@Column on field
 * <li>columnDefinition
 * <li>insertable
 * <li>length
 * <li>name
 * <li>nullable
 * <li>precision
 * <li>scale
 * <li>table
 * <li>updatable
 * </ul>
 *
 *
 * <li><b>@Entity</b> (14 of 65):
 * <p>Specifies that the class is an entity.
 * <p>Possible @Entity combinations:
 * <ul>
 * <li>@Entity on top-level class
 * <li>@Entity on top-level class with name specified
 * </ul>
 * <p>@Entity combinations exercised in this entity:
 * <ul>
 * <li>@Entity on top-level class
 * </ul>
 *
 *
 * <li><b>@Id</b> (22 of 65):
 * <p>Specifies the primary key property or field of an entity
 * <p>These are the possible @Id combinations:
 * <ul>
 * <li>@Id on field
 * <li>@Id on property
 * </ul>
 * <p>These are the @Id combinations exercised in this entity:
 * <ul>
 * <li>@Id on field
 * </ul>
 *
 *
 * <li><b>@Inheritance</b> (24 of 65):
 * <p>Defines the inheritance strategy to be used for an entity class hierarchy.
 * <p>These are the possible @Inheritance combinations:
 * <ul>
 * <li>@Inheritance on Entity
 * <li>strategy=InheritanceType.JOINED
 * <li>strategy=InheritanceType.SINGLE_TABLE)
 * <li>strategy=InheritanceType.TABLE_PER_CLASS)
 * </ul>
 * <p>These are the @Inheritance combinations exercised in this entity:
 * <ul>
 * <li>@Inheritance on Entity
 * <li>strategy=InheritanceType.TABLE_PER_CLASS)
 * </ul>
 *
 *
 * <li><b>@Table</b> (60 of 65):
 * <p>This annotation specifies the primary table for the annotated entity.
 * <p>Possible @Table combinations:
 * <ul>
 * <li>@Table on Entity
 * <li>name
 * <li>catalog
 * <li>schema
 * <li>uniqueConstraints
 * </ul>
 * <p>@Table combinations exercised in this entity:
 * <ul>
 * <li>@Table on Entity
 * <li>name
 * <li>catalog
 * <li>schema
 * <li>uniqueConstraints
 * </ul>
 *
 *
 * <li><b>@Temporal</b> (62 of 65):
 * <p>This annotation must be specified for persistent fields or properties of type java.util.Date and java.util.Calendar to provide metadata to the JDBC driver so it will know
 * which of the java.sql types to use.
 * This additional JDBC metadata is not required for the java.sql.Date, java.sql.Time, and java.sql.Timestamp fields/propropties.
 * <p>@Temporal combinations:
 * <ul>
 * <li>TemporalType.DATE on java.util.Date field/property
 * <li>TemporalType.DATE on java.util.Calendar field/property
 * <li>TemporalType.TIME on java.util.Date field/property
 * <li>TemporalType.TIME on java.util.Calendar field/property
 * <li>TemporalType.TIMESTAMP on java.util.Date field/property
 * <li>TemporalType.TIMESTAMP on java.util.Calendar field/property
 * </ul>
 * <p>@Temporal combinations exercised in this entity:
 * <ul>
 * <li>TemporalType.DATE on java.util.Date field
 * <li>TemporalType.DATE on java.util.Calendar field
 * <li>TemporalType.TIME on java.util.Date field
 * <li>TemporalType.TIME on java.util.Calendar field
 * <li>TemporalType.TIMESTAMP on java.util.Date field
 * <li>TemporalType.TIMESTAMP on java.util.Calendar field
 * </ul>
 *
 *
 * <li><b>@Version</b> (65 of 65):
 * <p>This annotation specifies the version field or property of an entity class that serves as its optimistic lock value.
 * <p>@Version combinations:
 * <ul>
 * <li>@Version on field
 * <li>@Version on property
 * </ul>
 * <p>@Version combinations exercised in this entity:
 * <ul>
 * <li>@Version on field
 * </ul>
 * </ul>
 *
 *
 * <p><b>Notes:</b>
 * <ol>
 * <li>None
 * </ol>
 */
@Access(AccessType.FIELD)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "CDM_EntityTemporal")
//     catalog = "CDM_EntityTemporal_Catalog",
//     schema = "CMD_EntityTemporal_Schema",
//     uniqueConstraints=@UniqueConstraint(columnNames={"entity01_string01"}))
public class EntityTemporal {

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_id",
            nullable = false,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Id
    private int entityTemporal_id;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_version",
            nullable = false,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.DATE)
    @Version
    private java.util.Date entityTemporal_version;

    //----------------------------------------------------------------------------------------------
    // @Temporal combinations
    //----------------------------------------------------------------------------------------------
    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_calendar01",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.DATE)
    private Calendar entityTemporal_calendar01;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_calendar02",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.DATE)
    private Calendar[] entityTemporal_calendar02;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date01",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.DATE)
    private java.util.Date entityTemporal_date01;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date02",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.DATE)
    private java.util.Date[] entityTemporal_date02;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_calendar03",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIME)
    private Calendar entityTemporal_calendar03;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_calendar04",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIME)
    private Calendar[] entityTemporal_calendar04;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date03",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIME)
    private java.util.Date entityTemporal_date03;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date04",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIME)
    private java.util.Date[] entityTemporal_date04;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_calendar05",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar entityTemporal_calendar05;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_calendar06",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar[] entityTemporal_calendar06;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date05",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date entityTemporal_date05;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date06",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date[] entityTemporal_date06;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date07",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    private java.sql.Date entityTemporal_date07;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_date08",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    private java.sql.Date[] entityTemporal_date08;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_time01",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    private java.sql.Time entityTemporal_time01;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_time02",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    private java.sql.Time[] entityTemporal_time02;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_timestamp01",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    private java.sql.Timestamp entityTemporal_timestamp01;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityTemporal_timestamp02",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityTemporal",
            updatable = true)
    private java.sql.Timestamp[] entityTemporal_timestamp02;

    public EntityTemporal() {}

    public EntityTemporal(int id,
                          java.util.Date version,
                          Calendar calendar01,
                          Calendar[] calendar02,
                          java.util.Date date01,
                          java.util.Date[] date02,
                          Calendar calendar03,
                          Calendar[] calendar04,
                          java.util.Date date03,
                          java.util.Date[] date04,
                          Calendar calendar05,
                          Calendar[] calendar06,
                          java.util.Date date05,
                          java.util.Date[] date06,
                          java.sql.Date date07,
                          java.sql.Date[] date08,
                          java.sql.Time time01,
                          java.sql.Time[] time02,
                          java.sql.Timestamp timestamp01,
                          java.sql.Timestamp[] timestamp02) {
        this.entityTemporal_id = id;
        this.entityTemporal_version = version;
        this.entityTemporal_calendar01 = calendar01;
        this.entityTemporal_calendar02 = calendar02;
        this.entityTemporal_date01 = date01;
        this.entityTemporal_date02 = date02;
        this.entityTemporal_calendar03 = calendar03;
        this.entityTemporal_calendar04 = calendar04;
        this.entityTemporal_date03 = date03;
        this.entityTemporal_date04 = date04;
        this.entityTemporal_calendar05 = calendar05;
        this.entityTemporal_calendar06 = calendar06;
        this.entityTemporal_date05 = date05;
        this.entityTemporal_date06 = date06;
        this.entityTemporal_date07 = date07;
        this.entityTemporal_date08 = date08;
        this.entityTemporal_time01 = time01;
        this.entityTemporal_time02 = time02;
        this.entityTemporal_timestamp01 = timestamp01;
        this.entityTemporal_timestamp02 = timestamp02;
    }

    @Override
    public String toString() {
        return (" EntityTemporal: " +
                " entityTemporal_id: " + getEntityTemporal_id() +
                " entityTemporal_version: " + getEntityTemporal_version() +
                " entityTemporal_calendar01: " + getEntityTemporal_calendar01() +
                " entityTemporal_calendar02: " + getEntityTemporal_calendar02() +
                " entityTemporal_date01: " + getEntityTemporal_date01() +
                " entityTemporal_date02: " + getEntityTemporal_date02() +
                " entityTemporal_calendar03: " + getEntityTemporal_calendar03() +
                " entityTemporal_calendar04: " + getEntityTemporal_calendar04() +
                " entityTemporal_date03: " + getEntityTemporal_date03() +
                " entityTemporal_date04: " + getEntityTemporal_date04() +
                " entityTemporal_calendar03: " + getEntityTemporal_calendar03() +
                " entityTemporal_calendar04: " + getEntityTemporal_calendar04() +
                " entityTemporal_date03: " + getEntityTemporal_date03() +
                " entityTemporal_date04: " + getEntityTemporal_date04() +
                " entityTemporal_calendar05: " + getEntityTemporal_calendar05() +
                " entityTemporal_calendar06: " + getEntityTemporal_calendar06() +
                " entityTemporal_date05: " + getEntityTemporal_date05() +
                " entityTemporal_date06: " + getEntityTemporal_date06() +
                " entityTemporal_date07: " + getEntityTemporal_date07() +
                " entityTemporal_date08: " + getEntityTemporal_date08() +
                " entityTemporal_time01: " + getEntityTemporal_time01() +
                " entityTemporal_time02: " + getEntityTemporal_time02() +
                " entityTemporal_timestamp01: " + getEntityTemporal_timestamp01() +
                " entityTemporal_timestamp02: " + getEntityTemporal_timestamp02());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntityTemporal_id() {
        return entityTemporal_id;
    }

    public void setEntityTemporal_id(int p) {
        this.entityTemporal_id = p;
    }

    public java.util.Date getEntityTemporal_version() {
        return entityTemporal_version;
    }

    public void setEntityTemporal_version(java.util.Date p) {
        this.entityTemporal_version = p;
    }

    public Calendar getEntityTemporal_calendar01() {
        return entityTemporal_calendar01;
    }

    public void setEntityTemporal_calendar01(Calendar p) {
        this.entityTemporal_calendar01 = p;
    }

    public Calendar[] getEntityTemporal_calendar02() {
        return entityTemporal_calendar02;
    }

    public void setEntityTemporal_calendar02(Calendar[] p) {
        this.entityTemporal_calendar02 = p;
    }

    public java.util.Date getEntityTemporal_date01() {
        return entityTemporal_date01;
    }

    public void setEntityTemporal_date01(java.util.Date p) {
        this.entityTemporal_date01 = p;
    }

    public java.util.Date[] getEntityTemporal_date02() {
        return entityTemporal_date02;
    }

    public void setEntityTemporal_date02(java.util.Date[] p) {
        this.entityTemporal_date02 = p;
    }

    public Calendar getEntityTemporal_calendar03() {
        return entityTemporal_calendar03;
    }

    public void setEntityTemporal_calendar03(Calendar p) {
        this.entityTemporal_calendar03 = p;
    }

    public Calendar[] getEntityTemporal_calendar04() {
        return entityTemporal_calendar04;
    }

    public void setEntityTemporal_calendar04(Calendar[] p) {
        this.entityTemporal_calendar04 = p;
    }

    public java.util.Date getEntityTemporal_date03() {
        return entityTemporal_date03;
    }

    public void setEntityTemporal_date03(java.util.Date p) {
        this.entityTemporal_date03 = p;
    }

    public java.util.Date[] getEntityTemporal_date04() {
        return entityTemporal_date04;
    }

    public void setEntityTemporal_date04(java.util.Date[] p) {
        this.entityTemporal_date04 = p;
    }

    public Calendar getEntityTemporal_calendar05() {
        return entityTemporal_calendar05;
    }

    public void setEntityTemporal_calendar05(Calendar p) {
        this.entityTemporal_calendar05 = p;
    }

    public Calendar[] getEntityTemporal_calendar06() {
        return entityTemporal_calendar06;
    }

    public void setEntityTemporal_calendar06(Calendar[] p) {
        this.entityTemporal_calendar06 = p;
    }

    public java.util.Date getEntityTemporal_date05() {
        return entityTemporal_date05;
    }

    public void setEntityTemporal_date05(java.util.Date p) {
        this.entityTemporal_date05 = p;
    }

    public java.util.Date[] getEntityTemporal_date06() {
        return entityTemporal_date06;
    }

    public void setEntityTemporal_date06(java.util.Date[] p) {
        this.entityTemporal_date06 = p;
    }

    public java.sql.Date getEntityTemporal_date07() {
        return entityTemporal_date07;
    }

    public void setEntityTemporal_date07(java.sql.Date p) {
        this.entityTemporal_date07 = p;
    }

    public java.sql.Date[] getEntityTemporal_date08() {
        return entityTemporal_date08;
    }

    public void setEntityTemporal_date08(java.sql.Date[] p) {
        this.entityTemporal_date08 = p;
    }

    public java.sql.Time getEntityTemporal_time01() {
        return entityTemporal_time01;
    }

    public void setEntityTemporal_time01(java.sql.Time p) {
        this.entityTemporal_time01 = p;
    }

    public java.sql.Time[] getEntityTemporal_time02() {
        return entityTemporal_time02;
    }

    public void setEntityTemporal_time02(java.sql.Time[] p) {
        this.entityTemporal_time02 = p;
    }

    public java.sql.Timestamp getEntityTemporal_timestamp01() {
        return entityTemporal_timestamp01;
    }

    public void setEntityTemporal_timestamp01(java.sql.Timestamp p) {
        this.entityTemporal_timestamp01 = p;
    }

    public java.sql.Timestamp[] getEntityTemporal_timestamp02() {
        return entityTemporal_timestamp02;
    }

    public void setEntityTemporal_timestamp02(java.sql.Timestamp[] p) {
        this.entityTemporal_timestamp02 = p;
    }
}
