/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.jpa20.entitymanager.model;

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
import javax.persistence.Version;

@Entity
public class JPA20EMDetachEntity {
    @Id
    private int id;

    private String strData;

    @ManyToMany
    @JoinTable(name = "EMDETACH_ENTAM2MLIST")
    private List<JPA20EMEntityA> entAM2MList;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(name = "EMDETACH_ENTAM2MLIST_CA")
    private List<JPA20EMEntityA> entAM2MList_CA;

    @ManyToMany(cascade = { CascadeType.DETACH })
    @JoinTable(name = "EMDETACH_ENTAM2MLIST_CD")
    private List<JPA20EMEntityA> entAM2MList_CD;

    @ManyToOne
    private JPA20EMEntityA entAM2O;

    @ManyToOne(cascade = { CascadeType.ALL })
    private JPA20EMEntityA entAM2O_CA;

    @ManyToOne(cascade = { CascadeType.DETACH })
    private JPA20EMEntityA entAM2O_CD;

    @OneToMany
    @JoinTable(name = "EMDETACH_ENTAO2MLIST")
    private List<JPA20EMEntityA> entAO2MList;

    @OneToMany(cascade = { CascadeType.ALL })
    @JoinTable(name = "EMDETACH_ENTAO2MLIST_CA")
    private List<JPA20EMEntityA> entAO2MList_CA;

    @OneToMany(cascade = { CascadeType.DETACH })
    @JoinTable(name = "EMDETACH_ENTAO2MLIST_CD")
    private List<JPA20EMEntityA> entAO2MList_CD;

    @OneToOne
    private JPA20EMEntityA entAO2O;

    @OneToOne(cascade = { CascadeType.ALL })
    private JPA20EMEntityA entAO2O_CA;

    @OneToOne(cascade = { CascadeType.DETACH })
    private JPA20EMEntityA entAO2O_CD;

    @Version
    private long version;

    public JPA20EMDetachEntity() {
        entAM2MList = new ArrayList<JPA20EMEntityA>();
        entAM2MList_CA = new ArrayList<JPA20EMEntityA>();
        entAM2MList_CD = new ArrayList<JPA20EMEntityA>();

        entAO2MList = new ArrayList<JPA20EMEntityA>();
        entAO2MList_CA = new ArrayList<JPA20EMEntityA>();
        entAO2MList_CD = new ArrayList<JPA20EMEntityA>();
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

    public JPA20EMEntityA getEntAM2O() {
        return entAM2O;
    }

    public void setEntAM2O(JPA20EMEntityA entAM2O) {
        this.entAM2O = entAM2O;
    }

    public JPA20EMEntityA getEntAM2O_CA() {
        return entAM2O_CA;
    }

    public void setEntAM2O_CA(JPA20EMEntityA entAM2OCA) {
        entAM2O_CA = entAM2OCA;
    }

    public JPA20EMEntityA getEntAM2O_CD() {
        return entAM2O_CD;
    }

    public void setEntAM2O_CD(JPA20EMEntityA entAM2OCD) {
        entAM2O_CD = entAM2OCD;
    }

    public JPA20EMEntityA getEntAO2O() {
        return entAO2O;
    }

    public void setEntAO2O(JPA20EMEntityA entAO2O) {
        this.entAO2O = entAO2O;
    }

    public JPA20EMEntityA getEntAO2O_CA() {
        return entAO2O_CA;
    }

    public void setEntAO2O_CA(JPA20EMEntityA entAO2OCA) {
        entAO2O_CA = entAO2OCA;
    }

    public JPA20EMEntityA getEntAO2O_CD() {
        return entAO2O_CD;
    }

    public void setEntAO2O_CD(JPA20EMEntityA entAO2OCD) {
        entAO2O_CD = entAO2OCD;
    }

    public List<JPA20EMEntityA> getEntAM2MList() {
        return entAM2MList;
    }

    public List<JPA20EMEntityA> getEntAM2MList_CA() {
        return entAM2MList_CA;
    }

    public void setEntAM2MList_CA(List<JPA20EMEntityA> entAM2MList_CA) {
        this.entAM2MList_CA = entAM2MList_CA;
    }

    public List<JPA20EMEntityA> getEntAM2MList_CD() {
        return entAM2MList_CD;
    }

    public List<JPA20EMEntityA> getEntAO2MList() {
        return entAO2MList;
    }

    public List<JPA20EMEntityA> getEntAO2MList_CA() {
        return entAO2MList_CA;
    }

    public List<JPA20EMEntityA> getEntAO2MList_CD() {
        return entAO2MList_CD;
    }

    public long getVersion() {
        return version;
    }

}
