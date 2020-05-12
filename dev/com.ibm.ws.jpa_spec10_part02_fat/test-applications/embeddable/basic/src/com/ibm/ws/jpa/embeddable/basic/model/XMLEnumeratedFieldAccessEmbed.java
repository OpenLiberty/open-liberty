package com.ibm.ws.jpa.embeddable.basic.model;

@SuppressWarnings("serial")
public class XMLEnumeratedFieldAccessEmbed implements java.io.Serializable {

    private XMLEnumeratedFieldAccessEnum enumeratedStringValueFA;
    private XMLEnumeratedFieldAccessEnum enumeratedOrdinalValueFA;

    public enum XMLEnumeratedFieldAccessEnum {
        ONE, TWO, THREE
    }

    public XMLEnumeratedFieldAccessEmbed() {
    }

    public XMLEnumeratedFieldAccessEmbed(XMLEnumeratedFieldAccessEnum enumeratedStringValueFA) {
        this.enumeratedStringValueFA = enumeratedStringValueFA;
        this.enumeratedOrdinalValueFA = enumeratedStringValueFA;
    }

    public XMLEnumeratedFieldAccessEnum getEnumeratedStringValueFA() {
        return this.enumeratedStringValueFA;
    }

    public void setEnumeratedStringValueFA(XMLEnumeratedFieldAccessEnum enumeratedStringValueFA) {
        this.enumeratedStringValueFA = enumeratedStringValueFA;
    }

    public XMLEnumeratedFieldAccessEnum getEnumeratedOrdinalValueFA() {
        return this.enumeratedOrdinalValueFA;
    }

    public void setEnumeratedOrdinalValueFA(XMLEnumeratedFieldAccessEnum enumeratedOrdinalValueFA) {
        this.enumeratedOrdinalValueFA = enumeratedOrdinalValueFA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLEnumeratedFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "enumeratedStringValueFA=" + enumeratedStringValueFA
               + ", enumeratedOrdinalValueFA=" + enumeratedOrdinalValueFA;
    }

}
