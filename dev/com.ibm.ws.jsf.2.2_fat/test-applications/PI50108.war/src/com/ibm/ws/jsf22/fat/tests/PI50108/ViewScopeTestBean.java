/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests.PI50108;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.html.HtmlDataTable;

@ManagedBean(name="viewScopeTestBean")
@ViewScoped
public class ViewScopeTestBean implements Serializable {

    private List<String> stringCollection;

    public List<String> getStringCollection() {
        return stringCollection;
    }

    public void setStringCollection(List<String> stringCollection) {
        this.stringCollection = stringCollection;
    }

    private transient HtmlDataTable dataTable;

    public HtmlDataTable getDataTable() {
        return dataTable;
    }

    public void setDataTable(HtmlDataTable dataTable) {
        this.dataTable = dataTable;
    }

    @PostConstruct
    public void init(){
        System.out.println("Post Construct fired!!");
        stringCollection = new ArrayList<String>();
        stringCollection.add("a");
        stringCollection.add("b");
        stringCollection.add("c");

    }

    public void action(){
        System.out.println("Clicked!!");

    }
}
