package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class XMLMapKeyEnumeratedValueEnumeratedEmbed implements java.io.Serializable {

    public enum XMLMapKeyEnumeratedValueEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> INIT;
    public static final Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> UPDATE;
    static {
        Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> map = new HashMap<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum>();
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.TWO, XMLMapKeyEnumeratedValueEnumeratedEnum.ONE);
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.ONE, XMLMapKeyEnumeratedValueEnumeratedEnum.TWO);
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum>();
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.THREE, XMLMapKeyEnumeratedValueEnumeratedEnum.TWO);
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.TWO, XMLMapKeyEnumeratedValueEnumeratedEnum.ONE);
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.ONE, XMLMapKeyEnumeratedValueEnumeratedEnum.THREE);
        UPDATE = Collections.unmodifiableMap(map);
    }

    private Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated;

    public XMLMapKeyEnumeratedValueEnumeratedEmbed() {
    }

    public XMLMapKeyEnumeratedValueEnumeratedEmbed(Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated) {
        this.mapKeyEnumeratedValueEnumerated = mapKeyEnumeratedValueEnumerated;
    }

    public Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> getMapKeyEnumeratedValueEnumerated() {
        return this.mapKeyEnumeratedValueEnumerated;
    }

    public void setMapKeyEnumeratedValueEnumerated(Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated) {
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
        if (!(otherObject instanceof XMLMapKeyEnumeratedValueEnumeratedEmbed))
            return false;
        return (((XMLMapKeyEnumeratedValueEnumeratedEmbed) otherObject).mapKeyEnumeratedValueEnumerated.equals(mapKeyEnumeratedValueEnumerated)); // Can't use hash b/c not sorted.
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
