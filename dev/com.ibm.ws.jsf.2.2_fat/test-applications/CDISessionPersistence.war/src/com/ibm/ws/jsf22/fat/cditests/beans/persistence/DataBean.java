/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.cditests.beans.persistence;

import javax.inject.Inject;
import javax.inject.Named;
import javax.faces.flow.FlowScoped;

@Named
@FlowScoped(value = "simpleFlow")
public class DataBean implements java.io.Serializable {

    private String data = "test";

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
