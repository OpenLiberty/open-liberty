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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni.annotation;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityB;

@Entity
public class MOUniEntityA implements IEntityA {
    /**
     * Entity primary key, an integer id number.
     */
    @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    /**
     * Field: defaultRelationship
     *
     * Many to one mapping to an IEntityB-type entity. No override of the foreign key column name.
     *
     * OneToMany Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    @ManyToOne
    private MOUniEntityB defaultRelationship;

    /*
     * Field: overrideColumnNameRelationship
     *
     * Many to one mapping to an IEntityB-type entity. Overriding the name of the join column to "MANYTOONE_ENTB".
     *
     * OneToMany Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "MANYTOONE_ENTB".
     */
    @ManyToOne
    @JoinColumn(name = "MANYTOONE_ENTB")
    private MOUniEntityB overrideColumnNameRelationship;

    /*
     * Field: Lazy
     *
     * Many to one mapping to an IEntityB-type entity. FetchType has been set to LAZY.
     *
     * OneToMany Config Cascade: default no Fetch: LAZY Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private MOUniEntityB lazy;

    /*
     * Field: CascadeAll
     *
     * Many to one mapping to an IEntityB-type entity. This relation field has the CascadeType of ALL.
     *
     * OneToMany Config Cascade: ALL Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    private MOUniEntityB cascadeAll;

    /*
     * Field: CascadeMerge
     *
     * One to Many mapping to an IEntityB-type entity. This relation field has the CascadeType of MERGE.
     *
     * OneToMany Config Cascade: MERGE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToOne(cascade = CascadeType.MERGE)
    private MOUniEntityB cascadeMerge;

    /*
     * Field: CascadePersist
     *
     * Many to one mapping to an IEntityB-type entity. This relation field has the CascadeType of PERSIST.
     *
     * OneToMany Config Cascade: PERSIST Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    private MOUniEntityB cascadePersist;

    /*
     * Field: CascadeRefresh
     *
     * Many to one mapping to an IEntityB-type entity. This relation field has the CascadeType of REFRESH.
     *
     * OneToMany Config Cascade: REFRESH Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToOne(cascade = CascadeType.REFRESH)
    private MOUniEntityB cascadeRefresh;

    /*
     * Field: CascadeRemove
     *
     * One to one mapping to an IEntityB-type entity This relation field has the CascadeType of REMOVE.
     *
     * OneToMany Config Cascade: REMOVE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToOne(cascade = CascadeType.REMOVE)
    private MOUniEntityB cascadeRemove;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public MOUniEntityB getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(MOUniEntityB cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public MOUniEntityB getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(MOUniEntityB cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public MOUniEntityB getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(MOUniEntityB cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public MOUniEntityB getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(MOUniEntityB cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public MOUniEntityB getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(MOUniEntityB cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    public MOUniEntityB getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(MOUniEntityB defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public MOUniEntityB getLazy() {
        return lazy;
    }

    public void setLazy(MOUniEntityB lazy) {
        this.lazy = lazy;
    }

    public MOUniEntityB getOverrideColumnNameRelationship() {
        return overrideColumnNameRelationship;
    }

    public void setOverrideColumnNameRelationship(MOUniEntityB overrideColumnNameRelationship) {
        this.overrideColumnNameRelationship = overrideColumnNameRelationship;
    }

    @Override
    public IEntityB getCascadeAllField() {
        return getCascadeAll();
    }

    @Override
    public IEntityB getCascadeMergeField() {
        return getCascadeMerge();
    }

    @Override
    public IEntityB getCascadePersistField() {
        return getCascadePersist();
    }

    @Override
    public IEntityB getCascadeRefreshField() {
        return getCascadeRefresh();
    }

    @Override
    public IEntityB getCascadeRemoveField() {
        return getCascadeRemove();
    }

    @Override
    public IEntityB getDefaultRelationshipField() {
        return getDefaultRelationship();
    }

    @Override
    public IEntityB getLazyField() {
        return getLazy();
    }

    @Override
    public IEntityB getOverrideColumnNameField() {
        return getOverrideColumnNameRelationship();
    }

    @Override
    public void setCascadeAllField(IEntityB entity) {
        setCascadeAll((MOUniEntityB) entity);
    }

    @Override
    public void setCascadeMergeField(IEntityB entity) {
        setCascadeMerge((MOUniEntityB) entity);
    }

    @Override
    public void setCascadePersistField(IEntityB entity) {
        setCascadePersist((MOUniEntityB) entity);
    }

    @Override
    public void setCascadeRefreshField(IEntityB entity) {
        setCascadeRefresh((MOUniEntityB) entity);
    }

    @Override
    public void setCascadeRemoveField(IEntityB entity) {
        setCascadeRemove((MOUniEntityB) entity);
    }

    @Override
    public void setDefaultRelationshipField(IEntityB entity) {
        setDefaultRelationship((MOUniEntityB) entity);
    }

    @Override
    public void setLazyField(IEntityB entity) {
        setLazy((MOUniEntityB) entity);
    }

    @Override
    public void setOverrideColumnNameField(IEntityB entity) {
        setOverrideColumnNameRelationship((MOUniEntityB) entity);
    }

    @Override
    public String toString() {
        return "MOUniEntityA [id=" + id + ", name=" + name + "]";
    }

}
