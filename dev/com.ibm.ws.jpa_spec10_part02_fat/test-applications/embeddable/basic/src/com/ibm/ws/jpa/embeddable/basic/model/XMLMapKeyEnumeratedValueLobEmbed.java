package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class XMLMapKeyEnumeratedValueLobEmbed implements java.io.Serializable {

    public enum XMLMapKeyEnumeratedValueLobEnum {
        ONE, TWO, THREE
    }

    public static final Map<XMLMapKeyEnumeratedValueLobEnum, String> INIT;
    public static final Map<XMLMapKeyEnumeratedValueLobEnum, String> UPDATE;
    static {
        Map<XMLMapKeyEnumeratedValueLobEnum, String> map = new HashMap<XMLMapKeyEnumeratedValueLobEnum, String>();
        map.put(XMLMapKeyEnumeratedValueLobEnum.TWO, "Init2");
        map.put(XMLMapKeyEnumeratedValueLobEnum.ONE, "Init1");
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<XMLMapKeyEnumeratedValueLobEnum, String>();
        map.put(XMLMapKeyEnumeratedValueLobEnum.THREE, "Update3");
        map.put(XMLMapKeyEnumeratedValueLobEnum.TWO, "Update2");
        map.put(XMLMapKeyEnumeratedValueLobEnum.ONE, "Update1");
        UPDATE = Collections.unmodifiableMap(map);
    }

    private Map<XMLMapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob;

    public XMLMapKeyEnumeratedValueLobEmbed() {
    }

    public XMLMapKeyEnumeratedValueLobEmbed(Map<XMLMapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob) {
        this.mapKeyEnumeratedValueLob = mapKeyEnumeratedValueLob;
    }

    public Map<XMLMapKeyEnumeratedValueLobEnum, String> getMapKeyEnumeratedValueLob() {
        return this.mapKeyEnumeratedValueLob;
    }

    public void setMapKeyEnumeratedValueLob(Map<XMLMapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob) {
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
        if (!(otherObject instanceof XMLMapKeyEnumeratedValueLobEmbed))
            return false;
        return (((XMLMapKeyEnumeratedValueLobEmbed) otherObject).mapKeyEnumeratedValueLob.equals(mapKeyEnumeratedValueLob)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mapKeyEnumeratedValueLob != null)
            sb.append("mapKeyEnumeratedValueLob=" + mapKeyEnumeratedValueLob.toString());
        else
            sb.append("mapKeyEnumeratedValueLob=null");
        return sb.toString();
    }

}
