package com.ibm.ws.jpa.entitymanager.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

@Entity
public class JPA20EntityManagerEntityA {
    @Id
    private int id;

    private String strData;

    @OneToOne(mappedBy = "entityA")
    private JPA20EntityManagerEntityC entityC;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "entityALazy")
    private JPA20EntityManagerEntityC entityCLazy;

    @ManyToMany
    private List<JPA20EntityManagerEntityB> entityBList;

    public JPA20EntityManagerEntityA() {
        entityBList = new ArrayList<JPA20EntityManagerEntityB>();
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

    public JPA20EntityManagerEntityC getEntityC() {
        return entityC;
    }

    public void setEntityC(JPA20EntityManagerEntityC entityC) {
        this.entityC = entityC;
    }

    public List<JPA20EntityManagerEntityB> getEntityBList() {
        return entityBList;
    }

    public JPA20EntityManagerEntityC getEntityCLazy() {
        return entityCLazy;
    }

    public void setEntityCLazy(JPA20EntityManagerEntityC entityCLazy) {
        this.entityCLazy = entityCLazy;
    }

    @Override
    public String toString() {
        return "JPA20EntityManagerEntityA [id=" + id + ", strData=" + strData + "]";
    }
}
