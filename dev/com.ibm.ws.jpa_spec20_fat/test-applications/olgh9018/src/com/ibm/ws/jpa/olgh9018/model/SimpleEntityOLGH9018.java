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

package com.ibm.ws.jpa.olgh9018.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SimpleEntityOLGH9018 {
    @Id
    private int id;

    private String str1;
    private String str2;

    private int int1;

    public SimpleEntityOLGH9018() {

    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the str1
     */
    public String getStr1() {
        return str1;
    }

    /**
     * @param str1 the str1 to set
     */
    public void setStr1(String str1) {
        this.str1 = str1;
    }

    /**
     * @return the str2
     */
    public String getStr2() {
        return str2;
    }

    /**
     * @param str2 the str2 to set
     */
    public void setStr2(String str2) {
        this.str2 = str2;
    }

    /**
     * @return the int1
     */
    public int getInt1() {
        return int1;
    }

    /**
     * @param int1 the int1 to set
     */
    public void setInt1(int int1) {
        this.int1 = int1;
    }
}
