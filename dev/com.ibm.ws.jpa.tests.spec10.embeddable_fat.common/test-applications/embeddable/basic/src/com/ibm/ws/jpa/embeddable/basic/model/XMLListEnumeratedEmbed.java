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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class XMLListEnumeratedEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum XMLListEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final List<XMLListEnumeratedEnum> INIT = new ArrayList<XMLListEnumeratedEnum>(Arrays.asList(XMLListEnumeratedEnum.THREE,
                                                                                                              XMLListEnumeratedEnum.ONE));
    public static final List<XMLListEnumeratedEnum> UPDATE = new ArrayList<XMLListEnumeratedEnum>(Arrays
                    .asList(XMLListEnumeratedEnum.THREE, XMLListEnumeratedEnum.ONE, XMLListEnumeratedEnum.TWO));

    private List<XMLListEnumeratedEnum> listEnumerated;

    public XMLListEnumeratedEmbed() {
    }

    public XMLListEnumeratedEmbed(List<XMLListEnumeratedEnum> listEnumerated) {
        this.listEnumerated = listEnumerated;
    }

    public List<XMLListEnumeratedEnum> getListEnumerated() {
        return this.listEnumerated;
    }

    public void setListEnumerated(List<XMLListEnumeratedEnum> listEnumerated) {
        this.listEnumerated = listEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLListEnumeratedEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (listEnumerated != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            List<XMLListEnumeratedEnum> temp = new Vector<XMLListEnumeratedEnum>(listEnumerated);
            sb.append("listEnumerated=" + temp.toString());
        } else {
            sb.append("listEnumerated=null");
        }
        return sb.toString();
    }
}
