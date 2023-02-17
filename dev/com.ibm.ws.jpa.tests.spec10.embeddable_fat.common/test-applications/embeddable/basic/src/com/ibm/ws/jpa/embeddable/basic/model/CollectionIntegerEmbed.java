/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;

@Embeddable
public class CollectionIntegerEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Collection<Integer> INIT = Arrays.asList(new Integer(2), new Integer(3), new Integer(1));
    public static final Collection<Integer> UPDATE = Arrays.asList(new Integer(2), new Integer(4), new Integer(3), new Integer(1));

    @CollectionTable(name = "ColInt", joinColumns = @JoinColumn(name = "parent_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "value")
    /*
     * JPA Spec 11.1.43 - OrderColumn Annotation
     * The OrderColumn annotation specifies a column that is used to maintain the persistent order of a list.
     *
     * EclipseLink requires a "java.util.List" type and the JPA spec seems to imply that a "list" is required, since a simple "collection" doesn't guarantee order.
     */
//    @OrderColumn(name = "valueOrderColumn")
    private Collection<Integer> collectionInteger;

    public CollectionIntegerEmbed() {
    }

    public CollectionIntegerEmbed(Collection<Integer> collectionInteger) {
        this.collectionInteger = collectionInteger;
    }

    public Collection<Integer> getCollectionInteger() {
        return this.collectionInteger;
    }

    public void setCollectionInteger(Collection<Integer> collectionInteger) {
        this.collectionInteger = collectionInteger;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof CollectionIntegerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionInteger != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            List<Integer> temp = new Vector<Integer>(collectionInteger);
            // With OrderColumn removed, sort the collection for JUnit comparisons
            Collections.sort(temp);
            sb.append("collectionInteger=" + temp.toString());
        } else {
            sb.append("collectionInteger=null");
        }
        return sb.toString();
    }
}
