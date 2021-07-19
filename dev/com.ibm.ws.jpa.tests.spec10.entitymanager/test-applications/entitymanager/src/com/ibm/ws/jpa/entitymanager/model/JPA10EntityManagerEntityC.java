package com.ibm.ws.jpa.entitymanager.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class JPA10EntityManagerEntityC {
    @Id
    private int id;

    private String strData;

    @OneToOne
    private JPA10EntityManagerEntityA entityA;

    @OneToOne(fetch = FetchType.LAZY)
    private JPA10EntityManagerEntityA entityALazy;

    public JPA10EntityManagerEntityC() {

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

    public JPA10EntityManagerEntityA getEntityA() {
        return entityA;
    }

    public void setEntityA(JPA10EntityManagerEntityA entityA) {
        this.entityA = entityA;
    }

    public JPA10EntityManagerEntityA getEntityALazy() {
        return entityALazy;
    }

    public void setEntityALazy(JPA10EntityManagerEntityA entityALazy) {
        this.entityALazy = entityALazy;
    }

    @Override
    public String toString() {
        return "JPA10EntityManagerEntityC [id=" + id + ", strData=" + strData + "]";
    }
}
