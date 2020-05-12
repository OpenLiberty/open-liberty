package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@SuppressWarnings("serial")
public class IntegerAttributeOverridesEmbed implements java.io.Serializable {

    public static final List<IntegerAttributeOverridesEmbed> LIST_INIT = Arrays.asList(new IntegerAttributeOverridesEmbed(new Integer(2)),
                                                                                       new IntegerAttributeOverridesEmbed(new Integer(3)),
                                                                                       new IntegerAttributeOverridesEmbed(new Integer(1)));
    public static final List<IntegerAttributeOverridesEmbed> LIST_UPDATE = Arrays.asList(new IntegerAttributeOverridesEmbed(new Integer(2)),
                                                                                         new IntegerAttributeOverridesEmbed(new Integer(4)),
                                                                                         new IntegerAttributeOverridesEmbed(new Integer(3)),
                                                                                         new IntegerAttributeOverridesEmbed(new Integer(1)));

    @Column(name = "doesNotExist")
    private Integer notIntegerValue;

    public IntegerAttributeOverridesEmbed() {
    }

    public IntegerAttributeOverridesEmbed(int notIntegerValue) {
        this.notIntegerValue = new Integer(notIntegerValue);
    }

    public Integer getNotIntegerValue() {
        return this.notIntegerValue;
    }

    public void setNotIntegerValue(Integer notIntegerValue) {
        this.notIntegerValue = notIntegerValue;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof IntegerAttributeOverridesEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "notIntegerValue=" + notIntegerValue;
    }

}
