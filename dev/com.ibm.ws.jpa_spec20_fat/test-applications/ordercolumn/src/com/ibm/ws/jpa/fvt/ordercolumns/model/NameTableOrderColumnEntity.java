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

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "NTblOColE")
public class NameTableOrderColumnEntity implements java.io.Serializable {

    private static final long serialVersionUID = 6122423180818539040L;

    @Id
    private int id;
    private String name;

    @ElementCollection
    @OrderColumn
    @CollectionTable(name = "NTblOColE_oNameTypeElem")
    private List<String> orderNameTypeElements;

    @ElementCollection
    @OrderColumn(name = "Diff_OrderColumn_Name")
    @CollectionTable(name = "Diff_Table_Name")
    private List<String> overrideOrderColumnNameElements;

    private List<String> xmlOrderNameTypeElements;
    private List<String> xmlOverrideOrderColumnNameElements;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOrderNameTypeElements() {
        return orderNameTypeElements;
    }

    public void setOrderNameTypeElements(List<String> elements) {
        this.orderNameTypeElements = elements;
    }

    public List<String> getXmlOrderNameTypeElements() {
        return xmlOrderNameTypeElements;
    }

    public void setXmlOrderNameTypeElements(List<String> elements) {
        this.xmlOrderNameTypeElements = elements;
    }

    public List<String> getOverrideOrderColumnNameElements() {
        return overrideOrderColumnNameElements;
    }

    public void setOverrideOrderColumnNameElements(List<String> elements) {
        this.overrideOrderColumnNameElements = elements;
    }

    public List<String> getXmlOverrideOrderColumnNameElements() {
        return xmlOverrideOrderColumnNameElements;
    }

    public void setXmlOverrideOrderColumnNameElements(List<String> elements) {
        this.xmlOverrideOrderColumnNameElements = elements;
    }

    @Override
    public String toString() {
        return "OrderColumnNameTypeEntity[" + id + "]=" + name;
    }
}
