package com.ibm.ws.jpa.entitymanager.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class JPA10EntityManagerEntityB implements java.io.Serializable {
    @Id
    private int id;

    private String strData;

    @ManyToMany(mappedBy = "entityBList")
    private List<JPA10EntityManagerEntityA> entityAList;

    public JPA10EntityManagerEntityB() {
        entityAList = new ArrayList<JPA10EntityManagerEntityA>();
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

    public List<JPA10EntityManagerEntityA> getEntityAList() {
        return entityAList;
    }

    @Override
    public String toString() {
        return "JPA10EntityManagerEntityB [id=" + id + ", strData=" + strData + "]";
    }
}
