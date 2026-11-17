package com.ibm.ws.jpa.entitymanager.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class JPA20EntityManagerEntityC {
    @Id
    private int id;

    private String strData;

    @OneToOne
    private JPA20EntityManagerEntityA entityA;

    @OneToOne(fetch = FetchType.LAZY)
    private JPA20EntityManagerEntityA entityALazy;

    public JPA20EntityManagerEntityC() {

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

    public JPA20EntityManagerEntityA getEntityA() {
        return entityA;
    }

    public void setEntityA(JPA20EntityManagerEntityA entityA) {
        this.entityA = entityA;
    }

    public JPA20EntityManagerEntityA getEntityALazy() {
        return entityALazy;
    }

    public void setEntityALazy(JPA20EntityManagerEntityA entityALazy) {
        this.entityALazy = entityALazy;
    }

    @Override
    public String toString() {
        return "JPA20EntityManagerEntityC [id=" + id + ", strData=" + strData + "]";
    }
}
