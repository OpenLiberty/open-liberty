package com.ibm.ws.jpa.entitymanager.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class JPA20EntityManagerEntityB implements java.io.Serializable {
    @Id
    private int id;

    private String strData;

    @ManyToMany(mappedBy = "entityBList")
    private List<JPA20EntityManagerEntityA> entityAList;

    public JPA20EntityManagerEntityB() {
        entityAList = new ArrayList<JPA20EntityManagerEntityA>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStrData() {
        return strData;
    }

    public void setStrData(String strData) {
        this.strData = strData;
    }

    public List<JPA20EntityManagerEntityA> getEntityAList() {
        return entityAList;
    }

    @Override
    public String toString() {
        return "JPA20EntityManagerEntityB [id=" + id + ", strData=" + strData + "]";
    }
}
