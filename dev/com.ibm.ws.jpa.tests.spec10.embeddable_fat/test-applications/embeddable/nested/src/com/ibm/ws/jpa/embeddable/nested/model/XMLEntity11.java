/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.nested.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class XMLEntity11 implements IEntity11 {

    private int id;
    private String ent11_str01;
    private String ent11_str02;
    private String ent11_str03;
    private List<Embeddable11> ent11_list;
    private LinkedList<Embeddable11> ent11_llist;
    private Map<Timestamp, Embeddable11> ent11_map;
    private Set<Embeddable11> ent11_set;
    private Vector<Embeddable11> ent11_vector;

    public XMLEntity11() {
        ent11_list = new ArrayList<Embeddable11>();
        ent11_llist = new LinkedList<Embeddable11>();
        ent11_map = new HashMap<Timestamp, Embeddable11>();
        ent11_set = new HashSet<Embeddable11>();
        ent11_vector = new Vector<Embeddable11>();
    }

    public XMLEntity11(String ent11_str01,
                       String ent11_str02,
                       String ent11_str03,
                       List<Embeddable11> ent11_list,
                       LinkedList<Embeddable11> ent11_llist,
                       Map<Timestamp, Embeddable11> ent11_map,
                       Set<Embeddable11> ent11_set,
                       Vector<Embeddable11> ent11_vector) {
        this.ent11_str01 = ent11_str01;
        this.ent11_str02 = ent11_str02;
        this.ent11_str02 = ent11_str03;
        this.ent11_list = ent11_list;
        this.ent11_llist = ent11_llist;
        this.ent11_map = ent11_map;
        this.ent11_set = ent11_set;
        this.ent11_vector = ent11_vector;
    }

    @Override
    public String toString() {
        return ("XMLEntity11: id: " + getId() +
                " ent11_str01: " + getEnt11_str01() +
                " ent11_str02: " + getEnt11_str02() +
                " ent11_str03: " + getEnt11_str03() +
                " ent11_list: " + getEnt11_list() +
                " ent11_llist: " + getEnt11_llist() +
                " ent11_map: " + getEnt11_map() +
                " ent11_set: " + getEnt11_set() +
                " ent11_vector: " + getEnt11_vector());
    }

    //----------------------------------------------------------------------------------------------
    // XMLEntity11 fields
    //----------------------------------------------------------------------------------------------
    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getEnt11_str01() {
        return ent11_str01;
    }

    @Override
    public void setEnt11_str01(String str) {
        this.ent11_str01 = str;
    }

    @Override
    public String getEnt11_str02() {
        return ent11_str02;
    }

    @Override
    public void setEnt11_str02(String str) {
        this.ent11_str02 = str;
    }

    @Override
    public String getEnt11_str03() {
        return ent11_str03;
    }

    @Override
    public void setEnt11_str03(String str) {
        this.ent11_str03 = str;
    }

    @Override
    public List<Embeddable11> getEnt11_list() {
        return ent11_list;
    }

    @Override
    public void setEnt11_list(List<Embeddable11> list) {
        this.ent11_list = list;
    }

    @Override
    public LinkedList<Embeddable11> getEnt11_llist() {
        return ent11_llist;
    }

    @Override
    public void setEnt11_llist(LinkedList<Embeddable11> llist) {
        this.ent11_llist = llist;
    }

    @Override
    public Map<Timestamp, Embeddable11> getEnt11_map() {
        return ent11_map;
    }

    @Override
    public void setEnt11_map(Map<Timestamp, Embeddable11> map) {
        this.ent11_map = map;
    }

    @Override
    public Set<Embeddable11> getEnt11_set() {
        return ent11_set;
    }

    @Override
    public void setEnt11_set(Set<Embeddable11> set) {
        this.ent11_set = set;
    }

    @Override
    public Vector<Embeddable11> getEnt11_vector() {
        return ent11_vector;
    }

    @Override
    public void setEnt11_vector(Vector<Embeddable11> vector) {
        this.ent11_vector = vector;
    }
}
