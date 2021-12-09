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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XMLMapKeyTemporalValueTemporalEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Map<Date, Date> INIT;
    public static final Map<Date, Date> UPDATE;
    static {
        Map<Date, Date> map = new HashMap<Date, Date>();
        map.put(new Date(1), new Date(System.currentTimeMillis() - 200000000));
        map.put(new Date(System.currentTimeMillis() - 200000000), new Date(1));
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<Date, Date>();
        map.put(new Date(), new Date(System.currentTimeMillis() - 200000000));
        map.put(new Date(System.currentTimeMillis() - 200000000), new Date());
        map.put(new Date(1), new Date(1));
        UPDATE = Collections.unmodifiableMap(map);
    }

    private Map<Date, Date> mapKeyTemporalValueTemporal;

    public XMLMapKeyTemporalValueTemporalEmbed() {
    }

    public XMLMapKeyTemporalValueTemporalEmbed(Map<Date, Date> mapKeyTemporalValueTemporal) {
        this.mapKeyTemporalValueTemporal = mapKeyTemporalValueTemporal;
    }

    public Map<Date, Date> getMapKeyTemporalValueTemporal() {
        return this.mapKeyTemporalValueTemporal;
    }

    public void setMapKeyTemporalValueTemporal(Map<Date, Date> mapKeyTemporalValueTemporal) {
        this.mapKeyTemporalValueTemporal = mapKeyTemporalValueTemporal;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLMapKeyTemporalValueTemporalEmbed))
            return false;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        Set<String> dateOnly = new HashSet<String>();
        Set<Date> keys = mapKeyTemporalValueTemporal.keySet();
        for (Date key : keys)
            dateOnly.add(sdf.format(key) + "=" + sdf.format(mapKeyTemporalValueTemporal.get(key)));
        Set<String> otherDateOnly = new HashSet<String>();
        Map<Date, Date> otherMap = ((XMLMapKeyTemporalValueTemporalEmbed) otherObject).getMapKeyTemporalValueTemporal();
        Set<Date> otherKeys = otherMap.keySet();
        for (Date key : otherKeys)
            otherDateOnly.add(sdf.format(key) + "=" + sdf.format(otherMap.get(key)));
        return (otherDateOnly.equals(dateOnly)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mapKeyTemporalValueTemporal != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            sb.append("mapKeyTemporalValueTemporal=[");
            Set<Date> keys = mapKeyTemporalValueTemporal.keySet();
            for (Date key : keys) {
                sb.append(sdf.format(key) + "=");
                sb.append(sdf.format(mapKeyTemporalValueTemporal.get(key)).toString());
                sb.append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        } else
            sb.append("mapKeyTemporalValueTemporal=null");
        return sb.toString();
    }

}
