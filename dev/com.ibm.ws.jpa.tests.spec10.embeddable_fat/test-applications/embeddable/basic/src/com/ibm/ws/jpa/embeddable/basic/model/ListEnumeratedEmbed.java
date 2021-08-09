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

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;

@Embeddable
public class ListEnumeratedEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum ListEnumeratedOrderColumnEnum {
        ONE, TWO, THREE
    }

    public static final List<ListEnumeratedOrderColumnEnum> INIT = new ArrayList<ListEnumeratedOrderColumnEnum>(Arrays.asList(ListEnumeratedOrderColumnEnum.THREE,
                                                                                                                              ListEnumeratedOrderColumnEnum.ONE));
    public static final List<ListEnumeratedOrderColumnEnum> UPDATE = new ArrayList<ListEnumeratedOrderColumnEnum>(Arrays
                    .asList(ListEnumeratedOrderColumnEnum.THREE, ListEnumeratedOrderColumnEnum.ONE, ListEnumeratedOrderColumnEnum.TWO));

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ListEnum", joinColumns = @JoinColumn(name = "parent_id"))
    @OrderColumn(name = "valueOrderColumn")
    @Column(name = "value")
    @Enumerated(EnumType.STRING)
    private List<ListEnumeratedOrderColumnEnum> listEnumerated;

    public ListEnumeratedEmbed() {
    }

    public ListEnumeratedEmbed(List<ListEnumeratedOrderColumnEnum> listEnumerated) {
        this.listEnumerated = listEnumerated;
    }

    public List<ListEnumeratedOrderColumnEnum> getListEnumerated() {
        return this.listEnumerated;
    }

    public void setListEnumerated(List<ListEnumeratedOrderColumnEnum> listEnumerated) {
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
        if (!(otherObject instanceof ListEnumeratedEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (listEnumerated != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            List<ListEnumeratedOrderColumnEnum> temp = new Vector<ListEnumeratedOrderColumnEnum>(listEnumerated);
            sb.append("listEnumerated=" + temp.toString());
        } else {
            sb.append("listEnumerated=null");
        }
        return sb.toString();
    }
}
