package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class XMLListIntegerOrderColumnEmbed implements java.io.Serializable {

    public static final List<Integer> INIT = new ArrayList<Integer>(Arrays.asList(new Integer(2), new Integer(3), new Integer(1)));
    public static final List<Integer> UPDATE = new ArrayList<Integer>(Arrays.asList(new Integer(2), new Integer(4), new Integer(3), new Integer(1)));

    private List<Integer> notListIntegerOrderColumn;

    public XMLListIntegerOrderColumnEmbed() {
    }

    public XMLListIntegerOrderColumnEmbed(List<Integer> notListIntegerOrderColumn) {
        this.notListIntegerOrderColumn = notListIntegerOrderColumn;
    }

    public List<Integer> getNotListIntegerOrderColumn() {
        return this.notListIntegerOrderColumn;
    }

    public void setNotListIntegerOrderColumn(List<Integer> notListIntegerOrderColumn) {
        this.notListIntegerOrderColumn = notListIntegerOrderColumn;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLListIntegerOrderColumnEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (notListIntegerOrderColumn != null)
            sb.append("notListIntegerOrderColumn=" + notListIntegerOrderColumn.toString());
        else
            sb.append("notListIntegerOrderColumn=null");
        return sb.toString();
    }

}
