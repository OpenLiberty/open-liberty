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
package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.annotated;

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
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.OrderBy;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IContainerTypeEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IContainerTypeEntityB;

@Entity
public class MMContainerTypeEntityA implements IContainerTypeEntityA {
    @Id
    private int id;

    private String name;

    // Collection Type
    @ManyToMany
    @JoinTable(name = "MMCTEA_GCT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private Collection<MMContainerTypeEntityB> genericizedCollectionType;

    @ManyToMany(targetEntity = MMContainerTypeEntityB.class)
    @JoinTable(name = "MMCTEA_UGCT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private Collection ungenericizedCollectionType;

    // Set Type
    @ManyToMany
    @JoinTable(name = "MMCTEA_GST",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private Set<MMContainerTypeEntityB> genericizedSetType;

    @ManyToMany(targetEntity = MMContainerTypeEntityB.class)
    @JoinTable(name = "MMCTEA_UGST",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private Set ungenericizedSetType;

    // List Type
    @ManyToMany
    @JoinTable(name = "MMCTEA_GLT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private List<MMContainerTypeEntityB> genericizedListType;

    @ManyToMany(targetEntity = MMContainerTypeEntityB.class)
    @JoinTable(name = "MMCTEA_UGLT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private List ungenericizedListType;

    @ManyToMany
    @JoinTable(name = "MMCTEA_OLT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    @OrderBy("name ASC")
    private List<MMContainerTypeEntityB> orderedListType;

    // Map Type
    @ManyToMany
    @JoinTable(name = "MMCTEA_GMT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private Map<Integer, MMContainerTypeEntityB> genericizedMapType;

//    @ManyToMany(targetEntity = MMContainerTypeEntityB.class)
//    @JoinTable(name = "MMCTEA_UGMT")
//    private Map ungenericizedMapType;

    @ManyToMany
    @MapKey(name = "name")
    @JoinTable(name = "MMCTEA_GMKT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    private Map<String, MMContainerTypeEntityB> genericizedMapWithKeyType;

    @ManyToMany(targetEntity = MMContainerTypeEntityB.class)
    @JoinTable(name = "MMCTEA_UGMKT",
               joinColumns = @JoinColumn(name = "MMContainerTypeEntityA_ID"),
               inverseJoinColumns = @JoinColumn(name = "MMContainerTypeEntityB_ID"))
    @MapKey(name = "name")
    private Map ungenericizedMapWithKeyType;

    public MMContainerTypeEntityA() {
        id = 0;
        name = "";

        genericizedCollectionType = new ArrayList<MMContainerTypeEntityB>();
        ungenericizedCollectionType = new ArrayList();
        genericizedSetType = new HashSet<MMContainerTypeEntityB>();
        ungenericizedSetType = new HashSet();
        genericizedListType = new ArrayList<MMContainerTypeEntityB>();
        ungenericizedListType = new ArrayList();
        orderedListType = new ArrayList<MMContainerTypeEntityB>();
        genericizedMapType = new HashMap<Integer, MMContainerTypeEntityB>();
//        ungenericizedMapType = new HashMap();
        genericizedMapWithKeyType = new HashMap<String, MMContainerTypeEntityB>();
        ungenericizedMapWithKeyType = new HashMap();
    }

    public Collection<MMContainerTypeEntityB> getGenericizedCollectionType() {
        return genericizedCollectionType;
    }

    public void setGenericizedCollectionType(Collection<MMContainerTypeEntityB> genericizedCollectionType) {
        this.genericizedCollectionType = genericizedCollectionType;
    }

    public List<MMContainerTypeEntityB> getGenericizedListType() {
        return genericizedListType;
    }

    public void setGenericizedListType(List<MMContainerTypeEntityB> genericizedListType) {
        this.genericizedListType = genericizedListType;
    }

    public Map<Integer, MMContainerTypeEntityB> getGenericizedMapType() {
        return genericizedMapType;
    }

    public void setGenericizedMapType(Map<Integer, MMContainerTypeEntityB> genericizedMapType) {
        this.genericizedMapType = genericizedMapType;
    }

    public Map<String, MMContainerTypeEntityB> getGenericizedMapWithKeyType() {
        return genericizedMapWithKeyType;
    }

    public void setGenericizedMapWithKeyType(Map<String, MMContainerTypeEntityB> genericizedMapWithKeyType) {
        this.genericizedMapWithKeyType = genericizedMapWithKeyType;
    }

    public Set<MMContainerTypeEntityB> getGenericizedSetType() {
        return genericizedSetType;
    }

    public void setGenericizedSetType(Set<MMContainerTypeEntityB> genericizedSetType) {
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

    public List<MMContainerTypeEntityB> getOrderedListType() {
        return orderedListType;
    }

    public void setOrderedListType(List<MMContainerTypeEntityB> orderedListType) {
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
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedCollectionType;

        Collection<MMContainerTypeEntityB> genericizedCollectionTypeCollection = getGenericizedCollectionType();
        genericizedCollectionTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedListType;

        List<MMContainerTypeEntityB> genericizedListTypeCollection = getGenericizedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedMapType;

        Map<Integer, MMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.put(entity.getId(), entity);
    }

    @Override
    public void insertGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, MMContainerTypeEntityB> genericizedMapTypeWithKeyCollection = getGenericizedMapWithKeyType();
        genericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    public void insertGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedSetType;

        Set<MMContainerTypeEntityB> genericizedSetTypeCollection = getGenericizedSetType();
        genericizedSetTypeCollection.add(entity);

    }

    @Override
    public void insertOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) orderedListType;

        List<MMContainerTypeEntityB> genericizedListTypeCollection = getOrderedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedCollectionType;

        Collection ungenericizedCollectionTypeCollection = getUngenericizedCollectionType();
        ungenericizedCollectionTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedListType;

        List ungenericizedListTypeCollection = getUngenericizedListType();
        ungenericizedListTypeCollection.add(entity);
    }

//    @SuppressWarnings("unchecked")
//    public void insertUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.put(entity.getId(), entity);
//    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapTypeWithKeyCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedSetType;

        Set ungenericizedSetTypeCollection = getUngenericizedSetType();
        ungenericizedSetTypeCollection.add(entity);
    }

    @Override
    public boolean isMemberOfGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedCollectionType;

        Collection<MMContainerTypeEntityB> collection = getGenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedListType;

        List<MMContainerTypeEntityB> collection = getGenericizedListType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedMapType;

        Map<Integer, MMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();

        return (genericizedMapTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, MMContainerTypeEntityB> genericizedMapWithKeTypeCollection = getGenericizedMapWithKeyType();

        return (genericizedMapWithKeTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedSetType;

        Set<MMContainerTypeEntityB> set = getGenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public boolean isMemberOfOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) orderedListType;

        List<MMContainerTypeEntityB> orderedList = getOrderedListType();

        return (orderedList.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedCollectionType;

        Collection collection = getUngenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedListType;

        List list = getUngenericizedListType();

        return (list.contains(entity));
    }

//    public boolean isMemberOfUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//
//        return (ungenericizedMapTypeCollection.containsValue(entity));
//    }

    @Override
    public boolean isMemberOfUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();

        return (ungenericizedMapWithKeyTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedSetType;

        Set set = getUngenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public void removeGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedCollectionType;
        Collection<MMContainerTypeEntityB> genericizedCollectionCollection = getGenericizedCollectionType();
        genericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedListType;
        List<MMContainerTypeEntityB> genericizedListCollection = getGenericizedListType();
        genericizedListCollection.remove(entity);
    }

    @Override
    public void removeGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedMapType;
        Map<Integer, MMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.remove(entity.getId());
    }

    @Override
    public void removeGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedMapWithKeyType;
        Map<String, MMContainerTypeEntityB> genericizedMapWithKeyTypeCollection = getGenericizedMapWithKeyType();
        genericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) genericizedSetType;
        Set<MMContainerTypeEntityB> genericizedSetCollection = getGenericizedSetType();
        genericizedSetCollection.remove(entity);
    }

    @Override
    public void removeOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) orderedListType;
        List<MMContainerTypeEntityB> orderedListCollection = getOrderedListType();
        orderedListCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedCollectionType;
        Collection ungenericizedCollectionCollection = getUngenericizedCollectionType();
        ungenericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedListType;
        List ungenericizedListCollection = getUngenericizedListType();
        ungenericizedListCollection.remove(entity);
    }

//    public void removeUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedMapType;
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.remove(entity.getId());
//    }

    @Override
    public void removeUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedMapWithKeyType;
        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        MMContainerTypeEntityB entity = (MMContainerTypeEntityB) ungenericizedSetType;
        Set ungenericizedSetCollection = getUngenericizedSetType();
        ungenericizedSetCollection.remove(entity);
    }

    @Override
    public void setGenericizedCollectionTypeCollectionField(Collection genericizedCollectionType) {
        Collection<MMContainerTypeEntityB> genericizedCollectionCollection = new ArrayList<MMContainerTypeEntityB>();

        Iterator i = genericizedCollectionType.iterator();
        while (i.hasNext()) {
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
            genericizedCollectionCollection.add(entity);
        }

        setGenericizedCollectionType(genericizedCollectionCollection);
    }

    @Override
    public void setGenericizedListTypeSetField(List genericizedListType) {
        List<MMContainerTypeEntityB> genericizedListCollection = new ArrayList<MMContainerTypeEntityB>();

        Iterator i = genericizedListType.iterator();
        while (i.hasNext()) {
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
            genericizedListCollection.add(entity);
        }

        setGenericizedListType(genericizedListCollection);
    }

    @Override
    public void setGenericizedMapTypeSetField(Map genericizedMapType) {
        Map<Integer, MMContainerTypeEntityB> genericizedMapCollection = new HashMap<Integer, MMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
            genericizedMapCollection.put(entity.getId(), entity);
        }

        setGenericizedMapType(genericizedMapCollection);

    }

    @Override
    public void setGenericizedMapWithKeyTypeSetField(Map genericizedMapWithKeyType) {
        Map<String, MMContainerTypeEntityB> genericizedMapWithKeyCollection = new HashMap<String, MMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapWithKeyType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
            genericizedMapWithKeyCollection.put(entity.getName(), entity);
        }

        setGenericizedMapWithKeyType(genericizedMapWithKeyCollection);
    }

    @Override
    public void setGenericizedSetTypeCollectionField(Set genericizedSetType) {
        Set<MMContainerTypeEntityB> genericizedSetCollection = new HashSet<MMContainerTypeEntityB>();

        Iterator i = genericizedSetCollection.iterator();
        while (i.hasNext()) {
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
            genericizedSetCollection.add(entity);
        }

        setGenericizedSetType(genericizedSetCollection);

    }

    @Override
    public void setOrderedListTypeSetField(List orderedListType) {
        List<MMContainerTypeEntityB> orderedListCollection = new ArrayList<MMContainerTypeEntityB>();

        Iterator i = orderedListType.iterator();
        while (i.hasNext()) {
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
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
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
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
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
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
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
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
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
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
            MMContainerTypeEntityB entity = (MMContainerTypeEntityB) i.next();
            ungenericizedSetCollection.add(entity);
        }

        setUngenericizedSetType(ungenericizedSetCollection);
    }

    @Override
    public String toString() {
        return "MMContainerTypeEntityA [id=" + id + ", name=" + name + ", genericizedCollectionType="
               + genericizedCollectionType + ", ungenericizedCollectionType=" + ungenericizedCollectionType
               + ", genericizedSetType=" + genericizedSetType + ", ungenericizedSetType=" + ungenericizedSetType
               + ", genericizedListType=" + genericizedListType + ", ungenericizedListType=" + ungenericizedListType
               + ", orderedListType=" + orderedListType + ", genericizedMapType=" + genericizedMapType
//                + ", ungenericizedMapType=" + ungenericizedMapType
               + ", genericizedMapWithKeyType="
               + genericizedMapWithKeyType + ", ungenericizedMapWithKeyType=" + ungenericizedMapWithKeyType + "]";
    }

}