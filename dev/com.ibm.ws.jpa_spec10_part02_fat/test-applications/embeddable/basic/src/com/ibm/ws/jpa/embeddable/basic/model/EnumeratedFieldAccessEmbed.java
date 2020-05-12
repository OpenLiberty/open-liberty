package com.ibm.ws.jpa.embeddable.basic.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
@Access(AccessType.FIELD)
@SuppressWarnings("serial")
public class EnumeratedFieldAccessEmbed implements java.io.Serializable {

    public enum EnumeratedFieldAccessEnum {
        ONE, TWO, THREE
    }

    @Enumerated(EnumType.STRING)
    private EnumeratedFieldAccessEnum enumeratedStringValueFA;

    @Enumerated(EnumType.ORDINAL)
    private EnumeratedFieldAccessEnum enumeratedOrdinalValueFA;

    public EnumeratedFieldAccessEmbed() {
    }

    public EnumeratedFieldAccessEmbed(EnumeratedFieldAccessEnum enumeratedStringValueFA) {
        this.enumeratedStringValueFA = enumeratedStringValueFA;
        this.enumeratedOrdinalValueFA = enumeratedStringValueFA;
    }

    public EnumeratedFieldAccessEnum getEnumeratedStringValueFA() {
        return this.enumeratedStringValueFA;
    }

    public void setEnumeratedStringValueFA(EnumeratedFieldAccessEnum enumeratedStringValueFA) {
        this.enumeratedStringValueFA = enumeratedStringValueFA;
    }

    public EnumeratedFieldAccessEnum getEnumeratedOrdinalValueFA() {
        return this.enumeratedOrdinalValueFA;
    }

    public void setEnumeratedOrdinalValueFA(EnumeratedFieldAccessEnum enumeratedOrdinalValueFA) {
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
        if (!(otherObject instanceof EnumeratedFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "enumeratedStringValueFA=" + enumeratedStringValueFA
               + ", enumeratedOrdinalValueFA=" + enumeratedOrdinalValueFA;
    }

}
