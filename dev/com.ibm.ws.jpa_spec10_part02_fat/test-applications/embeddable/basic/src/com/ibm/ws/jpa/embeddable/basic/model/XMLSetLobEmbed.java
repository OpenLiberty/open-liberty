package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class XMLSetLobEmbed implements java.io.Serializable {

    public static final Set<String> INIT = new HashSet<String>(Arrays.asList("Init1", "Init2"));
    public static final Set<String> UPDATE = new HashSet<String>(Arrays.asList("Update1", "Update2", "Update3"));

    private Set<String> setLob;

    public XMLSetLobEmbed() {
    }

    public XMLSetLobEmbed(Set<String> setLob) {
        this.setLob = setLob;
    }

    public Set<String> getSetLob() {
        return this.setLob;
    }

    public void setSetLob(Set<String> setLob) {
        this.setLob = setLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLSetLobEmbed))
            return false;
        return (((XMLSetLobEmbed) otherObject).setLob.equals(setLob)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (setLob != null)
            sb.append("setLob=" + setLob.toString());
        else
            sb.append("setLob=null");
        return sb.toString();
    }

}
