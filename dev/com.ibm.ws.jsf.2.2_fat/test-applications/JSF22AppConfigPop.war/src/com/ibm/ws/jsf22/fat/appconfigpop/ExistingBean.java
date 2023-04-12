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

/**
 * Simple test bean, this one defined through faces-config.xml
 */
public class ExistingBean {

    private String message = "SuccessfulExistingBeanTest";

    public String getMessage() {

        return message;
    }

    public void setMessageBean(String message) {
        this.message = message;
    }
}
