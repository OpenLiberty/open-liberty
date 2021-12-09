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
package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IContainerTypeEntityA;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.IContainerTypeEntityB;

//@Entity
public class XMLMMContainerTypeEntityA implements IContainerTypeEntityA {
    // @Id
    private int id;

    private String name;

    // Collection Type
    // @OneToMany
    private Collection<XMLMMContainerTypeEntityB> genericizedCollectionType;

    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
    private Collection ungenericizedCollectionType;

    // Set Type
    // @OneToMany
    private Set<XMLMMContainerTypeEntityB> genericizedSetType;

    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
    private Set ungenericizedSetType;

    // List Type
    // @OneToMany
    private List<XMLMMContainerTypeEntityB> genericizedListType;

    // @OneToMany (targetEntity=XMLContainerTypeEntityB.class)
    private List ungenericizedListType;

    // @OneToMany
    // @OrderBy("name ASC")
    private List<XMLMMContainerTypeEntityB> orderedListType;

    // Map Type
    // @OneToMany
    private Map<Integer, XMLMMContainerTypeEntityB> genericizedMapType;

    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
//    private Map ungenericizedMapType;

    // @OneToMany
    // @MapKey(name="name")
    private Map<String, XMLMMContainerTypeEntityB> genericizedMapWithKeyType;

    // @OneToMany(targetEntity=XMLContainerTypeEntityB.class)
    // @MapKey(name="name")
    private Map ungenericizedMapWithKeyType;

    public XMLMMContainerTypeEntityA() {
//        id = 0;
//        name = "";

        genericizedCollectionType = new ArrayList<XMLMMContainerTypeEntityB>();
        ungenericizedCollectionType = new ArrayList();
        genericizedSetType = new HashSet<XMLMMContainerTypeEntityB>();
        ungenericizedSetType = new HashSet();
        genericizedListType = new ArrayList<XMLMMContainerTypeEntityB>();
        ungenericizedListType = new ArrayList();
        orderedListType = new ArrayList<XMLMMContainerTypeEntityB>();
        genericizedMapType = new HashMap<Integer, XMLMMContainerTypeEntityB>();
//        ungenericizedMapType = new HashMap();
        genericizedMapWithKeyType = new HashMap<String, XMLMMContainerTypeEntityB>();
        ungenericizedMapWithKeyType = new HashMap();
    }

    public Collection<XMLMMContainerTypeEntityB> getGenericizedCollectionType() {
        return genericizedCollectionType;
    }

    public void setGenericizedCollectionType(Collection<XMLMMContainerTypeEntityB> genericizedCollectionType) {
        this.genericizedCollectionType = genericizedCollectionType;
    }

    public List<XMLMMContainerTypeEntityB> getGenericizedListType() {
        return genericizedListType;
    }

    public void setGenericizedListType(List<XMLMMContainerTypeEntityB> genericizedListType) {
        this.genericizedListType = genericizedListType;
    }

    public Map<Integer, XMLMMContainerTypeEntityB> getGenericizedMapType() {
        return genericizedMapType;
    }

    public void setGenericizedMapType(Map<Integer, XMLMMContainerTypeEntityB> genericizedMapType) {
        this.genericizedMapType = genericizedMapType;
    }

    public Map<String, XMLMMContainerTypeEntityB> getGenericizedMapWithKeyType() {
        return genericizedMapWithKeyType;
    }

    public void setGenericizedMapWithKeyType(Map<String, XMLMMContainerTypeEntityB> genericizedMapWithKeyType) {
        this.genericizedMapWithKeyType = genericizedMapWithKeyType;
    }

    public Set<XMLMMContainerTypeEntityB> getGenericizedSetType() {
        return genericizedSetType;
    }

    public void setGenericizedSetType(Set<XMLMMContainerTypeEntityB> genericizedSetType) {
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

    public List<XMLMMContainerTypeEntityB> getOrderedListType() {
        return orderedListType;
    }

    public void setOrderedListType(List<XMLMMContainerTypeEntityB> orderedListType) {
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
//      return null;
////        return ungenericizedMapType;
//    }
//
//    public void setUngenericizedMapType(Map ungenericizedMapType) {
////        this.ungenericizedMapType = ungenericizedMapType;
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
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedCollectionType;

        Collection<XMLMMContainerTypeEntityB> genericizedCollectionTypeCollection = getGenericizedCollectionType();
        genericizedCollectionTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedListType;

        List<XMLMMContainerTypeEntityB> genericizedListTypeCollection = getGenericizedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    public void insertGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedMapType;

        Map<Integer, XMLMMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.put(entity.getId(), entity);
    }

    @Override
    public void insertGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, XMLMMContainerTypeEntityB> genericizedMapTypeWithKeyCollection = getGenericizedMapWithKeyType();
        genericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    public void insertGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedSetType;

        Set<XMLMMContainerTypeEntityB> genericizedSetTypeCollection = getGenericizedSetType();
        genericizedSetTypeCollection.add(entity);

    }

    @Override
    public void insertOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) orderedListType;

        List<XMLMMContainerTypeEntityB> genericizedListTypeCollection = getOrderedListType();
        genericizedListTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedCollectionType;

        Collection ungenericizedCollectionTypeCollection = getUngenericizedCollectionType();
        ungenericizedCollectionTypeCollection.add(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedListType;

        List ungenericizedListTypeCollection = getUngenericizedListType();
        ungenericizedListTypeCollection.add(entity);
    }

//    @SuppressWarnings("unchecked")
//    public void insertUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.put(entity.getId(), entity);
//    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapTypeWithKeyCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapTypeWithKeyCollection.put(entity.getName(), entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insertUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedSetType;

        Set ungenericizedSetTypeCollection = getUngenericizedSetType();
        ungenericizedSetTypeCollection.add(entity);
    }

    @Override
    public boolean isMemberOfGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedCollectionType;

        Collection<XMLMMContainerTypeEntityB> collection = getGenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedListType;

        List<XMLMMContainerTypeEntityB> collection = getGenericizedListType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedMapType;

        Map<Integer, XMLMMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();

        return (genericizedMapTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedMapWithKeyType;

        Map<String, XMLMMContainerTypeEntityB> genericizedMapWithKeTypeCollection = getGenericizedMapWithKeyType();

        return (genericizedMapWithKeTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedSetType;

        Set<XMLMMContainerTypeEntityB> set = getGenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public boolean isMemberOfOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) orderedListType;

        List<XMLMMContainerTypeEntityB> orderedList = getOrderedListType();

        return (orderedList.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedCollectionType;

        Collection collection = getUngenericizedCollectionType();

        return (collection.contains(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedListType;

        List list = getUngenericizedListType();

        return (list.contains(entity));
    }

//    public boolean isMemberOfUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedMapType;
//
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//
//        return (ungenericizedMapTypeCollection.containsValue(entity));
//    }

    @Override
    public boolean isMemberOfUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedMapWithKeyType;

        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();

        return (ungenericizedMapWithKeyTypeCollection.containsValue(entity));
    }

    @Override
    public boolean isMemberOfUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedSetType;

        Set set = getUngenericizedSetType();

        return (set.contains(entity));
    }

    @Override
    public void removeGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedCollectionType;
        Collection<XMLMMContainerTypeEntityB> genericizedCollectionCollection = getGenericizedCollectionType();
        genericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeGenericizedListTypeField(IContainerTypeEntityB genericizedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedListType;
        List<XMLMMContainerTypeEntityB> genericizedListCollection = getGenericizedListType();
        genericizedListCollection.remove(entity);
    }

    @Override
    public void removeGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedMapType;
        Map<Integer, XMLMMContainerTypeEntityB> genericizedMapTypeCollection = getGenericizedMapType();
        genericizedMapTypeCollection.remove(entity.getId());
    }

    @Override
    public void removeGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedMapWithKeyType;
        Map<String, XMLMMContainerTypeEntityB> genericizedMapWithKeyTypeCollection = getGenericizedMapWithKeyType();
        genericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) genericizedSetType;
        Set<XMLMMContainerTypeEntityB> genericizedSetCollection = getGenericizedSetType();
        genericizedSetCollection.remove(entity);
    }

    @Override
    public void removeOrderedListTypeField(IContainerTypeEntityB orderedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) orderedListType;
        List<XMLMMContainerTypeEntityB> orderedListCollection = getOrderedListType();
        orderedListCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedCollectionType;
        Collection ungenericizedCollectionCollection = getUngenericizedCollectionType();
        ungenericizedCollectionCollection.remove(entity);
    }

    @Override
    public void removeUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedListType;
        List ungenericizedListCollection = getUngenericizedListType();
        ungenericizedListCollection.remove(entity);
    }

//    public void removeUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType) {
//        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedMapType;
//        Map ungenericizedMapTypeCollection = getUngenericizedMapType();
//        ungenericizedMapTypeCollection.remove(entity.getId());
//    }

    @Override
    public void removeUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedMapWithKeyType;
        Map ungenericizedMapWithKeyTypeCollection = getUngenericizedMapWithKeyType();
        ungenericizedMapWithKeyTypeCollection.remove(entity.getName());
    }

    @Override
    public void removeUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType) {
        XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) ungenericizedSetType;
        Set ungenericizedSetCollection = getUngenericizedSetType();
        ungenericizedSetCollection.remove(entity);
    }

    @Override
    public void setGenericizedCollectionTypeCollectionField(Collection genericizedCollectionType) {
        Collection<XMLMMContainerTypeEntityB> genericizedCollectionCollection = new ArrayList<XMLMMContainerTypeEntityB>();

        Iterator i = genericizedCollectionType.iterator();
        while (i.hasNext()) {
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
            genericizedCollectionCollection.add(entity);
        }

        setGenericizedCollectionType(genericizedCollectionCollection);
    }

    @Override
    public void setGenericizedListTypeSetField(List genericizedListType) {
        List<XMLMMContainerTypeEntityB> genericizedListCollection = new ArrayList<XMLMMContainerTypeEntityB>();

        Iterator i = genericizedListType.iterator();
        while (i.hasNext()) {
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
            genericizedListCollection.add(entity);
        }

        setGenericizedListType(genericizedListCollection);
    }

    @Override
    public void setGenericizedMapTypeSetField(Map genericizedMapType) {
        Map<Integer, XMLMMContainerTypeEntityB> genericizedMapCollection = new HashMap<Integer, XMLMMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
            genericizedMapCollection.put(entity.getId(), entity);
        }

        setGenericizedMapType(genericizedMapCollection);

    }

    @Override
    public void setGenericizedMapWithKeyTypeSetField(Map genericizedMapWithKeyType) {
        Map<String, XMLMMContainerTypeEntityB> genericizedMapWithKeyCollection = new HashMap<String, XMLMMContainerTypeEntityB>();

        Collection tempCollection = genericizedMapWithKeyType.values();
        Iterator i = tempCollection.iterator();
        while (i.hasNext()) {
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
            genericizedMapWithKeyCollection.put(entity.getName(), entity);
        }

        setGenericizedMapWithKeyType(genericizedMapWithKeyCollection);
    }

    @Override
    public void setGenericizedSetTypeCollectionField(Set genericizedSetType) {
        Set<XMLMMContainerTypeEntityB> genericizedSetCollection = new HashSet<XMLMMContainerTypeEntityB>();

        Iterator i = genericizedSetCollection.iterator();
        while (i.hasNext()) {
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
            genericizedSetCollection.add(entity);
        }

        setGenericizedSetType(genericizedSetCollection);

    }

    @Override
    public void setOrderedListTypeSetField(List orderedListType) {
        List<XMLMMContainerTypeEntityB> orderedListCollection = new ArrayList<XMLMMContainerTypeEntityB>();

        Iterator i = orderedListType.iterator();
        while (i.hasNext()) {
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
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
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
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
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
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
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
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
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
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
            XMLMMContainerTypeEntityB entity = (XMLMMContainerTypeEntityB) i.next();
            ungenericizedSetCollection.add(entity);
        }

        setUngenericizedSetType(ungenericizedSetCollection);
    }

    @Override
    public String toString() {
        return "XMLMMContainerTypeEntityA [id=" + id + ", name=" + name + ", genericizedCollectionType="
               + genericizedCollectionType + ", ungenericizedCollectionType=" + ungenericizedCollectionType
               + ", genericizedSetType=" + genericizedSetType + ", ungenericizedSetType=" + ungenericizedSetType
               + ", genericizedListType=" + genericizedListType + ", ungenericizedListType=" + ungenericizedListType
               + ", orderedListType=" + orderedListType + ", genericizedMapType=" + genericizedMapType
//                + ", ungenericizedMapType=" + ungenericizedMapType 
               + ", genericizedMapWithKeyType="
               + genericizedMapWithKeyType + ", ungenericizedMapWithKeyType=" + ungenericizedMapWithKeyType + "]";
    }

}