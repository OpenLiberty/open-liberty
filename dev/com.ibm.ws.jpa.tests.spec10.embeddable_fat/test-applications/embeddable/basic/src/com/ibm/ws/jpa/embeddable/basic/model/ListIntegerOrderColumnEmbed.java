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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;

@Embeddable
public class ListIntegerOrderColumnEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<Integer> INIT = new ArrayList<Integer>(Arrays.asList(new Integer(2), new Integer(3), new Integer(1)));
    public static final List<Integer> UPDATE = new ArrayList<Integer>(Arrays.asList(new Integer(2), new Integer(4), new Integer(3), new Integer(1)));

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ListIntOrderColumn", joinColumns = @JoinColumn(name = "parent_id"))
    @Column(name = "value")
    @OrderColumn(name = "valueOrderColumn")
    private List<Integer> notListIntegerOrderColumn;

    public ListIntegerOrderColumnEmbed() {
    }

    public ListIntegerOrderColumnEmbed(List<Integer> notListIntegerOrderColumn) {
        this.notListIntegerOrderColumn = notListIntegerOrderColumn;
    }

    public List<Integer> getNotListIntegerOrderColumn() {
        return this.notListIntegerOrderColumn;
    }

    public void setNotListIntegerOrderColumn(List<Integer> notListIntegerOrderColumn) {
        this.notListIntegerOrderColumn = notListIntegerOrderColumn;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof ListIntegerOrderColumnEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (notListIntegerOrderColumn != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            List<Integer> temp = new Vector<Integer>(notListIntegerOrderColumn);
            sb.append("notListIntegerOrderColumn=" + temp.toString());
        } else {
            sb.append("notListIntegerOrderColumn=null");
        }
        return sb.toString();
    }
}
