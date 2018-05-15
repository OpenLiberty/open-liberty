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
package com.ibm.ws.jsf23.fat.selectoneradio;

import java.io.Serializable;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.inject.Named;

/**
 *
 */
@Named
@SessionScoped
public class SelectOneRadioGroupBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;
    private String selectedValue;
    private String selectedValue2;

    private ArrayList<String> values = null;
    private ArrayList<SelectItem> selectItems = null;

    @PostConstruct
    public void init() {
        values = new ArrayList<String>(3);
        values.add("Value1");
        values.add("Value2");
        values.add("Value3");

        selectItems = new ArrayList<SelectItem>(3);
        selectItems.add(new SelectItem("Value1"));
        selectItems.add(new SelectItem("Value2"));
        selectItems.add(new SelectItem("Value3"));
    }

    public void setSelectedValue(String selectedValue) {
        this.selectedValue = selectedValue;
    }

    public String getSelectedValue() {
        return this.selectedValue;
    }

    public void setSelectedValue2(String selectedValue2) {
        this.selectedValue2 = selectedValue2;
    }

    public String getSelectedValue2() {
        return this.selectedValue2;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }

    public ArrayList<String> getValues() {
        return this.values;
    }

    public void setSelectItems(ArrayList<SelectItem> selectItems) {
        this.selectItems = selectItems;
    }

    public ArrayList<SelectItem> getSelectItems() {
        return this.selectItems;
    }

}
