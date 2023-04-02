/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

package com.ibm.ws.test;

import java.util.Map;

import com.ibm.ws.classloading.exporting.test.TestInterface2;

public class MultipleValidServices2 implements TestInterface2 {

    Map<String, String> bellProps = null;

    Map<String, String> updatedBellProps = null;

    public MultipleValidServices2() {}

    // Omitted. Verify server instead uses updateBell() to inject properties
    //public MultipleValidServices2(Map<String, String> props) {
    //    properties = props;
    //}

    Map<String,String> previousBellProps = null;

    public void updateBell(Map<String, String> props) {
        if (updatedBellProps != null) {
            previousBellProps = updatedBellProps;
        }
        updatedBellProps = props;
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
