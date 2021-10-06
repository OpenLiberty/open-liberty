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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.entities;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IContainerTypeEntityA {
    /*
     * Entity Primary Key
     */
    public int getId();

    public void setId(int id);

    /*
     * Simple Entity Persistent Data
     */
    public String getName();

    public void setName(String name);

    /*
     * Relationship Fields
     */

    /**
     * genericizedCollectionType
     *
     * OneXMany relationship using a Collection-type, with the target entity type defined by Java 5 Generics.
     */
    public Collection getGenericizedCollectionTypeCollectionField();

    public void setGenericizedCollectionTypeCollectionField(Collection genericizedCollectionType);

    public void insertGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType);

    public void removeGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType);

    public boolean isMemberOfGenericizedCollectionTypeField(IContainerTypeEntityB genericizedCollectionType);

    /**
     * ungenericizedCollectionType
     *
     * OneXMany relationship using a Collection-type, with the target entity type defined by annotation/XML.
     */
    public Collection getUngenericizedCollectionTypeCollectionField();

    public void setUngenericizedCollectionTypeCollectionField(Collection ungenericizedCollectionType);

    public void insertUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType);

    public void removeUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType);

    public boolean isMemberOfUngenericizedCollectionTypeField(IContainerTypeEntityB ungenericizedCollectionType);

    /**
     * genericizedSetType
     *
     * OneXMany relationship using a Set-type, with the target entity type defined by Java 5 Generics.
     */
    public Set getGenericizedSetTypeCollectionField();

    public void setGenericizedSetTypeCollectionField(Set genericizedSetType);

    public void insertGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType);

    public void removeGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType);

    public boolean isMemberOfGenericizedSetTypeField(IContainerTypeEntityB genericizedSetType);

    /**
     * ungenericizedSetType
     *
     * OneXMany relationship using a Set-type, with the target entity type defined by annotation/XML.
     */
    public Set getUngenericizedSetTypeCollectionField();

    public void setUngenericizedSetTypeCollectionField(Set ungenericizedSetType);

    public void insertUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType);

    public void removeUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType);

    public boolean isMemberOfUngenericizedSetTypeField(IContainerTypeEntityB ungenericizedSetType);

    /**
     * genericizedListType
     *
     * OneXMany relationship using a List-type, with the target entity type defined by Java 5 Generics.
     */
    public List getGenericizedListTypeCollectionField();

    public void setGenericizedListTypeSetField(List genericizedListType);

    public void insertGenericizedListTypeField(IContainerTypeEntityB genericizedListType);

    public void removeGenericizedListTypeField(IContainerTypeEntityB genericizedListType);

    public boolean isMemberOfGenericizedListTypeField(IContainerTypeEntityB genericizedListType);

    /**
     * ungenericizedListType
     *
     * OneXMany relationship using a List-type, with the target entity type defined by annotation/XML.
     */
    public List getUngenericizedListTypeCollectionField();

    public void setUngenericizedListTypeCollectionField(List ungenericizedListType);

    public void insertUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType);

    public void removeUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType);

    public boolean isMemberOfUngenericizedListTypeField(IContainerTypeEntityB ungenericizedListType);

    /**
     * orderedListType
     *
     * OneXMany relationship using a List-type, with the target entity type defined by Java 5 Generics, and with the
     * contents of the list ordered by IContainerTypeEntityB's name field.
     */
    public List getOrderedListTypeCollectionField();

    public void setOrderedListTypeSetField(List orderedListType);

    public void insertOrderedListTypeField(IContainerTypeEntityB orderedListType);

    public void removeOrderedListTypeField(IContainerTypeEntityB orderedListType);

    public boolean isMemberOfOrderedListTypeField(IContainerTypeEntityB orderedListType);

    /**
     * genericizedMapType
     *
     * OneXMany relationship using a Map-type, with the target entity type defined by Java 5 Generics.
     */
    public Map getGenericizedMapTypeCollectionField();

    public void setGenericizedMapTypeSetField(Map genericizedMapType);

    public void insertGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType);

    public void removeGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType);

    public boolean isMemberOfGenericizedMapTypeField(IContainerTypeEntityB genericizedMapType);

//    /**
//     * ungenericizedMapType
//     *
//     * OneXMany relationship using a Map-type, with the target entity type defined by annotation/XML.
//     */
//    public Map getUngenericizedMapTypeCollectionField();
//
//    public void setUngenericizedMapTypeCollectionField(Map ungenericizedMapType);
//
//    public void insertUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType);
//
//    public void removeUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType);
//
//    public boolean isMemberOfUngenericizedMapTypeField(IContainerTypeEntityB ungenericizedMapType);

    /**
     * genericizedMapWithKeyType
     *
     * OneXMany relationship using a Map-type, with the target entity type defined by Java 5 Generics, using
     * IContainerTypeEntityB's name field for the Set's list of keys.
     */
    public Map getGenericizedMapWithKeyTypeCollectionField();

    public void setGenericizedMapWithKeyTypeSetField(Map genericizedMapWithKeyType);

    public void insertGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType);

    public void removeGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType);

    public boolean isMemberOfGenericizedMapWithKeyTypeField(IContainerTypeEntityB genericizedMapWithKeyType);

    /**
     * ungenericizedMapWithKeyType
     *
     * OneXMany relationship using a Map-type, with the target entity type defined by annotation/XML, using
     * IContainerTypeEntityB's name field for the Set's list of keys.
     */
    public Map getUngenericizedMapWithKeyTypeCollectionField();

    public void setUngenericizedMapWithKeyTypeCollectionField(Map ungenericizedMapWithKeyType);

    public void insertUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType);

    public void removeUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType);

    public boolean isMemberOfUngenericizedMapWithKeyTypeField(IContainerTypeEntityB ungenericizedMapWithKeyType);
}
