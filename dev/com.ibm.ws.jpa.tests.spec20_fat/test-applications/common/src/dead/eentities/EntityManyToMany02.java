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

package dead.eentities;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * ppp>Entity of the Common Datamodel (which uses all the possible JPA 2.0 Annotations as described in the
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
 * <li><b>@ManyToMany</b> (29 of 65):
 * <p>Defines a many-valued association with many-to-many multiplicity.
 * <p>@ManyToMany combinations:
 * <ul>
 * <li>@ManyToMany on field
 * <li>@ManyToMany on property
 * <li>cascade
 * <li>fetch
 * <li>mappedBy
 * <li>targetEntity
 * </ul>
 * <p>@ManyToMany combinations exercised in this entity:
 * <ul>
 * <li>@ManyToMany on field
 * <li>cascade
 * <li>fetch
 * <li>mappedBy
 * <li>targetEntity
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
 *
 *
 * <p><b>UML:</b>
 *
 * <pre>
 *
 *          +----------------------+                +----------------------+                +----------------------+
 *          |  EntityManyToMany02  |<-------------->|  EntityManyToMany02  |<-------------->|  EntityManyToMany01  |
 *          +----------------------+  n          n  +----------------------+  n          n  +----------------------+
 *
 * </pre>
 */
@Access(AccessType.FIELD)
//@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "CDM_EntityManyToMany02")
//     catalog = "CDM_EntityManyToMany02_Catalog",
//     schema = "CMD_EntityManyToMany02_Schema",
//     uniqueConstraints=@UniqueConstraint(columnNames={"entity01_string01"}))
public class EntityManyToMany02 {

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityManyToMany02_id",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityManyToMany02",
            updatable = true)
    @Id
    private int entityManyToMany02_id;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityManyToMany02_version",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityManyToMany02",
            updatable = true)
    @Version
    private int entityManyToMany02_version;

    //----------------------------------------------------------------------------------------------
    // @ManyToMany combinations
    //----------------------------------------------------------------------------------------------
    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.LAZY)
    private Set entityManyToMany02_set01;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_set02",
//              targetEntity = EntityManyToMany02.class)
    private Set entityManyToMany02_set02;

    @ManyToMany(cascade = CascadeType.MERGE,
                fetch = FetchType.LAZY)
//              targetEntity = EntityManyToMany01.class)
    private Set entityManyToMany02_set03;

    @ManyToMany(cascade = CascadeType.PERSIST,
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_set04")
    private Set<EntityManyToMany01> entityManyToMany02_set04;

    @ManyToMany(cascade = CascadeType.REFRESH,
                fetch = FetchType.LAZY)
    private Set<EntityManyToMany01> entityManyToMany02_set05;

    @ManyToMany(cascade = CascadeType.REMOVE,
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_set07")
    private Set<EntityManyToMany02> entityManyToMany02_set06;

    @ManyToMany(cascade = { CascadeType.ALL, CascadeType.DETACH },
                fetch = FetchType.LAZY)
    private Set<EntityManyToMany02> entityManyToMany02_set07;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE },
                fetch = FetchType.EAGER)
    private List entityManyToMany02_list01;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },
                fetch = FetchType.LAZY)
//              mappedBy = "entityManyToMany01_list02",
//              targetEntity = EntityManyToMany02.class)
    private List entityManyToMany02_list02;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.REFRESH },
                fetch = FetchType.EAGER)
//              targetEntity = EntityManyToMany01.class)
    private List entityManyToMany02_list03;

    @ManyToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE },
                fetch = FetchType.LAZY)
//              mappedBy = "entityManyToMany01_list04")
    private List<EntityManyToMany01> entityManyToMany02_list04;

    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.EAGER)
    private List<EntityManyToMany01> entityManyToMany02_list05;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.LAZY)
//              mappedBy = "entityManyToMany01_list06")
    private List<EntityManyToMany02> entityManyToMany02_list06;

    @ManyToMany(cascade = CascadeType.MERGE,
                fetch = FetchType.EAGER)
    private List<EntityManyToMany02> entityManyToMany02_list07;

    @ManyToMany(cascade = CascadeType.PERSIST,
                fetch = FetchType.LAZY)
    private Collection entityManyToMany02_collection01;

    @ManyToMany(cascade = CascadeType.REFRESH,
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_collection02",
//              targetEntity = EntityManyToMany02.class)
    private Collection entityManyToMany02_collection02;

    @ManyToMany(cascade = CascadeType.REMOVE,
                fetch = FetchType.LAZY)
//              targetEntity = EntityManyToMany01.class)
    private Collection entityManyToMany02_collection03;

    @ManyToMany(cascade = { CascadeType.ALL, CascadeType.DETACH },
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_collection04")
    private Collection<EntityManyToMany01> entityManyToMany02_collection04;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE },
                fetch = FetchType.LAZY)
    private Collection<EntityManyToMany01> entityManyToMany02_collection05;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_collection06")
    private Collection<EntityManyToMany02> entityManyToMany02_collection06;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.REFRESH },
                fetch = FetchType.LAZY)
    private Collection<EntityManyToMany02> entityManyToMany02_collection07;

    @ManyToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE },
                fetch = FetchType.EAGER)
    private HashSet entityManyToMany02_hashset01;

    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.LAZY)
//              mappedBy = "entityManyToMany01_hashSet02",
//              targetEntity = EntityManyToMany02.class)
    private HashSet entityManyToMany02_hashset02;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.EAGER)
//              targetEntity = EntityManyToMany01.class)
    private HashSet entityManyToMany02_hashset03;

    @ManyToMany(cascade = CascadeType.MERGE,
                fetch = FetchType.LAZY)
//              mappedBy = "entityManyToMany01_hashSet04")
    private HashSet<EntityManyToMany01> entityManyToMany02_hashset04;

    @ManyToMany(cascade = CascadeType.PERSIST,
                fetch = FetchType.EAGER)
    private HashSet<EntityManyToMany01> entityManyToMany02_hashset05;

    @ManyToMany(cascade = CascadeType.REFRESH,
                fetch = FetchType.LAZY)
//              mappedBy = "entityManyToMany01_hashSet06")
    private HashSet<EntityManyToMany02> entityManyToMany02_hashset06;

    @ManyToMany(cascade = CascadeType.REMOVE,
                fetch = FetchType.EAGER)
    private HashSet<EntityManyToMany02> entityManyToMany02_hashset07;

    @ManyToMany(cascade = { CascadeType.ALL, CascadeType.DETACH },
                fetch = FetchType.LAZY)
    private Map entityManyToMany02_map01;

    @ManyToMany(cascade = { CascadeType.DETACH, CascadeType.MERGE },
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_map02",
//              targetEntity = EntityManyToMany02.class)
    private Map entityManyToMany02_map02;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },
                fetch = FetchType.LAZY)
//              targetEntity = EntityManyToMany01.class)
    private Map entityManyToMany02_map03;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.REFRESH },
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_map04")
    private Map<EntityManyToMany01, EntityManyToMany01> entityManyToMany02_map04;

    @ManyToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE },
                fetch = FetchType.LAZY)
    private Map<EntityManyToMany01, EntityManyToMany01> entityManyToMany02_map05;

    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.EAGER)
//              mappedBy = "entityManyToMany01_map06")
    private Map<EntityManyToMany02, EntityManyToMany02> entityManyToMany02_map06;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.LAZY)
    private Map<EntityManyToMany02, EntityManyToMany02> entityManyToMany02_map07;

    public EntityManyToMany02() {}

    public EntityManyToMany02(int id,
                              int version,
                              Set set01,
                              Set set02,
                              Set set03,
                              Set<EntityManyToMany01> set04,
                              Set<EntityManyToMany01> set05,
                              Set<EntityManyToMany02> set06,
                              Set<EntityManyToMany02> set07,
                              List list01,
                              List list02,
                              List list03,
                              List<EntityManyToMany01> list04,
                              List<EntityManyToMany01> list05,
                              List<EntityManyToMany02> list06,
                              List<EntityManyToMany02> list07,
                              Collection collection01,
                              Collection collection02,
                              Collection collection03,
                              Collection<EntityManyToMany01> collection04,
                              Collection<EntityManyToMany01> collection05,
                              Collection<EntityManyToMany02> collection06,
                              Collection<EntityManyToMany02> collection07,
                              HashSet hashset01,
                              HashSet hashset02,
                              HashSet hashset03,
                              HashSet<EntityManyToMany01> hashset04,
                              HashSet<EntityManyToMany01> hashset05,
                              HashSet<EntityManyToMany02> hashset06,
                              HashSet<EntityManyToMany02> hashset07,
                              Map map01,
                              Map map02,
                              Map map03,
                              Map<EntityManyToMany01, EntityManyToMany01> map04,
                              Map<EntityManyToMany01, EntityManyToMany01> map05,
                              Map<EntityManyToMany02, EntityManyToMany02> map06,
                              Map<EntityManyToMany02, EntityManyToMany02> map07) {
        this.entityManyToMany02_id = id;
        this.entityManyToMany02_version = version;
        this.entityManyToMany02_set01 = set01;
        this.entityManyToMany02_set02 = set02;
        this.entityManyToMany02_set03 = set03;
        this.entityManyToMany02_set04 = set04;
        this.entityManyToMany02_set05 = set05;
        this.entityManyToMany02_set06 = set06;
        this.entityManyToMany02_set07 = set07;
        this.entityManyToMany02_list01 = list01;
        this.entityManyToMany02_list02 = list02;
        this.entityManyToMany02_list03 = list03;
        this.entityManyToMany02_list04 = list04;
        this.entityManyToMany02_list05 = list05;
        this.entityManyToMany02_list06 = list06;
        this.entityManyToMany02_list07 = list07;
        this.entityManyToMany02_collection01 = collection01;
        this.entityManyToMany02_collection02 = collection02;
        this.entityManyToMany02_collection03 = collection03;
        this.entityManyToMany02_collection04 = collection04;
        this.entityManyToMany02_collection05 = collection05;
        this.entityManyToMany02_collection06 = collection06;
        this.entityManyToMany02_collection07 = collection07;
        this.entityManyToMany02_hashset01 = hashset01;
        this.entityManyToMany02_hashset02 = hashset02;
        this.entityManyToMany02_hashset03 = hashset03;
        this.entityManyToMany02_hashset04 = hashset04;
        this.entityManyToMany02_hashset05 = hashset05;
        this.entityManyToMany02_hashset06 = hashset06;
        this.entityManyToMany02_hashset07 = hashset07;
        this.entityManyToMany02_map01 = map01;
        this.entityManyToMany02_map02 = map02;
        this.entityManyToMany02_map03 = map03;
        this.entityManyToMany02_map04 = map04;
        this.entityManyToMany02_map05 = map05;
        this.entityManyToMany02_map06 = map06;
        this.entityManyToMany02_map07 = map07;
    }

    @Override
    public String toString() {
        return (" EntityManyToMany02: " +
                " entityManyToMany02_id: " + getEntityManyToMany02_id() +
                " entityManyToMany02_version: " + getEntityManyToMany02_version() +
                " entityManyToMany02_set01: " + getEntityManyToMany02_set01() +
                " entityManyToMany02_set02: " + getEntityManyToMany02_set02() +
                " entityManyToMany02_set03: " + getEntityManyToMany02_set03() +
                " entityManyToMany02_set04: " + getEntityManyToMany02_set04() +
                " entityManyToMany02_set05: " + getEntityManyToMany02_set05() +
                " entityManyToMany02_set06: " + getEntityManyToMany02_set06() +
                " entityManyToMany02_set07: " + getEntityManyToMany02_set07() +
                " entityManyToMany02_list01: " + getEntityManyToMany02_list01() +
                " entityManyToMany02_list02: " + getEntityManyToMany02_list02() +
                " entityManyToMany02_list03: " + getEntityManyToMany02_list03() +
                " entityManyToMany02_list04: " + getEntityManyToMany02_list04() +
                " entityManyToMany02_list05: " + getEntityManyToMany02_list05() +
                " entityManyToMany02_list06: " + getEntityManyToMany02_list06() +
                " entityManyToMany02_list07: " + getEntityManyToMany02_list07() +
                " entityManyToMany02_collection01: " + getEntityManyToMany02_collection01() +
                " entityManyToMany02_collection02: " + getEntityManyToMany02_collection02() +
                " entityManyToMany02_collection03: " + getEntityManyToMany02_collection03() +
                " entityManyToMany02_collection04: " + getEntityManyToMany02_collection04() +
                " entityManyToMany02_collection05: " + getEntityManyToMany02_collection05() +
                " entityManyToMany02_collection06: " + getEntityManyToMany02_collection06() +
                " entityManyToMany02_collection07: " + getEntityManyToMany02_collection07() +
                " entityManyToMany02_hashset01: " + getEntityManyToMany02_hashset01() +
                " entityManyToMany02_hashset02: " + getEntityManyToMany02_hashset02() +
                " entityManyToMany02_hashset03: " + getEntityManyToMany02_hashset03() +
                " entityManyToMany02_hashset04: " + getEntityManyToMany02_hashset04() +
                " entityManyToMany02_hashset05: " + getEntityManyToMany02_hashset05() +
                " entityManyToMany02_hashset06: " + getEntityManyToMany02_hashset06() +
                " entityManyToMany02_hashset07: " + getEntityManyToMany02_hashset07() +
                " entityManyToMany02_map01: " + getEntityManyToMany02_map01() +
                " entityManyToMany02_map02: " + getEntityManyToMany02_map02() +
                " entityManyToMany02_map03: " + getEntityManyToMany02_map03() +
                " entityManyToMany02_map04: " + getEntityManyToMany02_map04() +
                " entityManyToMany02_map05: " + getEntityManyToMany02_map05() +
                " entityManyToMany02_map06: " + getEntityManyToMany02_map05() +
                " entityManyToMany02_map07: " + getEntityManyToMany02_map07());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntityManyToMany02_id() {
        return entityManyToMany02_id;
    }

    public void setEntityManyToMany02_id(int p) {
        this.entityManyToMany02_id = p;
    }

    public int getEntityManyToMany02_version() {
        return entityManyToMany02_version;
    }

    public void setEntityManyToMany02_version(int p) {
        this.entityManyToMany02_version = p;
    }

    public Set getEntityManyToMany02_set01() {
        return entityManyToMany02_set01;
    }

    public void setEntityManyToMany02_set01(Set p) {
        this.entityManyToMany02_set01 = p;
    }

    public Set getEntityManyToMany02_set02() {
        return entityManyToMany02_set01;
    }

    public void setEntityManyToMany02_set02(Set p) {
        this.entityManyToMany02_set01 = p;
    }

    public Set getEntityManyToMany02_set03() {
        return entityManyToMany02_set01;
    }

    public void setEntityManyToMany02_set03(Set p) {
        this.entityManyToMany02_set01 = p;
    }

    public Set<EntityManyToMany01> getEntityManyToMany02_set04() {
        return entityManyToMany02_set04;
    }

    public void setEntityManyToMany02_set04(Set<EntityManyToMany01> p) {
        this.entityManyToMany02_set04 = p;
    }

    public Set<EntityManyToMany01> getEntityManyToMany02_set05() {
        return entityManyToMany02_set05;
    }

    public void setEntityManyToMany02_set05(Set<EntityManyToMany01> p) {
        this.entityManyToMany02_set05 = p;
    }

    public Set<EntityManyToMany02> getEntityManyToMany02_set06() {
        return entityManyToMany02_set06;
    }

    public void setEntityManyToMany02_set06(Set<EntityManyToMany02> p) {
        this.entityManyToMany02_set06 = p;
    }

    public Set<EntityManyToMany02> getEntityManyToMany02_set07() {
        return entityManyToMany02_set07;
    }

    public void setEntityManyToMany02_set07(Set<EntityManyToMany02> p) {
        this.entityManyToMany02_set07 = p;
    }

    public List getEntityManyToMany02_list01() {
        return entityManyToMany02_list01;
    }

    public void setEntityManyToMany02_list01(List p) {
        this.entityManyToMany02_list01 = p;
    }

    public List getEntityManyToMany02_list02() {
        return entityManyToMany02_list02;
    }

    public void setEntityManyToMany02_list02(List p) {
        this.entityManyToMany02_list02 = p;
    }

    public List getEntityManyToMany02_list03() {
        return entityManyToMany02_list03;
    }

    public void setEntityManyToMany02_list03(List p) {
        this.entityManyToMany02_list03 = p;
    }

    public List<EntityManyToMany01> getEntityManyToMany02_list04() {
        return entityManyToMany02_list04;
    }

    public void setEntityManyToMany02_list04(List<EntityManyToMany01> p) {
        this.entityManyToMany02_list04 = p;
    }

    public List<EntityManyToMany01> getEntityManyToMany02_list05() {
        return entityManyToMany02_list05;
    }

    public void setEntityManyToMany02_list05(List<EntityManyToMany01> p) {
        this.entityManyToMany02_list05 = p;
    }

    public List<EntityManyToMany02> getEntityManyToMany02_list06() {
        return entityManyToMany02_list06;
    }

    public void setEntityManyToMany02_list06(List<EntityManyToMany02> p) {
        this.entityManyToMany02_list06 = p;
    }

    public List<EntityManyToMany02> getEntityManyToMany02_list07() {
        return entityManyToMany02_list07;
    }

    public void setEntityManyToMany02_list07(List<EntityManyToMany02> p) {
        this.entityManyToMany02_list07 = p;
    }

    public Collection getEntityManyToMany02_collection01() {
        return entityManyToMany02_collection01;
    }

    public void setEntityManyToMany02_collection01(Collection p) {
        this.entityManyToMany02_collection01 = p;
    }

    public Collection getEntityManyToMany02_collection02() {
        return entityManyToMany02_collection02;
    }

    public void setEntityManyToMany02_collection02(Collection p) {
        this.entityManyToMany02_collection02 = p;
    }

    public Collection getEntityManyToMany02_collection03() {
        return entityManyToMany02_collection03;
    }

    public void setEntityManyToMany02_collection03(Collection p) {
        this.entityManyToMany02_collection03 = p;
    }

    public Collection<EntityManyToMany01> getEntityManyToMany02_collection04() {
        return entityManyToMany02_collection04;
    }

    public void setEntityManyToMany02_collection04(Collection<EntityManyToMany01> p) {
        this.entityManyToMany02_collection04 = p;
    }

    public Collection<EntityManyToMany01> getEntityManyToMany02_collection05() {
        return entityManyToMany02_collection05;
    }

    public void setEntityManyToMany02_collection05(Collection<EntityManyToMany01> p) {
        this.entityManyToMany02_collection05 = p;
    }

    public Collection<EntityManyToMany02> getEntityManyToMany02_collection06() {
        return entityManyToMany02_collection06;
    }

    public void setEntityManyToMany02_collection06(Collection<EntityManyToMany02> p) {
        this.entityManyToMany02_collection06 = p;
    }

    public Collection<EntityManyToMany02> getEntityManyToMany02_collection07() {
        return entityManyToMany02_collection07;
    }

    public void setEntityManyToMany02_collection07(Collection<EntityManyToMany02> p) {
        this.entityManyToMany02_collection07 = p;
    }

    public HashSet getEntityManyToMany02_hashset01() {
        return entityManyToMany02_hashset01;
    }

    public void setEntityManyToMany02_hashset01(HashSet p) {
        this.entityManyToMany02_hashset01 = p;
    }

    public HashSet getEntityManyToMany02_hashset02() {
        return entityManyToMany02_hashset02;
    }

    public void setEntityManyToMany02_hashset02(HashSet p) {
        this.entityManyToMany02_hashset02 = p;
    }

    public HashSet getEntityManyToMany02_hashset03() {
        return entityManyToMany02_hashset03;
    }

    public void setEntityManyToMany02_hashset03(HashSet p) {
        this.entityManyToMany02_hashset03 = p;
    }

    public HashSet<EntityManyToMany01> getEntityManyToMany02_hashset04() {
        return entityManyToMany02_hashset04;
    }

    public void setEntityManyToMany02_hashset04(HashSet<EntityManyToMany01> p) {
        this.entityManyToMany02_hashset04 = p;
    }

    public HashSet<EntityManyToMany01> getEntityManyToMany02_hashset05() {
        return entityManyToMany02_hashset05;
    }

    public void setEntityManyToMany02_hashset05(HashSet<EntityManyToMany01> p) {
        this.entityManyToMany02_hashset05 = p;
    }

    public HashSet<EntityManyToMany02> getEntityManyToMany02_hashset06() {
        return entityManyToMany02_hashset06;
    }

    public void setEntityManyToMany02_hashset06(HashSet<EntityManyToMany02> p) {
        this.entityManyToMany02_hashset06 = p;
    }

    public HashSet<EntityManyToMany02> getEntityManyToMany02_hashset07() {
        return entityManyToMany02_hashset07;
    }

    public void setEntityManyToMany02_hashset07(HashSet<EntityManyToMany02> p) {
        this.entityManyToMany02_hashset07 = p;
    }

    public Map getEntityManyToMany02_map01() {
        return entityManyToMany02_map01;
    }

    public void setEntityManyToMany02_map01(Map p) {
        this.entityManyToMany02_map01 = p;
    }

    public Map getEntityManyToMany02_map02() {
        return entityManyToMany02_map02;
    }

    public void setEntityManyToMany02_map02(Map p) {
        this.entityManyToMany02_map02 = p;
    }

    public Map getEntityManyToMany02_map03() {
        return entityManyToMany02_map03;
    }

    public void setEntityManyToMany02_map03(Map p) {
        this.entityManyToMany02_map03 = p;
    }

    public Map<EntityManyToMany01, EntityManyToMany01> getEntityManyToMany02_map04() {
        return entityManyToMany02_map04;
    }

    public void setEntityManyToMany02_map04(Map<EntityManyToMany01, EntityManyToMany01> p) {
        this.entityManyToMany02_map04 = p;
    }

    public Map<EntityManyToMany01, EntityManyToMany01> getEntityManyToMany02_map05() {
        return entityManyToMany02_map05;
    }

    public void setEntityManyToMany02_map05(Map<EntityManyToMany01, EntityManyToMany01> p) {
        this.entityManyToMany02_map05 = p;
    }

    public Map<EntityManyToMany02, EntityManyToMany02> getEntityManyToMany02_map06() {
        return entityManyToMany02_map06;
    }

    public void setEntityManyToMany02_map06(Map<EntityManyToMany02, EntityManyToMany02> p) {
        this.entityManyToMany02_map06 = p;
    }

    public Map<EntityManyToMany02, EntityManyToMany02> getEntityManyToMany02_map07() {
        return entityManyToMany02_map07;
    }

    public void setEntityManyToMany02_map07(Map<EntityManyToMany02, EntityManyToMany02> p) {
        this.entityManyToMany02_map07 = p;
    }
}
