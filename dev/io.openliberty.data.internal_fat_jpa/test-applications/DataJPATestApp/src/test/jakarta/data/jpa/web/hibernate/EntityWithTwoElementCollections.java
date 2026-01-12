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
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * Entity with two ElementCollection attributes.
 */
@Entity
public class EntityWithTwoElementCollections {

    @Id
    Integer id;

    @ElementCollection(fetch = FetchType.LAZY)
    List<String> lazyList1 = new ArrayList<>();

    // TODO uncomment to reproduce 33290
    //@ElementCollection(fetch = FetchType.LAZY)
    List<String> lazyList2 = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public List<String> getLazyList1() {
        return lazyList1;
    }

    public List<String> getLazyList2() {
        return lazyList2;
    }

    public void setLazyList1(List<String> list) {
        this.lazyList1 = list;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLazyList2(List<String> list) {
        this.lazyList2 = list;
    }

}