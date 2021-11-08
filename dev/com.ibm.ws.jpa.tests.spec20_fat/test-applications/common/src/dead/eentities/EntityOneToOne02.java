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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
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
 * <li><b>@OneToOne</b> (38 of 65):
 * <p>This annotation defines a single-valued association to another entity that has one-to-one multiplicity.
 * <p>@OneToOne combinations:
 * <ul>
 * <li>@OneToOne on field
 * <li>@OneToOne on property
 * <li>cascade
 * <li>fetch
 * <li>mappedBy
 * <li>orphanRemoval
 * <li>targetEntity
 * </ul>
 * <p>@OneToOne combinations exercised in this entity:
 * <ul>
 * <li>@OneToOne on field
 * <li>cascade
 * <li>fetch
 * <li>mappedBy
 * <li>orphanRemoval
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
 *          |  EntityOneToOne02  |<-------------->|  EntityOneToOne02  |<-------------->|  EntityOneToOne01  |
 *          +----------------------+  n          n  +----------------------+  n          n  +----------------------+
 *
 * </pre>
 */
@Access(AccessType.FIELD)
//@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "CDM_EntityManyToMany01")
//     catalog = "CDM_EntityManyToMany01_Catalog",
//     schema = "CMD_EntityManyToMany01_Schema",
//     uniqueConstraints=@UniqueConstraint(columnNames={"entity01_string01"}))
public class EntityOneToOne02 {

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityOneToOne02_id",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityManyToMany01",
            updatable = true)
    @Id
    private int entityOneToOne02_id;

    @Basic(fetch = FetchType.EAGER)
    @Column(columnDefinition = "INTEGER",
            insertable = true,
            length = 15,
            name = "entityOneToOne02_version",
            nullable = true,
            precision = 15,
            scale = 15,
            table = "CDM_EntityManyToMany01",
            updatable = true)
    @Version
    private int entityOneToOne02_version;

    //----------------------------------------------------------------------------------------------
    // @OneToOne combinations
    //----------------------------------------------------------------------------------------------
//  @OneToOne(cascade = CascadeType.ALL,
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Set entityOneToOne02_set01;
//
//  @OneToOne(cascade = CascadeType.DETACH,
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_set02",
//            orphanRemoval = true,
//            targetEntity = EntityOneToOne02.class)
//  private Set entityOneToOne02_set02;
//
//  @OneToOne(cascade = CascadeType.MERGE,
//            fetch = FetchType.LAZY,
//            orphanRemoval = false,
//            targetEntity = EntityOneToOne01.class)
//  private Set entityOneToOne02_set03;
//
//  @OneToOne(cascade = CascadeType.PERSIST,
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_set04",
//            orphanRemoval = true)
//  private Set<EntityOneToOne01> entityOneToOne02_set04;
//
//  @OneToOne(cascade = CascadeType.REFRESH,
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Set<EntityOneToOne01> entityOneToOne02_set05;
//
//  @OneToOne(cascade = CascadeType.REMOVE,
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_set07",
//            orphanRemoval = true)
//  private Set<EntityOneToOne02> entityOneToOne02_set06;
//
//  @OneToOne(cascade={CascadeType.ALL, CascadeType.DETACH},
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Set<EntityOneToOne02> entityOneToOne02_set07;
//
//  @OneToOne(cascade={CascadeType.DETACH, CascadeType.MERGE},
//            fetch = FetchType.EAGER,
//            orphanRemoval = true)
//  private List entityOneToOne02_list01;
//
//  @OneToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST},
//            fetch = FetchType.LAZY,
//            mappedBy = "entityOneToOne01_list02",
//            orphanRemoval = false,
//            targetEntity = EntityOneToOne02.class)
//  private List entityOneToOne02_list02;
//
//  @OneToOne(cascade={CascadeType.PERSIST, CascadeType.REFRESH},
//            fetch = FetchType.EAGER,
//            orphanRemoval = true,
//            targetEntity = EntityOneToOne01.class)
//  private List entityOneToOne02_list03;
//
//  @OneToOne(cascade={CascadeType.REFRESH, CascadeType.REMOVE},
//            fetch = FetchType.LAZY,
//            mappedBy = "entityOneToOne01_list04",
//            orphanRemoval = false)
//  private List<EntityOneToOne01> entityOneToOne02_list04;
//
//  @OneToOne(cascade = CascadeType.ALL,
//            fetch = FetchType.EAGER,
//            orphanRemoval = true)
//  private List<EntityOneToOne01> entityOneToOne02_list05;
//
//  @OneToOne(cascade = CascadeType.DETACH,
//            fetch = FetchType.LAZY,
//            mappedBy = "entityOneToOne01_list06",
//            orphanRemoval = false)
//  private List<EntityOneToOne02> entityOneToOne02_list06;
//
//  @OneToOne(cascade = CascadeType.MERGE,
//            fetch = FetchType.EAGER,
//            orphanRemoval = true)
//  private List<EntityOneToOne02> entityOneToOne02_list07;
//
//  @OneToOne(cascade = CascadeType.PERSIST,
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Collection entityOneToOne02_collection01;
//
//  @OneToOne(cascade = CascadeType.REFRESH,
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_collection02",
//            orphanRemoval = true,
//            targetEntity = EntityOneToOne02.class)
//  private Collection entityOneToOne02_collection02;
//
//  @OneToOne(cascade = CascadeType.REMOVE,
//            fetch = FetchType.LAZY,
//            orphanRemoval = false,
//            targetEntity = EntityOneToOne01.class)
//  private Collection entityOneToOne02_collection03;
//
//  @OneToOne(cascade={CascadeType.ALL, CascadeType.DETACH},
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_collection04",
//            orphanRemoval = true)
//  private Collection<EntityOneToOne01> entityOneToOne02_collection04;
//
//  @OneToOne(cascade={CascadeType.DETACH, CascadeType.MERGE},
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Collection<EntityOneToOne01> entityOneToOne02_collection05;
//
//  @OneToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST},
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_collection06",
//            orphanRemoval = true)
//  private Collection<EntityOneToOne02> entityOneToOne02_collection06;
//
//  @OneToOne(cascade={CascadeType.PERSIST, CascadeType.REFRESH},
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Collection<EntityOneToOne02> entityOneToOne02_collection07;
//
//  @OneToOne(cascade={CascadeType.REFRESH, CascadeType.REMOVE},
//            fetch = FetchType.EAGER,
//            orphanRemoval = true)
//  private HashSet entityOneToOne02_hashset01;
//
//  @OneToOne(cascade = CascadeType.ALL,
//            fetch = FetchType.LAZY,
//            mappedBy = "entityOneToOne01_hashSet02",
//            orphanRemoval = false,
//            targetEntity = EntityOneToOne02.class)
//  private HashSet entityOneToOne02_hashset02;
//
//  @OneToOne(cascade = CascadeType.DETACH,
//            fetch = FetchType.EAGER,
//            orphanRemoval = true,
//            targetEntity = EntityOneToOne01.class)
//  private HashSet entityOneToOne02_hashset03;
//
//  @OneToOne(cascade = CascadeType.MERGE,
//            fetch = FetchType.LAZY,
//            mappedBy = "entityOneToOne01_hashSet04",
//            orphanRemoval = false)
//  private HashSet<EntityOneToOne01> entityOneToOne02_hashset04;
//
//  @OneToOne(cascade = CascadeType.PERSIST,
//            fetch = FetchType.EAGER,
//            orphanRemoval = true)
//  private HashSet<EntityOneToOne01> entityOneToOne02_hashset05;
//
//  @OneToOne(cascade = CascadeType.REFRESH,
//            fetch = FetchType.LAZY,
//            mappedBy = "entityOneToOne01_hashSet06",
//            orphanRemoval = false)
//  private HashSet<EntityOneToOne02> entityOneToOne02_hashset06;
//
//  @OneToOne(cascade = CascadeType.REMOVE,
//            fetch = FetchType.EAGER,
//            orphanRemoval = true)
//  private HashSet<EntityOneToOne02> entityOneToOne02_hashset07;
//
//  @OneToOne(cascade={CascadeType.ALL, CascadeType.DETACH},
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Map entityOneToOne02_map01;
//
//  @OneToOne(cascade={CascadeType.DETACH, CascadeType.MERGE},
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_map02",
//            orphanRemoval = true,
//            targetEntity = EntityOneToOne02.class)
//  private Map entityOneToOne02_map02;
//
//  @OneToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST},
//            fetch = FetchType.LAZY,
//            orphanRemoval = false,
//            targetEntity = EntityOneToOne01.class)
//  private Map entityOneToOne02_map03;
//
//  @OneToOne(cascade={CascadeType.PERSIST, CascadeType.REFRESH},
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_map04",
//            orphanRemoval = true)
//  private Map<EntityOneToOne01,EntityOneToOne01> entityOneToOne02_map04;
//
//  @OneToOne(cascade={CascadeType.REFRESH, CascadeType.REMOVE},
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Map<EntityOneToOne01,EntityOneToOne01> entityOneToOne02_map05;
//
//  @OneToOne(cascade = CascadeType.ALL,
//            fetch = FetchType.EAGER,
//            mappedBy = "entityOneToOne01_map06",
//            orphanRemoval = true)
//  private Map<EntityOneToOne02,EntityOneToOne02> entityOneToOne02_map06;
//
//  @OneToOne(cascade = CascadeType.DETACH,
//            fetch = FetchType.LAZY,
//            orphanRemoval = false)
//  private Map<EntityOneToOne02,EntityOneToOne02> entityOneToOne02_map07;

//
    public EntityOneToOne02() {}

    public EntityOneToOne02(int id,
                            int version) {
//                          Set                                     set01,
//                          Set                                     set02,
//                          Set                                     set03,
//                          Set<EntityOneToOne01>                   set04,
//                          Set<EntityOneToOne01>                   set05,
//                          Set<EntityOneToOne02>                   set06,
//                          Set<EntityOneToOne02>                   set07,
//                          List                                    list01,
//                          List                                    list02,
//                          List                                    list03,
//                          List<EntityOneToOne01>                  list04,
//                          List<EntityOneToOne01>                  list05,
//                          List<EntityOneToOne02>                  list06,
//                          List<EntityOneToOne02>                  list07,
//                          Collection                              collection01,
//                          Collection                              collection02,
//                          Collection                              collection03,
//                          Collection<EntityOneToOne01>            collection04,
//                          Collection<EntityOneToOne01>            collection05,
//                          Collection<EntityOneToOne02>            collection06,
//                          Collection<EntityOneToOne02>            collection07,
//                          HashSet                                 hashset01,
//                          HashSet                                 hashset02,
//                          HashSet                                 hashset03,
//                          HashSet<EntityOneToOne01>               hashset04,
//                          HashSet<EntityOneToOne01>               hashset05,
//                          HashSet<EntityOneToOne02>               hashset06,
//                          HashSet<EntityOneToOne02>               hashset07,
//                          Map                                     map01,
//                          Map                                     map02,
//                          Map                                     map03,
//                          Map<EntityOneToOne01,EntityOneToOne01>  map04,
//                          Map<EntityOneToOne01,EntityOneToOne01>  map05,
//                          Map<EntityOneToOne02,EntityOneToOne02>  map06,
//                          Map<EntityOneToOne02,EntityOneToOne02>  map07) {
        this.entityOneToOne02_id = id;
        this.entityOneToOne02_version = version;
//      this.entityOneToOne02_set01        = set01;
//      this.entityOneToOne02_set02        = set02;
//      this.entityOneToOne02_set03        = set03;
//      this.entityOneToOne02_set04        = set04;
//      this.entityOneToOne02_set05        = set05;
//      this.entityOneToOne02_set06        = set06;
//      this.entityOneToOne02_set07        = set07;
//      this.entityOneToOne02_list01       = list01;
//      this.entityOneToOne02_list02       = list02;
//      this.entityOneToOne02_list03       = list03;
//      this.entityOneToOne02_list04       = list04;
//      this.entityOneToOne02_list05       = list05;
//      this.entityOneToOne02_list06       = list06;
//      this.entityOneToOne02_list07       = list07;
//      this.entityOneToOne02_collection01 = collection01;
//      this.entityOneToOne02_collection02 = collection02;
//      this.entityOneToOne02_collection03 = collection03;
//      this.entityOneToOne02_collection04 = collection04;
//      this.entityOneToOne02_collection05 = collection05;
//      this.entityOneToOne02_collection06 = collection06;
//      this.entityOneToOne02_collection07 = collection07;
//      this.entityOneToOne02_hashset01    = hashset01;
//      this.entityOneToOne02_hashset02    = hashset02;
//      this.entityOneToOne02_hashset03    = hashset03;
//      this.entityOneToOne02_hashset04    = hashset04;
//      this.entityOneToOne02_hashset05    = hashset05;
//      this.entityOneToOne02_hashset06    = hashset06;
//      this.entityOneToOne02_hashset07    = hashset07;
//      this.entityOneToOne02_map01        = map01;
//      this.entityOneToOne02_map02        = map02;
//      this.entityOneToOne02_map03        = map03;
//      this.entityOneToOne02_map04        = map04;
//      this.entityOneToOne02_map05        = map05;
//      this.entityOneToOne02_map06        = map06;
//      this.entityOneToOne02_map07        = map07;
    }

    @Override
    public String toString() {
        return (" EntityOneToOne02: " +
                " entityOneToOne02_id: " + getEntityOneToOne02_id() +
                " entityOneToOne02_version: " + getEntityOneToOne02_version());
//              " entityOneToOne02_set01: "        + getEntityOneToOne02_set01() +
//              " entityOneToOne02_set02: "        + getEntityOneToOne02_set02() +
//              " entityOneToOne02_set03: "        + getEntityOneToOne02_set03() +
//              " entityOneToOne02_set04: "        + getEntityOneToOne02_set04() +
//              " entityOneToOne02_set05: "        + getEntityOneToOne02_set05() +
//              " entityOneToOne02_set06: "        + getEntityOneToOne02_set06() +
//              " entityOneToOne02_set07: "        + getEntityOneToOne02_set07() +
//              " entityOneToOne02_list01: "       + getEntityOneToOne02_list01() +
//              " entityOneToOne02_list02: "       + getEntityOneToOne02_list02() +
//              " entityOneToOne02_list03: "       + getEntityOneToOne02_list03() +
//              " entityOneToOne02_list04: "       + getEntityOneToOne02_list04() +
//              " entityOneToOne02_list05: "       + getEntityOneToOne02_list05() +
//              " entityOneToOne02_list06: "       + getEntityOneToOne02_list06() +
//              " entityOneToOne02_list07: "       + getEntityOneToOne02_list07() +
//              " entityOneToOne02_collection01: " + getEntityOneToOne02_collection01() +
//              " entityOneToOne02_collection02: " + getEntityOneToOne02_collection02() +
//              " entityOneToOne02_collection03: " + getEntityOneToOne02_collection03() +
//              " entityOneToOne02_collection04: " + getEntityOneToOne02_collection04() +
//              " entityOneToOne02_collection05: " + getEntityOneToOne02_collection05() +
//              " entityOneToOne02_collection06: " + getEntityOneToOne02_collection06() +
//              " entityOneToOne02_collection07: " + getEntityOneToOne02_collection07() +
//              " entityOneToOne02_hashset01: "    + getEntityOneToOne02_hashset01() +
//              " entityOneToOne02_hashset02: "    + getEntityOneToOne02_hashset02() +
//              " entityOneToOne02_hashset03: "    + getEntityOneToOne02_hashset03() +
//              " entityOneToOne02_hashset04: "    + getEntityOneToOne02_hashset04() +
//              " entityOneToOne02_hashset05: "    + getEntityOneToOne02_hashset05() +
//              " entityOneToOne02_hashset06: "    + getEntityOneToOne02_hashset06() +
//              " entityOneToOne02_hashset07: "    + getEntityOneToOne02_hashset07() +
//              " entityOneToOne02_map01: "        + getEntityOneToOne02_map01() +
//              " entityOneToOne02_map02: "        + getEntityOneToOne02_map02() +
//              " entityOneToOne02_map03: "        + getEntityOneToOne02_map03() +
//              " entityOneToOne02_map04: "        + getEntityOneToOne02_map04() +
//              " entityOneToOne02_map05: "        + getEntityOneToOne02_map05() +
//              " entityOneToOne02_map06: "        + getEntityOneToOne02_map05() +
//              " entityOneToOne02_map07: "        + getEntityOneToOne02_map07() );
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntityOneToOne02_id() {
        return entityOneToOne02_id;
    }

    public void setEntityOneToOne02_id(int p) {
        this.entityOneToOne02_id = p;
    }

    public int getEntityOneToOne02_version() {
        return entityOneToOne02_version;
    }

    public void setEntityOneToOne02_version(int p) {
        this.entityOneToOne02_version = p;
    }

//  public Set getEntityOneToOne02_set01() {
//      return entityOneToOne02_set01;
//  }
//  public void setEntityOneToOne02_set01(Set p) {
//      this.entityOneToOne02_set01 = p;
//  }
//
//
//  public Set getEntityOneToOne02_set02() {
//      return entityOneToOne02_set01;
//  }
//  public void setEntityOneToOne02_set02(Set p) {
//      this.entityOneToOne02_set01 = p;
//  }
//
//
//  public Set getEntityOneToOne02_set03() {
//      return entityOneToOne02_set01;
//  }
//  public void setEntityOneToOne02_set03(Set p) {
//      this.entityOneToOne02_set01 = p;
//  }
//
//
//  public Set<EntityOneToOne01> getEntityOneToOne02_set04() {
//      return entityOneToOne02_set04;
//  }
//  public void setEntityOneToOne02_set04(Set<EntityOneToOne01> p) {
//      this.entityOneToOne02_set04 = p;
//  }
//
//
//  public Set<EntityOneToOne01> getEntityOneToOne02_set05() {
//      return entityOneToOne02_set05;
//  }
//  public void setEntityOneToOne02_set05(Set<EntityOneToOne01> p) {
//      this.entityOneToOne02_set05 = p;
//  }
//
//
//  public Set<EntityOneToOne02> getEntityOneToOne02_set06() {
//      return entityOneToOne02_set06;
//  }
//  public void setEntityOneToOne02_set06(Set<EntityOneToOne02> p) {
//      this.entityOneToOne02_set06 = p;
//  }
//
//
//  public Set<EntityOneToOne02> getEntityOneToOne02_set07() {
//      return entityOneToOne02_set07;
//  }
//  public void setEntityOneToOne02_set07(Set<EntityOneToOne02> p) {
//      this.entityOneToOne02_set07 = p;
//  }
//
//
//  public List getEntityOneToOne02_list01() {
//      return entityOneToOne02_list01;
//  }
//  public void setEntityOneToOne02_list01(List p) {
//      this.entityOneToOne02_list01 = p;
//  }
//
//
//  public List getEntityOneToOne02_list02() {
//      return entityOneToOne02_list02;
//  }
//  public void setEntityOneToOne02_list02(List p) {
//      this.entityOneToOne02_list02 = p;
//  }
//
//
//  public List getEntityOneToOne02_list03() {
//      return entityOneToOne02_list03;
//  }
//  public void setEntityOneToOne02_list03(List p) {
//      this.entityOneToOne02_list03 = p;
//  }
//
//
//  public List<EntityOneToOne01> getEntityOneToOne02_list04() {
//      return entityOneToOne02_list04;
//  }
//  public void setEntityOneToOne02_list04(List<EntityOneToOne01> p) {
//      this.entityOneToOne02_list04 = p;
//  }
//
//
//  public List<EntityOneToOne01> getEntityOneToOne02_list05() {
//      return entityOneToOne02_list05;
//  }
//  public void setEntityOneToOne02_list05(List<EntityOneToOne01> p) {
//      this.entityOneToOne02_list05 = p;
//  }
//
//
//  public List<EntityOneToOne02> getEntityOneToOne02_list06() {
//      return entityOneToOne02_list06;
//  }
//  public void setEntityOneToOne02_list06(List<EntityOneToOne02> p) {
//      this.entityOneToOne02_list06 = p;
//  }
//
//
//  public List<EntityOneToOne02> getEntityOneToOne02_list07() {
//      return entityOneToOne02_list07;
//  }
//  public void setEntityOneToOne02_list07(List<EntityOneToOne02> p) {
//      this.entityOneToOne02_list07 = p;
//  }
//
//
//  public Collection getEntityOneToOne02_collection01() {
//      return entityOneToOne02_collection01;
//  }
//  public void setEntityOneToOne02_collection01(Collection p) {
//      this.entityOneToOne02_collection01 = p;
//  }
//
//
//  public Collection getEntityOneToOne02_collection02() {
//      return entityOneToOne02_collection02;
//  }
//  public void setEntityOneToOne02_collection02(Collection p) {
//      this.entityOneToOne02_collection02 = p;
//  }
//
//
//  public Collection getEntityOneToOne02_collection03() {
//      return entityOneToOne02_collection03;
//  }
//  public void setEntityOneToOne02_collection03(Collection p) {
//      this.entityOneToOne02_collection03 = p;
//  }
//
//
//  public Collection<EntityOneToOne01> getEntityOneToOne02_collection04() {
//      return entityOneToOne02_collection04;
//  }
//  public void setEntityOneToOne02_collection04(Collection<EntityOneToOne01> p) {
//      this.entityOneToOne02_collection04 = p;
//  }
//
//
//  public Collection<EntityOneToOne01> getEntityOneToOne02_collection05() {
//      return entityOneToOne02_collection05;
//  }
//  public void setEntityOneToOne02_collection05(Collection<EntityOneToOne01> p) {
//      this.entityOneToOne02_collection05 = p;
//  }
//
//
//  public Collection<EntityOneToOne02> getEntityOneToOne02_collection06() {
//      return entityOneToOne02_collection06;
//  }
//  public void setEntityOneToOne02_collection06(Collection<EntityOneToOne02> p) {
//      this.entityOneToOne02_collection06 = p;
//  }
//
//
//  public Collection<EntityOneToOne02> getEntityOneToOne02_collection07() {
//      return entityOneToOne02_collection07;
//  }
//  public void setEntityOneToOne02_collection07(Collection<EntityOneToOne02> p) {
//      this.entityOneToOne02_collection07 = p;
//  }
//
//
//  public HashSet getEntityOneToOne02_hashset01() {
//      return entityOneToOne02_hashset01;
//  }
//  public void setEntityOneToOne02_hashset01(HashSet p) {
//      this.entityOneToOne02_hashset01 = p;
//  }
//
//
//  public HashSet getEntityOneToOne02_hashset02() {
//      return entityOneToOne02_hashset02;
//  }
//  public void setEntityOneToOne02_hashset02(HashSet p) {
//      this.entityOneToOne02_hashset02 = p;
//  }
//
//
//  public HashSet getEntityOneToOne02_hashset03() {
//      return entityOneToOne02_hashset03;
//  }
//  public void setEntityOneToOne02_hashset03(HashSet p) {
//      this.entityOneToOne02_hashset03 = p;
//  }
//
//
//  public HashSet<EntityOneToOne01> getEntityOneToOne02_hashset04() {
//      return entityOneToOne02_hashset04;
//  }
//  public void setEntityOneToOne02_hashset04(HashSet<EntityOneToOne01> p) {
//      this.entityOneToOne02_hashset04 = p;
//  }
//
//
//  public HashSet<EntityOneToOne01> getEntityOneToOne02_hashset05() {
//      return entityOneToOne02_hashset05;
//  }
//  public void setEntityOneToOne02_hashset05(HashSet<EntityOneToOne01> p) {
//      this.entityOneToOne02_hashset05 = p;
//  }
//
//
//  public HashSet<EntityOneToOne02> getEntityOneToOne02_hashset06() {
//      return entityOneToOne02_hashset06;
//  }
//  public void setEntityOneToOne02_hashset06(HashSet<EntityOneToOne02> p) {
//      this.entityOneToOne02_hashset06 = p;
//  }
//
//
//  public HashSet<EntityOneToOne02> getEntityOneToOne02_hashset07() {
//      return entityOneToOne02_hashset07;
//  }
//  public void setEntityOneToOne02_hashset07(HashSet<EntityOneToOne02> p) {
//      this.entityOneToOne02_hashset07 = p;
//  }
//
//
//  public Map getEntityOneToOne02_map01() {
//      return entityOneToOne02_map01;
//  }
//  public void setEntityOneToOne02_map01(Map p) {
//      this.entityOneToOne02_map01 = p;
//  }
//
//
//  public Map getEntityOneToOne02_map02() {
//      return entityOneToOne02_map02;
//  }
//  public void setEntityOneToOne02_map02(Map p) {
//      this.entityOneToOne02_map02 = p;
//  }
//
//
//  public Map getEntityOneToOne02_map03() {
//      return entityOneToOne02_map03;
//  }
//  public void setEntityOneToOne02_map03(Map p) {
//      this.entityOneToOne02_map03 = p;
//  }
//
//
//  public Map<EntityOneToOne01,EntityOneToOne01> getEntityOneToOne02_map04() {
//      return entityOneToOne02_map04;
//  }
//  public void setEntityOneToOne02_map04(Map<EntityOneToOne01,EntityOneToOne01> p) {
//      this.entityOneToOne02_map04 = p;
//  }
//
//
//  public Map<EntityOneToOne01,EntityOneToOne01> getEntityOneToOne02_map05() {
//      return entityOneToOne02_map05;
//  }
//  public void setEntityOneToOne02_map05(Map<EntityOneToOne01,EntityOneToOne01> p) {
//      this.entityOneToOne02_map05 = p;
//  }
//
//
//  public Map<EntityOneToOne02,EntityOneToOne02> getEntityOneToOne02_map06() {
//      return entityOneToOne02_map06;
//  }
//  public void setEntityOneToOne02_map06(Map<EntityOneToOne02,EntityOneToOne02> p) {
//      this.entityOneToOne02_map06 = p;
//  }
//
//
//  public Map<EntityOneToOne02,EntityOneToOne02> getEntityOneToOne02_map07() {
//      return entityOneToOne02_map07;
//  }
//  public void setEntityOneToOne02_map07(Map<EntityOneToOne02,EntityOneToOne02> p) {
//      this.entityOneToOne02_map07 = p;
//  }
}
