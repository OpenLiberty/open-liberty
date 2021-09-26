/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.test;

import java.util.Map;

import com.ibm.ws.classloading.exporting.test.TestInterface;

public class OneValidEntry implements TestInterface {

    Map<String, String> serviceProperties = null;

    public OneValidEntry() {}

    public OneValidEntry(Map<String,String> serviceProps) {
        serviceProperties = serviceProps;
    }

    @Override
    public String isThere(String name) {
        return name + " is there";
    }

    @Override
    public String hasProperties(String name) {
        return name + " has properties " + serviceProperties;
    }

}
