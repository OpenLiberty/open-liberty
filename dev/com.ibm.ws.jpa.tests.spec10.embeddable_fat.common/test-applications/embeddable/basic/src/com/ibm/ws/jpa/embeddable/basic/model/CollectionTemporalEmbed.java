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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
public class CollectionTemporalEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static List<Date> INIT;
    public static List<Date> UPDATE;
    static {
        try {
            /*
             * JPA Specification, section 11.1.51:
             * public enum TemporalType {
             * DATE, //java.sql.Date
             * TIME, //java.sql.Time
             * TIMESTAMP //java.sql.Timestamp
             * }
             *
             * JDBC Specification, TABLE B-6
             * DATE java.sql.Date
             * TIME java.sql.Time
             * TIMESTAMP java.sql.Timestamp
             *
             * java.util.Date stores Hours, Minutes, Seconds, ect. However, the DATE jdbc type
             * only supports Year, Month, Day.
             */
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            INIT = new ArrayList<Date>(Arrays.asList(
                                                     formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000))),
                                                     formatter.parse(formatter.format(new Date(1)))));
            UPDATE = new ArrayList<Date>(Arrays.asList(
                                                       formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000))),
                                                       formatter.parse(formatter.format(new Date())),
                                                       formatter.parse(formatter.format(new Date(1)))));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @CollectionTable(name = "ColDate", joinColumns = @JoinColumn(name = "parent_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "value")
    /*
     * JPA Spec 11.1.43 - OrderColumn Annotation
     * The OrderColumn annotation specifies a column that is used to maintain the persistent order of a list.
     *
     * EclipseLink requires a "java.util.List" type and the JPA spec seems to imply that a "list" is required, since a simple "collection" doesn't guarantee order.
     */
//    @OrderColumn(name = "valueOrderColumn")
    @Temporal(TemporalType.DATE)
    private Collection<Date> collectionDate;

    public CollectionTemporalEmbed() {
    }

    public CollectionTemporalEmbed(Collection<Date> collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Collection<Date> getCollectionDate() {
        return this.collectionDate;
    }

    public void setCollectionDate(Collection<Date> collectionDate) {
        this.collectionDate = collectionDate;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof CollectionTemporalEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionDate != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            List<Date> temp = new Vector<Date>(collectionDate);

            // With OrderColumn removed, sort the collection for JUnit comparisons
            Collections.sort(temp);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            sb.append("collectionDate=[");
            for (Date date : temp) {
                sb.append(sdf.format(date).toString());
                sb.append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        } else
            sb.append("collectionDate=null");
        return sb.toString();
    }

}
