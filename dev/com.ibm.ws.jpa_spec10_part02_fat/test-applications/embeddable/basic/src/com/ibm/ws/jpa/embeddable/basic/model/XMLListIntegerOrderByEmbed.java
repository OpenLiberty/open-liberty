package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class XMLListIntegerOrderByEmbed implements java.io.Serializable {

    public static final List<Integer> INIT = new ArrayList<Integer>(Arrays.asList(new Integer(2), new Integer(3), new Integer(1)));
    public static final List<Integer> INIT_ORDERED = new ArrayList<Integer>(Arrays.asList(new Integer(3), new Integer(2), new Integer(1)));
    public static final List<Integer> UPDATE = new ArrayList<Integer>(Arrays.asList(new Integer(2), new Integer(4), new Integer(3), new Integer(1)));
    public static final List<Integer> UPDATE_ORDERED = new ArrayList<Integer>(Arrays.asList(new Integer(4), new Integer(3), new Integer(2), new Integer(1)));

    private List<Integer> notListIntegerOrderBy;

    public XMLListIntegerOrderByEmbed() {
    }

    public XMLListIntegerOrderByEmbed(List<Integer> notListIntegerOrderBy) {
        this.notListIntegerOrderBy = notListIntegerOrderBy;
    }

    public List<Integer> getNotListIntegerOrderBy() {
        return this.notListIntegerOrderBy;
    }

    public void setNotListIntegerOrderBy(List<Integer> notListIntegerOrderBy) {
        this.notListIntegerOrderBy = notListIntegerOrderBy;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLListIntegerOrderByEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (notListIntegerOrderBy != null)
            sb.append("notListIntegerOrderBy=" + notListIntegerOrderBy.toString());
        else
            sb.append("notListIntegerOrderBy=null");
        return sb.toString();
    }

}
