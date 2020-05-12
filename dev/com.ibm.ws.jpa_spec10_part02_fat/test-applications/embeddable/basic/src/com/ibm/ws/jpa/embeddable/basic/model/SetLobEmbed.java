package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;

@Embeddable
@SuppressWarnings("serial")
public class SetLobEmbed implements java.io.Serializable {

    public static final Set<String> INIT = new HashSet<String>(Arrays.asList("Init1", "Init2"));
    public static final Set<String> UPDATE = new HashSet<String>(Arrays.asList("Update1", "Update2", "Update3"));

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SetLob", joinColumns = @JoinColumn(name = "parent_id"))
    @Column(name = "value")
    @Lob
    private Set<String> setLob;

    public SetLobEmbed() {
    }

    public SetLobEmbed(Set<String> setLob) {
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
        if (!(otherObject instanceof SetLobEmbed))
            return false;
        return (((SetLobEmbed) otherObject).setLob.equals(setLob)); // Can't use hash b/c not sorted.
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
