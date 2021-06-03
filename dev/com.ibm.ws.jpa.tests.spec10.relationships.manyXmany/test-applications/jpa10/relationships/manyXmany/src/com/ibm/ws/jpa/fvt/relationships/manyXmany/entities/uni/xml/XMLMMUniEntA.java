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

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityB;

//@Entity
public class XMLMMUniEntA implements IEntityA {
    /**
     * Entity primary key, an integer id number.
     */
    //@Id
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
    /*
     * @ManyToMany
     * 
     * @JoinTable(
     * name="ManyXManyDRUniJoinTable",
     * joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLMMUniEntB_DR> defaultRelationship;

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
/*
 * @ManyToMany(cascade=CascadeType.ALL)
 * 
 * @JoinTable(
 * name="ManyXManyCAUniJoinTable",
 * joinColumns=@JoinColumn(name="ENT_A"),
 * inverseJoinColumns=@JoinColumn(name="ENT_B"))
 */
    private Collection<XMLMMUniEntB_CA> cascadeAll;

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
/*
 * @ManyToMany(cascade=CascadeType.MERGE)
 * 
 * @JoinTable(
 * name="ManyXManyCMUniJoinTable",
 * joinColumns=@JoinColumn(name="ENT_A"),
 * inverseJoinColumns=@JoinColumn(name="ENT_B"))
 */
    private Collection<XMLMMUniEntB_CM> cascadeMerge;

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
/*
 * @ManyToMany(cascade=CascadeType.PERSIST)
 * 
 * @JoinTable(
 * name="ManyXManyCPUniJoinTable",
 * joinColumns=@JoinColumn(name="ENT_A"),
 * inverseJoinColumns=@JoinColumn(name="ENT_B"))
 */
    private Collection<XMLMMUniEntB_CP> cascadePersist;

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
/*
 * @ManyToMany(cascade=CascadeType.REFRESH)
 * 
 * @JoinTable(
 * name="ManyXManyCRFUniJoinTable",
 * joinColumns=@JoinColumn(name="ENT_A"),
 * inverseJoinColumns=@JoinColumn(name="ENT_B"))
 */
    private Collection<XMLMMUniEntB_CRF> cascadeRefresh;

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
/*
 * @ManyToMany(cascade=CascadeType.REMOVE)
 * 
 * @JoinTable(
 * name="ManyXManyCRMUniJoinTable",
 * joinColumns=@JoinColumn(name="ENT_A"),
 * inverseJoinColumns=@JoinColumn(name="ENT_B"))
 */
    private Collection<XMLMMUniEntB_CRM> cascadeRemove;

    public XMLMMUniEntA() {
        // Initialize the relational collection fields
        defaultRelationship = new ArrayList<XMLMMUniEntB_DR>();
        cascadeAll = new ArrayList<XMLMMUniEntB_CA>();
        cascadePersist = new ArrayList<XMLMMUniEntB_CP>();
        cascadeMerge = new ArrayList<XMLMMUniEntB_CM>();
        cascadeRefresh = new ArrayList<XMLMMUniEntB_CRF>();
        cascadeRemove = new ArrayList<XMLMMUniEntB_CRM>();
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

    public Collection<XMLMMUniEntB_CA> getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(Collection<XMLMMUniEntB_CA> cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public Collection<XMLMMUniEntB_CM> getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(Collection<XMLMMUniEntB_CM> cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public Collection<XMLMMUniEntB_CP> getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(Collection<XMLMMUniEntB_CP> cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public Collection<XMLMMUniEntB_CRF> getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(Collection<XMLMMUniEntB_CRF> cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public Collection<XMLMMUniEntB_DR> getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(Collection<XMLMMUniEntB_DR> defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public Collection<XMLMMUniEntB_CRM> getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(Collection<XMLMMUniEntB_CRM> cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    @Override
    public boolean isMemberOfCascadeAllField(IEntityB cascadeAll) {
        XMLMMUniEntB_CA entity = (XMLMMUniEntB_CA) cascadeAll;

        Collection<XMLMMUniEntB_CA> collection = getCascadeAll();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeMergeField(IEntityB cascadeMerge) {
        XMLMMUniEntB_CM entity = (XMLMMUniEntB_CM) cascadeMerge;

        Collection<XMLMMUniEntB_CM> collection = getCascadeMerge();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadePersistField(IEntityB cascadePersist) {
        XMLMMUniEntB_CP entity = (XMLMMUniEntB_CP) cascadePersist;

        Collection<XMLMMUniEntB_CP> collection = getCascadePersist();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLMMUniEntB_DR entity = (XMLMMUniEntB_DR) defaultRelationship;

        Collection<XMLMMUniEntB_DR> collection = getDefaultRelationship();

        return (collection.contains(entity));
    }

    @Override
    public boolean sMemberOfCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLMMUniEntB_CRF entity = (XMLMMUniEntB_CRF) cascadeRefresh;

        Collection<XMLMMUniEntB_CRF> collection = getCascadeRefresh();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeRemoveField(IEntityB cascadeRemove) {
        XMLMMUniEntB_CRM entity = (XMLMMUniEntB_CRM) cascadeRemove;

        Collection<XMLMMUniEntB_CRM> collection = getCascadeRemove();

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
        XMLMMUniEntB_CA entity = (XMLMMUniEntB_CA) cascadeAll;

        Collection<XMLMMUniEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.add(entity);
    }

    @Override
    public void insertCascadeMergeField(IEntityB cascadeMerge) {
        XMLMMUniEntB_CM entity = (XMLMMUniEntB_CM) cascadeMerge;

        Collection<XMLMMUniEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.add(entity);
    }

    @Override
    public void insertCascadePersistField(IEntityB cascadePersist) {
        XMLMMUniEntB_CP entity = (XMLMMUniEntB_CP) cascadePersist;

        Collection<XMLMMUniEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.add(entity);
    }

    @Override
    public void insertCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLMMUniEntB_CRF entity = (XMLMMUniEntB_CRF) cascadeRefresh;

        Collection<XMLMMUniEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.add(entity);
    }

    @Override
    public void insertCascadeRemoveField(IEntityB cascadeRemove) {
        XMLMMUniEntB_CRM entity = (XMLMMUniEntB_CRM) cascadeRemove;

        Collection<XMLMMUniEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.add(entity);
    }

    @Override
    public void insertDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLMMUniEntB_DR entity = (XMLMMUniEntB_DR) defaultRelationship;

        Collection<XMLMMUniEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.add(entity);
    }

    @Override
    public void removeCascadeAllField(IEntityB cascadeAll) {
        XMLMMUniEntB_CA entity = (XMLMMUniEntB_CA) cascadeAll;
        Collection<XMLMMUniEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.remove(entity);
    }

    @Override
    public void removeCascadeMergeField(IEntityB cascadeMerge) {
        XMLMMUniEntB_CM entity = (XMLMMUniEntB_CM) cascadeMerge;
        Collection<XMLMMUniEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.remove(entity);
    }

    @Override
    public void removeCascadePersistField(IEntityB cascadePersist) {
        XMLMMUniEntB_CP entity = (XMLMMUniEntB_CP) cascadePersist;
        Collection<XMLMMUniEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.remove(entity);
    }

    @Override
    public void removeCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLMMUniEntB_CRF entity = (XMLMMUniEntB_CRF) cascadeRefresh;
        Collection<XMLMMUniEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.remove(entity);
    }

    @Override
    public void removeCascadeRemoveField(IEntityB cascadeRemove) {
        XMLMMUniEntB_CRM entity = (XMLMMUniEntB_CRM) cascadeRemove;
        Collection<XMLMMUniEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.remove(entity);
    }

    @Override
    public void removeDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLMMUniEntB_DR entity = (XMLMMUniEntB_DR) defaultRelationship;
        Collection<XMLMMUniEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.remove(entity);
    }

    @Override
    public void setCascadeAllField(Collection cascadeAll) {
        Collection<XMLMMUniEntB_CA> cacadeAllCollection = new ArrayList<XMLMMUniEntB_CA>();

        Iterator i = cascadeAll.iterator();
        while (i.hasNext()) {
            XMLMMUniEntB_CA entity = (XMLMMUniEntB_CA) i.next();
            cacadeAllCollection.add(entity);
        }

        setCascadeAll(cacadeAllCollection);
    }

    @Override
    public void setCascadeMergeField(Collection cascadeMerge) {
        Collection<XMLMMUniEntB_CM> cacadeMergeCollection = new ArrayList<XMLMMUniEntB_CM>();

        Iterator i = cascadeMerge.iterator();
        while (i.hasNext()) {
            XMLMMUniEntB_CM entity = (XMLMMUniEntB_CM) i.next();
            cacadeMergeCollection.add(entity);
        }

        setCascadeMerge(cacadeMergeCollection);
    }

    @Override
    public void setCascadePersistField(Collection cascadePersist) {
        Collection<XMLMMUniEntB_CP> cacadePersistCollection = new ArrayList<XMLMMUniEntB_CP>();

        Iterator i = cascadePersist.iterator();
        while (i.hasNext()) {
            XMLMMUniEntB_CP entity = (XMLMMUniEntB_CP) i.next();
            cacadePersistCollection.add(entity);
        }

        setCascadePersist(cacadePersistCollection);
    }

    @Override
    public void setCascadeRefreshField(Collection cascadeRefresh) {
        Collection<XMLMMUniEntB_CRF> cacadeRefreshCollection = new ArrayList<XMLMMUniEntB_CRF>();

        Iterator i = cascadeRefresh.iterator();
        while (i.hasNext()) {
            XMLMMUniEntB_CRF entity = (XMLMMUniEntB_CRF) i.next();
            cacadeRefreshCollection.add(entity);
        }

        setCascadeRefresh(cacadeRefreshCollection);
    }

    @Override
    public void setCascadeRemoveField(Collection cascadeRemove) {
        Collection<XMLMMUniEntB_CRM> cacadeRemoveCollection = new ArrayList<XMLMMUniEntB_CRM>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            XMLMMUniEntB_CRM entity = (XMLMMUniEntB_CRM) i.next();
            cacadeRemoveCollection.add(entity);
        }

        setCascadeRemove(cacadeRemoveCollection);
    }

    @Override
    public void setDefaultRelationshipCollectionField(Collection defaultRelationship) {
        Collection<XMLMMUniEntB_DR> defaultRelationshipCollection = new ArrayList<XMLMMUniEntB_DR>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            XMLMMUniEntB_DR entity = (XMLMMUniEntB_DR) i.next();
            defaultRelationshipCollection.add(entity);
        }

        setDefaultRelationship(defaultRelationshipCollection);
    }

    @Override
    public String toString() {
        return "XMLMMUniEntA [id=" + id + ", name=" + name + ", defaultRelationship=" + defaultRelationship
               + ", cascadeAll=" + cascadeAll + ", cascadeMerge=" + cascadeMerge + ", cascadePersist="
               + cascadePersist + ", cascadeRefresh=" + cascadeRefresh + ", cascadeRemove=" + cascadeRemove + "]";
    }

}