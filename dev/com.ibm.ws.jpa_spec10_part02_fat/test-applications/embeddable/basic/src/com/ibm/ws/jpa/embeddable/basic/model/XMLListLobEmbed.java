package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class XMLListLobEmbed implements java.io.Serializable {

    public static final List<String> INIT = new ArrayList<String>(Arrays.asList("Init1", "Init2"));
    public static final List<String> UPDATE = new ArrayList<String>(Arrays.asList("Update1", "Update2", "Update3"));

    private List<String> listLob;

    public XMLListLobEmbed() {
    }

    public XMLListLobEmbed(List<String> listLob) {
        this.listLob = listLob;
    }

    public List<String> getListLob() {
        return this.listLob;
    }

    public void setListLob(List<String> listLob) {
        this.listLob = listLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLListLobEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (listLob != null)
            sb.append("listLob=" + listLob.toString());
        else
            sb.append("listLob=null");
        return sb.toString();
    }

}
