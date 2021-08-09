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

public class XMLCollectionEnumeratedEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum XMLCollectionEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Collection<XMLCollectionEnumeratedEnum> INIT = Arrays.asList(XMLCollectionEnumeratedEnum.THREE, XMLCollectionEnumeratedEnum.ONE);
    public static final Collection<XMLCollectionEnumeratedEnum> UPDATE = Arrays.asList(XMLCollectionEnumeratedEnum.THREE, XMLCollectionEnumeratedEnum.ONE,
                                                                                       XMLCollectionEnumeratedEnum.TWO);

    private Collection<XMLCollectionEnumeratedEnum> collectionEnumerated;

    public XMLCollectionEnumeratedEmbed() {
    }

    public XMLCollectionEnumeratedEmbed(Collection<XMLCollectionEnumeratedEnum> collectionEnumerated) {
        this.collectionEnumerated = collectionEnumerated;
    }

    public Collection<XMLCollectionEnumeratedEnum> getCollectionEnumerated() {
        return this.collectionEnumerated;
    }

    public void setCollectionEnumerated(Collection<XMLCollectionEnumeratedEnum> collectionEnumerated) {
        this.collectionEnumerated = collectionEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLCollectionEnumeratedEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionEnumerated != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            Collection<XMLCollectionEnumeratedEnum> temp = new Vector<XMLCollectionEnumeratedEnum>(collectionEnumerated);
            sb.append("collectionEnumerated=" + temp.toString());
        } else {
            sb.append("collectionEnumerated=null");
        }
        return sb.toString();
    }
}
