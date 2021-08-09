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
package com.ibm.ws.jpa.olgh10240.model;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Simple Entity that exists just so we can use the table in stored procedures
 */
@Entity
public class SimpleEntityOLGH10240 {

    @Id
    private String keyString;

    private String itemString1;

    private Integer itemInteger1;

    public SimpleEntityOLGH10240() {}

    public SimpleEntityOLGH10240(String keyString, String itemString1, Integer itemInteger1) {
        this.keyString = keyString;
        this.itemString1 = itemString1;
        this.itemInteger1 = itemInteger1;
    }

    public String getKeyString() {
        return keyString;
    }

    public void setKeyString(String keyString) {
        this.keyString = keyString;
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
}