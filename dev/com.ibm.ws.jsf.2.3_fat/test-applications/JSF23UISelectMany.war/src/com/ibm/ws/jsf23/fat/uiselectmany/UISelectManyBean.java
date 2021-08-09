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
package com.ibm.ws.jsf23.fat.uiselectmany;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.inject.Named;

/**
 * Simple Session Scoped bean to test UISelectMany through <h:selectManyCheckbox/>
 */
@Named
@SessionScoped
public class UISelectManyBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private List<String> selectedItems = null;
    private List<SelectItem> items = null;

    private List<Value> selectedValues = null;

    public static enum Value {
        A, B, C, D, E
    }

    private List<String> staticSelectedItems = null;

    @PostConstruct
    public void init() {
        items = new ArrayList<SelectItem>(3);
        items.add(new SelectItem("Item1"));
        items.add(new SelectItem("Item2"));
        items.add(new SelectItem("Item3"));
    }

    public List<String> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<String> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public List<SelectItem> getItems() {
        return this.items;
    }

    public List<Value> getAllValues() {
        return Arrays.asList(Value.values());
    }

    public List<Value> getSelectedValues() {
        return selectedValues;
    }

    public void setSelectedValues(List<Value> selectedValues) {
        this.selectedValues = selectedValues;
    }

    public List<String> getStaticSelectedItems() {
        return staticSelectedItems;
    }

    public void setStaticSelectedItems(List<String> staticSelectedItems) {
        this.staticSelectedItems = staticSelectedItems;
    }

}
