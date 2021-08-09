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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "BONameE")
public class BOrderNameEntity implements java.io.Serializable {

    private static final long serialVersionUID = -1059986449941927485L;

    @Id
    private int id;
    private String name;

    @ManyToOne
    private OrderColumnEntity column;

    @ManyToMany(mappedBy = "bm2mNames")
    @OrderColumn
    private List<OrderColumnEntity> columns;

    public BOrderNameEntity() {
    }

    public BOrderNameEntity(String name) {
        this.id = name.charAt(0) - 'A' + 1;
        this.name = name;
    }

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

    public OrderColumnEntity getColumn() {
        return column;
    }

    public void setColumn(OrderColumnEntity column) {
        this.column = column;
    }

    public List<OrderColumnEntity> getColumns() {
        return columns;
    }

    public void setColumns(List<OrderColumnEntity> columns) {
        this.columns = columns;
    }

    public void addColumns(OrderColumnEntity column) {
        if (columns == null) {
            columns = new ArrayList<OrderColumnEntity>();
        }
        columns.add(column);
    }

    public OrderColumnEntity removeColumns(OrderColumnEntity entity) {
        OrderColumnEntity rtnVal = null;
        if (columns != null) {
            if (columns.remove(entity))
                rtnVal = entity;
        }
        return rtnVal;
    }

    @Override
    public String toString() {
        return name;
    }
}
