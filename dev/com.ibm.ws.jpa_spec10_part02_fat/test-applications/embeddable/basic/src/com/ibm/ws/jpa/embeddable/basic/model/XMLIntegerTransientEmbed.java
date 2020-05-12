package com.ibm.ws.jpa.embeddable.basic.model;

@SuppressWarnings("serial")
public class XMLIntegerTransientEmbed implements java.io.Serializable {

    private transient Integer transientJavaValue;

    private Integer transientValue;

    public XMLIntegerTransientEmbed() {
    }

    public XMLIntegerTransientEmbed(int transientJavaValue, int transientValue) {
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

    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLIntegerTransientEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    public String toString() {
        return "transientJavaValue=" + transientJavaValue + ", transientValue=" + transientValue;
    }

}
