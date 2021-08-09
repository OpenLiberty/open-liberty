/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
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
