package com.ibm.ws.jpa.entitymanager.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

@Entity
public class JPA10EntityManagerEntityA {
    @Id
    private int id;

    private String strData;

    @OneToOne(mappedBy = "entityA")
    private JPA10EntityManagerEntityC entityC;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "entityALazy")
    private JPA10EntityManagerEntityC entityCLazy;

    @ManyToMany
    private List<JPA10EntityManagerEntityB> entityBList;

    public JPA10EntityManagerEntityA() {
        entityBList = new ArrayList<JPA10EntityManagerEntityB>();
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

    public JPA10EntityManagerEntityC getEntityC() {
        return entityC;
    }

    public void setEntityC(JPA10EntityManagerEntityC entityC) {
        this.entityC = entityC;
    }

    public List<JPA10EntityManagerEntityB> getEntityBList() {
        return entityBList;
    }

    public JPA10EntityManagerEntityC getEntityCLazy() {
        return entityCLazy;
    }

    public void setEntityCLazy(JPA10EntityManagerEntityC entityCLazy) {
        this.entityCLazy = entityCLazy;
    }

    @Override
    public String toString() {
        return "JPA10EntityManagerEntityA [id=" + id + ", strData=" + strData + "]";
    }
}
