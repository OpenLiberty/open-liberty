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
package com.ibm.ws.jpa.olgh8294.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SimpleEntityOLGH8294 {

    @Id
    @Column(name = "KEY_CHAR")
    private String KeyString;

    @Column(name = "ITEM_STRING1")
    private String itemString1;

    @Column(name = "ITEM_INTEGER1")
    private Integer itemInteger1;

    @Column(name = "ITEM_BOOLEAN1")
    private boolean itemBoolean1;

    @Column(name = "ITEM_DATE1")
    private Date itemDate1;

    public String getKeyString() {
        return KeyString;
    }

    public void setKeyString(String keyString) {
        KeyString = keyString;
    }

    public String getItemString1() {
        return itemString1;
    }

    public void setItemString1(String itemString1) {
        this.itemString1 = itemString1;
    }

    public Integer getItemInteger1() {
        return itemInteger1;
    }

    public void setItemInteger1(Integer itemInteger1) {
        this.itemInteger1 = itemInteger1;
    }

    public boolean isItemBoolean1() {
        return itemBoolean1;
    }

    public void setItemBoolean1(boolean itemBoolean1) {
        this.itemBoolean1 = itemBoolean1;
    }

    public Date getItemDate1() {
        return itemDate1;
    }

    public void setItemDate1(Date itemDate1) {
        this.itemDate1 = itemDate1;
    }
}
