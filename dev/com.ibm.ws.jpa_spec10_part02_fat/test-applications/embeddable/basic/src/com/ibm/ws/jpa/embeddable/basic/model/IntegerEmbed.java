package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Embeddable;

@Embeddable
@SuppressWarnings("serial")
public class IntegerEmbed implements java.io.Serializable {

    public static final List<IntegerEmbed> LIST_INIT = Arrays.asList(new IntegerEmbed(new Integer(2)),
                                                                     new IntegerEmbed(new Integer(3)),
                                                                     new IntegerEmbed(new Integer(1)));
    public static final List<IntegerEmbed> LIST_UPDATE = Arrays.asList(new IntegerEmbed(new Integer(2)),
                                                                       new IntegerEmbed(new Integer(4)),
                                                                       new IntegerEmbed(new Integer(3)),
                                                                       new IntegerEmbed(new Integer(1)));

    public static final Map<IntegerEmbed, IntegerEmbed> MAP_INIT2;
    static {
        Map<IntegerEmbed, IntegerEmbed> map = new HashMap<IntegerEmbed, IntegerEmbed>();
        map.put(new IntegerEmbed(new Integer(3)), new IntegerEmbed(new Integer(300)));
        map.put(new IntegerEmbed(new Integer(1)), new IntegerEmbed(new Integer(100)));
        map.put(new IntegerEmbed(new Integer(2)), new IntegerEmbed(new Integer(200)));
        MAP_INIT2 = Collections.unmodifiableMap(map);
    }

    private Integer integerValue;

    public IntegerEmbed() {
    }

    public IntegerEmbed(int integerValue) {
        this.integerValue = new Integer(integerValue);
    }

    public Integer getIntegerValue() {
        return this.integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof IntegerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "integerValue=" + integerValue;
    }
}
