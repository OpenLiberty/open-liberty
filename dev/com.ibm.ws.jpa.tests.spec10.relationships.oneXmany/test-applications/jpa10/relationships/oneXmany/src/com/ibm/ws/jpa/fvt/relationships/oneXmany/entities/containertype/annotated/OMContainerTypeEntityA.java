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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.annotated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IContainerTypeEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IContainerTypeEntityB;

@Entity
public class OMContainerTypeEntityA implements IContainerTypeEntityA {
    @Id
    private int id;

    private String name;

    // Collection Type
    @OneToMany
    @JoinTable(name = "OMCTEA_GCT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    private Collection<OMContainerTypeEntityB> genericizedCollectionType;

    @OneToMany(targetEntity = OMContainerTypeEntityB.class)
    @JoinTable(name = "OMCTEA_UGCT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    private Collection ungenericizedCollectionType;

    // Set Type
    @OneToMany
    @JoinTable(name = "OMCTEA_GST",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    private Set<OMContainerTypeEntityB> genericizedSetType;

    @OneToMany(targetEntity = OMContainerTypeEntityB.class)
    @JoinTable(name = "OMCTEA_UGST",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    private Set ungenericizedSetType;

    // List Type
    @OneToMany
    @JoinTable(name = "OMCTEA_GLT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    private List<OMContainerTypeEntityB> genericizedListType;

    @OneToMany(targetEntity = OMContainerTypeEntityB.class)
    @JoinTable(name = "OMCTEA_UGLT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    private List ungenericizedListType;

    @OneToMany
    @JoinTable(name = "OMCTEA_OLT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    @OrderBy("name ASC")
    private List<OMContainerTypeEntityB> orderedListType;

    // Map Type
    @OneToMany
    @JoinTable(name = "OMCTEA_GMT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    private Map<Integer, OMContainerTypeEntityB> genericizedMapType;

//    @OneToMany(targetEntity = OMContainerTypeEntityB.class)
//    @JoinTable(name = "OMCTEA_UGMT")
//    private Map ungenericizedMapType;

    @OneToMany
    @JoinTable(name = "OMCTEA_GMKT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    @MapKey(name = "name")
    private Map<String, OMContainerTypeEntityB> genericizedMapWithKeyType;

    @OneToMany(targetEntity = OMContainerTypeEntityB.class)
    @JoinTable(name = "OMCTEA_UGMKT",
               joinColumns = @JoinColumn(name = "OMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "OMContainerTypeEntityB_ID"))
    @MapKey(name = "name")
    private Map ungenericizedMapWithKeyType;

    public OMContainerTypeEntityA() {
        id = 0;
        name = "";

        genericizedCollectionType = new ArrayList<OMContainerTypeEntityB>();
        ungenericizedCollectionType = new ArrayList();
        genericizedSetType = new HashSet<OMContainerTypeEntityB>();
        ungenericizedSetType = new HashSet();
        genericizedListType = new ArrayList<OMContainerTypeEntityB>();
        ungenericizedListType = new ArrayList();
        orderedListType = new ArrayList<OMContainerTypeEntityB>();
        genericizedMapType = new HashMap<Integer, OMContainerTypeEntityB>();
//        ungenericizedMapType = new HashMap();
        genericizedMapWithKeyType = new HashMap<String, OMContainerTypeEntityB>();
        ungenericizedMapWithKeyType = new HashMap();
    }

    public Collection<OMContainerTypeEntityB> getGenericizedCollectionType() {
        return genericizedCollectionType;
    }

    public void setGenericizedCollectionType(Collection<OMContainerTypeEntityB> genericizedCollectionType) {
        this.genericizedCollectionType = genericizedCollectionType;
    }

    public List<OMContainerTypeEntityB> getGenericizedListType() {
        return genericizedListType;
    }

    public void setGenericizedListType(List<OMContainerTypeEntityB> genericizedListType) {
        this.genericizedListType = genericizedListType;
    }

    public Map<Integer, OMContainerTypeEntityB> getGenericizedMapType() {
        return genericizedMapType;
    }

    public void setGenericizedMapType(Map<Integer, OMContainerTypeEntityB> genericizedMapType) {
        this.genericizedMapType = genericizedMapType;
    }

    public Map<String, OMContainerTypeEntityB> getGenericizedMapWithKeyType() {
        return genericizedMapWithKeyType;
    }

    public void setGenericizedMapWithKeyType(Map<String, OMContainerTypeEntityB> genericizedMapWithKeyType) {
        this.genericizedMapWithKeyType = genericizedMapWithKeyType;
    }

    public Set<OMContainerTypeEntityB> getGenericizedSetType() {
        return genericizedSetType;
    }

    public void setGenericizedSetType(Set<OMContainerTypeEntityB> genericizedSetType) {
        this.genericizedSetType = genericizedSetType;
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

    public List<OMContainerTypeEntityB> getOrderedListType() {
        return orderedListType;
    }

    public void setOrderedListType(List<OMContainerTypeEntityB> orderedListType) {
        this.orderedListType = orderedListType;
    }

    public Collection getUngenericizedCollectionType() {
        return ungenericizedCollectionType;
    }

    public void setUngenericizedCollectionType(Collection ungenericizedCollectionType) {
        this.ungenericizedCollectionType = ungenericizedCollectionType;
    }

    public List getUngenericizedListType() {
        return ungenericizedListType;
    }

    public void setUngenericizedListType(List ungenericizedListType) {
        this.ungenericizedListType = ungenericizedListType;
    }

//    public Map getUngenericizedMapType() {
//        return ungenericizedMapType;
//    }
//
//    public void setUngenericizedMapType(Map ungenericizedMapType) {
//        this.ungenericizedMapType = ungenericizedMapType;
//    }

    public Map getUngenericizedMapWithKeyType() {
        return ungenericizedMapWithKeyType;
    }

    public void setUngenericizedMapWithKeyType(Map ungenericizedMapWithKeyType) {
        this.ungenericizedMapWithKeyType = ungenericizedMapWithKeyType;
    }

    public Set getUngenericizedSetType() {
        return ungenericizedSetType;
    }

    public void setUngenericizedSetType(Set ungenericizedSetType) {
        this.ungenericizedSetType = ungenericizedSetType;
    }

    @Override
    public Collection getGenericizedCollectionTypeCollectionField() {
        return getGenericizedCollectionType();
    }

    @Override
    public List getGenericizedListTypeCollectionField() {
        return getGenericizedListType();
    }

    @Override
    public Map getGenericizedMapTypeCollectionField() {
        return getGenericizedMapType();
    }

    @Override
    public Map getGenericizedMapWithKeyTypeCollectionField() {
        return getGenericizedMapWithKeyType();
    }

    @Override
    public Set getGenericizedSetTypeCollectionField() {
        return getGenericizedSetType();
    }

    @Override
    public List getOrderedListTypeCollectionField() {
        return getOrderedListType();
    }

    @Override
    public Collection getUngenericizedCollectionTypeCollectionField() {
        return getUngenericizedCollectionType();
    }

    @Override
    public List getUngenericizedListTypeCollectionField() {
        return getUngenericizedListType();
    }

//    public Map getUngenericizedMapTypeCollectionField() {
//        return getUngenericizedMapType();
//    }

    @Override
    public Map getUngenericizedMapWithKeyTypeCollectionField() {
        return getUngenericizedMapWithKeyType();
    }

    @Override
    public Set getUngenericizedSetTypeCollectionField() {
        return getUngenericizedSetType();
    }

    @Override
    public void insertGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedCollectionType;

        Collection<OMContainerTypeEntityB> genericizedCollectionTypeCollection = getGenericizedCollectionType();
        genericizedCollectionTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedListType;

        List<OMContainerTypeEntityB> genericizedListTypeCollection = getGenericizedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedMapType;

        Map<Integer, OMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.put(entity.getId(), entity);
    }

    @Override
    public void insertGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, OMContainerTypeEntityB> genericizedMapTypeWithKeyCollection = getGenericizedMapWithKeyType();
        genericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    public void insertGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedSetType;

        Set<OMContainerTypeEntityB> genericizedSetTypeCollection = getGenericizedSetType();
        genericizedSetTypeCollection.add(entity);

    }

    @Override
    public void insertOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) orderedListType;

        List<OMContainerTypeEntityB> genericizedListTypeCollection = getOrderedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedCollectionType;

        Collection ungenericizedCollectionTypeCollection = getUngenericizedCollectionType();
        ungenericizedCollectionTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedListType;

        List ungenericizedListTypeCollection = getUngenericizedListType();
        ungenericizedListTypeCollection.add(entity);
    }

//    @SuppressWarnings("unchecked")
//    public void insertUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.put(entity.getId(), entity);
//    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapTypeWithKeyCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedSetType;

        Set ungenericizedSetTypeCollection = getUngenericizedSetType();
        ungenericizedSetTypeCollection.add(entity);
    }

    @Override
    public boolean isMemberOfGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedCollectionType;

        Collection<OMContainerTypeEntityB> collection = getGenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedListType;

        List<OMContainerTypeEntityB> collection = getGenericizedListType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedMapType;

        Map<Integer, OMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();

        return (genericizedMapTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, OMContainerTypeEntityB> genericizedMapWithKeTypeCollection = getGenericizedMapWithKeyType();

        return (genericizedMapWithKeTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedSetType;

        Set<OMContainerTypeEntityB> set = getGenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public boolean isMemberOfOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) orderedListType;

        List<OMContainerTypeEntityB> orderedList = getOrderedListType();

        return (orderedList.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedCollectionType;

        Collection collection = getUngenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedListType;

        List list = getUngenericizedListType();

        return (list.contains(entity));
    }

//    public boolean isMemberOfUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//
//        return (ungenericizedMapTypeCollection.containsValue(entity));
//    }

    @Override
    public boolean isMemberOfUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();

        return (ungenericizedMapWithKeyTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedSetType;

        Set set = getUngenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public void removeGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedCollectionType;
        Collection<OMContainerTypeEntityB> genericizedCollectionCollection = getGenericizedCollectionType();
        genericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedListType;
        List<OMContainerTypeEntityB> genericizedListCollection = getGenericizedListType();
        genericizedListCollection.remove(entity);
    }

    @Override
    public void removeGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedMapType;
        Map<Integer, OMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.remove(entity.getId());
    }

    @Override
    public void removeGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedMapWithKeyType;
        Map<String, OMContainerTypeEntityB> genericizedMapWithKeyTypeCollection = getGenericizedMapWithKeyType();
        genericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) genericizedSetType;
        Set<OMContainerTypeEntityB> genericizedSetCollection = getGenericizedSetType();
        genericizedSetCollection.remove(entity);
    }

    @Override
    public void removeOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) orderedListType;
        List<OMContainerTypeEntityB> orderedListCollection = getOrderedListType();
        orderedListCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedCollectionType;
        Collection ungenericizedCollectionCollection = getUngenericizedCollectionType();
        ungenericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedListType;
        List ungenericizedListCollection = getUngenericizedListType();
        ungenericizedListCollection.remove(entity);
    }

//    public void removeUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedMapType;
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.remove(entity.getId());
//    }

    @Override
    public void removeUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedMapWithKeyType;
        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        OMContainerTypeEntityB entity = (OMContainerTypeEntityB) ungenericizedSetType;
        Set ungenericizedSetCollection = getUngenericizedSetType();
        ungenericizedSetCollection.remove(entity);
    }

    @Override
    public void setGenericizedCollectionTypeCollectionField(Collection genericizedCollectionType) {
        Collection<OMContainerTypeEntityB> genericizedCollectionCollection = new ArrayList<OMContainerTypeEntityB>();

        Iterator i = genericizedCollectionType.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            genericizedCollectionCollection.add(entity);
        }

        setGenericizedCollectionType(genericizedCollectionCollection);
    }

    @Override
    public void setGenericizedListTypeSetField(List genericizedListType) {
        List<OMContainerTypeEntityB> genericizedListCollection = new ArrayList<OMContainerTypeEntityB>();

        Iterator i = genericizedListType.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            genericizedListCollection.add(entity);
        }

        setGenericizedListType(genericizedListCollection);
    }

    @Override
    public void setGenericizedMapTypeSetField(Map genericizedMapType) {
        Map<Integer, OMContainerTypeEntityB> genericizedMapCollection = new HashMap<Integer, OMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            genericizedMapCollection.put(entity.getId(), entity);
        }

        setGenericizedMapType(genericizedMapCollection);

    }

    @Override
    public void setGenericizedMapWithKeyTypeSetField(Map genericizedMapWithKeyType) {
        Map<String, OMContainerTypeEntityB> genericizedMapWithKeyCollection = new HashMap<String, OMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapWithKeyType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            genericizedMapWithKeyCollection.put(entity.getName(), entity);
        }

        setGenericizedMapWithKeyType(genericizedMapWithKeyCollection);
    }

    @Override
    public void setGenericizedSetTypeCollectionField(Set genericizedSetType) {
        Set<OMContainerTypeEntityB> genericizedSetCollection = new HashSet<OMContainerTypeEntityB>();

        Iterator i = genericizedSetCollection.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            genericizedSetCollection.add(entity);
        }

        setGenericizedSetType(genericizedSetCollection);

    }

    @Override
    public void setOrderedListTypeSetField(List orderedListType) {
        List<OMContainerTypeEntityB> orderedListCollection = new ArrayList<OMContainerTypeEntityB>();

        Iterator i = orderedListType.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            orderedListCollection.add(entity);
        }

        setGenericizedListType(orderedListCollection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setUngenericizedCollectionTypeCollectionField(Collection ungenericizedCollectionType) {
        Collection ungenericizedCollectionCollection = new ArrayList();

        Iterator i = ungenericizedCollectionType.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            ungenericizedCollectionCollection.add(entity);
        }

        setUngenericizedCollectionType(ungenericizedCollectionCollection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setUngenericizedListTypeCollectionField(List ungenericizedListType) {
        List ungenericizedListCollection = new ArrayList();

        Iterator i = ungenericizedListType.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            ungenericizedListCollection.add(entity);
        }

        setUngenericizedListType(ungenericizedListCollection);
    }

    @SuppressWarnings("unchecked")
    public void setUngenericizedMapTypeCollectionField(Map ungenericizedMapType) {
        Map ungenericizedMapCollection = new HashMap();

        Collection tempCollection = ungenericizedMapType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            ungenericizedMapCollection.put(entity.getId(), entity);
        }

        setUngenericizedMapWithKeyType(ungenericizedMapCollection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setUngenericizedMapWithKeyTypeCollectionField(Map ungenericizedMapWithKeyType) {
        Map ungenericizedMapWithKeyCollection = new HashMap();

        Collection tempCollection = ungenericizedMapWithKeyType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            ungenericizedMapWithKeyCollection.put(entity.getName(), entity);
        }

        setUngenericizedMapWithKeyType(ungenericizedMapWithKeyCollection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setUngenericizedSetTypeCollectionField(Set ungenericizedSetType) {
        Set ungenericizedSetCollection = new HashSet();

        Iterator i = ungenericizedSetType.iterator();
        while (i.hasNext()) {
            OMContainerTypeEntityB entity = (OMContainerTypeEntityB) i.next();
            ungenericizedSetCollection.add(entity);
        }

        setUngenericizedSetType(ungenericizedSetCollection);
    }

    @Override
    public String toString() {
        return "OMContainerTypeEntityA [id=" + id + ", name=" + name + "]";
    }

}
