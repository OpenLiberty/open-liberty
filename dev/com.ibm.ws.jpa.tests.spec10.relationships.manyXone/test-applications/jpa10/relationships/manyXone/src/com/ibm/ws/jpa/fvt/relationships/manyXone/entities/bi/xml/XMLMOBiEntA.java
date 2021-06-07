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

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml;

import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXone.entities.IEntityB;

//@Entity
public class XMLMOBiEntA implements IEntityA {
    /**
     * Entity primary key, an integer id number.
     */
    // @Id
    private int id;

    /**
     * Simple data payload for the entity.
     */
    private String name;

    /**
     * Field: defaultRelationship
     *
     * Many to one mapping to an IEntityBBi-type entity. No override of the foreign key column name.
     *
     * OneToMany Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    // @ManyToOne
    private XMLMOBiEntB_DR defaultRelationship;

    /*
     * Field: overrideColumnNameRelationship
     *
     * Many to one mapping to an IEntityBBi-type entity. Overriding the name of the join column to "MANYTOONE_ENTB".
     *
     * OneToMany Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "MANYTOONE_ENTB".
     */
    // @ManyToOne
    // @JoinColumn(name="MANYTOONE_ENTB")
    private XMLMOBiEntB_JC overrideColumnNameRelationship;

    /*
     * Field: Lazy
     *
     * Many to one mapping to an IEntityBBi-type entity. FetchType has been set to LAZY.
     *
     * OneToMany Config Cascade: default no Fetch: LAZY Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    // @ManyToOne(fetch=FetchType.LAZY)
    private XMLMOBiEntB_LZ lazy;

    /*
     * Field: CascadeAll
     *
     * Many to one mapping to an IEntityBBi-type entity. This relation field has the CascadeType of ALL.
     *
     * OneToMany Config Cascade: ALL Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    // @ManyToOne(cascade=CascadeType.ALL)
    private XMLMOBiEntB_CA cascadeAll;

    /*
     * Field: CascadeMerge
     *
     * One to Many mapping to an IEntityBBi-type entity. This relation field has the CascadeType of MERGE.
     *
     * OneToMany Config Cascade: MERGE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    // @ManyToOne(cascade=CascadeType.MERGE)
    private XMLMOBiEntB_CM cascadeMerge;

    /*
     * Field: CascadePersist
     *
     * Many to one mapping to an IEntityBBi-type entity. This relation field has the CascadeType of PERSIST.
     *
     * OneToMany Config Cascade: PERSIST Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    // @ManyToOne(cascade=CascadeType.PERSIST)
    private XMLMOBiEntB_CP cascadePersist;

    /*
     * Field: CascadeRefresh
     *
     * Many to one mapping to an IEntityBBi-type entity. This relation field has the CascadeType of REFRESH.
     *
     * OneToMany Config Cascade: REFRESH Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    // @ManyToOne(cascade=CascadeType.REFRESH)
    private XMLMOBiEntB_CRF cascadeRefresh;

    /*
     * Field: CascadeRemove
     *
     * One to one mapping to an IEntityBBi-type entity This relation field has the CascadeType of REMOVE.
     *
     * OneToMany Config Cascade: REMOVE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    // @ManyToOne(cascade=CascadeType.REMOVE)
    private XMLMOBiEntB_CRM cascadeRemove;

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

    public XMLMOBiEntB_CA getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(XMLMOBiEntB_CA cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public XMLMOBiEntB_CM getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(XMLMOBiEntB_CM cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public XMLMOBiEntB_CP getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(XMLMOBiEntB_CP cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public XMLMOBiEntB_CRF getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(XMLMOBiEntB_CRF cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public XMLMOBiEntB_CRM getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(XMLMOBiEntB_CRM cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    public XMLMOBiEntB_DR getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(XMLMOBiEntB_DR defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public XMLMOBiEntB_LZ getLazy() {
        return lazy;
    }

    public void setLazy(XMLMOBiEntB_LZ lazy) {
        this.lazy = lazy;
    }

    public XMLMOBiEntB_JC getOverrideColumnNameRelationship() {
        return overrideColumnNameRelationship;
    }

    public void setOverrideColumnNameRelationship(XMLMOBiEntB_JC overrideColumnNameRelationship) {
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
        setCascadeAll((XMLMOBiEntB_CA) entity);
    }

    @Override
    public void setCascadeMergeField(IEntityB entity) {
        setCascadeMerge((XMLMOBiEntB_CM) entity);
    }

    @Override
    public void setCascadePersistField(IEntityB entity) {
        setCascadePersist((XMLMOBiEntB_CP) entity);
    }

    @Override
    public void setCascadeRefreshField(IEntityB entity) {
        setCascadeRefresh((XMLMOBiEntB_CRF) entity);
    }

    @Override
    public void setCascadeRemoveField(IEntityB entity) {
        setCascadeRemove((XMLMOBiEntB_CRM) entity);
    }

    @Override
    public void setDefaultRelationshipField(IEntityB entity) {
        setDefaultRelationship((XMLMOBiEntB_DR) entity);
    }

    @Override
    public void setLazyField(IEntityB entity) {
        setLazy((XMLMOBiEntB_LZ) entity);
    }

    @Override
    public void setOverrideColumnNameField(IEntityB entity) {
        setOverrideColumnNameRelationship((XMLMOBiEntB_JC) entity);
    }

    @Override
    public String toString() {
        return "XMLMOBiEntA [id=" + id + ", name=" + name + "]";
    }
}
