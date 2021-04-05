/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IEntityB;

//@Entity
public class XMLOMEntA implements IEntityA {
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
     * One to many mapping with an IEntityB-type entity. No override of the foreign key column name.
     *
     * ManyToMany Config Cascade: default no Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    // @OneToMany
    // @JoinTable(
    // name="OneXManyDRUniJoinTable",
    // joinColumns=@JoinColumn(name="ENT_A"),
    // inverseJoinColumns=@JoinColumn(name="ENT_B"))
    private Collection<XMLOMEntB_DR> defaultRelationship;

    /*
     * Field: CascadeAll
     *
     * One to many mapping with an IEntityB-type entity. This relation field has the CascadeType of ALL.
     *
     * ManyToMany Config Cascade: ALL Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    /*
     * @OneToMany(cascade=CascadeType.ALL)
     *
     * @JoinTable( name="OneXManyCAUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLOMEntB_CA> cascadeAll;

    /*
     * Field: CascadeMerge
     *
     * One to many mapping with an IEntityB-type entity. This relation field has the CascadeType of MERGE.
     *
     * ManyToMany Config Cascade: MERGE Fetch: default lazy (Collection based relationships are lazy loaded). Optional:
     * default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    /*
     * @OneToMany(cascade=CascadeType.MERGE)
     *
     * @JoinTable( name="OneXManyCMUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLOMEntB_CM> cascadeMerge;

    /*
     * Field: CascadePersist
     *
     * One to many mapping with an IEntityB-type entity. This relation field has the CascadeType of PERSIST.
     *
     * ManyToMany Config Cascade: PERSIST Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    /*
     * @OneToMany(cascade=CascadeType.PERSIST)
     *
     * @JoinTable( name="OneXManyCPUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLOMEntB_CP> cascadePersist;

    /*
     * Field: CascadeRefresh
     *
     * One to many mapping with an IEntityB-type entity. This relation field has the CascadeType of REFRESH.
     *
     * ManyToMany Config Cascade: REFRESH Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    /*
     * @OneToMany(cascade=CascadeType.REFRESH)
     *
     * @JoinTable( name="OneXManyCRFUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLOMEntB_CRF> cascadeRefresh;

    /*
     * Field: CascadeRemove
     *
     * One to many mapping with an IEntityB-type entity. This relation field has the CascadeType of REMOVE.
     *
     * ManyToMany Config Cascade: REFRESH Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: Default column name.
     */
    /*
     * @OneToMany(cascade=CascadeType.REMOVE)
     *
     * @JoinTable( name="OneXManyCRMUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLOMEntB_CRM> cascadeRemove;

    public XMLOMEntA() {
        // Initialize the relational collection fields
        defaultRelationship = new ArrayList<XMLOMEntB_DR>();
        cascadeAll = new ArrayList<XMLOMEntB_CA>();
        cascadePersist = new ArrayList<XMLOMEntB_CP>();
        cascadeMerge = new ArrayList<XMLOMEntB_CM>();
        cascadeRefresh = new ArrayList<XMLOMEntB_CRF>();
        cascadeRemove = new ArrayList<XMLOMEntB_CRM>();
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

    public Collection<XMLOMEntB_CA> getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(Collection<XMLOMEntB_CA> cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public Collection<XMLOMEntB_CM> getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(Collection<XMLOMEntB_CM> cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public Collection<XMLOMEntB_CP> getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(Collection<XMLOMEntB_CP> cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public Collection<XMLOMEntB_CRF> getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(Collection<XMLOMEntB_CRF> cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public Collection<XMLOMEntB_DR> getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(Collection<XMLOMEntB_DR> defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public Collection<XMLOMEntB_CRM> getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(Collection<XMLOMEntB_CRM> cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    @Override
    public boolean isMemberOfCascadeAllField(IEntityB cascadeAll) {
        XMLOMEntB_CA entity = (XMLOMEntB_CA) cascadeAll;

        Collection<XMLOMEntB_CA> collection = getCascadeAll();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeMergeField(IEntityB cascadeMerge) {
        XMLOMEntB_CM entity = (XMLOMEntB_CM) cascadeMerge;

        Collection<XMLOMEntB_CM> collection = getCascadeMerge();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadePersistField(IEntityB cascadePersist) {
        XMLOMEntB_CP entity = (XMLOMEntB_CP) cascadePersist;

        Collection<XMLOMEntB_CP> collection = getCascadePersist();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLOMEntB_DR entity = (XMLOMEntB_DR) defaultRelationship;

        Collection<XMLOMEntB_DR> collection = getDefaultRelationship();

        return (collection.contains(entity));
    }

    @Override
    public boolean sMemberOfCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLOMEntB_CRF entity = (XMLOMEntB_CRF) cascadeRefresh;

        Collection<XMLOMEntB_CRF> collection = getCascadeRefresh();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeRemoveField(IEntityB cascadeRemove) {
        XMLOMEntB_CRM entity = (XMLOMEntB_CRM) cascadeRemove;

        Collection<XMLOMEntB_CRM> collection = getCascadeRemove();

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
        XMLOMEntB_CA entity = (XMLOMEntB_CA) cascadeAll;

        Collection<XMLOMEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.add(entity);
    }

    @Override
    public void insertCascadeMergeField(IEntityB cascadeMerge) {
        XMLOMEntB_CM entity = (XMLOMEntB_CM) cascadeMerge;

        Collection<XMLOMEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.add(entity);
    }

    @Override
    public void insertCascadePersistField(IEntityB cascadePersist) {
        XMLOMEntB_CP entity = (XMLOMEntB_CP) cascadePersist;

        Collection<XMLOMEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.add(entity);
    }

    @Override
    public void insertCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLOMEntB_CRF entity = (XMLOMEntB_CRF) cascadeRefresh;

        Collection<XMLOMEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.add(entity);
    }

    @Override
    public void insertCascadeRemoveField(IEntityB cascadeRemove) {
        XMLOMEntB_CRM entity = (XMLOMEntB_CRM) cascadeRemove;

        Collection<XMLOMEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.add(entity);
    }

    @Override
    public void insertDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLOMEntB_DR entity = (XMLOMEntB_DR) defaultRelationship;

        Collection<XMLOMEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.add(entity);
    }

    @Override
    public void removeCascadeAllField(IEntityB cascadeAll) {
        XMLOMEntB_CA entity = (XMLOMEntB_CA) cascadeAll;
        Collection<XMLOMEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.remove(entity);
    }

    @Override
    public void removeCascadeMergeField(IEntityB cascadeMerge) {
        XMLOMEntB_CM entity = (XMLOMEntB_CM) cascadeMerge;
        Collection<XMLOMEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.remove(entity);
    }

    @Override
    public void removeCascadePersistField(IEntityB cascadePersist) {
        XMLOMEntB_CP entity = (XMLOMEntB_CP) cascadePersist;
        Collection<XMLOMEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.remove(entity);
    }

    @Override
    public void removeCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLOMEntB_CRF entity = (XMLOMEntB_CRF) cascadeRefresh;
        Collection<XMLOMEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.remove(entity);
    }

    @Override
    public void removeCascadeRemoveField(IEntityB cascadeRemove) {
        XMLOMEntB_CRM entity = (XMLOMEntB_CRM) cascadeRemove;
        Collection<XMLOMEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.remove(entity);
    }

    @Override
    public void removeDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLOMEntB_DR entity = (XMLOMEntB_DR) defaultRelationship;
        Collection<XMLOMEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.remove(entity);
    }

    @Override
    public void setCascadeAllField(Collection cascadeAll) {
        Collection<XMLOMEntB_CA> cacadeAllCollection = new ArrayList<XMLOMEntB_CA>();

        Iterator i = cascadeAll.iterator();
        while (i.hasNext()) {
            XMLOMEntB_CA entity = (XMLOMEntB_CA) i.next();
            cacadeAllCollection.add(entity);
        }

        setCascadeAll(cacadeAllCollection);
    }

    @Override
    public void setCascadeMergeField(Collection cascadeMerge) {
        Collection<XMLOMEntB_CM> cacadeMergeCollection = new ArrayList<XMLOMEntB_CM>();

        Iterator i = cascadeMerge.iterator();
        while (i.hasNext()) {
            XMLOMEntB_CM entity = (XMLOMEntB_CM) i.next();
            cacadeMergeCollection.add(entity);
        }

        setCascadeMerge(cacadeMergeCollection);
    }

    @Override
    public void setCascadePersistField(Collection cascadePersist) {
        Collection<XMLOMEntB_CP> cacadePersistCollection = new ArrayList<XMLOMEntB_CP>();

        Iterator i = cascadePersist.iterator();
        while (i.hasNext()) {
            XMLOMEntB_CP entity = (XMLOMEntB_CP) i.next();
            cacadePersistCollection.add(entity);
        }

        setCascadePersist(cacadePersistCollection);
    }

    @Override
    public void setCascadeRefreshField(Collection cascadeRefresh) {
        Collection<XMLOMEntB_CRF> cacadeRefreshCollection = new ArrayList<XMLOMEntB_CRF>();

        Iterator i = cascadeRefresh.iterator();
        while (i.hasNext()) {
            XMLOMEntB_CRF entity = (XMLOMEntB_CRF) i.next();
            cacadeRefreshCollection.add(entity);
        }

        setCascadeRefresh(cacadeRefreshCollection);
    }

    @Override
    public void setCascadeRemoveField(Collection cascadeRemove) {
        Collection<XMLOMEntB_CRM> cacadeRemoveCollection = new ArrayList<XMLOMEntB_CRM>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            XMLOMEntB_CRM entity = (XMLOMEntB_CRM) i.next();
            cacadeRemoveCollection.add(entity);
        }

        setCascadeRemove(cacadeRemoveCollection);
    }

    @Override
    public void setDefaultRelationshipCollectionField(Collection defaultRelationship) {
        Collection<XMLOMEntB_DR> defaultRelationshipCollection = new ArrayList<XMLOMEntB_DR>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            XMLOMEntB_DR entity = (XMLOMEntB_DR) i.next();
            defaultRelationshipCollection.add(entity);
        }

        setDefaultRelationship(defaultRelationshipCollection);
    }

    @Override
    public String toString() {
        return "XMLOMEntA [id=" + id + ", name=" + name + "]";
    }

}
