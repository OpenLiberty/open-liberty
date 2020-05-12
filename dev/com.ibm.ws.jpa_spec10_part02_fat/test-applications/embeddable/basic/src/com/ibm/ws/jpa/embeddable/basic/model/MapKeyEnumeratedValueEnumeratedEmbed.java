package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;

@Embeddable
@SuppressWarnings("serial")
public class MapKeyEnumeratedValueEnumeratedEmbed implements java.io.Serializable {

    public enum MapKeyEnumeratedValueEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Map<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum> INIT;
    public static final Map<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum> UPDATE;
    static {
        Map<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum> map = new HashMap<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum>();
        map.put(MapKeyEnumeratedValueEnumeratedEnum.TWO, MapKeyEnumeratedValueEnumeratedEnum.ONE);
        map.put(MapKeyEnumeratedValueEnumeratedEnum.ONE, MapKeyEnumeratedValueEnumeratedEnum.TWO);
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum>();
        map.put(MapKeyEnumeratedValueEnumeratedEnum.THREE, MapKeyEnumeratedValueEnumeratedEnum.TWO);
        map.put(MapKeyEnumeratedValueEnumeratedEnum.TWO, MapKeyEnumeratedValueEnumeratedEnum.ONE);
        map.put(MapKeyEnumeratedValueEnumeratedEnum.ONE, MapKeyEnumeratedValueEnumeratedEnum.THREE);
        UPDATE = Collections.unmodifiableMap(map);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MapEnumEnum", joinColumns = @JoinColumn(name = "parent_id"))
    @Column(name = "value")
    @Enumerated(EnumType.STRING)
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "mykey")
    private Map<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated;

    public MapKeyEnumeratedValueEnumeratedEmbed() {
    }

    public MapKeyEnumeratedValueEnumeratedEmbed(Map<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated) {
        this.mapKeyEnumeratedValueEnumerated = mapKeyEnumeratedValueEnumerated;
    }

    public Map<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum> getMapKeyEnumeratedValueEnumerated() {
        return this.mapKeyEnumeratedValueEnumerated;
    }

    public void setMapKeyEnumeratedValueEnumerated(Map<MapKeyEnumeratedValueEnumeratedEnum, MapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated) {
        this.mapKeyEnumeratedValueEnumerated = mapKeyEnumeratedValueEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof MapKeyEnumeratedValueEnumeratedEmbed))
            return false;
        return (((MapKeyEnumeratedValueEnumeratedEmbed) otherObject).mapKeyEnumeratedValueEnumerated.equals(mapKeyEnumeratedValueEnumerated)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mapKeyEnumeratedValueEnumerated != null)
            sb.append("mapKeyEnumeratedValueEnumerated=" + mapKeyEnumeratedValueEnumerated.toString());
        else
            sb.append("mapKeyEnumeratedValueEnumerated=null");
        return sb.toString();
    }

}
