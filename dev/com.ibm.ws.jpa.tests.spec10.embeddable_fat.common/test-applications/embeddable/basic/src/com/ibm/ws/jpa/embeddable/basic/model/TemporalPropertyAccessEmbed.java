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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
@Access(AccessType.PROPERTY)
public class TemporalPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static List<TemporalPropertyAccessEmbed> LIST_INIT;
    public static List<TemporalPropertyAccessEmbed> LIST_UPDATE;
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
            LIST_INIT = new ArrayList<TemporalPropertyAccessEmbed>(Arrays
                            .asList(new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000)))),
                                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(1))))));
            LIST_UPDATE = new ArrayList<TemporalPropertyAccessEmbed>(Arrays
                            .asList(new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000)))),
                                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date()))),
                                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(1))))));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, TemporalPropertyAccessEmbed> MAP_INIT;
    public static Map<Integer, TemporalPropertyAccessEmbed> MAP_UPDATE;
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
            Map<Integer, TemporalPropertyAccessEmbed> map = new HashMap<Integer, TemporalPropertyAccessEmbed>();
            map.put(new Integer(3), new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000)))));
            map.put(new Integer(2), new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(1)))));
            MAP_INIT = Collections.unmodifiableMap(map);

            map = new HashMap<Integer, TemporalPropertyAccessEmbed>();
            map.put(new Integer(3), new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000)))));
            map.put(new Integer(1), new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date()))));
            map.put(new Integer(2), new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(1)))));
            MAP_UPDATE = Collections.unmodifiableMap(map);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static Map<Date, TemporalPropertyAccessEmbed> MAP2_INIT;
    public static Map<Date, TemporalPropertyAccessEmbed> MAP2_UPDATE;
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
            Map<Date, TemporalPropertyAccessEmbed> map = new HashMap<Date, TemporalPropertyAccessEmbed>();
            map.put(formatter.parse(formatter.format(new Date(1))),
                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000)))));
            map.put(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000))),
                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(formatter.parse(formatter.format(new Date(1)))))));
            MAP2_INIT = Collections.unmodifiableMap(map);

            map = new HashMap<Date, TemporalPropertyAccessEmbed>();
            map.put(formatter.parse(formatter.format(new Date())),
                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000)))));
            map.put(formatter.parse(formatter.format(new Date(1))),
                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(formatter.parse(formatter.format(new Date()))))));
            map.put(formatter.parse(formatter.format(new Date(System.currentTimeMillis() - 200000000))),
                    new TemporalPropertyAccessEmbed(formatter.parse(formatter.format(formatter.parse(formatter.format(new Date(1)))))));
            MAP2_UPDATE = Collections.unmodifiableMap(map);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private Date temporalValuePA;

    public TemporalPropertyAccessEmbed() {
    }

    public TemporalPropertyAccessEmbed(Date temporalValuePA) {
        this.temporalValuePA = temporalValuePA;
    }

    @Temporal(TemporalType.DATE)
    public Date getTemporalValuePA() {
        return this.temporalValuePA;
    }

    public void setTemporalValuePA(Date temporalValuePA) {
        this.temporalValuePA = temporalValuePA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof TemporalPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        return "temporalValuePA=" + (temporalValuePA != null ? sdf.format(getTemporalValuePA()).toString() : "null");
    }

}
