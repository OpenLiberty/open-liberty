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

@Embeddable
@SuppressWarnings("serial")
public class SetIntegerEmbed implements java.io.Serializable {

    public static final Set<Integer> INIT = new HashSet<Integer>(Arrays.asList(new Integer(2), new Integer(1)));

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SetInt", joinColumns = @JoinColumn(name = "parent_id"))
    @Column(name = "value")
    private Set<Integer> notSetInteger;

    public SetIntegerEmbed() {
    }

    public SetIntegerEmbed(Set<Integer> notSetInteger) {
        this.notSetInteger = notSetInteger;
    }

    public Set<Integer> getNotSetInteger() {
        return this.notSetInteger;
    }

    public void setNotSetInteger(Set<Integer> notSetInteger) {
        this.notSetInteger = notSetInteger;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof SetIntegerEmbed))
            return false;
        return (((SetIntegerEmbed) otherObject).notSetInteger.equals(notSetInteger)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (notSetInteger != null)
            sb.append("notSetInteger=" + notSetInteger.toString());
        else
            sb.append("notSetInteger=null");
        return sb.toString();
    }

}
