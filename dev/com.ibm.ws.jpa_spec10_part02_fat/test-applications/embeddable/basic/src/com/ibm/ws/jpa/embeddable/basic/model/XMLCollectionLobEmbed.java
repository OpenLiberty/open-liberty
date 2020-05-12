package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("serial")
public class XMLCollectionLobEmbed implements java.io.Serializable {

    public static final Collection<String> INIT = Arrays.asList("Init1", "Init2");
    public static final Collection<String> UPDATE = Arrays.asList("Update1", "Update2", "Update3");

    private Collection<String> collectionLob;

    public XMLCollectionLobEmbed() {
    }

    public XMLCollectionLobEmbed(Collection<String> collectionLob) {
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
        if (!(otherObject instanceof XMLCollectionLobEmbed))
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
