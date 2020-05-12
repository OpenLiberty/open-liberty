package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class XMLSetIntegerEmbed implements java.io.Serializable {

    public static final Set<Integer> INIT = new HashSet<Integer>(Arrays.asList(new Integer(2), new Integer(1)));

    private Set<Integer> notSetInteger;

    public XMLSetIntegerEmbed() {
    }

    public XMLSetIntegerEmbed(Set<Integer> notSetInteger) {
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
        if (!(otherObject instanceof XMLSetIntegerEmbed))
            return false;
        return (((XMLSetIntegerEmbed) otherObject).notSetInteger.equals(notSetInteger)); // Can't use hash b/c not sorted.
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
