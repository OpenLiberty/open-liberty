package com.ibm.ws.jpa.embeddable.basic.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.FIELD)
@SuppressWarnings("serial")
public class IntegerFieldAccessEmbed implements java.io.Serializable {

    @Column(name = "integerValueFAColumn")
    private Integer integerValueFieldAccessColumn;

    public IntegerFieldAccessEmbed() {
    }

    public IntegerFieldAccessEmbed(int integerValueFieldAccessColumn) {
        this.integerValueFieldAccessColumn = new Integer(integerValueFieldAccessColumn);
    }

    public Integer getIntegerValueFieldAccessColumn() {
        return this.integerValueFieldAccessColumn;
    }

    public void setIntegerValueFieldAccessColumn(
                                                 Integer integerValueFieldAccessColumn) {
        this.integerValueFieldAccessColumn = integerValueFieldAccessColumn;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof IntegerFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "integerValueFieldAccessColumn=" + integerValueFieldAccessColumn;
    }

}
