/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

public class PmiProps {
    private int type;
    private String factoryId;
    private String providerId;
    private String pmiName;

    PmiProps() {
        type = 0;
        factoryId = "";
        providerId = "";
        pmiName = "";
    }

    public String getFactoryId() {
        return factoryId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getPmiName() {
        return pmiName;
    }

    public int getType() {
        return type;
    }

    public void setFactoryId(String factoryId) {
        this.factoryId = factoryId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setPmiName(String pmiName) {
        this.pmiName = pmiName;
    }

    public void setType(int type) {
        this.type = type;
    }
}
