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

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation;

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
public class MMUniEntA implements IEntityA {
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
     * ManyToMany Config
     * Cascade: default no
     * Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     * 
     * JoinColumn Config (complete default, so no JoinColumn annotation)
     * Name: Default column name.
     */
    @ManyToMany
    @JoinTable(
               name = "ManyXManyDRUniJoinTable",
               joinColumns = @JoinColumn(name = "ENT_A"),
               inverseJoinColumns = @JoinColumn(name = "ENT_B"))
    private Collection<MMUniEntB_DR> defaultRelationship;

    /*
     * Field: CascadeAll
     * 
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of ALL.
     * 
     * ManyToMany Config
     * Cascade: ALL
     * Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     * 
     * JoinColumn Config
     * Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.ALL)
    /*
     * @JoinTable(
     * name="ManyXManyCAUniJoinTable",
     * joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMUniEntB_CA> cascadeAll;

    /*
     * Field: CascadeMerge
     * 
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of MERGE.
     * 
     * ManyToMany Config
     * Cascade: MERGE
     * Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     * 
     * JoinColumn Config
     * Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.MERGE)
    /*
     * @JoinTable(
     * name="ManyXManyCMUniJoinTable",
     * joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMUniEntB_CM> cascadeMerge;

    /*
     * Field: CascadePersist
     * 
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of PERSIST.
     * 
     * ManyToMany Config
     * Cascade: PERSIST
     * Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     * 
     * JoinColumn Config
     * Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.PERSIST)
    /*
     * @JoinTable(
     * name="ManyXManyCPUniJoinTable",
     * joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMUniEntB_CP> cascadePersist;

    /*
     * Field: CascadeRefresh
     * 
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of REFRESH.
     * 
     * ManyToMany Config
     * Cascade: REFRESH
     * Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     * 
     * JoinColumn Config
     * Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.REFRESH)
    /*
     * @JoinTable(
     * name="ManyXManyCRFUniJoinTable",
     * joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMUniEntB_CRF> cascadeRefresh;

    /*
     * Field: CascadeRemove
     * 
     * Many to many mapping with an IEntityB-type entity. This relation field has the CascadeType of REMOVE.
     * 
     * ManyToMany Config
     * Cascade: REFRESH
     * Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     * 
     * JoinColumn Config
     * Name: Default column name.
     */
    @ManyToMany(cascade = CascadeType.REMOVE)
    /*
     * @JoinTable(
     * name="ManyXManyCRMUniJoinTable",
     * joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<MMUniEntB_CRM> cascadeRemove;

    public MMUniEntA() {
        // Initialize the relational collection fields
        defaultRelationship = new ArrayList<MMUniEntB_DR>();
        cascadeAll = new ArrayList<MMUniEntB_CA>();
        cascadePersist = new ArrayList<MMUniEntB_CP>();
        cascadeMerge = new ArrayList<MMUniEntB_CM>();
        cascadeRefresh = new ArrayList<MMUniEntB_CRF>();
        cascadeRemove = new ArrayList<MMUniEntB_CRM>();
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

    public Collection<MMUniEntB_CA> getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(Collection<MMUniEntB_CA> cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public Collection<MMUniEntB_CM> getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(Collection<MMUniEntB_CM> cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public Collection<MMUniEntB_CP> getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(Collection<MMUniEntB_CP> cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public Collection<MMUniEntB_CRF> getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(Collection<MMUniEntB_CRF> cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public Collection<MMUniEntB_DR> getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(Collection<MMUniEntB_DR> defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public Collection<MMUniEntB_CRM> getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(Collection<MMUniEntB_CRM> cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    @Override
    public boolean isMemberOfCascadeAllField(IEntityB cascadeAll) {
        MMUniEntB_CA entity = (MMUniEntB_CA) cascadeAll;

        Collection<MMUniEntB_CA> collection = getCascadeAll();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeMergeField(IEntityB cascadeMerge) {
        MMUniEntB_CM entity = (MMUniEntB_CM) cascadeMerge;

        Collection<MMUniEntB_CM> collection = getCascadeMerge();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadePersistField(IEntityB cascadePersist) {
        MMUniEntB_CP entity = (MMUniEntB_CP) cascadePersist;

        Collection<MMUniEntB_CP> collection = getCascadePersist();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfDefaultRelationshipField(IEntityB defaultRelationship) {
        MMUniEntB_DR entity = (MMUniEntB_DR) defaultRelationship;

        Collection<MMUniEntB_DR> collection = getDefaultRelationship();

        return (collection.contains(entity));
    }

    @Override
    public boolean sMemberOfCascadeRefreshField(IEntityB cascadeRefresh) {
        MMUniEntB_CRF entity = (MMUniEntB_CRF) cascadeRefresh;

        Collection<MMUniEntB_CRF> collection = getCascadeRefresh();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeRemoveField(IEntityB cascadeRemove) {
        MMUniEntB_CRM entity = (MMUniEntB_CRM) cascadeRemove;

        Collection<MMUniEntB_CRM> collection = getCascadeRemove();

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
        MMUniEntB_CA entity = (MMUniEntB_CA) cascadeAll;

        Collection<MMUniEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.add(entity);
    }

    @Override
    public void insertCascadeMergeField(IEntityB cascadeMerge) {
        MMUniEntB_CM entity = (MMUniEntB_CM) cascadeMerge;

        Collection<MMUniEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.add(entity);
    }

    @Override
    public void insertCascadePersistField(IEntityB cascadePersist) {
        MMUniEntB_CP entity = (MMUniEntB_CP) cascadePersist;

        Collection<MMUniEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.add(entity);
    }

    @Override
    public void insertCascadeRefreshField(IEntityB cascadeRefresh) {
        MMUniEntB_CRF entity = (MMUniEntB_CRF) cascadeRefresh;

        Collection<MMUniEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.add(entity);
    }

    @Override
    public void insertCascadeRemoveField(IEntityB cascadeRemove) {
        MMUniEntB_CRM entity = (MMUniEntB_CRM) cascadeRemove;

        Collection<MMUniEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.add(entity);
    }

    @Override
    public void insertDefaultRelationshipField(IEntityB defaultRelationship) {
        MMUniEntB_DR entity = (MMUniEntB_DR) defaultRelationship;

        Collection<MMUniEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.add(entity);
    }

    @Override
    public void removeCascadeAllField(IEntityB cascadeAll) {
        MMUniEntB_CA entity = (MMUniEntB_CA) cascadeAll;
        Collection<MMUniEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.remove(entity);
    }

    @Override
    public void removeCascadeMergeField(IEntityB cascadeMerge) {
        MMUniEntB_CM entity = (MMUniEntB_CM) cascadeMerge;
        Collection<MMUniEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.remove(entity);
    }

    @Override
    public void removeCascadePersistField(IEntityB cascadePersist) {
        MMUniEntB_CP entity = (MMUniEntB_CP) cascadePersist;
        Collection<MMUniEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.remove(entity);
    }

    @Override
    public void removeCascadeRefreshField(IEntityB cascadeRefresh) {
        MMUniEntB_CRF entity = (MMUniEntB_CRF) cascadeRefresh;
        Collection<MMUniEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.remove(entity);
    }

    @Override
    public void removeCascadeRemoveField(IEntityB cascadeRemove) {
        MMUniEntB_CRM entity = (MMUniEntB_CRM) cascadeRemove;
        Collection<MMUniEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.remove(entity);
    }

    @Override
    public void removeDefaultRelationshipField(IEntityB defaultRelationship) {
        MMUniEntB_DR entity = (MMUniEntB_DR) defaultRelationship;
        Collection<MMUniEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.remove(entity);
    }

    @Override
    public void setCascadeAllField(Collection cascadeAll) {
        Collection<MMUniEntB_CA> cacadeAllCollection = new ArrayList<MMUniEntB_CA>();

        Iterator i = cascadeAll.iterator();
        while (i.hasNext()) {
            MMUniEntB_CA entity = (MMUniEntB_CA) i.next();
            cacadeAllCollection.add(entity);
        }

        setCascadeAll(cacadeAllCollection);
    }

    @Override
    public void setCascadeMergeField(Collection cascadeMerge) {
        Collection<MMUniEntB_CM> cacadeMergeCollection = new ArrayList<MMUniEntB_CM>();

        Iterator i = cascadeMerge.iterator();
        while (i.hasNext()) {
            MMUniEntB_CM entity = (MMUniEntB_CM) i.next();
            cacadeMergeCollection.add(entity);
        }

        setCascadeMerge(cacadeMergeCollection);
    }

    @Override
    public void setCascadePersistField(Collection cascadePersist) {
        Collection<MMUniEntB_CP> cacadePersistCollection = new ArrayList<MMUniEntB_CP>();

        Iterator i = cascadePersist.iterator();
        while (i.hasNext()) {
            MMUniEntB_CP entity = (MMUniEntB_CP) i.next();
            cacadePersistCollection.add(entity);
        }

        setCascadePersist(cacadePersistCollection);
    }

    @Override
    public void setCascadeRefreshField(Collection cascadeRefresh) {
        Collection<MMUniEntB_CRF> cacadeRefreshCollection = new ArrayList<MMUniEntB_CRF>();

        Iterator i = cascadeRefresh.iterator();
        while (i.hasNext()) {
            MMUniEntB_CRF entity = (MMUniEntB_CRF) i.next();
            cacadeRefreshCollection.add(entity);
        }

        setCascadeRefresh(cacadeRefreshCollection);
    }

    @Override
    public void setCascadeRemoveField(Collection cascadeRemove) {
        Collection<MMUniEntB_CRM> cacadeRemoveCollection = new ArrayList<MMUniEntB_CRM>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            MMUniEntB_CRM entity = (MMUniEntB_CRM) i.next();
            cacadeRemoveCollection.add(entity);
        }

        setCascadeRemove(cacadeRemoveCollection);
    }

    @Override
    public void setDefaultRelationshipCollectionField(Collection defaultRelationship) {
        Collection<MMUniEntB_DR> defaultRelationshipCollection = new ArrayList<MMUniEntB_DR>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            MMUniEntB_DR entity = (MMUniEntB_DR) i.next();
            defaultRelationshipCollection.add(entity);
        }

        setDefaultRelationship(defaultRelationshipCollection);
    }

    @Override
    public String toString() {
        return "MMUniEntA [id=" + id + ", name=" + name + ", defaultRelationship=" + defaultRelationship
               + ", cascadeAll=" + cascadeAll + ", cascadeMerge=" + cascadeMerge + ", cascadePersist="
               + cascadePersist + ", cascadeRefresh=" + cascadeRefresh + ", cascadeRemove=" + cascadeRemove + "]";
    }

}