package com.ibm.ws.jpa.entitymanager.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class JPA20EntityManagerDetachEntity {
    @Id
    private int id;

    private String strData;

    @ManyToMany
    @JoinTable(name = "EMDETACH_ENTAM2MLIST")
    private List<JPA20EntityManagerEntityA> entAM2MList;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(name = "EMDETACH_ENTAM2MLIST_CA")
    private List<JPA20EntityManagerEntityA> entAM2MList_CA;

    @ManyToMany(cascade = { CascadeType.DETACH })
    @JoinTable(name = "EMDETACH_ENTAM2MLIST_CD")
    private List<JPA20EntityManagerEntityA> entAM2MList_CD;

    @ManyToOne
    private JPA20EntityManagerEntityA entAM2O;

    @ManyToOne(cascade = { CascadeType.ALL })
    private JPA20EntityManagerEntityA entAM2O_CA;

    @ManyToOne(cascade = { CascadeType.DETACH })
    private JPA20EntityManagerEntityA entAM2O_CD;

    @OneToMany
    @JoinTable(name = "EMDETACH_ENTAO2MLIST")
    private List<JPA20EntityManagerEntityA> entAO2MList;

    @OneToMany(cascade = { CascadeType.ALL })
    @JoinTable(name = "EMDETACH_ENTAO2MLIST_CA")
    private List<JPA20EntityManagerEntityA> entAO2MList_CA;

    @OneToMany(cascade = { CascadeType.DETACH })
    @JoinTable(name = "EMDETACH_ENTAO2MLIST_CD")
    private List<JPA20EntityManagerEntityA> entAO2MList_CD;

    @OneToOne
    private JPA20EntityManagerEntityA entAO2O;

    @OneToOne(cascade = { CascadeType.ALL })
    private JPA20EntityManagerEntityA entAO2O_CA;

    @OneToOne(cascade = { CascadeType.DETACH })
    private JPA20EntityManagerEntityA entAO2O_CD;

    public JPA20EntityManagerDetachEntity() {
        entAM2MList = new ArrayList<JPA20EntityManagerEntityA>();
        entAM2MList_CA = new ArrayList<JPA20EntityManagerEntityA>();
        entAM2MList_CD = new ArrayList<JPA20EntityManagerEntityA>();

        entAO2MList = new ArrayList<JPA20EntityManagerEntityA>();
        entAO2MList_CA = new ArrayList<JPA20EntityManagerEntityA>();
        entAO2MList_CD = new ArrayList<JPA20EntityManagerEntityA>();
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

    public JPA20EntityManagerEntityA getEntAM2O() {
        return entAM2O;
    }

    public void setEntAM2O(JPA20EntityManagerEntityA entAM2O) {
        this.entAM2O = entAM2O;
    }

    public JPA20EntityManagerEntityA getEntAM2O_CA() {
        return entAM2O_CA;
    }

    public void setEntAM2O_CA(JPA20EntityManagerEntityA entAM2OCA) {
        entAM2O_CA = entAM2OCA;
    }

    public JPA20EntityManagerEntityA getEntAM2O_CD() {
        return entAM2O_CD;
    }

    public void setEntAM2O_CD(JPA20EntityManagerEntityA entAM2OCD) {
        entAM2O_CD = entAM2OCD;
    }

    public JPA20EntityManagerEntityA getEntAO2O() {
        return entAO2O;
    }

    public void setEntAO2O(JPA20EntityManagerEntityA entAO2O) {
        this.entAO2O = entAO2O;
    }

    public JPA20EntityManagerEntityA getEntAO2O_CA() {
        return entAO2O_CA;
    }

    public void setEntAO2O_CA(JPA20EntityManagerEntityA entAO2OCA) {
        entAO2O_CA = entAO2OCA;
    }

    public JPA20EntityManagerEntityA getEntAO2O_CD() {
        return entAO2O_CD;
    }

    public void setEntAO2O_CD(JPA20EntityManagerEntityA entAO2OCD) {
        entAO2O_CD = entAO2OCD;
    }

    public List<JPA20EntityManagerEntityA> getEntAM2MList() {
        return entAM2MList;
    }

    public List<JPA20EntityManagerEntityA> getEntAM2MList_CA() {
        return entAM2MList_CA;
    }

    public void setEntAM2MList_CA(List<JPA20EntityManagerEntityA> entAM2MList_CA) {
        this.entAM2MList_CA = entAM2MList_CA;
    }

    public List<JPA20EntityManagerEntityA> getEntAM2MList_CD() {
        return entAM2MList_CD;
    }

    public List<JPA20EntityManagerEntityA> getEntAO2MList() {
        return entAO2MList;
    }

    public List<JPA20EntityManagerEntityA> getEntAO2MList_CA() {
        return entAO2MList_CA;
    }

    public List<JPA20EntityManagerEntityA> getEntAO2MList_CD() {
        return entAO2MList_CD;
    }

    @Override
    public String toString() {
        return "JPA20EntityManagerDetachEntity [id=" + id + ", strData=" + strData + ", entAM2MList=" + entAM2MList + ", entAM2MList_CA=" + entAM2MList_CA + ", entAM2MList_CD="
               + entAM2MList_CD + ", entAM2O=" + entAM2O + ", entAM2O_CA=" + entAM2O_CA + ", entAM2O_CD=" + entAM2O_CD + ", entAO2MList=" + entAO2MList + ", entAO2MList_CA="
               + entAO2MList_CA + ", entAO2MList_CD=" + entAO2MList_CD + ", entAO2O=" + entAO2O + ", entAO2O_CA=" + entAO2O_CA + ", entAO2O_CD=" + entAO2O_CD + "]";
    }
}
