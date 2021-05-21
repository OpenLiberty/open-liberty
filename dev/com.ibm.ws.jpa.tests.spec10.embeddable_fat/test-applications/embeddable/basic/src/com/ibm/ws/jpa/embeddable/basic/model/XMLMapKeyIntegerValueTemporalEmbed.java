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

public class XMLMapKeyIntegerValueTemporalEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static Map<Integer, Date> INIT;
    public static Map<Integer, Date> UPDATE;
    static {
        Map<Integer, Date> map = new HashMap<Integer, Date>();
        map.put(new Integer(2), new Date(System.currentTimeMillis() - 200000000));
        map.put(new Integer(1), new Date(1));
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<Integer, Date>();
        map.put(new Integer(2), new Date(System.currentTimeMillis() - 200000000));
        map.put(new Integer(3), new Date());
        map.put(new Integer(1), new Date(1));
        UPDATE = Collections.unmodifiableMap(map);
    }

    private Map<Integer, Date> mapKeyIntegerValueTemporal;

    public XMLMapKeyIntegerValueTemporalEmbed() {
    }

    public XMLMapKeyIntegerValueTemporalEmbed(Map<Integer, Date> mapKeyIntegerValueTemporal) {
        this.mapKeyIntegerValueTemporal = mapKeyIntegerValueTemporal;
    }

    public Map<Integer, Date> getMapKeyIntegerValueTemporal() {
        return this.mapKeyIntegerValueTemporal;
    }

    public void setMapKeyIntegerValueTemporal(Map<Integer, Date> mapKeyIntegerValueTemporal) {
        this.mapKeyIntegerValueTemporal = mapKeyIntegerValueTemporal;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLMapKeyIntegerValueTemporalEmbed))
            return false;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        Set<String> dateOnly = new HashSet<String>();
        Set<Integer> keys = mapKeyIntegerValueTemporal.keySet();
        for (Integer key : keys)
            dateOnly.add(key + "=" + sdf.format(mapKeyIntegerValueTemporal.get(key)));
        Set<String> otherDateOnly = new HashSet<String>();
        Map<Integer, Date> otherMap = ((XMLMapKeyIntegerValueTemporalEmbed) otherObject).getMapKeyIntegerValueTemporal();
        Set<Integer> otherKeys = otherMap.keySet();
        for (Integer key : otherKeys)
            otherDateOnly.add(key + "=" + sdf.format(otherMap.get(key)));
        return (otherDateOnly.equals(dateOnly)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mapKeyIntegerValueTemporal != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            sb.append("mapKeyIntegerValueTemporal=[");
            Set<Integer> keys = mapKeyIntegerValueTemporal.keySet();
            for (Integer key : keys) {
                sb.append(key + "=");
                sb.append(sdf.format(mapKeyIntegerValueTemporal.get(key)).toString());
                sb.append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        } else
            sb.append("mapKeyIntegerValueTemporal=null");
        return sb.toString();
    }

}
