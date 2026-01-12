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
package test.jakarta.data.jpa.web.hibernate;

import java.util.ArrayList;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * Entity with an attribute that is an ArrayList.
 */
@Entity
public class EntityWithArrayList {

    @Id
    String id;

    // TODO enable if #33205 is addressed
    //@ElementCollection(fetch = FetchType.EAGER)
    ArrayList<Long> longList = new ArrayList<>();

    public String getId() {
        return id;
    }

    public ArrayList<Long> getLongList() {
        return longList;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLongList(ArrayList<Long> longList) {
        this.longList = longList;
    }

}