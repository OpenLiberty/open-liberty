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
 *          |  EntityManyToMany01  |<-------------->|  EntityManyToMany01  |<-------------->|  EntityManyToMany02  |
 *          +----------------------+  n          n  +----------------------+  n          n  +----------------------+
 *
 * </pre>
 */
@Access(AccessType.FIELD)
//@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "CDM_EntityManyToMany01")
public class EntityManyToMany01 {

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityManyToMany01_id",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityManyToMany01",
            updatable = true)
    @Id
    private int entityManyToMany01_id;

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityManyToMany01_version",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityManyToMany01",
            updatable = true)
    @Version
    private int entityManyToMany01_version;

    //----------------------------------------------------------------------------------------------
    // @ManyToMany combinations
    //----------------------------------------------------------------------------------------------
    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.EAGER)
    private Set entityManyToMany01_set01;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.LAZY)
    private Set entityManyToMany01_set02;

    @ManyToMany(cascade = CascadeType.MERGE,
                fetch = FetchType.EAGER)
    private Set entityManyToMany01_set03;

    @ManyToMany(cascade = CascadeType.PERSIST,
                fetch = FetchType.LAZY)
    private Set<EntityManyToMany01> entityManyToMany01_set04;

    @ManyToMany(cascade = CascadeType.REFRESH,
                fetch = FetchType.EAGER)
    private Set<EntityManyToMany01> entityManyToMany01_set05;

    @ManyToMany(cascade = { CascadeType.ALL, CascadeType.DETACH },
                fetch = FetchType.LAZY)
    private Set<EntityManyToMany02> entityManyToMany01_set06;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },
                fetch = FetchType.EAGER)
    private Set<EntityManyToMany02> entityManyToMany01_set07;

    @ManyToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE },
                fetch = FetchType.LAZY)
    private List entityManyToMany01_list01;

    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.EAGER)
    private List entityManyToMany01_list02;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.LAZY)
    private List entityManyToMany01_list03;

    @ManyToMany(cascade = CascadeType.MERGE,
                fetch = FetchType.EAGER)
    private List<EntityManyToMany01> entityManyToMany01_list04;

    @ManyToMany(cascade = CascadeType.PERSIST,
                fetch = FetchType.LAZY)
    private List<EntityManyToMany01> entityManyToMany01_list05;

    @ManyToMany(cascade = CascadeType.REFRESH,
                fetch = FetchType.EAGER)
    private List<EntityManyToMany02> entityManyToMany01_list06;

    @ManyToMany(cascade = CascadeType.REMOVE,
                fetch = FetchType.LAZY)
    private List<EntityManyToMany02> entityManyToMany01_list07;

    @ManyToMany(cascade = { CascadeType.ALL, CascadeType.DETACH },
                fetch = FetchType.EAGER)
    private Collection entityManyToMany01_collection01;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },
                fetch = FetchType.LAZY)
    private Collection entityManyToMany01_collection02;

    @ManyToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE },
                fetch = FetchType.EAGER)
    private Collection entityManyToMany01_collection03;

    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.LAZY)
    private Collection<EntityManyToMany01> entityManyToMany01_collection04;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.EAGER)
    private Collection<EntityManyToMany01> entityManyToMany01_collection05;

    @ManyToMany(cascade = CascadeType.MERGE,
                fetch = FetchType.LAZY)
    private Collection<EntityManyToMany02> entityManyToMany01_collection06;

    @ManyToMany(cascade = CascadeType.PERSIST,
                fetch = FetchType.EAGER)
    private Collection<EntityManyToMany02> entityManyToMany01_collection07;

    @ManyToMany(cascade = CascadeType.REFRESH,
                fetch = FetchType.LAZY)
    private HashSet entityManyToMany01_hashset01;

    @ManyToMany(cascade = CascadeType.REMOVE,
                fetch = FetchType.EAGER)
    private HashSet entityManyToMany01_hashset02;

    @ManyToMany(cascade = { CascadeType.ALL, CascadeType.DETACH },
                fetch = FetchType.LAZY)
    private HashSet entityManyToMany01_hashset03;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },
                fetch = FetchType.EAGER)
    private HashSet<EntityManyToMany01> entityManyToMany01_hashset04;

    @ManyToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE },
                fetch = FetchType.LAZY)
    private HashSet<EntityManyToMany01> entityManyToMany01_hashset05;

    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.EAGER)
    private HashSet<EntityManyToMany02> entityManyToMany01_hashset06;

    @ManyToMany(cascade = CascadeType.DETACH,
                fetch = FetchType.LAZY)
    private HashSet<EntityManyToMany02> entityManyToMany01_hashset07;

    @ManyToMany(cascade = CascadeType.MERGE,
                fetch = FetchType.EAGER)
    private Map entityManyToMany01_map01;

    @ManyToMany(cascade = CascadeType.PERSIST,
                fetch = FetchType.LAZY)
    private Map entityManyToMany01_map02;

    @ManyToMany(cascade = CascadeType.REFRESH,
                fetch = FetchType.EAGER)
    private Map entityManyToMany01_map03;

    @ManyToMany(cascade = CascadeType.REMOVE,
                fetch = FetchType.LAZY)
    private Map<EntityManyToMany01, EntityManyToMany01> entityManyToMany01_map04;

    @ManyToMany(cascade = { CascadeType.ALL, CascadeType.DETACH },
                fetch = FetchType.EAGER)
    private Map<EntityManyToMany01, EntityManyToMany01> entityManyToMany01_map05;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },
                fetch = FetchType.LAZY)
    private Map<EntityManyToMany02, EntityManyToMany02> entityManyToMany01_map06;

    @ManyToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE },
                fetch = FetchType.EAGER)
    private Map<EntityManyToMany02, EntityManyToMany02> entityManyToMany01_map07;

    public EntityManyToMany01() {}

    public EntityManyToMany01(int id,
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
        this.entityManyToMany01_id = id;
        this.entityManyToMany01_version = version;
        this.entityManyToMany01_set01 = set01;
        this.entityManyToMany01_set02 = set02;
        this.entityManyToMany01_set03 = set03;
        this.entityManyToMany01_set04 = set04;
        this.entityManyToMany01_set05 = set05;
        this.entityManyToMany01_set06 = set06;
        this.entityManyToMany01_set07 = set07;
        this.entityManyToMany01_list01 = list01;
        this.entityManyToMany01_list02 = list02;
        this.entityManyToMany01_list03 = list03;
        this.entityManyToMany01_list04 = list04;
        this.entityManyToMany01_list05 = list05;
        this.entityManyToMany01_list06 = list06;
        this.entityManyToMany01_list07 = list07;
        this.entityManyToMany01_collection01 = collection01;
        this.entityManyToMany01_collection02 = collection02;
        this.entityManyToMany01_collection03 = collection03;
        this.entityManyToMany01_collection04 = collection04;
        this.entityManyToMany01_collection05 = collection05;
        this.entityManyToMany01_collection06 = collection06;
        this.entityManyToMany01_collection07 = collection07;
        this.entityManyToMany01_hashset01 = hashset01;
        this.entityManyToMany01_hashset02 = hashset02;
        this.entityManyToMany01_hashset03 = hashset03;
        this.entityManyToMany01_hashset04 = hashset04;
        this.entityManyToMany01_hashset05 = hashset05;
        this.entityManyToMany01_hashset06 = hashset06;
        this.entityManyToMany01_hashset07 = hashset07;
        this.entityManyToMany01_map01 = map01;
        this.entityManyToMany01_map02 = map02;
        this.entityManyToMany01_map03 = map03;
        this.entityManyToMany01_map04 = map04;
        this.entityManyToMany01_map05 = map05;
        this.entityManyToMany01_map06 = map06;
        this.entityManyToMany01_map07 = map07;
    }

    @Override
    public String toString() {
        return (" EntityManyToMany01: " +
                " entityManyToMany01_id: " + getEntityManyToMany01_id() +
                " entityManyToMany01_version: " + getEntityManyToMany01_version() +
                " entityManyToMany01_set01: " + getEntityManyToMany01_set01() +
                " entityManyToMany01_set02: " + getEntityManyToMany01_set02() +
                " entityManyToMany01_set03: " + getEntityManyToMany01_set03() +
                " entityManyToMany01_set04: " + getEntityManyToMany01_set04() +
                " entityManyToMany01_set05: " + getEntityManyToMany01_set05() +
                " entityManyToMany01_set06: " + getEntityManyToMany01_set06() +
                " entityManyToMany01_set07: " + getEntityManyToMany01_set07() +
                " entityManyToMany01_list01: " + getEntityManyToMany01_list01() +
                " entityManyToMany01_list02: " + getEntityManyToMany01_list02() +
                " entityManyToMany01_list03: " + getEntityManyToMany01_list03() +
                " entityManyToMany01_list04: " + getEntityManyToMany01_list04() +
                " entityManyToMany01_list05: " + getEntityManyToMany01_list05() +
                " entityManyToMany01_list06: " + getEntityManyToMany01_list06() +
                " entityManyToMany01_list07: " + getEntityManyToMany01_list07() +
                " entityManyToMany01_collection01: " + getEntityManyToMany01_collection01() +
                " entityManyToMany01_collection02: " + getEntityManyToMany01_collection02() +
                " entityManyToMany01_collection03: " + getEntityManyToMany01_collection03() +
                " entityManyToMany01_collection04: " + getEntityManyToMany01_collection04() +
                " entityManyToMany01_collection05: " + getEntityManyToMany01_collection05() +
                " entityManyToMany01_collection06: " + getEntityManyToMany01_collection06() +
                " entityManyToMany01_collection07: " + getEntityManyToMany01_collection07() +
                " entityManyToMany01_hashset01: " + getEntityManyToMany01_hashset01() +
                " entityManyToMany01_hashset02: " + getEntityManyToMany01_hashset02() +
                " entityManyToMany01_hashset03: " + getEntityManyToMany01_hashset03() +
                " entityManyToMany01_hashset04: " + getEntityManyToMany01_hashset04() +
                " entityManyToMany01_hashset05: " + getEntityManyToMany01_hashset05() +
                " entityManyToMany01_hashset06: " + getEntityManyToMany01_hashset06() +
                " entityManyToMany01_hashset07: " + getEntityManyToMany01_hashset07() +
                " entityManyToMany01_map01: " + getEntityManyToMany01_map01() +
                " entityManyToMany01_map02: " + getEntityManyToMany01_map02() +
                " entityManyToMany01_map03: " + getEntityManyToMany01_map03() +
                " entityManyToMany01_map04: " + getEntityManyToMany01_map04() +
                " entityManyToMany01_map05: " + getEntityManyToMany01_map05() +
                " entityManyToMany01_map06: " + getEntityManyToMany01_map05() +
                " entityManyToMany01_map07: " + getEntityManyToMany01_map07());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntityManyToMany01_id() {
        return entityManyToMany01_id;
    }

    public void setEntityManyToMany01_id(int p) {
        this.entityManyToMany01_id = p;
    }

    public int getEntityManyToMany01_version() {
        return entityManyToMany01_version;
    }

    public void setEntityManyToMany01_version(int p) {
        this.entityManyToMany01_version = p;
    }

    public Set getEntityManyToMany01_set01() {
        return entityManyToMany01_set01;
    }

    public void setEntityManyToMany01_set01(Set p) {
        this.entityManyToMany01_set01 = p;
    }

    public Set getEntityManyToMany01_set02() {
        return entityManyToMany01_set01;
    }

    public void setEntityManyToMany01_set02(Set p) {
        this.entityManyToMany01_set01 = p;
    }

    public Set getEntityManyToMany01_set03() {
        return entityManyToMany01_set01;
    }

    public void setEntityManyToMany01_set03(Set p) {
        this.entityManyToMany01_set01 = p;
    }

    public Set<EntityManyToMany01> getEntityManyToMany01_set04() {
        return entityManyToMany01_set04;
    }

    public void setEntityManyToMany01_set04(Set<EntityManyToMany01> p) {
        this.entityManyToMany01_set04 = p;
    }

    public Set<EntityManyToMany01> getEntityManyToMany01_set05() {
        return entityManyToMany01_set05;
    }

    public void setEntityManyToMany01_set05(Set<EntityManyToMany01> p) {
        this.entityManyToMany01_set05 = p;
    }

    public Set<EntityManyToMany02> getEntityManyToMany01_set06() {
        return entityManyToMany01_set06;
    }

    public void setEntityManyToMany01_set06(Set<EntityManyToMany02> p) {
        this.entityManyToMany01_set06 = p;
    }

    public Set<EntityManyToMany02> getEntityManyToMany01_set07() {
        return entityManyToMany01_set07;
    }

    public void setEntityManyToMany01_set07(Set<EntityManyToMany02> p) {
        this.entityManyToMany01_set07 = p;
    }

    public List getEntityManyToMany01_list01() {
        return entityManyToMany01_list01;
    }

    public void setEntityManyToMany01_list01(List p) {
        this.entityManyToMany01_list01 = p;
    }

    public List getEntityManyToMany01_list02() {
        return entityManyToMany01_list02;
    }

    public void setEntityManyToMany01_list02(List p) {
        this.entityManyToMany01_list02 = p;
    }

    public List getEntityManyToMany01_list03() {
        return entityManyToMany01_list03;
    }

    public void setEntityManyToMany01_list03(List p) {
        this.entityManyToMany01_list03 = p;
    }

    public List<EntityManyToMany01> getEntityManyToMany01_list04() {
        return entityManyToMany01_list04;
    }

    public void setEntityManyToMany01_list04(List<EntityManyToMany01> p) {
        this.entityManyToMany01_list04 = p;
    }

    public List<EntityManyToMany01> getEntityManyToMany01_list05() {
        return entityManyToMany01_list05;
    }

    public void setEntityManyToMany01_list05(List<EntityManyToMany01> p) {
        this.entityManyToMany01_list05 = p;
    }

    public List<EntityManyToMany02> getEntityManyToMany01_list06() {
        return entityManyToMany01_list06;
    }

    public void setEntityManyToMany01_list06(List<EntityManyToMany02> p) {
        this.entityManyToMany01_list06 = p;
    }

    public List<EntityManyToMany02> getEntityManyToMany01_list07() {
        return entityManyToMany01_list07;
    }

    public void setEntityManyToMany01_list07(List<EntityManyToMany02> p) {
        this.entityManyToMany01_list07 = p;
    }

    public Collection getEntityManyToMany01_collection01() {
        return entityManyToMany01_collection01;
    }

    public void setEntityManyToMany01_collection01(Collection p) {
        this.entityManyToMany01_collection01 = p;
    }

    public Collection getEntityManyToMany01_collection02() {
        return entityManyToMany01_collection02;
    }

    public void setEntityManyToMany01_collection02(Collection p) {
        this.entityManyToMany01_collection02 = p;
    }

    public Collection getEntityManyToMany01_collection03() {
        return entityManyToMany01_collection03;
    }

    public void setEntityManyToMany01_collection03(Collection p) {
        this.entityManyToMany01_collection03 = p;
    }

    public Collection<EntityManyToMany01> getEntityManyToMany01_collection04() {
        return entityManyToMany01_collection04;
    }

    public void setEntityManyToMany01_collection04(Collection<EntityManyToMany01> p) {
        this.entityManyToMany01_collection04 = p;
    }

    public Collection<EntityManyToMany01> getEntityManyToMany01_collection05() {
        return entityManyToMany01_collection05;
    }

    public void setEntityManyToMany01_collection05(Collection<EntityManyToMany01> p) {
        this.entityManyToMany01_collection05 = p;
    }

    public Collection<EntityManyToMany02> getEntityManyToMany01_collection06() {
        return entityManyToMany01_collection06;
    }

    public void setEntityManyToMany01_collection06(Collection<EntityManyToMany02> p) {
        this.entityManyToMany01_collection06 = p;
    }

    public Collection<EntityManyToMany02> getEntityManyToMany01_collection07() {
        return entityManyToMany01_collection07;
    }

    public void setEntityManyToMany01_collection07(Collection<EntityManyToMany02> p) {
        this.entityManyToMany01_collection07 = p;
    }

    public HashSet getEntityManyToMany01_hashset01() {
        return entityManyToMany01_hashset01;
    }

    public void setEntityManyToMany01_hashset01(HashSet p) {
        this.entityManyToMany01_hashset01 = p;
    }

    public HashSet getEntityManyToMany01_hashset02() {
        return entityManyToMany01_hashset02;
    }

    public void setEntityManyToMany01_hashset02(HashSet p) {
        this.entityManyToMany01_hashset02 = p;
    }

    public HashSet getEntityManyToMany01_hashset03() {
        return entityManyToMany01_hashset03;
    }

    public void setEntityManyToMany01_hashset03(HashSet p) {
        this.entityManyToMany01_hashset03 = p;
    }

    public HashSet<EntityManyToMany01> getEntityManyToMany01_hashset04() {
        return entityManyToMany01_hashset04;
    }

    public void setEntityManyToMany01_hashset04(HashSet<EntityManyToMany01> p) {
        this.entityManyToMany01_hashset04 = p;
    }

    public HashSet<EntityManyToMany01> getEntityManyToMany01_hashset05() {
        return entityManyToMany01_hashset05;
    }

    public void setEntityManyToMany01_hashset05(HashSet<EntityManyToMany01> p) {
        this.entityManyToMany01_hashset05 = p;
    }

    public HashSet<EntityManyToMany02> getEntityManyToMany01_hashset06() {
        return entityManyToMany01_hashset06;
    }

    public void setEntityManyToMany01_hashset06(HashSet<EntityManyToMany02> p) {
        this.entityManyToMany01_hashset06 = p;
    }

    public HashSet<EntityManyToMany02> getEntityManyToMany01_hashset07() {
        return entityManyToMany01_hashset07;
    }

    public void setEntityManyToMany01_hashset07(HashSet<EntityManyToMany02> p) {
        this.entityManyToMany01_hashset07 = p;
    }

    public Map getEntityManyToMany01_map01() {
        return entityManyToMany01_map01;
    }

    public void setEntityManyToMany01_map01(Map p) {
        this.entityManyToMany01_map01 = p;
    }

    public Map getEntityManyToMany01_map02() {
        return entityManyToMany01_map02;
    }

    public void setEntityManyToMany01_map02(Map p) {
        this.entityManyToMany01_map02 = p;
    }

    public Map getEntityManyToMany01_map03() {
        return entityManyToMany01_map03;
    }

    public void setEntityManyToMany01_map03(Map p) {
        this.entityManyToMany01_map03 = p;
    }

    public Map<EntityManyToMany01, EntityManyToMany01> getEntityManyToMany01_map04() {
        return entityManyToMany01_map04;
    }

    public void setEntityManyToMany01_map04(Map<EntityManyToMany01, EntityManyToMany01> p) {
        this.entityManyToMany01_map04 = p;
    }

    public Map<EntityManyToMany01, EntityManyToMany01> getEntityManyToMany01_map05() {
        return entityManyToMany01_map05;
    }

    public void setEntityManyToMany01_map05(Map<EntityManyToMany01, EntityManyToMany01> p) {
        this.entityManyToMany01_map05 = p;
    }

    public Map<EntityManyToMany02, EntityManyToMany02> getEntityManyToMany01_map06() {
        return entityManyToMany01_map06;
    }

    public void setEntityManyToMany01_map06(Map<EntityManyToMany02, EntityManyToMany02> p) {
        this.entityManyToMany01_map06 = p;
    }

    public Map<EntityManyToMany02, EntityManyToMany02> getEntityManyToMany01_map07() {
        return entityManyToMany01_map07;
    }

    public void setEntityManyToMany01_map07(Map<EntityManyToMany02, EntityManyToMany02> p) {
        this.entityManyToMany01_map07 = p;
    }
}
