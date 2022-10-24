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
import java.util.Date;
import java.util.List;

public class XMLListTemporalEmbed implements java.io.Serializable {
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

    private List<Date> listDate;

    public XMLListTemporalEmbed() {
    }

    public XMLListTemporalEmbed(List<Date> listDate) {
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
        if (!(otherObject instanceof XMLListTemporalEmbed))
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
