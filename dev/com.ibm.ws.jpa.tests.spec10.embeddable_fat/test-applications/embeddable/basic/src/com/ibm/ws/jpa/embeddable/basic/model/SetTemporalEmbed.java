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
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
public class SetTemporalEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Set<Date> INIT = new HashSet<Date>(Arrays.asList(new Date(System.currentTimeMillis() - 200000000), new Date(1)));
    public static final Set<Date> UPDATE = new HashSet<Date>(Arrays.asList(new Date(System.currentTimeMillis() - 200000000), new Date(), new Date(1)));

    @CollectionTable(name = "SetDate", joinColumns = @JoinColumn(name = "parent_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Temporal(TemporalType.DATE)
    @Column(name = "value")
    private Set<Date> setDate;

    public SetTemporalEmbed() {
    }

    public SetTemporalEmbed(Set<Date> setDate) {
        this.setDate = setDate;
    }

    public Set<Date> getSetDate() {
        return this.setDate;
    }

    public void setSetDate(Set<Date> setDate) {
        this.setDate = setDate;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof SetTemporalEmbed))
            return false;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        Set<String> setDateOnly = new HashSet<String>();
        for (Date date : setDate)
            setDateOnly.add(sdf.format(date));
        Set<String> otherSetDateOnly = new HashSet<String>();
        Set<Date> otherSetDate = ((SetTemporalEmbed) otherObject).getSetDate();
        for (Date date : otherSetDate)
            otherSetDateOnly.add(sdf.format(date));
        return (otherSetDateOnly.equals(setDateOnly)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (setDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            sb.append("setDate=[");
            for (Date date : setDate) {
                sb.append(sdf.format(date).toString());
                sb.append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        } else
            sb.append("setDate=null");
        return sb.toString();
    }

}
