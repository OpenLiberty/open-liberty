/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import com.ibm.ws.classloading.exporting.test.TestInterface2;

public class PropertiesResolveLibertyVars implements TestInterface2 {

    Map<String, String> bellProps = null;

    Map<String, String> updatedBellProps = null;

    public PropertiesResolveLibertyVars() {}

    public PropertiesResolveLibertyVars(Map<String, String> bProps) {
        System.out.println("PropertiesResolveLibertyVars.CTOR.props: " + bProps);
        bellProps = bProps;
    }

    Map<String,String> previousBellProps = null;

    public void updateBell(Map<String, String> ubProps) {
        if (updatedBellProps != null) {
            previousBellProps = updatedBellProps;
        }
        updatedBellProps = ubProps;
    }

    @Override
    public String isThere2(String name) {
        return name + " is there";
    }

    @Override
    public String hasProperties2(String name) {
        return name + " has properties " + bellProps;
    }

    @Override
    public String hasUpdatedProperties2(String name) {
        return name + " has updated properties " + updatedBellProps;
    }
}
