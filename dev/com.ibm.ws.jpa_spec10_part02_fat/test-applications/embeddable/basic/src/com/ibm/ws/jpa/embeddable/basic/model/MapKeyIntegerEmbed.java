package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

@Embeddable
@SuppressWarnings("serial")
public class MapKeyIntegerEmbed implements java.io.Serializable {

    public static final Map<Integer, Integer> INIT;
    static {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(new Integer(3), new Integer(300));
        map.put(new Integer(1), new Integer(100));
        map.put(new Integer(2), new Integer(200));
        INIT = Collections.unmodifiableMap(map);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MapIntInt", joinColumns = @JoinColumn(name = "parent_id"))
    @MapKeyColumn(name = "mykey")
    @Column(name = "value")
    private Map<Integer, Integer> notMapKeyInteger;

    public MapKeyIntegerEmbed() {
    }

    public MapKeyIntegerEmbed(Map<Integer, Integer> notMapKeyInteger) {
        this.notMapKeyInteger = notMapKeyInteger;
    }

    public Map<Integer, Integer> getNotMapKeyInteger() {
        return this.notMapKeyInteger;
    }

    public void setNotMapKeyInteger(Map<Integer, Integer> notMapKeyInteger) {
        this.notMapKeyInteger = notMapKeyInteger;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof MapKeyIntegerEmbed))
            return false;
        return (((MapKeyIntegerEmbed) otherObject).notMapKeyInteger.equals(notMapKeyInteger)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (notMapKeyInteger != null)
            sb.append("notMapKeyInteger=" + notMapKeyInteger.toString());
        else
            sb.append("notMapKeyInteger=null");
        return sb.toString();
    }

}
