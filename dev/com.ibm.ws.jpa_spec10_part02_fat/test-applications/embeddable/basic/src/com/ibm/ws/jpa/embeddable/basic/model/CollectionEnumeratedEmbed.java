package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;

@Embeddable
@SuppressWarnings("serial")
public class CollectionEnumeratedEmbed implements java.io.Serializable {

    public enum CollectionEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Collection<CollectionEnumeratedEnum> INIT = Arrays.asList(CollectionEnumeratedEnum.THREE, CollectionEnumeratedEnum.ONE);
    public static final Collection<CollectionEnumeratedEnum> UPDATE = Arrays.asList(CollectionEnumeratedEnum.THREE, CollectionEnumeratedEnum.ONE, CollectionEnumeratedEnum.TWO);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ColEnum", joinColumns = @JoinColumn(name = "parent_id"))
//    @OrderColumn(name = "valueOrderColumn")
    @Column(name = "value")
    @Enumerated(EnumType.STRING)
    private Collection<CollectionEnumeratedEnum> collectionEnumerated;

    public CollectionEnumeratedEmbed() {
    }

    public CollectionEnumeratedEmbed(Collection<CollectionEnumeratedEnum> collectionEnumerated) {
        this.collectionEnumerated = collectionEnumerated;
    }

    public Collection<CollectionEnumeratedEnum> getCollectionEnumerated() {
        return this.collectionEnumerated;
    }

    public void setCollectionEnumerated(Collection<CollectionEnumeratedEnum> collectionEnumerated) {
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
        if (!(otherObject instanceof CollectionEnumeratedEmbed))
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
