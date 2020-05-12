package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("serial")
public class XMLCollectionEnumeratedEmbed implements java.io.Serializable {

    public enum XMLCollectionEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Collection<XMLCollectionEnumeratedEnum> INIT = Arrays.asList(XMLCollectionEnumeratedEnum.THREE, XMLCollectionEnumeratedEnum.ONE);
    public static final Collection<XMLCollectionEnumeratedEnum> UPDATE = Arrays.asList(XMLCollectionEnumeratedEnum.THREE, XMLCollectionEnumeratedEnum.ONE,
                                                                                       XMLCollectionEnumeratedEnum.TWO);

    private Collection<XMLCollectionEnumeratedEnum> collectionEnumerated;

    public XMLCollectionEnumeratedEmbed() {
    }

    public XMLCollectionEnumeratedEmbed(Collection<XMLCollectionEnumeratedEnum> collectionEnumerated) {
        this.collectionEnumerated = collectionEnumerated;
    }

    public Collection<XMLCollectionEnumeratedEnum> getCollectionEnumerated() {
        return this.collectionEnumerated;
    }

    public void setCollectionEnumerated(Collection<XMLCollectionEnumeratedEnum> collectionEnumerated) {
        this.collectionEnumerated = collectionEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLCollectionEnumeratedEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionEnumerated != null)
            sb.append("collectionEnumerated=" + collectionEnumerated.toString());
        else
            sb.append("collectionEnumerated=null");
        return sb.toString();
    }

}
