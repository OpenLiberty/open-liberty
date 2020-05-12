package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class XMLListEnumeratedEmbed implements java.io.Serializable {

    public enum XMLListEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final List<XMLListEnumeratedEnum> INIT = new ArrayList<XMLListEnumeratedEnum>(Arrays.asList(XMLListEnumeratedEnum.THREE,
                                                                                                              XMLListEnumeratedEnum.ONE));
    public static final List<XMLListEnumeratedEnum> UPDATE = new ArrayList<XMLListEnumeratedEnum>(Arrays
                    .asList(XMLListEnumeratedEnum.THREE, XMLListEnumeratedEnum.ONE, XMLListEnumeratedEnum.TWO));

    private List<XMLListEnumeratedEnum> listEnumerated;

    public XMLListEnumeratedEmbed() {
    }

    public XMLListEnumeratedEmbed(List<XMLListEnumeratedEnum> listEnumerated) {
        this.listEnumerated = listEnumerated;
    }

    public List<XMLListEnumeratedEnum> getListEnumerated() {
        return this.listEnumerated;
    }

    public void setListEnumerated(List<XMLListEnumeratedEnum> listEnumerated) {
        this.listEnumerated = listEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLListEnumeratedEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (listEnumerated != null)
            sb.append("listEnumerated=" + listEnumerated.toString());
        else
            sb.append("listEnumerated=null");
        return sb.toString();
    }

}
