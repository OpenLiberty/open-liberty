/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.converter.validator.behavior.injection.beans;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Simple ApplicationScoped bean
 */
@Named
@ApplicationScoped
public class TestCDIBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private String testValue;
    private final String earth = "Earth";
    private final String world = "World";

    public String getEarth() {
        return earth;
    }

    public String getWorld() {
        return world;
    }

    public String getTestValue() {
        return testValue;
    }

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }

}
