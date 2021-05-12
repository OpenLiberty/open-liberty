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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IContainerTypeEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IContainerTypeEntityB;

//@Entity
public class XMLOMContainerTypeEntityA implements IContainerTypeEntityA {
    // @Id
    private int id;

    private String name;

    // Collection Type
    // @OneToMany
    private Collection<XMLOMContainerTypeEntityB> genericizedCollectionType;

    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
    private Collection ungenericizedCollectionType;

    // Set Type
    // @OneToMany
    private Set<XMLOMContainerTypeEntityB> genericizedSetType;

    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
    private Set ungenericizedSetType;

    // List Type
    // @OneToMany
    private List<XMLOMContainerTypeEntityB> genericizedListType;

    // @OneToMany (targetEntity=XMLContainerTypeEntityB.class)
    private List ungenericizedListType;

    // @OneToMany
    // @OrderBy("name ASC")
    private List<XMLOMContainerTypeEntityB> orderedListType;

    // Map Type
    // @OneToMany
    private Map<Integer, XMLOMContainerTypeEntityB> genericizedMapType;

//    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
//    private Map ungenericizedMapType;

    // @OneToMany
    // @MapKey(name="name")
    private Map<String, XMLOMContainerTypeEntityB> genericizedMapWithKeyType;

    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
    // @MapKey(name="name")
    private Map ungenericizedMapWithKeyType;

    public XMLOMContainerTypeEntityA() {
        id = 0;
        name = "";

        genericizedCollectionType = new ArrayList<XMLOMContainerTypeEntityB>();
        ungenericizedCollectionType = new ArrayList();
        genericizedSetType = new HashSet<XMLOMContainerTypeEntityB>();
        ungenericizedSetType = new HashSet();
        genericizedListType = new ArrayList<XMLOMContainerTypeEntityB>();
        ungenericizedListType = new ArrayList();
        orderedListType = new ArrayList<XMLOMContainerTypeEntityB>();
        genericizedMapType = new HashMap<Integer, XMLOMContainerTypeEntityB>();
//        ungenericizedMapType = new HashMap();
        genericizedMapWithKeyType = new HashMap<String, XMLOMContainerTypeEntityB>();
        ungenericizedMapWithKeyType = new HashMap();
    }

    public Collection<XMLOMContainerTypeEntityB> getGenericizedCollectionType() {
        return genericizedCollectionType;
    }

    public void setGenericizedCollectionType(Collection<XMLOMContainerTypeEntityB> genericizedCollectionType) {
        this.genericizedCollectionType = genericizedCollectionType;
    }

    public List<XMLOMContainerTypeEntityB> getGenericizedListType() {
        return genericizedListType;
    }

    public void setGenericizedListType(List<XMLOMContainerTypeEntityB> genericizedListType) {
        this.genericizedListType = genericizedListType;
    }

    public Map<Integer, XMLOMContainerTypeEntityB> getGenericizedMapType() {
        return genericizedMapType;
    }

    public void setGenericizedMapType(Map<Integer, XMLOMContainerTypeEntityB> genericizedMapType) {
        this.genericizedMapType = genericizedMapType;
    }

    public Map<String, XMLOMContainerTypeEntityB> getGenericizedMapWithKeyType() {
        return genericizedMapWithKeyType;
    }

    public void setGenericizedMapWithKeyType(Map<String, XMLOMContainerTypeEntityB> genericizedMapWithKeyType) {
        this.genericizedMapWithKeyType = genericizedMapWithKeyType;
    }

    public Set<XMLOMContainerTypeEntityB> getGenericizedSetType() {
        return genericizedSetType;
    }

    public void setGenericizedSetType(Set<XMLOMContainerTypeEntityB> genericizedSetType) {
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

    public List<XMLOMContainerTypeEntityB> getOrderedListType() {
        return orderedListType;
    }

    public void setOrderedListType(List<XMLOMContainerTypeEntityB> orderedListType) {
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
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedCollectionType;

        Collection<XMLOMContainerTypeEntityB> genericizedCollectionTypeCollection = getGenericizedCollectionType();
        genericizedCollectionTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedListType;

        List<XMLOMContainerTypeEntityB> genericizedListTypeCollection = getGenericizedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedMapType;

        Map<Integer, XMLOMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.put(entity.getId(), entity);
    }

    @Override
    public void insertGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, XMLOMContainerTypeEntityB> genericizedMapTypeWithKeyCollection = getGenericizedMapWithKeyType();
        genericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    public void insertGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedSetType;

        Set<XMLOMContainerTypeEntityB> genericizedSetTypeCollection = getGenericizedSetType();
        genericizedSetTypeCollection.add(entity);

    }

    @Override
    public void insertOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) orderedListType;

        List<XMLOMContainerTypeEntityB> genericizedListTypeCollection = getOrderedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedCollectionType;

        Collection ungenericizedCollectionTypeCollection = getUngenericizedCollectionType();
        ungenericizedCollectionTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedListType;

        List ungenericizedListTypeCollection = getUngenericizedListType();
        ungenericizedListTypeCollection.add(entity);
    }

//    @SuppressWarnings("unchecked")
//    public void insertUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.put(entity.getId(), entity);
//    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapTypeWithKeyCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedSetType;

        Set ungenericizedSetTypeCollection = getUngenericizedSetType();
        ungenericizedSetTypeCollection.add(entity);
    }

    @Override
    public boolean isMemberOfGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedCollectionType;

        Collection<XMLOMContainerTypeEntityB> collection = getGenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedListType;

        List<XMLOMContainerTypeEntityB> collection = getGenericizedListType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedMapType;

        Map<Integer, XMLOMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();

        return (genericizedMapTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, XMLOMContainerTypeEntityB> genericizedMapWithKeTypeCollection = getGenericizedMapWithKeyType();

        return (genericizedMapWithKeTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedSetType;

        Set<XMLOMContainerTypeEntityB> set = getGenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public boolean isMemberOfOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) orderedListType;

        List<XMLOMContainerTypeEntityB> orderedList = getOrderedListType();

        return (orderedList.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedCollectionType;

        Collection collection = getUngenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedListType;

        List list = getUngenericizedListType();

        return (list.contains(entity));
    }

//    public boolean isMemberOfUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//
//        return (ungenericizedMapTypeCollection.containsValue(entity));
//    }

    @Override
    public boolean isMemberOfUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();

        return (ungenericizedMapWithKeyTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedSetType;

        Set set = getUngenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public void removeGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedCollectionType;
        Collection<XMLOMContainerTypeEntityB> genericizedCollectionCollection = getGenericizedCollectionType();
        genericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedListType;
        List<XMLOMContainerTypeEntityB> genericizedListCollection = getGenericizedListType();
        genericizedListCollection.remove(entity);
    }

    @Override
    public void removeGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedMapType;
        Map<Integer, XMLOMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.remove(entity.getId());
    }

    @Override
    public void removeGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedMapWithKeyType;
        Map<String, XMLOMContainerTypeEntityB> genericizedMapWithKeyTypeCollection = getGenericizedMapWithKeyType();
        genericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) genericizedSetType;
        Set<XMLOMContainerTypeEntityB> genericizedSetCollection = getGenericizedSetType();
        genericizedSetCollection.remove(entity);
    }

    @Override
    public void removeOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) orderedListType;
        List<XMLOMContainerTypeEntityB> orderedListCollection = getOrderedListType();
        orderedListCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedCollectionType;
        Collection ungenericizedCollectionCollection = getUngenericizedCollectionType();
        ungenericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedListType;
        List ungenericizedListCollection = getUngenericizedListType();
        ungenericizedListCollection.remove(entity);
    }

//    public void removeUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedMapType;
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.remove(entity.getId());
//    }

    @Override
    public void removeUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedMapWithKeyType;
        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) ungenericizedSetType;
        Set ungenericizedSetCollection = getUngenericizedSetType();
        ungenericizedSetCollection.remove(entity);
    }

    @Override
    public void setGenericizedCollectionTypeCollectionField(Collection genericizedCollectionType) {
        Collection<XMLOMContainerTypeEntityB> genericizedCollectionCollection = new ArrayList<XMLOMContainerTypeEntityB>();

        Iterator i = genericizedCollectionType.iterator();
        while (i.hasNext()) {
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
            genericizedCollectionCollection.add(entity);
        }

        setGenericizedCollectionType(genericizedCollectionCollection);
    }

    @Override
    public void setGenericizedListTypeSetField(List genericizedListType) {
        List<XMLOMContainerTypeEntityB> genericizedListCollection = new ArrayList<XMLOMContainerTypeEntityB>();

        Iterator i = genericizedListType.iterator();
        while (i.hasNext()) {
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
            genericizedListCollection.add(entity);
        }

        setGenericizedListType(genericizedListCollection);
    }

    @Override
    public void setGenericizedMapTypeSetField(Map genericizedMapType) {
        Map<Integer, XMLOMContainerTypeEntityB> genericizedMapCollection = new HashMap<Integer, XMLOMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
            genericizedMapCollection.put(entity.getId(), entity);
        }

        setGenericizedMapType(genericizedMapCollection);

    }

    @Override
    public void setGenericizedMapWithKeyTypeSetField(Map genericizedMapWithKeyType) {
        Map<String, XMLOMContainerTypeEntityB> genericizedMapWithKeyCollection = new HashMap<String, XMLOMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapWithKeyType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
            genericizedMapWithKeyCollection.put(entity.getName(), entity);
        }

        setGenericizedMapWithKeyType(genericizedMapWithKeyCollection);
    }

    @Override
    public void setGenericizedSetTypeCollectionField(Set genericizedSetType) {
        Set<XMLOMContainerTypeEntityB> genericizedSetCollection = new HashSet<XMLOMContainerTypeEntityB>();

        Iterator i = genericizedSetCollection.iterator();
        while (i.hasNext()) {
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
            genericizedSetCollection.add(entity);
        }

        setGenericizedSetType(genericizedSetCollection);

    }

    @Override
    public void setOrderedListTypeSetField(List orderedListType) {
        List<XMLOMContainerTypeEntityB> orderedListCollection = new ArrayList<XMLOMContainerTypeEntityB>();

        Iterator i = orderedListType.iterator();
        while (i.hasNext()) {
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
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
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
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
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
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
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
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
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
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
            XMLOMContainerTypeEntityB entity = (XMLOMContainerTypeEntityB) i.next();
            ungenericizedSetCollection.add(entity);
        }

        setUngenericizedSetType(ungenericizedSetCollection);
    }

    @Override
    public String toString() {
        return "XMLOMContainerTypeEntityA [id=" + id + ", name=" + name + "]";
    }

}
