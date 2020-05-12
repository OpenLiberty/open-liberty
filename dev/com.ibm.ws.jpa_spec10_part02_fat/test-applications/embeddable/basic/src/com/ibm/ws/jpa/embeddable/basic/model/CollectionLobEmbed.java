package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;

@Embeddable
@SuppressWarnings("serial")
public class CollectionLobEmbed implements java.io.Serializable {

    public static final Collection<String> INIT = Arrays.asList("Init1", "Init2");
    public static final Collection<String> UPDATE = Arrays.asList("Update1", "Update2", "Update3");

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ColLob", joinColumns = @JoinColumn(name = "parent_id"))
//    @OrderColumn(name = "valueOrderColumn")
    @Column(name = "value")
    @Lob
    private Collection<String> collectionLob;

    public CollectionLobEmbed() {
    }

    public CollectionLobEmbed(Collection<String> collectionLob) {
        this.collectionLob = collectionLob;
    }

    public Collection<String> getCollectionLob() {
        return this.collectionLob;
    }

    public void setCollectionLob(Collection<String> collectionLob) {
        this.collectionLob = collectionLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof CollectionLobEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionLob != null)
            sb.append("collectionLob=" + collectionLob.toString());
        else
            sb.append("collectionLob=null");
        return sb.toString();
    }

}
