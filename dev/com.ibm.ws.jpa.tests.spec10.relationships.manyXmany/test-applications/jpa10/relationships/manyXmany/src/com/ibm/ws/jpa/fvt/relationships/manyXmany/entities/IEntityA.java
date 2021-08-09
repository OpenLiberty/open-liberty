/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities;

import java.util.Collection;

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
     * Many to many mapping with an IEntityB-type entity.
     *
     * OneToMany Config Cascade: default no Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    public Collection getDefaultRelationshipCollectionField();

    public void setDefaultRelationshipCollectionField(Collection defaultRelationship);

    public void insertDefaultRelationshipField(IEntityB defaultRelationship);

    public void removeDefaultRelationshipField(IEntityB defaultRelationship);

    public boolean isMemberOfDefaultRelationshipField(IEntityB defaultRelationship);

    /*
     * Field: CascadeAll
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of ALL.
     *
     * OneToMany Config Cascade: ALL Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public Collection getCascadeAllCollectionField();

    public void setCascadeAllField(Collection cascadeAll);

    public void insertCascadeAllField(IEntityB cascadeAll);

    public void removeCascadeAllField(IEntityB cascadeAll);

    public boolean isMemberOfCascadeAllField(IEntityB cascadeAll);

    /*
     * Field: CascadeMerge
     *
     * One to many mapping with an IEntityB-type entity. This relation field has the CascadeType of MERGE.
     *
     * OneToMany Config Cascade: MERGE Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public Collection getCascadeMergeCollectionField();

    public void setCascadeMergeField(Collection cascadeMerge);

    public void insertCascadeMergeField(IEntityB cascadeMerge);

    public void removeCascadeMergeField(IEntityB cascadeMerge);

    public boolean isMemberOfCascadeMergeField(IEntityB cascadeMerge);

    /*
     * Field: CascadePersist
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of PERSIST.
     *
     * OneToMany Config Cascade: PERSIST Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public Collection getCascadePersistCollectionField();

    public void setCascadePersistField(Collection cascadePersist);

    public void insertCascadePersistField(IEntityB cascadePersist);

    public void removeCascadePersistField(IEntityB cascadePersist);

    public boolean isMemberOfCascadePersistField(IEntityB cascadePersist);

    /*
     * Field: CascadeRefresh
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of REFRESH.
     *
     * OneToMany Config Cascade: REFRESH Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public Collection getCascadeRefreshCollectionField();

    public void setCascadeRefreshField(Collection cascadeRefresh);

    public void insertCascadeRefreshField(IEntityB cascadeRefresh);

    public void removeCascadeRefreshField(IEntityB cascadeRefresh);

    public boolean sMemberOfCascadeRefreshField(IEntityB cascadeRefresh);

    /*
     * Field: CascadeRemove
     *
     * One to many mapping with an IEntityB-type entity This relation field has the CascadeType of REMOVE.
     *
     * OneToMany Config Cascade: REMOVE Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    public Collection getCascadeRemoveCollectionField();

    public void setCascadeRemoveField(Collection cascadeRemove);

    public void insertCascadeRemoveField(IEntityB cascadeRemove);

    public void removeCascadeRemoveField(IEntityB cascadeRemove);

    public boolean isMemberOfCascadeRemoveField(IEntityB cascadeRemove);
}