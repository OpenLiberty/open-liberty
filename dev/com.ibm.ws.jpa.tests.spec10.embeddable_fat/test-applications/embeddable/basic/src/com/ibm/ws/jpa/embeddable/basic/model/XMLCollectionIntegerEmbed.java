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
import java.util.Vector;

public class XMLCollectionIntegerEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Collection<Integer> INIT = Arrays.asList(new Integer(2), new Integer(3), new Integer(1));
    public static final Collection<Integer> UPDATE = Arrays.asList(new Integer(2), new Integer(4), new Integer(3), new Integer(1));

    private Collection<Integer> collectionInteger;

    public XMLCollectionIntegerEmbed() {
    }

    public XMLCollectionIntegerEmbed(Collection<Integer> collectionInteger) {
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
        if (!(otherObject instanceof XMLCollectionIntegerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionInteger != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            Collection<Integer> temp = new Vector<Integer>(collectionInteger);
            sb.append("collectionInteger=" + temp.toString());
        } else {
            sb.append("collectionInteger=null");
        }
        return sb.toString();
    }
}
