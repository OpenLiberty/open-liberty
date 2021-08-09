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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
public class ListTemporalEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<Date> INIT = new ArrayList<Date>(Arrays.asList(new Date(System.currentTimeMillis() - 200000000), new Date(1)));
    public static final List<Date> UPDATE = new ArrayList<Date>(Arrays.asList(new Date(System.currentTimeMillis() - 200000000), new Date(), new Date(1)));

    @CollectionTable(name = "ListDate", joinColumns = @JoinColumn(name = "parent_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Temporal(TemporalType.DATE)
    @Column(name = "value")
    @OrderColumn(name = "valueOrderColumn")
    private List<Date> listDate;

    public ListTemporalEmbed() {
    }

    public ListTemporalEmbed(List<Date> listDate) {
        this.listDate = listDate;
    }

    public List<Date> getListDate() {
        return this.listDate;
    }

    public void setListDate(List<Date> listDate) {
        this.listDate = listDate;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof ListTemporalEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (listDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            sb.append("listDate=[");
            for (Date date : listDate) {
                sb.append(sdf.format(date).toString());
                sb.append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        } else
            sb.append("listDate=null");
        return sb.toString();
    }

}
