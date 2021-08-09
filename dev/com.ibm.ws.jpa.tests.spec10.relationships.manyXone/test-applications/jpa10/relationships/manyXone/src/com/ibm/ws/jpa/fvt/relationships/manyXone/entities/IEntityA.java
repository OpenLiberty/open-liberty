/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities;

/**
 * Interface implemented by all entities belonging to the owning side of the Many-to-One relationship.
 *
 */
public interface IEntityA {
    /*
     * Entity Primary Key
     */
    public int getId();

    public void setId(int id);

    /*
     * Simple Entity Persistent Data
     */
    public String getName();

    public void setName(String name);

    /*
     * Relationship Fields
     */

    /*
     * Field: defaultRelationship
     *
     * Many to one mapping to an IEntityB-type entity. No override of the foreign key column name.
     *
     * OneToMany Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    public IEntityB getDefaultRelationshipField();

    public void setDefaultRelationshipField(IEntityB b2);

    /*
     * Field: overrideColumnNameRelationship
     *
     * Many to one mapping to an IEntityB-type entity. Overriding the name of the join column to "MANYTOONE_ENTB".
     *
     * OneToMany Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "MANYTOONE_ENTB".
     */
    public IEntityB getOverrideColumnNameField();

    public void setOverrideColumnNameField(IEntityB b1);

    /*
     * Field: Lazy
     *
     * Many to one mapping to an IEntityB-type entity. FetchType has been set to LAZY.
     *
     * OneToMany Config Cascade: default no Fetch: LAZY Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public IEntityB getLazyField();

    public void setLazyField(IEntityB b4);

    /*
     * Field: CascadeAll
     *
     * Many to one mapping to an IEntityB-type entity. This relation field has the CascadeType of ALL.
     *
     * OneToMany Config Cascade: ALL Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public IEntityB getCascadeAllField();

    public void setCascadeAllField(IEntityB b5ca);

    /*
     * Field: CascadeMerge
     *
     * One to Many mapping to an IEntityB-type entity. This relation field has the CascadeType of MERGE.
     *
     * OneToMany Config Cascade: MERGE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public IEntityB getCascadeMergeField();

    public void setCascadeMergeField(IEntityB b5cm);

    /*
     * Field: CascadePersist
     *
     * Many to one mapping to an IEntityB-type entity. This relation field has the CascadeType of PERSIST.
     *
     * OneToMany Config Cascade: PERSIST Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public IEntityB getCascadePersistField();

    public void setCascadePersistField(IEntityB b5cp);

    /*
     * Field: CascadeRefresh
     *
     * Many to one mapping to an IEntityB-type entity. This relation field has the CascadeType of REFRESH.
     *
     * OneToMany Config Cascade: REFRESH Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public IEntityB getCascadeRefreshField();

    public void setCascadeRefreshField(IEntityB b5rf);

    /*
     * Field: CascadeRemove
     *
     * One to one mapping to an IEntityB-type entity This relation field has the CascadeType of REMOVE.
     *
     * OneToMany Config Cascade: REMOVE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public IEntityB getCascadeRemoveField();

    public void setCascadeRemoveField(IEntityB b5rm);
}
