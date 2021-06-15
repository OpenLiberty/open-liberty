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

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityB;

@Entity
public class MMBiEntA implements IEntityA {
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
     * Many to many mapping with an IEntityB-type entity. No override of the foreign key column name.
     *
     * ManyToMany Config Cascade: default no Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    @ManyToMany
    @JoinTable(name = "ManyXManyDRBiJoinTable", joinColumns = @JoinColumn(name = "ENT_A"), inverseJoinColumns = @JoinColumn(name = "ENT_B"))
    private Collection<MMBiEntB_DR> defaultRelationship;

    /*
     * Field: CascadeAll
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of ALL.
     *
     * ManyToMany Config Cascade: ALL Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.ALL)
    /*
     * @JoinTable( name="ManyXManyCABiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMBiEntB_CA> cascadeAll;

    /*
     * Field: CascadeMerge
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of MERGE.
     *
     * ManyToMany Config Cascade: MERGE Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.MERGE)
    /*
     * @JoinTable( name="ManyXManyCMBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMBiEntB_CM> cascadeMerge;

    /*
     * Field: CascadePersist
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of PERSIST.
     *
     * ManyToMany Config Cascade: PERSIST Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.PERSIST)
    /*
     * @JoinTable( name="ManyXManyCPBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMBiEntB_CP> cascadePersist;

    /*
     * Field: CascadeRefresh
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of REFRESH.
     *
     * ManyToMany Config Cascade: REFRESH Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.REFRESH)
    /*
     * @JoinTable( name="ManyXManyCRFBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMBiEntB_CRF> cascadeRefresh;

    /*
     * Field: CascadeRemove
     *
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of REMOVE.
     *
     * ManyToMany Config Cascade: REFRESH Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.REMOVE)
    /*
     * @JoinTable( name="ManyXManyCRMBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMBiEntB_CRM> cascadeRemove;

    public MMBiEntA() {
        // Initialize the relational collection fields
        defaultRelationship = new ArrayList<MMBiEntB_DR>();
        cascadeAll = new ArrayList<MMBiEntB_CA>();
        cascadePersist = new ArrayList<MMBiEntB_CP>();
        cascadeMerge = new ArrayList<MMBiEntB_CM>();
        cascadeRefresh = new ArrayList<MMBiEntB_CRF>();
        cascadeRemove = new ArrayList<MMBiEntB_CRM>();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public Collection<MMBiEntB_CA> getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(Collection<MMBiEntB_CA> cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public Collection<MMBiEntB_CM> getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(Collection<MMBiEntB_CM> cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public Collection<MMBiEntB_CP> getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(Collection<MMBiEntB_CP> cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public Collection<MMBiEntB_CRF> getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(Collection<MMBiEntB_CRF> cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public Collection<MMBiEntB_DR> getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(Collection<MMBiEntB_DR> defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public Collection<MMBiEntB_CRM> getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(Collection<MMBiEntB_CRM> cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    @Override
    public boolean isMemberOfCascadeAllField(IEntityB cascadeAll) {
        MMBiEntB_CA entity = (MMBiEntB_CA) cascadeAll;

        Collection<MMBiEntB_CA> collection = getCascadeAll();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeMergeField(IEntityB cascadeMerge) {
        MMBiEntB_CM entity = (MMBiEntB_CM) cascadeMerge;

        Collection<MMBiEntB_CM> collection = getCascadeMerge();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadePersistField(IEntityB cascadePersist) {
        MMBiEntB_CP entity = (MMBiEntB_CP) cascadePersist;

        Collection<MMBiEntB_CP> collection = getCascadePersist();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfDefaultRelationshipField(IEntityB defaultRelationship) {
        MMBiEntB_DR entity = (MMBiEntB_DR) defaultRelationship;

        Collection<MMBiEntB_DR> collection = getDefaultRelationship();

        return (collection.contains(entity));
    }

    @Override
    public boolean sMemberOfCascadeRefreshField(IEntityB cascadeRefresh) {
        MMBiEntB_CRF entity = (MMBiEntB_CRF) cascadeRefresh;

        Collection<MMBiEntB_CRF> collection = getCascadeRefresh();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeRemoveField(IEntityB cascadeRemove) {
        MMBiEntB_CRM entity = (MMBiEntB_CRM) cascadeRemove;

        Collection<MMBiEntB_CRM> collection = getCascadeRemove();

        return (collection.contains(entity));
    }

    @Override
    public Collection getCascadeAllCollectionField() {
        return getCascadeAll();
    }

    @Override
    public Collection getCascadeMergeCollectionField() {
        return getCascadeMerge();
    }

    @Override
    public Collection getCascadePersistCollectionField() {
        return getCascadePersist();
    }

    @Override
    public Collection getCascadeRefreshCollectionField() {
        return getCascadeRefresh();
    }

    @Override
    public Collection getCascadeRemoveCollectionField() {
        return getCascadeRemove();
    }

    @Override
    public Collection getDefaultRelationshipCollectionField() {
        return getDefaultRelationship();
    }

    @Override
    public void insertCascadeAllField(IEntityB cascadeAll) {
        MMBiEntB_CA entity = (MMBiEntB_CA) cascadeAll;

        Collection<MMBiEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.add(entity);
    }

    @Override
    public void insertCascadeMergeField(IEntityB cascadeMerge) {
        MMBiEntB_CM entity = (MMBiEntB_CM) cascadeMerge;

        Collection<MMBiEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.add(entity);
    }

    @Override
    public void insertCascadePersistField(IEntityB cascadePersist) {
        MMBiEntB_CP entity = (MMBiEntB_CP) cascadePersist;

        Collection<MMBiEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.add(entity);
    }

    @Override
    public void insertCascadeRefreshField(IEntityB cascadeRefresh) {
        MMBiEntB_CRF entity = (MMBiEntB_CRF) cascadeRefresh;

        Collection<MMBiEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.add(entity);
    }

    @Override
    public void insertCascadeRemoveField(IEntityB cascadeRemove) {
        MMBiEntB_CRM entity = (MMBiEntB_CRM) cascadeRemove;

        Collection<MMBiEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.add(entity);
    }

    @Override
    public void insertDefaultRelationshipField(IEntityB defaultRelationship) {
        MMBiEntB_DR entity = (MMBiEntB_DR) defaultRelationship;

        Collection<MMBiEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.add(entity);
    }

    @Override
    public void removeCascadeAllField(IEntityB cascadeAll) {
        MMBiEntB_CA entity = (MMBiEntB_CA) cascadeAll;
        Collection<MMBiEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.remove(entity);
    }

    @Override
    public void removeCascadeMergeField(IEntityB cascadeMerge) {
        MMBiEntB_CM entity = (MMBiEntB_CM) cascadeMerge;
        Collection<MMBiEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.remove(entity);
    }

    @Override
    public void removeCascadePersistField(IEntityB cascadePersist) {
        MMBiEntB_CP entity = (MMBiEntB_CP) cascadePersist;
        Collection<MMBiEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.remove(entity);
    }

    @Override
    public void removeCascadeRefreshField(IEntityB cascadeRefresh) {
        MMBiEntB_CRF entity = (MMBiEntB_CRF) cascadeRefresh;
        Collection<MMBiEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.remove(entity);
    }

    @Override
    public void removeCascadeRemoveField(IEntityB cascadeRemove) {
        MMBiEntB_CRM entity = (MMBiEntB_CRM) cascadeRemove;
        Collection<MMBiEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.remove(entity);
    }

    @Override
    public void removeDefaultRelationshipField(IEntityB defaultRelationship) {
        MMBiEntB_DR entity = (MMBiEntB_DR) defaultRelationship;
        Collection<MMBiEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.remove(entity);
    }

    @Override
    public void setCascadeAllField(Collection cascadeAll) {
        Collection<MMBiEntB_CA> cacadeAllCollection = new ArrayList<MMBiEntB_CA>();

        Iterator i = cascadeAll.iterator();
        while (i.hasNext()) {
            MMBiEntB_CA entity = (MMBiEntB_CA) i.next();
            cacadeAllCollection.add(entity);
        }

        setCascadeAll(cacadeAllCollection);
    }

    @Override
    public void setCascadeMergeField(Collection cascadeMerge) {
        Collection<MMBiEntB_CM> cacadeMergeCollection = new ArrayList<MMBiEntB_CM>();

        Iterator i = cascadeMerge.iterator();
        while (i.hasNext()) {
            MMBiEntB_CM entity = (MMBiEntB_CM) i.next();
            cacadeMergeCollection.add(entity);
        }

        setCascadeMerge(cacadeMergeCollection);
    }

    @Override
    public void setCascadePersistField(Collection cascadePersist) {
        Collection<MMBiEntB_CP> cacadePersistCollection = new ArrayList<MMBiEntB_CP>();

        Iterator i = cascadePersist.iterator();
        while (i.hasNext()) {
            MMBiEntB_CP entity = (MMBiEntB_CP) i.next();
            cacadePersistCollection.add(entity);
        }

        setCascadePersist(cacadePersistCollection);
    }

    @Override
    public void setCascadeRefreshField(Collection cascadeRefresh) {
        Collection<MMBiEntB_CRF> cacadeRefreshCollection = new ArrayList<MMBiEntB_CRF>();

        Iterator i = cascadeRefresh.iterator();
        while (i.hasNext()) {
            MMBiEntB_CRF entity = (MMBiEntB_CRF) i.next();
            cacadeRefreshCollection.add(entity);
        }

        setCascadeRefresh(cacadeRefreshCollection);
    }

    @Override
    public void setCascadeRemoveField(Collection cascadeRemove) {
        Collection<MMBiEntB_CRM> cacadeRemoveCollection = new ArrayList<MMBiEntB_CRM>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            MMBiEntB_CRM entity = (MMBiEntB_CRM) i.next();
            cacadeRemoveCollection.add(entity);
        }

        setCascadeRemove(cacadeRemoveCollection);
    }

    @Override
    public void setDefaultRelationshipCollectionField(Collection defaultRelationship) {
        Collection<MMBiEntB_DR> defaultRelationshipCollection = new ArrayList<MMBiEntB_DR>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            MMBiEntB_DR entity = (MMBiEntB_DR) i.next();
            defaultRelationshipCollection.add(entity);
        }

        setDefaultRelationship(defaultRelationshipCollection);
    }

    @Override
    public String toString() {
        return "MMBiEntA [id=" + id + ", name=" + name + "]";
    }

}