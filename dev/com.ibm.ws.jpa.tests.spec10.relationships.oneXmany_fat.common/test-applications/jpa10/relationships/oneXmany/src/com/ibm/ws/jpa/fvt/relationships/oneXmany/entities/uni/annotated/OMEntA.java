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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IEntityB;

@Entity
public class OMEntA implements IEntityA {
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
     * One to many mapping with an IEntityB-type entity. No override of the foreign key column name.
     *
     * ManyToMany Config Cascade: default no Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    @OneToMany
    @JoinTable(name = "OneXManyDRUniJoinTable", joinColumns = @JoinColumn(name = "ENT_A"), inverseJoinColumns = @JoinColumn(name = "ENT_B"))
    private Collection<OMEntB_DR> defaultRelationship;

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
    @OneToMany(cascade = CascadeType.ALL)
    /*
     * @JoinTable( name="OneXManyCAUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<OMEntB_CA> cascadeAll;

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
    @OneToMany(cascade = CascadeType.MERGE)
    /*
     * @JoinTable( name="OneXManyCMUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<OMEntB_CM> cascadeMerge;

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
    @OneToMany(cascade = CascadeType.PERSIST)
    /*
     * @JoinTable( name="OneXManyCPUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<OMEntB_CP> cascadePersist;

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
    @OneToMany(cascade = CascadeType.REFRESH)
    /*
     * @JoinTable( name="OneXManyCRFUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<OMEntB_CRF> cascadeRefresh;

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
    @OneToMany(cascade = CascadeType.REMOVE)
    /*
     * @JoinTable( name="OneXManyCRMUniJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<OMEntB_CRM> cascadeRemove;

    public OMEntA() {
        // Initialize the relational collection fields
        defaultRelationship = new ArrayList<OMEntB_DR>();
        cascadeAll = new ArrayList<OMEntB_CA>();
        cascadePersist = new ArrayList<OMEntB_CP>();
        cascadeMerge = new ArrayList<OMEntB_CM>();
        cascadeRefresh = new ArrayList<OMEntB_CRF>();
        cascadeRemove = new ArrayList<OMEntB_CRM>();
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

    public Collection<OMEntB_CA> getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(Collection<OMEntB_CA> cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public Collection<OMEntB_CM> getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(Collection<OMEntB_CM> cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public Collection<OMEntB_CP> getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(Collection<OMEntB_CP> cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public Collection<OMEntB_CRF> getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(Collection<OMEntB_CRF> cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public Collection<OMEntB_DR> getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(Collection<OMEntB_DR> defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public Collection<OMEntB_CRM> getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(Collection<OMEntB_CRM> cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    @Override
    public boolean isMemberOfCascadeAllField(IEntityB cascadeAll) {
        OMEntB_CA entity = (OMEntB_CA) cascadeAll;

        Collection<OMEntB_CA> collection = getCascadeAll();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeMergeField(IEntityB cascadeMerge) {
        OMEntB_CM entity = (OMEntB_CM) cascadeMerge;

        Collection<OMEntB_CM> collection = getCascadeMerge();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadePersistField(IEntityB cascadePersist) {
        OMEntB_CP entity = (OMEntB_CP) cascadePersist;

        Collection<OMEntB_CP> collection = getCascadePersist();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfDefaultRelationshipField(IEntityB defaultRelationship) {
        OMEntB_DR entity = (OMEntB_DR) defaultRelationship;

        Collection<OMEntB_DR> collection = getDefaultRelationship();

        return (collection.contains(entity));
    }

    @Override
    public boolean sMemberOfCascadeRefreshField(IEntityB cascadeRefresh) {
        OMEntB_CRF entity = (OMEntB_CRF) cascadeRefresh;

        Collection<OMEntB_CRF> collection = getCascadeRefresh();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeRemoveField(IEntityB cascadeRemove) {
        OMEntB_CRM entity = (OMEntB_CRM) cascadeRemove;

        Collection<OMEntB_CRM> collection = getCascadeRemove();

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
        OMEntB_CA entity = (OMEntB_CA) cascadeAll;

        Collection<OMEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.add(entity);
    }

    @Override
    public void insertCascadeMergeField(IEntityB cascadeMerge) {
        OMEntB_CM entity = (OMEntB_CM) cascadeMerge;

        Collection<OMEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.add(entity);
    }

    @Override
    public void insertCascadePersistField(IEntityB cascadePersist) {
        OMEntB_CP entity = (OMEntB_CP) cascadePersist;

        Collection<OMEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.add(entity);
    }

    @Override
    public void insertCascadeRefreshField(IEntityB cascadeRefresh) {
        OMEntB_CRF entity = (OMEntB_CRF) cascadeRefresh;

        Collection<OMEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.add(entity);
    }

    @Override
    public void insertCascadeRemoveField(IEntityB cascadeRemove) {
        OMEntB_CRM entity = (OMEntB_CRM) cascadeRemove;

        Collection<OMEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.add(entity);
    }

    @Override
    public void insertDefaultRelationshipField(IEntityB defaultRelationship) {
        OMEntB_DR entity = (OMEntB_DR) defaultRelationship;

        Collection<OMEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.add(entity);
    }

    @Override
    public void removeCascadeAllField(IEntityB cascadeAll) {
        OMEntB_CA entity = (OMEntB_CA) cascadeAll;
        Collection<OMEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.remove(entity);
    }

    @Override
    public void removeCascadeMergeField(IEntityB cascadeMerge) {
        OMEntB_CM entity = (OMEntB_CM) cascadeMerge;
        Collection<OMEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.remove(entity);
    }

    @Override
    public void removeCascadePersistField(IEntityB cascadePersist) {
        OMEntB_CP entity = (OMEntB_CP) cascadePersist;
        Collection<OMEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.remove(entity);
    }

    @Override
    public void removeCascadeRefreshField(IEntityB cascadeRefresh) {
        OMEntB_CRF entity = (OMEntB_CRF) cascadeRefresh;
        Collection<OMEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.remove(entity);
    }

    @Override
    public void removeCascadeRemoveField(IEntityB cascadeRemove) {
        OMEntB_CRM entity = (OMEntB_CRM) cascadeRemove;
        Collection<OMEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.remove(entity);
    }

    @Override
    public void removeDefaultRelationshipField(IEntityB defaultRelationship) {
        OMEntB_DR entity = (OMEntB_DR) defaultRelationship;
        Collection<OMEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.remove(entity);
    }

    @Override
    public void setCascadeAllField(Collection cascadeAll) {
        Collection<OMEntB_CA> cacadeAllCollection = new ArrayList<OMEntB_CA>();

        Iterator i = cascadeAll.iterator();
        while (i.hasNext()) {
            OMEntB_CA entity = (OMEntB_CA) i.next();
            cacadeAllCollection.add(entity);
        }

        setCascadeAll(cacadeAllCollection);
    }

    @Override
    public void setCascadeMergeField(Collection cascadeMerge) {
        Collection<OMEntB_CM> cacadeMergeCollection = new ArrayList<OMEntB_CM>();

        Iterator i = cascadeMerge.iterator();
        while (i.hasNext()) {
            OMEntB_CM entity = (OMEntB_CM) i.next();
            cacadeMergeCollection.add(entity);
        }

        setCascadeMerge(cacadeMergeCollection);
    }

    @Override
    public void setCascadePersistField(Collection cascadePersist) {
        Collection<OMEntB_CP> cacadePersistCollection = new ArrayList<OMEntB_CP>();

        Iterator i = cascadePersist.iterator();
        while (i.hasNext()) {
            OMEntB_CP entity = (OMEntB_CP) i.next();
            cacadePersistCollection.add(entity);
        }

        setCascadePersist(cacadePersistCollection);
    }

    @Override
    public void setCascadeRefreshField(Collection cascadeRefresh) {
        Collection<OMEntB_CRF> cacadeRefreshCollection = new ArrayList<OMEntB_CRF>();

        Iterator i = cascadeRefresh.iterator();
        while (i.hasNext()) {
            OMEntB_CRF entity = (OMEntB_CRF) i.next();
            cacadeRefreshCollection.add(entity);
        }

        setCascadeRefresh(cacadeRefreshCollection);
    }

    @Override
    public void setCascadeRemoveField(Collection cascadeRemove) {
        Collection<OMEntB_CRM> cacadeRemoveCollection = new ArrayList<OMEntB_CRM>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            OMEntB_CRM entity = (OMEntB_CRM) i.next();
            cacadeRemoveCollection.add(entity);
        }

        setCascadeRemove(cacadeRemoveCollection);
    }

    @Override
    public void setDefaultRelationshipCollectionField(Collection defaultRelationship) {
        Collection<OMEntB_DR> defaultRelationshipCollection = new ArrayList<OMEntB_DR>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            OMEntB_DR entity = (OMEntB_DR) i.next();
            defaultRelationshipCollection.add(entity);
        }

        setDefaultRelationship(defaultRelationshipCollection);
    }

    @Override
    public String toString() {
        return "OMEntA [id=" + id + ", name=" + name + "]";
    }

}
