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

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IEntityB;

//@Entity
public class XMLMMBiEntA implements IEntityA {
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
     * Many to many mapping with an IEntityB-type entity. No override of the foreign key column name.
     *
     * ManyToMany Config Cascade: default no Fetch: default lazy (Collection based relationships are lazy loaded).
     * Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    /*
     * @ManyToMany
     *
     * @JoinTable( name="ManyXManyDRBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLMMBiEntB_DR> defaultRelationship;

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
    // @ManyToMany(cascade=CascadeType.ALL)
    /*
     * @JoinTable( name="ManyXManyCABiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLMMBiEntB_CA> cascadeAll;

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
    // @ManyToMany(cascade=CascadeType.MERGE)
    /*
     * @JoinTable( name="ManyXManyCMBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLMMBiEntB_CM> cascadeMerge;

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
    // @ManyToMany(cascade=CascadeType.PERSIST)
    /*
     * @JoinTable( name="ManyXManyCPBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLMMBiEntB_CP> cascadePersist;

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
    // @ManyToMany(cascade=CascadeType.REFRESH)
    /*
     * @JoinTable( name="ManyXManyCRFBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLMMBiEntB_CRF> cascadeRefresh;

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
    // @ManyToMany(cascade=CascadeType.REMOVE)
    /*
     * @JoinTable( name="ManyXManyCRMBiJoinTable", joinColumns=@JoinColumn(name="ENT_A"),
     * inverseJoinColumns=@JoinColumn(name="ENT_B"))
     */
    private Collection<XMLMMBiEntB_CRM> cascadeRemove;

    public XMLMMBiEntA() {
        // Initialize the relational collection fields
        defaultRelationship = new ArrayList<XMLMMBiEntB_DR>();
        cascadeAll = new ArrayList<XMLMMBiEntB_CA>();
        cascadePersist = new ArrayList<XMLMMBiEntB_CP>();
        cascadeMerge = new ArrayList<XMLMMBiEntB_CM>();
        cascadeRefresh = new ArrayList<XMLMMBiEntB_CRF>();
        cascadeRemove = new ArrayList<XMLMMBiEntB_CRM>();
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

    public Collection<XMLMMBiEntB_CA> getCascadeAll() {
        return cascadeAll;
    }

    public void setCascadeAll(Collection<XMLMMBiEntB_CA> cascadeAll) {
        this.cascadeAll = cascadeAll;
    }

    public Collection<XMLMMBiEntB_CM> getCascadeMerge() {
        return cascadeMerge;
    }

    public void setCascadeMerge(Collection<XMLMMBiEntB_CM> cascadeMerge) {
        this.cascadeMerge = cascadeMerge;
    }

    public Collection<XMLMMBiEntB_CP> getCascadePersist() {
        return cascadePersist;
    }

    public void setCascadePersist(Collection<XMLMMBiEntB_CP> cascadePersist) {
        this.cascadePersist = cascadePersist;
    }

    public Collection<XMLMMBiEntB_CRF> getCascadeRefresh() {
        return cascadeRefresh;
    }

    public void setCascadeRefresh(Collection<XMLMMBiEntB_CRF> cascadeRefresh) {
        this.cascadeRefresh = cascadeRefresh;
    }

    public Collection<XMLMMBiEntB_DR> getDefaultRelationship() {
        return defaultRelationship;
    }

    public void setDefaultRelationship(Collection<XMLMMBiEntB_DR> defaultRelationship) {
        this.defaultRelationship = defaultRelationship;
    }

    public Collection<XMLMMBiEntB_CRM> getCascadeRemove() {
        return cascadeRemove;
    }

    public void setCascadeRemove(Collection<XMLMMBiEntB_CRM> cascadeRemove) {
        this.cascadeRemove = cascadeRemove;
    }

    @Override
    public boolean isMemberOfCascadeAllField(IEntityB cascadeAll) {
        XMLMMBiEntB_CA entity = (XMLMMBiEntB_CA) cascadeAll;

        Collection<XMLMMBiEntB_CA> collection = getCascadeAll();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeMergeField(IEntityB cascadeMerge) {
        XMLMMBiEntB_CM entity = (XMLMMBiEntB_CM) cascadeMerge;

        Collection<XMLMMBiEntB_CM> collection = getCascadeMerge();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadePersistField(IEntityB cascadePersist) {
        XMLMMBiEntB_CP entity = (XMLMMBiEntB_CP) cascadePersist;

        Collection<XMLMMBiEntB_CP> collection = getCascadePersist();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLMMBiEntB_DR entity = (XMLMMBiEntB_DR) defaultRelationship;

        Collection<XMLMMBiEntB_DR> collection = getDefaultRelationship();

        return (collection.contains(entity));
    }

    @Override
    public boolean sMemberOfCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLMMBiEntB_CRF entity = (XMLMMBiEntB_CRF) cascadeRefresh;

        Collection<XMLMMBiEntB_CRF> collection = getCascadeRefresh();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfCascadeRemoveField(IEntityB cascadeRemove) {
        XMLMMBiEntB_CRM entity = (XMLMMBiEntB_CRM) cascadeRemove;

        Collection<XMLMMBiEntB_CRM> collection = getCascadeRemove();

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
        XMLMMBiEntB_CA entity = (XMLMMBiEntB_CA) cascadeAll;

        Collection<XMLMMBiEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.add(entity);
    }

    @Override
    public void insertCascadeMergeField(IEntityB cascadeMerge) {
        XMLMMBiEntB_CM entity = (XMLMMBiEntB_CM) cascadeMerge;

        Collection<XMLMMBiEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.add(entity);
    }

    @Override
    public void insertCascadePersistField(IEntityB cascadePersist) {
        XMLMMBiEntB_CP entity = (XMLMMBiEntB_CP) cascadePersist;

        Collection<XMLMMBiEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.add(entity);
    }

    @Override
    public void insertCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLMMBiEntB_CRF entity = (XMLMMBiEntB_CRF) cascadeRefresh;

        Collection<XMLMMBiEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.add(entity);
    }

    @Override
    public void insertCascadeRemoveField(IEntityB cascadeRemove) {
        XMLMMBiEntB_CRM entity = (XMLMMBiEntB_CRM) cascadeRemove;

        Collection<XMLMMBiEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.add(entity);
    }

    @Override
    public void insertDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLMMBiEntB_DR entity = (XMLMMBiEntB_DR) defaultRelationship;

        Collection<XMLMMBiEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.add(entity);
    }

    @Override
    public void removeCascadeAllField(IEntityB cascadeAll) {
        XMLMMBiEntB_CA entity = (XMLMMBiEntB_CA) cascadeAll;
        Collection<XMLMMBiEntB_CA> cascadeAllCollection = getCascadeAll();
        cascadeAllCollection.remove(entity);
    }

    @Override
    public void removeCascadeMergeField(IEntityB cascadeMerge) {
        XMLMMBiEntB_CM entity = (XMLMMBiEntB_CM) cascadeMerge;
        Collection<XMLMMBiEntB_CM> cascadeMergeCollection = getCascadeMerge();
        cascadeMergeCollection.remove(entity);
    }

    @Override
    public void removeCascadePersistField(IEntityB cascadePersist) {
        XMLMMBiEntB_CP entity = (XMLMMBiEntB_CP) cascadePersist;
        Collection<XMLMMBiEntB_CP> cascadePersistCollection = getCascadePersist();
        cascadePersistCollection.remove(entity);
    }

    @Override
    public void removeCascadeRefreshField(IEntityB cascadeRefresh) {
        XMLMMBiEntB_CRF entity = (XMLMMBiEntB_CRF) cascadeRefresh;
        Collection<XMLMMBiEntB_CRF> cascadeRefreshCollection = getCascadeRefresh();
        cascadeRefreshCollection.remove(entity);
    }

    @Override
    public void removeCascadeRemoveField(IEntityB cascadeRemove) {
        XMLMMBiEntB_CRM entity = (XMLMMBiEntB_CRM) cascadeRemove;
        Collection<XMLMMBiEntB_CRM> cascadeRemoveCollection = getCascadeRemove();
        cascadeRemoveCollection.remove(entity);
    }

    @Override
    public void removeDefaultRelationshipField(IEntityB defaultRelationship) {
        XMLMMBiEntB_DR entity = (XMLMMBiEntB_DR) defaultRelationship;
        Collection<XMLMMBiEntB_DR> defaultRelationshipCollection = getDefaultRelationship();
        defaultRelationshipCollection.remove(entity);
    }

    @Override
    public void setCascadeAllField(Collection cascadeAll) {
        Collection<XMLMMBiEntB_CA> cacadeAllCollection = new ArrayList<XMLMMBiEntB_CA>();

        Iterator i = cascadeAll.iterator();
        while (i.hasNext()) {
            XMLMMBiEntB_CA entity = (XMLMMBiEntB_CA) i.next();
            cacadeAllCollection.add(entity);
        }

        setCascadeAll(cacadeAllCollection);
    }

    @Override
    public void setCascadeMergeField(Collection cascadeMerge) {
        Collection<XMLMMBiEntB_CM> cacadeMergeCollection = new ArrayList<XMLMMBiEntB_CM>();

        Iterator i = cascadeMerge.iterator();
        while (i.hasNext()) {
            XMLMMBiEntB_CM entity = (XMLMMBiEntB_CM) i.next();
            cacadeMergeCollection.add(entity);
        }

        setCascadeMerge(cacadeMergeCollection);
    }

    @Override
    public void setCascadePersistField(Collection cascadePersist) {
        Collection<XMLMMBiEntB_CP> cacadePersistCollection = new ArrayList<XMLMMBiEntB_CP>();

        Iterator i = cascadePersist.iterator();
        while (i.hasNext()) {
            XMLMMBiEntB_CP entity = (XMLMMBiEntB_CP) i.next();
            cacadePersistCollection.add(entity);
        }

        setCascadePersist(cacadePersistCollection);
    }

    @Override
    public void setCascadeRefreshField(Collection cascadeRefresh) {
        Collection<XMLMMBiEntB_CRF> cacadeRefreshCollection = new ArrayList<XMLMMBiEntB_CRF>();

        Iterator i = cascadeRefresh.iterator();
        while (i.hasNext()) {
            XMLMMBiEntB_CRF entity = (XMLMMBiEntB_CRF) i.next();
            cacadeRefreshCollection.add(entity);
        }

        setCascadeRefresh(cacadeRefreshCollection);
    }

    @Override
    public void setCascadeRemoveField(Collection cascadeRemove) {
        Collection<XMLMMBiEntB_CRM> cacadeRemoveCollection = new ArrayList<XMLMMBiEntB_CRM>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            XMLMMBiEntB_CRM entity = (XMLMMBiEntB_CRM) i.next();
            cacadeRemoveCollection.add(entity);
        }

        setCascadeRemove(cacadeRemoveCollection);
    }

    @Override
    public void setDefaultRelationshipCollectionField(Collection defaultRelationship) {
        Collection<XMLMMBiEntB_DR> defaultRelationshipCollection = new ArrayList<XMLMMBiEntB_DR>();

        Iterator i = cascadeRemove.iterator();
        while (i.hasNext()) {
            XMLMMBiEntB_DR entity = (XMLMMBiEntB_DR) i.next();
            defaultRelationshipCollection.add(entity);
        }

        setDefaultRelationship(defaultRelationshipCollection);
    }

    @Override
    public String toString() {
        return "XMLMMBiEntA [id=" + id + ", name=" + name + "]";
    }
}