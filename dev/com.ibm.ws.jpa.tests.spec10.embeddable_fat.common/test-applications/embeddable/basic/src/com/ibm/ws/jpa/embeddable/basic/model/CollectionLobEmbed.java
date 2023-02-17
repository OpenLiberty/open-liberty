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
import javax.persistence.Lob;

@Embeddable
public class CollectionLobEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Collection<String> INIT = Arrays.asList("Init1", "Init2");
    public static final Collection<String> UPDATE = Arrays.asList("Update1", "Update2", "Update3");

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ColLob", joinColumns = @JoinColumn(name = "parent_id"))
    @Column(name = "value")
    /*
     * JPA Spec 11.1.43 - OrderColumn Annotation
     * The OrderColumn annotation specifies a column that is used to maintain the persistent order of a list.
     *
     * EclipseLink requires a "java.util.List" type and the JPA spec seems to imply that a "list" is required, since a simple "collection" doesn't guarantee order.
     */
//    @OrderColumn(name = "valueOrderColumn")
    @Lob
    private Collection<String> collectionLob;

    public CollectionLobEmbed() {
    }

    public CollectionLobEmbed(Collection<String> collectionLob) {
        this.collectionLob = collectionLob;
    }

    public Collection<String> getCollectionLob() {
        return this.collectionLob;
    }

    public void setCollectionLob(Collection<String> collectionLob) {
        this.collectionLob = collectionLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof CollectionLobEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionLob != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            List<String> temp = new Vector<String>(collectionLob);
            // With OrderColumn removed, sort the collection for JUnit comparisons
            Collections.sort(temp);
            sb.append("collectionLob=" + temp.toString());
        } else {
            sb.append("collectionLob=null");
        }
        return sb.toString();
    }
}
