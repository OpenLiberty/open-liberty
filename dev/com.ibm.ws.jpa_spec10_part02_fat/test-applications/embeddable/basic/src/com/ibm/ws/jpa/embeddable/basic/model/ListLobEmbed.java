package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OrderColumn;

@Embeddable
@SuppressWarnings("serial")
public class ListLobEmbed implements java.io.Serializable {

    public static final List<String> INIT = new ArrayList<String>(Arrays.asList("Init1", "Init2"));
    public static final List<String> UPDATE = new ArrayList<String>(Arrays.asList("Update1", "Update2", "Update3"));

    @CollectionTable(name = "ListLob", joinColumns = @JoinColumn(name = "parent_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Lob
    @Column(name = "value")
    @OrderColumn(name = "valueOrderColumn")
    private List<String> listLob;

    public ListLobEmbed() {
    }

    public ListLobEmbed(List<String> listLob) {
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
        if (!(otherObject instanceof ListLobEmbed))
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
