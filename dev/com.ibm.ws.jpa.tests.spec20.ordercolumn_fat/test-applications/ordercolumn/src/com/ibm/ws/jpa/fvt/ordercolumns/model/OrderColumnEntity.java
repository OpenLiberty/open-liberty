/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.ordercolumns.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "OColE")
public class OrderColumnEntity implements java.io.Serializable {

    private static final long serialVersionUID = 5360816293114522649L;

    @Id
    private int id;

    @OneToMany
    @OrderColumn
    @JoinTable(name = "OColE_UONameE")
    private List<UOrderNameEntity> uo2mNames;

    @OneToMany(mappedBy = "column")
    @OrderColumn
    @JoinTable(name = "OColE_BONameE")
    private List<BOrderNameEntity> bo2mNames;

    @ManyToMany
    @OrderColumn
    @JoinTable(name = "OColE_UONameE")
    private List<UOrderNameEntity> um2mNames;

    @ManyToMany
    @OrderColumn
    @JoinTable(name = "OColE_BONameE")
    private List<BOrderNameEntity> bm2mNames;

    @ElementCollection
    @OrderColumn(name = "OVRELEMENTS_ORDER")
    @CollectionTable(name = "OColE_listElements")
    private List<String> listElements;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<UOrderNameEntity> getUo2mNames() {
        return uo2mNames;
    }

    public void setUo2mNames(List<UOrderNameEntity> names) {
        this.uo2mNames = names;
    }

    public void addUo2mNames(UOrderNameEntity name) {
        if (uo2mNames == null) {
            uo2mNames = new ArrayList<UOrderNameEntity>();
        }
        uo2mNames.add(name);
    }

    public UOrderNameEntity removeUo2mNames(int location) {
        UOrderNameEntity rtnVal = null;
        if (uo2mNames != null) {
            rtnVal = uo2mNames.remove(location);
        }
        return rtnVal;
    }

    public void insertUo2mNames(int location, UOrderNameEntity name) {
        if (uo2mNames == null) {
            uo2mNames = new ArrayList<UOrderNameEntity>();
        }
        uo2mNames.add(location, name);
    }

    public List<BOrderNameEntity> getBo2mNames() {
        return bo2mNames;
    }

    public void setBo2mNames(List<BOrderNameEntity> names) {
        this.bo2mNames = names;
    }

    public void addBo2mNames(BOrderNameEntity name) {
        if (bo2mNames == null) {
            bo2mNames = new ArrayList<BOrderNameEntity>();
        }
        bo2mNames.add(name);
    }

    public BOrderNameEntity removeBo2mNames(int location) {
        BOrderNameEntity rtnVal = null;
        if (bo2mNames != null) {
            rtnVal = bo2mNames.remove(location);
        }
        return rtnVal;
    }

    public void insertBo2mNames(int location, BOrderNameEntity name) {
        if (bo2mNames == null) {
            bo2mNames = new ArrayList<BOrderNameEntity>();
        }
        bo2mNames.add(location, name);
    }

    public List<UOrderNameEntity> getUm2mNames() {
        return um2mNames;
    }

    public void setUm2mNames(List<UOrderNameEntity> names) {
        this.um2mNames = names;
    }

    public void addUm2mNames(UOrderNameEntity name) {
        if (um2mNames == null) {
            um2mNames = new ArrayList<UOrderNameEntity>();
        }
        um2mNames.add(name);
    }

    public UOrderNameEntity removeUm2mNames(int location) {
        UOrderNameEntity rtnVal = null;
        if (um2mNames != null) {
            rtnVal = um2mNames.remove(location);
        }
        return rtnVal;
    }

    public void insertUm2mNames(int location, UOrderNameEntity name) {
        if (um2mNames == null) {
            um2mNames = new ArrayList<UOrderNameEntity>();
        }
        um2mNames.add(location, name);
    }

    public List<BOrderNameEntity> getBm2mNames() {
        return bm2mNames;
    }

    public void setBm2mNames(List<BOrderNameEntity> names) {
        this.bm2mNames = names;
    }

    public void addBm2mNames(BOrderNameEntity name) {
        if (bm2mNames == null) {
            bm2mNames = new ArrayList<BOrderNameEntity>();
        }
        bm2mNames.add(name);
    }

    public BOrderNameEntity removeBm2mNames(int location) {
        BOrderNameEntity rtnVal = null;
        if (bm2mNames != null) {
            rtnVal = bm2mNames.remove(location);
        }
        return rtnVal;
    }

    public void insertBm2mNames(int location, BOrderNameEntity name) {
        if (bm2mNames == null) {
            bm2mNames = new ArrayList<BOrderNameEntity>();
        }
        bm2mNames.add(location, name);
    }

    public List<String> getListElements() {
        return listElements;
    }

    public void setListElements(List<String> elements) {
        this.listElements = elements;
    }

    public void addListElements(String element) {
        if (listElements == null) {
            listElements = new ArrayList<String>();
        }
        listElements.add(element);
    }

    public String removeListElements(int location) {
        String rtnVal = null;
        if (listElements != null) {
            rtnVal = listElements.remove(location);
        }
        return rtnVal;
    }

    public void insertListElements(int location, String name) {
        if (listElements == null) {
            listElements = new ArrayList<String>();
        }
        listElements.add(location, name);
    }

    @Override
    public String toString() {
        return "OrderColumnEntity[" + id + "]=" + uo2mNames;
    }
}
