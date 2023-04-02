/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.appconfigpop;

import java.io.Serializable;

/**
 * Simple test bean with that uses managed-properties defined in new configuration callback defined APplicationConfigurationPopulator.
 */
public class MPBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean boolVal = false;

    private String addedBeanVal = "";

    private String val = "";

    public boolean getBoolVal() {
        return boolVal;
    }

    public void setBoolVal(boolean boolVal) {
        this.boolVal = boolVal;
    }

    public String getVal() {
        return "SuccessfulValTest";
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String getAddedBeanVal() {
        return addedBeanVal;
    }

    public void setAddedBeanVal(String addedBeanVal) {
        this.addedBeanVal = addedBeanVal;
    }
}
