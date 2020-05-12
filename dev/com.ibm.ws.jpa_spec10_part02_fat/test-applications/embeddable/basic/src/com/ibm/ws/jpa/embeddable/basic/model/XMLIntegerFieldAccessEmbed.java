package com.ibm.ws.jpa.embeddable.basic.model;

@SuppressWarnings("serial")
public class XMLIntegerFieldAccessEmbed implements java.io.Serializable {

    private Integer integerValueFieldAccessColumn;

    public XMLIntegerFieldAccessEmbed() {
    }

    public XMLIntegerFieldAccessEmbed(int integerValueFieldAccessColumn) {
        this.integerValueFieldAccessColumn =
            new Integer(integerValueFieldAccessColumn);
    }

    public Integer getIntegerValueFieldAccessColumn() {
        return this.integerValueFieldAccessColumn;
    }

    public void setIntegerValueFieldAccessColumn(
        Integer integerValueFieldAccessColumn) {
        this.integerValueFieldAccessColumn = integerValueFieldAccessColumn;
    }

    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLIntegerFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    public String toString() {
        return "integerValueFieldAccessColumn=" + integerValueFieldAccessColumn;
    }

}
