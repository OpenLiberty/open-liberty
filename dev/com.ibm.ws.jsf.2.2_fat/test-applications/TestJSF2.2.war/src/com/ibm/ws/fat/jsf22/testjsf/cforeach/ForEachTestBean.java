/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.fat.jsf22.testjsf.cforeach;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 * This bean helps testing the c:forEach tag when using an object with custom equals method (overriden equals)
 * and when using an object that is not serializable.
 */
@RequestScoped
@ManagedBean(name = "forEachTest")
public class ForEachTestBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<SimpleOverridenEqualsObject> list;
    private List<SimpleSerializableObject> listSerializable;
    private List<SimpleNotSerializableObject> listNotSerializable;
    private int counter;

    public ForEachTestBean() {
        counter = 0;
        initalize();
    }

    public void initalize() {
        changeItems();
    }

    public List<SimpleOverridenEqualsObject> getList() {
        return list;
    }

    public void setList(List<SimpleOverridenEqualsObject> list) {
        this.list = list;
    }

    public List<SimpleSerializableObject> getListSerializable() {
        return listSerializable;
    }

    public void setListSerializable(List<SimpleSerializableObject> listSerializable) {
        this.listSerializable = listSerializable;
    }

    public List<SimpleNotSerializableObject> getListNotSerializable() {
        return listNotSerializable;
    }

    public void setListNotSerializable(List<SimpleNotSerializableObject> listNotSerializable) {
        this.listNotSerializable = listNotSerializable;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    /**
     * Create the objects to be tested and add each of them in their respective list
     * so they can be used in the c:forEach tag.
     */
    public void changeItems() {
        counter++;
        list = new ArrayList<SimpleOverridenEqualsObject>();
        listNotSerializable = new ArrayList<SimpleNotSerializableObject>();
        listSerializable = new ArrayList<SimpleSerializableObject>();
        for (int i = 0; i < 5; i++) {
            SimpleOverridenEqualsObject obj = new SimpleOverridenEqualsObject();
            obj.setId(Long.valueOf(i));
            obj.setValue(String.valueOf(i * counter));
            list.add(obj);
            SimpleSerializableObject objSerializable = new SimpleSerializableObject();
            objSerializable.setId(Long.valueOf(i));
            objSerializable.setValue(String.valueOf(i * counter));
            listSerializable.add(objSerializable);
            SimpleNotSerializableObject objNotSerializable = new SimpleNotSerializableObject();
            objNotSerializable.setId(Long.valueOf(i));
            objNotSerializable.setValue(String.valueOf(i * counter));
            listNotSerializable.add(objNotSerializable);
        }
    }
}
