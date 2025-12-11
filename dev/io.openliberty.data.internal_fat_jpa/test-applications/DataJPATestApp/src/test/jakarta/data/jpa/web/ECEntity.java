/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * Entity with and without ElementCollection attributes.
 */
@Entity
public class ECEntity {

    @Id
    String id;

    int[] intArray = new int[] {};

    ArrayList<Long> longList = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    ArrayList<Long> longListEC = new ArrayList<>();

    Set<String> stringSet = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    Set<String> stringSetEC = new HashSet<>();

    public String getId() {
        return id;
    }

    public int[] getIntArray() {
        return intArray;
    }

    public ArrayList<Long> getLongList() {
        return longList;
    }

    public ArrayList<Long> getLongListEC() {
        return longListEC;
    }

    public Set<String> getStringSet() {
        return stringSet;
    }

    public Set<String> getStringSetEC() {
        return stringSetEC;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    public void setLongList(ArrayList<Long> longList) {
        this.longList = longList;
    }

    public void setLongListEC(ArrayList<Long> longListEC) {
        this.longListEC = longListEC;
    }

    public void setStringSet(Set<String> stringSet) {
        this.stringSet = stringSet;
    }

    public void setStringSetEC(Set<String> stringSetEC) {
        this.stringSetEC = stringSetEC;
    }
}