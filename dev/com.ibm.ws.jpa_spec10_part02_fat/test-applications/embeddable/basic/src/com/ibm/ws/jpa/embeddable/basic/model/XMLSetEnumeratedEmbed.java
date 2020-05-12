package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class XMLSetEnumeratedEmbed implements java.io.Serializable {

    public enum XMLSetEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Set<XMLSetEnumeratedEnum> INIT = new HashSet<XMLSetEnumeratedEnum>(Arrays.asList(XMLSetEnumeratedEnum.ONE, XMLSetEnumeratedEnum.THREE));
    public static final Set<XMLSetEnumeratedEnum> UPDATE = new HashSet<XMLSetEnumeratedEnum>(Arrays.asList(XMLSetEnumeratedEnum.ONE, XMLSetEnumeratedEnum.THREE,
                                                                                                           XMLSetEnumeratedEnum.TWO));

    private Set<XMLSetEnumeratedEnum> setEnumerated;

    public XMLSetEnumeratedEmbed() {
    }

    public XMLSetEnumeratedEmbed(Set<XMLSetEnumeratedEnum> setEnumerated) {
        this.setEnumerated = setEnumerated;
    }

    public Set<XMLSetEnumeratedEnum> getSetEnumerated() {
        return this.setEnumerated;
    }

    public void setSetEnumerated(Set<XMLSetEnumeratedEnum> setEnumerated) {
        this.setEnumerated = setEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLSetEnumeratedEmbed))
            return false;
        return (((XMLSetEnumeratedEmbed) otherObject).setEnumerated.equals(setEnumerated)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (setEnumerated != null)
            sb.append("setEnumerated=" + setEnumerated.toString());
        else
            sb.append("setEnumerated=null");
        return sb.toString();
    }

}
