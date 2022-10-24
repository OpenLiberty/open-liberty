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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;

@Embeddable
public class MapKeyEnumeratedValueLobEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum MapKeyEnumeratedValueLobEnum {
        ONE, TWO, THREE
    }

    public static final Map<MapKeyEnumeratedValueLobEnum, String> INIT;
    public static final Map<MapKeyEnumeratedValueLobEnum, String> UPDATE;
    static {
        Map<MapKeyEnumeratedValueLobEnum, String> map = new HashMap<MapKeyEnumeratedValueLobEnum, String>();
        map.put(MapKeyEnumeratedValueLobEnum.TWO, "Init2");
        map.put(MapKeyEnumeratedValueLobEnum.ONE, "Init1");
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<MapKeyEnumeratedValueLobEnum, String>();
        map.put(MapKeyEnumeratedValueLobEnum.THREE, "Update3");
        map.put(MapKeyEnumeratedValueLobEnum.TWO, "Update2");
        map.put(MapKeyEnumeratedValueLobEnum.ONE, "Update1");
        UPDATE = Collections.unmodifiableMap(map);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MapEnumLob", joinColumns = @JoinColumn(name = "parent_id"))
    @Column(name = "value")
    @Lob
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "mykey")
    private Map<MapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob;

    public MapKeyEnumeratedValueLobEmbed() {
    }

    public MapKeyEnumeratedValueLobEmbed(Map<MapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob) {
        this.mapKeyEnumeratedValueLob = mapKeyEnumeratedValueLob;
    }

    public Map<MapKeyEnumeratedValueLobEnum, String> getMapKeyEnumeratedValueLob() {
        return this.mapKeyEnumeratedValueLob;
    }

    public void setMapKeyEnumeratedValueLob(Map<MapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob) {
        this.mapKeyEnumeratedValueLob = mapKeyEnumeratedValueLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof MapKeyEnumeratedValueLobEmbed))
            return false;
        return (((MapKeyEnumeratedValueLobEmbed) otherObject).mapKeyEnumeratedValueLob.equals(mapKeyEnumeratedValueLob)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mapKeyEnumeratedValueLob != null) {
            // EclipseLink wraps order column Lists in an IndirectMap implementation and overrides toString()
            Map<MapKeyEnumeratedValueLobEnum, String> temp = new HashMap<MapKeyEnumeratedValueLobEnum, String>(mapKeyEnumeratedValueLob);
            sb.append("mapKeyEnumeratedValueLob=" + temp.toString());
        } else {
            sb.append("mapKeyEnumeratedValueLob=null");
        }
        return sb.toString();
    }
}
