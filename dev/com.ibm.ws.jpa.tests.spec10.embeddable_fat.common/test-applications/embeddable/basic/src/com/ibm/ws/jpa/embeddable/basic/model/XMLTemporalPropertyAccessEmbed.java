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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLTemporalPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<XMLTemporalPropertyAccessEmbed> LIST_INIT = Arrays.asList(new XMLTemporalPropertyAccessEmbed(new Date(System.currentTimeMillis() - 200000000)),
                                                                                       new XMLTemporalPropertyAccessEmbed(new Date(1)));
    public static final List<XMLTemporalPropertyAccessEmbed> LIST_UPDATE = Arrays.asList(new XMLTemporalPropertyAccessEmbed(new Date(System.currentTimeMillis() - 200000000)),
                                                                                         new XMLTemporalPropertyAccessEmbed(new Date()),
                                                                                         new XMLTemporalPropertyAccessEmbed(new Date(1)));

    public static final Map<Integer, XMLTemporalPropertyAccessEmbed> MAP_INIT;
    public static final Map<Integer, XMLTemporalPropertyAccessEmbed> MAP_UPDATE;
    static {
        Map<Integer, XMLTemporalPropertyAccessEmbed> map = new HashMap<Integer, XMLTemporalPropertyAccessEmbed>();
        map.put(new Integer(3), new XMLTemporalPropertyAccessEmbed(new Date(System.currentTimeMillis() - 200000000)));
        map.put(new Integer(2), new XMLTemporalPropertyAccessEmbed(new Date(1)));
        MAP_INIT = Collections.unmodifiableMap(map);

        map = new HashMap<Integer, XMLTemporalPropertyAccessEmbed>();
        map.put(new Integer(3), new XMLTemporalPropertyAccessEmbed(new Date(System.currentTimeMillis() - 200000000)));
        map.put(new Integer(1), new XMLTemporalPropertyAccessEmbed(new Date()));
        map.put(new Integer(2), new XMLTemporalPropertyAccessEmbed(new Date(1)));
        MAP_UPDATE = Collections.unmodifiableMap(map);
    }

    public static final Map<Date, XMLTemporalPropertyAccessEmbed> MAP2_INIT;
    public static final Map<Date, XMLTemporalPropertyAccessEmbed> MAP2_UPDATE;
    static {
        Map<Date, XMLTemporalPropertyAccessEmbed> map = new HashMap<Date, XMLTemporalPropertyAccessEmbed>();
        map.put(new Date(1), new XMLTemporalPropertyAccessEmbed(new Date(System.currentTimeMillis() - 200000000)));
        map.put(new Date(System.currentTimeMillis() - 200000000), new XMLTemporalPropertyAccessEmbed(new Date(1)));
        MAP2_INIT = Collections.unmodifiableMap(map);

        map = new HashMap<Date, XMLTemporalPropertyAccessEmbed>();
        map.put(new Date(), new XMLTemporalPropertyAccessEmbed(new Date(System.currentTimeMillis() - 200000000)));
        map.put(new Date(1), new XMLTemporalPropertyAccessEmbed(new Date()));
        map.put(new Date(System.currentTimeMillis() - 200000000), new XMLTemporalPropertyAccessEmbed(new Date(1)));
        MAP2_UPDATE = Collections.unmodifiableMap(map);
    }

    private Date temporalValuePA;

    public XMLTemporalPropertyAccessEmbed() {
    }

    public XMLTemporalPropertyAccessEmbed(Date temporalValuePA) {
        this.temporalValuePA = temporalValuePA;
    }

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
        if (!(otherObject instanceof XMLTemporalPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        return "temporalValuePA=" + (temporalValuePA != null ? sdf.format(getTemporalValuePA()).toString() : "null");
    }

}
