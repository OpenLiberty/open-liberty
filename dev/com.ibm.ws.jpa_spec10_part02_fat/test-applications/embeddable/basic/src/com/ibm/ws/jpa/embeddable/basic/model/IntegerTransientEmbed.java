package com.ibm.ws.jpa.embeddable.basic.model;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
@SuppressWarnings("serial")
public class IntegerTransientEmbed implements java.io.Serializable {

    private transient Integer transientJavaValue;

    @Transient
    private Integer transientValue;

    public IntegerTransientEmbed() {
    }

    public IntegerTransientEmbed(int transientJavaValue, int transientValue) {
        this.transientJavaValue = new Integer(transientJavaValue);
        this.transientValue = new Integer(transientValue);
    }

    public Integer getTransientJavaValue() {
        return this.transientJavaValue;
    }

    public void setTransientJavaValue(Integer transientJavaValue) {
        this.transientJavaValue = transientJavaValue;
    }

    public Integer getTransientValue() {
        return this.transientValue;
    }

    public void setTransientValue(Integer transientValue) {
        this.transientValue = transientValue;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof IntegerTransientEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "transientJavaValue=" + transientJavaValue + ", transientValue=" + transientValue;
    }

}
