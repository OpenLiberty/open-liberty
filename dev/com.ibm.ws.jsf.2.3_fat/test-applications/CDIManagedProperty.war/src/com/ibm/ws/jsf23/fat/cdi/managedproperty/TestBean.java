/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.cdi.managedproperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A CDI bean that will be used for injection in ManagedPropertyBean.java.
 *
 * There are multiple values being tested to ensure @ManagedProperty injection works
 * with an assortment of types.
 *
 */
@Named
@RequestScoped
public class TestBean {
    private int number = 0;
    private String text = "zero";
    private List<String> list = new ArrayList<String>(Arrays.asList("zero"));
    private String[] stringArray = new String[] { "zero" };
    private String listValue = "zero";
    private String stringArrayValue = "zero";

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumber() {
        return this.number;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    public List<String> getList() {
        return this.list;
    }

    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public String[] getStringArray() {
        return this.stringArray;
    }

    public void setListValue(String listValue) {
        this.listValue = listValue;
    }

    public String getListValue() {
        return this.listValue;
    }

    public void setStringArrayValue(String stringArrayValue) {
        this.stringArrayValue = stringArrayValue;
    }

    public String getStringArrayValue() {
        return this.stringArrayValue;
    }

    public void addValues() {
        list.add(0, getListValue());
        stringArray[0] = getStringArrayValue();
    }
}
