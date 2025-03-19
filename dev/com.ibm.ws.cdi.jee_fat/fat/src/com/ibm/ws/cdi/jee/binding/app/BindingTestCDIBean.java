/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.binding.app;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BindingTestCDIBean {

    @Resource(name = "testValue1", lookup = "testValue1")
    private String testValue1;

    @Resource(name = "testValue2")
    private String testValue2;

    @Resource(name = "testValue3")
    private String testValue3;

    public String getTestValue1() {
        return testValue1;
    }

    public String getTestValue2() {
        return testValue2;
    }

    public String getTestValue3() {
        return testValue3;
    }
}
