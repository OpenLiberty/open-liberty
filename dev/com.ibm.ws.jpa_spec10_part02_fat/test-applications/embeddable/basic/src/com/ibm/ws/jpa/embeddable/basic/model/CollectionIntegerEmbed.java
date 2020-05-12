package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;

@Embeddable
@SuppressWarnings("serial")
public class CollectionIntegerEmbed implements java.io.Serializable {

    public static final Collection<Integer> INIT = Arrays.asList(new Integer(2), new Integer(3), new Integer(1));
    public static final Collection<Integer> UPDATE = Arrays.asList(new Integer(2), new Integer(4), new Integer(3), new Integer(1));

    @CollectionTable(name = "ColInt", joinColumns = @JoinColumn(name = "parent_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "value")
//    @OrderColumn(name = "valueOrderColumn")
    private Collection<Integer> collectionInteger;

    public CollectionIntegerEmbed() {
    }

    public CollectionIntegerEmbed(Collection<Integer> collectionInteger) {
        this.collectionInteger = collectionInteger;
    }

    public Collection<Integer> getCollectionInteger() {
        return this.collectionInteger;
    }

    public void setCollectionInteger(Collection<Integer> collectionInteger) {
        this.collectionInteger = collectionInteger;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof CollectionIntegerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionInteger != null)
            sb.append("collectionInteger=" + collectionInteger.toString());
        else
            sb.append("collectionInteger=null");
        return sb.toString();
    }

}
