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

package com.ibm.ws.jpa.query.forcebindparameters.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AFEntity {
    @Id
    private int id;

    private String strData;
    private int intData;

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
     * @return the strData
     */
    public String getStrData() {
        return strData;
    }

    /**
     * @param strData the strData to set
     */
    public void setStrData(String strData) {
        this.strData = strData;
    }

    /**
     * @return the intData
     */
    public int getIntData() {
        return intData;
    }

    /**
     * @param intData the intData to set
     */
    public void setIntData(int intData) {
        this.intData = intData;
    }

}
