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

import java.io.Serializable;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Table;
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
 * <li><b>@Lob</b> (28 of 65):
 * <p>Specifies that a persistent property or field should be persisted as a large object to a database-supported large object type.
 * <p>@Lob combinations:
 * <ul>
 * <li>@Lob on field
 * <li>@Lob on property
 * </ul>
 * <p>@Lob combinations exercised in this entity:
 * <ul>
 * <li>@Lob on field
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
@Table(name = "CDM_EntityLob")
//     catalog = "CDM_EntityLob_Catalog",
//     schema = "CMD_EntityLob_Schema",
//     uniqueConstraints=@UniqueConstraint(columnNames={"entity01_string01"}))
public class EntityLob {

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_id",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Id
    private int entityLob_id;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_version",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Version
    private int entityLob_version;

    //----------------------------------------------------------------------------------------------
    // @Lob combinations
    //----------------------------------------------------------------------------------------------
    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob01",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private Byte[] entityLob_lob01;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob02",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private byte[] entityLob_lob02;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob03",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private String entityLob_lob03;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob04",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private String[] entityLob_lob04;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob05",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private Object entityLob_lob05;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob06",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private Object[] entityLob_lob06;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob07",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private List<String> entityLob_lob07;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob08",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private List<String>[] entityLob_lob08;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob09",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private char[] entityLob_lob09;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob10",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private Character[] entityLob_lob10;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob11",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private Serializable entityLob_lob11;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityLob_lob12",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityLob",
            updatable = true)
    @Lob
    private Serializable[] entityLob_lob12;

    public EntityLob() {}

    public EntityLob(int id,
                     int version,
                     Byte[] lob01,
                     byte[] lob02,
                     String lob03,
                     String[] lob04,
                     Object lob05,
                     Object[] lob06,
                     List<String> lob07,
                     List<String>[] lob08,
                     char[] lob09,
                     Character[] lob10,
                     Serializable lob11,
                     Serializable[] lob12) {
        this.entityLob_id = id;
        this.entityLob_version = version;
        this.entityLob_lob01 = lob01;
        this.entityLob_lob02 = lob02;
        this.entityLob_lob03 = lob03;
        this.entityLob_lob04 = lob04;
        this.entityLob_lob05 = lob05;
        this.entityLob_lob06 = lob06;
        this.entityLob_lob07 = lob07;
        this.entityLob_lob08 = lob08;
        this.entityLob_lob09 = lob09;
        this.entityLob_lob10 = lob10;
        this.entityLob_lob11 = lob11;
        this.entityLob_lob12 = lob12;
    }

    @Override
    public String toString() {
        return (" EntityLob: " +
                " entityLob_id: " + getEntityLob_id() +
                " entityLob_version: " + getEntityLob_version() +
                " entityLob_lob01: " + getEntityLob_lob01() +
                " entityLob_lob02: " + getEntityLob_lob02() +
                " entityLob_lob03: " + getEntityLob_lob03() +
                " entityLob_lob04: " + getEntityLob_lob04() +
                " entityLob_lob05: " + getEntityLob_lob05() +
                " entityLob_lob06: " + getEntityLob_lob06() +
                " entityLob_lob07: " + getEntityLob_lob07() +
                " entityLob_lob08: " + getEntityLob_lob08() +
                " entityLob_lob09: " + getEntityLob_lob09() +
                " entityLob_lob10: " + getEntityLob_lob10() +
                " entityLob_lob11: " + getEntityLob_lob11() +
                " entityLob_lob12: " + getEntityLob_lob12());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntityLob_id() {
        return entityLob_id;
    }

    public void setEntityLob_id(int p) {
        this.entityLob_id = p;
    }

    public int getEntityLob_version() {
        return entityLob_version;
    }

    public void setEntityLob_version(int p) {
        this.entityLob_version = p;
    }

    public Byte[] getEntityLob_lob01() {
        return entityLob_lob01;
    }

    public void setEntityLob_lob01(Byte[] p) {
        this.entityLob_lob01 = p;
    }

    public byte[] getEntityLob_lob02() {
        return entityLob_lob02;
    }

    public void setEntityLob_lob02(byte[] p) {
        this.entityLob_lob02 = p;
    }

    public String getEntityLob_lob03() {
        return entityLob_lob03;
    }

    public void setEntityLob_lob03(String p) {
        this.entityLob_lob03 = p;
    }

    public String[] getEntityLob_lob04() {
        return entityLob_lob04;
    }

    public void setEntityLob_lob04(String[] p) {
        this.entityLob_lob04 = p;
    }

    public Object getEntityLob_lob05() {
        return entityLob_lob05;
    }

    public void setEntityLob_lob05(Object p) {
        this.entityLob_lob05 = p;
    }

    public Object[] getEntityLob_lob06() {
        return entityLob_lob06;
    }

    public void setEntityLob_lob06(Object[] p) {
        this.entityLob_lob06 = p;
    }

    public List<String> getEntityLob_lob07() {
        return entityLob_lob07;
    }

    public void setEntityLob_lob07(List<String> l) {
        this.entityLob_lob07 = l;
    }

    public List<String>[] getEntityLob_lob08() {
        return entityLob_lob08;
    }

    public void setEntityLob_lob08(List<String>[] l) {
        this.entityLob_lob08 = l;
    }

    public char[] getEntityLob_lob09() {
        return entityLob_lob09;
    }

    public void setEntityLob_lob09(char[] p) {
        this.entityLob_lob09 = p;
    }

    public Character[] getEntityLob_lob10() {
        return entityLob_lob10;
    }

    public void setEntityLob_lob10(Character[] p) {
        this.entityLob_lob10 = p;
    }

    public Serializable getEntityLob_lob11() {
        return entityLob_lob11;
    }

    public void setEntityLob_lob11(Serializable p) {
        this.entityLob_lob11 = p;
    }

    public Serializable[] getEntityLob_lob12() {
        return entityLob_lob12;
    }

    public void setEntityLob_lob12(Serializable[] p) {
        this.entityLob_lob12 = p;
    }
}
