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
package com.ibm.ws.jpa.olgh8014.model;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SimpleEntityOLGH8014 {

    @Id
    @Column(name = "KEY_CHAR")
    private String KeyString;

    @Column(name = "ITEM_STRING1")
    private String itemString1;

    @Column(name = "ITEM_INTEGER1")
    private int itemInteger1;

    @Column(name = "ITEM_INTEGER2")
    private Integer itemInteger2;

    @Column(name = "ITEM_FLOAT1")
    private float itemFloat1;

    @Column(name = "ITEM_FLOAT2")
    private Float itemFloat2;

    @Column(name = "ITEM_BIG_INTEGER1")
    private BigInteger itemBigInteger1;

    @Column(name = "ITEM_BIG_DECIMAL1", precision = 8, scale = 6)
    private BigDecimal itemBigDecimal1;

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

    public int getItemInteger1() {
        return itemInteger1;
    }

    public void setItemInteger1(int itemInteger1) {
        this.itemInteger1 = itemInteger1;
    }

    public Integer getItemInteger2() {
        return itemInteger2;
    }

    public void setItemInteger2(Integer itemInteger2) {
        this.itemInteger2 = itemInteger2;
    }

    public float getItemFloat1() {
        return itemFloat1;
    }

    public void setItemFloat1(float itemFloat1) {
        this.itemFloat1 = itemFloat1;
    }

    public Float getItemFloat2() {
        return itemFloat2;
    }

    public void setItemFloat2(Float itemFloat2) {
        this.itemFloat2 = itemFloat2;
    }

    public BigInteger getItemBigInteger1() {
        return itemBigInteger1;
    }

    public void setItemBigInteger1(BigInteger itemBigInteger1) {
        this.itemBigInteger1 = itemBigInteger1;
    }

    public BigDecimal getItemBigDecimal1() {
        return itemBigDecimal1;
    }

    public void setItemBigDecimal1(BigDecimal itemBigDecimal1) {
        this.itemBigDecimal1 = itemBigDecimal1;
    }
}
